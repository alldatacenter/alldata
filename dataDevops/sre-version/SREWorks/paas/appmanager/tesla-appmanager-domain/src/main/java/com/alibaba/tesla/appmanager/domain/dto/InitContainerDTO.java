package com.alibaba.tesla.appmanager.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author qianmo.zm@alibaba-inc.com
 * @date 2020/11/26.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitContainerDTO {

    /**
     * 任务名
     */
    private String name;

    /**
     * 任务类型
     */
    private String type;

    /**
     * dockerfile路径
     */
    private String dockerfilePath;

    public String createDockerFileTemplate() {
        return "Dockerfile-" + createContainerName() + ".tpl";
    }

    public String createContainerName() {
        return name;
    }
}
