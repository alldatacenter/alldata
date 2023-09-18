package com.datasophon.common.model;

import com.datasophon.common.enums.InstallState;
import lombok.Data;

import java.util.Date;

@Data
public class HostInfo {

    private String hostname;

    private String ip;
    /**
     * 是否受管
     */
    private boolean managed;

    /**
     * 检测结果
     */
    private CheckResult checkResult;

    private String sshUser;

    private Integer sshPort;
    /**
     * 安装进度
     */
    private Integer progress;

    private String clusterCode;

    /**
     * 安装状态1:正在安装 2：安装成功 3：安装失败
     */
    private InstallState installState;

    private Integer installStateCode;

    private String errMsg;

    private String message;

    private Date createTime;

    private String cpuArchitecture;


}
