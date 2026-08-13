package com.example.banksimulator.service;

import com.example.banksimulator.model.PaymentRequest;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    private final ValidationService validationService;
    private final EpiTransactionBuilder transactionBuilder;
    private final ChecksumService checksumService;
    private final EncryptionService encryptionService;

    public PaymentService(
            ValidationService validationService,
            EpiTransactionBuilder transactionBuilder,
            ChecksumService checksumService,
            EncryptionService encryptionService) {

        this.validationService = validationService;
        this.transactionBuilder = transactionBuilder;
        this.checksumService = checksumService;
        this.encryptionService = encryptionService;
    }

    public String preparePayment(
            PaymentRequest request,
            String encryptionKey) {

        // Step 1: Validate Gateway input
        validationService.validatePaymentRequest(request);

        String rawTransaction =
                transactionBuilder.buildRawTransaction(request);

        String checksum =
                checksumService.generateChecksum(
                        rawTransaction
                );

        String transactionWithChecksum =
                rawTransaction + "&CHECKSUM="+ checksum;

        return encryptionService.encrypt(
                transactionWithChecksum,
                encryptionKey
        );
    }
}