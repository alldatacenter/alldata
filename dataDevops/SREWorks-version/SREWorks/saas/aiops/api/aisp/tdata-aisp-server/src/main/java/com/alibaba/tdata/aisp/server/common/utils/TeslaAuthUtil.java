package com.alibaba.tdata.aisp.server.common.utils;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * @ClassName: TeslaAuthUtil
 * @Author: dyj
 * @DATE: 2021-08-24
 * @Description:
 **/
public class TeslaAuthUtil {

    public static String convertHashBytes2Str(byte[] bytes) {
        char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
        char str[] = new char[16 * 2];
        int k = 0;
        for (int i = 0; i < 16; i++) {
            byte byte0 = bytes[i];
            str[k++] = hexDigits[byte0 >>> 4 & 0xf];
            str[k++] = hexDigits[byte0 & 0xf];
        }
        return new String(str);
    }

    public static String getPasswdHash(String userName, String passwd)
        throws UnsupportedEncodingException, NoSuchAlgorithmException {
        DateFormat df = new SimpleDateFormat("yyyyMMdd");
        Date today = Calendar.getInstance().getTime();
        String todayStr = df.format(today);

        String rawHash = String.format("%s%s%s", userName, todayStr, passwd);
        byte[] bytesOfMessage = rawHash.getBytes("UTF-8");
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] theDigest = md.digest(bytesOfMessage);
        return convertHashBytes2Str(theDigest);
    }

}
