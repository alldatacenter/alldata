package com.qcloud.cos.utils;

import org.junit.Test;

import java.math.BigInteger;

import static org.junit.Assert.assertEquals;

public class CRC64Test {
    @Test
    public void testCrc64() {
        String str1 = "hello ";
        String str2 = "world!";
        String str3 = str1 + str2;
        CRC64 crc1 = new CRC64(0);
        crc1.update(str1.getBytes(), str1.length());
        assertEquals(crc1.getValue(), 1796661072844795914L);

        crc1.reset();
        crc1.update(str1.getBytes(), str1.length());
        assertEquals(crc1.getValue(), 1796661072844795914L);

        CRC64 crc2 = new CRC64(0);
        crc2.update(str2.getBytes(), str2.length());
        assertEquals(crc2.getValue(), -8404377302871452766L);

        CRC64 crc3 = new CRC64(0);
        crc3.update(str1.getBytes(), str1.length());
        crc3.update(str2.getBytes(), str2.length());

        CRC64 crc4 = CRC64.combine(crc1, crc2, str2.length());
        assertEquals(crc3.getValue(), crc4.getValue());

        String serverCrc64Str = "9548687815775124833";
        BigInteger bigInteger = new BigInteger(serverCrc64Str);
        assertEquals(crc3.getValue(), bigInteger.longValue());

        CRC64 crc5 = new CRC64(0);
        crc5.update(str3.getBytes(), str3.length());
        assertEquals(crc3.getValue(), crc5.getValue());

    }
}
