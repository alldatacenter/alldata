/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytedance.bitsail.common.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Created 2020/6/30.
 */
public class JsonVisitor {
  public static JsonNode getPathValue(JsonNode jsonNode, FieldPathUtils.PathInfo pathInfo, boolean isCaseInsensitive) {
    if (Objects.isNull(jsonNode) || Objects.isNull(pathInfo)) {
      return null;
    }
    JsonNode nextJsonNode = null;
    if (isCaseInsensitive) {
      for (Iterator<Map.Entry<String, JsonNode>> it = jsonNode.fields(); it.hasNext(); ) {
        Map.Entry<String, JsonNode> entry = it.next();
        if (entry.getKey().equalsIgnoreCase(pathInfo.getName())) {
          nextJsonNode = entry.getValue();
          break;
        }
      }
    } else {
      nextJsonNode = jsonNode.get(pathInfo.getName());
    }
    if (Objects.isNull(pathInfo.getNestPathInfo())) {
      return nextJsonNode;
    }
    return getPathValue(nextJsonNode, pathInfo.getNestPathInfo(), isCaseInsensitive);
  }

  public static Object getPathValue(JSONObject value, FieldPathUtils.PathInfo pathInfo) {

    if (Objects.isNull(value) || Objects.isNull(pathInfo)) {
      return null;
    }

    Object pathValue = value;
    for (FieldPathUtils.PathInfo nextPath = pathInfo; Objects.nonNull(pathValue)
        && Objects.nonNull(nextPath); nextPath = nextPath.getNestPathInfo()) {
      pathValue = getPathNode(pathValue, nextPath);
    }
    return pathValue;
  }

  private static Object getPathNode(Object pathNode, FieldPathUtils.PathInfo pathInfo) {
    if (!(pathNode instanceof JSONObject)) {
      return null;
    }

    Object nextPathNode = ((JSONObject) pathNode).get(pathInfo.getName());
    if (FieldPathUtils.PathType.ARRAY.equals(pathInfo.getPathType())) {
      if (nextPathNode instanceof JSONArray) {
        List<Object> arrayPathNode = ((List) nextPathNode);
        if (pathInfo.getIndex() < arrayPathNode.size()) {
          return arrayPathNode.get(pathInfo.getIndex());
        }
        return null;
      }
      throw new IllegalArgumentException(String.format("Path info %s is array type, but actually is not.",
          pathInfo.getName()));
    }
    return nextPathNode;
  }
}

