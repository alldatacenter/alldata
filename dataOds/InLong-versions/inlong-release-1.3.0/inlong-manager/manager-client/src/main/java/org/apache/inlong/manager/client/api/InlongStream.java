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

package org.apache.inlong.manager.client.api;

import org.apache.inlong.manager.pojo.sink.StreamSink;
import org.apache.inlong.manager.pojo.source.StreamSource;
import org.apache.inlong.manager.pojo.stream.StreamField;
import org.apache.inlong.manager.pojo.stream.StreamPipeline;
import org.apache.inlong.manager.pojo.stream.StreamTransform;

import java.util.List;
import java.util.Map;

/**
 * Inlong stream.
 */
public interface InlongStream {

    /**
     * Return name of stream.
     */
    String getInlongStreamId();

    /**
     * Return field definitions of stream.
     */
    List<StreamField> getStreamFields();

    /**
     * Return data sources defined in stream, key is source name which must be unique within one stream scope.
     */
    Map<String, StreamSource> getSources();

    /**
     * Return sinks defined in stream, key is sink name which must be unique within one stream scope.
     */
    Map<String, StreamSink> getSinks();

    /**
     * Get detail info of data sink by id.
     */
    StreamSink getSinkInfoById(Integer sinkId);

    /**
     * Get detail info of data sink by name.
     */
    StreamSink getSinkInfoByName(String sinkName);

    /**
     * Return data transform node defined in stream(split, string replace etc.)
     * key is transform name which must be unique within one stream scope.
     */
    Map<String, StreamTransform> getTransforms();

    /**
     * Add data source to stream, this method will throw exception when source name already exists in stream.
     */
    InlongStream addSource(StreamSource source);

    /**
     * Add sink to stream, this method will throw exception when sink name already exists in stream.
     */
    InlongStream addSink(StreamSink streamSink);

    /**
     * Add data transform node to stream, this method will throw exception when transform name already exists in stream.
     */
    InlongStream addTransform(StreamTransform transform);

    /**
     * Delete data source by source name.
     */
    InlongStream deleteSource(String sourceName);

    /**
     * Delete data sink by sink name.
     */
    InlongStream deleteSink(String sinkName);

    /**
     * Delete data transform node by transform name.
     */
    InlongStream deleteTransform(String transformName);

    /**
     * Update data source by source name, add new one if source name not exists.
     */
    InlongStream updateSource(StreamSource source);

    /**
     * Update sink by sink name, add new one if sink name not exists.
     */
    InlongStream updateSink(StreamSink streamSink);

    /**
     * Update data transform node by transform name, add new one if transform name not exists.
     */
    InlongStream updateTransform(StreamTransform transform);

    /**
     * Create stream pipeline by sources, transforms, sinks defined in stream.
     */
    StreamPipeline createPipeline();

    /**
     * Update stream definition in manager service, which must be invoked after add/delete/update source/sink/transform.
     */
    InlongStream update();

    StreamSource getSourceById(int sourceId);

}
