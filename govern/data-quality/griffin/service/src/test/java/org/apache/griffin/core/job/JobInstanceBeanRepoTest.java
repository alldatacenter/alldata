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

import static org.apache.griffin.core.job.entity.LivySessionStates.State.BUSY;
import static org.apache.griffin.core.job.entity.LivySessionStates.State.IDLE;
import static org.apache.griffin.core.job.entity.LivySessionStates.State.NOT_STARTED;
import static org.apache.griffin.core.job.entity.LivySessionStates.State.RECOVERING;
import static org.apache.griffin.core.job.entity.LivySessionStates.State.RUNNING;
import static org.apache.griffin.core.job.entity.LivySessionStates.State.STARTING;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.apache.griffin.core.config.EclipseLinkJpaConfigForTest;
import org.apache.griffin.core.job.entity.JobInstanceBean;
import org.apache.griffin.core.job.entity.LivySessionStates;
import org.apache.griffin.core.job.entity.VirtualJob;
import org.apache.griffin.core.job.repo.JobInstanceRepo;
import org.apache.hadoop.hive.metastore.IMetaStoreClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@PropertySource("classpath:application.properties")
@ContextConfiguration(classes = {EclipseLinkJpaConfigForTest.class})
@DataJpaTest
public class JobInstanceBeanRepoTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JobInstanceRepo jobInstanceRepo;

    @MockBean
    private IMetaStoreClient client;

    @Before
    public void setUp() {
        setEntityManager();
    }

    @Test
    public void testFindByJobIdWithPageable() {
        Pageable pageRequest = new PageRequest(0, 10, Sort.Direction.DESC,
                "tms");
        List<JobInstanceBean> instances = jobInstanceRepo.findByJobId(1L,
                pageRequest);
        assertEquals(3, instances.size());
    }


    @Test
    public void testFindByActiveState() {
        LivySessionStates.State[] states = {STARTING, NOT_STARTED, RECOVERING,
                IDLE, RUNNING, BUSY};
        List<JobInstanceBean> list = jobInstanceRepo.findByActiveState(states);
        assertEquals(1, list.size());
    }


    private void setEntityManager() {
        VirtualJob job = new VirtualJob();
        JobInstanceBean instance1 = new JobInstanceBean(1L, LivySessionStates
                .State.SUCCESS,
                "appId1", "http://domain.com/uri1", System.currentTimeMillis(),
                System.currentTimeMillis());
        instance1.setJob(job);
        JobInstanceBean instance2 = new JobInstanceBean(2L, LivySessionStates
                .State.ERROR,
                "appId2", "http://domain.com/uri2", System.currentTimeMillis(),
                System.currentTimeMillis());
        instance2.setJob(job);
        JobInstanceBean instance3 = new JobInstanceBean(2L, LivySessionStates
                .State.STARTING,
                "appId3", "http://domain.com/uri3", System.currentTimeMillis(),
                System.currentTimeMillis());
        instance3.setJob(job);
        entityManager.persistAndFlush(job);
        entityManager.persistAndFlush(instance1);
        entityManager.persistAndFlush(instance2);
        entityManager.persistAndFlush(instance3);
    }
}
