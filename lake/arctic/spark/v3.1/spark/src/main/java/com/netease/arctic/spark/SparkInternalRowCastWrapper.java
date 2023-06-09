package com.netease.arctic.spark;

import com.netease.arctic.data.ChangeAction;
import com.netease.arctic.spark.sql.utils.ProjectingInternalRow;
import com.netease.arctic.spark.table.SupportsUpsert;
import org.apache.spark.sql.catalyst.InternalRow;
import org.apache.spark.sql.catalyst.expressions.GenericInternalRow;
import org.apache.spark.sql.catalyst.util.ArrayData;
import org.apache.spark.sql.catalyst.util.MapData;
import org.apache.spark.sql.types.DataType;
import org.apache.spark.sql.types.Decimal;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.apache.spark.unsafe.types.CalendarInterval;
import org.apache.spark.unsafe.types.UTF8String;
import scala.collection.Seq;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * cast internal row to upsert internal row
 */
public class SparkInternalRowCastWrapper extends GenericInternalRow {
  private final InternalRow row;
  private final StructType schema;
  private int middle;
  private boolean isDelete;
  private boolean isMerge;
  private boolean isKeyedTable;
  private ChangeAction changeAction = ChangeAction.INSERT;
  private List<DataType> dataTypeList;

  public SparkInternalRowCastWrapper(InternalRow row, StructType schema, ChangeAction changeAction, boolean isDelete) {
    StructType newSchema = new StructType(Arrays.stream(schema.fields())
            .filter(field -> !field.name().equals(SupportsUpsert.UPSERT_OP_COLUMN_NAME)).toArray(StructField[]::new));
    this.row = buildInternalRow(row, schema, changeAction);
    this.schema = newSchema;
    this.middle = newSchema.size() / 2;
    if (isUpsertRow(row, newSchema)) {
      this.changeAction = ChangeAction.INSERT;
    } else {
      this.changeAction = changeAction;
    }
    this.isDelete = isDelete;
    this.isMerge = false;
  }

  public SparkInternalRowCastWrapper(InternalRow row, ChangeAction changeAction) {
    this.row = row;
    this.changeAction = changeAction;
    this.schema = ((ProjectingInternalRow) row).schema();
    this.isMerge = true;
  }

  /**
   * Insert into unUpsert table Construct
   * @param row
   * @param schema
   * @param changeAction INSERT or DELETE
   * @return InternalRow like
   * +---+----+----+
   * | id|name|data|
   * +---+----+----+
   * |  3| ddd|cbcd|
   * +---+----+----+
   */
  public SparkInternalRowCastWrapper(InternalRow row, StructType schema,
                                     ChangeAction changeAction, boolean isDelete, boolean isUpsert) {
    if (!isUpsert) {
      this.row = buildSimpleInternalRow(row, schema, 0, schema.size());
    } else {
      this.row = row;
    }
    this.schema = schema;
    this.middle = schema.size();
    this.changeAction = changeAction;
    this.isDelete = isDelete;
    this.isMerge = false;
  }

  /**
   * Update unKeyed table insert file Construct
   * unkeyed insert row like
   +-----------------+---+----+----+--------------------+----+
   |_arctic_upsert_op| id|name|data|               _file|_pos|
   +-----------------+---+----+----+--------------------+----+
   |                I|  3| ddd|cbcd|file:/Users/...|   2|
   +-----------------+---+----+----+--------------------+----+
   * @param row
   * @param schema
   * @param changeAction INSERT or DELETE
   * @return InternalRow like
   * +---+----+----+
   * | id|name|data|
   * +---+----+----+
   * |  3| ddd|cbcd|
   * +---+----+----+
   */
  public SparkInternalRowCastWrapper(InternalRow row, StructType schema, ChangeAction changeAction) {
    dataTypeList = Arrays.stream(schema.fields())
        .map(StructField::dataType).collect(Collectors.toList());
    if (Arrays.stream(schema.fieldNames()).findFirst().get().equals(SupportsUpsert.UPSERT_OP_COLUMN_NAME)) {
      List<Object> rows = new ArrayList<>();
      GenericInternalRow genericInternalRow = null;
      for (int i = 1; i < schema.size() - 2; i++) {
        rows.add(row.get(i, dataTypeList.get(i)));
      }
      genericInternalRow = new GenericInternalRow(rows.toArray());

      this.row = genericInternalRow;
    } else {
      this.row = row;
    }
    StructType newSchema = new StructType(Arrays.stream(schema.fields())
        .filter(field -> !field.name().equals(SupportsUpsert.UPSERT_OP_COLUMN_NAME)).toArray(StructField[]::new));
    this.schema = newSchema;
    this.middle = newSchema.size() / 2;
    this.changeAction = changeAction;
    this.isDelete = false;
    this.isMerge = false;
  }

