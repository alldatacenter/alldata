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

package org.apache.drill.exec.planner.index;

import org.apache.drill.common.parser.LogicalExpressionParser;
import com.mapr.db.Admin;
import com.mapr.db.MapRDB;
import com.mapr.db.exceptions.DBException;
import com.mapr.db.index.IndexDesc;
import com.mapr.db.index.IndexDesc.MissingAndNullOrdering;
import com.mapr.db.index.IndexFieldDesc;
import org.apache.calcite.rel.RelFieldCollation;
import org.apache.calcite.rel.RelFieldCollation.NullDirection;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.Types;
import org.apache.drill.common.util.DrillFileUtils;
import org.apache.drill.exec.physical.base.AbstractDbGroupScan;
import org.apache.drill.exec.physical.base.GroupScan;
import org.apache.drill.exec.planner.common.DrillScanRelBase;
import org.apache.drill.exec.planner.logical.DrillTable;
import org.apache.drill.exec.planner.physical.ScanPrel;
import org.apache.drill.exec.store.dfs.DrillFileSystem;
import org.apache.drill.exec.store.dfs.FileSelection;
import org.apache.drill.exec.store.dfs.FileSystemPlugin;
import org.apache.drill.exec.store.mapr.db.MapRDBFormatMatcher;
import org.apache.drill.exec.store.mapr.db.MapRDBFormatPlugin;
import org.apache.drill.exec.store.mapr.db.MapRDBGroupScan;
import org.apache.drill.exec.store.mapr.db.json.FieldPathHelper;
import org.apache.drill.exec.util.ImpersonationUtil;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.security.UserGroupInformation;
import org.ojai.FieldPath;

import java.io.IOException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.drill.shaded.guava.com.google.common.collect.Maps;

public class MapRDBIndexDiscover extends IndexDiscoverBase implements IndexDiscover {

  public static final String DEFAULT_STRING_CAST_LEN_STR = "256";

  public MapRDBIndexDiscover(GroupScan inScan, DrillScanRelBase scanRel) {
    super((AbstractDbGroupScan) inScan, scanRel);
  }

  public MapRDBIndexDiscover(GroupScan inScan, ScanPrel scanRel) {
    super((AbstractDbGroupScan) inScan, scanRel);
  }

  @Override
  public IndexCollection getTableIndex(String tableName) {
    return getTableIndexFromMFS(tableName);
  }

  /**
   * For a given table name get the list of indexes defined on the table according to the visibility of
   * the indexes based on permissions.
   * @param tableName table name
   * @return an IndexCollection representing the list of indexes for that table
   */
  private IndexCollection getTableIndexFromMFS(String tableName) {
    try {
      Set<DrillIndexDescriptor> idxSet = new HashSet<>();
      Collection<IndexDesc> indexes = admin().getTableIndexes(new Path(tableName));
      if (indexes.size() == 0 ) {
        logger.error("No index returned from Admin.getTableIndexes for table {}", tableName);
        return null;
      }
      for (IndexDesc idx : indexes) {
        DrillIndexDescriptor hbaseIdx = buildIndexDescriptor(tableName, idx);
        if (hbaseIdx == null) {
          // not able to build a valid index based on the index info from MFS
          logger.error("Not able to build index for {}", idx.toString());
          continue;
        }
        idxSet.add(hbaseIdx);
      }
      if (idxSet.size() == 0) {
        logger.error("No index found for table {}.", tableName);
        return null;
      }
      return new DrillIndexCollection(getOriginalScanRel(), idxSet);
    } catch (DBException ex) {
      logger.error("Could not get table index from File system.", ex);
    }
    catch(InvalidIndexDefinitionException ex) {
      logger.error("Invalid index definition detected.", ex);
    }
    return null;
  }

  FileSelection deriveFSSelection(DrillFileSystem fs, IndexDescriptor idxDesc) throws IOException {
    String tableName = idxDesc.getTableName();
    String[] tablePath = tableName.split(DrillFileUtils.SEPARATOR);
    String tableParent = tableName.substring(0, tableName.lastIndexOf(DrillFileUtils.SEPARATOR));

    return FileSelection.create(fs, tableParent, tablePath[tablePath.length - 1], false);
  }

  @Override
  public DrillTable getNativeDrillTable(IndexDescriptor idxDescriptor) {

    try {
      final AbstractDbGroupScan origScan = getOriginalScan();
      if (!(origScan instanceof MapRDBGroupScan)) {
        return null;
      }
      MapRDBFormatPlugin maprFormatPlugin = ((MapRDBGroupScan) origScan).getFormatPlugin();
      FileSystemPlugin fsPlugin = (FileSystemPlugin) (origScan.getStoragePlugin());

      DrillFileSystem fs = ImpersonationUtil.createFileSystem(origScan.getUserName(), fsPlugin.getFsConf());
      MapRDBFormatMatcher matcher = (MapRDBFormatMatcher) (maprFormatPlugin.getMatcher());
      FileSelection fsSelection = deriveFSSelection(fs, idxDescriptor);
      return matcher.isReadableIndex(fs, fsSelection, fsPlugin, fsPlugin.getName(),
          origScan.getUserName(), idxDescriptor);

    } catch (Exception e) {
      logger.error("Failed to get native DrillTable.", e);
    }
    return null;
  }

