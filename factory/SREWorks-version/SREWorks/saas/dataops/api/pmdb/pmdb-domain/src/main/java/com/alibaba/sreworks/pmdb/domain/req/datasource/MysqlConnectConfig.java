package com.alibaba.sreworks.pmdb.domain.req.datasource;

import lombok.Data;
import lombok.NonNull;

/**
 * mysql数据源链接配置
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/12/09 16:24
 */
@Data
public class MysqlConnectConfig {

    @NonNull
    String host;

    @NonNull
    String username;

    @NonNull
    Integer port;

    @NonNull
    String db;

    @NonNull
    String password;

    public static String getTipMsg() {
        return "{" +
                "\"host\": \"xxx\", " +
                "\"port\": 9200, " +
                "\"username\": \"user\", " +
                "\"password\": \"pwd\", " +
                "\"db\": \"aaa\"" +
                "}";
    }
}
