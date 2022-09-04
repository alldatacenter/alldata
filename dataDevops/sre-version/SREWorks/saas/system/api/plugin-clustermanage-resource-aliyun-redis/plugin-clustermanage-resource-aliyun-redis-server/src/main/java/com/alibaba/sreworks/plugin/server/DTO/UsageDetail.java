package com.alibaba.sreworks.plugin.server.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author jinghua.yjh
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UsageDetail {

    private String user;

    private String password;

    private String publicHost;

    private String publicPort;

    private String innerHost;

    private String innerPort;

    private String privateHost;

    private String privatePort;

}