  private SchemaPath fieldName2SchemaPath(String fieldName) {
    if (fieldName.contains(":")) {
      fieldName = fieldName.split(":")[1];
    }
    if (fieldName.contains(".")) {
      return FieldPathHelper.fieldPath2SchemaPath(FieldPath.parseFrom(fieldName));
    }
    return SchemaPath.getSimplePath(fieldName);
  }

  String getDrillTypeStr(String maprdbTypeStr) {
    String typeStr = maprdbTypeStr.toUpperCase();
    String[] typeTokens = typeStr.split("[)(]");
    String typeData = DEFAULT_STRING_CAST_LEN_STR;
    if(typeTokens.length > 1) {
      typeStr = typeTokens[0];
      typeData = typeTokens[1];
    }
    switch(typeStr){
      case "STRING":
        // set default width since it is not specified
        return "VARCHAR("+typeData+")";
      case "LONG":
        return "BIGINT";
      case "INT":
      case "INTEGER":
        return "INT";
      case "FLOAT":
        return "FLOAT4";
      case "DOUBLE":
        return "FLOAT8";
      case "INTERVAL_YEAR_MONTH":
        return "INTERVALYEAR";
      case "INTERVAL_DAY_TIME":
        return "INTERVALDAY";
      case "BOOLEAN":
        return "BIT";
      case "BINARY":
        return "VARBINARY";
      case "ANY":
      case "DECIMAL":
        return null;
      default: return typeStr;
    }

  }

  TypeProtos.MajorType getDrillType(String typeStr) {
    switch(typeStr){
      case "VARCHAR":
      case "CHAR":
      case "STRING":
        // set default width since it is not specified
        return
            Types.required(TypeProtos.MinorType.VARCHAR).toBuilder().setWidth(
                getOriginalScanRel().getCluster().getTypeFactory().createSqlType(SqlTypeName.VARCHAR).getPrecision()).build();
      case "LONG":
      case "BIGINT":
        return Types.required(TypeProtos.MinorType.BIGINT);
      case "INT":
      case "INTEGER":
        return Types.required(TypeProtos.MinorType.INT);
      case "FLOAT":
        return Types.required(TypeProtos.MinorType.FLOAT4);
      case "DOUBLE":
        return Types.required(TypeProtos.MinorType.FLOAT8);
      case "INTERVAL_YEAR_MONTH":
        return Types.required(TypeProtos.MinorType.INTERVALYEAR);
      case "INTERVAL_DAY_TIME":
        return Types.required(TypeProtos.MinorType.INTERVALDAY);
      case "BOOLEAN":
        return Types.required(TypeProtos.MinorType.BIT);
      case "BINARY":
        return Types.required(TypeProtos.MinorType.VARBINARY).toBuilder().build();
      case "ANY":
      case "DECIMAL":
        return null;
      default: return Types.required(TypeProtos.MinorType.valueOf(typeStr));
    }
  }

  private LogicalExpression castFunctionSQLSyntax(String field, String type) throws InvalidIndexDefinitionException {
    // get castTypeStr so we can construct SQL syntax string before MapRDB could provide such syntax
    String castTypeStr = getDrillTypeStr(type);
    if(castTypeStr == null) {  // no cast
      throw new InvalidIndexDefinitionException("cast function type not recognized: " + type + "for field " + field);
    }
    try {
      String castFunc = String.format("cast( %s as %s)", field, castTypeStr);
      return LogicalExpressionParser.parse(castFunc);
    } catch (Exception ex) {
      logger.error("parse failed: {}", ex);
    }
    return null;
  }

  private LogicalExpression getIndexExpression(IndexFieldDesc desc) throws InvalidIndexDefinitionException {
    final String fieldName = desc.getFieldPath().asPathString();
    final String functionDef = desc.getFunctionName();
    if ((functionDef != null)) {  // this is a function
      String[] tokens = functionDef.split("\\s+");
      if (tokens[0].equalsIgnoreCase("cast")) {
        if (tokens.length != 3) {
          throw new InvalidIndexDefinitionException("cast function definition not recognized: " + functionDef);
        }
        LogicalExpression idxExpr = castFunctionSQLSyntax(fieldName, tokens[2]);
        if (idxExpr == null) {
          throw new InvalidIndexDefinitionException("got null expression for function definition: " + functionDef);
        }
        return idxExpr;
      } else {
        throw new InvalidIndexDefinitionException("function definition is not supported for indexing: " + functionDef);
      }
    }
    // else it is a schemaPath
    return fieldName2SchemaPath(fieldName);
  }

