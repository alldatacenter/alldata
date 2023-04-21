package org.dromara.cloudeon.controller.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class NodeInfoVO {
    private Integer id;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 主机名
     */
    private String hostname;
    /**
     * IP
     */
    private String ip;
    /**
     * 机架
     */
    private String rack;
    /**
     * 核数
     */
    private Integer coreNum;
    /**
     * 总内存
     */
    private String  totalMem;
    /**
     * 总磁盘
     */
    private String totalDisk;
    private String containerRuntimeVersion;
    private String kubeletVersion;
    private String kernelVersion;
    private String osImage;

    private String sshUser;
    private Integer sshPort;

    /**
     * 集群id
     */
    private Integer clusterId;


    private String cpuArchitecture;

    private String nodeLabel;

    private Integer serviceRoleNum;
}
