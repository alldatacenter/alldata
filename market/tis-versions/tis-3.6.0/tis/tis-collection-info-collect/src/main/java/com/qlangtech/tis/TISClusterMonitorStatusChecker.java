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
package com.qlangtech.tis;

import com.qlangtech.tis.health.check.IStatusChecker;
import com.qlangtech.tis.health.check.Mode;
import com.qlangtech.tis.health.check.StatusModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.ServletContextAware;

import javax.servlet.ServletContext;



/**
 * 判断tis集群增量统计工作是否正常进行中
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2016年3月1日
 */
public class TISClusterMonitorStatusChecker implements IStatusChecker, ServletContextAware {

    private ServletContext servletContext;

    private static final Logger logger = LoggerFactory.getLogger("check_health");

    private TSearcherClusterInfoCollect tisClusterInfoCollect;

    // private ZkStateReader zkStateReader;

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    @Override
    public StatusModel check() {
        throw new UnsupportedOperationException();
//        logger.info("do check_health");
//        StatusModel stateModel = new StatusModel();
//        stateModel.level = StatusLevel.OK;
//        try {
//            zkStateReader.getZkClient().getChildren("/", null, true);
//            logger.info("zk is regular");
//        } catch (Exception e) {
//            logger.error("zk is unhealth!!!!!!", e);
//            stateModel.level = StatusLevel.FAIL;
//            stateModel.message = ExceptionUtils.getMessage(e);
//            return stateModel;
//        }
//        // ////////////////////////////////////////////////////////////////
//        final long lastCollectTimeStamp = tisClusterInfoCollect.getLastCollectTimeStamp();
//        if (System.currentTimeMillis() > (lastCollectTimeStamp + (TSearcherClusterInfoCollect.COLLECT_STATE_INTERVAL * 2 * 1000))) {
//            stateModel.level = StatusLevel.FAIL;
//            SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm");
//            stateModel.message = "from:" + timeFormat.format(new Date(lastCollectTimeStamp)) + " have not collect cluster status";
//            logger.info("cluster collect has error:" + stateModel.message);
//            logger.info("System.currentTimeMillis():" + System.currentTimeMillis());
//            logger.info("lastCollectTimeStamp:" + lastCollectTimeStamp);
//        } else {
//            logger.info("cluster collect is regular");
//        }
//        return stateModel;
    }

    @Override
    public void init() {
        throw new UnsupportedOperationException();
//        ApplicationContext appContext = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);
//        this.tisClusterInfoCollect = appContext.getBean("clusterInfoCollect", TSearcherClusterInfoCollect.class);
//        this.zkStateReader = appContext.getBean("solrClient", ZkStateReader.class);
    }

    @Override
    public Mode mode() {
        return Mode.PUB;
    }

    @Override
    public int order() {
        return 0;
    }
    // public static void main(String[] args) throws Exception {
    // ClassLoader loader = Thread.currentThread().getContextClassLoader();
    // Enumeration<URL> urls = loader.getResources("META-INF/services/" + StatusChecker.class.getName());
    // while (urls.hasMoreElements()) {
    // System.out.println(urls.nextElement());
    // }
    // }
}
