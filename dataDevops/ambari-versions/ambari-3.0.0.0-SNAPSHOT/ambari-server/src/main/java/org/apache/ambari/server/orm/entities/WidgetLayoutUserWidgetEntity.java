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

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@IdClass(WidgetLayoutUserWidgetEntityPK.class)
@Entity
@Table(name = "widget_layout_user_widget")
public class WidgetLayoutUserWidgetEntity {


  @Id
  @Column(name = "widget_layout_id", nullable = false, updatable = false, insertable = false)
  private Long widgetLayoutId;

  @Id
  @Column(name = "widget_id", nullable = false, updatable = false, insertable = false)
  private Long userWidgetId;

  @ManyToOne
  @JoinColumn(name = "widget_layout_id", referencedColumnName = "id")
  private WidgetLayoutEntity widgetLayout;

  @ManyToOne(cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH})
  @JoinColumn(name = "widget_id", referencedColumnName = "id")
  private WidgetEntity widget;

  @Column(name = "widget_order")
  private Integer widgetOrder;

  public Long getWidgetLayoutId() {
    return widgetLayoutId;
  }

  public void setWidgetLayoutId(Long widgetLayoutId) {
    this.widgetLayoutId = widgetLayoutId;
  }

  public Long getUserWidgetId() {
    return userWidgetId;
  }

  public void setUserWidgetId(Long userWidgetId) {
    this.userWidgetId = userWidgetId;
  }

  public WidgetLayoutEntity getWidgetLayout() {
    return widgetLayout;
  }

  public void setWidgetLayout(WidgetLayoutEntity widgetLayout) {
    this.widgetLayout = widgetLayout;
  }

  public WidgetEntity getWidget() {
    return widget;
  }

  public void setWidget(WidgetEntity widget) {
    this.widget = widget;
  }

  public Integer getWidgetOrder() {
    return widgetOrder;
  }

  public void setWidgetOrder(Integer widgetOrder) {
    this.widgetOrder = widgetOrder;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    WidgetLayoutUserWidgetEntity that = (WidgetLayoutUserWidgetEntity) o;

    return (widgetLayout.equals(that.widgetLayout) && widget.equals(that.widget));
  }

  @Override
  public int hashCode() {
    int result = null != widgetLayout ? widgetLayout.hashCode() : 0;
    result = 31 * result + (widget != null ? widget.hashCode() : 0);
    return result;
  }

}
