package com.alibaba.tesla.appmanager.server.dag.domain;

import lombok.Builder;
import lombok.Data;

/**
 * @ClassName: DagNodeCollector
 * @Author: dyj
 * @DATE: 2020-12-02
 * @Description:
 **/
@Builder
@Data
public class DagNode {
    private String nodeName;

    private String preNodeName;

    private String expression;

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof DagNode)) {
            return false;
        } else {
            DagNode other = (DagNode) o;
            if (!other.canEqual(this)) {
                return false;
            } else {
                Object thisNodeName = this.getNodeName();
                Object otherNodeName = other.getNodeName();
                if (thisNodeName == null) {
                    if (otherNodeName != null) {
                        return false;
                    }
                } else if (!thisNodeName.equals(otherNodeName)) {
                    return false;
                }

                Object thisPreNodeName = this.getPreNodeName();
                Object otherPreNodeName = other.getPreNodeName();
                if (thisPreNodeName == null) {
                    return otherPreNodeName == null;
                } else {
                    return thisPreNodeName.equals(otherPreNodeName);
                }
            }
        }
    }

    protected boolean canEqual(Object other) {
        return other instanceof DagNode;
    }

    @Override
    public int hashCode() {
        int result = 1;
        Object nodeName = this.getNodeName();
        result = result * 59 + (nodeName == null ? 43 : nodeName.hashCode());
        Object preNodeName = this.getPreNodeName();
        result = result * 59 + (preNodeName == null ? 43 : preNodeName.hashCode());
        return result;
    }
}
