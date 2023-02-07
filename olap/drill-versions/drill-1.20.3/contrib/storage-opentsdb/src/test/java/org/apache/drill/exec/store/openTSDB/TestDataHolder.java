/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.store.openTSDB;

public class TestDataHolder {

  public static final String SAMPLE_DATA_FOR_POST_REQUEST_WITH_TAGS = "[{" +
      "\"metric\":\"warp.speed.test\"," +
      "\"tags\":{\"symbol\":\"VOD.L\"}," +
      "\"aggregateTags\":[]," +
      "\"dps\":{" +
      "\"1407165399\":196.3000030517578," +
      "\"1407165402\":196.3000030517578," +
      "\"1407165405\":196.3000030517578," +
      "\"1407165407\":196.3000030517578," +
      "\"1407165410\":196.3000030517578," +
      "\"1407165422\":196.3000030517578," +
      "\"1488271956\":111.11000061035156}}," +
      "{\"metric\":\"warp.speed.test\"," +
      "\"tags\":{\"symbol\":\"BP.L\"}," +
      "\"aggregateTags\":[]," +
      "\"dps\":{" +
      "\"1407165399\":484.20001220703125," +
      "\"1407165403\":484.1499938964844," +
      "\"1407165405\":484.1499938964844," +
      "\"1407165408\":484.1499938964844," +
      "\"1407165419\":484.1499938964844," +
      "\"1407165423\":484.2550048828125}}," +
      "{\"metric\":\"warp.speed.test\"," +
      "\"tags\":{\"symbol\":\"BARC.L\"}," +
      "\"aggregateTags\":[]," +
      "\"dps\":{" +
      "\"1407165401\":224.14999389648438," +
      "\"1407165404\":224.14999389648438," +
      "\"1407165406\":224.14999389648438," +
      "\"1407165409\":224.14999389648438," +
      "\"1407165422\":224.14999389648438}" +
      "}]";

  public static final String SAMPLE_DATA_FOR_GET_TABLE_REQUEST =
      "[{" +
          "\"metric\":\"warp.speed.test\"," +
          "\"tags\":{}," +
          "\"aggregateTags\":[\"symbol\"]," +
          "\"dps\":{" +
          "\"1407165399\":680.5000152587891," +
          "\"1407165401\":904.625," +
          "\"1407165402\":904.6124954223633," +
          "\"1407165403\":904.5999908447266," +
          "\"1407165404\":904.5999908447266," +
          "\"1407165405\":904.5999908447266," +
          "\"1407165406\":904.5999908447266," +
          "\"1407165407\":904.5999908447266," +
          "\"1407165408\":904.5999908447266," +
          "\"1407165409\":904.5999908447266," +
          "\"1407165410\":904.5999908447266," +
          "\"1407165419\":904.5999908447266," +
          "\"1407165422\":904.6787490844727," +
          "\"1407165423\":680.5550068842233," +
          "\"1488271956\":111.11000061035156}" +
          "}]";

  public static final String SAMPLE_DATA_FOR_POST_DOWNSAMPLE_REQUEST_WITH_TAGS =
      "[{" +
          "\"metric\":\"warp.speed.test\"," +
          "\"tags\":{\"symbol\":\"VOD.L\"}," +
          "\"aggregateTags\":[]," +
          "\"dps\":{" +
          "\"1261440000\":196.3000030517578," +
          "\"1419120000\":111.11000061035156}" +
          "},{" +
          "\"metric\":\"warp.speed.test\"" +
          ",\"tags\":{\"symbol\":\"BP.L\"}," +
          "\"aggregateTags\":[]," +
          "\"dps\":{" +
          "\"1261440000\":484.1758321126302}" +
          "},{" +
          "\"metric\":\"warp.speed.test\"," +
          "\"tags\":{" +
          "\"symbol\":\"BARC.L\"}," +
          "\"aggregateTags\":[]," +
          "\"dps\":{" +
          "\"1261440000\":224.14999389648438}" +
          "}]";

  public static final String SAMPLE_DATA_FOR_GET_TABLE_NAME_REQUEST = "[\"warp.speed.test\"]";

  public static final String SAMPLE_DATA_FOR_POST_DOWNSAMPLE_REQUEST_WITHOUT_TAGS =
      "[{" +
          "\"metric\":\"warp.speed.test\"," +
          "\"tags\":{}," +
          "\"aggregateTags\":[" +
          "\"symbol\"]," +
          "\"dps\":{" +
          "\"1261440000\":904.6258290608723," +
          "\"1419120000\":111.11000061035156}" +
          "}]";

