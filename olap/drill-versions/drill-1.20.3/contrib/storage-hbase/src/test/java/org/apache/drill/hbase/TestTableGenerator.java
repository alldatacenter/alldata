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
package org.apache.drill.hbase;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Date;
import java.util.Random;

import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.BufferedMutator;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Order;
import org.apache.hadoop.hbase.util.OrderedBytes;
import org.apache.hadoop.hbase.util.PositionedByteRange;
import org.apache.hadoop.hbase.util.SimplePositionedMutableByteRange;

public class TestTableGenerator {

  public static final byte[][] SPLIT_KEYS = {
    {'b'}, {'c'}, {'d'}, {'e'}, {'f'}, {'g'}, {'h'}, {'i'},
    {'j'}, {'k'}, {'l'}, {'m'}, {'n'}, {'o'}, {'p'}, {'q'},
    {'r'}, {'s'}, {'t'}, {'u'}, {'v'}, {'w'}, {'x'}, {'y'}, {'z'}
  };

  static final byte[] FAMILY_F = {'f'};
  static final byte[] COLUMN_C = {'c'};

  public static void generateHBaseDataset1(Connection conn, Admin admin, TableName tableName, int numberRegions) throws Exception {
    if (admin.tableExists(tableName)) {
      admin.disableTable(tableName);
      admin.deleteTable(tableName);
    }

    HTableDescriptor desc = new HTableDescriptor(tableName);
    desc.addFamily(new HColumnDescriptor("f"));
    desc.addFamily(new HColumnDescriptor("f2"));
    if (numberRegions > 1) {
      admin.createTable(desc, Arrays.copyOfRange(SPLIT_KEYS, 0, numberRegions-1));
    } else {
      admin.createTable(desc);
    }

    BufferedMutator table = conn.getBufferedMutator(tableName);

    Put p = new Put("a1".getBytes());
    p.addColumn("f".getBytes(), "c1".getBytes(), "1".getBytes());
    p.addColumn("f".getBytes(), "c2".getBytes(), "2".getBytes());
    p.addColumn("f".getBytes(), "c3".getBytes(), "3".getBytes());
    p.addColumn("f".getBytes(), "c4".getBytes(), "4".getBytes());
    p.addColumn("f".getBytes(), "c5".getBytes(), "5".getBytes());
    p.addColumn("f".getBytes(), "c6".getBytes(), "6".getBytes());
    table.mutate(p);

    p = new Put("a2".getBytes());
    p.addColumn("f".getBytes(), "c1".getBytes(), "1".getBytes());
    p.addColumn("f".getBytes(), "c2".getBytes(), "2".getBytes());
    p.addColumn("f".getBytes(), "c3".getBytes(), "3".getBytes());
    p.addColumn("f".getBytes(), "c4".getBytes(), "4".getBytes());
    p.addColumn("f".getBytes(), "c5".getBytes(), "5".getBytes());
    p.addColumn("f".getBytes(), "c6".getBytes(), "6".getBytes());
    table.mutate(p);

    p = new Put("a3".getBytes());
    p.addColumn("f".getBytes(), "c1".getBytes(), "1".getBytes());
    p.addColumn("f".getBytes(), "c3".getBytes(), "2".getBytes());
    p.addColumn("f".getBytes(), "c5".getBytes(), "3".getBytes());
    p.addColumn("f".getBytes(), "c7".getBytes(), "4".getBytes());
    p.addColumn("f".getBytes(), "c8".getBytes(), "5".getBytes());
    p.addColumn("f".getBytes(), "c9".getBytes(), "6".getBytes());
    table.mutate(p);

    p = new Put(new byte[]{'b', '4', 0});
    p.addColumn("f".getBytes(), "c1".getBytes(), "1".getBytes());
    p.addColumn("f2".getBytes(), "c2".getBytes(), "2".getBytes());
    p.addColumn("f".getBytes(), "c3".getBytes(), "3".getBytes());
    p.addColumn("f2".getBytes(), "c4".getBytes(), "4".getBytes());
    p.addColumn("f".getBytes(), "c5".getBytes(), "5".getBytes());
    p.addColumn("f2".getBytes(), "c6".getBytes(), "6".getBytes());
    table.mutate(p);

    p = new Put("b4".getBytes());
    p.addColumn("f".getBytes(), "c1".getBytes(), "1".getBytes());
    p.addColumn("f2".getBytes(), "c2".getBytes(), "2".getBytes());
    p.addColumn("f".getBytes(), "c3".getBytes(), "3".getBytes());
    p.addColumn("f2".getBytes(), "c4".getBytes(), "4".getBytes());
    p.addColumn("f".getBytes(), "c5".getBytes(), "5".getBytes());
    p.addColumn("f2".getBytes(), "c6".getBytes(), "6".getBytes());
    table.mutate(p);

    p = new Put("b5".getBytes());
    p.addColumn("f2".getBytes(), "c1".getBytes(), "1".getBytes());
    p.addColumn("f".getBytes(), "c2".getBytes(), "2".getBytes());
    p.addColumn("f2".getBytes(), "c3".getBytes(), "3".getBytes());
    p.addColumn("f".getBytes(), "c4".getBytes(), "4".getBytes());
    p.addColumn("f2".getBytes(), "c5".getBytes(), "5".getBytes());
    p.addColumn("f".getBytes(), "c6".getBytes(), "6".getBytes());
    table.mutate(p);

    p = new Put("b6".getBytes());
    p.addColumn("f".getBytes(), "c1".getBytes(), "1".getBytes());
    p.addColumn("f2".getBytes(), "c3".getBytes(), "2".getBytes());
    p.addColumn("f".getBytes(), "c5".getBytes(), "3".getBytes());
    p.addColumn("f2".getBytes(), "c7".getBytes(), "4".getBytes());
    p.addColumn("f".getBytes(), "c8".getBytes(), "5".getBytes());
    p.addColumn("f2".getBytes(), "c9".getBytes(), "6".getBytes());
    table.mutate(p);

    p = new Put("b7".getBytes());
    p.addColumn("f".getBytes(), "c1".getBytes(), "1".getBytes());
    p.addColumn("f".getBytes(), "c2".getBytes(), "2".getBytes());
    table.mutate(p);

    table.close();
  }

