package com.datasophon.common.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class StartWorkerMessage  implements Serializable {
    private int coreNum;
    private double totalMem;
    private double totalDisk;
    private double usedDisk;
    private double diskAvail;
    private String hostname;
    private double memUsedPersent;
    private double diskUsedPersent;
    private double averageLoad;
    private Integer clusterId;
    private String ip;
    private String cpuArchitecture;

    private Long deliveryId;

}
