package com.alibaba.sreworks.other.server.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemFile {

    private Long teamId;

    private String teamName;

    private String name;

    private Long size;

    private String creator;

    private String creatorName;

    private String url;

}
