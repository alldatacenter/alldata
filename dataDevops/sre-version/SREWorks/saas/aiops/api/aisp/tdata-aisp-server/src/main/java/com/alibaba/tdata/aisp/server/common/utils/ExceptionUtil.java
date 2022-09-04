package com.alibaba.tdata.aisp.server.common.utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

/**
 * @ClassName: ExceptionUtil
 * @Author: dyj
 * @DATE: 2021-04-01
 * @Description:
 **/
public class ExceptionUtil {
    public static String getStackTrace(Throwable aThrowable) {
        if (aThrowable == null) {
            return null;
        }
        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
        aThrowable.printStackTrace(printWriter);
        return result.toString();
    }
}
