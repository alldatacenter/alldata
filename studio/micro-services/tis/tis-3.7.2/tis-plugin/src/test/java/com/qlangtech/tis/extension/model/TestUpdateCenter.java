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

package com.qlangtech.tis.extension.model;

import com.google.common.collect.Sets;
import com.qlangtech.tis.TIS;
import com.qlangtech.tis.common.utils.Assert;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.PluginManager;
import com.qlangtech.tis.manage.common.CenterResource;
import com.qlangtech.tis.manage.common.Config;
import com.qlangtech.tis.manage.common.HttpUtils;
import com.qlangtech.tis.maven.plugins.tpi.PluginClassifier;
import com.qlangtech.tis.plugin.ds.DataSourceFactory;
import com.qlangtech.tis.util.TestHeteroList;
import com.qlangtech.tis.utils.TisMetaProps;
import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-05-10 09:13
 **/
public class TestUpdateCenter extends TestCase {
    File dataDir;


    @Override
    protected void setUp() throws Exception {
        HttpUtils.mockConnMaker = new HttpUtils.DefaultMockConnectionMaker();

        HttpUtils.addMockApply(PluginManager.PACAKGE_TPI_EXTENSION, new HttpUtils.IClasspathRes() {
            @Override
            public InputStream getResourceAsStream(URL url) {
                ///opt/data/tis/libs/plugins/
                try {
                    File tpi = new File("/opt/data/tis/libs/plugins/", StringUtils.substringAfterLast(url.getPath(), "/"));
                    return FileUtils.openInputStream(tpi);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        HttpUtils.addMockGlobalParametersConfig();
        CenterResource.setNotFetchFromCenterRepository();
        //http://mirror.qlangtech.com/update-site/default.json
        System.clearProperty(Config.KEY_DATA_DIR);
        dataDir = Config.setTestDataDir();
        TestHeteroList.setTISField();
        TIS.clean();
    }

    public void testIntallPlugin() throws Exception {
        HttpUtils.CacheMockRes cacheMockRes = getUpdateSiteHttpStub();

        final String mysqlV5Ds = DataSourceFactory.DS_TYPE_MYSQL + "-V5";

        assertNotNull(cacheMockRes);

        UpdateCenter updateCenter = new UpdateCenter();
        updateCenter.load();
        updateCenter.updateAllSites();

        assertTrue("verfiyResHasFetch", cacheMockRes.verfiyResHasFetch());


        UpdateSite.Plugin dsMysqlPlugin = updateCenter.getPlugin("tis-ds-mysql-plugin");
        assertNotNull(dsMysqlPlugin);

        Set<String> dsMysqlPluginNeededDependenciesSet = Sets.newHashSet("tis-datax-common-plugin", "tis-datax-common-rdbms-plugin");
        List<UpdateSite.Plugin> dsMysqlPluginNeededDependencies = dsMysqlPlugin.getNeededDependencies();
        assertEquals(2, dsMysqlPluginNeededDependencies.size());
        for (UpdateSite.Plugin p : dsMysqlPluginNeededDependencies) {
            assertTrue(p.getDisplayName() + " shall exist in "
                            + dsMysqlPluginNeededDependenciesSet.stream().collect(Collectors.joining(","))
                    , dsMysqlPluginNeededDependenciesSet.contains(p.title));
        }


        List<Descriptor<DataSourceFactory>> descriptorList = TIS.get().getDescriptorList(DataSourceFactory.class);
        Optional<Descriptor<DataSourceFactory>> first = descriptorList.stream().filter((r) -> mysqlV5Ds.equals(r.getDisplayName())).findFirst();
        assertFalse(DataSourceFactory.DS_TYPE_MYSQL + " descriptor must NOT present", first.isPresent());
        String pluginName = "tis-ds-mysql-v5-plugin";
        UpdateSite.Plugin mysqlDSPlugin = updateCenter.getPlugin(pluginName);
        assertNotNull(pluginName + " can not be null", mysqlDSPlugin);
        /** ==========================================================================
         * 开始安装
         * ==========================================================================*/
        Optional<PluginClassifier> classifier = Optional.empty();
        Future<UpdateCenter.UpdateCenterJob> job = mysqlDSPlugin.deploy(true, classifier);
        UpdateCenter.DownloadJob downloadJob = (UpdateCenter.DownloadJob) job.get();
        System.out.println(downloadJob.status);
        // 安装成功
        assertTrue(downloadJob.status instanceof UpdateCenter.DownloadJob.Success);
        // TIS.clean();
        // 重新获取插件实例
        descriptorList = TIS.get().getDescriptorList(DataSourceFactory.class);
        first = descriptorList.stream().filter((r) -> mysqlV5Ds.equals(r.getDisplayName())).findFirst();
        assertTrue(mysqlV5Ds + " descriptor must present", first.isPresent());

    }

    private HttpUtils.CacheMockRes getUpdateSiteHttpStub() {
        if (HttpUtils.mockConnMaker != null) {
            // HttpUtils.mockConnMaker.clearStubs();
        }
        //http://mirror.qlangtech.com/3.6.0/update-site/default.json?id=default&version=%3F

        return HttpUtils.addMockApply(0
                , "http://mirror.qlangtech.com/" + TisMetaProps.getInstance().getVersion()
                        + "/update-site/default.json", "default-update-site.json", TestUpdateCenter.class);
    }

    public void testLoad() throws Exception {

        HttpUtils.CacheMockRes cacheMockRes = getUpdateSiteHttpStub();

        File localDftUpdateSiteJSON = new File(TIS.pluginCfgRoot, "updates/default.json");
        FileUtils.deleteQuietly(localDftUpdateSiteJSON);

        UpdateCenter updateCenter = new UpdateCenter();
        updateCenter.load();

        List<FormValidation> formValidations = updateCenter.updateAllSites();
        assertEquals(1, formValidations.size());

        assertTrue("verfiyResHasFetch", cacheMockRes.verfiyResHasFetch());

        assertTrue(updateCenter.getSiteList().size() > 0);


        List<UpdateSite.Plugin> availables = updateCenter.getAvailables();
        assertEquals(37, availables.size());

        Map<String, UpdateSite.Plugin> plugins = availables.stream().collect(Collectors.toMap((p) -> p.getDisplayName(), (p) -> p));

        final String dataxHudiPlugin = "tis-datax-hudi-plugin";
        UpdateSite.Plugin dataxHudi = plugins.get(dataxHudiPlugin);
        Assert.assertNotNull("dataxHudi can not be null", dataxHudi);

        Assert.assertTrue(dataxHudi.getArts().size() == 2);

        Set<String> assertClassifies = Sets.newHashSet("hudi_0.10.1;spark_3.2.1;hive_3.1.2;hadoop_3.2.1"
                , "hudi_0.10.1;spark_2.4.4;hive_2.3.1;hadoop_2.7.3");

        List<IPluginCoord> arts = dataxHudi.getArts();
        Optional<PluginClassifier> classifier = null;
        PluginClassifier c = null;
        for (IPluginCoord coord : arts) {
            Assert.assertEquals(dataxHudiPlugin, coord.getIdentityName());

            Assert.assertNotNull(coord.getDownloadUrl());
            Assert.assertTrue(coord.getSize() > 0);

            classifier = coord.getClassifier();
            Assert.assertTrue(classifier.isPresent());
            c = classifier.get();
            Assert.assertTrue("must contain in set:" + c.getClassifier(), assertClassifies.contains(c.getClassifier()));
        }
    }

}
