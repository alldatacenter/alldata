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

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.apache.griffin.core.config.EclipseLinkJpaConfigForTest;
import org.apache.griffin.core.job.entity.AbstractJob;
import org.apache.griffin.core.job.entity.BatchJob;
import org.apache.griffin.core.job.entity.VirtualJob;
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
public class JobRepoTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JobRepo jobRepo;

    @MockBean
    private IMetaStoreClient client;


    @Before
    public void setup() {
        entityManager.clear();
        entityManager.flush();
        setEntityManager();
    }

    @Test
    public void testCountByJobNameAndDeleted() {
        int count = jobRepo.countByJobNameAndDeleted("griffinJobName1", false);
        assertEquals(count, 1);
    }

    @Test
    public void testFindByDeleted() {
        List<AbstractJob> jobs = jobRepo.findByDeleted(false);
        assertEquals(jobs.size(), 4);
    }

    @Test
    public void findByJobNameAndDeleted() {
        List<AbstractJob> jobs = jobRepo
                .findByJobNameAndDeleted("griffinJobName1", false);
        assertEquals(jobs.size(), 1);
    }

    @Test
    public void findByMeasureIdAndDeleted() {
        List<AbstractJob> jobs = jobRepo.findByMeasureIdAndDeleted(1L, false);
        assertEquals(jobs.size(), 4);
    }

    @Test
    public void findByIdAndDeleted() {
        AbstractJob job = jobRepo.findByIdAndDeleted(1L, true);
        assert job == null;
    }

    public void setEntityManager() {
        AbstractJob job1 = new BatchJob(1L, "griffinJobName1", "qName1",
                "qGroup1", false);
        AbstractJob job2 = new BatchJob(1L, "griffinJobName2", "qName2",
                "qGroup2", false);
        AbstractJob job3 = new VirtualJob("virtualJobName1", 1L, "metricName1");
        AbstractJob job4 = new VirtualJob("virtualJobName2", 1L, "metricName2");
        entityManager.persistAndFlush(job1);
        entityManager.persistAndFlush(job2);
        entityManager.persistAndFlush(job3);
        entityManager.persistAndFlush(job4);
    }
}
