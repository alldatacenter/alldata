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

package org.apache.griffin.core.job.factory;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.griffin.core.exception.GriffinException;
import org.apache.griffin.core.job.FileExistPredicator;
import org.apache.griffin.core.job.Predicator;
import org.apache.griffin.core.job.entity.SegmentPredicate;
import org.apache.griffin.core.util.PredicatorMock;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.HashMap;

import static org.apache.griffin.core.util.EntityMocksHelper.createFileExistPredicate;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
public class PredicatorFactoryTest {

    @Test
    public void testFileExistPredicatorCreation() throws IOException {
        Predicator predicator = PredicatorFactory.newPredicateInstance(createFileExistPredicate());
        assertNotNull(predicator);
        assertTrue(predicator instanceof FileExistPredicator);
    }

    @Test(expected = GriffinException.NotFoundException.class)
    public void testUnknownPredicator() throws JsonProcessingException {
        PredicatorFactory.newPredicateInstance(
                new SegmentPredicate("unknown", null));
    }

    @Test
    public void testPluggablePredicator() throws JsonProcessingException {
        String predicatorClass = "org.apache.griffin.core.util.PredicatorMock";
        HashMap<String, Object> map = new HashMap<>();
        map.put("class", predicatorClass);
        SegmentPredicate segmentPredicate = new SegmentPredicate("custom", null);
        segmentPredicate.setConfigMap(map);
        Predicator predicator = PredicatorFactory.newPredicateInstance(segmentPredicate);
        assertNotNull(predicator);
        assertTrue(predicator instanceof PredicatorMock);
    }
}
