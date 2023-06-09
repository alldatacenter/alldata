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
#include "drill/common.hpp"
#include "drill/fieldmeta.hpp"
#include "../protobuf/UserBitShared.pb.h"
#include "../protobuf/User.pb.h"

namespace {
// List of SQL types as string constants
static std::string SQLAny("ANY");
static std::string SQLArray("ARRAY");
static std::string SQLBigint("BIGINT");
static std::string SQLBinary("BINARY");
static std::string SQLBoolean("BOOLEAN");
static std::string SQLChar("CHARACTER");
static std::string SQLDate("DATE");
static std::string SQLDecimal("DECIMAL");
static std::string SQLDouble("DOUBLE");
static std::string SQLFloat("FLOAT");
static std::string SQLInteger("INTEGER");
static std::string SQLInterval("INTERVAL");
static std::string SQLIntervalYearMonth("INTERVAL YEAR TO MONTH");
static std::string SQLIntervalDaySecond("INTERVAL DAY TO SECOND");
static std::string SQLNChar("NATIONAL CHARACTER");
static std::string SQLNull("NULL");
static std::string SQLMap("MAP");
static std::string SQLSmallint("SMALLINT");
static std::string SQLTime("TIME");
static std::string SQLTimestamp("TIMESTAMP");
static std::string SQLTimestampTZ("TIMESTAMP WITH TIME ZONE");
static std::string SQLTimeTZ("TIME WITH TIME ZONE");
static std::string SQLTinyint("TINYINT");
static std::string SQLUnion("UNION");
static std::string SQLVarbinary("BINARY VARYING");
static std::string SQLVarchar("CHARACTER VARYING");
static std::string SQLVarnchar("NATIONAL CHARACTER VARYING");
static std::string SQLUnknown("__unknown__");

static const std::string& getSQLType(common::MinorType type, common::DataMode mode) {
  if (mode == common::DM_REPEATED || type == common::LIST) {
    return SQLArray;
  }

  switch(type) {
    case common::BIT:             return SQLBoolean;

    case common::TINYINT:         return SQLTinyint;
    case common::SMALLINT:        return SQLSmallint;
    case common::INT:             return SQLInteger;
    case common::BIGINT:          return SQLBigint;
    case common::FLOAT4:          return SQLFloat;
    case common::FLOAT8:          return SQLDouble;

    case common::DECIMAL9:
    case common::DECIMAL18:
    case common::DECIMAL28DENSE:
    case common::DECIMAL28SPARSE:
    case common::DECIMAL38DENSE:
    case common::VARDECIMAL:
    case common::DECIMAL38SPARSE: return SQLDecimal;

    case common::VARCHAR:         return SQLVarchar;
    case common::FIXEDCHAR:       return SQLChar;

    case common::VAR16CHAR:       return SQLVarnchar;
    case common::FIXED16CHAR:     return SQLNChar;

    case common::VARBINARY:       return SQLVarbinary;
    case common::FIXEDBINARY:     return SQLBinary;

    case common::DATE:            return SQLDate;
    case common::TIME:            return SQLTime;
    case common::TIMETZ:          return SQLTimeTZ;
    case common::TIMESTAMP:       return SQLTimestamp;
    case common::TIMESTAMPTZ:     return SQLTimestampTZ;

    case common::INTERVALYEAR:    return SQLIntervalYearMonth;
    case common::INTERVALDAY:     return SQLIntervalDaySecond;
    case common::INTERVAL:        return SQLInterval;
    case common::MONEY:           return SQLDecimal;

    case common::MAP:             return SQLMap;
    case common::LATE:            return SQLAny;
    case common::DM_UNKNOWN:      return SQLNull;
    case common::UNION:           return SQLUnion;

    case common::UINT1:           return SQLTinyint;
    case common::UINT2:           return SQLSmallint;
    case common::UINT4:           return SQLInteger;
    case common::UINT8:           return SQLBigint;

    default:
      return SQLUnknown;
  }
}

static bool isSortable(common::MinorType type) {
  return type != common::MAP && type != common::LIST;
}

static bool isNullable(common::DataMode mode) {
  return mode == common::DM_OPTIONAL; // Same behaviour as JDBC
}

static bool isSigned(common::MinorType type, common::DataMode mode) {
  if (mode == common::DM_REPEATED) {
    return false;// SQL ARRAY
  }

  switch(type) {
    case common::SMALLINT:
    case common::INT:
    case common::BIGINT:
    case common::FLOAT4:
    case common::FLOAT8:

    case common::DECIMAL9:
    case common::DECIMAL18:
    case common::DECIMAL28DENSE:
    case common::DECIMAL38DENSE:
    case common::DECIMAL38SPARSE:
    case common::VARDECIMAL:

    case common::INTERVALYEAR:
    case common::INTERVALDAY:
    case common::INTERVAL:
    case common::MONEY:
    case common::TINYINT:
      return true;

    case common::BIT:
    case common::VARCHAR:
    case common::FIXEDCHAR:

    case common::VAR16CHAR:
    case common::FIXED16CHAR:

    case common::VARBINARY:
    case common::FIXEDBINARY:

    case common::DATE:
    case common::TIME:
    case common::TIMETZ:
    case common::TIMESTAMP:
    case common::TIMESTAMPTZ:

    case common::MAP:
    case common::LATE:
    case common::DM_UNKNOWN:
    case common::UNION:

    case common::UINT1:
    case common::UINT2:
    case common::UINT4:
    case common::UINT8:
      return false;

    default:
      return false;
  }
}

static Drill::FieldMetadata::ColumnSearchability getSearchability(exec::user::ColumnSearchability s) {
  switch(s) {
    case exec::user::UNKNOWN_SEARCHABILITY: return Drill::FieldMetadata::UNKNOWN_SEARCHABILITY;
    case exec::user::NONE:                  return Drill::FieldMetadata::NONE;
    case exec::user::CHAR:                  return Drill::FieldMetadata::CHAR;
    case exec::user::NUMBER:                return Drill::FieldMetadata::NUMBER;
    case exec::user::ALL:                   return Drill::FieldMetadata::ALL;

    default:
      return Drill::FieldMetadata::UNKNOWN_SEARCHABILITY;
  }
}

static Drill::FieldMetadata::ColumnUpdatability getUpdatability(exec::user::ColumnUpdatability u) {
  switch(u) {
    case exec::user::UNKNOWN_UPDATABILITY: return Drill::FieldMetadata::UNKNOWN_UPDATABILITY;
    case exec::user::READ_ONLY:            return Drill::FieldMetadata::READ_ONLY;
    case exec::user::WRITABLE:             return Drill::FieldMetadata::WRITABLE;

    default:
      return Drill::FieldMetadata::UNKNOWN_UPDATABILITY;
  }
}

// Based on ODBC spec
// https://msdn.microsoft.com/en-us/library/ms711786(v=vs.85).aspx
static uint32_t getColumnSize(const std::string& type, uint32_t precision) {
	if (type == SQLBoolean) {
		return 1;
	}
	else if (type == SQLTinyint) {
		return 3;
	}
	else if (type == SQLSmallint) {
		return 5;
	}
	else if (type == SQLInteger) {
		return 10;
	}
	else if (type == SQLBigint) {
		return 19;
	}
	else if (type == SQLFloat) {
		return 7;
	}
	else if (type == SQLDouble) {
		return 15;
	}
	else if (type == SQLDecimal) {
		return precision;
	}
	else if (type == SQLBinary || type == SQLVarbinary
			|| type == SQLChar || type == SQLVarchar
			|| type == SQLNChar || type == SQLVarnchar) {
		return precision;
	}
	else if (type == SQLDate) {
		return 10; // 'yyyy-MM-dd' format
	}
	else if (type == SQLTime) {
		if (precision > 0) {
			return 9 + precision;
		}
		else return 8; // 'hh-mm-ss' format
	}
	else if (type == SQLTimestamp) {
		return (precision > 0)
			? 20 + precision
			: 19; // 'yyyy-MM-ddThh-mm-ss' format
	}
	else if (type == SQLIntervalYearMonth) {
		return (precision > 0)
				? 5 + precision // P..M31
				: 9; // we assume max is P9999Y12M
	}
	else if (type == SQLIntervalDaySecond) {
		return (precision > 0)
			? 12 + precision // P..DT12H60M60....S
			: 22; // the first 4 bytes give the number of days, so we assume max is P2147483648DT12H60M60S
	}
	else {
		return 0;
	}
}

static uint32_t getPrecision(const ::common::MajorType& type) {
	const ::common::MinorType& minor_type = type.minor_type();

	if (type.has_precision()) {
		return type.precision();
	}

	if (minor_type == ::common::VARBINARY || minor_type == ::common::VARCHAR) {
		return 65535;
	}

	return 0;
}

// From Types.java
// Based on ODBC spec:
// https://msdn.microsoft.com/en-us/library/ms713974(v=vs.85).aspx
static uint32_t getDisplaySize(const ::common::MajorType& type) {
    if (type.mode() == ::common::DM_REPEATED || type.minor_type() == ::common::LIST) {
      return 0;
    }

    uint32_t precision = getPrecision(type);

    switch(type.minor_type()) {
    case ::common::BIT:             return 1; // 1 digit

    case ::common::TINYINT:         return 4; // sign + 3 digit
    case ::common::SMALLINT:        return 6; // sign + 5 digits
    case ::common::INT:             return 11; // sign + 10 digits
    case ::common::BIGINT:          return 20; // sign + 19 digits

    case ::common::UINT1:          return 3; // 3 digits
    case ::common::UINT2:          return 5; // 5 digits
    case ::common::UINT4:          return 10; // 10 digits
    case ::common::UINT8:          return 19; // 19 digits

    case ::common::FLOAT4:          return 14; // sign + 7 digits + decimal point + E + 2 digits
    case ::common::FLOAT8:          return 24; // sign + 15 digits + decimal point + E + 3 digits

    case ::common::DECIMAL9:
    case ::common::DECIMAL18:
    case ::common::DECIMAL28DENSE:
    case ::common::DECIMAL28SPARSE:
    case ::common::DECIMAL38DENSE:
    case ::common::DECIMAL38SPARSE:
    case ::common::VARDECIMAL:
    case ::common::MONEY:           return 2 + precision; // precision of the column plus a sign and a decimal point

    case ::common::VARCHAR:
    case ::common::FIXEDCHAR:
    case ::common::VAR16CHAR:
    case ::common::FIXED16CHAR:     return precision; // number of characters

    case ::common::VARBINARY:
    case ::common::FIXEDBINARY:     return 2 * precision; // each binary byte is represented as a 2digit hex number

    case ::common::DATE:            return 10; // yyyy-mm-dd
    case ::common::TIME:
      return precision > 0
        ? 9 + precision // hh-mm-ss.SSS
        : 8; // hh-mm-ss
    case ::common::TIMETZ:
      return precision > 0
        ? 15 + precision // hh-mm-ss.SSS-zz:zz
        : 14; // hh-mm-ss-zz:zz
    case ::common::TIMESTAMP:
      return precision > 0
         ? 20 + precision // yyyy-mm-ddThh:mm:ss.SSS
         : 19; // yyyy-mm-ddThh:mm:ss
    case ::common::TIMESTAMPTZ:
      return precision > 0
        ? 26 + precision // yyyy-mm-ddThh:mm:ss.SSS:ZZ-ZZ
        : 25; // yyyy-mm-ddThh:mm:ss-ZZ:ZZ

    case ::common::INTERVALYEAR:
      return precision > 0
          ? 5 + precision // P..Y12M
          : 9; // we assume max is P9999Y12M

    case ::common::INTERVALDAY:
      return precision > 0
          ? 12 + precision // P..DT12H60M60S assuming fractional seconds precision is not supported
          : 22; // the first 4 bytes give the number of days, so we assume max is P2147483648DT12H60M60S

    default:
    	// We don't know how to compute a display size, let's return 0 (unknown)
    	return 0;
}
}
} // namespace

