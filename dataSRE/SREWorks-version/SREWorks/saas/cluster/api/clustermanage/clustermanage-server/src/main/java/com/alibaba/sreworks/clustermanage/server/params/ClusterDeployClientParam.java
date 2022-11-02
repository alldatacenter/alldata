package com.alibaba.sreworks.clustermanage.server.params;


import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.domain.DO.Cluster;
import lombok.Data;

/**
 * @author jinghua.yjh
 */
@Data
public class ClusterDeployClientParam {

    private JSONObject envMap;

}
