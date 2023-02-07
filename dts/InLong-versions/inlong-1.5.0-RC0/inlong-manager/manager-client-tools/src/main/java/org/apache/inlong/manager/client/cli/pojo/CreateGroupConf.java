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

package org.apache.inlong.manager.client.cli.pojo;

import lombok.Data;
import org.apache.inlong.manager.client.api.transform.MultiDependencyTransform;
import org.apache.inlong.manager.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.pojo.sink.StreamSink;
import org.apache.inlong.manager.pojo.source.StreamSource;
import org.apache.inlong.manager.pojo.stream.InlongStreamInfo;
import org.apache.inlong.manager.pojo.stream.StreamField;

import java.util.List;

/**
 * The config of group, including inlong stream, stream source, stream sink, etc.
 */
@Data
public class CreateGroupConf {

    private InlongGroupInfo groupInfo;
    private InlongStreamInfo streamInfo;
    private MultiDependencyTransform streamTransform;
    private List<StreamField> streamFieldList;
    private StreamSource streamSource;
    private StreamSink streamSink;

}
