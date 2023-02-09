package com.qcloud.cos.model.ciModel.workflow;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 媒体处理工作流 拓扑信息实体
 */
public class MediaTopology {
    /**
     * 节点依赖关系
     * eg:
     *  <Start>Snapshot_1581665960536,Snapshot_1581665960537,Animation_1581665960538,Animation_1581665960539</Start>
     *  <Snapshot_1581665960536>End</Snapshot_1581665960536>
     *  <Snapshot_1581665960537>End</Snapshot_1581665960537>
     *  <Animation_1581665960538>End</Animation_1581665960538>
     *  <Animation_1581665960539>End</Animation_1581665960539>
     *  <SmartCover_1581665960539>End</SmartCover_1581665960539>
     */
    private Map<String,MediaWorkflowDependency> mediaWorkflowDependency;

    /**
     * 节点列表
     */
    private Map<String, MediaWorkflowNode> mediaWorkflowNodes;

    public Map<String,MediaWorkflowDependency> getMediaWorkflowDependency() {
        if (mediaWorkflowDependency==null){
            mediaWorkflowDependency = new LinkedHashMap<>();
        }
        return mediaWorkflowDependency;
    }

    public void setMediaWorkflowDependency(Map<String, MediaWorkflowDependency> mediaWorkflowDependency) {
        this.mediaWorkflowDependency = mediaWorkflowDependency;
    }

    public Map<String, MediaWorkflowNode> getMediaWorkflowNodes() {
        if (mediaWorkflowNodes == null){
            mediaWorkflowNodes = new LinkedHashMap<>();
        }
        return mediaWorkflowNodes;
    }

    public void setMediaWorkflowNodes(Map<String, MediaWorkflowNode> mediaWorkflowNodes) {
        this.mediaWorkflowNodes = mediaWorkflowNodes;
    }

    @Override
    public String toString() {
        return "MediaTopology{" +
                "mediaWorkflowDependency=" + mediaWorkflowDependency +
                ", mediaWorkflowNodes=" + mediaWorkflowNodes +
                '}';
    }
}
