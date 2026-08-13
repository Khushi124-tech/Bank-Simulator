/*
 * =========================================================
 * PAYMENT PAGE
 * =========================================================
 */


/*
 * =========================================================
 * FIXED DEMO CONFIGURATION
 * =========================================================
 *
 * These values are gateway configuration values.
 * They are not entered by the user.
 */

const PAYMENT_CONFIG = {

    clientCode: "Amazon",

    merchantCode: "Merch1",

    merchantReference: "A123401",

    currency: "INR",

    subMerchant: "SUB123",

    clientAccount: "22222",

    bankID:"YBM",

    returnUrl: "https://www.billdesk.com"
};


/*
 * =========================================================
 * DOM ELEMENTS
 * =========================================================
 */

const paymentForm =
    document.getElementById("paymentForm");

const paymentResult =
    document.getElementById("paymentResult");

const payButton =
    document.getElementById("payButton");

const transactionAmountInput =
    document.getElementById("fldTxnAmt");

const serviceAmountInput =
    document.getElementById("fldTxnScAmt");


/*
 * =========================================================
 * PAYMENT SUBMISSION
 * =========================================================
 */

paymentForm.addEventListener(
    "submit",
    async function (event) {

        /*
         * IMPORTANT
         *
         * Prevent normal HTML form submission.
         *
         * Without this, the browser would navigate to:
         *
         * /?fldClientCode=...
         *
         * instead of calling our Spring Boot API.
         */
        event.preventDefault();


        /*
         * Disable button while processing.
         */
        payButton.disabled = true;

        payButton.textContent =
            "Sending to Bank...";


        /*
         * Clear previous result.
         */
        paymentResult.innerHTML = "";

        paymentResult.hidden = true;


        /*
         * =================================================
         * OPEN BANK SIMULATOR
         * =================================================
         *
         * Open immediately inside the user's click event.
         *
         * This prevents the browser from blocking the
         * popup because of an asynchronous fetch().
         */

        const bankWindow =
            window.open(
                "about:blank",
                "_blank"
            );


        if (!bankWindow) {

            showPaymentError(
                "Unable to open Bank Simulator",
                "Please allow pop-ups for this site and try again."
            );

            payButton.disabled = false;

            payButton.textContent =
                "Pay";

            return;
        }


        /*
         * =================================================
         * VALIDATE USER INPUT
         * =================================================
         */

        const transactionAmount =
            Number(
                transactionAmountInput.value
            );

        const serviceAmount =
            Number(
                serviceAmountInput.value
            );


        if (
            !Number.isFinite(transactionAmount) ||
            transactionAmount <= 0
        ) {

            bankWindow.close();

            showPaymentError(
                "Invalid Transaction Amount",
                "Transaction amount must be greater than zero."
            );

            payButton.disabled = false;

            payButton.textContent =
                "Pay";

            return;
        }


        if (
            !Number.isFinite(serviceAmount) ||
            serviceAmount < 0
        ) {

            bankWindow.close();

            showPaymentError(
                "Invalid Service Amount",
                "Service amount cannot be negative."
            );

            payButton.disabled = false;

            payButton.textContent =
                "Pay";

            return;
        }


        /*
         * =================================================
         * BUILD PAYMENT REQUEST
         * =================================================
         *
         * Fixed gateway configuration comes from
         * PAYMENT_CONFIG.
         *
         * Only transaction/service amounts come from
         * the user.
         */

        const paymentRequest = {

            /*
             * Fixed gateway configuration
             */

            fldClientCode:
            PAYMENT_CONFIG.clientCode,

            fldMerchCode:
            PAYMENT_CONFIG.merchantCode,

            fldTxnCurr:
            PAYMENT_CONFIG.currency,


            /*
             * User-entered amounts
             */

            fldTxnAmt:
            transactionAmount,

            fldTxnScAmt:
            serviceAmount,


            /*
             * Fixed merchant reference
             */

            fldMerchRefNbr:
            PAYMENT_CONFIG.merchantReference,


            /*
             * Generated transaction date/time
             */

            fldDatTimeTxn:
                getCurrentDateTime(),


            /*
             * EPI fields
             */

            fldRef1: "",

            fldRef2:
            PAYMENT_CONFIG.subMerchant,

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


            /*
             * Return URL
             *
             * IMPORTANT:
             * This was the cause of your previous
             * "RU is mandatory" HTTP 500 error.
             */

            ru:
            PAYMENT_CONFIG.returnUrl,


            /*
             * Fixed demo customer account
             */

            fldClientAcctNo:
            PAYMENT_CONFIG.clientAccount,

            bankid:
            PAYMENT_CONFIG.bankId,
        };


        /*
         * Log request in browser console.
         */
        console.log(
            "Sending payment request:",
            paymentRequest
        );


        /*
         * =================================================
         * SEND TO SPRING BOOT GATEWAY
         * =================================================
         */

        try {

            const response =
                await fetch(
                    "/gateway/payment",
                    {
                        method: "POST",

                        headers: {
                            "Content-Type":
                                "application/json"
                        },

                        body:
                            JSON.stringify(
                                paymentRequest
                            )
                    }
                );


            /*
             * =================================================
             * HANDLE HTTP ERROR
             * =================================================
             */

            if (!response.ok) {

                /*
                 * Try to read the server's actual error.
                 *
                 * This is much more useful than simply
                 * displaying "HTTP 500".
                 */

                let serverMessage =
                    `HTTP ${response.status}`;

                try {

                    const errorBody =
                        await response.text();

                    if (errorBody) {

                        serverMessage =
                            errorBody;
                    }

                } catch (readError) {

                    console.warn(
                        "Unable to read server error:",
                        readError
                    );
                }


                throw new Error(
                    `Unable to send payment to bank. ${serverMessage}`
                );
            }


            /*
             * =================================================
             * READ GATEWAY RESPONSE
             * =================================================
             */

            const result =
                await response.json();


            console.log(
                "Gateway response:",
                result
            );


            /*
             * =================================================
             * NAVIGATE BANK SIMULATOR
             * =================================================
             *
             * The gateway has successfully delivered the
             * transaction to the Bank RECEIVE stage.
             */

            if (
                bankWindow &&
                !bankWindow.closed
            ) {

                bankWindow.location.href =
                    "/bank/simulator";
            }


            /*
             * =================================================
             * DISPLAY PENDING RESULT
             * =================================================
             *
             * The transaction has reached the bank,
             * but has NOT been approved yet.
             */

            displayPendingResult(result);


        } catch (error) {

            /*
             * =================================================
             * PAYMENT ERROR
             * =================================================
             */

            console.error(
                "Payment submission error:",
                error
            );


            /*
             * Close Bank Simulator if the gateway
             * submission failed.
             */

            if (
                bankWindow &&
                !bankWindow.closed
            ) {

                bankWindow.close();
            }


            showPaymentError(
                "Payment Submission Failed",
                error.message
            );

        } finally {

            /*
             * Re-enable Pay button.
             */

            payButton.disabled = false;

            payButton.textContent =
                "Pay";
        }
    }
);


