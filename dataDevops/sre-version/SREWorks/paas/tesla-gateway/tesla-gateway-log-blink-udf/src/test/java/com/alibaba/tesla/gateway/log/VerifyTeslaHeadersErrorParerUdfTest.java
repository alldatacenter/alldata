package com.alibaba.tesla.gateway.log;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.gateway.log.domain.VerifyTeslaHeaderResultLog;
import com.alibaba.tesla.gateway.log.util.LogParserTimeUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
public class VerifyTeslaHeadersErrorParerUdfTest {


    @Test
    public void testEval(){
        String log = "[2020-04-21 19:33:21 813] WARN [reactor-http-epoll-2][c.a.t.g.s.f.g.AuthCheckGlobalFilter]- "
            + "gatewayAuthCheck:verifyTeslaHeadersError!request header can't be "
            + "empty||requestId=1a290741||requestPath=/proxy/chatBot/chatApi/sendoto||headers=[Host:\"api.tesla"
            + ".alibaba-inc.com\", X-Real-IP:\"42.120.72.76\", Web-Server-Type:\"nginx\", X-Forwarded-For:\"42.120.72"
            + ".76\", Connection:\"close\", Accept:\"*/*\", Accept-Language:\"zh-cn\", Accept-Encoding:\"gzip, "
            + "deflate\", User-Agent:\"%E7%86%8A%E6%8E%8C%E8%AE%B0/8179 CFNetwork/974.1 Darwin/18.0.0 (x86_64)\", "
            + "eagleeye-rpcid:\"0.1\", tesla-gateway-access-number:\"1\", "
            + "eagleeye-traceid:\"0b152e5415874688018124893d0969\"]";

        VerifyTeslaHeaderResultLog resultLog = eval(log);
        System.out.println(resultLog.toString());
    }

    public static final String REG = ".*(gatewayAuthCheck:verifyTeslaHeadersError.*)";
    public static final Pattern pattern = Pattern.compile(REG);

    protected String match(String content){
        Matcher matcher = pattern.matcher(content);
        if(matcher.find()){
            return matcher.group(1);
        }
        return null;
    }

    public VerifyTeslaHeaderResultLog eval(String content){
        String value = match(content);
        if(StringUtils.isEmpty(value)){
            return null;
        }
        String[] strs = value.split("\\|\\|");
        JSONObject json = new JSONObject();
        for (String str : strs) {
            String[] kv = str.split("=", 2);
            if (kv.length > 1) {
                json.put(kv[0], kv[1]);
            }
        }

        VerifyTeslaHeaderResultLog resultLog = new VerifyTeslaHeaderResultLog();
        resultLog.setContent(value);

        Object requestId = json.get("requestId");
        if (requestId instanceof String) {
            resultLog.setRequestId((String)requestId);
        }

        Object requestPath = json.get("requestPath");
        if (requestPath instanceof String) {
            resultLog.setRequestPath((String) requestPath);
        }

        LocalDateTime dateTime = getCreateTime(content);
        if (dateTime != null) {
            resultLog.setCreateTime(dateTime.toString());
        }
        return resultLog;
    }

    private LocalDateTime getCreateTime(String content) {
        return LogParserTimeUtils.parserTimeToDate(content);
    }
}