package com.alibaba.tesla.appmanager.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImagePushRegistryDTO {

    /**
     * Docker Registry
     */
    private String dockerRegistry;

    /**
     * Docker Namespace
     */
    private String dockerNamespace;

    /**
     * 是否使用分支作为 Tag
     */
    private boolean useBranchAsTag;
}
