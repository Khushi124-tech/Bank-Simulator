package com.example.banksimulator.controller.gateway;

import com.example.banksimulator.model.BankPaymentRequest;
import com.example.banksimulator.model.PaymentRequest;
import com.example.banksimulator.service.BankPaymentService;
import com.example.banksimulator.service.PaymentService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/gateway")
public class GatewayController {

    private static final String BANK_ID = "YBM";

    private static final String BANK_PID =
            "PID_XYZ_001";

    private static final String BANK_ENCRYPTION_KEY =
            "BILL#1234";

    /*
     * Fixed simulator return URL.
     *
     * This is NOT entered by the customer.
     */
    private static final String RETURN_URL =
            "http://localhost:9090/";


    private final PaymentService paymentService;

    private final BankPaymentService bankPaymentService;


    public GatewayController(
            PaymentService paymentService,
            BankPaymentService bankPaymentService) {

        this.paymentService =
                paymentService;

        this.bankPaymentService =
                bankPaymentService;
    }


    @PostMapping("/payment")
    public Object processPayment(
            @RequestBody PaymentRequest request) {

        /*
         * =====================================================
         * FIXED GATEWAY CONFIGURATION
         * =====================================================
         *
         * The customer does not provide RU.
         *
         * The Gateway supplies it.
         */
        request.setBankid(BANK_ID);
        request.setRu(RETURN_URL);


        /*
         * =====================================================
         * 1. CREATE ENCRYPTED TRANSACTION
         * =====================================================
         */

        String encdata =
                paymentService.preparePayment(
                        request,
                        BANK_ENCRYPTION_KEY
                );


        /*
         * =====================================================
         * 2. CREATE BANK REQUEST
         * =====================================================
         */

        BankPaymentRequest bankRequest =
                new BankPaymentRequest();

        bankRequest.setPid(BANK_PID);

        bankRequest.setEncdata(encdata);


        /*
         * =====================================================
         * 3. SEND TO BANK RECEIVE STAGE
         * =====================================================
         *
         * This does NOT debit the account.
         *
         * It creates a pending transaction.
         */

        return bankPaymentService.receiveTransaction(
                bankRequest.getPid(),
                bankRequest.getEncdata()
        );
    }
}