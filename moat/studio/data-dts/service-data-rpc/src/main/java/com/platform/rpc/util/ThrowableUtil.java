package com.platform.rpc.util;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 *
 * @author AllDataDC
 * @date 2023/3/26 11:14
 * 将异常转换为String
 **/
public class ThrowableUtil {

	public static String toString(Throwable e) {
		StringWriter stringWriter = new StringWriter();
		e.printStackTrace(new PrintWriter(stringWriter));
		String errorMsg = stringWriter.toString();
		return errorMsg;
	}

}
