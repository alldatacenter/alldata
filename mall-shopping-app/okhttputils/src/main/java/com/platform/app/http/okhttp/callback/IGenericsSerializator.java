package com.platform.app.http.okhttp.callback;

/**
 * Created by wulinhao on 2019/9/23.
 */
public interface IGenericsSerializator {
    <T> T transform(String response, Class<T> classOfT);
}
