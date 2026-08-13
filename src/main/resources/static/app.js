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

const summaryAmount =
    document.getElementById("summaryAmount");

const summaryServiceAmount =
    document.getElementById("summaryServiceAmount");


/*
 * =========================================================
 * AMOUNT SUMMARY
 * =========================================================
 */

function updatePaymentSummary() {

    const transactionAmount =
        transactionAmountInput.value;

    const serviceAmount =
        serviceAmountInput.value;

    summaryAmount.textContent =
        formatAmount(transactionAmount);

    summaryServiceAmount.textContent =
        formatAmount(serviceAmount);
}


transactionAmountInput.addEventListener(
    "input",
    updatePaymentSummary
);

serviceAmountInput.addEventListener(
    "input",
    updatePaymentSummary
);


/*
 * =========================================================
 * PAYMENT SUBMISSION
 * =========================================================
 */

paymentForm.addEventListener(
    "submit",
    async function (event) {

        event.preventDefault();

        payButton.disabled = true;

        payButton.textContent =
            "Sending to Bank...";

        paymentResult.innerHTML = "";

        paymentResult.hidden = true;


        /*
         * Build PaymentRequest from the values
         * entered on the BillDesk UI.
         */
        const paymentRequest = {

            fldClientCode:
            document.getElementById(
                "fldClientCode"
            ).value,

            fldMerchCode:
            document.getElementById(
                "fldMerchCode"
            ).value,

            fldTxnCurr:
            document.getElementById(
                "fldTxnCurr"
            ).value,

            fldTxnAmt:
                Number(
                    transactionAmountInput.value
                ),

            fldTxnScAmt:
                Number(
                    serviceAmountInput.value
                ),

            fldMerchRefNbr:
            document.getElementById(
                "fldMerchRefNbr"
            ).value,

            fldDatTimeTxn:
                getCurrentDateTime(),

            fldRef1: "",

            fldRef2:
            document.getElementById(
                "fldRef2"
            ).value,

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

            ru:
            document.getElementById(
                "ru"
            ).value,

            fldClientAcctNo:
            document.getElementById(
                "fldClientAcctNo"
            ).value
        };


        try {

            /*
             * =================================================
             * SEND PAYMENT TO GATEWAY
             * =================================================
             */

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
             * Read JSON regardless of HTTP status.
             */
            const result =
                await response.json();


            /*
             * =================================================
             * GATEWAY ERROR
             * =================================================
             */

            if (!response.ok) {

                console.error("Gateway response: ", result);
                throw new Error(
                    result.message ||
                    `Payment initiation failed. HTTP ${response.status}`
                );
            }


            /*
             * =================================================
             * BANK REDIRECTION
             * =================================================
             *
             * Gateway has now:
             *
             * 1. Built EPI
             * 2. Generated checksum
             * 3. Encrypted encdata
             * 4. Delivered transaction to Bank
             * 5. Stored it as PENDING
             *
             * Gateway response contains:
             *
             * bankUrl
             * pid
             * status
             * request
             *
             * The browser now navigates to the Bank.
             */

            if (!result.bankUrl) {

                throw new Error(
                    "Bank URL was not returned by Gateway"
                );
            }


            /*
             * Actual browser redirection.
             */
            window.location.href =
                result.bankUrl;


        } catch (error) {

            console.error(
                "Payment submission error:",
                error
            );


            paymentResult.hidden = false;

            paymentResult.innerHTML = `
                <div class="payment-error">

                    <h3>
                        Payment Submission Failed
                    </h3>

                    <p>
                        ${escapeHtml(
                error.message
            )}
                    </p>

                </div>
            `;


            payButton.disabled = false;

            payButton.textContent =
                "Pay";
        }
    }
);


/*
 * =========================================================
 * DATE / TIME
 * =========================================================
 *
 * Spring Boot LocalDateTime expects:
 *
 * yyyy-MM-ddTHH:mm:ss
 *
 * Example:
 * 2026-08-13T11:03:04
 */

function getCurrentDateTime() {

    const now = new Date();

    const year =
        now.getFullYear();

    const month =
        String(now.getMonth() + 1)
            .padStart(2, "0");

    const day =
        String(now.getDate())
            .padStart(2, "0");

    const hours =
        String(now.getHours())
            .padStart(2, "0");

    const minutes =
        String(now.getMinutes())
            .padStart(2, "0");

    const seconds =
        String(now.getSeconds())
            .padStart(2, "0");

    return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`;
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
        amount === "" ||
        Number.isNaN(Number(amount))
    ) {

        return "₹0.00";
    }


    return "₹" +
        Number(amount).toLocaleString(
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