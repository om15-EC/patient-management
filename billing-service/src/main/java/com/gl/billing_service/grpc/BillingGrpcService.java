package com.gl.billing_service.grpc;

import billing.BillingRequest;
import billing.BillingResponse;
import billing.BillingServiceGrpc;
import com.gl.billing_service.model.BillingAccount;
import com.gl.billing_service.repository.BillingAccountRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

@GrpcService
@RequiredArgsConstructor
public class BillingGrpcService extends BillingServiceGrpc.BillingServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(BillingGrpcService.class);

    private final BillingAccountRepository billingAccountRepository;

    @Override
    public void createBillingAccount(BillingRequest request,
                                     StreamObserver<BillingResponse> responseObserver) {
        log.info("createBillingAccount request received {}" , request.toString());

        if(billingAccountRepository.existsByPatientId(request.getPatientId())){
            log.warn("Billing account is already exists for patientIdL {}", request.getPatientId());
            responseObserver.onError(
                    Status.ALREADY_EXISTS
                            .withDescription("Billing account already exists for patient: " + request.getPatientId())
                            .asRuntimeException()
            );
            return;
        }

        BillingAccount account = BillingAccount.builder()
                .patientId(request.getPatientId())
                .status("ACTIVE")
                .billingAmount(BigDecimal.ZERO)
                .build();

        BillingAccount savedAccount = billingAccountRepository.save(account);
        log.info("Billing account created with id: {}", savedAccount.getId());

        BillingResponse response = BillingResponse.newBuilder()
                .setAccountId(savedAccount.getId().toString())
                .setStatus(savedAccount.getStatus())
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

}
