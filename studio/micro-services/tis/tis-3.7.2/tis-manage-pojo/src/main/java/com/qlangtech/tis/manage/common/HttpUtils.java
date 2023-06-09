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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.qlangtech.tis.ajax.AjaxResult;
import com.qlangtech.tis.manage.common.ConfigFileContext.HTTPMethod;
import com.qlangtech.tis.manage.common.ConfigFileContext.StreamProcess;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2014年11月4日上午11:24:38
 */
public class HttpUtils {


    public interface IMsgProcess {
        public void err(String content);
    }

    // =================================================================
    // >> 测试环境使用
    public static void addMockGlobalParametersConfig() {
        addMockApply(1, "global_parameters_config_action", "global_params.json");
    }

    public static void addMockApply(int order, String testStr, String classpath) {
        addMockApply(order, testStr, classpath, HttpUtils.class);
    }

    public static CacheMockRes addMockApply(int order, String testStr, String classpath, Class<?> clazz) {
        return addMockApply(order, testStr, new ClasspathRes(order, classpath, clazz));
    }

    public static void addMockApply(String testStr, IClasspathRes classpathRes) {
        addMockApply(-1, testStr, classpathRes);
    }

    public static CacheMockRes addMockApply(int orderIndex, MockMatchKey test, IClasspathRes classpathRes) {
        if (HttpUtils.mockConnMaker == null) {
            HttpUtils.mockConnMaker = new DefaultMockConnectionMaker();
        }
        CacheMockRes cacheMockRes = mockConnMaker.resourceStore.get(test);
        if (cacheMockRes == null) {
            cacheMockRes = new CacheMockRes();
            mockConnMaker.resourceStore.put(test, cacheMockRes);
        }
        if (orderIndex < 0) {
            cacheMockRes.resources.add(classpathRes);
        } else {
            while (orderIndex > (cacheMockRes.resources.size() - 1)) {
                cacheMockRes.resources.add(null);
            }
            cacheMockRes.resources.set(orderIndex, classpathRes);
        }
        return cacheMockRes;
    }

    public static CacheMockRes addMockApply(int orderIndex, String testStr, IClasspathRes classpathRes) {
        return addMockApply(orderIndex, new MockMatchKey(testStr, false, false), classpathRes);
    }

    public static DefaultMockConnectionMaker mockConnMaker;

