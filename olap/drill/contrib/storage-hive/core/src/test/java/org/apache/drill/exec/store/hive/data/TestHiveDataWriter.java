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

package org.apache.drill.exec.store.hive.data;

import org.apache.drill.exec.expr.holders.TimeStampHolder;
import org.apache.drill.exec.store.hive.writers.primitive.HiveTimestampWriter;
import org.apache.drill.exec.vector.complex.writer.FieldWriter;
import org.apache.drill.exec.vector.complex.writer.TimeStampWriter;
import org.apache.hadoop.hive.common.type.Timestamp;
import org.apache.hadoop.hive.serde2.io.TimestampWritableV2;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.WritableTimestampObjectInspector;
import org.apache.hadoop.io.LongWritable;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestHiveDataWriter {

  @Test
  public void testTimestampWriter() {
    TestTimeStampWriter testWriter = new TestTimeStampWriter();
    HiveTimestampWriter writer = new HiveTimestampWriter(new WritableTimestampObjectInspector(), testWriter);
    long testLong = 1643341736000L;// parquet long logical-type TIMESTAMP_MICRO
    long expectedLong = 1643341736L;

    // test long value
    writer.write(new LongWritable(testLong));
    assertEquals(testWriter.getTimestamp(), expectedLong);

    // test timestampV2 value
    Timestamp ht = new Timestamp();
    ht.setTimeInMillis(testWriter.getTimestamp());
    TimestampWritableV2 tw2 = new TimestampWritableV2(ht);
    writer.write(tw2);
    assertEquals(testWriter.getTimestamp(), expectedLong);
  }
}

class TestTimeStampWriter implements TimeStampWriter {

  private long timestamp;

  @Override
  public void write(TimeStampHolder h) { }

  @Override
  public void writeTimeStamp(long value) {
    this.timestamp = value;
  }

  public long getTimestamp() {
    return timestamp;
  }

  @Override
  public FieldWriter getParent() {
    return null;
  }

  @Override
  public int getValueCapacity() {
    return 0;
  }

  @Override
  public void close() throws Exception { }

  @Override
  public void setPosition(int index) { }
}
