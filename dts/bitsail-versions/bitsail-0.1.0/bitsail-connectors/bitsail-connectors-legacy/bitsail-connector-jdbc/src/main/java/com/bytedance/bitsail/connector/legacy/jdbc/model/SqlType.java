/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytedance.bitsail.connector.legacy.jdbc.model;

import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.exception.CommonErrorCode;
import com.bytedance.bitsail.connector.legacy.jdbc.exception.DBUtilErrorCode;
import com.bytedance.bitsail.connector.legacy.jdbc.utils.Db2Util;
import com.bytedance.bitsail.connector.legacy.jdbc.utils.MysqlUtil;
import com.bytedance.bitsail.connector.legacy.jdbc.utils.OracleUtil;
import com.bytedance.bitsail.connector.legacy.jdbc.utils.PostgresqlUtil;
import com.bytedance.bitsail.connector.legacy.jdbc.utils.SqlServerUtil;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;

import static org.apache.commons.lang3.StringUtils.lowerCase;

public class SqlType {

  public static SqlTypes getSqlType(final String type, final String driveName) throws BitSailException {
    final String lowerCaseType = lowerCase(type);
    switch (driveName) {
      case MysqlUtil.DRIVER_NAME:
        return getSqlTypeFromMysql(lowerCaseType);
      case OracleUtil.DRIVER_NAME:
        return getSqlTypeFromOracle(lowerCaseType);
      case PostgresqlUtil.DRIVER_NAME:
        return getSqlTypeFromPostgresql(lowerCaseType);
      case Db2Util.DRIVER_NAME:
        return getSqlTypeFromDb2(lowerCaseType);
      case SqlServerUtil.DRIVER_NAME:
        return getSqlTypeFromSqlServer(lowerCaseType);
      default:
        throw new UnsupportedOperationException("current drive: " + driveName + " not supported. Only support mysql and oracle");
    }
  }

  public static SqlTypes getSqlTypeFromOracle(final String type) throws BitSailException {
    switch (type) {
      case "bit":
      case "bool":
        return SqlTypes.Boolean;
      case "smallint":
        return SqlTypes.Short;
      case "integer":
      case "int":
        return SqlTypes.Int;
      case "number":
        return SqlTypes.BigInt;
      case "timestamp":
      case "datetime":
        return SqlTypes.Timestamp;
      case "date":
        return SqlTypes.Date;
      case "float":
      case "binary_float":
        return SqlTypes.Float;
      case "double":
      case "double precision":
      case "real":
      case "binary_double":
        return SqlTypes.Double;
      case "numeric":
      case "decimal":
        return SqlTypes.BigDecimal;
      case "char":
      case "varchar":
      case "varchar2":
      case "nchar":
      case "nvarchar2":
      case "long":
      case "clob":
      case "nclob":
      case "string":
      case "character":
      case "rowid":
      case "urowid":
      case "xmltype":
        return SqlTypes.String;
      case "blob":
      case "bfile":
      case "raw":
      case "long raw":
        return SqlTypes.Bytes;
      default:
        throw BitSailException.asBitSailException(DBUtilErrorCode.UNSUPPORTED_TYPE,
            String.format("Oracle: The column data type in your configuration is not support. Column type:[%s]." +
                " Please try to change the column data type or don't transmit this column.", type)
        );
    }
  }

  public static SqlTypes getSqlTypeFromSqlServer(final String type) throws BitSailException {
    switch (type) {
      case "bit":
        return SqlTypes.Boolean;
      case "smallint":
      case "tinyint":
        return SqlTypes.Short;
      case "int":
      case "int identity":
      case "integer":
        return SqlTypes.Int;
      case "bigint":
        return SqlTypes.Long;
      case "timestamp":
      case "datetime":
      case "datetime2":
        return SqlTypes.Timestamp;
      case "date":
        return SqlTypes.Date;
      case "time":
        return SqlTypes.Time;
      case "float":
        return SqlTypes.Float;
      case "double precision":
      case "real":
        return SqlTypes.Double;
      case "numeric":
      case "decimal":
        return SqlTypes.BigDecimal;
      case "char":
      case "varchar":
      case "text":
      case "nchar":
      case "nvarchar":
      case "ntext":
        return SqlTypes.String;
      case "binary":
      case "varbinary":
      case "image":
        return SqlTypes.Bytes;
      default:
        throw BitSailException.asBitSailException(DBUtilErrorCode.UNSUPPORTED_TYPE,
            String.format("SQL: The column data type in your configuration is not support. Column type:[%s]." +
                " Please try to change the column data type or don't transmit this column.", type)
        );
    }
  }

