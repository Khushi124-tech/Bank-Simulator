document.addEventListener("DOMContentLoaded", async function () {

        const processButton =
            document.getElementById("processPaymentButton");

        await loadPendingTransaction();

        processButton.addEventListener(
            "click",
            processPayment
        );
});


/*
 * =========================================================
 * LOAD PENDING TRANSACTION
 * =========================================================
 */

async function loadPendingTransaction() {

        try {

                const response =
                    await fetch("/bank/pending");

                if (!response.ok) {

                        throw new Error(
                            `Unable to load transaction. HTTP ${response.status}`
                        );
                }

                const transaction =
                    await response.json();

                if (!transaction) {

                        showWaitingState();

                        return;
                }

                displayTransaction(transaction);

        } catch (error) {

                console.error(
                    "Bank transaction loading error:",
                    error
                );

                document.getElementById(
                    "validationStatus"
                ).textContent =
                    "Unable to load transaction";

                document.getElementById(
                    "processPaymentButton"
                ).disabled = true;
        }
}


/*
 * =========================================================
 * DISPLAY PENDING TRANSACTION
 * =========================================================
 */

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

        const transactionAmount =
            Number(
                transaction.request?.fldTxnAmt || 0
            );

        const serviceAmount =
            Number(
                transaction.request?.fldTxnScAmt || 0
            );

        const totalDebit =
            transactionAmount + serviceAmount;

        document.getElementById(
            "transactionAmount"
        ).textContent =
            formatAmount(transactionAmount);

        document.getElementById(
            "serviceAmount"
        ).textContent =
            formatAmount(serviceAmount);

        document.getElementById(
            "totalDebit"
        ).textContent =
            formatAmount(totalDebit);

        document.getElementById(
            "merchantReference"
        ).textContent =
            transaction.request?.fldMerchRefNbr || "—";

        document.getElementById(
            "transactionDateTime"
        ).textContent =
            transaction.request?.fldDatTimeTxn || "—";


        /*
         * Checksum status
         */

        const validationStatus =
            document.getElementById(
                "validationStatus"
            );

        if (transaction.checksumValid === true) {

                validationStatus.textContent =
                    "Checksum verified";

                validationStatus.className =
                    "validation-success";

        } else {

                validationStatus.textContent =
                    "Checksum verification failed";

                validationStatus.className =
                    "validation-error";
        }


        /*
         * Enable payment processing
         */

        document.getElementById(
            "processPaymentButton"
        ).disabled = false;
}


/*
 * =========================================================
 * PROCESS PAYMENT
 * =========================================================
 */

async function processPayment() {

        const button =
            document.getElementById(
                "processPaymentButton"
            );

        const scenario =
            document.getElementById(
                "scenario"
            ).value;

        const result =
            document.getElementById(
                "paymentResult"
            );

        button.disabled = true;

        button.textContent =
            "PROCESSING...";

        result.innerHTML = "";


        try {

                const response =
                    await fetch(
                        `/bank/payment/process?scenario=${encodeURIComponent(
                            scenario
                        )}`,
                        {
                                method: "POST"
                        }
                    );

                const data =
                    await response.json();


                /*
                 * HTTP error = technical/API problem
                 *
                 * Business failures such as:
                 * INVALID_ACCOUNT
                 * INSUFFICIENT_FUNDS
                 * INVALID_PID
                 *
                 * should still return HTTP 200 with
                 * flgSuccess = "F".
                 */

                if (!response.ok) {

                        throw new Error(
                            data.message ||
                            `HTTP ${response.status}`
                        );
                }

                /*
 * Successful bank response.
 *
 * Backend has already completed:
 *
 * Bank processing
 *      ↓
 * Debit
 *      ↓
 * PaymentResponse
 *      ↓
 * Dual Verification
 *      ↓
 * Verified
 *
 * The delay below is UI-only.
 */

                if (data.flgSuccess === "S") {

                        showProcessingState();

                        setTimeout(() => {
                                displayPaymentResult(data);
                        }, 3000);

                } else {

                        displayPaymentResult(data);
                }

        } catch (error) {

                console.error(
                    "Payment processing error:",
                    error
                );

                result.innerHTML = `
            <div class="payment-processing-error">

                <h3>
                    Payment Processing Error
                </h3>

                <p>
                    ${escapeHtml(error.message)}
                </p>

            </div>
        `;

        } finally {

                button.disabled = false;

                button.textContent =
                    "PROCESS PAYMENT";
        }
}
/*
 * =========================================================
 * PROCESSING STATE
 * =========================================================
 */

function showProcessingState() {

        const paymentResult =
            document.getElementById(
                "paymentResult"
            );

        paymentResult.hidden = false;

        paymentResult.innerHTML = `
        <div class="payment-processing">

            <h3>
                Processing Payment...
            </h3>

            <p>
                Please wait while we verify your transaction.
            </p>

        </div>
    `;
}

/*
 * =========================================================
 * DISPLAY PAYMENT RESULT
 * =========================================================
 */

function displayPaymentResult(result) {

        const success =
            result.flgSuccess === "S";

        const reference =
            result.bankRefNo || "N/A";

        const transactionAmount =
            Number(result.fldTxnAmt || 0);

        const serviceAmount =
            Number(result.fldTxnScAmt || 0);

        const totalDebit =
            transactionAmount + serviceAmount;


        document.getElementById(
            "paymentResult"
        ).innerHTML = `

        <div class="${
            success
                ? "payment-success"
                : "payment-error"
        }">

            <h3>
                ${
            success
                ? "Payment Successful"
                : "Payment Failed"
        }
            </h3>

            <p>
                <span>Status</span>
                ${escapeHtml(
            result.flgSuccess || "N/A"
        )}
            </p>

            <p>
                <span>Message</span>
                ${escapeHtml(
            result.message || "N/A"
        )}
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


/*
 * =========================================================
 * WAITING STATE
 * =========================================================
 */

function showWaitingState() {

        document.getElementById(
            "validationStatus"
        ).textContent =
            "Waiting for transaction";

        document.getElementById(
            "validationStatus"
        ).className = "";

        document.getElementById(
            "processPaymentButton"
        ).disabled = true;
}


/*
 * =========================================================
 * FORMAT AMOUNT
 * =========================================================
 */

function formatAmount(amount) {

        if (
            amount === null ||
            amount === undefined ||
            Number.isNaN(Number(amount))
        ) {
                return "0.00";
        }

        return Number(amount).toLocaleString(
            "en-IN",
            {
                    minimumFractionDigits: 2,
                    maximumFractionDigits: 2
            }
        );
}


/*
 * =========================================================
 * HTML ESCAPING
 * =========================================================
 */

function escapeHtml(value) {

        const div =
            document.createElement("div");

        div.textContent =
            String(value ?? "");

        return div.innerHTML;
}