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

    private String innerHost;

    private String publicHost;

    private String innerPort;

    private String publicPort;

    private String user;

    private String password;

}