  public static void generateHBaseDatasetSingleSchema(Connection conn, Admin admin, TableName tableName, int numberRegions) throws Exception {
    if (admin.tableExists(tableName)) {
      admin.disableTable(tableName);
      admin.deleteTable(tableName);
    }

    HTableDescriptor desc = new HTableDescriptor(tableName);
    desc.addFamily(new HColumnDescriptor("f"));
    if (numberRegions > 1) {
      admin.createTable(desc, Arrays.copyOfRange(SPLIT_KEYS, 0, numberRegions - 1));
    } else {
      admin.createTable(desc);
    }

    BufferedMutator table = conn.getBufferedMutator(tableName);

    Put p = new Put("a1".getBytes());
    p.addColumn("f".getBytes(), "c1".getBytes(), "21".getBytes());
    p.addColumn("f".getBytes(), "c2".getBytes(), "22".getBytes());
    p.addColumn("f".getBytes(), "c3".getBytes(), "23".getBytes());
    table.mutate(p);

    p = new Put("a2".getBytes());
    p.addColumn("f".getBytes(), "c1".getBytes(), "11".getBytes());
    p.addColumn("f".getBytes(), "c2".getBytes(), "12".getBytes());
    p.addColumn("f".getBytes(), "c3".getBytes(), "13".getBytes());
    table.mutate(p);

    p = new Put("a3".getBytes());
    p.addColumn("f".getBytes(), "c1".getBytes(), "31".getBytes());
    p.addColumn("f".getBytes(), "c2".getBytes(), "32".getBytes());
    p.addColumn("f".getBytes(), "c3".getBytes(), "33".getBytes());
    table.mutate(p);

    table.close();
  }

