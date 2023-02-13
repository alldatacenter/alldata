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

package com.qlangtech.tis.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-11-25 20:30
 **/
public class TisMetaProps {
    public Properties tisMetaProps;

    private static TisMetaProps instance;

    public static TisMetaProps getInstance() {
        if (instance == null) {
            synchronized (TisMetaProps.class) {
                if (instance == null) {
                    try {
                        try (InputStream reader = TisMetaProps.class.getResourceAsStream("/tis-meta")) {
                            Properties p = new Properties();
                            p.load(reader);
                            instance = new TisMetaProps(p);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return instance;
    }


    private TisMetaProps(Properties props) {
        this.tisMetaProps = props;
    }

    public String getVersion() {
        return this.tisMetaProps.getProperty("buildVersion");
    }
}
