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

package com.qlangtech.tis.plugins.incr.flink.connector.starrocks;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-09-01 09:51
 **/
public class StarRocksContainer extends GenericContainer {

    public static final DockerImageName STARROCKS_DOCKER_IMAGE_NAME = DockerImageName.parse(
            "tis/starrocks"
            // "registry.cn-hangzhou.aliyuncs.com/tis/oracle-xe:18.4.0-slim"
    );

    private static final int FE_PORT = 9030;
    private static final int BE_PORT = 8030;
    private static final int BE_LOAD_PORT = 8040;

    public StarRocksContainer() {
        super(STARROCKS_DOCKER_IMAGE_NAME);
        this.addExposedPorts(new int[]{FE_PORT, BE_PORT, BE_LOAD_PORT});
    }

    public int getFePort() {
        return this.getMappedPort(FE_PORT);
    }

    public int getBePort() {
        return this.getMappedPort(BE_PORT);
    }

    public int getBeStreamLoadPort() {
        return this.getMappedPort(BE_LOAD_PORT);
    }
}