  private List<LogicalExpression> field2SchemaPath(Collection<IndexFieldDesc> descCollection)
      throws InvalidIndexDefinitionException {
    List<LogicalExpression> listSchema = new ArrayList<>();
    for (IndexFieldDesc field : descCollection) {
        listSchema.add(getIndexExpression(field));
    }
    return listSchema;
  }

  private List<RelFieldCollation> getFieldCollations(IndexDesc desc, Collection<IndexFieldDesc> descCollection) {
    List<RelFieldCollation> fieldCollations = new ArrayList<>();
    int i = 0;
    for (IndexFieldDesc field : descCollection) {
      RelFieldCollation.Direction direction = (field.getSortOrder() == IndexFieldDesc.Order.Asc) ?
          RelFieldCollation.Direction.ASCENDING : (field.getSortOrder() == IndexFieldDesc.Order.Desc ?
              RelFieldCollation.Direction.DESCENDING : null);
      if (direction != null) {
        // assume null direction of NULLS UNSPECIFIED for now until MapR-DB adds that to the APIs
        RelFieldCollation.NullDirection nulldir =
            direction == RelFieldCollation.Direction.ASCENDING ? NullDirection.LAST :
            (direction == RelFieldCollation.Direction.DESCENDING ?
                NullDirection.FIRST : NullDirection.UNSPECIFIED);
        RelFieldCollation c = new RelFieldCollation(i++, direction, nulldir);
        fieldCollations.add(c);
      } else {
        // if the direction is not present for a field, no need to examine remaining fields
        break;
      }
    }
    return fieldCollations;
  }

  private CollationContext buildCollationContext(List<LogicalExpression> indexFields,
      List<RelFieldCollation> indexFieldCollations) {
    assert indexFieldCollations.size() <= indexFields.size();
    Map<LogicalExpression, RelFieldCollation> collationMap = Maps.newHashMap();
    for (int i = 0; i < indexFieldCollations.size(); i++) {
      collationMap.put(indexFields.get(i), indexFieldCollations.get(i));
    }
    return new CollationContext(collationMap, indexFieldCollations);
  }

  private DrillIndexDescriptor buildIndexDescriptor(String tableName, IndexDesc desc)
      throws InvalidIndexDefinitionException {
    if (desc.isExternal()) {
      // External index is not currently supported
      return null;
    }

    IndexDescriptor.IndexType idxType = IndexDescriptor.IndexType.NATIVE_SECONDARY_INDEX;
    List<LogicalExpression> indexFields = field2SchemaPath(desc.getIndexedFields());
    List<LogicalExpression> coveringFields = field2SchemaPath(desc.getIncludedFields());
    coveringFields.add(SchemaPath.getSimplePath("_id"));
    CollationContext collationContext = null;
    if (!desc.isHashed()) { // hash index has no collation property
      List<RelFieldCollation> indexFieldCollations = getFieldCollations(desc, desc.getIndexedFields());
      collationContext = buildCollationContext(indexFields, indexFieldCollations);
    }

    DrillIndexDescriptor idx = new MapRDBIndexDescriptor (
        indexFields,
        collationContext,
        coveringFields,
        null,
        desc.getIndexName(),
        tableName,
        idxType,
        desc,
        this.getOriginalScan(),
        desc.getMissingAndNullOrdering() == MissingAndNullOrdering.MissingAndNullFirst ? NullDirection.FIRST :
            (desc.getMissingAndNullOrdering() == MissingAndNullOrdering.MissingAndNullLast ?
                NullDirection.LAST : NullDirection.UNSPECIFIED));

    String storageName = this.getOriginalScan().getStoragePlugin().getName();
    materializeIndex(storageName, idx);
    return idx;
  }

  @SuppressWarnings("deprecation")
  private Admin admin() {
    assert getOriginalScan() instanceof MapRDBGroupScan;

    final MapRDBGroupScan dbGroupScan = (MapRDBGroupScan) getOriginalScan();
    final UserGroupInformation currentUser = ImpersonationUtil.createProxyUgi(dbGroupScan.getUserName());
    final Configuration conf = dbGroupScan.getFormatPlugin().getFsConf();

    final Admin admin;
    try {
      admin = currentUser.doAs((PrivilegedExceptionAction<Admin>) () -> MapRDB.getAdmin(conf));
    } catch (Exception e) {
      throw new DrillRuntimeException("Failed to get Admin instance for user: " + currentUser.getUserName(), e);
    }
    return admin;
  }
}
