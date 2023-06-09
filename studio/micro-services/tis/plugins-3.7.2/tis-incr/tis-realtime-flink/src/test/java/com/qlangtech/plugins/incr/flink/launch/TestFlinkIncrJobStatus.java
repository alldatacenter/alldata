/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qlangtech.plugins.incr.flink.launch;

import com.qlangtech.tis.coredefine.module.action.IFlinkIncrJobStatus;
import org.apache.commons.collections.CollectionUtils;
import org.apache.flink.api.common.JobID;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-04-15 18:30
 **/
public class TestFlinkIncrJobStatus {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testTriggerSavePoint() throws Exception {
        File incrJobFile = folder.newFile();
        //主动发起一个 savepoint 但不执行stop
        String savePointDir = "file:///opt/data/savepoint/savepoint_20220519182209246";
        FlinkIncrJobStatus incrJobStatus = new FlinkIncrJobStatus(incrJobFile);
        JobID jobid = JobID.fromHexString("ac99018a3dfd65fde538133d91ca58d7");
        incrJobStatus.createNewJob(jobid);

        incrJobStatus.addSavePoint(savePointDir, IFlinkIncrJobStatus.State.RUNNING);
        Assert.assertEquals(FlinkIncrJobStatus.State.RUNNING, incrJobStatus.getState());
        assertSavepointEqual(incrJobStatus, savePointDir);

        // 重新加载
        incrJobStatus = new FlinkIncrJobStatus(incrJobFile);
        Assert.assertEquals(FlinkIncrJobStatus.State.RUNNING, incrJobStatus.getState());
        assertSavepointEqual(incrJobStatus, savePointDir);
    }

    @Test
    public void testDiscardSavePoint() throws Exception {
        File incrJobFile = folder.newFile();
        //主动发起一个 savepoint 但不执行stop

        FlinkIncrJobStatus incrJobStatus = new FlinkIncrJobStatus(incrJobFile);
        JobID jobid = JobID.fromHexString("ac99018a3dfd65fde538133d91ca58d7");
        incrJobStatus.createNewJob(jobid);

        String savePointDir = "file:///opt/data/savepoint/savepoint_20220519182209246";
        incrJobStatus.stop(savePointDir);
        Assert.assertEquals(FlinkIncrJobStatus.State.STOPED, incrJobStatus.getState());
        assertSavepointEqual(incrJobStatus, savePointDir);

        // 重新加载
        incrJobStatus = new FlinkIncrJobStatus(incrJobFile);
        try {
            incrJobStatus.discardSavepoint(savePointDir);
            Assert.fail("must be faild,current stat is stoped can not discardSavepoint() can not be invoked");
        } catch (Exception e) {

        }
        jobid = JobID.fromHexString("ac99018a3dfd65fde538133d91ca9999");
        incrJobStatus.relaunch(jobid);
        incrJobStatus.discardSavepoint(savePointDir);
        Assert.assertEquals(FlinkIncrJobStatus.State.RUNNING, incrJobStatus.getState());
        Assert.assertEquals(0, incrJobStatus.getSavepointPaths().size());

        // 重新加载
        incrJobStatus = new FlinkIncrJobStatus(incrJobFile);
        Assert.assertEquals(FlinkIncrJobStatus.State.RUNNING, incrJobStatus.getState());
        Assert.assertEquals(0, incrJobStatus.getSavepointPaths().size());

    }

    @Test
    public void testCreateNewJob() throws Exception {

        File incrJobFile = folder.newFile();

        FlinkIncrJobStatus incrJobStatus = new FlinkIncrJobStatus(incrJobFile);

        JobID jobid = JobID.fromHexString("ac99018a3dfd65fde538133d91ca58d7");
        incrJobStatus.createNewJob(jobid);
        Assert.assertEquals(FlinkIncrJobStatus.State.RUNNING, incrJobStatus.getState());

        String savePointDir = "file:///opt/data/savepoint/savepoint_20220415182209246";
        incrJobStatus.stop(savePointDir);
        Assert.assertEquals(FlinkIncrJobStatus.State.STOPED, incrJobStatus.getState());

        assertSavepointEqual(incrJobStatus, savePointDir);

        // 重新加载
        incrJobStatus = new FlinkIncrJobStatus(incrJobFile);
        Assert.assertEquals(FlinkIncrJobStatus.State.STOPED, incrJobStatus.getState());
        assertSavepointEqual(incrJobStatus, savePointDir);
        Assert.assertEquals(jobid, incrJobStatus.getLaunchJobID());

        JobID relaunchJobid = JobID.fromHexString("ac99018a3dfd65fde538133d91ca58d6");
        incrJobStatus.relaunch(relaunchJobid);

        Assert.assertEquals(relaunchJobid, incrJobStatus.getLaunchJobID());
        Assert.assertEquals(FlinkIncrJobStatus.State.RUNNING, incrJobStatus.getState());
        assertSavepointEqual(incrJobStatus, savePointDir);
        // 重新加载
        incrJobStatus = new FlinkIncrJobStatus(incrJobFile);
        Assert.assertEquals(relaunchJobid, incrJobStatus.getLaunchJobID());
        Assert.assertEquals(FlinkIncrJobStatus.State.RUNNING, incrJobStatus.getState());
        assertSavepointEqual(incrJobStatus, savePointDir);

        incrJobStatus.cancel();
        Assert.assertNull(incrJobStatus.getLaunchJobID());
        Assert.assertEquals(FlinkIncrJobStatus.State.NONE, incrJobStatus.getState());

        // 重新加载
        incrJobStatus = new FlinkIncrJobStatus(incrJobFile);
        Assert.assertNull(incrJobStatus.getLaunchJobID());

        Assert.assertTrue(CollectionUtils.isEmpty(incrJobStatus.getSavepointPaths()));
        Assert.assertEquals(FlinkIncrJobStatus.State.NONE, incrJobStatus.getState());


    }

    private void assertSavepointEqual(FlinkIncrJobStatus incrJobStatus, String savePointDir) {

        for (IFlinkIncrJobStatus.FlinkSavepoint sp : incrJobStatus.getSavepointPaths()) {
            if (sp.getPath().equals(savePointDir)) {
                Assert.assertTrue(sp.getCreateTimestamp() > 0);
                return;
            }
        }

        Assert.fail("savePointDir:" + savePointDir + " can not match");
        //  Assert.assertTrue(CollectionUtils.isEqualCollection(Collections.singletonList(savePointDir), incrJobStatus.getSavepointPaths()));
    }
}
