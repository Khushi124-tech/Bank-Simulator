package com.example.banksimulator.model;

public class DualVerificationResult {

    private boolean verified;
    private String message;

    public DualVerificationResult() {
    }

    public DualVerificationResult(
            boolean verified,
            String message) {

        this.verified = verified;
        this.message = message;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}