  private boolean isUpsertRow(InternalRow row, StructType schema) {
    for (int i = 0; i < schema.size() / 2; i++) {
      if (!row.isNullAt(i)) {
        return false;
      }
    }
    return true;
  }

  /**
   *
   * @param row like
   * 1. delete file
   * +-----------------+---+----+----+--------------------+----+
   * |_arctic_upsert_op| id|name|data|               _file|_pos|
   * +-----------------+---+----+----+--------------------+----+
   * |                D|  3| ccc|cbcd|file:/Users/...|   2|
   * +-----------------+---+----+----+--------------------+----+
   * 2.update keyed table file
   * +---+----+----+----------------+------------------+------------------+
   * | id|name|data|_arctic_after_id|_arctic_after_name|_arctic_after_data|
   * +---+----+----+----------------+------------------+------------------+
   * |  3| ccc|cbcd|               3|               ddd|              cccc|
   * +---+----+----+----------------+------------------+------------------+
   * 3.insert into keyed table file
   * +---+----+----+----------------+------------------+------------------+
   * | id|name|data|_arctic_after_id|_arctic_after_name|_arctic_after_data|
   * +---+----+----+----------------+------------------+------------------+
   * |null|null|null|               3|               ddd|             cccc|
   * +---+----+----+----------------+------------------+------------------+
   * @param schema
   * @param changeAction
   * @return InternalRow like
   * 1. delete file
   * +---+----+----+--------------------+----+
   * | id|name|data|               _file|_pos|
   * +---+----+----+--------------------+----+
   * |  3| ccc|cbcd|file:/Users/...|   2|
   * +---+----+----+--------------------+----+
   * 2.update keyed table file
   * +---+----+----+
   * | id|name|data|
   * +---+----+----+
   * |  3| ddd|cccc|
   * |  3| ccc|cbcd|
   * +---+----+----+
   * 3.insert into keyed table file
   * +---+----+----+
   * | id|name|data|
   * +---+----+----+
   * |  3| ddd|cccc|
   * +---+----+----+
   */
  private InternalRow buildInternalRow(InternalRow row, StructType schema, ChangeAction changeAction) {
    dataTypeList = Arrays.stream(schema.fields())
        .map(StructField::dataType).collect(Collectors.toList());
    if (Arrays.stream(schema.fieldNames()).findFirst().get().equals(SupportsUpsert.UPSERT_OP_COLUMN_NAME)) {
      return buildSimpleInternalRow(row, schema, 1, schema.size());
    } else {
      int middle = schema.size() / 2;
      List<Object> rows = new ArrayList<>();
      if (changeAction.equals(ChangeAction.UPDATE_AFTER)) {
        GenericInternalRow genericInternalRow = null;
        for (int i = middle; i < schema.size(); i++) {
          rows.add(row.get(i, dataTypeList.get(i)));
        }
        genericInternalRow = new GenericInternalRow(rows.toArray());

        for (int i = middle, j = 0; i < schema.size() && j < middle; i++, j++) {
          genericInternalRow.update(j, row.get(i, dataTypeList.get(i)));
        }
        return genericInternalRow;
      } else {
        if (isUpsertRow(row, schema)) {
          return null;
        } else {
          return row;
        }
      }
    }
  }

  private InternalRow buildSimpleInternalRow(InternalRow row, StructType schema, int start, int end) {
    dataTypeList = Arrays.stream(schema.fields())
        .map(StructField::dataType).collect(Collectors.toList());
    List<Object> rows = new ArrayList<>();
    for (int i = start; i < end; i++) {
      rows.add(row.get(i, dataTypeList.get(i)));
    }
    return new GenericInternalRow(rows.toArray());
  }

