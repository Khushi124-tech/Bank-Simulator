# EPI Simulator Specification

## Components

- Merchant
- Payment Gateway
- Bank Simulator

## Payment Flow

Merchant
↓
Payment Gateway
↓
Bank Simulator
↓
Payment Gateway
↓
Merchant

## URLs

Payment URL:
Return URL:
Dual Verification URL:
S2S Confirmation URL:

## Payment Request

Parameters:
- 

## Payment Response

Parameters:
- 

## Encryption

Key derivation:
Cipher:
Mode:
Padding:
IV:
Encoding:

## Checksum

Algorithm:

## Validation

Merchant validation:
Customer validation:
Transaction validation:

## Transaction Statuses

Success:
Failure:
Pending:

## Authentication

Auto-auth:
Checker-maker:

## Dual Verification

Request:
Response:

## Payment Confirmation S2S

Request:
Response:

## Notes
## Architecture

                    CUSTOMER
                       │
                       │ clicks PAY
                       ▼
                ┌───────────────┐
                │    MERCHANT   │
                └───────┬───────┘
                        │
                        │ initiate payment
                        ▼
                ┌───────────────┐
                │    GATEWAY    │
                │validate merchant  │
                │ identify bank │
                │ get PID       │
                │ prepare data  │
                └───────┬───────┘
                        │
                        │ bankPaymentUrl
                        │ PID + encdata
                        ▼
                ┌───────────────┐
                │    MERCHANT   │
                │               │
                │ submit form  │
                └───────┬───────┘
                        │
                        │ POST PID + encdata
                        ▼
                ┌───────────────┐
                │ BANK SIMULATOR│
                │               │
                │ decrypt       │
                │ validate      │
                │ authenticate  │
                │ account       │
                │ process       │
                └───────┬───────┘
                        │
                        │ encrypted response
                        ▼
                ┌───────────────┐
                │    MERCHANT   │
                │               │
                │ decrypt       │
                │ display result│
                └───────────────┘



## Dual Verification

Payment Gateway
│
│ HTTPS S2S (+BankRefNo. + Flag = V)
▼
Bank Simulator
│ Decrypts and verify
│ Verification Response in encryption
▼
Payment Gateway


## Payment Confirmation

Bank Simulator
│
│ HTTPS S2S
▼
Merchant