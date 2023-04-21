package org.dromara.cloudeon.dto;

import lombok.Data;

@Data
public class CheckHostInfo {
    private Integer coreNum;
    private Integer totalMem;
    private Integer totalDisk;
    private String hostname;
    private String arch;
}
