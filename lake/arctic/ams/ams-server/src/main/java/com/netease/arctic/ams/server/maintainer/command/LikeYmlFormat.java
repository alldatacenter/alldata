/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.ams.server.maintainer.command;

import java.util.ArrayList;
import java.util.List;

public class LikeYmlFormat {

  private String content;

  private List<LikeYmlFormat> childes = new ArrayList<>();

  public LikeYmlFormat(String content) {
    this.content = content;
  }

  public LikeYmlFormat child(String childContent) {
    LikeYmlFormat child = new LikeYmlFormat(childContent);
    childes.add(child);
    return child;
  }

  public static LikeYmlFormat content(String content) {
    return new LikeYmlFormat(content);
  }

  public static LikeYmlFormat blank() {
    return new LikeYmlFormat(null);
  }

  public String print() {
    return print("", this);
  }

  private static String print(String prefix, LikeYmlFormat likeYmlFormat) {
    StringBuilder sb = new StringBuilder();
    if (likeYmlFormat.content != null) {
      sb.append(prefix).append(likeYmlFormat.content);
      if (likeYmlFormat.childes.size() != 0) {
        sb.append(":");
      }
      sb.append("\n");
      prefix = prefix + "\t";
    }
    for (LikeYmlFormat child: likeYmlFormat.childes) {
      sb.append(print(prefix, child));
    }
    return sb.toString();
  }

  @Override
  public String toString() {
    return print();
  }

}
