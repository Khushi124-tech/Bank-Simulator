package com.example.banksimulator.controller.bank;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class BankSimulatorPageController {

    @GetMapping("/bank/simulator")
    public String bankSimulatorPage() {
        return "forward:/bank-payment.html";
    }
}
