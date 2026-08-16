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
                rawTransaction + "&CHECKSUM=" + checksum;

        String encdata =
                encryptionService.encrypt(
                        transactionWithChecksum,
                        encryptionKey
                );

        /*
         * =====================================================
         * GATEWAY CONSOLE LOG (EDUCATIONAL / DEBUG TRACE)
         * =====================================================
         *
         * Mirrors what a real BillDesk-style gateway integration
         * would log at this point: the raw EPI string before it's
         * touched, the checksum computed over it, and the final
         * encrypted payload. Nothing sensitive is masked here
         * because this is a simulator, not production code.
         */
        System.out.println(
                "========================================"
        );
        System.out.println(
                "[GATEWAY] Preparing outbound transaction"
        );
        System.out.println(
                "[GATEWAY] Raw transaction (EPI string):"
        );
        System.out.println(
                rawTransaction
        );
        System.out.println(
                "[GATEWAY] Checksum (MD5): " + checksum
        );
        System.out.println(
                "[GATEWAY] Raw + Checksum:"
        );
        System.out.println(
                transactionWithChecksum
        );
        System.out.println(
                "[GATEWAY] Encrypted (encdata):"
        );
        System.out.println(
                encdata
        );
        System.out.println(
                "========================================"
        );

        return encdata;
    }
}