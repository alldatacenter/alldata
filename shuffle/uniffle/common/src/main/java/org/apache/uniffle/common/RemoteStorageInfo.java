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

package org.apache.uniffle.common;

import java.io.Serializable;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import org.apache.uniffle.common.util.Constants;

public class RemoteStorageInfo implements Serializable {
  public static final RemoteStorageInfo EMPTY_REMOTE_STORAGE = new RemoteStorageInfo("", "");
  private final String path;
  private final Map<String, String> confItems;

  public RemoteStorageInfo(String path) {
    this(path, Maps.newHashMap());
  }

  public RemoteStorageInfo(String path, Map<String, String> confItems) {
    this.path = path;
    if (confItems == null) {
      this.confItems = Maps.newHashMap();
    } else {
      this.confItems = confItems;
    }
  }

  public RemoteStorageInfo(String path, String confString) {
    this.path = path;
    this.confItems = Maps.newHashMap();
    if (!StringUtils.isEmpty(confString)) {
      String[] items = confString.split(Constants.COMMA_SPLIT_CHAR);
      if (!ArrayUtils.isEmpty(items)) {
        for (String item : items) {
          String[] kv = item.split(Constants.EQUAL_SPLIT_CHAR);
          if (kv.length == 2) {
            this.confItems.put(kv[0], kv[1]);
          }
        }
      }
    }
  }

  public String getPath() {
    return path;
  }

  public Map<String, String> getConfItems() {
    return confItems;
  }

  public boolean isEmpty() {
    return StringUtils.isEmpty(path);
  }

  public String getConfString() {
    if (confItems.isEmpty()) {
      return  "";
    }
    return confItems.entrySet()
        .stream()
        .map(e -> String.join("=", e.getKey(), e.getValue()))
        .collect(Collectors.joining(","));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    RemoteStorageInfo that = (RemoteStorageInfo) o;
    if (!Objects.equal(path, that.path)) {
      return false;
    }

    if (this.confItems.size() != that.confItems.size())  {
      return false;
    }

    for (Map.Entry<String, String> entry : this.confItems.entrySet()) {
      if (!that.confItems.containsKey(entry.getKey())) {
        return false;
      }
      String value = that.confItems.get(entry.getKey());
      if (!Objects.equal(entry.getValue(), value)) {
        return false;
      }
    }

    return true;
  }

  @Override
  public String toString() {
    if (isEmpty()) {
      return "Empty Remote Storage";
    } else if (confItems == null) {
      return String.join(Constants.COMMA_SPLIT_CHAR, path, "null conf");
    } else if (confItems.isEmpty()) {
      return String.join(Constants.COMMA_SPLIT_CHAR, path, "empty conf");
    } else {
      return String.join(Constants.COMMA_SPLIT_CHAR, path, Joiner.on(",").withKeyValueSeparator("=").join(confItems));
    }
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(path, confItems);
  }
}
