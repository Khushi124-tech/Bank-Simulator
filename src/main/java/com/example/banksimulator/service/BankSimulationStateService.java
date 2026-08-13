package com.example.banksimulator.service;

import com.example.banksimulator.model.BankTransactionState;
import org.springframework.stereotype.Service;

@Service
public class BankSimulationStateService {

    private BankTransactionState pendingTransaction;

    public synchronized void setPendingTransaction(
            BankTransactionState transaction) {

        this.pendingTransaction = transaction;
    }

    public synchronized BankTransactionState getPendingTransaction() {

        return pendingTransaction;
    }

    public synchronized boolean hasPendingTransaction() {

        return pendingTransaction != null;
    }

    public synchronized void clear() {

        this.pendingTransaction = null;
    }

}