  public static void generateHBaseDatasetMultiCF(Connection conn, Admin admin, TableName tableName, int numberRegions) throws Exception {
    if (admin.tableExists(tableName)) {
      admin.disableTable(tableName);
      admin.deleteTable(tableName);
    }

    HTableDescriptor desc = new HTableDescriptor(tableName);
    desc.addFamily(new HColumnDescriptor("f0"));
    desc.addFamily(new HColumnDescriptor("F"));
    if (numberRegions > 1) {
      admin.createTable(desc, Arrays.copyOfRange(SPLIT_KEYS, 0, numberRegions - 1));
    } else {
      admin.createTable(desc);
    }

    BufferedMutator table = conn.getBufferedMutator(tableName);

    Put p = new Put("a1".getBytes());
    p.addColumn("f0".getBytes(), "c1".getBytes(), "21".getBytes());
    p.addColumn("f0".getBytes(), "c2".getBytes(), "22".getBytes());
    p.addColumn("F".getBytes(), "c3".getBytes(), "23".getBytes());
    table.mutate(p);

    p = new Put("a2".getBytes());
    p.addColumn("f0".getBytes(), "c1".getBytes(), "11".getBytes());
    p.addColumn("f0".getBytes(), "c2".getBytes(), "12".getBytes());
    p.addColumn("F".getBytes(), "c3".getBytes(), "13".getBytes());
    table.mutate(p);

    p = new Put("a3".getBytes());
    p.addColumn("f0".getBytes(), "c1".getBytes(), "31".getBytes());
    p.addColumn("f0".getBytes(), "c2".getBytes(), "32".getBytes());
    p.addColumn("F".getBytes(), "c3".getBytes(), "33".getBytes());
    table.mutate(p);

    table.close();
  }

  public static void generateHBaseDataset2(Connection conn, Admin admin, TableName tableName, int numberRegions) throws Exception {
    if (admin.tableExists(tableName)) {
      admin.disableTable(tableName);
      admin.deleteTable(tableName);
    }

    HTableDescriptor desc = new HTableDescriptor(tableName);
    desc.addFamily(new HColumnDescriptor("f"));

    if (numberRegions > 1) {
      admin.createTable(desc, Arrays.copyOfRange(SPLIT_KEYS, 0, numberRegions-1));
    } else {
      admin.createTable(desc);
    }

    BufferedMutator table = conn.getBufferedMutator(tableName);

    int rowCount = 0;
    byte[] bytes = null;
    final int numColumns = 5;
    Random random = new Random();
    int iteration = 0;
    while (rowCount < 1000) {
      char rowKeyChar = 'a';
      for (int i = 0; i < numberRegions; i++) {
        Put p = new Put((""+rowKeyChar+iteration).getBytes());
        for (int j = 1; j <= numColumns; j++) {
          bytes = new byte[5000];
          random.nextBytes(bytes);
          p.addColumn("f".getBytes(), ("c"+j).getBytes(), bytes);
        }
        table.mutate(p);

        ++rowKeyChar;
        ++rowCount;
      }
      ++iteration;
    }

    table.close();

    admin.flush(tableName);
  }

  public static void generateHBaseDataset3(Connection conn, Admin admin, TableName tableName, int numberRegions) throws Exception {
    if (admin.tableExists(tableName)) {
      admin.disableTable(tableName);
      admin.deleteTable(tableName);
    }

    HTableDescriptor desc = new HTableDescriptor(tableName);
    desc.addFamily(new HColumnDescriptor(FAMILY_F));

    if (numberRegions > 1) {
      admin.createTable(desc, Arrays.copyOfRange(SPLIT_KEYS, 0, numberRegions-1));
    } else {
      admin.createTable(desc);
    }

    BufferedMutator table = conn.getBufferedMutator(tableName);

    for (int i = 0; i <= 100; ++i) {
      Put p = new Put((String.format("%03d", i)).getBytes());
      p.addColumn(FAMILY_F, COLUMN_C, String.format("value %03d", i).getBytes());
      table.mutate(p);
    }
    for (int i = 0; i <= 1000; ++i) {
      Put p = new Put((String.format("%04d", i)).getBytes());
      p.addColumn(FAMILY_F, COLUMN_C, String.format("value %04d", i).getBytes());
      table.mutate(p);
    }

    Put p = new Put("%_AS_PREFIX_ROW1".getBytes());
    p.addColumn(FAMILY_F, COLUMN_C, "dummy".getBytes());
    table.mutate(p);

    p = new Put("%_AS_PREFIX_ROW2".getBytes());
    p.addColumn(FAMILY_F, COLUMN_C, "dummy".getBytes());
    table.mutate(p);

    table.close();

    admin.flush(tableName);
  }