  private static SqlTypes getSqlTypeFromDb2(final String type) throws BitSailException {
    switch (type) {
      case "smallint":
        return SqlTypes.Short;
      case "integer":
      case "int":
        return SqlTypes.Int;
      case "bigint":
        return SqlTypes.Long;
      case "timestamp":
        return SqlTypes.Timestamp;
      case "date":
        return SqlTypes.Date;
      case "time":
        return SqlTypes.Time;
      case "float":
        return SqlTypes.Float;
      case "double":
      case "real":
        return SqlTypes.Double;
      case "numeric":
      case "decimal":
      case "decfloat":
        return SqlTypes.BigDecimal;
      case "char":
      case "nchar":
      case "varchar":
      case "nvarchar":
      case "graphic":
      case "vargraphic":
      case "clob":
      case "nclob":
      case "dbclob":
      case "character":
      case "rowid":
      case "xml":
        return SqlTypes.String;
      case "blob":
      case "binary":
      case "varbinary":
        return SqlTypes.Bytes;
      default:
        throw BitSailException.asBitSailException(DBUtilErrorCode.UNSUPPORTED_TYPE,
            String.format("DB2: The column data type in your configuration is not support. Column type:[%s]." +
                " Please try to change the column data type or don't transmit this column.", type)
        );
    }
  }

  public static SqlTypes getSqlTypeFromMysql(final String type) throws BitSailException {
    switch (type) {
      case "bit":
        return SqlTypes.Boolean;
      case "tinyint":
      case "tinyint unsigned":
        return SqlTypes.Short;
      case "smallint":
      case "smallint unsigned":
      case "mediumint":
      case "mediumint unsigned":
      case "int":
      case "int unsigned":
        return SqlTypes.Long;
      case "bigint":
      case "bigint unsigned":
        return SqlTypes.BigInt;
      case "datetime":
      case "timestamp":
        return SqlTypes.Timestamp;
      case "time":
        return SqlTypes.Time;
      case "date":
        return SqlTypes.Date;
      case "year":
        return SqlTypes.Year;
      case "float":
        return SqlTypes.Float;
      case "double":
      case "real":
        return SqlTypes.Double;
      case "decimal":
        return SqlTypes.BigDecimal;
      case "enum":
      case "char":
      case "nvar":
      case "varchar":
      case "nvarchar":
      case "longnvarchar":
      case "longtext":
      case "text":
      case "mediumtext":
      case "string":
      case "longvarchar":
      case "tinytext":
      case "json":
        return SqlTypes.String;
      case "tinyblob":
      case "blob":
      case "mediumblob":
      case "longblob":
      case "binary":
      case "longvarbinary":
      case "varbinary":
        return SqlTypes.Bytes;
      default:
        throw BitSailException.asBitSailException(DBUtilErrorCode.UNSUPPORTED_TYPE,
            String.format("MySQL: The column data type in your configuration is not support. Column type:[%s]." +
                " Please try to change the column data type or don't transmit this column.", type)
        );
    }
  }

  public static SqlTypes getSqlTypeFromPostgresql(final String type) throws BitSailException {
    switch (type) {
      case "boolean":
        return SqlTypes.Boolean;
      case "smallserial":
      case "smallint":
      case "int2":
        return SqlTypes.Short;
      case "integer":
      case "int4":
      case "int":
      case "serial":
        return SqlTypes.Int;
      case "int8":
      case "bigint":
      case "bigserial":
        return SqlTypes.Long;
      case "timestamp":
        return SqlTypes.Timestamp;
      case "time":
        return SqlTypes.Time;
      case "date":
        return SqlTypes.Date;
      case "real":
      case "float":
      case "float4":
        return SqlTypes.Float;
      case "double":
      case "float8":
      case "double precision":
        return SqlTypes.Double;
      case "money":
        return SqlTypes.Money;
      case "decimal":
      case "numeric":
        return SqlTypes.BigDecimal;
      case "char":
      case "varchar":
      case "text":
      case "character varying":
      case "string":
      case "character":
        return SqlTypes.String;
      case "bit":
      case "bit varying":
      case "varbit":
      case "uuid":
      case "cidr":
      case "xml":
      case "macaddr":
      case "json":
      case "enum":
        return SqlTypes.OtherTypeString;
      case "bytea":
        return SqlTypes.Bytes;
      default:
        throw BitSailException.asBitSailException(DBUtilErrorCode.UNSUPPORTED_TYPE,
            String.format("PostgreSQL: The column data type in your configuration is not support. Column type:[%s]." +
                " Please try to change the column data type or don't transmit this column.", type)
        );
    }
  }

