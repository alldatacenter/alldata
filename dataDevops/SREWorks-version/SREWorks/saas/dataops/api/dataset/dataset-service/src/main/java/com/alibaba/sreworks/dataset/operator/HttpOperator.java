package com.alibaba.sreworks.dataset.operator;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.dataset.common.exception.RequestException;
import com.alibaba.sreworks.dataset.connection.HttpClient;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.stereotype.Service;


/**
 * HTTP工具类
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/11/29 16:50
 */
@Service
@Slf4j
public abstract class HttpOperator{

    protected JSONObject doRequest(Request request) throws Exception {
        OkHttpClient okHttpClient = HttpClient.getHttpClient();
        Response response = okHttpClient.newCall(request).execute();
        if (response.code() != 200) {
            throw new RequestException(String.format("请求接口[%s]异常, 异常信息:%s", request.url().url(), response.cacheResponse()));
        }

        ResponseBody responseBody = response.body();
        if (responseBody == null) {
            throw new RequestException(String.format("请求接口[%s]异常, 响应题为空, 异常信息:%s", request.url().url(), response.message()));
        }

        String bodyStr = responseBody.string();
        JSONObject retBody = JSONObject.parseObject(bodyStr);
        int retCode = retBody.getIntValue("code");
        if (retCode >= 400) {
            throw new RequestException(String.format("请求接口[%s]失败, 请检查接口参数, code:%d, msg:%s", request.url().url(), retCode, retBody.getString("message")));
        }
        return retBody;
    }
}