namespace Drill{

void FieldMetadata::set(const exec::shared::SerializedField& f){
    m_name=f.name_part().name();
    m_minorType=f.major_type().minor_type();
    m_dataMode=f.major_type().mode();
    m_valueCount=f.value_count();
    m_scale=f.major_type().scale();
    m_precision=f.major_type().precision();
    m_bufferLength=f.buffer_length();
    m_catalogName="DRILL";
    m_schemaName=""; // unknown
    m_tableName=""; // unknown;
    m_label=m_name;
    m_sqlType=::getSQLType(m_minorType, m_dataMode);
    m_nullable=::isNullable(m_dataMode);
    m_signed=::isSigned(m_minorType, m_dataMode);
    m_displaySize=::getDisplaySize(f.major_type());
    m_searchability=ALL;
    m_updatability=READ_ONLY;
    m_autoIncremented=false;
    m_caseSensitive=false;
    m_sortable=::isSortable(m_minorType);
    m_currency=false;
    m_columnSize = ::getColumnSize(m_sqlType, m_precision);
}

void FieldMetadata::set(const exec::user::ResultColumnMetadata& m){
    m_name=m.column_name();
    m_minorType=static_cast<common::MinorType>(-1);
    m_dataMode=static_cast<common::DataMode>(-1);
    m_valueCount=0;
    m_scale=m.scale();
    m_precision=m.precision();
    m_bufferLength=0;
    m_catalogName=m.catalog_name();
    m_schemaName=m.schema_name();
    m_tableName=m.table_name();
    m_label=m.label();
    m_sqlType=m.data_type();
    m_nullable=m.is_nullable();
    m_displaySize=m.display_size();
    m_signed=m.signed_();
    m_searchability=::getSearchability(m.searchability());
    m_updatability=::getUpdatability(m.updatability());
    m_autoIncremented=m.auto_increment();
    m_caseSensitive=m.case_sensitivity();
    m_sortable=m.sortable();
    m_currency=m.is_currency();
    m_columnSize =::getColumnSize(m_sqlType, m_precision);
}

}// namespace Drill

