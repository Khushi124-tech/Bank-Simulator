package com.example.banksimulator.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChecksumServiceTest {

    private final ChecksumService checksumService =
            new ChecksumService();

    @Test
    void shouldGenerateMd5Checksum() {

        String input = "hello";

        String checksum =
                checksumService.generateChecksum(input);

        assertNotNull(checksum);
        assertEquals(32, checksum.length());
        assertEquals(
                "5d41402abc4b2a76b9719d911017c592",
                checksum
        );
    }

    @Test
    void shouldGenerateSameChecksumForSameInput() {

        String input =
                "fldClientCode=Amazon&fldMerchCode=Merch1";

        String checksum1 =
                checksumService.generateChecksum(input);

        String checksum2 =
                checksumService.generateChecksum(input);

        assertEquals(checksum1, checksum2);
    }

    @Test
    void shouldGenerateDifferentChecksumForDifferentInput() {

        String checksum1 =
                checksumService.generateChecksum("hello");

        String checksum2 =
                checksumService.generateChecksum("Hello");

        assertNotEquals(checksum1, checksum2);
    }

    @Test
    void shouldRejectNullInput() {

        assertThrows(
                IllegalArgumentException.class,
                () -> checksumService.generateChecksum(null)
        );
    }
}
