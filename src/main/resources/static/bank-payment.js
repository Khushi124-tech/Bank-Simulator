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
                        setTimeout(async () => {
                                await sendS2SCallback(data);
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
 * S2S CALLBACK TO GATEWAY
 * =========================================================
 *
 * Before returning the browser to BillDesk, the Bank posts its
 * encrypted, checksummed result to the Gateway's real S2S
 * callback endpoint - the same /payment/result the Gateway
 * would receive directly from the bank's servers in a real
 * integration.
 *
 * This is a real network request: open DevTools -> Network ->
 * click "result" -> "Payload" tab to see the actual encdata
 * body being sent, and "Response" to see what the Gateway did
 * with it (it decrypts, verifies the checksum, parses the EPI
 * response, and logs it server-side - check the Spring Boot
 * console for the "[GATEWAY] S2S callback received" trace).
 */
async function sendS2SCallback(data) {
        if (!data.encdata) {
                console.warn(
                    "No encdata on response - skipping S2S callback"
                );
                return;
        }

        try {
                const response = await fetch("/payment/result", {
                        method: "POST",
                        headers: {
                                "Content-Type":
                                    "application/x-www-form-urlencoded"
                        },
                        body: new URLSearchParams({
                                encdata: data.encdata
                        }).toString()
                });

                const text = await response.text();

                console.log(
                    "S2S callback to /payment/result:",
                    response.status,
                    text
                );

        } catch (error) {
                console.error(
                    "S2S callback failed:",
                    error
                );
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
         * A real bank redirect sends the customer's browser back
         * to the SAME tab/window that started the checkout - not
         * a brand new one. This tab (the Bank Simulator) was
         * opened via window.open() from the BillDesk page, so
         * window.opener still points back at that original tab.
         *
         * Preferred:  navigate window.opener (the original
         *             BillDesk tab) to the result, then close
         *             this Bank Simulator tab - the customer ends
         *             up back where they started, exactly like a
         *             real redirect.
         *
         * Fallback:   if window.opener isn't available (blocked,
         *             already closed, or the Bank Simulator was
         *             opened directly rather than via Pay), this
         *             tab becomes the result page instead so the
         *             flow still completes.
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

        if (window.opener && !window.opener.closed) {

                window.opener.location.href = returnUrl.toString();
                window.opener.focus();

                showReturnedToBillDeskState();

                /*
                 * Give the opener a moment to start navigating
                 * before closing this tab.
                 */
                setTimeout(() => {
                        window.close();
                }, 400);

                return;
        }

        /*
         * No opener available - this tab becomes the BillDesk
         * result page itself.
         */
        window.location.assign(returnUrl.toString());
}

function showReturnedToBillDeskState() {
        const paymentResult =
            document.getElementById("paymentResult");

        paymentResult.hidden = false;

        paymentResult.innerHTML = `
        <div class="payment-success">
            <h3>Payment Successful</h3>
            <p>
                Returning to BillDesk. This tab will close
                automatically - if it doesn't, you can close it
                yourself.
            </p>
        </div>
    `;
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