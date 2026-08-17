/* =========================================================
 * BILLDESK PAYMENT PAGE
 * ========================================================= */

const paymentForm = document.getElementById("paymentForm");
const paymentResult = document.getElementById("paymentResult");
const payButton = document.getElementById("payButton");
const transactionAmountInput = document.getElementById("fldTxnAmt");
const serviceAmountInput = document.getElementById("fldTxnScAmt");
const merchantReferenceInput = document.getElementById("fldMerchRefNbr");
const dateTimeInput = document.getElementById("fldDatTimeTxn");

const BANK_WINDOW_NAME = "bankSimulatorWindow";

/*
 * =========================================================
 * ONE PAYMENT ATTEMPT PER PAGE LOAD
 * =========================================================
 *
 * Once a transaction has actually been submitted to the
 * Gateway, this page must not be able to send another one -
 * not via a second click, and not via the browser Back button
 * returning to this same loaded page. A genuinely new payment
 * requires a fresh page load, which is also what generates a
 * fresh transaction reference below.
 */
let transactionAlreadySent = false;

/*
 * =========================================================
 * TRANSACTION REFERENCE
 * =========================================================
 *
 * Generated fresh for every Pay click, not hardcoded. A static
 * reference would collide with the Bank's duplicate-transaction
 * guard on the second legitimate payment. Real gateways do the
 * same thing - the merchant's order/reference ID is unique per
 * attempt, never reused.
 */
function generateMerchantReference() {
    const timestamp = Date.now();
    const random = Math.floor(100 + Math.random() * 900);
    return `TXN${timestamp}${random}`;
}

/*
 * =========================================================
 * REAL FORM SUBMISSION - NOT fetch()
 * =========================================================
 *
 * This listener does NOT call event.preventDefault() on the
 * happy path. It only validates and fills in the last couple
 * of hidden fields; the actual send is the browser's own native
 * form submission straight to /gateway/payment - a real POST
 * you can inspect in DevTools -> Network, not a background
 * fetch() call.
 *
 * The form's target="bankSimulatorWindow" attribute sends the
 * response into the SAME tab we open below (matched by name),
 * so this BillDesk tab itself never navigates away.
 */
paymentForm.addEventListener("submit", function (event) {

    if (transactionAlreadySent) {
        event.preventDefault();

        showPaymentError(
            "Payment Already Sent",
            "A payment has already been sent to the bank from this " +
            "page. Reload the page to start a new payment."
        );
        return;
    }

    const transactionAmount = Number(transactionAmountInput.value);
    const serviceAmount = Number(serviceAmountInput.value);

    if (!Number.isFinite(transactionAmount) || transactionAmount <= 0) {
        event.preventDefault();
        showPaymentError(
            "Invalid Transaction Amount",
            "Transaction amount must be greater than zero."
        );
        return;
    }

    if (!Number.isFinite(serviceAmount) || serviceAmount < 0) {
        event.preventDefault();
        showPaymentError(
            "Invalid Service Amount",
            "Service amount cannot be negative."
        );
        return;
    }

    if (serviceAmount > transactionAmount) {
        event.preventDefault();
        showPaymentError(
            "Invalid Service Amount",
            "Service amount cannot be greater than the transaction amount."
        );
        return;
    }

    /*
     * Fill in the fields that must be fresh per attempt. This
     * runs synchronously before the browser's default submit
     * action, so the values are already in the form fields by
     * the time the real POST goes out.
     */
    const merchantReference = generateMerchantReference();
    merchantReferenceInput.value = merchantReference;
    dateTimeInput.value = getCurrentDateTime();

    console.log(
        "Submitting real form POST to /gateway/payment"
    );
    console.log(
        "Transaction reference generated:", merchantReference
    );
    console.log(
        "Open DevTools -> Network -> 'payment' -> Payload to see " +
        "the actual form fields being sent."
    );

    /*
     * Open (or reuse) the named window BEFORE the form submits,
     * synchronously, in direct response to this click - this is
     * what keeps popup blockers from stepping in. Its .opener
     * will point back at this tab, which is what lets the Bank
     * Simulator redirect the customer back here at the very end.
     */
    const bankWindow = window.open("about:blank", BANK_WINDOW_NAME);

    if (!bankWindow) {
        event.preventDefault();
        showPaymentError(
            "Unable to open Bank Simulator",
            "Please allow pop-ups for this site and try again."
        );
        return;
    }

    transactionAlreadySent = true;
    payButton.disabled = true;
    payButton.textContent = "Payment Sent — Reload To Pay Again";

    displayPendingResult({
        merchant: document.getElementById("fldMerchCode").value,
        amount: transactionAmount,
        reference: merchantReference
    });

    /*
     * No event.preventDefault() here - the browser now performs
     * its own real navigation of the "bankSimulatorWindow" tab
     * to /gateway/payment.
     */
});

function displayPendingResult(details) {
    paymentResult.hidden = false;

    paymentResult.innerHTML = `
        <div class="payment-pending">
            <h3>Payment Sent to Bank</h3>

            <p>
                The transaction has been submitted to the Gateway and
                is being handed off to the bank in the new tab.
            </p>

            <div class="pending-details">
                <p>
                    <strong>Merchant:</strong>
                    ${escapeHtml(details.merchant || "N/A")}
                </p>

                <p>
                    <strong>Amount:</strong>
                    ₹${formatAmount(details.amount)}
                </p>

                <p>
                    <strong>Reference:</strong>
                    ${escapeHtml(details.reference || "N/A")}
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
                <div class="success-icon">
                    <svg viewBox="0 0 66 66" class="success-icon-svg">
                        <circle
                            class="success-icon-ring"
                            cx="33" cy="33" r="29"
                            fill="none" stroke-width="3"/>
                        <path
                            class="success-icon-check"
                            fill="none" stroke-width="4"
                            stroke-linecap="round"
                            stroke-linejoin="round"
                            d="M20 34 L29 43 L47 23"/>
                    </svg>
                </div>

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
                        <span>Total Debit</span>
                        <strong>₹${formatAmount(
        (Number(data.transactionAmount) || 0) +
        (Number(data.serviceAmount) || 0)
    )}</strong>
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
                    <svg viewBox="0 0 16 16" class="verification-badge-icon">
                        <path
                            fill="none" stroke="currentColor"
                            stroke-width="2" stroke-linecap="round"
                            stroke-linejoin="round"
                            d="M3 8.5 L6.5 12 L13 4"/>
                    </svg>
                    Dual Verification Completed
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