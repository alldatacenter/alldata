package com.alibaba.sreworks.domain.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppComponentHelmDetail {

    private String chartUrl;

    private String repoUrl;

    private String chartName;

    private String chartVersion;

}
