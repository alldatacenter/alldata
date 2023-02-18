
package com.datasophon.common.command;

import lombok.Data;

import java.io.Serializable;

@Data
public class HostInfoCollectResult implements Serializable {


    private static final long serialVersionUID = 4197649708954689128L;
    private int coreNum;
    private double totalMem;
    private double totalDisk;
    private double usedDisk;
    private double diskAvail;
    private String hostname;
    private double memUsedPersent;
    private double diskUsedPersent;
    private double averageLoad;
    private String clusterCode;


}