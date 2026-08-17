package com.ybm.banksimulator.controller.gateway;

import com.ybm.banksimulator.model.PaymentRequest;
import com.ybm.banksimulator.service.PaymentService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequestMapping("/gateway")
public class GatewayController {

    private static final String BANK_ID = "YBM";

    private static final String BANK_PID =
            "PID_XYZ_001";

    private static final String BANK_ENCRYPTION_KEY =
            "BILL#1234";

    /*
     * Fixed simulator return URL - where the BANK will eventually
     * send the browser back to, after it finishes processing.
     *
     * This is NOT entered by the customer.
     */
    private static final String RETURN_URL =
            "http://localhost:9090/";

    /*
     * =========================================================
     * BANK ROUTING TABLE
     * =========================================================
     *
     * A real gateway talks to many different banks, each with
     * its own switch/endpoint URL. The Gateway decides WHICH
     * bank to hand the transaction off to by looking up the
     * "bankid" that came in on the payment request - here it's
     * fixed to "YBM" (see above), but the lookup itself is
     * written the way a multi-bank gateway actually would be:
     * bankid -> bank's receive URL.
     *
     * This is the bank's REAL base URL - the customer's browser
     * is actually redirected here, with PID and encdata attached
     * as query parameters, matching how real banks (e.g. Yes
     * Bank's own externaltransfer/create endpoint) structure
     * this exact handoff:
     *
     *   https://<bank-host>/pay?PID=<pid>&encdata=<encrypted>
     */
    private static final Map<String, String> BANK_RECEIVE_URLS =
            Map.of(
                    BANK_ID, "http://localhost:9090/bank/receive"
            );

    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");


    private final PaymentService paymentService;


    public GatewayController(
            PaymentService paymentService) {

        this.paymentService =
                paymentService;
    }


    /*
     * =========================================================
     * POST /gateway/payment
     * =========================================================
     *
     * Called by a REAL HTML <form method="POST" action="/gateway
     * /payment"> submission from the browser - not by
     * JavaScript's fetch(). Because it's a genuine browser
     * navigation, every field below arrives as ordinary form
     * data, visible in DevTools -> Network -> "payment" ->
     * Payload, exactly the way a real checkout page posts to a
     * real gateway.
     *
     * The response is a genuine HTTP 302 redirect (not an HTML
     * page with a hidden auto-submitting form). The browser
     * itself performs the next navigation, via GET, straight to
     * the Bank's real URL, with PID and encdata attached as
     * query parameters - this is what makes the encrypted
     * payload visible directly in the address bar, the same way
     * it is in a real bank's redirect URL.
     */
    @PostMapping(path = "/payment")
    public ResponseEntity<Void> processPayment(
            @RequestParam String fldClientCode,
            @RequestParam String fldMerchCode,
            @RequestParam String fldTxnCurr,
            @RequestParam BigDecimal fldTxnAmt,
            @RequestParam BigDecimal fldTxnScAmt,
            @RequestParam String fldMerchRefNbr,
            @RequestParam String fldDatTimeTxn,
            @RequestParam(required = false, defaultValue = "") String fldRef2,
            @RequestParam String fldClientAcctNo) {

        /*
         * =====================================================
         * BUILD THE REQUEST SERVER-SIDE
         * =====================================================
         *
         * bankid and ru are deliberately NOT accepted from the
         * form at all - the Gateway supplies both itself, the
         * same way it did before. A customer's browser has no
         * way to influence which bank gets used or where the
         * bank redirects back to.
         */

        PaymentRequest request =
                new PaymentRequest();

        request.setFldClientCode(fldClientCode);
        request.setFldMerchCode(fldMerchCode);
        request.setFldTxnCurr(fldTxnCurr);
        request.setFldTxnAmt(fldTxnAmt);
        request.setFldTxnScAmt(fldTxnScAmt);
        request.setFldMerchRefNbr(fldMerchRefNbr);
        request.setFldDatTimeTxn(
                LocalDateTime.parse(fldDatTimeTxn, DATE_TIME_FORMAT)
        );
        request.setFldRef2(fldRef2);
        request.setFldClientAcctNo(fldClientAcctNo);

        request.setBankid(BANK_ID);
        request.setRu(RETURN_URL);


        /*
         * =====================================================
         * 1. CREATE ENCRYPTED TRANSACTION
         * =====================================================
         */

        String encdata =
                paymentService.preparePayment(
                        request,
                        BANK_ENCRYPTION_KEY
                );


        /*
         * =====================================================
         * 2. RESOLVE WHICH BANK TO HAND OFF TO
         * =====================================================
         */

        String bankReceiveUrl =
                BANK_RECEIVE_URLS.get(BANK_ID);

        if (bankReceiveUrl == null) {

            throw new IllegalStateException(
                    "No receive URL configured for bank: "
                            + BANK_ID
            );
        }


        /*
         * =====================================================
         * 3. BUILD THE REAL REDIRECT URL
         * =====================================================
         *
         * encdata is Base64 and can contain '+', '/' and '='.
         * Those characters are NOT safe to drop straight into a
         * query string - '+' would be read back as a space, '='
         * would be read as ending the value early. URLEncoder
         * percent-encodes them so the Bank gets back the exact
         * original string. This is standard practice for any
         * encrypted payload placed in a URL, not specific to
         * this project.
         */

        String encodedPid =
                URLEncoder.encode(
                        BANK_PID,
                        StandardCharsets.UTF_8
                );

        String encodedEncdata =
                URLEncoder.encode(
                        encdata,
                        StandardCharsets.UTF_8
                );

        String fullBankUrl =
                bankReceiveUrl
                        + "?PID=" + encodedPid
                        + "&encdata=" + encodedEncdata;

        System.out.println(
                "========================================"
        );
        System.out.println(
                "[GATEWAY] Redirecting browser (GET) to:"
        );
        System.out.println(
                fullBankUrl
        );
        System.out.println(
                "========================================"
        );


        /*
         * =====================================================
         * 4. REAL HTTP 302 REDIRECT
         * =====================================================
         *
         * No HTML page, no hidden form, no JavaScript submit().
         * The browser performs this navigation itself, and the
         * full URL - including PID and the encrypted payload -
         * is visible directly in the address bar, exactly like
         * a real bank redirect.
         */

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, fullBankUrl)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .build();
    }
}