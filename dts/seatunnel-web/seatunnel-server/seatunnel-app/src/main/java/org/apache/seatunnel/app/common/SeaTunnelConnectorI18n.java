/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.seatunnel.app.common;

import org.apache.seatunnel.shade.com.typesafe.config.Config;
import org.apache.seatunnel.shade.com.typesafe.config.ConfigFactory;

import org.apache.commons.io.IOUtils;

import java.nio.charset.StandardCharsets;

public class SeaTunnelConnectorI18n {
    public static Config CONNECTOR_I18N_CONFIG_EN;
    public static Config CONNECTOR_I18N_CONFIG_ZH;

    static {
        try {
            CONNECTOR_I18N_CONFIG_EN =
                    ConfigFactory.parseString(
                            IOUtils.toString(
                                    SeaTunnelConnectorI18n.class.getResourceAsStream(
                                            "/i18n_en.config"),
                                    StandardCharsets.UTF_8));
            CONNECTOR_I18N_CONFIG_ZH =
                    ConfigFactory.parseString(
                            IOUtils.toString(
                                    SeaTunnelConnectorI18n.class.getResourceAsStream(
                                            "/i18n_zh.config"),
                                    StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