  public static void generateHBaseDatasetCompositeKeyDate(Connection conn, Admin admin, TableName tableName, int numberRegions) throws Exception {
    if (admin.tableExists(tableName)) {
      admin.disableTable(tableName);
      admin.deleteTable(tableName);
    }

    HTableDescriptor desc = new HTableDescriptor(tableName);
    desc.addFamily(new HColumnDescriptor(FAMILY_F));

    if (numberRegions > 1) {
      admin.createTable(desc, Arrays.copyOfRange(SPLIT_KEYS, 0, numberRegions-1));
    } else {
      admin.createTable(desc);
    }

    BufferedMutator table = conn.getBufferedMutator(tableName);

    Date startDate = new Date(1408924800000L);
    long startTime  = startDate.getTime();
    long MILLISECONDS_IN_A_DAY  = (long)1000 * 60 * 60 * 24;
    long MILLISECONDS_IN_A_YEAR = MILLISECONDS_IN_A_DAY * 365;
    long endTime    = startTime + MILLISECONDS_IN_A_YEAR;
    long interval   = MILLISECONDS_IN_A_DAY / 3;

    for (long ts = startTime, counter = 0; ts < endTime; ts += interval, counter++) {
      byte[] rowKey = ByteBuffer.allocate(16) .putLong(ts).array();

      for(int i = 0; i < 8; ++i) {
        rowKey[8 + i] = (byte)(counter >> (56 - (i * 8)));
      }

      Put p = new Put(rowKey);
      p.addColumn(FAMILY_F, COLUMN_C, "dummy".getBytes());
      table.mutate(p);
    }

    table.close();
  }

  public static void generateHBaseDatasetCompositeKeyTime(Connection conn, Admin admin, TableName tableName, int numberRegions) throws Exception {
    if (admin.tableExists(tableName)) {
      admin.disableTable(tableName);
      admin.deleteTable(tableName);
    }

    HTableDescriptor desc = new HTableDescriptor(tableName);
    desc.addFamily(new HColumnDescriptor(FAMILY_F));

    if (numberRegions > 1) {
      admin.createTable(desc, Arrays.copyOfRange(SPLIT_KEYS, 0, numberRegions-1));
    } else {
      admin.createTable(desc);
    }

    BufferedMutator table = conn.getBufferedMutator(tableName);

    long startTime  = 0;
    long MILLISECONDS_IN_A_SEC  = (long)1000;
    long MILLISECONDS_IN_A_DAY = MILLISECONDS_IN_A_SEC * 60 * 60 * 24;
    long endTime    = startTime + MILLISECONDS_IN_A_DAY;
    long smallInterval   = 25;
    long largeInterval   = MILLISECONDS_IN_A_SEC * 42;
    long interval        = smallInterval;

    for (long ts = startTime, counter = 0; ts < endTime; ts += interval, counter++) {
      byte[] rowKey = ByteBuffer.allocate(16) .putLong(ts).array();

      for(int i = 0; i < 8; ++i) {
        rowKey[8 + i] = (byte)(counter >> (56 - (i * 8)));
      }

      Put p = new Put(rowKey);
      p.addColumn(FAMILY_F, COLUMN_C, "dummy".getBytes());
      table.mutate(p);

      if (interval == smallInterval) {
        interval = largeInterval;
      } else {
        interval = smallInterval;
      }
    }

    table.close();
  }

