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

package com.qlangtech.tis.sql.parser;

import com.google.common.collect.Sets;

import java.io.File;
import java.util.Set;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-10-20 17:10
 **/
public interface IDBNodeMeta {
   public static Set<String> appendDBDependenciesClasspath(Set<IDBNodeMeta> dependencyDBNodes) {
        Set<String> classpathElements = Sets.newHashSet();
        for (IDBNodeMeta db : dependencyDBNodes) {
            File jarFile = new File(db.getDaoDir(), db.getDbName() + "-dao.jar");
            if (!jarFile.exists()) {
                throw new IllegalStateException("jarfile:" + jarFile.getAbsolutePath() + " is not exist");
            }
            classpathElements.add(jarFile.getAbsolutePath());
        }
        return classpathElements;
    }

    // getDaoDir(), db.getDbName()
    File getDaoDir();

    String getDbName();
}
