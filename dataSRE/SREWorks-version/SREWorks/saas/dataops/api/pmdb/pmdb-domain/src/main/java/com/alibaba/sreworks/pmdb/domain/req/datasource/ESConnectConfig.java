package com.alibaba.sreworks.pmdb.domain.req.datasource;

import lombok.Data;
import lombok.NonNull;

/**
 * es据源链接配置
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/12/09 16:24
 */
@Data
public class ESConnectConfig {

    @NonNull
    String schema;

    @NonNull
    String host;

    @NonNull
    Integer port;

    String username;

    String password;

    public static String getTipMsg() {
        return "{" +
                "\"schema\": \"http\", " +
                "\"host\": \"xxx\", " +
                "\"port\": 9200, " +
                "\"username\": \"可置空\", " +
                "\"password\": \"可置空\"" +
                "}";
    }
}
