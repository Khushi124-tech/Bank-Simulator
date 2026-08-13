package com.example.banksimulator.controller.gateway;

import com.example.banksimulator.model.PaymentResponse;
import com.example.banksimulator.service.ChecksumService;
import com.example.banksimulator.service.EncryptionService;
import com.example.banksimulator.service.EpiResponseParser;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
public class GatewayCallbackController {

    private static final String ENCRYPTION_KEY =
            "BILL#1234";

    private final EncryptionService encryptionService;
    private final ChecksumService checksumService;
    private final EpiResponseParser epiResponseParser;

    public GatewayCallbackController(
            EncryptionService encryptionService,
            ChecksumService checksumService,
            EpiResponseParser epiResponseParser) {

        this.encryptionService =
                encryptionService;

        this.checksumService =
                checksumService;

        this.epiResponseParser =
                epiResponseParser;
    }

    /*
     * =========================================================
     * BANK S2S CALLBACK
     * =========================================================
     *
     * Bank POSTs:
     *
     * encdata
     *
     * to:
     *
     * /payment/result
     */
    @PostMapping(
            value = "/payment/result",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    public String receiveCallback(
            @RequestParam("encdata") String encdata) {

        if (encdata == null || encdata.isBlank()) {
            throw new IllegalArgumentException(
                    "encdata cannot be empty"
            );
        }

        /*
         * =====================================================
         * 1. DECRYPT
         * =====================================================
         */

        String decryptedData =
                encryptionService.decrypt(
                        encdata,
                        ENCRYPTION_KEY
                );

        /*
         * =====================================================
         * 2. EXTRACT CHECKSUM
         * =====================================================
         */

        int checksumIndex =
                decryptedData.lastIndexOf(
                        "&CHECKSUM="
                );

        if (checksumIndex == -1) {
            throw new IllegalArgumentException(
                    "CHECKSUM not found"
            );
        }

        String rawResponse =
                decryptedData.substring(
                        0,
                        checksumIndex
                );

        String receivedChecksum =
                decryptedData.substring(
                        checksumIndex
                                + "&CHECKSUM=".length()
                );

        /*
         * =====================================================
         * 3. VERIFY CHECKSUM
         * =====================================================
         */

        String calculatedChecksum =
                checksumService.generateChecksum(
                        rawResponse
                );

        if (!calculatedChecksum.equalsIgnoreCase(
                receivedChecksum)) {

            throw new IllegalArgumentException(
                    "Checksum verification failed"
            );
        }

        /*
         * =====================================================
         * 4. PARSE EPI RESPONSE
         * =====================================================
         */

        PaymentResponse response =
                epiResponseParser.parse(
                        rawResponse
                );

        /*
         * =====================================================
         * 5. GATEWAY RECEIVED BANK RESULT
         * =====================================================
         */

        System.out.println(
                "S2S callback received"
        );

        System.out.println(
                "Merchant Reference: "
                        + response.getFldMerchRefNbr()
        );

        System.out.println(
                "Bank Reference: "
                        + response.getBankRefNo()
        );

        System.out.println(
                "Success: "
                        + response.getFlgSuccess()
        );

        /*
         * The gateway would normally persist/update the
         * transaction here.
         */

        return "OK";
    }
}