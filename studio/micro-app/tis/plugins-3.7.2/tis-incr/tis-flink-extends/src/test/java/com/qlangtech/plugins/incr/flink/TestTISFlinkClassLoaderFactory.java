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

package com.qlangtech.plugins.incr.flink;

import com.qlangtech.plugins.incr.flink.utils.UberJarUtil;
import com.qlangtech.tis.TIS;
import com.qlangtech.tis.async.message.client.consumer.impl.MQListenerFactory;
import com.qlangtech.tis.coredefine.module.action.TargetResName;
import com.qlangtech.tis.datax.TimeFormat;
import com.qlangtech.tis.datax.impl.DataxProcessor;
import com.qlangtech.tis.manage.common.CenterResource;
import com.qlangtech.tis.offline.DataxUtils;
import com.qlangtech.tis.plugin.incr.TISSinkFactory;
import com.qlangtech.tis.test.TISEasyMock;
import com.qlangtech.tis.util.HeteroEnum;
import org.apache.flink.runtime.execution.librarycache.BlobLibraryCacheManager;
import org.apache.flink.runtime.execution.librarycache.FlinkUserCodeClassLoaders;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.net.URL;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2023-03-13 15:41
 **/
public class TestTISFlinkClassLoaderFactory implements TISEasyMock {

    @Test
    public void testBuildServerLoaderFactory() throws Exception {
        BlobLibraryCacheManager.ClassLoaderFactory loaderFactory;
        URL[] libraryURLs;


        CenterResource.setNotFetchFromCenterRepository();
        String collectionName = "testCollection";
        String current = TimeFormat.yyyyMMddHHmmss.format(DataxUtils.currentTimeStamp());

        MQListenerFactory incrSource = mock("incrSource", MQListenerFactory.class);
        EasyMock.expect(incrSource.create()).andReturn(null);
        //EasyMock.expectLastCall().anyTimes();
        DataxProcessor dataXProcessor = mock("dataXProcessor", DataxProcessor.class);
        TISSinkFactory sinkFactory = mock("sinkFactory", TISSinkFactory.class);

        EasyMock.expect(sinkFactory.createSinkFunction(dataXProcessor)).andReturn(null);

        HeteroEnum.incrSourceListenerFactoryStub = (n) -> {
            return incrSource;
        };

        DataxProcessor.processorGetter = (n) -> {
            return dataXProcessor;
        };

        TISSinkFactory.stubGetter = (n) -> {
            return sinkFactory;
        };


        TISFlinkClassLoaderFactory flinkClassLoaderFactory = new TISFlinkClassLoaderFactory();
        String[] alwaysParentFirstPatterns = new String[]{};


        loaderFactory = flinkClassLoaderFactory.buildServerLoaderFactory(FlinkUserCodeClassLoaders.ResolveOrder.CHILD_FIRST, alwaysParentFirstPatterns, (ex) -> {
            Assert.fail(ex.getMessage());
        }, true);
        this.replay();
        File streamUberJar = UberJarUtil.createStreamUberJar(new TargetResName(collectionName), Long.parseLong(current));

        libraryURLs = new URL[]{streamUberJar.toURL()};

        TIS.clean();


        Assert.assertFalse("shall have not been initialized", TIS.initialized);
        Assert.assertNotNull("loaderFactory result can not be null", loaderFactory.createClassLoader(libraryURLs));

        this.verifyAll();
    }
}
