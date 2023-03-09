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

package org.apache.griffin.core.job;

import static org.apache.griffin.core.util.EntityMocksHelper.createGriffinMeasure;

import java.util.ArrayList;
import java.util.List;

import org.apache.griffin.core.event.EventSourceType;
import org.apache.griffin.core.event.EventType;
import org.apache.griffin.core.event.GriffinEvent;
import org.apache.griffin.core.event.GriffinHook;
import org.apache.griffin.core.exception.GriffinException;
import org.apache.griffin.core.job.entity.BatchJob;
import org.apache.griffin.core.job.entity.JobDataSegment;
import org.apache.griffin.core.measure.entity.GriffinMeasure;
import org.apache.griffin.core.measure.entity.Measure;
import org.apache.griffin.core.util.EntityMocksHelper;
import org.apache.hadoop.hive.metastore.IMetaStoreClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@ComponentScan("org.apache.griffin.core")
public class EventServiceTest {
    @Autowired
    private JobService jobService;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private List<GriffinEvent> eventList;

    @MockBean
    private IMetaStoreClient client;

    @Before
    public void setup() throws Exception {
        entityManager.clear();
        entityManager.flush();
        setEntityManager();
    }

    @Test
    public void testAddJobEvent() throws Exception {
        BatchJob batch_Job = EntityMocksHelper.createGriffinJob();
        batch_Job.setCronExpression("0 0 12 * * ?");
        batch_Job.setTimeZone("Asia/Shanghai");
        JobDataSegment jds = new JobDataSegment();
        jds.setAsTsBaseline(true);
        jds.setDataConnectorName("target_name");
        List jds_list = new ArrayList();
        jds_list.add(jds);
        batch_Job.setSegments(jds_list);
        jobService.addJob(batch_Job);
        Assert.assertEquals(2, eventList.size());
        Assert.assertEquals(EventType.CREATION_EVENT, eventList.get(0).getType());
        Assert.assertEquals(EventSourceType.JOB, eventList.get(1).getSourceType());
    }

    public void setEntityManager() throws Exception {
        Measure measure1 = createGriffinMeasure("m1");
        measure1.setOrganization("org1");
        ((GriffinMeasure) measure1).setProcessType(GriffinMeasure.ProcessType.BATCH);
        entityManager.persistAndFlush(measure1);
    }

    @Configuration(value = "GriffinTestJobEventHook")
    public static class TestJobEventHook implements GriffinHook {
        private List<GriffinEvent> eventList = new ArrayList<>();

        @Override
        public void onEvent(GriffinEvent event) throws GriffinException {
            eventList.add(event);
        }

        @Bean
        public List<GriffinEvent> getReceivedEvents() {
            return eventList;
        }
    }
}