  public static void generateHBaseDatasetCompositeKeyInt(Connection conn, Admin admin, TableName tableName, int numberRegions) throws Exception {
    if (admin.tableExists(tableName)) {
      admin.disableTable(tableName);
      admin.deleteTable(tableName);
    }

    HTableDescriptor desc = new HTableDescriptor(tableName);
    desc.addFamily(new HColumnDescriptor(FAMILY_F));

    if (numberRegions > 1) {
      admin.createTable(desc, Arrays.copyOfRange(SPLIT_KEYS, 0, numberRegions-1));
    } else {
      admin.createTable(desc);
    }

    BufferedMutator table = conn.getBufferedMutator(tableName);

    int startVal = 0;
    int stopVal = 1000;
    int interval = 47;
    long counter = 0;
    for (int i = startVal; i < stopVal; i += interval, counter++) {
      byte[] rowKey = ByteBuffer.allocate(12).putInt(i).array();

      for(int j = 0; j < 8; ++j) {
        rowKey[4 + j] = (byte)(counter >> (56 - (j * 8)));
      }

      Put p = new Put(rowKey);
      p.addColumn(FAMILY_F, COLUMN_C, "dummy".getBytes());
      table.mutate(p);
    }

    table.close();
  }

  public static void generateHBaseDatasetDoubleOB(Connection conn, Admin admin, TableName tableName, int numberRegions) throws Exception {
    if (admin.tableExists(tableName)) {
      admin.disableTable(tableName);
      admin.deleteTable(tableName);
    }

    HTableDescriptor desc = new HTableDescriptor(tableName);
    desc.addFamily(new HColumnDescriptor(FAMILY_F));

    if (numberRegions > 1) {
      admin.createTable(desc, Arrays.copyOfRange(SPLIT_KEYS, 0, numberRegions-1));
    } else {
      admin.createTable(desc);
    }

    BufferedMutator table = conn.getBufferedMutator(tableName);

    for (double i = 0.5; i <= 100.00; i += 0.75) {
      byte[] bytes = new byte[9];
      PositionedByteRange br = new SimplePositionedMutableByteRange(bytes, 0, 9);
      OrderedBytes.encodeFloat64(br, i, Order.ASCENDING);
      Put p = new Put(bytes);
      p.addColumn(FAMILY_F, COLUMN_C, String.format("value %03f", i).getBytes());
      table.mutate(p);
    }

    table.close();

    admin.flush(tableName);
  }

  public static void generateHBaseDatasetFloatOB(Connection conn, Admin admin, TableName tableName, int numberRegions) throws Exception {
    if (admin.tableExists(tableName)) {
      admin.disableTable(tableName);
      admin.deleteTable(tableName);
    }

    HTableDescriptor desc = new HTableDescriptor(tableName);
    desc.addFamily(new HColumnDescriptor(FAMILY_F));

    if (numberRegions > 1) {
      admin.createTable(desc, Arrays.copyOfRange(SPLIT_KEYS, 0, numberRegions-1));
    } else {
      admin.createTable(desc);
    }

    BufferedMutator table = conn.getBufferedMutator(tableName);

    for (float i = 0.5f; i <= 100.00; i += 0.75f) {
      byte[] bytes = new byte[5];
      PositionedByteRange br = new SimplePositionedMutableByteRange(bytes, 0, 5);
      OrderedBytes.encodeFloat32(br, i,Order.ASCENDING);
      Put p = new Put(bytes);
      p.addColumn(FAMILY_F, COLUMN_C, String.format("value %03f", i).getBytes());
      table.mutate(p);
    }

    table.close();

    admin.flush(tableName);
  }

