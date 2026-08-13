/* =========================================================
 * BILLDESK PAYMENT PAGE
 * ========================================================= */

const PAYMENT_CONFIG = {
    clientCode: "Amazon",
    merchantCode: "Merch1",
    merchantReference: "A123401",
    currency: "INR",
    subMerchant: "SUB123",
    clientAccount: "22222",
    bankId: "YBM",
    returnUrl: window.location.origin + "/"
};

const paymentForm = document.getElementById("paymentForm");
const paymentResult = document.getElementById("paymentResult");
const payButton = document.getElementById("payButton");
const transactionAmountInput = document.getElementById("fldTxnAmt");
const serviceAmountInput = document.getElementById("fldTxnScAmt");

paymentForm.addEventListener("submit", async function (event) {
    event.preventDefault();

    payButton.disabled = true;
    payButton.textContent = "Sending to Bank...";
    paymentResult.innerHTML = "";
    paymentResult.hidden = true;

    /*
     * Open the Bank Simulator synchronously from the click.
     * This preserves window.opener for the final redirect.
     */
    const bankWindow = window.open("about:blank", "_blank");

    if (!bankWindow) {
        showPaymentError(
            "Unable to open Bank Simulator",
            "Please allow pop-ups for this site and try again."
        );
        resetPayButton();
        return;
    }

    const transactionAmount = Number(transactionAmountInput.value);
    const serviceAmount = Number(serviceAmountInput.value);

    if (!Number.isFinite(transactionAmount) || transactionAmount <= 0) {
        bankWindow.close();
        showPaymentError(
            "Invalid Transaction Amount",
            "Transaction amount must be greater than zero."
        );
        resetPayButton();
        return;
    }

    if (!Number.isFinite(serviceAmount) || serviceAmount < 0) {
        bankWindow.close();
        showPaymentError(
            "Invalid Service Amount",
            "Service amount cannot be negative."
        );
        resetPayButton();
        return;
    }

    /* Bank-side business rule: service amount cannot exceed transaction amount. */
    if (serviceAmount > transactionAmount) {
        bankWindow.close();
        showPaymentError(
            "Invalid Service Amount",
            "Service amount cannot be greater than the transaction amount."
        );
        resetPayButton();
        return;
    }

    const paymentRequest = {
        fldClientCode: PAYMENT_CONFIG.clientCode,
        fldMerchCode: PAYMENT_CONFIG.merchantCode,
        fldTxnCurr: PAYMENT_CONFIG.currency,
        fldTxnAmt: transactionAmount,
        fldTxnScAmt: serviceAmount,
        fldMerchRefNbr: PAYMENT_CONFIG.merchantReference,
        fldDatTimeTxn: getCurrentDateTime(),

        fldRef1: "",
        fldRef2: PAYMENT_CONFIG.subMerchant,
        fldRef3: "",
        fldRef4: "",
        fldRef5: "",
        fldRef6: "",
        fldRef7: "",
        fldRef8: "",
        fldRef9: "",
        fldRef10: "",
        fldRef11: "",
        fldDate1: "",
        fldDate2: "",

        ru: PAYMENT_CONFIG.returnUrl,
        fldClientAcctNo: PAYMENT_CONFIG.clientAccount,
        bankid: PAYMENT_CONFIG.bankId
    };

    console.log("Sending payment request:", paymentRequest);

    try {
        const response = await fetch("/gateway/payment", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(paymentRequest)
        });

        if (!response.ok) {
            let serverMessage = `HTTP ${response.status}`;

            try {
                const errorBody = await response.text();
                if (errorBody) {
                    serverMessage = errorBody;
                }
            } catch (ignored) {
                // Keep HTTP status if response body cannot be read.
            }

            throw new Error(
                `Unable to send payment to bank. ${serverMessage}`
            );
        }

        const result = await response.json();

        console.log("Gateway response:", result);

        /*
         * Gateway has delivered the transaction to Bank RECEIVE.
         * The bank page can now read /bank/pending.
         */
        if (bankWindow && !bankWindow.closed) {
            bankWindow.location.href = "/bank/simulator";
        }

        displayPendingResult(result);

    } catch (error) {
        console.error("Payment submission error:", error);

        if (bankWindow && !bankWindow.closed) {
            bankWindow.close();
        }

        showPaymentError(
            "Payment Submission Failed",
            error.message
        );
    } finally {
        resetPayButton();
    }
});

