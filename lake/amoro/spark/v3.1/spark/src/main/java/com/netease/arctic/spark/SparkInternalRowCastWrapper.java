package com.netease.arctic.spark;

import com.netease.arctic.data.ChangeAction;
import com.netease.arctic.spark.sql.utils.ProjectingInternalRow;
import org.apache.spark.sql.catalyst.InternalRow;
import org.apache.spark.sql.catalyst.expressions.GenericInternalRow;
import org.apache.spark.sql.types.DataType;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

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
  public int numFields() {
    return schema.size() / 2;
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

  public InternalRow getRow() {
    return this.row;
  }


  public ChangeAction getChangeAction() {
    return changeAction;
  }

  public InternalRow setFileOffset(Long fileOffset) {
    List<DataType> dataTypeList = Arrays
        .stream(schema.fields()).map(StructField::dataType).collect(Collectors.toList());
    List<Object> objectSeq = new ArrayList<>(dataTypeList.size() + 1);;
    row.toSeq(schema).toStream().foreach(objectSeq::add);
    objectSeq.add(fileOffset);
    return new GenericInternalRow(objectSeq.toArray());
  }
}
