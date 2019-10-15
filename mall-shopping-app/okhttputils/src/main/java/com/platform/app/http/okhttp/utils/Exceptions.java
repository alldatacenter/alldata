package com.platform.app.http.okhttp.utils;

/**
 * Created by wulinhao on 2019/9/14.
 */
public class Exceptions
{
    public static void illegalArgument(String msg, Object... params)
    {
        throw new IllegalArgumentException(String.format(msg, params));
    }


}
