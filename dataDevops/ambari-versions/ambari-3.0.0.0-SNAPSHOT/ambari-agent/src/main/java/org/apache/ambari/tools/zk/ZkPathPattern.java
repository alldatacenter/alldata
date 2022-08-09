/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.tools.zk;

import static java.util.Collections.singletonList;
import static org.apache.ambari.tools.zk.ZkAcl.append;

import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;

/**
 * Represents a Zookeeper path where each '/' separated segment might contain wildcard mask (*)
 * E.g.: /ab/c/*&#47;/def
 */
public class ZkPathPattern {
  private final List<Segment> segments;

  /**
   * Creates a instance from a pattern like /*&#47;/abc*
   */
  public static ZkPathPattern fromString(String pattern) {
    pattern = pattern.trim();
    if (!pattern.startsWith("/")) {
      throw new IllegalArgumentException("ZkPath must start with: '/'. Invalid path: " + pattern);
    }
    if ("/".equals(pattern)) {
      return new ZkPathPattern(singletonList(new Segment("*")));
    }
    List<Segment> segments = new ArrayList<>();
    for (String segment : pattern.substring(1).split("/")) {
      if (segment.isEmpty()) {
        throw new IllegalArgumentException("Invalid ZkPath: " + pattern);
      }
      segments.add(new Segment(segment));
    }
    if (segments.isEmpty()) {
      throw new IllegalArgumentException("Empty ZkPath: " + pattern);
    }
    return new ZkPathPattern(segments);
  }

  private ZkPathPattern(List<Segment> segments) {
    this.segments = segments;
  }

  /**
   * Finds matching paths from the given base node
   * E.g.:
   *  If the pattern is /*&#47;/abc* it will find nodes like:
   *    /a/abcdef
   *    /xy/abc/efg
   *    /def/abc
   */
  public List<String> findMatchingPaths(ZooKeeper zkClient, String basePath) throws KeeperException, InterruptedException {
    List<String> result = new ArrayList<>();
    collectMatching(zkClient, basePath, result);
    return result;
  }

  private void collectMatching(ZooKeeper zkClient, String basePath, List<String> result) throws KeeperException, InterruptedException {
    for (String child : zkClient.getChildren(basePath, null)) {
      if (first().matches(child)) {
        if (rest() == null) {
          result.add(append(basePath, child));
        } else {
          rest().collectMatching(zkClient, append(basePath, child), result);
        }
      }
    }
  }

  private Segment first() {
    return segments.get(0);
  }

  private ZkPathPattern rest() {
    List<Segment> tail = segments.subList(1, segments.size());
    return tail.isEmpty() ? null : new ZkPathPattern(tail);
  }

  public static class Segment {
    private final PathMatcher glob;

    public Segment(String segment) {
      this.glob = FileSystems.getDefault().getPathMatcher("glob:" + segment);
    }

    public boolean matches(String node) {
      return glob.matches(Paths.get(node));
    }
  }
}