    public static <T> T processContent(String urlString, StreamProcess<T> process) {
        try {
            URL url = new URL(urlString);
            return ConfigFileContext.processContent(url, process);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T get(URL url, StreamProcess<T> process) {
        return ConfigFileContext.processContent(url, process);
    }

    public static <T> T processContent(URL url, StreamProcess<T> process) {
        return ConfigFileContext.processContent(url, process);
    }

    public static <T> T processContent(URL url, StreamProcess<T> process, int maxRetry) {
        return ConfigFileContext.processContent(url, process, maxRetry);
    }

    public static <T> T post(URL url, byte[] content, PostFormStreamProcess<T> process) {
        return ConfigFileContext.processContent(url, content, process);
    }

    public static <T> T post(URL url, String content, PostFormStreamProcess<T> process) {
        return ConfigFileContext.processContent(url, content, process);
    }

    public static <T> T post(URL url, List<PostParam> params, PostFormStreamProcess<T> process) {
        return process(url, params, process, HTTPMethod.POST);
    }

    public static <T> T process(URL url, List<PostParam> params, PostFormStreamProcess<T> process, HTTPMethod httpMethod) {
        return ConfigFileContext.processContent(url, process, httpMethod, process.getContentType().serializeParams(params), 1);
    }

    public static <T> T post(String urlString, List<PostParam> params, PostFormStreamProcess<T> process) {
        try {
            URL url = new URL(urlString);
            return post(url, params, process);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T delete(String urlString, List<PostParam> params, PostFormStreamProcess<T> process) {
        try {
            return process(new URL(urlString), params, process, HTTPMethod.DELETE);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T put(String urlString, List<PostParam> params, PostFormStreamProcess<T> process) {
        try {
            return process(new URL(urlString), params, process, HTTPMethod.PUT);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> AjaxResult<T> soapRemote(final String url, Class<T> clazz) {
        return soapRemote(url, Collections.emptyList(), clazz, true);
    }

    public static <T> AjaxResult<T> soapRemote(final String url, List<PostParam> params, Class<T> clazz) {
        return soapRemote(url, params, clazz, true);
    }

    /**
     * 向console节点发送http请求，并且按照console节点返回信息的格式反序列化
     *
     * @param url
     * @param params
     * @param clazz
     * @param errorShortCircuit
     * @return
     */
    public static <T> AjaxResult<T> soapRemote(final String url, List<PostParam> params, Class<T> clazz, boolean errorShortCircuit) {
        return HttpUtils.post(url, params, new PostFormStreamProcess<AjaxResult<T>>() {

            @Override
            public ContentType getContentType() {
                return ContentType.Application_x_www_form_urlencoded;
            }

            @Override
            public AjaxResult<T> p(int status, InputStream stream, Map<String, List<String>> headerFields) {
                AjaxResult<T> r = new AjaxResult<>();
                JSONObject result = null;
                try {
                    result = JSON.parseObject(IOUtils.toString(stream, TisUTF8.get()));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                JSONArray errors = null;
                JSONArray msgs = null;
                r.setSuccess(result.getBoolean(IAjaxResult.KEY_SUCCESS));
                if (!r.isSuccess()) {
                    errors = result.getJSONArray(IAjaxResult.KEY_ERROR_MSG);
                    r.setErrormsg(errors.stream().map((rr) -> {
                        return (String) rr;
                    }).collect(Collectors.toList()));
                    if (errorShortCircuit) {
                        throw new IllegalStateException(Joiner.on("\n").join(r.getErrormsg()));
                    }
                }
                msgs = result.getJSONArray(IAjaxResult.KEY_MSG);
                r.setMsg(msgs.stream().map((m) -> (String) m).collect(Collectors.toList()));
                if (clazz == Void.class) {
                    return r;
                }
                if (result.containsKey(IAjaxResult.KEY_BIZRESULT)) {
                    r.setBizresult(result.getObject(IAjaxResult.KEY_BIZRESULT, clazz));
                }
                return r;
            }
        });
    }

    public static class PostParam {

        private final String key;

        private final Object value;

        public PostParam(String key, Object value) {
            super();
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return this.key;
        }

        public String getValue() {
            return String.valueOf(this.value);
        }
    }


    public static class MockMatchKey {
        private final String matchTxt;
        private final boolean startWith;
        private final boolean endWith;

        public MockMatchKey(String matchTxt, boolean startWith, boolean endWith) {
            this.matchTxt = matchTxt;
            this.startWith = startWith;
            this.endWith = endWith;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MockMatchKey that = (MockMatchKey) o;
            return Objects.equals(matchTxt, that.matchTxt);
        }

        @Override
        public int hashCode() {
            return Objects.hash(matchTxt);
        }
    }

    public static class DefaultMockConnectionMaker implements MockConnectionMaker {

        private final Map<MockMatchKey, CacheMockRes> /*** url test  */
                resourceStore = Maps.newHashMap();

        public void clearStubs() {
            this.resourceStore.clear();
        }

        @Override
        public MockHttpURLConnection create(URL url, List<ConfigFileContext.Header> heads, HTTPMethod method, byte[] content) {
            MockMatchKey matchKey = null;
            for (Map.Entry<MockMatchKey, CacheMockRes> entry : resourceStore.entrySet()) {
                matchKey = entry.getKey();
                if (matchKey.startWith) {
                    if (StringUtils.startsWith(url.toString(), matchKey.matchTxt)) {
                        return createConnection(url, heads, method, content, entry.getValue().get());
                    }
                    // return null;
                } else if (matchKey.endWith) {
                    if (StringUtils.endsWith(url.toString(), matchKey.matchTxt)) {
                        return createConnection(url, heads, method, content, entry.getValue().get());
                    }
                    // return null;
                } else if (StringUtils.indexOf(url.toString(), matchKey.matchTxt) > -1) {
                    return createConnection(url, heads, method, content, entry.getValue().get());
                }
            }
            return null;
        }

        protected MockHttpURLConnection createConnection(URL url, List<ConfigFileContext.Header> heads
                , HTTPMethod method, byte[] content, IClasspathRes cpRes) {
            return new MockHttpURLConnection(cpRes.getResourceAsStream(url), cpRes.headerFields());
        }
    }

    public static class CacheMockRes {
        // 测试每访问一次递增1
        int getIndex = 0;
        List<IClasspathRes> resources = Lists.newArrayList();
        IClasspathRes lastElemnet = null;

        int resFetchCount = 0;

        public boolean verfiyResHasFetch() {
            return resFetchCount > 0;
        }

        IClasspathRes get() {
            IClasspathRes res = null;
            if (getIndex >= resources.size()) {
                res = this.lastElemnet;
            } else {
                res = (lastElemnet = resources.get(getIndex++));
            }
            IClasspathRes finalRes = res;
            return new IClasspathRes() {
                @Override
                public InputStream getResourceAsStream(URL url) {
                    resFetchCount++;
                    return finalRes.getResourceAsStream(url);
                }

                @Override
                public Map<String, List<String>> headerFields() {
                    return finalRes.headerFields();
                }
            };
        }
    }

    public interface IClasspathRes {
        InputStream getResourceAsStream(URL url);

        default Map<String, List<String>> headerFields() {
            return Collections.emptyMap();
        }
    }

    public static class ClasspathRes implements IClasspathRes {

        public final int order;

        private final String classpath;

        private final Class<?> relClass;

        public ClasspathRes(int order, String classpath, Class<?> relClass) {
            this.classpath = classpath;
            this.relClass = relClass;
            this.order = order;
        }

        public InputStream getResourceAsStream(URL url) {
            return getResourceAsStream(classpath);
        }

        public InputStream getResourceAsStream(String cpResName) {
            return this.relClass.getResourceAsStream(cpResName);
        }
    }

    public static abstract class LatestUpdateTimestampClasspathRes implements IClasspathRes {
        @Override
        public Map<String, List<String>> headerFields() {
            return ImmutableMap.of(ConfigFileContext.KEY_HEAD_LAST_UPDATE, Lists.newArrayList(String.valueOf(System.currentTimeMillis())));
        }
    }
}
