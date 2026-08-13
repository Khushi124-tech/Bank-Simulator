document.addEventListener("DOMContentLoaded", async function () {
        const processButton =
            document.getElementById("processPaymentButton");

        await loadPendingTransaction();

        if (processButton) {
                processButton.addEventListener("click", processPayment);
        }
});

/* =========================================================
 * LOAD PENDING TRANSACTION
 * ========================================================= */

async function loadPendingTransaction() {
        try {
                const response = await fetch("/bank/pending");

                if (!response.ok) {
                        throw new Error(
                            `Unable to load transaction. HTTP ${response.status}`
                        );
                }

                const transaction = await response.json();

                if (!transaction) {
                        showWaitingState();
                        return;
                }

                displayTransaction(transaction);

        } catch (error) {
                console.error("Bank transaction loading error:", error);

                const validationStatus =
                    document.getElementById("validationStatus");

                const processButton =
                    document.getElementById("processPaymentButton");

                if (validationStatus) {
                        validationStatus.textContent = "Unable to load transaction";
                        validationStatus.className = "validation-error";
                }

                if (processButton) {
                        processButton.disabled = true;
                }
        }
}

/* =========================================================
 * DISPLAY PENDING TRANSACTION
 * ========================================================= */

function displayTransaction(transaction) {
        document.getElementById("pid").textContent =
            transaction.pid || "—";

        document.getElementById("clientCode").textContent =
            transaction.request?.fldClientCode || "—";

        document.getElementById("merchantCode").textContent =
            transaction.request?.fldMerchCode || "—";

        document.getElementById("accountNumber").textContent =
            transaction.request?.fldClientAcctNo || "—";

        document.getElementById("currency").textContent =
            transaction.request?.fldTxnCurr || "—";

        const transactionAmount = Number(
            transaction.request?.fldTxnAmt || 0
        );

        const serviceAmount = Number(
            transaction.request?.fldTxnScAmt || 0
        );

        const totalDebit = transactionAmount + serviceAmount;

        document.getElementById("transactionAmount").textContent =
            formatAmount(transactionAmount);

        document.getElementById("serviceAmount").textContent =
            formatAmount(serviceAmount);

        document.getElementById("totalDebit").textContent =
            formatAmount(totalDebit);

        document.getElementById("merchantReference").textContent =
            transaction.request?.fldMerchRefNbr || "—";

        document.getElementById("transactionDateTime").textContent =
            transaction.request?.fldDatTimeTxn || "—";

        const validationStatus =
            document.getElementById("validationStatus");

        if (transaction.checksumValid === true) {
                validationStatus.textContent = "Checksum verified";
                validationStatus.className = "validation-success";
        } else {
                validationStatus.textContent = "Checksum verification failed";
                validationStatus.className = "validation-error";
        }

        document.getElementById("processPaymentButton").disabled = false;
}

/* =========================================================
 * PROCESS PAYMENT
 * ========================================================= */

async function processPayment() {
        const button =
            document.getElementById("processPaymentButton");

        const scenario =
            document.getElementById("scenario").value;

        const result =
            document.getElementById("paymentResult");

        button.disabled = true;
        button.textContent = "PROCESSING...";
        result.innerHTML = "";

        try {
                const response = await fetch(
                    `/bank/payment/process?scenario=${encodeURIComponent(scenario)}`,
                    {
                            method: "POST"
                    }
                );

                const data = await response.json();

                if (!response.ok) {
                        throw new Error(
                            data.message || `HTTP ${response.status}`
                        );
                }

                /*
                 * The backend is the source of truth.
                 * A successful final result requires BOTH:
                 *
                 * flgSuccess = S
                 * fldVerify  = V
                 */
                const verifiedSuccess =
                    data.flgSuccess === "S" &&
                    data.fldVerify === "V";

                if (verifiedSuccess) {
                        showProcessingState();

                        /* UI-only 3 second verification animation. */
                        setTimeout(() => {
                                redirectToBillDesk(data);
                        }, 3000);

                } else {
                        displayPaymentResult(data);
                        button.disabled = false;
                        button.textContent = "PROCESS PAYMENT";
                }

        } catch (error) {
                console.error("Payment processing error:", error);

                result.hidden = false;
                result.innerHTML = `
            <div class="payment-processing-error">
                <h3>Payment Processing Error</h3>
                <p>${escapeHtml(error.message)}</p>
            </div>
        `;

                button.disabled = false;
                button.textContent = "PROCESS PAYMENT";
        }
}

/* =========================================================
 * PROCESSING STATE
 * ========================================================= */

