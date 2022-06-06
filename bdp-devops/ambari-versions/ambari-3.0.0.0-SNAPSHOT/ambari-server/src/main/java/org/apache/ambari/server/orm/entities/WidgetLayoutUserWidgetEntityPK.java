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
package org.apache.ambari.server.orm.entities;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Id;

public class WidgetLayoutUserWidgetEntityPK  implements Serializable {


  private Long widgetLayoutId;

  @Id
  @Column(name = "widget_layout_id", nullable = false, updatable = false)
  public Long getWidgetLayoutId() {
    return widgetLayoutId;
  }

  public void setWidgetLayoutId(Long widgetLayoutId) {
    this.widgetLayoutId = widgetLayoutId;
  }

  private Long userWidgetId;

  @Id
  @Column(name = "widget_id", nullable = false, updatable = false)
  public Long getUserWidgetId() {
    return userWidgetId;
  }

  public void setUserWidgetId(Long userWidgetId) {
    this.userWidgetId = userWidgetId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    WidgetLayoutUserWidgetEntityPK that = (WidgetLayoutUserWidgetEntityPK) o;

    return Objects.equals(widgetLayoutId, that.widgetLayoutId) &&
      Objects.equals(userWidgetId, that.userWidgetId);
  }

  @Override
  public int hashCode() {
    int result = null != widgetLayoutId ? widgetLayoutId.hashCode() : 0;
    result = 31 * result + (userWidgetId != null ? userWidgetId.hashCode() : 0);
    return result;
  }

}
