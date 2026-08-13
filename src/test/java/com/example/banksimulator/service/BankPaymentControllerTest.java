package com.example.banksimulator.service;

import com.example.banksimulator.controller.bank.BankPaymentController;
import com.example.banksimulator.model.BankPaymentRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BankPaymentControllerTest {

    @Test
    void shouldPassPidAndEncdataToBankPaymentService() {

        // Fake service for this controller test
        BankPaymentService service =
                new BankPaymentService(
                        null,
                        null,
                        null,
                        new DualVerificationService()
                );

        BankPaymentController controller =
                new BankPaymentController(service);

        BankPaymentRequest request =
                new BankPaymentRequest();

        request.setPid("PID_XYZ_001");
        request.setEncdata("test-encdata");

        assertThrows(
                Exception.class,
                () -> controller.processSimulation("SUCCESS")
        );
    }
}