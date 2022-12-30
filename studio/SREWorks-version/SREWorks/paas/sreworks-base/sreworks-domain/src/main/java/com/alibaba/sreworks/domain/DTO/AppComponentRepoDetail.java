package com.alibaba.sreworks.domain.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AppComponentRepoDetail {

    private String url;

    private String branch;

    private String dockerfileTemplate;

    private Long teamRegistryId;

    private Long teamRepoId;

}
