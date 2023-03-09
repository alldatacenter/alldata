/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/

package org.apache.griffin.core.measure.repo;

import static org.apache.griffin.core.util.EntityMocksHelper.createDataConnector;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.griffin.core.config.EclipseLinkJpaConfigForTest;
import org.apache.griffin.core.measure.entity.DataConnector;
import org.apache.hadoop.hive.metastore.IMetaStoreClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@ContextConfiguration(classes = {EclipseLinkJpaConfigForTest.class})
public class DataConnectorRepoTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private DataConnectorRepo dcRepo;

    @MockBean
    private IMetaStoreClient client;

    @Before
    public void setup() throws Exception {
        entityManager.clear();
        entityManager.flush();
        setEntityManager();
    }

    @Test
    public void testFindByConnectorNames() {
        List<DataConnector> connectors = dcRepo.findByConnectorNames(Arrays
                .asList("name1", "name2"));
        assertEquals(connectors.size(), 2);
    }

    @Test
    public void testFindByConnectorNamesWithEmpty() {
        List<DataConnector> connectors = dcRepo.findByConnectorNames(
                new ArrayList<>());
        assertEquals(connectors.size(), 0);
    }

    public void setEntityManager() throws Exception {
        DataConnector dc1 = createDataConnector("name1", "database1", "table1",
                "/dt=#YYYYMM#");

        entityManager.persistAndFlush(dc1);

        DataConnector dc2 = createDataConnector("name2", "database2", "table2",
                "/dt=#YYYYMM#");
        entityManager.persistAndFlush(dc2);

        DataConnector dc3 = createDataConnector("name3", "database3", "table3",
                "/dt=#YYYYMM#");
        entityManager.persistAndFlush(dc3);
    }

}
