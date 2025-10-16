package com.nh.shorturl.util;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.UUID;

public class Base62 {

    private static final String BASE62_ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int BASE = BASE62_ALPHABET.length();

    /**
     * UUID를 Base62 문자열로 변환하고 8자리로 자른다.
     */
    public static String encodeUUID(UUID uuid) {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        BigInteger number = new BigInteger(1, bb.array());
        StringBuilder sb = new StringBuilder();

        while (number.compareTo(BigInteger.ZERO) > 0) {
            BigInteger[] divRem = number.divideAndRemainder(BigInteger.valueOf(BASE));
            sb.append(BASE62_ALPHABET.charAt(divRem[1].intValue()));
            number = divRem[0];
        }

        return sb.reverse().substring(0, 8);
    }
}