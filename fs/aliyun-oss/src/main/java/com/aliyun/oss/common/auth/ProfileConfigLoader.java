/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.aliyun.oss.common.auth;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import com.aliyun.oss.common.utils.AuthUtils;
import com.aliyun.oss.common.utils.IniEditor;

public class ProfileConfigLoader {
    
    public Map<String, String> loadProfile(File file) throws IOException {
        IniEditor iniProfile = new IniEditor();
        iniProfile.load(file);
        return iniProfile.getSectionMap(AuthUtils.DEFAULT_SECTION_NAME);
    }
    
}
