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
package org.apache.drill.exec.physical.impl.window;

import org.apache.drill.test.TestTools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GenerateTestData {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(GenerateTestData.class);

  private static final int BATCH_SIZE = 20;

  private static class Builder {
    List<Partition> partitions = new ArrayList<>();

    int cur_length;
    List<Integer> cur_subs = new ArrayList<>();
    List<Integer> cur_subs_size = new ArrayList<>();

    Builder partition(int length) {
      if (cur_length > 0) {
        addPartition();
      }

      cur_length = length;
      cur_subs.clear();
      cur_subs_size.clear();
      return this;
    }

    Builder sub(int subId) {
      return sub(subId, subId);
    }

    Builder sub(int subId, int num) {
      cur_subs.add(subId);
      cur_subs_size.add(num);
      return this;
    }

    void addPartition() {
      partitions.add(
        new Partition(cur_length,
          cur_subs.toArray(new Integer[cur_subs.size()]),
          cur_subs_size.toArray(new Integer[cur_subs_size.size()])));
    }

    Partition[] build() {
      if (cur_length > 0) {
        addPartition();
      }

      // set previous partitions
      for (int i = 1; i < partitions.size(); i++) {
        partitions.get(i).previous = partitions.get(i - 1);
      }

      return partitions.toArray(new Partition[partitions.size()]);
    }
  }

  private static class Partition {
    Partition previous;
    final int length;
    final Integer[] subs;
    final Integer[] subs_sizes;

    public Partition(int length, Integer[] subs, Integer[] sub_sizes) {
      this.length = length;
      this.subs = subs;
      this.subs_sizes = sub_sizes;
    }

    /**
     * @return total number of rows since first partition, this partition included
     */
    public int cumulLength() {
      int prevLength = previous != null ? previous.cumulLength() : 0;
      return length + prevLength;
    }

    public boolean isPartOf(int rowNumber) {
      int prevLength = previous != null ? previous.cumulLength() : 0;
      return rowNumber >= prevLength && rowNumber < cumulLength();
    }

    public int getSubIndex(final int sub) {
      return Arrays.binarySearch(subs, sub);
    }

    public int getSubSize(int sub) {
      if (sub != subs[subs.length - 1]) {
        return subs_sizes[getSubIndex(sub)];
      } else {
        //last sub has enough rows to reach partition length
        int size = length;
        for (int i = 0; i < subs.length - 1; i++) {
          size -= subs_sizes[i];
        }
        return size;
      }
    }

    /**
     * @return sub id of the sub that contains rowNumber
     */
    public int getSubId(int rowNumber) {
      assert isPartOf(rowNumber) : "row "+rowNumber+" isn't part of this partition";

      int prevLength = previous != null ? previous.cumulLength() : 0;
      rowNumber -= prevLength; // row num from start of this partition

      for (int s : subs) {
        if (rowNumber < subRunningCount(s)) {
          return s;
        }
      }

      throw new RuntimeException("should never happen!");
    }

    /**
     * @return running count of rows from first row of the partition to current sub, this sub included
     */
    public int subRunningCount(int sub) {
      int count = 0;
      for (int s : subs) {
        count += getSubSize(s);
        if (s == sub) {
          break;
        }
      }
      return count;
    }

    /**
     * @return running sum of salaries from first row of the partition to current sub, this sub included
     */
    public int subRunningSum(int sub) {
      int sum = 0;
      for (int s : subs) {
        sum += (s+10) * getSubSize(s);
        if (s == sub) {
          break;
        }
      }
      return sum;
    }

    /**
     * @return sum of salaries for all rows of the partition
     */
    public int totalSalary() {
      return subRunningSum(subs[subs.length-1]);
    }

  }

  private static Partition[] dataB1P1() {
    // partition rows 20, subs [1, 2, 3, 4, 5, 6]
    return new Builder()
      .partition(20).sub(1).sub(2).sub(3).sub(4).sub(5).sub(6)
      .build();
  }

  private static Partition[] dataB1P2(boolean pby) {
    // partition rows 10, subs [1, 2, 3, 4]
    // partition rows 10, subs [4, 5, 6]
    if (pby) {
      return new Builder()
        .partition(10).sub(1).sub(2).sub(3).sub(4)
        .partition(10).sub(4).sub(5).sub(6)
        .build();
    } else {
      return new Builder()
        .partition(20).sub(1).sub(2).sub(3).sub(4, 8).sub(5).sub(6)
        .build();
    }
  }

  private static Partition[] dataB2P2(boolean pby) {
    // partition rows 20, subs [3, 5, 9]
    // partition rows 20, subs [9, 10]
    if (pby) {
      return new Builder()
        .partition(20).sub(3).sub(5).sub(9)
        .partition(20).sub(9).sub(10)
        .build();
    } else {
      return new Builder()
        .partition(40).sub(3).sub(5).sub(9, 12 + 9).sub(10)
        .build();
    }
  }

  private static Partition[] dataB2P4(boolean pby) {
    // partition rows 5, subs [1, 2, 3]
    // partition rows 10, subs [3, 4, 5]
    // partition rows 15, subs [5, 6, 7]
    // partition rows 10, subs [7, 8]
    if (pby) {
      return new Builder()
        .partition(5).sub(1).sub(2).sub(3)
        .partition(10).sub(3).sub(4).sub(5)
        .partition(15).sub(5).sub(6).sub(7)
        .partition(10).sub(7).sub(8)
        .build();
    } else {
      return new Builder()
        .partition(40).sub(1).sub(2).sub(3, 5).sub(4).sub(5, 8).sub(6).sub(7, 11).sub(8)
        .build();
    }
  }

  private static Partition[] dataB3P2(boolean pby) {
    // partition rows 5, subs [1, 2, 3]
    // partition rows 55, subs [4, 5, 7, 8, 9, 10, 11, 12]
    if (pby) {
      return new Builder()
        .partition(5).sub(1).sub(2).sub(3)
        .partition(55).sub(4).sub(5).sub(7).sub(8).sub(9).sub(10).sub(11).sub(12)
        .build();
    } else {
      return new Builder()
        .partition(60).sub(1).sub(2).sub(3, 2).sub(4).sub(5).sub(7).sub(8).sub(9).sub(10).sub(11).sub(12)
        .build();
    }
  }

  private static Partition[] dataB4P4(boolean pby) {
    // partition rows 10, subs [1, 2, 3]
    // partition rows 30, subs [3, 4, 5, 6, 7, 8]
    // partition rows 20, subs [8, 9, 10]
    // partition rows 20, subs [10, 11]
    if (pby) {
      return new Builder()
        .partition(10).sub(1).sub(2).sub(3)
        .partition(30).sub(3).sub(4).sub(5).sub(6).sub(7).sub(8)
        .partition(20).sub(8).sub(9).sub(10)
        .partition(20).sub(10).sub(11)
        .build();
    } else {
      return new Builder()
        .partition(80).sub(1).sub(2).sub(3, 10)
        .sub(4).sub(5).sub(6).sub(7).sub(8, 13)
        .sub(9).sub(10, 13).sub(11, 10)
        .build();
    }
  }

  private static void writeData(final Path path, final Partition[] partitions, final boolean addLineNo)
      throws FileNotFoundException {

    // total number of rows
    int total = partitions[partitions.length - 1].cumulLength();

    // create data rows in random order
    List<Integer> emp_ids = new ArrayList<>(total);
    for (int i = 0; i < total; i++) {
      emp_ids.add(i);
    }
    Collections.shuffle(emp_ids);

    // data file(s)
    int fileId = 0;
    PrintStream dataStream = new PrintStream(path.resolve(fileId + ".data.json").toFile());

    int emp_idx = 0;
    int lineNo = 0;
    for (int id : emp_ids) {
      int p = 0;
      while (!partitions[p].isPartOf(id)) { // emp x is @ row x-1
        p++;
      }

      int sub = partitions[p].getSubId(id);
      int salary = 10 + sub;

      if (addLineNo) {
        dataStream.printf("{ \"employee_id\":%d, \"position_id\":%d, \"sub\":%d, \"salary\":%d, \"line_no\":%d }%n",
          id, p + 1, sub, salary, lineNo);
      } else {
        dataStream.printf("{ \"employee_id\":%d, \"position_id\":%d, \"sub\":%d, \"salary\":%d }%n", id, p + 1, sub, salary);
      }
      emp_idx++;
      if ((emp_idx % BATCH_SIZE)==0 && emp_idx < total) {
        dataStream.close();
        fileId++;
        dataStream = new PrintStream(path + "/" + fileId + ".data.json");
      }

      lineNo++;
    }

    dataStream.close();
  }

  private static void writeResults(final Path path, final String prefix, final Partition[] partitions) throws FileNotFoundException {
    // expected results for query without order by clause
    final PrintStream resultStream = new PrintStream(path.toString() + prefix + ".tsv");
    // expected results for query with order by clause
    final PrintStream resultOrderStream = new PrintStream(path.toString() + prefix + ".oby.tsv");

    int idx = 0;
    for (final Partition partition : partitions) {
      for (int i = 0; i < partition.length; i++, idx++) {

        final int sub = partition.getSubId(idx);
        final int rowNumber = i + 1;
        final int rank = 1 + partition.subRunningCount(sub) - partition.getSubSize(sub);
        final int denseRank = partition.getSubIndex(sub) + 1;
        final double cumeDist = (double) partition.subRunningCount(sub) / partition.length;
        final double percentRank = partition.length == 1 ? 0 : (double)(rank - 1)/(partition.length - 1);

        // each line has: count(*)  sum(salary)  row_number()  rank()  dense_rank()  cume_dist()  percent_rank()
        resultOrderStream.printf("%d\t%d\t%d\t%d\t%d\t%s\t%s%n",
          partition.subRunningCount(sub), partition.subRunningSum(sub),
          rowNumber, rank, denseRank, Double.toString(cumeDist), Double.toString(percentRank));

        // each line has: count(*)  sum(salary)
        resultStream.printf("%d\t%d%n", partition.length, partition.totalSalary());
      }
    }

    resultStream.close();
    resultOrderStream.close();
  }

  private static void generateData(final String tableName, final Partition[] pby_data, final Partition[] nopby_data)
      throws FileNotFoundException {
    generateData(tableName, pby_data, nopby_data, false);
  }

  private static void generateData(final String tableName, final Partition[] pby_data, final Partition[] nopby_data,
      final boolean addLineNo) throws FileNotFoundException {
    final Path path = TestTools.WORKING_PATH
      .resolve(TestTools.TEST_RESOURCES_REL)
      .resolve(Paths.get("window", tableName));

    final File pathFolder = path.toFile();

    if (!pathFolder.exists()) {
      if (!pathFolder.mkdirs()) {
        logger.error("Couldn't create folder {}, exiting", path);
      }
    }

    writeData(path, pby_data, addLineNo);
    writeResults(path, "", nopby_data);
    writeResults(path, ".pby", pby_data);
  }

  public static void main(String[] args) throws FileNotFoundException {
    generateData("b1.p1", dataB1P1(), dataB1P1());
    generateData("b1.p2", dataB1P2(true), dataB1P2(false));
    generateData("b2.p2", dataB2P2(true), dataB2P2(false));
    generateData("b2.p4", dataB2P4(true), dataB2P4(false));
    generateData("b3.p2", dataB3P2(true), dataB3P2(false));
    generateData("b4.p4", dataB4P4(true), dataB4P4(false), true);
  }

}