  public static final String SAMPLE_DATA_FOR_POST_END_REQUEST_WITHOUT_TAGS =
      "[{" +
          "\"metric\":\"warp.speed.test\"," +
          "\"tags\":{}," +
          "\"aggregateTags\":[" +
          "\"symbol\"]," +
          "\"dps\":{" +
          "\"1407165399\":680.5000152587891," +
          "\"1407165401\":904.625," +
          "\"1407165402\":904.6124954223633," +
          "\"1419120000\":904.5999908447266}" +
          "}]";

  public static final String DOWNSAMPLE_REQUEST_WTIHOUT_TAGS =
      "{" +
          "\"start\":\"47y-ago\"," +
          "\"end\":null," +
          "\"queries\":[{" +
          "\"aggregator\":\"sum\"," +
          "\"metric\":\"warp.speed.test\"," +
          "\"rate\":null," +
          "\"downsample\":\"5y-avg\"," +
          "\"tags\":{}" +
          "}]" +
          "}";

  public static final String END_PARAM_REQUEST_WTIHOUT_TAGS =
        "{" +
          "\"start\":\"47y-ago\"," +
          "\"end\":\"1407165403000\"," +
          "\"queries\":[{" +
          "\"aggregator\":\"sum\"," +
          "\"metric\":\"warp.speed.test\"," +
          "\"rate\":null," +
          "\"downsample\":null," +
          "\"tags\":{}" +
          "}]" +
          "}";


  public static final String DOWNSAMPLE_REQUEST_WITH_TAGS =
      "{" +
          "\"start\":\"47y-ago\"," +
          "\"end\":null," +
          "\"queries\":[{" +
          "\"aggregator\":\"sum\"," +
          "\"metric\":\"warp.speed.test\"," +
          "\"rate\":null," +
          "\"downsample\":\"5y-avg\"," +
          "\"tags\":{" +
          "\"symbol\":\"*\"}" +
          "}]" +
          "}";

  public static final String END_PARAM_REQUEST_WITH_TAGS =
      "{" +
          "\"start\":\"47y-ago\"," +
          "\"end\":\"1407165403000\"," +
          "\"queries\":[{" +
          "\"aggregator\":\"sum\"," +
          "\"metric\":\"warp.speed.test\"," +
          "\"rate\":null," +
          "\"downsample\":null," +
          "\"tags\":{" +
          "\"symbol\":\"*\"}" +
          "}]" +
          "}";

  public static final String REQUEST_TO_NONEXISTENT_METRIC =
      "{" +
          "\"start\":\"47y-ago\"," +
          "\"end\":null," +
          "\"queries\":[{" +
          "\"aggregator\":\"sum\"," +
          "\"metric\":\"warp.spee\"," +
          "\"rate\":null," +
          "\"downsample\":null," +
          "\"tags\":{" + "}" +
          "}]" +
          "}";


  public static final String POST_REQUEST_WITHOUT_TAGS =
      "{" +
          "\"start\":\"47y-ago\"," +
          "\"end\":null," +
          "\"queries\":[{" +
          "\"aggregator\":\"sum\"," +
          "\"metric\":\"warp.speed.test\"," +
          "\"rate\":null," +
          "\"downsample\":null," +
          "\"tags\":{}" +
          "}]" +
          "}";


  public static final String POST_REQUEST_WITH_TAGS =
      "{" +
          "\"start\":\"47y-ago\"," +
          "\"end\":null," +
          "\"queries\":[{" +
          "\"aggregator\":\"sum\"," +
          "\"metric\":\"warp.speed.test\"," +
          "\"rate\":null," +
          "\"downsample\":null," +
          "\"tags\":{" +
          "\"symbol\":\"*\"}" +
          "}]" +
          "}";

  public static final String SAMPLE_DATA_FOR_POST_END_REQUEST_WITH_TAGS =
      "[{" +
          "\"metric\":\"warp.speed.test\"," +
          "\"tags\":{\"symbol\":\"VOD.L\"}," +
          "\"aggregateTags\":[]," +
          "\"dps\":{" +
          "\"1407165399\":196.3000030517578," +
          "\"1407165402\":196.3000030517578}" +
          "},{" +
          "\"metric\":\"warp.speed.test\"" +
          ",\"tags\":{\"symbol\":\"BP.L\"}," +
          "\"aggregateTags\":[]," +
          "\"dps\":{" +
          "\"1407165399\":484.20001220703125," +
          "\"1407165403\":484.1499938964844}" +
          "},{" +
          "\"metric\":\"warp.speed.test\"," +
          "\"tags\":{" +
          "\"symbol\":\"BARC.L\"}," +
          "\"aggregateTags\":[]," +
          "\"dps\":{" +
          "\"1407165401\":224.14999389648438}" +
          "}]";
}
