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
package com.qlangtech.tis.manage.common;

import com.qlangtech.tis.git.GitUtils;
import com.qlangtech.tis.offline.pojo.TISDb;
import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class TestGitUtils extends TestCase {

    static final String tmpDir = "/tmp/tis";

    static {
        Config.setDataDir(tmpDir);
        try {
            FileUtils.forceDelete(new File(tmpDir));
        } catch (IOException e) {
            // throw new RuntimeException(e);
        }
        try {
            FileUtils.forceMkdir(new File(tmpDir));
        } catch (IOException e) {
            // throw new RuntimeException(e);
        }
    }

    private static final GitUtils GIT_UTILS = GitUtils.$();

    public void testGetFacadeDBConfig() {
//        DBConfig orderCfg = GIT_UTILS.getDbLinkMetaData("order", DbScope.FACADE);
//        assertNotNull("orderCfg can not be null", orderCfg);
    }

//    public void testGetDbConfig() {
//        final AtomicLong current = new AtomicLong(System.currentTimeMillis());
//        final AtomicInteger httpApplyCount = new AtomicInteger();
//        final AtomicInteger headLastUpdateCount = new AtomicInteger();
//        HttpUtils.mockConnMaker = new HttpUtils.DefaultMockConnectionMaker() {
//
//            @Override
//            protected MockHttpURLConnection createConnection(List<ConfigFileContext.Header> heads, ConfigFileContext.HTTPMethod method
//                    , byte[] content, HttpUtils.IClasspathRes cpRes) {
//                httpApplyCount.incrementAndGet();
//                Map<String, List<String>> headerFields = new HashMap<String, List<String>>() {
//
//                    @Override
//                    public List<String> get(Object key) {
//                        if (ConfigFileContext.KEY_HEAD_LAST_UPDATE.equals(key)) {
//                            headLastUpdateCount.incrementAndGet();
//                        }
//                        return super.get(key);
//                    }
//                };
//                // head中不应该有查找文件最新时间的情况
//                Optional<ConfigFileContext.Header> first = heads.stream().filter((r) -> ConfigFileContext.StreamProcess.HEADER_KEY_GET_FILE_META.equals(r.getKey())).findFirst();
//                assertFalse("head中不应该有查找文件最新时间的情况", first.isPresent());
//                headerFields.put(ConfigFileContext.KEY_HEAD_LAST_UPDATE, Collections.singletonList(String.valueOf(current.get())));
//                return new MockHttpURLConnection(cpRes.getResourceAsStream(), headerFields);
//            }
//        };
//        HttpUtils.addMockApply(1, "stream_script_repo.action?path=db%2Ftis-fullbuild-datasource-daily%2Ftable_cfg%2Forder%2Ftotalpayinfo%2Fprofile", "profile_totalpay.json", TestGitUtils.class);
//        HttpUtils.addMockApply(2, "stream_script_repo.action?path=db%2Ftis-fullbuild-datasource-daily%2Ftable_cfg%2Forder%2Ftotalpayinfo%2Fsql", "sql_content_totalpay.txt", TestGitUtils.class);
//        // http://127.0.0.1:8080/tjs/config/stream_script_repo.action?path=db%2Ftis-fullbuild-datasource-daily%2Ftable_cfg%2Forder%2Ftotalpayinfo%2Fprofile
//        // http://127.0.0.1:8080/tjs/config/stream_script_repo.action?path=db%2Ftis-fullbuild-datasource-daily%2Ftable_cfg%2Forder%2Ftotalpayinfo%2Fsql
//        GitUtils fetch = GitUtils.$();
//        assertTrue(StringUtils.startsWith(fetch.dbRootDir.getAbsolutePath(), tmpDir));
//        TISTable tableConfig = fetch.getTableConfig("order", "totalpayinfo");
//        assertNotNull(tableConfig);
//        assertEquals("order", tableConfig.getDbName());
//        assertEquals("totalpayinfo", tableConfig.getTableName());
//        assertEquals(999, tableConfig.getPartitionNum());
//        assertEquals(4, headLastUpdateCount.get());
//        assertEquals(2, httpApplyCount.get());
//        // 再取一次，应该不会再覆盖本地文件了
//        tableConfig = fetch.getTableConfig("order", "totalpayinfo");
//        // 只加2是因为，在判断文件是否过期，判断结果为没有过期，所以不会再取head中的内容了
//        assertEquals(6, headLastUpdateCount.get());
//        assertEquals(4, httpApplyCount.get());
//        // 加5秒文件更新了
//        current.addAndGet(5000);
//        // 再取一次，应该会覆盖本地文件
//        tableConfig = fetch.getTableConfig("order", "totalpayinfo");
//        // 只加2是因为，在判断文件是否过期，判断结果为过期，所以会取head中的内容了
//        assertEquals(10, headLastUpdateCount.get());
//        assertEquals(6, httpApplyCount.get());
//    }

    public void testListDbConfigPath() {
        List<String> subDirs = GitUtils.$().listDbConfigPath("order");
        assertTrue(subDirs.size() > 0);
    }

//    public void testGetTable() throws Exception {
//        TISTable table = GIT_UTILS.getTableConfig("order", "instancedetail");
//        Assert.assertNotNull(table);
//        Assert.assertNotNull(table.getSelectSql());
//        // System.out.println(table.getSelectSql());
//    }

    public void testDataSourceConfig() throws Exception {
//        DBConfig dbConfig = GIT_UTILS.getDbLinkMetaData("order", DbScope.DETAILED);
//        Assert.assertTrue(dbConfig.getDbEnum().size() > 0);
//        Assert.assertNotNull(dbConfig.getHostDesc());
//        Assert.assertNotNull(dbConfig.getName());
//        List<String> childPath = GIT_UTILS.listDbConfigPath("order");
//        Assert.assertNotNull(childPath);
//        Assert.assertEquals(2, childPath.size());
//        for (String c : childPath) {
//            System.out.println(c);
//        }
    }

    public void testCreateDatabaseDaily() throws Exception {
        TISDb db = new TISDb();
        db.setPassword("123456");
        db.setDbName("test");
        db.setDbType("mysql");
        db.setEncoding("utf8");
        GIT_UTILS.createDatabase(db, "test");
    }

//    public void testTableDaily() throws Exception {
//        TISTable tab = new TISTable();
//        // String tableLogicName, String tableName, int partitionNum, Integer dbId,
//        // int partitionInterval, String selectSql
//        final String dbName = "order";
//        final String tabName = "tableName";
//        final String logicName = "tableLogicName";
//        GitUtils.GitUser user = GitUtils.GitUser.dft();
//        GIT_UTILS.deleteTableDaily(dbName, logicName, user);
//        tab.setTableLogicName(logicName);
//        tab.setTableName(tabName);
//        tab.setDbName(dbName);
//        tab.setPartitionInterval(4);
//        tab.setPartitionNum(2);
//        tab.setDbId(999);
//        tab.setSelectSql("select a,b,c FROM USER");
//        // GIT_UTILS.createTableDaily(tab, "hahah");
//        TISTable newTab = GIT_UTILS.getTableConfig(dbName, logicName);
//        Assert.assertNotNull(newTab);
//        Assert.assertEquals("select a,b,c FROM USER", newTab.getSelectSql());
//        // GitDatasourceTablePojo table = GIT_UTILS.getTableConfig("order",
//        // "instancedetail");
//        //
//        // Assert.assertNotNull(table);
//        // Assert.assertNotNull(table.getSelectSql());
//        // System.out.println(table.getSelectSql());
//    }

    // public void testGetTotalpay_instanceJoiner() throws Exception {
    // GIT_UTILS.getWorkflow(wfName, branch);
    // }
    public void testDelete() throws Exception {
        // GitUtils.$().deleteWorkflow("test");
    }

    public void testGetFile() throws Exception {
        // System.out.println(GIT_UTILS.isFileExisted(GitUtils.DATASOURCE_PROJECT_ID,
        // "dmall/db_config"));
    }

    public void testCreateFile() throws Exception {
        // GIT_UTILS.createDatasourceFileOnline("dmall/db_config", "hello", "add
        // dmall");
    }

    public void testDeleteDb() throws Exception {
        // GIT_UTILS.deleteDb("dmall6");
    }

    public void testCreateBranch() throws Exception {
        // GIT_UTILS.createWorkflowBarnch("hello1");
    }

    public void testDeleteBranch() throws Exception {
        // GIT_UTILS.deleteWorkflowBranch("test1");
    }

    public void testCreateMergeRequest() throws Exception {
        // GIT_UTILS.createMergeRequest(
        // GitUtils.WORKFLOW_GIT_PROJECT_ID, "test1", "master", "merge test1");
    }

    public void testAcceptMergeRequest() throws Exception {
        // GIT_UTILS.acceptMergeRequest(GitUtils.WORKFLOW_GIT_PROJECT_ID, 19808, "merge
        // from test1");
    }

    public void testMergeWorkflowChange() throws Exception {
        // GIT_UTILS.mergeWorkflowChange("test2");
    }

    public void testCloseMergeRequest() throws Exception {
        // GIT_UTILS.closeMergeRequest(GitUtils.WORKFLOW_GIT_PROJECT_ID, 20238);
    }

    public void testRawFileJson() throws Exception {
        // System.out.println(GIT_UTILS.getRawFileJSON(GitUtils.WORKFLOW_GIT_PROJECT_ID,
        // "00d6f466b8d3525b7ca0b9b8a5ff0516fa8802c3", "test1"));
    }
    // public void testGetLatestSha() throws Exception {
    // System.out.println(GIT_UTILS.getLatestSha(GitUtils.WORKFLOW_GIT_PROJECT_ID));
    // }
}
