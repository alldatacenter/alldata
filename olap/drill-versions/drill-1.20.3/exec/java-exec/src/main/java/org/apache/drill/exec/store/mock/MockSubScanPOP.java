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
package org.apache.drill.exec.store.mock;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.drill.exec.physical.base.AbstractBase;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.base.PhysicalVisitor;
import org.apache.drill.exec.physical.base.SubScan;
import org.apache.drill.exec.store.mock.MockTableDef.MockScanEntry;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

/**
 * Describes a physical scan operation for the mock data source. Each operator
 * can, in general, give rise to one or more actual scans. For the mock data
 * source, each sub-scan does exactly one (simulated) scan.
 */

@JsonTypeName("mock-sub-scan")
public class MockSubScanPOP extends AbstractBase implements SubScan {

  public static final String OPERATOR_TYPE = "MOCK_SUB_SCAN";

  private final String url;
  protected final List<MockScanEntry> readEntries;
  private final boolean extended;

  /**
   * This constructor is called from Jackson and is designed to support both
   * older physical plans and the newer ("extended") plans. Jackson will fill
   * in a null value for the <tt>extended</tt> field for older plans; we use
   * that null value to know that the plan is old, thus not extended. Newer
   * plans simply provide the value.
   *
   * @param url
   *          not used for the mock plan, appears to be a vestige of creating
   *          this from a file-based plugin. Must keep it because older physical
   *          plans contained a dummy URL value.
   * @param extended
   *          see above
   * @param readEntries
   *          a description of the columns to generate in a Jackson-serialized
   *          form unique to the mock data source plugin.
   */

  @JsonCreator
  public MockSubScanPOP(@JsonProperty("url") String url,
                        @JsonProperty("extended") Boolean extended,
                        @JsonProperty("entries") List<MockScanEntry> readEntries) {
    this.readEntries = readEntries;
//    OperatorCost cost = new OperatorCost(0,0,0,0);
//    Size size = new Size(0,0);
//    for(MockGroupScanPOP.MockScanEntry r : readEntries){
//      cost = cost.add(r.getCost());
//      size = size.add(r.getSize());
//    }
//    this.cost = cost;
//    this.size = size;
    this.url = url;
    this.extended = extended == null ? false : extended;
  }

  public String getUrl() { return url; }
  public boolean isExtended() { return extended; }

  @JsonProperty("entries")
  public List<MockScanEntry> getReadEntries() {
    return readEntries;
  }

  @Override
  public Iterator<PhysicalOperator> iterator() {
    return Collections.emptyIterator();
  }

  // will want to replace these two methods with an interface above for AbstractSubScan
  @Override
  public boolean isExecutable() {
    return true;
  }

  @Override
  public <T, X, E extends Throwable> T accept(PhysicalVisitor<T, X, E> physicalVisitor, X value) throws E{
    return physicalVisitor.visitSubScan(this, value);
  }
  // see comment above about replacing this

  @Override
  @JsonIgnore
  public PhysicalOperator getNewWithChildren(List<PhysicalOperator> children) {
    Preconditions.checkArgument(children.isEmpty());
    return new MockSubScanPOP(url, extended, readEntries);

  }

  @Override
  public String getOperatorType() {
    return OPERATOR_TYPE;
  }
}