  public static void generateHBaseDatasetBigIntOB(Connection conn, Admin admin, TableName tableName, int numberRegions) throws Exception {
    if (admin.tableExists(tableName)) {
      admin.disableTable(tableName);
      admin.deleteTable(tableName);
    }

    HTableDescriptor desc = new HTableDescriptor(tableName);
    desc.addFamily(new HColumnDescriptor(FAMILY_F));

    if (numberRegions > 1) {
      admin.createTable(desc, Arrays.copyOfRange(SPLIT_KEYS, 0, numberRegions-1));
    } else {
      admin.createTable(desc);
    }

    BufferedMutator table = conn.getBufferedMutator(tableName);
    long startTime = (long)1438034423 * 1000;
    for (long i = startTime; i <= startTime + 100; i++) {
      byte[] bytes = new byte[9];
      PositionedByteRange br = new SimplePositionedMutableByteRange(bytes, 0, 9);
      OrderedBytes.encodeInt64(br, i, Order.ASCENDING);
      Put p = new Put(bytes);
      p.addColumn(FAMILY_F, COLUMN_C, String.format("value %d", i).getBytes());
      table.mutate(p);
    }

    table.close();

    admin.flush(tableName);
  }

  public static void generateHBaseDatasetIntOB(Connection conn, Admin admin, TableName tableName, int numberRegions) throws Exception {
    if (admin.tableExists(tableName)) {
      admin.disableTable(tableName);
      admin.deleteTable(tableName);
    }

    HTableDescriptor desc = new HTableDescriptor(tableName);
    desc.addFamily(new HColumnDescriptor(FAMILY_F));

    if (numberRegions > 1) {
      admin.createTable(desc, Arrays.copyOfRange(SPLIT_KEYS, 0, numberRegions-1));
    } else {
      admin.createTable(desc);
    }

    BufferedMutator table = conn.getBufferedMutator(tableName);

    for (int i = -49; i <= 100; i++) {
      byte[] bytes = new byte[5];
      PositionedByteRange br = new SimplePositionedMutableByteRange(bytes, 0, 5);
      OrderedBytes.encodeInt32(br, i, Order.ASCENDING);
      Put p = new Put(bytes);
      p.addColumn(FAMILY_F, COLUMN_C, String.format("value %d", i).getBytes());
      table.mutate(p);
    }

    table.close();

    admin.flush(tableName);
  }

  public static void generateHBaseDatasetDoubleOBDesc(Connection conn, Admin admin, TableName tableName, int numberRegions) throws Exception {
    if (admin.tableExists(tableName)) {
      admin.disableTable(tableName);
      admin.deleteTable(tableName);
    }

    HTableDescriptor desc = new HTableDescriptor(tableName);
    desc.addFamily(new HColumnDescriptor(FAMILY_F));

    if (numberRegions > 1) {
      admin.createTable(desc, Arrays.copyOfRange(SPLIT_KEYS, 0, numberRegions-1));
    } else {
      admin.createTable(desc);
    }

    BufferedMutator table = conn.getBufferedMutator(tableName);

    for (double i = 0.5; i <= 100.00; i += 0.75) {
      byte[] bytes = new byte[9];
      PositionedByteRange br = new SimplePositionedMutableByteRange(bytes, 0, 9);
      OrderedBytes.encodeFloat64(br, i, Order.DESCENDING);
      Put p = new Put(bytes);
      p.addColumn(FAMILY_F, COLUMN_C, String.format("value %03f", i).getBytes());
      table.mutate(p);
    }

    table.close();

    admin.flush(tableName);
  }

  public static void generateHBaseDatasetFloatOBDesc(Connection conn, Admin admin, TableName tableName, int numberRegions) throws Exception {
    if (admin.tableExists(tableName)) {
      admin.disableTable(tableName);
      admin.deleteTable(tableName);
    }

    HTableDescriptor desc = new HTableDescriptor(tableName);
    desc.addFamily(new HColumnDescriptor(FAMILY_F));

    if (numberRegions > 1) {
      admin.createTable(desc, Arrays.copyOfRange(SPLIT_KEYS, 0, numberRegions-1));
    } else {
      admin.createTable(desc);
    }

    BufferedMutator table = conn.getBufferedMutator(tableName);

    for (float i = 0.5f; i <= 100.00; i += 0.75f) {
      byte[] bytes = new byte[5];
      PositionedByteRange br = new SimplePositionedMutableByteRange(bytes, 0, 5);
      OrderedBytes.encodeFloat32(br, i, Order.DESCENDING);
      Put p = new Put(bytes);
      p.addColumn(FAMILY_F, COLUMN_C, String.format("value %03f", i).getBytes());
      table.mutate(p);
    }

    table.close();

    admin.flush(tableName);
  }

