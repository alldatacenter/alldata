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
package com.qlangtech.tis.coredefine.module.action;

import com.google.common.collect.Lists;
//import org.apache.solr.common.cloud.Replica;
import java.util.List;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @create: 2020-07-06 15:16
 */
public class CollectionTopology {

    private List<Shared> shareds = Lists.newArrayList();

    public List<Shared> getShareds() {
        return this.shareds;
    }

    public void addShard(Shared shard) {
        this.shareds.add(shard);
    }

    public static class Shared {

        private final String name;

        public Shared(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

//        private List<Replica> replics = Lists.newArrayList();
//
//        public List<Replica> getReplics() {
//            return this.replics;
//        }
//
//        public void addReplic(Replica replic) {
//            this.replics.add(replic);
//        }
    }
}
