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

package com.qlangtech.tis.web.start;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.function.Supplier;

/**
 * TIS 的运行模式
 *
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2023-03-07 18:47
 **/
public enum TisRunMode {
    // 单节点运行 所有组件都运行在一个VM中
    Standalone(() -> {
        // return TisAppLaunch.get().isZeppelinActivate();
        // 单机版默认已经安装了
        return TisAppLaunch.get().isZeppelinContextInitialized();
    })
    // 本地测试，组件分布在多个VM中且IP端口不同
    , LocalTest(new Supplier<Boolean>() {
        boolean zeppelinContextInitialized = false;
        int tryCount = 0;

        @Override
        public Boolean get() {
            if (zeppelinContextInitialized) {
                return true;
            }
            if (tryCount > 100) {
                // 最多尝试100次
                return false;
            }
            try {
                URL test = new URL("http://127.0.0.1:" + TisSubModule.ZEPPELIN.getLaunchPort() + TisSubModule.ZEPPELIN.servletContext + "/api/version");
                HttpURLConnection conn = (HttpURLConnection) test.openConnection();
                tryCount++;
                if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    // throw new IllegalStateException("ERROR_CODE=" + conn.getResponseCode() + "  request faild, revsion center apply url :" + url);
                    return false;
                }
                return zeppelinContextInitialized = true;
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                return false;
            }
        }
    })
    // 分不同的节点运行
    , Distribute(() -> {
        // return TisAppLaunch.get().isZeppelinActivate();
        throw new UnsupportedOperationException();
    });

    public final Supplier<Boolean> zeppelinContextInitialized;

    private TisRunMode(Supplier<Boolean> zeppelinContextInitialized) {
        this.zeppelinContextInitialized = zeppelinContextInitialized;
    }
}
