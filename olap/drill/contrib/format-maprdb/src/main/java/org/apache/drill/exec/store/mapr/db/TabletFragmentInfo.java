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
package org.apache.drill.exec.store.mapr.db;

import org.apache.hadoop.hbase.HRegionInfo;

import com.mapr.db.impl.TabletInfoImpl;

public class TabletFragmentInfo  implements Comparable<TabletFragmentInfo> {

  final private HRegionInfo regionInfo;
  final private TabletInfoImpl tabletInfoImpl;

  public TabletFragmentInfo(HRegionInfo regionInfo) {
    this(null, regionInfo);
  }

  public TabletFragmentInfo(TabletInfoImpl tabletInfoImpl) {
    this(tabletInfoImpl, null);
  }

  TabletFragmentInfo(TabletInfoImpl tabletInfoImpl, HRegionInfo regionInfo) {
    this.regionInfo = regionInfo;
    this.tabletInfoImpl = tabletInfoImpl;
  }

  public HRegionInfo getRegionInfo() {
    return regionInfo;
  }

  public TabletInfoImpl getTabletInfoImpl() {
    return tabletInfoImpl;
  }

  public boolean containsRow(byte[] row) {
    return tabletInfoImpl != null ? tabletInfoImpl.containsRow(row) :
        regionInfo.containsRow(row);
  }

  public byte[] getStartKey() {
    return tabletInfoImpl != null ? tabletInfoImpl.getStartRow() :
        regionInfo.getStartKey();
  }

  public byte[] getEndKey() {
    return tabletInfoImpl != null ? tabletInfoImpl.getStopRow() :
        regionInfo.getEndKey();
  }

  @Override
  public int compareTo(TabletFragmentInfo o) {
    return tabletInfoImpl != null ? tabletInfoImpl.compareTo(o.tabletInfoImpl) :
        regionInfo.compareTo(o.regionInfo);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((regionInfo == null) ? 0 : regionInfo.hashCode());
    result = prime * result + ((tabletInfoImpl == null) ? 0 : tabletInfoImpl.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    TabletFragmentInfo other = (TabletFragmentInfo) obj;
    if (regionInfo == null) {
      if (other.regionInfo != null) {
        return false;
      }
    } else if (!regionInfo.equals(other.regionInfo)) {
      return false;
    }
    if (tabletInfoImpl == null) {
      if (other.tabletInfoImpl != null) {
        return false;
      }
    } else if (!tabletInfoImpl.equals(other.tabletInfoImpl)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "TabletFragmentInfo [regionInfo=" + regionInfo + ", tabletInfoImpl=" + tabletInfoImpl
        + "]";
  }

}
