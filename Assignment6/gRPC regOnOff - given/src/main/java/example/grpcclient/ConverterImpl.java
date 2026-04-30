package example.grpcclient;

import io.grpc.stub.StreamObserver;
import service.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ConverterImpl extends ConverterGrpc.ConverterImplBase {

    @Override
    public void convert(ConversionRequest req, StreamObserver<ConversionResponse> responseObserver) {
        System.out.println("Received from client: " + req.getValue() + " " + req.getFromUnit() + " to " + req.getToUnit());
        ConversionResponse.Builder response = ConversionResponse.newBuilder();

        String fromUnit = req.getFromUnit().toUpperCase();
        String toUnit = req.getToUnit().toUpperCase();
        double value = req.getValue();

        if (fromUnit.isEmpty()) {
            responseObserver.onNext(response.setIsSuccess(false).setError("from_unit is required").build());
            responseObserver.onCompleted();
            return;
        }
        if (toUnit.isEmpty()) {
            responseObserver.onNext(response.setIsSuccess(false).setError("to_unit is required").build());
            responseObserver.onCompleted();
            return;
        }
        if (fromUnit.equals(toUnit)) {
            responseObserver.onNext(response.setIsSuccess(false).setError("same unit - no conversion needed").build());
            responseObserver.onCompleted();
            return;
        }

        Set<String> lengthUnits = new HashSet<>(Arrays.asList("KILOMETER", "MILE", "YARD", "FOOT"));
        Set<String> weightUnits = new HashSet<>(Arrays.asList("KILOGRAM", "POUND"));
        Set<String> tempUnits   = new HashSet<>(Arrays.asList("CELSIUS", "FAHRENHEIT"));

        if (!lengthUnits.contains(fromUnit) && !weightUnits.contains(fromUnit) && !tempUnits.contains(fromUnit)) {
            responseObserver.onNext(response.setIsSuccess(false).setError("unsupported unit: " + fromUnit).build());
            responseObserver.onCompleted();
            return;
        }
        if (!lengthUnits.contains(toUnit) && !weightUnits.contains(toUnit) && !tempUnits.contains(toUnit)) {
            responseObserver.onNext(response.setIsSuccess(false).setError("unsupported unit: " + toUnit).build());
            responseObserver.onCompleted();
            return;
        }

        boolean bothLength = lengthUnits.contains(fromUnit) && lengthUnits.contains(toUnit);
        boolean bothWeight = weightUnits.contains(fromUnit) && weightUnits.contains(toUnit);
        boolean bothTemp   = tempUnits.contains(fromUnit)   && tempUnits.contains(toUnit);

        if (!bothLength && !bothWeight && !bothTemp) {
            responseObserver.onNext(response.setIsSuccess(false)
                .setError("units do not match - cannot convert " + fromUnit + " to " + toUnit).build());
            responseObserver.onCompleted();
            return;
        }

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
            if (fromUnit.equals("CELSIUS")) {
                result = value * 9.0 / 5.0 + 32;
            } else {
                result = (value - 32) * 5.0 / 9.0;
            }
        } else if (bothLength) {
            double meters;
            switch (fromUnit) {
                case "KILOMETER": meters = value * 1000;    break;
                case "MILE":      meters = value * 1609.34; break;
                case "YARD":      meters = value * 0.9144;  break;
                default:          meters = value * 0.3048;  break; // FOOT
            }
            switch (toUnit) {
                case "KILOMETER": result = meters / 1000;    break;
                case "MILE":      result = meters / 1609.34; break;
                case "YARD":      result = meters / 0.9144;  break;
                default:          result = meters / 0.3048;  break; // FOOT
            }
        } else { // bothWeight
            double kg = fromUnit.equals("KILOGRAM") ? value : value * 0.453592;
            result = toUnit.equals("KILOGRAM") ? kg : kg / 0.453592;
        }

        responseObserver.onNext(response.setIsSuccess(true).setResult(result).build());
        responseObserver.onCompleted();
    }
}
