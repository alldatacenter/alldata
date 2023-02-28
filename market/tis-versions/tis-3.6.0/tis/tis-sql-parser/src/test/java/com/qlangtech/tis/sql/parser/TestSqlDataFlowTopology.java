/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.qlangtech.tis.sql.parser;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.qlangtech.tis.ajax.AjaxResult;
import com.qlangtech.tis.common.utils.Assert;
import com.qlangtech.tis.manage.common.HttpUtils;
import com.qlangtech.tis.manage.common.IAjaxResult;
import com.qlangtech.tis.sql.parser.SqlTaskNodeMeta.SqlDataFlowTopology;
import com.qlangtech.tis.sql.parser.meta.DependencyNode;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class TestSqlDataFlowTopology extends BasicTestCase {

    public void testDeserialize() throws Exception {
        // 通过yaml的反序列化方式
        SqlDataFlowTopology topology = SqlTaskNodeMeta.getSqlDataFlowTopology("totalpay");
        // topology.getFinalNode()
        String jsonContent = com.alibaba.fastjson.JSON.toJSONString(topology, SerializerFeature.DisableCircularReferenceDetect, SerializerFeature.PrettyFormat);
        // Assert.assertTrue(topology.getTimestamp() > 0);
        Assert.assertTrue(topology.getDumpNodes().size() > 0);
        Assert.assertTrue(topology.getNodeMetas().size() > 0);
        Assert.assertTrue(StringUtils.isNotBlank(topology.getName()));
        Assert.assertNotNull(jsonContent);
        // System.out.println(jsonContent);
        SqlDataFlowTopology topology2 = SqlDataFlowTopology.deserialize(jsonContent);
        Assert.assertNotNull(topology2);
        Collection<DependencyNode> dumpNodes = topology2.getDumpNodes();
        Assert.assertEquals(topology.getDumpNodes().size(), dumpNodes.size());
        Assert.assertEquals(topology.getName(), topology2.getName());
        List<SqlTaskNodeMeta> sqlNode = topology2.getNodeMetas();
        for (SqlTaskNodeMeta n : sqlNode) {
            Assert.assertTrue(n.getDependencies().size() > 0);
            Assert.assertNotNull(n.getSql());
        }
        Assert.assertEquals(topology.getNodeMetas().size(), sqlNode.size());
        Assert.assertEquals(topology.getTimestamp(), topology2.getTimestamp());
    }

    // view-source:http://10.1.21.134:8080/config/config.ajax?action=fullbuild_workflow_action&event_submit_do_get_workflow_detail=true&workflow_id=45&current_time=1510899645000
    public void testDeserializeConsoleHttpRemote() throws Exception {
        AjaxResult<SqlDataFlowTopology> result = null;
        SqlDataFlowTopology topology = null;
        try (InputStream input = this.getClass().getResourceAsStream("SqlDataFlowTopologyHttpResponse.json")) {
            String jContent = IOUtils.toString(input, "utf8");
            JSONObject jObject = JSON.parseObject(jContent);
            SqlDataFlowTopology topology2 = SqlDataFlowTopology.deserialize(jObject.getString(IAjaxResult.KEY_BIZRESULT));
            Assert.assertNotNull(topology2);
            Assert.assertEquals("totalpay", topology2.getName());
            Assert.assertTrue(topology2.getDumpNodes().size() > 0);
            Assert.assertTrue(topology2.getNodeMetas().size() > 0);
        }
        HttpUtils.addMockApply(-1, "mock", "/com/qlangtech/tis/sql/parser/SqlDataFlowTopologyHttpResponse.json");
        // try (InputStream input = this.getClass().getResourceAsStream("SqlDataFlowTopologyHttpResponse.json")) {
        // HttpUtils.mockConnMaker = new MockConnectionMaker() {
        //
        // @Override
        // public MockHttpURLConnection create(URL url, List<Header> heads, HTTPMethod method, byte[] content) {
        // return new MockHttpURLConnection(input);
        // }
        // };
        // }
        result = HttpUtils.soapRemote("http://mock", SqlDataFlowTopology.class);
        Assert.assertNotNull(result);
        topology = result.getBizresult();
        Assert.assertNotNull(topology);
        Assert.assertEquals("totalpay", topology.getName());
    }
}
