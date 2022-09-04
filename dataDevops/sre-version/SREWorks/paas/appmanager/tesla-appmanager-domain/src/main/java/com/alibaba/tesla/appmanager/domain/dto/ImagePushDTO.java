package com.alibaba.tesla.appmanager.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImagePushDTO {
    /**
     *
     */
    private String dockerSecretName;

    /**
     * 镜像仓库
     */
    private ImagePushRegistryDTO imagePushRegistry;
}
