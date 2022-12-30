package com.alibaba.tesla.appmanager.server.dag.helper;

import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.server.dag.domain.DagNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedList;

/**
 * @ClassName: DagGraphFactoryImpl
 * @Author: dyj
 * @DATE: 2020-12-02
 * @Description:
 **/
@Slf4j
@Service
public class DagGraphFactoryImpl implements DagGraphFactory {
    private final LinkedList<DagNode> dagNodeList = new LinkedList<>();

    @Override
    public void appendDagEdge(DagNode dagNode) {
        if (isExist(dagNodeList, dagNode)) {
            throw new AppException(AppErrorCode.USER_CONFIG_ERROR,
                    String.format("actionName=appendDagNode|%s is exists!", dagNode.getNodeName()));
        } else {
            dagNodeList.add(dagNode);
            log.info("actionName=appendDagNode");
        }
    }

    @Override
    public LinkedList<DagNode> getDagNodeList() {
        return dagNodeList;
    }

    private boolean isExist(LinkedList<DagNode> dagNodeList, DagNode dagNode) {
        for (DagNode node : dagNodeList) {
            if (node.equals(dagNode)) {
                return true;
            }
        }
        return false;

    }
}
