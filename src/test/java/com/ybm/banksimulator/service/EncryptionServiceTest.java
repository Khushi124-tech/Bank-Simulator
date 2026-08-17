package com.ybm.banksimulator.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EncryptionServiceTest {

    private final EncryptionService encryptionService =
            new EncryptionService();

    private static final String KEY = "BILL#1234";

    @Test
    void shouldEncryptAndDecryptSuccessfully() {

        String plainText =
                "fldClientCode=101&fldMerchCode=BILLDESK&fldTxnAmt=100";

        String encrypted =
                encryptionService.encrypt(
                        plainText,
                        KEY
                );

        String decrypted =
                encryptionService.decrypt(
                        encrypted,
                        KEY
                );

        assertNotNull(encrypted);
        assertFalse(encrypted.isBlank());
        assertEquals(plainText, decrypted);
    }

    @Test
    void shouldProduceSameEncryptedValueForSameInput() {

        String plainText =
                "fldClientCode=101&fldMerchCode=BILLDESK";

        String encrypted1 =
                encryptionService.encrypt(
                        plainText,
                        KEY
                );

        String encrypted2 =
                encryptionService.encrypt(
                        plainText,
                        KEY
                );

        assertEquals(encrypted1, encrypted2);
    }

    @Test
    void shouldFailWithWrongDecryptionKey() {

        String plainText =
                "fldClientCode=101&fldMerchCode=BILLDESK";

        String encrypted =
                encryptionService.encrypt(
                        plainText,
                        KEY
                );

        assertThrows(
                IllegalStateException.class,
                () -> encryptionService.decrypt(
                        encrypted,
                        "WRONG_KEY"
                )
        );
    }

    @Test
    void shouldRejectNullPlainText() {

        assertThrows(
                IllegalArgumentException.class,
                () -> encryptionService.encrypt(
                        null,
                        KEY
                )
        );
    }

    @Test
    void shouldRejectBlankEncryptedText() {

        assertThrows(
                IllegalArgumentException.class,
                () -> encryptionService.decrypt(
                        "   ",
                        KEY
                )
        );
    }

    @Test
    void shouldRejectNullEncryptionKey() {

        assertThrows(
                IllegalArgumentException.class,
                () -> encryptionService.encrypt(
                        "test",
                        null
                )
        );
    }

    @Test
    void shouldRejectBlankEncryptionKey() {

        assertThrows(
                IllegalArgumentException.class,
                () -> encryptionService.encrypt(
                        "test",
                        "   "
                )
        );
    }
}