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
package org.apache.drill.exec.store.easy.json;

import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.planner.common.DrillStatsTable;
import org.apache.drill.exec.store.EventBasedRecordWriter.FieldConverter;
import org.apache.drill.exec.store.JSONBaseStatisticsRecordWriter;
import org.apache.drill.exec.vector.complex.reader.FieldReader;
import org.apache.drill.metastore.statistics.Statistic;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class StatisticsCollectorImpl extends JSONBaseStatisticsRecordWriter {

  private final List<DrillStatsTable.ColumnStatistics> columnStatisticsList = new ArrayList<>();

  private String nextField = null;
  private DrillStatsTable.ColumnStatistics columnStatistics;
  private LocalDate dirComputedTime = null;
  private boolean errStatus = false;

  @Override
  public void startStatisticsRecord() {
    columnStatistics = new DrillStatsTable.ColumnStatistics_v1();
  }

  @Override
  public void endStatisticsRecord() {
    columnStatisticsList.add(columnStatistics);
  }

  @Override
  public boolean hasStatistics() {
    return !columnStatisticsList.isEmpty();
  }

  public DrillStatsTable.TableStatistics getStatistics() {
    return DrillStatsTable.generateDirectoryStructure(dirComputedTime.toString(),
        columnStatisticsList);
  }

  public boolean hasErrors() {
    return errStatus;
  }

  @Override
  public FieldConverter getNewBigIntConverter(int fieldId, String fieldName, FieldReader reader) {
    return new BigIntJsonConverter(fieldId, fieldName, reader);
  }

  @Override
  public FieldConverter getNewIntConverter(int fieldId, String fieldName, FieldReader reader) {
    return new IntJsonConverter(fieldId, fieldName, reader);
  }

  @Override
  public FieldConverter getNewDateConverter(int fieldId, String fieldName, FieldReader reader) {
    return new DateJsonConverter(fieldId, fieldName, reader);
  }

  @Override
  public FieldConverter getNewVarCharConverter(int fieldId, String fieldName, FieldReader reader) {
    return new VarCharJsonConverter(fieldId, fieldName, reader);
  }

  @Override
  public FieldConverter getNewNullableBigIntConverter(int fieldId, String fieldName, FieldReader reader) {
    return new NullableBigIntJsonConverter(fieldId, fieldName, reader);
  }

  @Override
  public FieldConverter getNewNullableVarBinaryConverter(int fieldId, String fieldName, FieldReader reader) {
    return new NullableVarBinaryJsonConverter(fieldId, fieldName, reader);
  }

  @Override
  public FieldConverter getNewNullableFloat8Converter(int fieldId, String fieldName, FieldReader reader) {
    return new NullableFloat8JsonConverter(fieldId, fieldName, reader);
  }

  public class BigIntJsonConverter extends FieldConverter {

    public BigIntJsonConverter(int fieldId, String fieldName, FieldReader reader) {
      super(fieldId, fieldName, reader);
    }

    @Override
    public void startField() throws IOException {
      switch (fieldName) {
        case Statistic.SCHEMA:
        case Statistic.ROWCOUNT:
        case Statistic.NNROWCOUNT:
        case Statistic.NDV:
        case Statistic.AVG_WIDTH:
        case Statistic.SUM_DUPS:
          nextField = fieldName;
      }
    }

    @Override
    public void writeField() throws IOException {
      if (nextField == null) {
        errStatus = true;
        throw new IOException("Statistics writer encountered unexpected field");
      }
      DrillStatsTable.ColumnStatistics_v1 columnStatistics =
          (DrillStatsTable.ColumnStatistics_v1) StatisticsCollectorImpl.this.columnStatistics;
      switch (nextField) {
        case Statistic.SCHEMA:
          columnStatistics.setSchema(reader.readLong());
          break;
        case Statistic.ROWCOUNT:
          columnStatistics.setCount(reader.readLong());
          break;
        case Statistic.NNROWCOUNT:
          columnStatistics.setNonNullCount(reader.readLong());
          break;
        case Statistic.NDV:
          columnStatistics.setNdv(reader.readLong());
          break;
        case Statistic.AVG_WIDTH:
          columnStatistics.setAvgWidth(reader.readLong());
          break;
        case Statistic.SUM_DUPS:
          // Ignore Count_Approx_Dups statistic
          break;
      }
    }

    @Override
    public void endField() {
      nextField = null;
    }
  }

  public class IntJsonConverter extends FieldConverter {

    public IntJsonConverter(int fieldId, String fieldName, FieldReader reader) {
      super(fieldId, fieldName, reader);
    }

    @Override
    public void startField() throws IOException {
      if (fieldName.equals(Statistic.COLTYPE)) {
        nextField = fieldName;
      }
    }

    @Override
    public void writeField() throws IOException {
      if (nextField == null) {
        errStatus = true;
        throw new IOException("Statistics writer encountered unexpected field");
      }
      if (nextField.equals(Statistic.COLTYPE)) {
        // Do not write out the type
      }
    }

    @Override
    public void endField() {
      nextField = null;
    }
  }

  public class DateJsonConverter extends FieldConverter {

    public DateJsonConverter(int fieldId, String fieldName, FieldReader reader) {
      super(fieldId, fieldName, reader);
    }

    @Override
    public void startField() throws IOException {
      if (fieldName.equals(Statistic.COMPUTED)) {
        nextField = fieldName;
      }
    }

    @Override
    public void writeField() throws IOException {
      if (nextField == null) {
        errStatus = true;
        throw new IOException("Statistics writer encountered unexpected field");
      }
      if (nextField.equals((Statistic.COMPUTED))) {
        LocalDate computedTime = reader.readLocalDate();
        if (dirComputedTime == null
            || computedTime.compareTo(dirComputedTime) > 0) {
          dirComputedTime = computedTime;
        }
      }
    }

    @Override
    public void endField() {
      nextField = null;
    }
  }

  public class VarCharJsonConverter extends FieldConverter {

    public VarCharJsonConverter(int fieldId, String fieldName, FieldReader reader) {
      super(fieldId, fieldName, reader);
    }

    @Override
    public void startField() throws IOException {
      switch (fieldName) {
        case Statistic.COLNAME:
        case Statistic.COLTYPE:
          nextField = fieldName;
      }
    }

    @Override
    public void writeField() throws IOException {
      if (nextField == null) {
        errStatus = true;
        throw new IOException("Statistics writer encountered unexpected field");
      }
      switch (nextField) {
        case Statistic.COLNAME:
          // column name is escaped, so SchemaPath.parseFromString() should be used here
          ((DrillStatsTable.ColumnStatistics_v1) columnStatistics).setName(SchemaPath.parseFromString(reader.readText().toString()));
          break;
        case Statistic.COLTYPE:
          TypeProtos.MajorType fieldType = DrillStatsTable.getMapper().readValue(reader.readText().toString(), TypeProtos.MajorType.class);
          ((DrillStatsTable.ColumnStatistics_v1) columnStatistics).setType(fieldType);
          break;
      }
    }

    @Override
    public void endField() {
      nextField = null;
    }
  }

  public class NullableBigIntJsonConverter extends FieldConverter {

    public NullableBigIntJsonConverter(int fieldId, String fieldName, FieldReader reader) {
      super(fieldId, fieldName, reader);
    }

    @Override
    public void startField() throws IOException {
      if (!skipNullFields || this.reader.isSet()) {
        switch (fieldName) {
          case Statistic.ROWCOUNT:
          case Statistic.NNROWCOUNT:
          case Statistic.NDV:
          case Statistic.SUM_DUPS:
            nextField = fieldName;
            break;
        }
      }
    }

    @Override
    public void writeField() throws IOException {
      if (!skipNullFields || this.reader.isSet()) {
        if (nextField == null) {
          errStatus = true;
          throw new IOException("Statistics writer encountered unexpected field");
        }
        switch (nextField) {
          case Statistic.ROWCOUNT:
            ((DrillStatsTable.ColumnStatistics_v1) columnStatistics).setCount(reader.readLong());
            break;
          case Statistic.NNROWCOUNT:
            ((DrillStatsTable.ColumnStatistics_v1) columnStatistics).setNonNullCount(reader.readLong());
            break;
          case Statistic.NDV:
            ((DrillStatsTable.ColumnStatistics_v1) columnStatistics).setNdv(reader.readLong());
            break;
          case Statistic.SUM_DUPS:
            // Ignore Count_Approx_Dups statistic
            break;
        }
      }
    }

    @Override
    public void endField() {
      nextField = null;
    }
  }

  public class NullableVarBinaryJsonConverter extends FieldConverter {

    public NullableVarBinaryJsonConverter(int fieldId, String fieldName, FieldReader reader) {
      super(fieldId, fieldName, reader);
    }

    @Override
    public void startField() throws IOException {
      if (!skipNullFields || this.reader.isSet()) {
        switch (fieldName) {
          case Statistic.HLL:
          case Statistic.HLL_MERGE:
          case Statistic.TDIGEST_MERGE:
            nextField = fieldName;
            break;
        }
      }
    }

    @Override
    public void writeField() throws IOException {
      if (!skipNullFields || this.reader.isSet()) {
        if (nextField == null) {
          errStatus = true;
          throw new IOException("Statistics writer encountered unexpected field");
        }
        switch (nextField) {
          case Statistic.HLL:
          case Statistic.HLL_MERGE:
            // Do NOT write out the HLL output, since it is not used yet for computing statistics for a
            // subset of partitions in the query OR for computing NDV with incremental statistics.
            break;
          case Statistic.TDIGEST_MERGE:
            byte[] tdigest_bytearray = reader.readByteArray();
            ((DrillStatsTable.ColumnStatistics_v1) columnStatistics).buildHistogram(tdigest_bytearray);
            break;
        }
      }
    }

    @Override
    public void endField() {
      nextField = null;
    }
  }

  public class NullableFloat8JsonConverter extends FieldConverter {

    public NullableFloat8JsonConverter(int fieldId, String fieldName, FieldReader reader) {
      super(fieldId, fieldName, reader);
    }

    @Override
    public void startField() throws IOException {
      if (!skipNullFields || this.reader.isSet()) {
        if (fieldName.equals(Statistic.AVG_WIDTH)) {
          nextField = fieldName;
        }
      }
    }

    @Override
    public void writeField() throws IOException {
      if (!skipNullFields || this.reader.isSet()) {
        if (nextField == null) {
          errStatus = true;
          throw new IOException("Statistics writer encountered unexpected field");
        }
        if (nextField.equals(Statistic.AVG_WIDTH)) {
          ((DrillStatsTable.ColumnStatistics_v1) columnStatistics).setAvgWidth(reader.readDouble());
        }
      }
    }

    @Override
    public void endField() {
      nextField = null;
    }
  }
}
