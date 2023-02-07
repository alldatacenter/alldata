/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.manager.client.api.impl;

import org.apache.inlong.manager.client.api.InlongGroup;
import org.apache.inlong.manager.client.api.InlongGroupContext;
import org.apache.inlong.manager.client.api.InlongStream;
import org.apache.inlong.manager.client.api.InlongStreamBuilder;
import org.apache.inlong.manager.pojo.group.InlongGroupCountResponse;
import org.apache.inlong.manager.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.pojo.group.InlongGroupTopicInfo;
import org.apache.inlong.manager.pojo.sort.BaseSortConf;
import org.apache.inlong.manager.pojo.stream.InlongStreamInfo;

import java.util.List;

/**
 * Check inlong group if is exists.
 */
public class BlankInlongGroup implements InlongGroup {

    @Override
    public InlongStreamBuilder createStream(InlongStreamInfo streamInfo) {
        throw new UnsupportedOperationException("Inlong group is not exists");
    }

    @Override
    public InlongGroupContext context() throws Exception {
        throw new UnsupportedOperationException("Inlong group is not exists");
    }

    @Override
    public InlongGroupContext context(String credentials) throws Exception {
        throw new UnsupportedOperationException("Inlong group is not exists");
    }

    @Override
    public InlongGroupContext init() throws Exception {
        throw new UnsupportedOperationException("Inlong group is not exists");
    }

    @Override
    public void update(InlongGroupInfo originGroupInfo, BaseSortConf sortConf) throws Exception {
        throw new UnsupportedOperationException("Inlong group is not exists");
    }

    @Override
    public void update(BaseSortConf sortConf) throws Exception {
        throw new UnsupportedOperationException("Inlong group is not exists");
    }

    @Override
    public InlongGroupContext reInitOnUpdate(InlongGroupInfo originGroupInfo, BaseSortConf sortConf) {
        throw new UnsupportedOperationException("Inlong group is not exists");
    }

    @Override
    public InlongGroupContext suspend() throws Exception {
        throw new UnsupportedOperationException("Inlong group is not exists");
    }

    @Override
    public InlongGroupContext suspend(boolean async) throws Exception {
        throw new UnsupportedOperationException("Inlong group is not exists");
    }

    @Override
    public InlongGroupContext restart() throws Exception {
        throw new UnsupportedOperationException("Inlong group is not exists");
    }

    @Override
    public InlongGroupContext restart(boolean async) throws Exception {
        throw new UnsupportedOperationException("Inlong group is not exists");
    }

    @Override
    public InlongGroupContext delete() throws Exception {
        throw new UnsupportedOperationException("Inlong group is not exists");
    }

    @Override
    public InlongGroupContext delete(boolean async) throws Exception {
        throw new UnsupportedOperationException("Inlong group is not exists");
    }

    @Override
    public List<InlongStream> listStreams() throws Exception {
        throw new UnsupportedOperationException("Inlong group is not exists");
    }

    @Override
    public InlongGroupContext reset(int rerun, int resetFinalStatus) throws Exception {
        throw new UnsupportedOperationException("Inlong group is not exists");
    }

    @Override
    public InlongGroupCountResponse countGroupByUser() throws Exception {
        throw new UnsupportedOperationException("Inlong group is not exists");
    }

    @Override
    public InlongGroupTopicInfo getTopic(String id) throws Exception {
        throw new UnsupportedOperationException("Inlong group is not exists");
    }
}
