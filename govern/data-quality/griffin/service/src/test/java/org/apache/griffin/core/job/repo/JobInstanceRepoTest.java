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

package org.apache.griffin.core.job.repo;

import static org.apache.griffin.core.job.entity.LivySessionStates.State;
import static org.apache.griffin.core.job.entity.LivySessionStates.State.BUSY;
import static org.apache.griffin.core.job.entity.LivySessionStates.State.FINDING;
import static org.apache.griffin.core.job.entity.LivySessionStates.State.IDLE;
import static org.apache.griffin.core.job.entity.LivySessionStates.State.NOT_FOUND;
import static org.apache.griffin.core.job.entity.LivySessionStates.State.NOT_STARTED;
import static org.apache.griffin.core.job.entity.LivySessionStates.State.RECOVERING;
import static org.apache.griffin.core.job.entity.LivySessionStates.State.RUNNING;
import static org.apache.griffin.core.job.entity.LivySessionStates.State.STARTING;
import static org.apache.griffin.core.job.entity.LivySessionStates.State.SUCCESS;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.apache.avro.generic.GenericData;
import org.apache.griffin.core.config.EclipseLinkJpaConfigForTest;
import org.apache.griffin.core.job.entity.BatchJob;
import org.apache.griffin.core.job.entity.JobInstanceBean;
import org.apache.griffin.core.job.entity.StreamingJob;
import org.apache.hadoop.hive.metastore.HiveMetaStoreClient;
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
public class JobInstanceRepoTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JobInstanceRepo jobInstanceRepo;
    
    @MockBean
    private IMetaStoreClient client;

    private List<Long> entityIds;

    @Before
    public void setup() {
        entityManager.clear();
        entityManager.flush();
        setEntityManager();
    }

    @Test
    public void testFindByActiveState() {
        State[] states = {STARTING, NOT_STARTED, RECOVERING, IDLE, RUNNING,
                BUSY};
        List<JobInstanceBean> beans = jobInstanceRepo.findByActiveState(states);
        assertThat(beans.size()).isEqualTo(1);
    }

    @Test
    public void testFindByPredicateName() {
        JobInstanceBean bean = jobInstanceRepo.findByPredicateName("pName1");
        assertThat(bean).isNotNull();
    }

    @Test
    public void testFindByInstanceId() {
        JobInstanceBean bean = jobInstanceRepo.findByInstanceId(entityIds.get(0));
        assertThat(bean).isNotNull();
    }

    @Test
    public void testFindByExpireTmsLessThanEqual() {
        List<JobInstanceBean> beans = jobInstanceRepo
                .findByExpireTmsLessThanEqual(1516004640092L);
        assertThat(beans.size()).isEqualTo(2);
    }

    @Test
    public void testDeleteByExpireTimestamp() {
        int count = jobInstanceRepo.deleteByExpireTimestamp(1516004640092L);
        assertThat(count).isEqualTo(2);
    }

    private void setEntityManager() {
        JobInstanceBean bean1 = new JobInstanceBean(
                FINDING,
                "pName1",
                "pGroup1",
                null,
                1516004640092L);
        JobInstanceBean bean2 = new JobInstanceBean(
                NOT_FOUND,
                "pName2",
                "pGroup2",
                null,
                1516004640093L);
        JobInstanceBean bean3 = new JobInstanceBean(
                RUNNING,
                "pName3",
                "pGroup3",
                null,
                1516004640082L);
        JobInstanceBean bean4 = new JobInstanceBean(
                SUCCESS,
                "pName4",
                "pGroup4",
                null,
                1516004640094L);
        BatchJob job1 = new BatchJob();
        StreamingJob job2 = new StreamingJob();
        bean1.setJob(job1);
        bean2.setJob(job1);
        bean3.setJob(job2);
        bean4.setJob(job2);
        entityManager.persistAndFlush(job1);
        entityManager.persistAndFlush(job2);
        entityManager.persistAndFlush(bean1);
        entityManager.persistAndFlush(bean2);
        entityManager.persistAndFlush(bean3);
        entityManager.persistAndFlush(bean4);
        entityIds = new ArrayList<>();
        entityIds.add(bean1.getId());
        entityIds.add(bean2.getId());
        entityIds.add(bean3.getId());
        entityIds.add(bean4.getId());
    }
}