  public static void setSqlValue(PreparedStatement upload, int index, final String type, Object value, final String driveName) throws BitSailException {
    SqlTypes sqlType = getSqlType(type, driveName);
    try {
      switch (sqlType) {
        case Boolean:
          Boolean b = (value instanceof String) ? Boolean.valueOf((String) value) : (Boolean) value;
          upload.setBoolean(index, b);
          break;
        case Short:
          Short s = (value instanceof String) ? Short.valueOf((String) value) : (Short) value;
          upload.setShort(index, s);
          break;
        case Int:
          Integer i = (value instanceof String) ? Integer.valueOf((String) value) : (Integer) value;
          upload.setInt(index, i);
          break;
        case Long:
          Long l = (value instanceof String) ? Long.valueOf((String) value) : (Long) value;
          upload.setLong(index, l);
          break;
        case Timestamp:
          upload.setTimestamp(index, new Timestamp(((java.util.Date) value).getTime()));
          break;
        case Time:
          upload.setTime(index, new Time(((java.util.Date) value).getTime()));
          break;
        case Date:
          upload.setDate(index, new Date(((java.util.Date) value).getTime()));
          break;
        case Year:
          Integer y = (value instanceof String) ? Short.valueOf((String) value) : ((java.util.Date) value).getYear();
          upload.setLong(index, y);
          break;
        case Float:
          if (value instanceof Float) {
            Float f = (Float) value;
            upload.setFloat(index, f.floatValue());
          } else {
            Double d = (value instanceof String) ? Double.valueOf((String) value) : (Double) value;
            upload.setFloat(index, d.floatValue());
          }
          break;
        case Double:
          Double d = (value instanceof String) ? Double.valueOf((String) value) : (Double) value;
          upload.setDouble(index, d);
          break;
        case Money:
          Double m = (value instanceof String) ? Double.valueOf((String) value) : (Double) value;
          upload.setObject(index, m, Types.OTHER);
          break;
        case String:
          upload.setString(index, (String) value);
          break;
        case OtherTypeString:
          upload.setObject(index, value, Types.OTHER);
          break;
        case Bytes:
          upload.setBytes(index, (byte[]) value);
          break;
        case BigInt:
          String setValue;
          if (value instanceof String) {
            setValue = new BigInteger((String) value).toString();
          }
          if (value instanceof BigInteger) {
            setValue = ((BigInteger) value).toString();
          } else {
            setValue = value.toString();
          }
          upload.setString(index, setValue);
          break;
        case BigDecimal:
          BigDecimal bigDecimal = (value instanceof String) ? new BigDecimal((String) value) : (BigDecimal) value;
          upload.setBigDecimal(index, bigDecimal);
          break;
        default:
          throw BitSailException.asBitSailException(DBUtilErrorCode.UNSUPPORTED_TYPE,
              String.format("Set SQL Value: The column data type in your configuration is not support. Column index:[%d], Column type:[%s]." +
                  " Please try to change the column data type or don't transmit this column.", index, type)
          );
      }
    } catch (Exception e) {
      throw BitSailException.asBitSailException(CommonErrorCode.RUNTIME_ERROR, String.format("value is not support, " +
          "value: [%s], type: [%s]", value, type), e);
    }
  }

  public enum SqlTypes {
    Boolean,
    Short,
    Int,
    Long,
    Timestamp,
    Time,
    Date,
    Year,
    Float,
    Double,
    Money,
    OtherTypeString,
    String,
    Bytes,
    BigInt,
    BigDecimal,
    Array
  }
}
