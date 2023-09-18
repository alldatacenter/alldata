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

package com.qlangtech.tis.plugin.datax;

import com.qlangtech.tis.realtime.utils.NetUtils;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Collections;
import java.util.Set;


/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-10-15 13:25
 **/
public class FTPContainer //extends GenericContainer {
        extends FixedHostPortGenericContainer {

    public static final DockerImageName FTP_DOCKER_IMAGE_NAME = DockerImageName.parse(
            "fauria/vsftpd"
    );

    private static final int PORT21 = 21;
    private static final int PORT20 = 20;

    private static final int MIN_PASV_PORT = 21100;
    private static final int MAX_PAV_PORT = 21110;

    public static final String USER_NAME = "test";
    public static final String PASSWORD = "test";

    public FTPContainer() {
        super("fauria/vsftpd");
        this.addExposedPorts(new int[]{PORT21});
        // this.addExposedPorts(new int[]{PORT20});
        for (int i = MIN_PASV_PORT; i <= MAX_PAV_PORT; i++) {
            // this.withFixedExposedPort(i, i);
            this.addExposedPorts(new int[]{i});
        }
        this.addEnv("FTP_USER", USER_NAME);
        this.addEnv("FTP_PASS", PASSWORD);
        this.addEnv("PASV_MIN_PORT", String.valueOf(MIN_PASV_PORT));
        this.addEnv("PASV_MAX_PORT", String.valueOf(MAX_PAV_PORT));
        this.addEnv("PASV_ADDRESS", NetUtils.getHost());
    }

    @Override
    public Set<Integer> getLivenessCheckPortNumbers() {
        // 启动的时候有一些端口（MIN_PASV_PORT-MAX_PAV_PORT）还没有打开，不需要校验
        return Collections.emptySet();
    }

    public int getPort21() {
        return this.getMappedPort(PORT21);
    }

}
