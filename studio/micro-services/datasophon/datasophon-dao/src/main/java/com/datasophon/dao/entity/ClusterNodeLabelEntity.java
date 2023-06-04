package com.datasophon.dao.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_ddh_cluster_node_label")
public class ClusterNodeLabelEntity {

    @TableId
    private Integer id;

    private Integer clusterId;

    private String nodeLabel;
}
