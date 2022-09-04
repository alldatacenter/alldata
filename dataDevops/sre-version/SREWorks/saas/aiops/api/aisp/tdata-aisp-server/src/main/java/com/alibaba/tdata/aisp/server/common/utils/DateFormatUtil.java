package com.alibaba.tdata.aisp.server.common.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * @ClassName: DateFormateUtil
 * @Author: dyj
 * @DATE: 2021-07-07
 * @Description:
 **/
public class DateFormatUtil {
    public static String simpleFormate(Date date){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return simpleDateFormat.format(date);
    }

    public static Date convertGmtRfcToDate(String rfcStr){
        StringBuilder rfcFormat = new StringBuilder("yyyy-MM-dd'T'HH:mm:ss");
        int i = rfcStr.indexOf(".");
        int z = rfcStr.indexOf("Z");
        if (i>=0 && z>i){
            rfcFormat.append(".");
            int num = z - i - 1;
            while (num!=0){
                rfcFormat.append("S");
                num--;
            }
        }
        rfcFormat.append("'Z'");
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(rfcFormat.toString());
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date date;
        try {
            date = simpleDateFormat.parse(rfcStr);
        } catch (ParseException e) {
            throw new IllegalArgumentException("action=convertGmtRfcToDate|| can not convert RFC3339 to Date object, rfcStr:"+ rfcStr
                +" message:"+e.getMessage(), e.getCause());
        }
        return date;
    }
}
