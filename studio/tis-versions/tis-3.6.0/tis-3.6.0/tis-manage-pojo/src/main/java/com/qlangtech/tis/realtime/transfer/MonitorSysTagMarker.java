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
package com.qlangtech.tis.realtime.transfer;

import java.io.InputStream;
import java.net.URL;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;
import com.qlangtech.tis.manage.common.ConfigFileContext;
import com.qlangtech.tis.manage.common.PostFormStreamProcess;

/**
 * 监控系统标签标注器，会为各种metric的指标打标签
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2017年2月18日
 */
public abstract class MonitorSysTagMarker {

    public static final int FalconSendTimeStep = 60;

    /**
     * 指标名称
     *
     * @param metricName
     * @return
     */
    public abstract boolean match(String metricName);

    public abstract boolean shallCollectByMonitorSystem();

    public abstract String getTags(String metricName);

    public abstract String getFalconMetric();

    public static MonitorSysTagMarker[] createMonitorSysTagMarker(final ITableFocuseGetter tableGetter) {
        if (tableGetter == null) {
            throw new IllegalArgumentException("param 'tableFocuse' can not be null");
        }
        return new MonitorSysTagMarker[] { new ConsumeTagMarker(), new SolrUpdateTagMarker(), new TableTagMarker() {

            @Override
            public boolean match(String metricName) {
                return tableGetter.getFocusedTabs().contains(metricName);
            }
        }, new LackSolrRecordTagMarker(), new DefaultMonitorSysTagMarker() };
    }

    public static MonitorSysTagMarker[] createMonitorSysTagMarker(Set<String> focuse) {
        return new MonitorSysTagMarker[] {};
    }

    private static class DefaultMonitorSysTagMarker extends MonitorSysTagMarker {

        @Override
        public boolean match(String metricName) {
            return true;
        }

        @Override
        public boolean shallCollectByMonitorSystem() {
            return false;
        }

        @Override
        public String getTags(String metricName) {
            return null;
        }

        @Override
        public String getFalconMetric() {
            return "default";
        }
    }

    private static class ConsumeTagMarker extends MonitorSysTagMarker {

        private static final String TAGS = "type=";

        private static final String FalconMetric = "consume";

        private static final Pattern p = Pattern.compile("consume\\d+");

        @Override
        public String getFalconMetric() {
            return FalconMetric;
        }

        @Override
        public boolean shallCollectByMonitorSystem() {
            return false;
        }

        @Override
        public boolean match(String metricName) {
            Matcher m = p.matcher(metricName);
            return m.matches();
        }

        @Override
        public String getTags(String metricName) {
            return TAGS + metricName;
        }
    }

    private abstract static class TableTagMarker extends MonitorSysTagMarker {

        private static final String TAGS = "tab=";

        private static final String FalconMetric = "tabs";

        @Override
        public String getFalconMetric() {
            return FalconMetric;
        }

        @Override
        public boolean shallCollectByMonitorSystem() {
            return true;
        }

        @Override
        public String getTags(String metricName) {
            return TAGS + metricName;
        }
    }

    public static final String KEY_LACK_SOLR_RECORD = "lackSolrRecord";

    private static final String KEY_LACK_SOLR_RECORD_METRIC = "lackrecord";

    private static class LackSolrRecordTagMarker extends MonitorSysTagMarker {

        @Override
        public boolean match(String metricName) {
            return KEY_LACK_SOLR_RECORD.equals(metricName);
        }

        @Override
        public String getFalconMetric() {
            return KEY_LACK_SOLR_RECORD_METRIC;
        }

        @Override
        public boolean shallCollectByMonitorSystem() {
            return true;
        }

        @Override
        public String getTags(String metricName) {
            // TAGS + metricName;
            return null;
        }
    }

    private static class SolrUpdateTagMarker extends MonitorSysTagMarker {

        // private static final String TARGET_NAME = "solrConsume";
        private static final String FalconMetric = "solrupdate";

        @Override
        public boolean match(String metricName) {
            return IIncreaseCounter.SOLR_CONSUME_COUNT.equals(metricName);
        }

        @Override
        public String getFalconMetric() {
            return FalconMetric;
        }

        @Override
        public boolean shallCollectByMonitorSystem() {
            return true;
        }

        @Override
        public String getTags(String metricName) {
            return null;
        }
    }

    public static JSONObject addMetric(String hostName, long timestamp, String collection, String metricName, long value, CounterType counterType) {
        return addMetric(hostName, timestamp, collection, metricName, value, counterType, null);
    }

    public static JSONObject addMetric(String hostName, long timestamp, String collection, String metricName, long value, CounterType counterType, IIncreaseCounter counter) {
        MonitorSysTagMarker tagMarker = null;
        JSONObject o = new JSONObject();
        if (counter != null && (tagMarker = counter.getMonitorTagMarker()) != null) {
            o.put("metric", tagMarker.getFalconMetric());
        } else {
            o.put("metric", metricName);
        }
        o.put("endpoint", hostName);
        o.put("timestamp", timestamp);
        o.put("value", value);
        o.put("step", MonitorSysTagMarker.FalconSendTimeStep);
        // COUNTER or GAUGE
        o.put("counterType", counterType.getValue());
        String tags = "index=" + collection;
        String tag = null;
        if (tagMarker != null && (tag = tagMarker.getTags(metricName)) != null) {
            tags += ("," + tag);
        }
        o.put("tags", tags);
        return o;
    }

    public static void main(String[] args) throws Exception {
    // int increase = 0;
    // for (int i = 0; i < 3; i++) {
    // final long timestamp = System.currentTimeMillis() / 1000;
    // JSONArray result = new org.json.JSONArray();
    // increase += (3000 * Math.random());
    // result.put(addMetric("125.0.0.1", timestamp, "search4xxx search4kkkk", "tis_fullgc", increase,
    // CounterType.COUNTER));
    // final String content = result.toString();
    // System.out.println(content);
    // ConfigFileContext.processContent(new URL("http://127.0.0.1:1988/v1/push"), content,
    // new PostFormStreamProcess<Object>() {
    // @Override
    // public Object p(int status, InputStream stream, String md5) {
    // return null;
    // }
    // });
    //
    // System.out.println("increae:" + increase);
    // Thread.sleep(MonitorSysTagMarker.FalconSendTimeStep * 1000);
    // HttpUtils.post(, content.getBytes(Charset.forName("utf8")),
    // new PostFormStreamProcess<Object>() {
    // @Override
    // public Object p(int status, InputStream stream, String md5) {
    // return null;
    // }
    // });
    // }
    }
}
