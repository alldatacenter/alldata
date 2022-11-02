package com.alibaba.tesla.tkgone.server.domain.dto;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.tkgone.server.domain.Config;
import lombok.*;
import org.springframework.beans.BeanUtils;

/**
 * @author yangjinghua
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConfigDto extends Config {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private String category;
    private String nrType;
    private String nrId;
    private String name;
    private String content;
    private String modifier;
    private Object contentObject;

    public ConfigDto(Config config) {
        BeanUtils.copyProperties(config, this);
        try {
            this.contentObject = JSONObject.parse(content);
        } catch (Exception e) {
            this.contentObject = content;
        }
    }

    public Config toConfig() {

        Config config = new Config();
        BeanUtils.copyProperties(this, config);
        return config;

    }

}