  @Override
  public Object genericGet(int ordinal) {
    return row.get(ordinal, schema.apply(ordinal).dataType());
  }

  @Override
  public Seq<Object> toSeq(Seq<DataType> fieldTypes) {
    return super.toSeq(fieldTypes);
  }

  @Override
  public int numFields() {
    return schema.size() / 2;
  }

  @Override
  public void setNullAt(int i) {
    super.setNullAt(i);
  }

  @Override
  public void update(int i, Object value) {
    super.update(i, value);
  }

  @Override
  public boolean isNullAt(int ordinal) {
    dataTypeList = Arrays.stream(schema.fields())
        .map(StructField::dataType).collect(Collectors.toList());
    return row.get(ordinal, dataTypeList.get(ordinal)) == null;
  }

  @Override
  public Object get(int pos, DataType dt) {
    if (row.get(pos, dt) == null) {
      return row.get(pos + middle, dt);
    } else {
      return row.get(pos, dt);
    }
  }

  @Override
  public boolean getBoolean(int ordinal) {
    return super.getBoolean(ordinal);
  }

  @Override
  public byte getByte(int ordinal) {
    return super.getByte(ordinal);
  }

  @Override
  public short getShort(int ordinal) {
    return super.getShort(ordinal);
  }

  @Override
  public int getInt(int ordinal) {
    return super.getInt(ordinal);
  }

  @Override
  public long getLong(int ordinal) {
    return super.getLong(ordinal);
  }

  @Override
  public float getFloat(int ordinal) {
    return super.getFloat(ordinal);
  }

  @Override
  public double getDouble(int ordinal) {
    return super.getDouble(ordinal);
  }

  @Override
  public Decimal getDecimal(int ordinal, int precision, int scale) {
    return super.getDecimal(ordinal, precision, scale);
  }

  @Override
  public UTF8String getUTF8String(int ordinal) {
    return super.getUTF8String(ordinal);
  }

  @Override
  public byte[] getBinary(int ordinal) {
    return super.getBinary(ordinal);
  }

  @Override
  public ArrayData getArray(int ordinal) {
    return super.getArray(ordinal);
  }

  @Override
  public CalendarInterval getInterval(int ordinal) {
    return super.getInterval(ordinal);
  }

  @Override
  public MapData getMap(int ordinal) {
    return super.getMap(ordinal);
  }

  @Override
  public InternalRow getStruct(int ordinal, int numFields) {
    return super.getStruct(ordinal, numFields);
  }

  public InternalRow getRow() {
    return this.row;
  }


  public ChangeAction getChangeAction() {
    return changeAction;
  }

  @Override
  public String toString() {
    return super.toString();
  }

  @Override
  public GenericInternalRow copy() {
    return super.copy();
  }

  @Override
  public boolean equals(Object o) {
    return super.equals(o);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public Object[] values() {
    return super.values();
  }

  public InternalRow setFileOffset(Long fileOffset) {
    List<DataType> dataTypeList;
    List<Object> rows;
    if (isDelete) {
      dataTypeList = Arrays.stream(schema.fields())
          .filter(field -> !field.name().equals(SupportsUpsert.UPSERT_OP_COLUMN_NAME))
          .map(StructField::dataType).collect(Collectors.toList());
      rows = new ArrayList<>(schema.size());
      for (int i = 0; i < schema.size(); i++) {
        rows.add(row.get(i, dataTypeList.get(i)));
      }
    } else if (isMerge) {
      dataTypeList = Arrays.stream(schema.fields()).map(StructField::dataType).collect(Collectors.toList());
      rows = new ArrayList<>(dataTypeList.size() + 1);
      for (int i = 0; i < dataTypeList.size(); i++) {
        rows.add(row.get(i, dataTypeList.get(i)));
      }
    } else {
      dataTypeList = Arrays.stream(schema.fields()).map(StructField::dataType).collect(Collectors.toList());
      rows = new ArrayList<>(middle + 1);
      for (int i = 0; i < middle; i++) {
        rows.add(row.get(i, dataTypeList.get(i)));
      }
    }
    rows.add(fileOffset);
    return new GenericInternalRow(rows.toArray());
  }
}
