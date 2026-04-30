package example.grpcclient;

import io.grpc.stub.StreamObserver;
import service.*;

// ###### THE INTERESTING PART #####
// Implementing the Echo service
class EchoImpl extends EchoGrpc.EchoImplBase {
    
    // We only defined one service so we only overwrite one method, we just echo back the client message
    @Override
    public void parrot(ClientRequest req, StreamObserver<ServerResponse> responseObserver) {

        System.out.println("Received from client: " + req.getMessage());
        ServerResponse.Builder response = ServerResponse.newBuilder();
        if (req.getMessage().isEmpty()) {
            response.setIsSuccess(false).setError("No message provided");
        } else {
            response.setIsSuccess(true).setMessage(req.getMessage());
        }
        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }     
}