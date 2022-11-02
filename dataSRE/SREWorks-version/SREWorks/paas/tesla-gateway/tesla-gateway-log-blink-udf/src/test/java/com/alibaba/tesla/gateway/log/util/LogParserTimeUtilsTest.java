package com.alibaba.tesla.gateway.log.util;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
public class LogParserTimeUtilsTest {

    @Test
    public void parserTime() {

        String content = "[2019-11-26 12:30:41 967] INFO [reactor-http-epoll-2][c.a.t.g.s.f.global.LogGlobalFilter]- actionName=executeLog||id=96e740ad||traceId=0b88aa9b15747426419597804d08de||type=onComplete||x-env=null||routeId=channel-service-prod||method=POST||originUri=/service/channel/query/task||requestHost=api.tesla.alibaba-inc.com||targetUri=/query/task||targetHost=channel.mw.tesla.alibaba-inc.com:80||httpCode=200||cost=8";
        String s = LogParserTimeUtils.parserTime(content);
        System.out.println(s);
    }
}