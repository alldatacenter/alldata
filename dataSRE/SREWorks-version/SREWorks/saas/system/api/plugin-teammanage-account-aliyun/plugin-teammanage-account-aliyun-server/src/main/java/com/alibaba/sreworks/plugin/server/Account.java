package com.alibaba.sreworks.plugin.server;

import com.alibaba.sreworks.common.annotation.Alias;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author jinghua.yjh
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Account {

    @Alias("accessKeyId")
    private String accessKeyId;

    @Alias("accessKeySecret")
    private String accessKeySecret;

}
