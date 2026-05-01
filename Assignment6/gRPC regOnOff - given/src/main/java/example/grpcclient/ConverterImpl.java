package example.grpcclient;

import io.grpc.stub.StreamObserver;
import service.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConverterImpl extends ConverterGrpc.ConverterImplBase {

    @Override
    public void convert(ConversionRequest req, StreamObserver<ConversionResponse> responseObserver) {
        System.out.println("Received from client: " + req.getValue() + " " + req.getFromUnit() + " to " + req.getToUnit());
        ConversionResponse.Builder response = ConversionResponse.newBuilder();

        // change units to uppercase
        String fromUnit = req.getFromUnit().toUpperCase();
        String toUnit = req.getToUnit().toUpperCase();
        double value = req.getValue();

        // check for non-finite values
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            responseObserver.onNext(response.setIsSuccess(false).setError("value must be a finite number").build());
            responseObserver.onCompleted();
            return;
        }

        // check missing from unit
        if (fromUnit.isEmpty()) {
            responseObserver.onNext(response.setIsSuccess(false).setError("from_unit is required").build());
            responseObserver.onCompleted();
            return;
        }
        // check missing to unit
        if (toUnit.isEmpty()) {
            responseObserver.onNext(response.setIsSuccess(false).setError("to_unit is required").build());
            responseObserver.onCompleted();
            return;
        }
        // check identical units
        if (fromUnit.equals(toUnit)) {
            responseObserver.onNext(response.setIsSuccess(false).setError("same unit - no conversion needed").build());
            responseObserver.onCompleted();
            return;
        }

        // supported units by category
        List<String> lengthUnits = new ArrayList<>(Arrays.asList("KILOMETER", "MILE", "YARD", "FOOT"));
        List<String> weightUnits = new ArrayList<>(Arrays.asList("KILOGRAM", "POUND"));
        List<String> tempUnits = new ArrayList<>(Arrays.asList("CELSIUS", "FAHRENHEIT"));

        // check unknown from unit
        if (!lengthUnits.contains(fromUnit) && !weightUnits.contains(fromUnit) && !tempUnits.contains(fromUnit)) {
            responseObserver.onNext(response.setIsSuccess(false).setError("unsupported unit: " + fromUnit).build());
            responseObserver.onCompleted();
            return;
        }
        // check unknown to unit
        if (!lengthUnits.contains(toUnit) && !weightUnits.contains(toUnit) && !tempUnits.contains(toUnit)) {
            responseObserver.onNext(response.setIsSuccess(false).setError("unsupported unit: " + toUnit).build());
            responseObserver.onCompleted();
            return;
        }

        // check units share a category
        boolean bothLength = lengthUnits.contains(fromUnit) && lengthUnits.contains(toUnit);
        boolean bothWeight = weightUnits.contains(fromUnit) && weightUnits.contains(toUnit);
        boolean bothTemp = tempUnits.contains(fromUnit) && tempUnits.contains(toUnit);

        // check cross-category conversion
        if (!bothLength && !bothWeight && !bothTemp) {
            responseObserver.onNext(response.setIsSuccess(false)
                .setError("units do not match - cannot convert " + fromUnit + " to " + toUnit).build());
            responseObserver.onCompleted();
            return;
        }

        // look to see if its absolute zero
        if (fromUnit.equals("CELSIUS") && value < -273.15) {
            responseObserver.onNext(response.setIsSuccess(false)
                .setError("temperature below absolute zero (−273.15°C or −459.67°F)").build());
            responseObserver.onCompleted();
            return;
        }
        if (fromUnit.equals("FAHRENHEIT") && value < -459.67) {
            responseObserver.onNext(response.setIsSuccess(false)
                .setError("temperature below absolute zero (−273.15°C or −459.67°F)").build());
            responseObserver.onCompleted();
            return;
        }

        double result = 0;

        if (bothTemp) {
            // convert between Celsius and Fahrenheit
            if (fromUnit.equals("CELSIUS")) {
                result = value * 9.0/5.0 + 32;
            } else {
                result = (value - 32) * 5.0/9.0;
            }
        } else if (bothLength) {
            // change to meters first
            double meters;
            switch (fromUnit) {
                case "KILOMETER":
                    meters = value * 1000;
                    break;
                case "MILE":
                    meters = value * 1609.34;
                    break;
                case "YARD":
                    meters = value * 0.9144;
                    break;
                default:
                    meters = value * 0.3048;
                    break; // FOOT
            }
            // convert meters to target unit
            switch (toUnit) {
                case "KILOMETER":
                    result = meters / 1000;
                    break;
                case "MILE":
                    result = meters / 1609.34;
                    break;
                case "YARD":
                    result = meters / 0.9144;
                    break;
                default:
                    result = meters / 0.3048;
                    break; // FOOT
            }
        } else { // bothWeight 1 kg = 2.20462 lb
            // change to kilograms first
            double kg;
            if (fromUnit.equals("KILOGRAM")) {
                kg = value;
            } else {
                kg = value / 2.20462;
            }
            // convert kg to target unit
            if (toUnit.equals("KILOGRAM")) {
                result = kg;
            } else {
                result = kg * 2.20462;
            }
        }

        responseObserver.onNext(response.setIsSuccess(true).setResult(result).build());
        responseObserver.onCompleted();
    }
}
