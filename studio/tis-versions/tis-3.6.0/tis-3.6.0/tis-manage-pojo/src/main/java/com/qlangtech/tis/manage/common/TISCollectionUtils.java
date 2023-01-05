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

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringUtils;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2016年8月6日
 */
public class TISCollectionUtils {

    public static String INDEX_BACKFLOW_READED = "readed";

    public static String INDEX_BACKFLOW_ALL = "all";

    public static String NAME_PREFIX = "search4";

    private static final Pattern coreNamePattern = Pattern.compile(NAME_PREFIX + "(.+?)_shard(\\d+?)_replica_n(\\d+?)");

    public static class TisCoreName {

        private String name;

        private int sharedNo;

        private int replicaNo;

        public String getName() {
            return NAME_PREFIX + this.name;
        }

        public int getSharedNo() {
            return this.sharedNo;
        }

        public int getReplicaNo() {
            return this.replicaNo;
        }
    }

    public static TisCoreName parse(String corename) {
        TisCoreName coreName = new TisCoreName();
        Matcher coreNameMatcher = coreNamePattern.matcher(corename);
        if (!coreNameMatcher.matches()) {
            throw new IllegalArgumentException("core name:" + corename + " does not match pattern:" + coreNamePattern);
        }
        coreName.name = coreNameMatcher.group(1);
        coreName.sharedNo = Integer.parseInt(coreNameMatcher.group(2));
        coreName.replicaNo = Integer.parseInt(coreNameMatcher.group(3));
        return coreName;
    }
}
