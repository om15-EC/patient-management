//package com.gl.patient_service.grpc;
//
//import billing.BillingRequest;
//import billing.BillingResponse;
//import billing.BillingServiceGrpc;
//import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
//import io.grpc.ManagedChannel;
//import io.grpc.ManagedChannelBuilder;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//
//import java.util.concurrent.TimeUnit;
//
//@Slf4j
//@Service
//public class  BillingServiceGrpcClient {
//
//    private final BillingServiceGrpc.BillingServiceBlockingStub blockingStub;
//
//    public BillingServiceGrpcClient(
//            @Value("${billing.service.address:localhost}") String serverAddress,
//            @Value("${billing-service-grpc.port:9001}") int serverPort) {
//
//        log.info("Connecting to billing service grpc service at {} : {}" , serverAddress,serverPort);
//
//        ManagedChannel channel = ManagedChannelBuilder
//                .forAddress(serverAddress,serverPort)
//                .usePlaintext()
//                .build();
//
//        blockingStub = BillingServiceGrpc.newBlockingStub(channel);
//    }
//
//    @CircuitBreaker(name = "billingService", fallbackMethod = "createBillingAccountFallback")
//    public BillingResponse createBillingAccount(String patientId, String name, String email){
//        log.info("Calling billing service via gRPC for patientId: {}", patientId);
//        BillingRequest request = BillingRequest.newBuilder()
//                .setPatientId(patientId)
//                .setName(name)
//                .setEmail(email)
//                .build();
//        BillingResponse response = blockingStub
//                .withDeadlineAfter(3, TimeUnit.SECONDS)
//                        .createBillingAccount(request);
//        log.info("received response from billing service via gRPC: {}", response);
//        return response;
//    }
//
//    public BillingResponse createBillingAccountFallback(String patientId, String name, String email, Throwable t){
//        return BillingResponse.newBuilder()
//                .setAccountId("PENDING")
//                .setStatus("BILLING_PENDING")
//                .build();
//    }
//}