/*
 * =========================================================
 * PENDING PAYMENT RESULT
 * =========================================================
 */

function displayPendingResult(result) {

    paymentResult.hidden = false;


    /*
     * Gateway currently returns the PaymentRequest
     * directly.
     *
     * But this also supports:
     *
     * {
     *     request: { ... }
     * }
     */

    const request =
        result.request || result;


    const amount =
        request.fldTxnAmt;


    const merchantReference =
        request.fldMerchRefNbr;


    paymentResult.innerHTML = `

        <div class="payment-pending">

            <h3>
                Payment Sent to Bank
            </h3>

            <p>
                The transaction has been received by
                the Bank Simulator and is awaiting
                processing.
            </p>


            <div class="pending-details">

                <p>
                    <strong>Merchant:</strong>
                    ${escapeHtml(
        request.fldMerchCode || "N/A"
    )}
                </p>


                <p>
                    <strong>Amount:</strong>
                    ₹${formatAmount(amount)}
                </p>


                <p>
                    <strong>Reference:</strong>
                    ${escapeHtml(
        merchantReference || "N/A"
    )}
                </p>


                <p>
                    <strong>Status:</strong>
                    Waiting for bank decision
                </p>

            </div>


            <p class="pending-message">

                The Bank Simulator tester must now
                select a test scenario and process
                the transaction.

            </p>

        </div>
    `;
}


/*
 * =========================================================
 * PAYMENT ERROR
 * =========================================================
 */

function showPaymentError(
    title,
    message
) {

    paymentResult.hidden = false;


    paymentResult.innerHTML = `

        <div class="payment-error">

            <h3>
                ${escapeHtml(title)}
            </h3>

            <p>
                ${escapeHtml(message)}
            </p>

        </div>
    `;
}


/*
 * =========================================================
 * DATE / TIME
 * =========================================================
 */

function getCurrentDateTime() {

    const now =
        new Date();


    return now
        .toISOString()
        .slice(0, 19);
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
        amount === ""
    ) {

        return "0.00";
    }


    const numericAmount =
        Number(amount);


    if (
        Number.isNaN(numericAmount)
    ) {

        return "0.00";
    }


    return numericAmount.toLocaleString(
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