package utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by wulinhao on 2017/7/10.
 */
public class Sign {

    /**
     * Computes the MD5 hash of the given data and returns it as an array of
     * bytes.
     */
    public static byte[] computeMD5Hash(byte[] input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return md.digest(input);
        } catch (NoSuchAlgorithmException e) {
            // should never get here
            throw new IllegalStateException(e);
        }
    }

    static public String signMd5(String ts, String apiUser, String privateKey) {
        String sb = ts + apiUser + privateKey;
        byte[] bytes = computeMD5Hash(sb.getBytes());
        StringBuffer stringBuffer = new StringBuffer();
        for (byte b : bytes) {
            int bt = b & 0xff;
            if (bt < 16) {
                stringBuffer.append(0);
            }
            stringBuffer.append(Integer.toHexString(bt));
        }
        String re = stringBuffer.toString().toUpperCase();
        return re;
    }

    static public String getSign(String apiUser, String privateKey) {
        long ts = System.currentTimeMillis();
        String tsStr = Long.toString(ts);
        return tsStr + "," + apiUser + "," + signMd5(tsStr, apiUser, privateKey);
    }

    static public boolean checkSign(String wantSign, String privateKey) {
        try {
            String[] temp = wantSign.split(",");
            String ts = temp[0];
            String user = temp[1];
            String md5 = temp[2];
            String calSign = signMd5(ts, user, privateKey);
            return md5.equals(calSign);
        } catch (Exception e) {
            return false;
        }
    }

}