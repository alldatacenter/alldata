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
package com.qlangtech.tis.realtime.yarn.rpc;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.qlangtech.tis.manage.common.HttpUtils;
import com.qlangtech.tis.manage.common.PostFormStreamProcess;
import com.qlangtech.tis.manage.common.TisUTF8;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2016年4月9日
 */
public enum JobType {

    IndexJobRunning(1, "JobRunning");
//    //
//    QueryIndexJobRunningStatus(2, "QueryIncrStatus"),
//    // incr process tags的状态
//    Collection_TopicTags_status(3, "collection_topic_tags_status"),
//    // 取得增量监听的tags
//    ACTION_getTopicTags(4, "get_topic_tags");

    public int getValue() {
        return value;
    }

    private final int value;

    private final String name;

    private JobType(int value, String name) {
        this.value = value;
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public static JobType parseJobType(int value) {
        // }
        for (JobType jt : JobType.values()) {
            if (jt.value == value) {
                return jt;
            }
        }
        throw new IllegalArgumentException("value:" + value + " is not illegal");
    }

    public static JobType parseJobType(String name) {
        for (JobType jt : JobType.values()) {
            if (jt.name.equals(name)) {
                return jt;
            }
        }
        // }
        throw new IllegalArgumentException("name:" + name + " is not illegal");
    }

    public <T> RemoteCallResult<T> assembIncrControlWithResult(
            String getAssembleHttpHost,
            String collectionName, List<HttpUtils.PostParam> extraParams, Class<T> clazz) throws MalformedURLException {
        return assembIncrControl(getAssembleHttpHost, collectionName, extraParams, clazz == null ? null : new IAssembIncrControlResult() {

            public T deserialize(JSONObject json) {
                return JSON.toJavaObject(json, clazz);
            }
        });
    }

    /**
     * relevant server class: IncrControlServlet
     *
     * @param extraParams
     * @return
     * @throws MalformedURLException
     */
    public <T> RemoteCallResult<T> assembIncrControl(String getAssembleHttpHost, String collectionName
            , List<HttpUtils.PostParam> extraParams, IAssembIncrControlResult deserialize) throws MalformedURLException {
        URL applyUrl = new URL(getAssembleHttpHost + "/incr-control");
        List<HttpUtils.PostParam> params = Lists.newArrayList();
        params.add(new HttpUtils.PostParam("collection", collectionName));
        params.add(new HttpUtils.PostParam("action", this.getName()));
        params.addAll(extraParams);
        return //
                HttpUtils.post(//
                        applyUrl, //
                        params, new PostFormStreamProcess<RemoteCallResult>() {

                            public RemoteCallResult p(int status, InputStream stream, Map<String, List<String>> headerFields) {
                                RemoteCallResult<T> result = new RemoteCallResult<>();
                                try {
                                    // System.out.println(IOUtils.toString(stream, TisUTF8.get()));
                                    com.alibaba.fastjson.JSONObject j = JSON.parseObject(IOUtils.toString(stream, TisUTF8.get()));
                                    result.success = j.getBoolean("success");
                                    result.msg = j.getString("msg");
                                    if (deserialize != null && j.containsKey(KEY_BIZ)) {
                                        // JSON.toJavaObject(j.getJSONObject(KEY_BIZ), clazz);
                                        result.biz = deserialize.deserialize(j.getJSONObject(KEY_BIZ));
                                    }
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                                return result;
                            }
                        });
    }

    public interface IAssembIncrControlResult {

        <T> T deserialize(JSONObject json);
    }

    private static final String KEY_BIZ = "biz";

    public static class RemoteCallResult<T> {

        public boolean success;

        public String msg;

        public T biz;
    }
}