function displayPendingResult(result) {
    paymentResult.hidden = false;

    const request = result.request || result;

    paymentResult.innerHTML = `
        <div class="payment-pending">
            <h3>Payment Sent to Bank</h3>

            <p>
                The transaction has been received by the Bank Simulator
                and is awaiting processing.
            </p>

            <div class="pending-details">
                <p>
                    <strong>Merchant:</strong>
                    ${escapeHtml(request.fldMerchCode || "N/A")}
                </p>

                <p>
                    <strong>Amount:</strong>
                    ₹${formatAmount(request.fldTxnAmt)}
                </p>

                <p>
                    <strong>Reference:</strong>
                    ${escapeHtml(request.fldMerchRefNbr || "N/A")}
                </p>

                <p>
                    <strong>Status:</strong>
                    Waiting for bank decision
                </p>
            </div>

            <p class="pending-message">
                Select the required test scenario in the Bank Simulator.
            </p>
        </div>
    `;
}

function showPaymentError(title, message) {
    paymentResult.hidden = false;

    paymentResult.innerHTML = `
        <div class="payment-error">
            <h3>${escapeHtml(title)}</h3>
            <p>${escapeHtml(message)}</p>
        </div>
    `;
}

/* =========================================================
 * BILLDESK RETURN RESULT
 * ========================================================= */

function handleBankReturn() {
    const params = new URLSearchParams(window.location.search);

    const status = params.get("status");
    const fldVerify = params.get("fldVerify");

    if (status !== "SUCCESS" || fldVerify !== "V") {
        return;
    }

    showFinalSuccessPopup({
        transactionAmount: params.get("fldTxnAmt"),
        serviceAmount: params.get("fldTxnScAmt"),
        merchantReference: params.get("fldMerchRefNbr"),
        bankReference: params.get("bankRefNo")
    });

    window.history.replaceState(
        {},
        document.title,
        window.location.pathname
    );
}

document.addEventListener("DOMContentLoaded", handleBankReturn);

function showFinalSuccessPopup(data) {
    const existing = document.getElementById("finalSuccessModal");
    if (existing) {
        existing.remove();
    }

    const modal = document.createElement("div");
    modal.id = "finalSuccessModal";

    modal.innerHTML = `
        <div class="success-modal-overlay">
            <div class="success-modal">
                <div class="success-icon">✓</div>

                <h2>Payment Successful</h2>

                <p class="success-subtitle">
                    Your payment has been successfully processed and verified.
                </p>

                <div class="success-details">
                    <div>
                        <span>Transaction Amount</span>
                        <strong>₹${formatAmount(data.transactionAmount)}</strong>
                    </div>

                    <div>
                        <span>Service Amount</span>
                        <strong>₹${formatAmount(data.serviceAmount)}</strong>
                    </div>

                    <div>
                        <span>Merchant Reference</span>
                        <strong>${escapeHtml(data.merchantReference || "N/A")}</strong>
                    </div>

                    <div>
                        <span>Bank Reference</span>
                        <strong>${escapeHtml(data.bankReference || "N/A")}</strong>
                    </div>
                </div>

                <div class="verification-badge">
                    ✓ Dual Verification Completed
                </div>

                <button type="button" onclick="closeFinalSuccessPopup()">
                    Done
                </button>
            </div>
        </div>
    `;

    document.body.appendChild(modal);
}

function closeFinalSuccessPopup() {
    const modal = document.getElementById("finalSuccessModal");
    if (modal) {
        modal.remove();
    }
}

function getCurrentDateTime() {
    return new Date().toISOString().slice(0, 19);
}

function formatAmount(amount) {
    if (amount === null || amount === undefined || amount === "") {
        return "0.00";
    }

    const numericAmount = Number(amount);

    if (Number.isNaN(numericAmount)) {
        return "0.00";
    }

    return numericAmount.toLocaleString("en-IN", {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    });
}

function escapeHtml(value) {
    const div = document.createElement("div");
    div.textContent = String(value ?? "");
    return div.innerHTML;
}

function resetPayButton() {
    payButton.disabled = false;
    payButton.textContent = "Pay";
}
