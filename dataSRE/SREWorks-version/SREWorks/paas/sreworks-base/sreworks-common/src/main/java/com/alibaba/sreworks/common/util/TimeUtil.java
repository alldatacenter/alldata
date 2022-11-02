package com.alibaba.sreworks.common.util;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.TimeZone;

/**
 * @author jinghua.yjh
 */
public class TimeUtil {

    public static String timeStamp2Date(Long seconds, String format) {
        if (seconds == null) {
            seconds = 0L;
        }
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
        return sdf.format(new Date(seconds * 1000));
    }

    public static String timeStamp2Date(Integer seconds, String format) {
        return timeStamp2Date((long)(int)seconds, format);
    }

    public static String timeStamp2Date(Integer seconds) {
        return timeStamp2Date(seconds, "yyyy-MM-dd HH:mm:ss");
    }

    public static String timeStamp2Date(Long seconds) {
        return timeStamp2Date(seconds, "yyyy-MM-dd HH:mm:ss");
    }

    public static Long offsetDateTime2Timestamp(OffsetDateTime odt) {
        ZonedDateTime zdt = odt.toZonedDateTime();
        return Timestamp.from(zdt.toInstant()).getTime() / 1000;
    }

}