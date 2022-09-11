/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.manager.service.core.impl;

import com.github.pagehelper.PageInfo;
import org.apache.inlong.manager.common.pojo.node.DataNodePageRequest;
import org.apache.inlong.manager.common.pojo.node.DataNodeRequest;
import org.apache.inlong.manager.common.pojo.node.DataNodeResponse;
import org.apache.inlong.manager.service.ServiceBaseTest;
import org.apache.inlong.manager.service.core.DataNodeService;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Data node service test for {@link DataNodeService}
 */
public class DataNodeServiceTest extends ServiceBaseTest {

    @Autowired
    private DataNodeService dataNodeService;

    /**
     * Save data node info.
     */
    public Integer saveOpt(String nodeName, String type, String url, String username, String password) {
        DataNodeRequest request = new DataNodeRequest();
        request.setName(nodeName);
        request.setType(type);
        request.setUrl(url);
        request.setUsername(username);
        request.setToken(password);
        request.setInCharges(GLOBAL_OPERATOR);
        return dataNodeService.save(request, GLOBAL_OPERATOR);
    }

    /**
     * Get data node list info.
     */
    public PageInfo<DataNodeResponse> listOpt(String type, String name) {
        DataNodePageRequest request = new DataNodePageRequest();
        request.setType(type);
        request.setName(name);
        return dataNodeService.list(request);
    }

    /**
     * update data node info.
     */
    public Boolean updateOpt(Integer id, String nodeName, String type, String url, String username, String password) {
        DataNodeRequest request = new DataNodeRequest();
        request.setId(id);
        request.setName(nodeName);
        request.setType(type);
        request.setUrl(url);
        request.setUsername(username);
        request.setToken(password);
        return dataNodeService.update(request, GLOBAL_OPERATOR);
    }

    /**
     * Delete data node info.
     */
    public Boolean deleteOpt(Integer id) {
        return dataNodeService.delete(id, GLOBAL_OPERATOR);
    }

    @Test
    public void testDataService() {
        String nodeName = "hiveNode1";
        String type = "HIVE";
        String url = "127.0.0.1:8080";
        String usename = "admin";
        String password = "123";

        // test save data node
        Integer id = this.saveOpt(nodeName, type, url, usename, password);
        Assert.assertNotNull(id);

        // test get data node
        DataNodeResponse nodeResponse = dataNodeService.get(id);
        Assert.assertNotNull(nodeResponse);
        Assert.assertEquals(type, nodeResponse.getType());

        // test get data node list
        PageInfo<DataNodeResponse> listDataNode = this.listOpt(type, nodeName);
        Assert.assertEquals(listDataNode.getTotal(), 1);

        // test update data node
        String newNodeName = "kafkaNode1";
        String newType = "KAFKA";
        String newUrl = "127.0.0.1:8083";
        String newUsername = "admin2";
        String newPassword = "456";
        Boolean updateSuccess = this.updateOpt(id, newNodeName, newType, newUrl, newUsername, newPassword);
        Assert.assertTrue(updateSuccess);

        // test delete data node
        Boolean deleteSuccess = this.deleteOpt(id);
        Assert.assertTrue(deleteSuccess);
    }

}
