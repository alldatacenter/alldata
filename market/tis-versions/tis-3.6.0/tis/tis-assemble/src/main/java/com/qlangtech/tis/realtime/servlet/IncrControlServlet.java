///**
// * Copyright (c) 2020 QingLang, Inc. <baisui@qlangtech.com>
// *
// * This program is free software: you can use, redistribute, and/or modify
// * it under the terms of the GNU Affero General Public License, version 3
// * or later ("AGPL"), as published by the Free Software Foundation.
// *
// * This program is distributed in the hope that it will be useful, but WITHOUT
// * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// * FITNESS FOR A PARTICULAR PURPOSE.
// *
// * You should have received a copy of the GNU Affero General Public License
// * along with this program. If not, see <http://www.gnu.org/licenses/>.
// */
//package com.qlangtech.tis.realtime.servlet;
//
//import com.alibaba.fastjson.JSONObject;
//import com.qlangtech.tis.grpc.LaunchReportInfoEntry;
//import com.qlangtech.tis.grpc.TopicInfo;
//import com.qlangtech.tis.realtime.yarn.rpc.JobType;
//import com.qlangtech.tis.rpc.server.IncrStatusUmbilicalProtocolImpl;
//import org.apache.commons.lang.StringUtils;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import javax.servlet.ServletConfig;
//import javax.servlet.ServletException;
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//import java.io.IOException;
//import java.util.Map;
//
///**
// * 控制增量node启停
// *
// * @author 百岁（baisui@qlangtech.com）
// * @date 2016年4月13日
// */
//public class IncrControlServlet extends javax.servlet.http.HttpServlet {
//
//    private static final long serialVersionUID = 1L;
//
//    private static final Logger logger = LoggerFactory.getLogger(IncrControlServlet.class);
//
//    private IncrStatusUmbilicalProtocolImpl incrStatusUmbilicalProtocol;
//
//    public IncrControlServlet() {
//    // this.incrStatusUmbilicalProtocol = incrStatusUmbilicalProtocol;
//    }
//
//    @Override
//    public void init(ServletConfig config) throws ServletException {
//        this.incrStatusUmbilicalProtocol = IncrStatusUmbilicalProtocolImpl.getInstance();
//    }
//
//    @Override
//    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
//        String collection = req.getParameter("collection");
//        if (StringUtils.isBlank(collection)) {
//            throw new ServletException("param collection:" + collection + " can not be null");
//        }
//        JobType jobTpe = JobType.parseJobType(req.getParameter("action"));
//        if (jobTpe == JobType.IndexJobRunning) {
//            boolean stop = Boolean.parseBoolean(req.getParameter("stop"));
//            if (stop) {
//                incrStatusUmbilicalProtocol.stop(collection);
//            } else {
//                incrStatusUmbilicalProtocol.resume(collection);
//            }
//            logger.info("collection:" + collection + " stop:" + stop);
//            wirteXml2Client(resp, true, "success execute");
//        } else if (jobTpe == JobType.QueryIndexJobRunningStatus) {
//            wirteXml2Client(resp, true, "success execute", incrStatusUmbilicalProtocol.getIndexJobRunningStatus(collection));
//        } else if (jobTpe == JobType.Collection_TopicTags_status) {
//            // curl -d"collection=search4totalpay&action=collection_topic_tags_status" http://localhost:8080/incr-control?collection=search4totalpay
//            final Map<String, Long> /* absolute count */
//            tagCountMap = this.incrStatusUmbilicalProtocol.getUpdateAbsoluteCountMap(collection);
//            wirteXml2Client(resp, true, StringUtils.EMPTY, tagCountMap);
//        } else if (jobTpe == JobType.ACTION_getTopicTags) {
//            // 取得增量任务的topic tags
//            TopicInfo topicInfo = this.incrStatusUmbilicalProtocol.getFocusTopicInfo(collection);
//            if (topicInfo == null) {
//                throw new IllegalStateException("collection:" + collection + " relevant topicInfo can not be null,please check incr process have launch properly");
//            }
//            com.qlangtech.tis.realtime.yarn.rpc.TopicInfo convert = new com.qlangtech.tis.realtime.yarn.rpc.TopicInfo();
//            for (LaunchReportInfoEntry info : topicInfo.getTopicWithTagsList()) {
//                for (String tagName : info.getTagNameList()) {
//                    convert.addTag(info.getTopicName(), tagName);
//                }
//            }
//            wirteXml2Client(resp, true, StringUtils.EMPTY, convert);
//        } else {
//            throw new ServletException("action:" + req.getParameter("action") + " is not illegal");
//        }
//    }
//
//    protected void wirteXml2Client(HttpServletResponse response, boolean success, String msg, Object... biz) {
//        try {
//            response.setContentType("text/json");
//            JSONObject json = new JSONObject();
//            json.put("success", success);
//            json.put("msg", msg);
//            for (int i = 0; i < biz.length; i++) {
//                json.put("biz", biz[i]);
//                break;
//            }
//            response.getWriter().write(json.toJSONString());
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }
//}
