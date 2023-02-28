///**
// *   Licensed to the Apache Software Foundation (ASF) under one
// *   or more contributor license agreements.  See the NOTICE file
// *   distributed with this work for additional information
// *   regarding copyright ownership.  The ASF licenses this file
// *   to you under the Apache License, Version 2.0 (the
// *   "License"); you may not use this file except in compliance
// *   with the License.  You may obtain a copy of the License at
// *
// *       http://www.apache.org/licenses/LICENSE-2.0
// *
// *   Unless required by applicable law or agreed to in writing, software
// *   distributed under the License is distributed on an "AS IS" BASIS,
// *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// *   See the License for the specific language governing permissions and
// *   limitations under the License.
// */
//package com.qlangtech.tis.manage.common;
//
//import org.apache.commons.io.IOUtils;
//import org.noggit.JSONParser;
//import org.noggit.ObjectBuilder;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import java.io.InputStream;
//import java.util.Map;
//
///**
// * @author 百岁（baisui@qlangtech.com）
// * @create: 2020-08-20 17:54
// */
//public class ProcessResponse {
//
//    private static final Logger logger = LoggerFactory.getLogger(ProcessResponse.class);
//
//    public boolean success;
//
//    public Object result;
//
//    public String respBody;
//
//    @SuppressWarnings("all")
//    public static ProcessResponse processResponse(InputStream stream, HttpUtils.IMsgProcess msgProcess) {
//        ProcessResponse result = new ProcessResponse();
//        Object resp = null;
//        String respBody = null;
//        try {
//            respBody = IOUtils.toString(stream, TisUTF8.get());
//            resp = ObjectBuilder.getVal(new JSONParser(respBody));
//            result.respBody = respBody;
//            result.result = resp;
//        } catch (Exception pe) {
//            throw new RuntimeException("Expected JSON response from server but received: " + respBody
//              + "\nTypically, this indicates a problem with the Solr server; check the Solr server logs for more information.");
//        }
//        Map<String, Object> json = null;
//        if (resp != null && resp instanceof Map) {
//            json = (Map<String, Object>) resp;
//        } else {
//            throw new RuntimeException("Expected JSON object in response but received " + resp);
//        }
//        Long statusCode = asLong("/responseHeader/status", json);
//        if (statusCode == -1) {
//            // addErrorMessage(context, "Unable to determine outcome of GET
//            // request! Response: " + json);
//            msgProcess.err("Unable to determine outcome of GET request! Response: " + json);
//            result.success = false;
//            return result;
//        } else if (statusCode != 0) {
//            String errMsg = asString("/error/msg", json);
//            if (errMsg == null)
//                errMsg = String.valueOf(json);
//            // addErrorMessage(context, errMsg);
//            logger.error(errMsg);
//            msgProcess.err(errMsg);
//            result.success = false;
//            return result;
//        } else {
//            // make sure no "failure" object in there either
//            Object failureObj = json.get("failure");
//            if (failureObj != null) {
//                if (failureObj instanceof Map) {
//                    Object err = ((Map) failureObj).get("");
//                    if (err != null) {
//                        // addErrorMessage(context, err.toString());
//                        logger.error(err.toString());
//                        msgProcess.err(err.toString());
//                    }
//                // throw new SolrServerException(err.toString());
//                }
//                // addErrorMessage(context, failureObj.toString());
//                logger.error(failureObj.toString());
//                msgProcess.err(failureObj.toString());
//                result.success = false;
//                return result;
//            }
//        }
//        result.success = true;
//        return result;
//    }
//
//    /**
//     * Helper function for reading a Long value from a JSON Object tree.
//     */
//    private static Long asLong(String jsonPath, Map<String, Object> json) {
//        return pathAs(Long.class, jsonPath, json);
//    }
//
//    @SuppressWarnings("unchecked")
//    static <T> T pathAs(Class<T> clazz, String jsonPath, Map<String, Object> json) {
//        T val = null;
//        Object obj = atPath(jsonPath, json);
//        if (obj != null) {
//            if (clazz.isAssignableFrom(obj.getClass())) {
//                val = (T) obj;
//            } else {
//                // no ok if it's not null and of a different type
//                throw new IllegalStateException("Expected a " + clazz.getName() + " at path " + jsonPath + " but found " + obj + " instead! " + json);
//            }
//        }
//        // it's ok if it is null
//        return val;
//    }
//
//    /**
//     * Helper function for reading a String value from a JSON Object tree.
//     */
//    private static String asString(String jsonPath, Map<String, Object> json) {
//        return pathAs(String.class, jsonPath, json);
//    }
//
//    @SuppressWarnings({ "rawtypes", "unchecked" })
//    private static Object atPath(String jsonPath, Map<String, Object> json) {
//        if ("/".equals(jsonPath))
//            return json;
//        if (!jsonPath.startsWith("/"))
//            throw new IllegalArgumentException("Invalid JSON path: " + jsonPath + "! Must start with a /");
//        Map<String, Object> parent = json;
//        Object result = null;
//        // Break on all slashes
//        String[] path = jsonPath.split("(?<![\\\\])/");
//        // backslash
//        for (int p = 1; p < path.length; p++) {
//            String part = path[p];
//            if (part.startsWith("\\")) {
//                part = part.substring(1);
//            }
//            Object child = parent.get(part);
//            if (child == null)
//                break;
//            if (p == path.length - 1) {
//                // success - found the node at the desired path
//                result = child;
//            } else {
//                if (child instanceof Map) {
//                    // keep walking the path down to the desired node
//                    parent = (Map) child;
//                } else {
//                    // early termination - hit a leaf before the requested node
//                    break;
//                }
//            }
//        }
//        return result;
//    }
//}
