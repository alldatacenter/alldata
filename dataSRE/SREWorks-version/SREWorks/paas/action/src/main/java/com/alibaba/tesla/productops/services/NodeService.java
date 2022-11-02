package com.alibaba.tesla.productops.services;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.productops.DO.ProductopsNode;
import com.alibaba.tesla.productops.common.JsonUtil;
import com.alibaba.tesla.productops.repository.ProductopsAppRepository;
import com.alibaba.tesla.productops.repository.ProductopsNodeRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NodeService {

    @Autowired
    ProductopsNodeRepository productopsNodeRepository;

    @Autowired
    ProductopsAppRepository productopsAppRepository;

    private void patchTree(JSONObject tree, String stageId) {
        String parentNodeTypePath = tree.getString("nodeTypePath");
        List<ProductopsNode> nodeList = productopsNodeRepository
            .findAllByParentNodeTypePathAndStageId(parentNodeTypePath, stageId);
        tree.getJSONArray("children").addAll(
            nodeList.stream()
                .sorted(Comparator.comparing(ProductopsNode::order))
                .map(x -> {
                    JSONObject ret = x.toJsonObject(false, tree.getIntValue("level") + 1);
                    ret.putAll(JSONObject.parseObject(x.getConfig()));
                    return ret;
                })
                .collect(Collectors.toList())
        );
        for (Object child : tree.getJSONArray("children")) {
            patchTree((JSONObject)child, stageId);
        }
    }

    public ProductopsNode getNode(String nodeTypePath, String stageId){
        ProductopsNode node = productopsNodeRepository.findFirstByNodeTypePathAndStageId(nodeTypePath, stageId);
        return node;
    }

    public JSONObject tree(String nodeTypePath, String appId, String stageId) {
        JSONObject tree = productopsAppRepository.findFirstByAppIdAndStageId(appId, stageId).toJsonObject();
        ProductopsNode node = productopsNodeRepository.findFirstByNodeTypePathAndStageId(nodeTypePath, stageId);
        if (node != null) {
            tree.putAll(JSONObject.parseObject(node.getConfig()));
        }
        tree.putAll(JsonUtil.map(
            "nodeTypePath", nodeTypePath,
            "nodeId", nodeTypePath,
            "children", new JSONArray(),
            "level", 0,
            "isRootNode", true,
            "treeType", "app",
            "name", appId
        ));
        patchTree(tree, stageId);
        return tree;
    }

    public void cleanNodesRole(ProductopsNode node, String role, String stageId) {
        cleanNodeRole(node, role);
        List<ProductopsNode> childNodeList = productopsNodeRepository
            .findAllByParentNodeTypePathAndStageId(node.getNodeTypePath(), stageId);
        for (ProductopsNode childNode : childNodeList) {
            cleanNodesRole(childNode, role, stageId);
        }
    }

    public void cleanNodeRole(ProductopsNode node, String role) {
        JSONObject configJsonObject = JSONObject.parseObject(node.getConfig());
        JSONArray roleList = configJsonObject.getJSONArray("roleList");
        if (roleList != null) {
            roleList.remove(role);
        }
        configJsonObject.put("roleList", roleList);
        node.setConfig(JSONObject.toJSONString(configJsonObject));
        node.setGmtModified(System.currentTimeMillis());
        productopsNodeRepository.saveAndFlush(node);
    }

}
