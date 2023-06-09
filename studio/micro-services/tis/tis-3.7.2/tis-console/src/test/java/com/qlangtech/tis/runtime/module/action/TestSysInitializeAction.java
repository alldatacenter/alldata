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
package com.qlangtech.tis.runtime.module.action;

import com.qlangtech.tis.manage.common.Config;
import com.qlangtech.tis.manage.spring.TISDataSourceFactory;
import com.qlangtech.tis.pubhook.common.RunEnvironment;
import com.qlangtech.tis.test.TISEasyMock;
import junit.framework.TestCase;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.easymock.EasyMock;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2021-01-06 18:03
 */
public class TestSysInitializeAction extends TestCase implements TISEasyMock {

  @Override
  public void setUp() throws Exception {
    super.setUp();
    this.clearMocks();
  }

  //  public void testInitializeAppAndSchema() throws Exception {
//    request.setParameter("emethod", "init");
//    request.setParameter("action", "sys_initialize_action");
//
//    ActionProxy proxy = getActionProxy("/runtime/index_query.ajax");
//    assertNotNull(proxy);
//    SysInitializeAction initAction = (SysInitializeAction) proxy.getAction();
//    assertNotNull(initAction);
//    initAction.initializeAppAndSchema();
//
//  }

  /**
   * 测试系统数据库初始化
   */
  public void testSystemDBInitializWithDerby() throws Exception {
    File initialSuccessToken = SysInitializeAction.getSysInitializedTokenFile();
    FileUtils.deleteQuietly(initialSuccessToken);
    final String tis_ansible_home = StringUtils.defaultIfEmpty(System.getenv("tis_ansible_home"), "/opt/misc/tis-ansible");
    String[] args = new String[]{tis_ansible_home + "/tis_console_derby.sql", Config.DB_TYPE_DERBY};
    Config config = this.mock("config", Config.class);


    EasyMock.expect(config.getZkHost()).andReturn("192.168.28.200:2181/tis/cloud");
    EasyMock.expect(config.getRuntime()).andReturn(RunEnvironment.DAILY.getKeyName()).anyTimes();
    Config.TisDbConfig mockDBType = new Config.TisDbConfig();
    mockDBType.dbtype = Config.DB_TYPE_DERBY;
    mockDBType.dbname = "tis_console_db";
    File dbDir = new File(Config.getDataDir(), mockDBType.dbname);
    FileUtils.deleteQuietly(dbDir);
    EasyMock.expect(config.getDbConfig()).andReturn(mockDBType).anyTimes();
    Config.setConfig(config);

    replay();
    int[] tryIndex = new int[1];
    AtomicBoolean hasExecDSCreateInspector = new AtomicBoolean();
    TISDataSourceFactory.dsCreateInspector = new TISDataSourceFactory.IDSCreatorInspect() {
      @Override
      public void checkDataSource(boolean getDSFromJNDI, BasicDataSource dataSource) {
        assertFalse(getDSFromJNDI);
        if (tryIndex[0]++ == 0) {
          assertEquals("jdbc:derby:" + mockDBType.dbname + ";create=true", dataSource.getUrl());
        } else {
          assertEquals("jdbc:derby:" + mockDBType.dbname + ";create=false", dataSource.getUrl());
        }

        hasExecDSCreateInspector.set(true);
      }
    };

    SysInitializeAction.main(args);
    assertTrue("hasExecDSCreateInspector must be true", hasExecDSCreateInspector.get());
    // File initialSuccessToken = SysInitializeAction.getSysInitializedTokenFile();
    assertTrue("initialSuccessToken fiel:" + initialSuccessToken.getAbsolutePath(), initialSuccessToken.exists());
    assertEquals("create db 2 times", tryIndex[0], 2);
    assertTrue("dbDir must exist:" + dbDir.getAbsolutePath(), dbDir.exists());
    verifyAll();
  }


//  public void testZKInitialize() {
//    SysInitializeAction sysInitializeAction = new SysInitializeAction();
//    assertTrue(sysInitializeAction.initializeZkPath("192.168.28.201:2181/tis/cloud"));
//  }

}