function showProcessingState() {
        const paymentResult =
            document.getElementById("paymentResult");

        paymentResult.hidden = false;

        paymentResult.innerHTML = `
        <div class="payment-processing">
            <h3>Processing Payment...</h3>
            <p>
                Please wait while we complete payment verification.
            </p>
        </div>
    `;
}

/* =========================================================
 * REDIRECT TO BILLDESK
 * ========================================================= */

function redirectToBillDesk(result) {
        /*
         * =====================================================
         * RETURN TO BILLDESK
         * =====================================================
         *
         * The Bank Simulator and BillDesk simulator run from the
         * same application/host in this project.
         *
         * We therefore redirect the current Bank Simulator window
         * directly to the BillDesk root page instead of depending
         * on window.opener.
         *
         * This is intentionally a simple simulator representation
         * of the bank returning the verified result to BillDesk.
         */

        const returnUrl = new URL("/", window.location.origin);

        returnUrl.search = new URLSearchParams({
                status: "SUCCESS",
                fldTxnAmt: result.fldTxnAmt ?? "",
                fldTxnScAmt: result.fldTxnScAmt ?? "",
                fldMerchRefNbr: result.fldMerchRefNbr ?? "",
                bankRefNo: result.bankRefNo ?? "",
                fldVerify: result.fldVerify ?? "V"
        }).toString();

        console.log(
            "Returning verified payment to BillDesk:",
            returnUrl.toString()
        );

        /*
         * The current Bank Simulator window becomes the BillDesk
         * result page. BillDesk's app.js reads the query parameters
         * and displays the final success popup.
         */
        window.location.assign(returnUrl.toString());
}

/* =========================================================
 * DISPLAY PAYMENT RESULT FOR FAILURE
 * ========================================================= */

function displayPaymentResult(result) {
        const success =
            result.flgSuccess === "S" &&
            result.fldVerify === "V";

        const paymentResult =
            document.getElementById("paymentResult");

        paymentResult.hidden = false;

        if (!success) {
                /*
                 * =====================================================
                 * FAILURE RESULT
                 * =====================================================
                 *
                 * A failed transaction never debits the account, so
                 * we deliberately do NOT repeat financial details
                 * here (Transaction Amount / Service Amount /
                 * Total Debit / Bank Reference). Those already remain
                 * visible in the "Incoming Payment" section above.
                 *
                 * We just show a clear Status + Reason.
                 */
                paymentResult.innerHTML = `
            <div class="payment-error">
                <h3>Payment Failed</h3>

                <p>
                    <span>Status</span>
                    FAILED
                </p>

                <p>
                    <span>Reason</span>
                    ${escapeHtml(result.message || "N/A")}
                </p>
            </div>
        `;

                return;
        }

        /*
         * =========================================================
         * SUCCESS RESULT (fallback display)
         * =========================================================
         *
         * In the normal flow, a verified success is intercepted by
         * processPayment() before this function is called, and the
         * user is redirected to BillDesk instead. This branch is a
         * fallback in case that redirect doesn't happen.
         */
        const reference = result.bankRefNo || "N/A";
        const transactionAmount = Number(result.fldTxnAmt || 0);
        const serviceAmount = Number(result.fldTxnScAmt || 0);
        const totalDebit = transactionAmount + serviceAmount;

        paymentResult.innerHTML = `
        <div class="payment-success">
            <h3>Payment Successful</h3>

            <p>
                <span>Status</span>
                SUCCESS
            </p>

            <p>
                <span>Dual Verification</span>
                VERIFIED
            </p>

            <p>
                <span>Bank Reference</span>
                ${escapeHtml(reference)}
            </p>

            <p>
                <span>Transaction Amount</span>
                ₹${formatAmount(transactionAmount)}
            </p>

            <p>
                <span>Service Amount</span>
                ₹${formatAmount(serviceAmount)}
            </p>

            <p class="result-total">
                <span>Total Debit</span>
                ₹${formatAmount(totalDebit)}
            </p>
        </div>
    `;
}

function showWaitingState() {
        const validationStatus =
            document.getElementById("validationStatus");

        validationStatus.textContent =
            "Waiting for transaction";

        validationStatus.className = "";

        document.getElementById(
            "processPaymentButton"
        ).disabled = true;
}

function formatAmount(amount) {
        if (
            amount === null ||
            amount === undefined ||
            Number.isNaN(Number(amount))
        ) {
                return "0.00";
        }

        return Number(amount).toLocaleString("en-IN", {
                minimumFractionDigits: 2,
                maximumFractionDigits: 2
        });
}

function escapeHtml(value) {
        const div = document.createElement("div");
        div.textContent = String(value ?? "");
        return div.innerHTML;
}