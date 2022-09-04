package com.alibaba.tesla.appmanager.server.dag.helper;

import com.alibaba.tesla.appmanager.server.dag.domain.DagNode;

import java.util.LinkedList;

/**
 * @InterfaceName: DagGraphFactory
 * @Author: dyj
 * @DATE: 2020-12-02
 * @Description:
 **/
public interface DagGraphFactory {
    void appendDagEdge(DagNode dagNode);

    LinkedList<DagNode> getDagNodeList();
}