  public static void generateHBaseDatasetBigIntOBDesc(Connection conn, Admin admin, TableName tableName, int numberRegions) throws Exception {
    if (admin.tableExists(tableName)) {
      admin.disableTable(tableName);
      admin.deleteTable(tableName);
    }

    HTableDescriptor desc = new HTableDescriptor(tableName);
    desc.addFamily(new HColumnDescriptor(FAMILY_F));

    if (numberRegions > 1) {
      admin.createTable(desc, Arrays.copyOfRange(SPLIT_KEYS, 0, numberRegions-1));
    } else {
      admin.createTable(desc);
    }

    BufferedMutator table = conn.getBufferedMutator(tableName);
    long startTime = (long)1438034423 * 1000;
    for (long i = startTime; i <= startTime + 100; i++) {
      byte[] bytes = new byte[9];
      PositionedByteRange br = new SimplePositionedMutableByteRange(bytes, 0, 9);
      OrderedBytes.encodeInt64(br, i, Order.DESCENDING);
      Put p = new Put(bytes);
      p.addColumn(FAMILY_F, COLUMN_C, String.format("value %d", i).getBytes());
      table.mutate(p);
    }

    table.close();

    admin.flush(tableName);
  }


  public static void generateHBaseDatasetIntOBDesc(Connection conn, Admin admin, TableName tableName, int numberRegions) throws Exception {
    if (admin.tableExists(tableName)) {
      admin.disableTable(tableName);
      admin.deleteTable(tableName);
    }

    HTableDescriptor desc = new HTableDescriptor(tableName);
    desc.addFamily(new HColumnDescriptor(FAMILY_F));

    if (numberRegions > 1) {
      admin.createTable(desc, Arrays.copyOfRange(SPLIT_KEYS, 0, numberRegions-1));
    } else {
      admin.createTable(desc);
    }

    BufferedMutator table = conn.getBufferedMutator(tableName);

    for (int i = -49; i <= 100; i++) {
      byte[] bytes = new byte[5];
      PositionedByteRange br = new SimplePositionedMutableByteRange(bytes, 0, 5);
      OrderedBytes.encodeInt32(br, i, Order.DESCENDING);
      Put p = new Put(bytes);
      p.addColumn(FAMILY_F, COLUMN_C, String.format("value %d", i).getBytes());
      table.mutate(p);
    }

    table.close();

    admin.flush(tableName);
  }

  public static void generateHBaseDatasetNullStr(Connection conn, Admin admin, TableName tableName, int numberRegions) throws Exception {
    if (admin.tableExists(tableName)) {
      admin.disableTable(tableName);
      admin.deleteTable(tableName);
    }

    HTableDescriptor desc = new HTableDescriptor(tableName);
    desc.addFamily(new HColumnDescriptor("f"));
    if (numberRegions > 1) {
      admin.createTable(desc, Arrays.copyOfRange(SPLIT_KEYS, 0, numberRegions-1));
    } else {
      admin.createTable(desc);
    }

    BufferedMutator table = conn.getBufferedMutator(tableName);

    Put p = new Put("a1".getBytes());
    p.addColumn("f".getBytes(), "c1".getBytes(), "".getBytes());
    p.addColumn("f".getBytes(), "c2".getBytes(), "".getBytes());
    p.addColumn("f".getBytes(), "c3".getBytes(), "5".getBytes());
    p.addColumn("f".getBytes(), "c4".getBytes(), "".getBytes());
    table.mutate(p);

    table.close();
  }

}
