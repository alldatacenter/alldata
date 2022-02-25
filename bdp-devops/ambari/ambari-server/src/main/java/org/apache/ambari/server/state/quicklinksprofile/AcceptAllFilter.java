/*
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

package org.apache.ambari.server.state.quicklinksprofile;

import org.apache.ambari.server.state.quicklinks.Link;

/**
 * A filter that accepts all links. It is useful to specify a general rule while the more specific
 * ({@link LinkNameFilter} and {@link LinkAttributeFilter}) filters handle more special cases.
 */
public class AcceptAllFilter extends Filter {

  @Override
  public boolean accept(Link link) {
    return true;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AcceptAllFilter that = (AcceptAllFilter) o;
    return isVisible() == that.isVisible();
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(isVisible());
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + " (visible=" + isVisible() + ")";
  }

}
