package com.netease.arctic.spark;

import com.netease.arctic.data.ChangeAction;
import com.netease.arctic.spark.sql.utils.ProjectingInternalRow;
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
  private ChangeAction changeAction = ChangeAction.INSERT;
  private List<DataType> dataTypeList;

  public SparkInternalRowCastWrapper(InternalRow row, ChangeAction changeAction, StructType schema) {
    this.row = row;
    this.changeAction = changeAction;
    if (row instanceof ProjectingInternalRow) {
      this.schema = ((ProjectingInternalRow) row).schema();
    } else {
      this.schema = schema;
    }
  }

  public StructType getSchema() {
    return this.schema;
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
    return row.get(pos, dt);
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
    List<DataType> dataTypeList = Arrays
        .stream(schema.fields()).map(StructField::dataType).collect(Collectors.toList());
    List<Object> objectSeq = new ArrayList<>(dataTypeList.size() + 1);
    row.toSeq(schema).toStream().foreach(objectSeq::add);
    objectSeq.add(fileOffset);
    return new GenericInternalRow(objectSeq.toArray());
  }
}
