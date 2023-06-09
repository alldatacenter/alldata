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
#include <boost/assign.hpp>
#include <boost/functional/hash.hpp>
#include <boost/unordered_set.hpp>
#include "drillClientImpl.hpp"

#include "metadata.hpp"

const std::string Drill::meta::DrillMetadata::s_connectorName(DRILL_CONNECTOR_NAME);
const std::string Drill::meta::DrillMetadata::s_connectorVersion(DRILL_VERSION_STRING);

namespace Drill {
namespace meta {
namespace { // Anonymous namespace
using boost::assign::list_of;

// Default values based on Drill 1.8 support
static const std::size_t s_maxIdentifierSize = 1024;
static const std::string s_catalogSeparator(".");
static const std::string s_catalogTerm("catalog");
static const std::string s_identifierQuoteString("`");

static const std::vector<std::string> s_sqlKeywords = boost::assign::list_of
		("ABS")("ALLOW")("ARRAY")("ASENSITIVE")("ASYMMETRIC")("ATOMIC")("BIGINT")("BINARY")("BLOB")
		("BOOLEAN")("CALL")("CALLED")("CARDINALITY")("CEIL")("CEILING")("CLOB")("COLLECT")("CONDITION")
		("CORR")("COVAR_POP")("COVAR_SAMP")("CUBE")("CUME_DIST")("CURRENT_CATALOG")
		("CURRENT_DEFAULT_TRANSFORM_GROUP")("CURRENT_PATH")("CURRENT_ROLE")("CURRENT_SCHEMA")
		("CURRENT_TRANSFORM_GROUP_FOR_TYPE")("CYCLE")("DATABASE")("DATABASES")("DENSE_RANK")("DEREF")
		("DETERMINISTIC")("DISALLOW")("DYNAMIC")("EACH")("ELEMENT")("EVERY")("EXP")("EXPLAIN")
		("EXTEND")("FILES")("FILTER")("FIRST_VALUE")("FLOOR")("FREE")("FUNCTION")("FUSION")("GROUPING")
		("HOLD")("IF")("IMPORT")("INOUT")("INTERSECTION")("LARGE")("LAST_VALUE")("LATERAL")("LIMIT")("LN")
		("LOCALTIME")("LOCALTIMESTAMP")("MEMBER")("MERGE")("METADATA")("METHOD")("MOD")("MODIFIES")
		("MULTISET")("NCLOB")("NEW")("NONE")("NORMALIZE")("OFFSET")("OLD")("OUT")("OVER")("OVERLAY")
		("PARAMETER")("PARTITION")("PERCENTILE_CONT")("PERCENTILE_DISC")("PERCENT_RANK")("POWER")
		("RANGE")("RANK")("READS")("RECURSIVE")("REF")("REFERENCING")("REFRESH")("REGR_AVGX")("REGR_AVGY")
		("REGR_COUNT")("REGR_INTERCEPT")("REGR_R2")("REGR_SLOPE")("REGR_SXX")("REGR_SXY")("REGR_SYY")
		("RELEASE")("REPLACE")("RESET")("RESULT")("RETURN")("RETURNS")("ROLLUP")("ROW")("ROW_NUMBER")
		("SAVEPOINT")("SCHEMAS")("SCOPE")("SEARCH")("SENSITIVE")("SHOW")("SIMILAR")("SPECIFIC")("SPECIFICTYPE")
		("SQLEXCEPTION")("SQLWARNING")("SQRT")("START")("STATIC")("STDDEV_POP")("STDDEV_SAMP")("STREAM")
		("SUBMULTISET")("SYMMETRIC")("SYSTEM")("TABLES")("TABLESAMPLE")("TINYINT")("TREAT")("TRIGGER")
		("UESCAPE")("UNNEST")("UPSERT")("USE")("VARBINARY")("VAR_POP")("VAR_SAMP")("WIDTH_BUCKET")
		("WINDOW")("WITHIN")("WITHOUT");

static const std::vector<std::string> s_numericFunctions = boost::assign::list_of
		("ABS")("EXP")("LOG")("LOG10")("MOD")("POWER");

static const std::string s_schemaTerm("schema");
static const std::string s_searchEscapeString("\\");
static const std::string s_specialCharacters;

static const std::vector<std::string> s_stringFunctions = boost::assign::list_of
		("CONCAT")("INSERT")("LCASE")("LENGTH")("LOCATE")("LTRIM")("RTRIM")("SUBSTRING")("UCASE");

static const std::vector<std::string> s_systemFunctions;

static const std::string s_tableTerm("table");

static const std::vector<std::string> s_dateTimeFunctions = boost::assign::list_of
		("CURDATE")("CURTIME")("NOW")("QUARTER");

static const std::vector<exec::user::DateTimeLiteralsSupport> s_dateTimeLiterals = boost::assign::list_of
		(exec::user::DL_DATE)(exec::user::DL_TIME)(exec::user::DL_TIMESTAMP)(exec::user::DL_INTERVAL_YEAR)
		(exec::user::DL_INTERVAL_MONTH)(exec::user::DL_INTERVAL_DAY)(exec::user::DL_INTERVAL_HOUR)
		(exec::user::DL_INTERVAL_MINUTE)(exec::user::DL_INTERVAL_SECOND)(exec::user::DL_INTERVAL_YEAR_TO_MONTH)
		(exec::user::DL_INTERVAL_DAY_TO_HOUR)(exec::user::DL_INTERVAL_DAY_TO_MINUTE)
		(exec::user::DL_INTERVAL_DAY_TO_SECOND)(exec::user::DL_INTERVAL_HOUR_TO_MINUTE)
		(exec::user::DL_INTERVAL_HOUR_TO_SECOND)(exec::user::DL_INTERVAL_MINUTE_TO_SECOND);

static const std::vector<exec::user::OrderBySupport> s_orderBySupport = boost::assign::list_of
		(exec::user::OB_UNRELATED)(exec::user::OB_EXPRESSION);

static const std::vector<exec::user::OuterJoinSupport> s_outerJoinSupport = boost::assign::list_of
		(exec::user::OJ_LEFT)(exec::user::OJ_RIGHT)(exec::user::OJ_FULL);

static const std::vector<exec::user::SubQuerySupport> s_subQuerySupport = boost::assign::list_of
		(exec::user::SQ_CORRELATED)(exec::user::SQ_IN_COMPARISON)(exec::user::SQ_IN_EXISTS)
		(exec::user::SQ_IN_QUANTIFIED);

static const std::vector<exec::user::UnionSupport> s_unionSupport = boost::assign::list_of
		(exec::user::U_UNION)(exec::user::U_UNION_ALL);

static exec::user::ConvertSupport ConvertSupport(common::MinorType from, common::MinorType to) {
	exec::user::ConvertSupport convertSupport;
	convertSupport.set_from(from);
	convertSupport.set_to(to);

	return convertSupport;
}

static const convert_support_set s_convertMap = boost::assign::list_of
		(ConvertSupport(common::TINYINT, common::INT))
		(ConvertSupport(common::TINYINT, common::BIGINT))
		(ConvertSupport(common::TINYINT, common::DECIMAL9))
		(ConvertSupport(common::TINYINT, common::DECIMAL18))
		(ConvertSupport(common::TINYINT, common::DECIMAL28SPARSE))
		(ConvertSupport(common::TINYINT, common::DECIMAL38SPARSE))
		(ConvertSupport(common::TINYINT, common::VARDECIMAL))
		(ConvertSupport(common::TINYINT, common::DATE))
		(ConvertSupport(common::TINYINT, common::TIME))
		(ConvertSupport(common::TINYINT, common::TIMESTAMP))
		(ConvertSupport(common::TINYINT, common::INTERVAL))
		(ConvertSupport(common::TINYINT, common::FLOAT4))
		(ConvertSupport(common::TINYINT, common::FLOAT8))
		(ConvertSupport(common::TINYINT, common::BIT))
		(ConvertSupport(common::TINYINT, common::VARCHAR))
		(ConvertSupport(common::TINYINT, common::VAR16CHAR))
		(ConvertSupport(common::TINYINT, common::VARBINARY))
		(ConvertSupport(common::TINYINT, common::INTERVALYEAR))
		(ConvertSupport(common::TINYINT, common::INTERVALDAY))
		(ConvertSupport(common::SMALLINT, common::INT))
		(ConvertSupport(common::SMALLINT, common::BIGINT))
		(ConvertSupport(common::SMALLINT, common::DECIMAL9))
		(ConvertSupport(common::SMALLINT, common::DECIMAL18))
		(ConvertSupport(common::SMALLINT, common::DECIMAL28SPARSE))
		(ConvertSupport(common::SMALLINT, common::DECIMAL38SPARSE))
		(ConvertSupport(common::SMALLINT, common::VARDECIMAL))
		(ConvertSupport(common::SMALLINT, common::DATE))
		(ConvertSupport(common::SMALLINT, common::TIME))
		(ConvertSupport(common::SMALLINT, common::TIMESTAMP))
		(ConvertSupport(common::SMALLINT, common::INTERVAL))
		(ConvertSupport(common::SMALLINT, common::FLOAT4))
		(ConvertSupport(common::SMALLINT, common::FLOAT8))
		(ConvertSupport(common::SMALLINT, common::BIT))
		(ConvertSupport(common::SMALLINT, common::VARCHAR))
		(ConvertSupport(common::SMALLINT, common::VAR16CHAR))
		(ConvertSupport(common::SMALLINT, common::VARBINARY))
		(ConvertSupport(common::SMALLINT, common::INTERVALYEAR))
		(ConvertSupport(common::SMALLINT, common::INTERVALDAY))
		(ConvertSupport(common::INT, common::INT))
		(ConvertSupport(common::INT, common::BIGINT))
		(ConvertSupport(common::INT, common::DECIMAL9))
		(ConvertSupport(common::INT, common::DECIMAL18))
		(ConvertSupport(common::INT, common::DECIMAL28SPARSE))
		(ConvertSupport(common::INT, common::DECIMAL38SPARSE))
		(ConvertSupport(common::INT, common::VARDECIMAL))
		(ConvertSupport(common::INT, common::DATE))
		(ConvertSupport(common::INT, common::TIME))
		(ConvertSupport(common::INT, common::TIMESTAMP))
		(ConvertSupport(common::INT, common::INTERVAL))
		(ConvertSupport(common::INT, common::FLOAT4))
		(ConvertSupport(common::INT, common::FLOAT8))
		(ConvertSupport(common::INT, common::BIT))
		(ConvertSupport(common::INT, common::VARCHAR))
		(ConvertSupport(common::INT, common::VAR16CHAR))
		(ConvertSupport(common::INT, common::VARBINARY))
		(ConvertSupport(common::INT, common::INTERVALYEAR))
		(ConvertSupport(common::INT, common::INTERVALDAY))
		(ConvertSupport(common::BIGINT, common::INT))
		(ConvertSupport(common::BIGINT, common::BIGINT))
		(ConvertSupport(common::BIGINT, common::DECIMAL9))
		(ConvertSupport(common::BIGINT, common::DECIMAL18))
		(ConvertSupport(common::BIGINT, common::DECIMAL28SPARSE))
		(ConvertSupport(common::BIGINT, common::DECIMAL38SPARSE))
		(ConvertSupport(common::BIGINT, common::VARDECIMAL))
		(ConvertSupport(common::BIGINT, common::DATE))
		(ConvertSupport(common::BIGINT, common::TIME))
		(ConvertSupport(common::BIGINT, common::TIMESTAMP))
		(ConvertSupport(common::BIGINT, common::INTERVAL))
		(ConvertSupport(common::BIGINT, common::FLOAT4))
		(ConvertSupport(common::BIGINT, common::FLOAT8))
		(ConvertSupport(common::BIGINT, common::BIT))
		(ConvertSupport(common::BIGINT, common::VARCHAR))
		(ConvertSupport(common::BIGINT, common::VAR16CHAR))
		(ConvertSupport(common::BIGINT, common::VARBINARY))
		(ConvertSupport(common::BIGINT, common::INTERVALYEAR))
		(ConvertSupport(common::BIGINT, common::INTERVALDAY))
		(ConvertSupport(common::DECIMAL9, common::INT))
		(ConvertSupport(common::DECIMAL9, common::BIGINT))
		(ConvertSupport(common::DECIMAL9, common::DECIMAL9))
		(ConvertSupport(common::DECIMAL9, common::DECIMAL18))
		(ConvertSupport(common::DECIMAL9, common::DECIMAL28SPARSE))
		(ConvertSupport(common::DECIMAL9, common::DECIMAL38SPARSE))
		(ConvertSupport(common::DECIMAL9, common::VARDECIMAL))
		(ConvertSupport(common::DECIMAL9, common::DATE))
		(ConvertSupport(common::DECIMAL9, common::TIME))
		(ConvertSupport(common::DECIMAL9, common::TIMESTAMP))
		(ConvertSupport(common::DECIMAL9, common::INTERVAL))
		(ConvertSupport(common::DECIMAL9, common::FLOAT4))
		(ConvertSupport(common::DECIMAL9, common::FLOAT8))
		(ConvertSupport(common::DECIMAL9, common::BIT))
		(ConvertSupport(common::DECIMAL9, common::VARCHAR))
		(ConvertSupport(common::DECIMAL9, common::VAR16CHAR))
		(ConvertSupport(common::DECIMAL9, common::VARBINARY))
		(ConvertSupport(common::DECIMAL9, common::INTERVALYEAR))
		(ConvertSupport(common::DECIMAL9, common::INTERVALDAY))
		(ConvertSupport(common::DECIMAL18, common::INT))
		(ConvertSupport(common::DECIMAL18, common::BIGINT))
		(ConvertSupport(common::DECIMAL18, common::DECIMAL9))
		(ConvertSupport(common::DECIMAL18, common::DECIMAL18))
		(ConvertSupport(common::DECIMAL18, common::DECIMAL28SPARSE))
		(ConvertSupport(common::DECIMAL18, common::DECIMAL38SPARSE))
		(ConvertSupport(common::DECIMAL18, common::VARDECIMAL))
		(ConvertSupport(common::DECIMAL18, common::DATE))
		(ConvertSupport(common::DECIMAL18, common::TIME))
		(ConvertSupport(common::DECIMAL18, common::TIMESTAMP))
		(ConvertSupport(common::DECIMAL18, common::INTERVAL))
		(ConvertSupport(common::DECIMAL18, common::FLOAT4))
		(ConvertSupport(common::DECIMAL18, common::FLOAT8))
		(ConvertSupport(common::DECIMAL18, common::BIT))
		(ConvertSupport(common::DECIMAL18, common::VARCHAR))
		(ConvertSupport(common::DECIMAL18, common::VAR16CHAR))
		(ConvertSupport(common::DECIMAL18, common::VARBINARY))
		(ConvertSupport(common::DECIMAL18, common::INTERVALYEAR))
		(ConvertSupport(common::DECIMAL18, common::INTERVALDAY))
		(ConvertSupport(common::DECIMAL28SPARSE, common::INT))
		(ConvertSupport(common::DECIMAL28SPARSE, common::BIGINT))
		(ConvertSupport(common::DECIMAL28SPARSE, common::DECIMAL9))
		(ConvertSupport(common::DECIMAL28SPARSE, common::DECIMAL18))
		(ConvertSupport(common::DECIMAL28SPARSE, common::DECIMAL28SPARSE))
		(ConvertSupport(common::DECIMAL28SPARSE, common::DECIMAL38SPARSE))
		(ConvertSupport(common::DECIMAL28SPARSE, common::VARDECIMAL))
		(ConvertSupport(common::DECIMAL28SPARSE, common::DATE))
		(ConvertSupport(common::DECIMAL28SPARSE, common::TIME))
		(ConvertSupport(common::DECIMAL28SPARSE, common::TIMESTAMP))
		(ConvertSupport(common::DECIMAL28SPARSE, common::INTERVAL))
		(ConvertSupport(common::DECIMAL28SPARSE, common::FLOAT4))
		(ConvertSupport(common::DECIMAL28SPARSE, common::FLOAT8))
		(ConvertSupport(common::DECIMAL28SPARSE, common::BIT))
		(ConvertSupport(common::DECIMAL28SPARSE, common::VARCHAR))
		(ConvertSupport(common::DECIMAL28SPARSE, common::VAR16CHAR))
		(ConvertSupport(common::DECIMAL28SPARSE, common::VARBINARY))
		(ConvertSupport(common::DECIMAL28SPARSE, common::INTERVALYEAR))
		(ConvertSupport(common::DECIMAL28SPARSE, common::INTERVALDAY))
		(ConvertSupport(common::DECIMAL38SPARSE, common::INT))
		(ConvertSupport(common::DECIMAL38SPARSE, common::BIGINT))
		(ConvertSupport(common::DECIMAL38SPARSE, common::DECIMAL9))
		(ConvertSupport(common::DECIMAL38SPARSE, common::DECIMAL18))
		(ConvertSupport(common::DECIMAL38SPARSE, common::DECIMAL28SPARSE))
		(ConvertSupport(common::DECIMAL38SPARSE, common::DECIMAL38SPARSE))
		(ConvertSupport(common::DECIMAL38SPARSE, common::VARDECIMAL))
		(ConvertSupport(common::DECIMAL38SPARSE, common::DATE))
		(ConvertSupport(common::DECIMAL38SPARSE, common::TIME))
		(ConvertSupport(common::DECIMAL38SPARSE, common::TIMESTAMP))
		(ConvertSupport(common::DECIMAL38SPARSE, common::INTERVAL))
		(ConvertSupport(common::DECIMAL38SPARSE, common::FLOAT4))
		(ConvertSupport(common::DECIMAL38SPARSE, common::FLOAT8))
		(ConvertSupport(common::DECIMAL38SPARSE, common::BIT))
		(ConvertSupport(common::DECIMAL38SPARSE, common::VARCHAR))
		(ConvertSupport(common::DECIMAL38SPARSE, common::VAR16CHAR))
		(ConvertSupport(common::DECIMAL38SPARSE, common::VARBINARY))
		(ConvertSupport(common::DECIMAL38SPARSE, common::INTERVALYEAR))
		(ConvertSupport(common::DECIMAL38SPARSE, common::INTERVALDAY))
		(ConvertSupport(common::VARDECIMAL, common::INT))
		(ConvertSupport(common::VARDECIMAL, common::BIGINT))
		(ConvertSupport(common::VARDECIMAL, common::DECIMAL9))
		(ConvertSupport(common::VARDECIMAL, common::DECIMAL18))
		(ConvertSupport(common::VARDECIMAL, common::DECIMAL28SPARSE))
		(ConvertSupport(common::VARDECIMAL, common::DECIMAL38SPARSE))
		(ConvertSupport(common::VARDECIMAL, common::VARDECIMAL))
		(ConvertSupport(common::VARDECIMAL, common::DATE))
		(ConvertSupport(common::VARDECIMAL, common::TIME))
		(ConvertSupport(common::VARDECIMAL, common::TIMESTAMP))
		(ConvertSupport(common::VARDECIMAL, common::INTERVAL))
		(ConvertSupport(common::VARDECIMAL, common::FLOAT4))
		(ConvertSupport(common::VARDECIMAL, common::FLOAT8))
		(ConvertSupport(common::VARDECIMAL, common::BIT))
		(ConvertSupport(common::VARDECIMAL, common::VARCHAR))
		(ConvertSupport(common::VARDECIMAL, common::VAR16CHAR))
		(ConvertSupport(common::VARDECIMAL, common::VARBINARY))
		(ConvertSupport(common::VARDECIMAL, common::INTERVALYEAR))
		(ConvertSupport(common::VARDECIMAL, common::INTERVALDAY))
		(ConvertSupport(common::MONEY, common::INT))
		(ConvertSupport(common::MONEY, common::BIGINT))
		(ConvertSupport(common::MONEY, common::DECIMAL9))
		(ConvertSupport(common::MONEY, common::DECIMAL18))
		(ConvertSupport(common::MONEY, common::DECIMAL28SPARSE))
		(ConvertSupport(common::MONEY, common::DECIMAL38SPARSE))
		(ConvertSupport(common::MONEY, common::VARDECIMAL))
		(ConvertSupport(common::MONEY, common::DATE))
		(ConvertSupport(common::MONEY, common::TIME))
		(ConvertSupport(common::MONEY, common::TIMESTAMP))
		(ConvertSupport(common::MONEY, common::INTERVAL))
		(ConvertSupport(common::MONEY, common::FLOAT4))
		(ConvertSupport(common::MONEY, common::FLOAT8))
		(ConvertSupport(common::MONEY, common::BIT))
		(ConvertSupport(common::MONEY, common::VARCHAR))
		(ConvertSupport(common::MONEY, common::VAR16CHAR))
		(ConvertSupport(common::MONEY, common::VARBINARY))
		(ConvertSupport(common::MONEY, common::INTERVALYEAR))
		(ConvertSupport(common::MONEY, common::INTERVALDAY))
		(ConvertSupport(common::DATE, common::INT))
		(ConvertSupport(common::DATE, common::BIGINT))
		(ConvertSupport(common::DATE, common::DECIMAL9))
		(ConvertSupport(common::DATE, common::DECIMAL18))
		(ConvertSupport(common::DATE, common::DECIMAL28SPARSE))
		(ConvertSupport(common::DATE, common::DECIMAL38SPARSE))
		(ConvertSupport(common::DATE, common::VARDECIMAL))
		(ConvertSupport(common::DATE, common::DATE))
		(ConvertSupport(common::DATE, common::TIME))
		(ConvertSupport(common::DATE, common::TIMESTAMP))
		(ConvertSupport(common::DATE, common::INTERVAL))
		(ConvertSupport(common::DATE, common::FLOAT4))
		(ConvertSupport(common::DATE, common::FLOAT8))
		(ConvertSupport(common::DATE, common::BIT))
		(ConvertSupport(common::DATE, common::VARCHAR))
		(ConvertSupport(common::DATE, common::VAR16CHAR))
		(ConvertSupport(common::DATE, common::VARBINARY))
		(ConvertSupport(common::DATE, common::INTERVALYEAR))
		(ConvertSupport(common::DATE, common::INTERVALDAY))
		(ConvertSupport(common::TIME, common::INT))
		(ConvertSupport(common::TIME, common::BIGINT))
		(ConvertSupport(common::TIME, common::DECIMAL9))
		(ConvertSupport(common::TIME, common::DECIMAL18))
		(ConvertSupport(common::TIME, common::DECIMAL28SPARSE))
		(ConvertSupport(common::TIME, common::DECIMAL38SPARSE))
		(ConvertSupport(common::TIME, common::VARDECIMAL))
		(ConvertSupport(common::TIME, common::DATE))
		(ConvertSupport(common::TIME, common::TIME))
		(ConvertSupport(common::TIME, common::TIMESTAMP))
		(ConvertSupport(common::TIME, common::INTERVAL))
		(ConvertSupport(common::TIME, common::FLOAT4))
		(ConvertSupport(common::TIME, common::FLOAT8))
		(ConvertSupport(common::TIME, common::BIT))
		(ConvertSupport(common::TIME, common::VARCHAR))
		(ConvertSupport(common::TIME, common::VAR16CHAR))
		(ConvertSupport(common::TIME, common::VARBINARY))
		(ConvertSupport(common::TIME, common::INTERVALYEAR))
		(ConvertSupport(common::TIME, common::INTERVALDAY))
		(ConvertSupport(common::TIMESTAMPTZ, common::INT))
		(ConvertSupport(common::TIMESTAMPTZ, common::BIGINT))
		(ConvertSupport(common::TIMESTAMPTZ, common::DECIMAL9))
		(ConvertSupport(common::TIMESTAMPTZ, common::DECIMAL18))
		(ConvertSupport(common::TIMESTAMPTZ, common::DECIMAL28SPARSE))
		(ConvertSupport(common::TIMESTAMPTZ, common::DECIMAL38SPARSE))
		(ConvertSupport(common::TIMESTAMPTZ, common::VARDECIMAL))
		(ConvertSupport(common::TIMESTAMPTZ, common::DATE))
		(ConvertSupport(common::TIMESTAMPTZ, common::TIME))
		(ConvertSupport(common::TIMESTAMPTZ, common::TIMESTAMP))
		(ConvertSupport(common::TIMESTAMPTZ, common::INTERVAL))
		(ConvertSupport(common::TIMESTAMPTZ, common::FLOAT4))
		(ConvertSupport(common::TIMESTAMPTZ, common::FLOAT8))
		(ConvertSupport(common::TIMESTAMPTZ, common::BIT))
		(ConvertSupport(common::TIMESTAMPTZ, common::VARCHAR))
		(ConvertSupport(common::TIMESTAMPTZ, common::VAR16CHAR))
		(ConvertSupport(common::TIMESTAMPTZ, common::VARBINARY))
		(ConvertSupport(common::TIMESTAMPTZ, common::INTERVALYEAR))
		(ConvertSupport(common::TIMESTAMPTZ, common::INTERVALDAY))
		(ConvertSupport(common::TIMESTAMP, common::INT))
		(ConvertSupport(common::TIMESTAMP, common::BIGINT))
		(ConvertSupport(common::TIMESTAMP, common::DECIMAL9))
		(ConvertSupport(common::TIMESTAMP, common::DECIMAL18))
		(ConvertSupport(common::TIMESTAMP, common::DECIMAL28SPARSE))
		(ConvertSupport(common::TIMESTAMP, common::DECIMAL38SPARSE))
		(ConvertSupport(common::TIMESTAMP, common::VARDECIMAL))
		(ConvertSupport(common::TIMESTAMP, common::DATE))
		(ConvertSupport(common::TIMESTAMP, common::TIME))
		(ConvertSupport(common::TIMESTAMP, common::TIMESTAMP))
		(ConvertSupport(common::TIMESTAMP, common::INTERVAL))
		(ConvertSupport(common::TIMESTAMP, common::FLOAT4))
		(ConvertSupport(common::TIMESTAMP, common::FLOAT8))
		(ConvertSupport(common::TIMESTAMP, common::BIT))
		(ConvertSupport(common::TIMESTAMP, common::VARCHAR))
		(ConvertSupport(common::TIMESTAMP, common::VAR16CHAR))
		(ConvertSupport(common::TIMESTAMP, common::VARBINARY))
		(ConvertSupport(common::TIMESTAMP, common::INTERVALYEAR))
		(ConvertSupport(common::TIMESTAMP, common::INTERVALDAY))
		(ConvertSupport(common::INTERVAL, common::INT))
		(ConvertSupport(common::INTERVAL, common::BIGINT))
		(ConvertSupport(common::INTERVAL, common::DECIMAL9))
		(ConvertSupport(common::INTERVAL, common::DECIMAL18))
		(ConvertSupport(common::INTERVAL, common::DECIMAL28SPARSE))
		(ConvertSupport(common::INTERVAL, common::DECIMAL38SPARSE))
		(ConvertSupport(common::INTERVAL, common::VARDECIMAL))
		(ConvertSupport(common::INTERVAL, common::DATE))
		(ConvertSupport(common::INTERVAL, common::TIME))
		(ConvertSupport(common::INTERVAL, common::TIMESTAMP))
		(ConvertSupport(common::INTERVAL, common::INTERVAL))
		(ConvertSupport(common::INTERVAL, common::FLOAT4))
		(ConvertSupport(common::INTERVAL, common::FLOAT8))
		(ConvertSupport(common::INTERVAL, common::BIT))
		(ConvertSupport(common::INTERVAL, common::VARCHAR))
		(ConvertSupport(common::INTERVAL, common::VAR16CHAR))
		(ConvertSupport(common::INTERVAL, common::VARBINARY))
		(ConvertSupport(common::INTERVAL, common::INTERVALYEAR))
		(ConvertSupport(common::INTERVAL, common::INTERVALDAY))
		(ConvertSupport(common::FLOAT4, common::INT))
		(ConvertSupport(common::FLOAT4, common::BIGINT))
		(ConvertSupport(common::FLOAT4, common::DECIMAL9))
		(ConvertSupport(common::FLOAT4, common::DECIMAL18))
		(ConvertSupport(common::FLOAT4, common::DECIMAL28SPARSE))
		(ConvertSupport(common::FLOAT4, common::DECIMAL38SPARSE))
		(ConvertSupport(common::FLOAT4, common::VARDECIMAL))
		(ConvertSupport(common::FLOAT4, common::DATE))
		(ConvertSupport(common::FLOAT4, common::TIME))
		(ConvertSupport(common::FLOAT4, common::TIMESTAMP))
		(ConvertSupport(common::FLOAT4, common::INTERVAL))
		(ConvertSupport(common::FLOAT4, common::FLOAT4))
		(ConvertSupport(common::FLOAT4, common::FLOAT8))
		(ConvertSupport(common::FLOAT4, common::BIT))
		(ConvertSupport(common::FLOAT4, common::VARCHAR))
		(ConvertSupport(common::FLOAT4, common::VAR16CHAR))
		(ConvertSupport(common::FLOAT4, common::VARBINARY))
		(ConvertSupport(common::FLOAT4, common::INTERVALYEAR))
		(ConvertSupport(common::FLOAT4, common::INTERVALDAY))
		(ConvertSupport(common::FLOAT8, common::INT))
		(ConvertSupport(common::FLOAT8, common::BIGINT))
		(ConvertSupport(common::FLOAT8, common::DECIMAL9))
		(ConvertSupport(common::FLOAT8, common::DECIMAL18))
		(ConvertSupport(common::FLOAT8, common::DECIMAL28SPARSE))
		(ConvertSupport(common::FLOAT8, common::DECIMAL38SPARSE))
		(ConvertSupport(common::FLOAT8, common::VARDECIMAL))
		(ConvertSupport(common::FLOAT8, common::DATE))
		(ConvertSupport(common::FLOAT8, common::TIME))
		(ConvertSupport(common::FLOAT8, common::TIMESTAMP))
		(ConvertSupport(common::FLOAT8, common::INTERVAL))
		(ConvertSupport(common::FLOAT8, common::FLOAT4))
		(ConvertSupport(common::FLOAT8, common::FLOAT8))
		(ConvertSupport(common::FLOAT8, common::BIT))
		(ConvertSupport(common::FLOAT8, common::VARCHAR))
		(ConvertSupport(common::FLOAT8, common::VAR16CHAR))
		(ConvertSupport(common::FLOAT8, common::VARBINARY))
		(ConvertSupport(common::FLOAT8, common::INTERVALYEAR))
		(ConvertSupport(common::FLOAT8, common::INTERVALDAY))
		(ConvertSupport(common::BIT, common::TINYINT))
		(ConvertSupport(common::BIT, common::INT))
		(ConvertSupport(common::BIT, common::BIGINT))
		(ConvertSupport(common::BIT, common::DECIMAL9))
		(ConvertSupport(common::BIT, common::DECIMAL18))
		(ConvertSupport(common::BIT, common::DECIMAL28SPARSE))
		(ConvertSupport(common::BIT, common::DECIMAL38SPARSE))
		(ConvertSupport(common::BIT, common::VARDECIMAL))
		(ConvertSupport(common::BIT, common::DATE))
		(ConvertSupport(common::BIT, common::TIME))
		(ConvertSupport(common::BIT, common::TIMESTAMP))
		(ConvertSupport(common::BIT, common::INTERVAL))
		(ConvertSupport(common::BIT, common::FLOAT4))
		(ConvertSupport(common::BIT, common::FLOAT8))
		(ConvertSupport(common::BIT, common::BIT))
		(ConvertSupport(common::BIT, common::VARCHAR))
		(ConvertSupport(common::BIT, common::VAR16CHAR))
		(ConvertSupport(common::BIT, common::VARBINARY))
		(ConvertSupport(common::BIT, common::INTERVALYEAR))
		(ConvertSupport(common::BIT, common::INTERVALDAY))
		(ConvertSupport(common::FIXEDCHAR, common::TINYINT))
		(ConvertSupport(common::FIXEDCHAR, common::INT))
		(ConvertSupport(common::FIXEDCHAR, common::BIGINT))
		(ConvertSupport(common::FIXEDCHAR, common::DECIMAL9))
		(ConvertSupport(common::FIXEDCHAR, common::DECIMAL18))
		(ConvertSupport(common::FIXEDCHAR, common::DECIMAL28SPARSE))
		(ConvertSupport(common::FIXEDCHAR, common::DECIMAL38SPARSE))
		(ConvertSupport(common::FIXEDCHAR, common::VARDECIMAL))
		(ConvertSupport(common::FIXEDCHAR, common::DATE))
		(ConvertSupport(common::FIXEDCHAR, common::TIME))
		(ConvertSupport(common::FIXEDCHAR, common::TIMESTAMP))
		(ConvertSupport(common::FIXEDCHAR, common::INTERVAL))
		(ConvertSupport(common::FIXEDCHAR, common::FLOAT4))
		(ConvertSupport(common::FIXEDCHAR, common::FLOAT8))
		(ConvertSupport(common::FIXEDCHAR, common::BIT))
		(ConvertSupport(common::FIXEDCHAR, common::VARCHAR))
		(ConvertSupport(common::FIXEDCHAR, common::VAR16CHAR))
		(ConvertSupport(common::FIXEDCHAR, common::VARBINARY))
		(ConvertSupport(common::FIXEDCHAR, common::INTERVALYEAR))
		(ConvertSupport(common::FIXEDCHAR, common::INTERVALDAY))
		(ConvertSupport(common::FIXED16CHAR, common::TINYINT))
		(ConvertSupport(common::FIXED16CHAR, common::INT))
		(ConvertSupport(common::FIXED16CHAR, common::BIGINT))
		(ConvertSupport(common::FIXED16CHAR, common::DECIMAL9))
		(ConvertSupport(common::FIXED16CHAR, common::DECIMAL18))
		(ConvertSupport(common::FIXED16CHAR, common::DECIMAL28SPARSE))
		(ConvertSupport(common::FIXED16CHAR, common::DECIMAL38SPARSE))
		(ConvertSupport(common::FIXED16CHAR, common::VARDECIMAL))
		(ConvertSupport(common::FIXED16CHAR, common::DATE))
		(ConvertSupport(common::FIXED16CHAR, common::TIME))
		(ConvertSupport(common::FIXED16CHAR, common::TIMESTAMP))
		(ConvertSupport(common::FIXED16CHAR, common::INTERVAL))
		(ConvertSupport(common::FIXED16CHAR, common::FLOAT4))
		(ConvertSupport(common::FIXED16CHAR, common::FLOAT8))
		(ConvertSupport(common::FIXED16CHAR, common::BIT))
		(ConvertSupport(common::FIXED16CHAR, common::VARCHAR))
		(ConvertSupport(common::FIXED16CHAR, common::VAR16CHAR))
		(ConvertSupport(common::FIXED16CHAR, common::VARBINARY))
		(ConvertSupport(common::FIXED16CHAR, common::INTERVALYEAR))
		(ConvertSupport(common::FIXED16CHAR, common::INTERVALDAY))
		(ConvertSupport(common::FIXEDBINARY, common::INT))
		(ConvertSupport(common::FIXEDBINARY, common::BIGINT))
		(ConvertSupport(common::FIXEDBINARY, common::DECIMAL9))
		(ConvertSupport(common::FIXEDBINARY, common::DECIMAL18))
		(ConvertSupport(common::FIXEDBINARY, common::DECIMAL28SPARSE))
		(ConvertSupport(common::FIXEDBINARY, common::DECIMAL38SPARSE))
		(ConvertSupport(common::FIXEDBINARY, common::VARDECIMAL))
		(ConvertSupport(common::FIXEDBINARY, common::DATE))
		(ConvertSupport(common::FIXEDBINARY, common::TIME))
		(ConvertSupport(common::FIXEDBINARY, common::TIMESTAMP))
		(ConvertSupport(common::FIXEDBINARY, common::INTERVAL))
		(ConvertSupport(common::FIXEDBINARY, common::FLOAT4))
		(ConvertSupport(common::FIXEDBINARY, common::FLOAT8))
		(ConvertSupport(common::FIXEDBINARY, common::BIT))
		(ConvertSupport(common::FIXEDBINARY, common::VARCHAR))
		(ConvertSupport(common::FIXEDBINARY, common::VAR16CHAR))
		(ConvertSupport(common::FIXEDBINARY, common::VARBINARY))
		(ConvertSupport(common::FIXEDBINARY, common::INTERVALYEAR))
		(ConvertSupport(common::FIXEDBINARY, common::INTERVALDAY))
		(ConvertSupport(common::VARCHAR, common::TINYINT))
		(ConvertSupport(common::VARCHAR, common::INT))
		(ConvertSupport(common::VARCHAR, common::BIGINT))
		(ConvertSupport(common::VARCHAR, common::DECIMAL9))
		(ConvertSupport(common::VARCHAR, common::DECIMAL18))
		(ConvertSupport(common::VARCHAR, common::DECIMAL28SPARSE))
		(ConvertSupport(common::VARCHAR, common::DECIMAL38SPARSE))
		(ConvertSupport(common::VARCHAR, common::VARDECIMAL))
		(ConvertSupport(common::VARCHAR, common::DATE))
		(ConvertSupport(common::VARCHAR, common::TIME))
		(ConvertSupport(common::VARCHAR, common::TIMESTAMP))
		(ConvertSupport(common::VARCHAR, common::INTERVAL))
		(ConvertSupport(common::VARCHAR, common::FLOAT4))
		(ConvertSupport(common::VARCHAR, common::FLOAT8))
		(ConvertSupport(common::VARCHAR, common::BIT))
		(ConvertSupport(common::VARCHAR, common::VARCHAR))
		(ConvertSupport(common::VARCHAR, common::VAR16CHAR))
		(ConvertSupport(common::VARCHAR, common::VARBINARY))
		(ConvertSupport(common::VARCHAR, common::INTERVALYEAR))
		(ConvertSupport(common::VARCHAR, common::INTERVALDAY))
		(ConvertSupport(common::VAR16CHAR, common::TINYINT))
		(ConvertSupport(common::VAR16CHAR, common::INT))
		(ConvertSupport(common::VAR16CHAR, common::BIGINT))
		(ConvertSupport(common::VAR16CHAR, common::DECIMAL9))
		(ConvertSupport(common::VAR16CHAR, common::DECIMAL18))
		(ConvertSupport(common::VAR16CHAR, common::DECIMAL28SPARSE))
		(ConvertSupport(common::VAR16CHAR, common::DECIMAL38SPARSE))
		(ConvertSupport(common::VAR16CHAR, common::VARDECIMAL))
		(ConvertSupport(common::VAR16CHAR, common::DATE))
		(ConvertSupport(common::VAR16CHAR, common::TIME))
		(ConvertSupport(common::VAR16CHAR, common::TIMESTAMP))
		(ConvertSupport(common::VAR16CHAR, common::INTERVAL))
		(ConvertSupport(common::VAR16CHAR, common::FLOAT4))
		(ConvertSupport(common::VAR16CHAR, common::FLOAT8))
		(ConvertSupport(common::VAR16CHAR, common::BIT))
		(ConvertSupport(common::VAR16CHAR, common::VARCHAR))
		(ConvertSupport(common::VAR16CHAR, common::VARBINARY))
		(ConvertSupport(common::VAR16CHAR, common::INTERVALYEAR))
		(ConvertSupport(common::VAR16CHAR, common::INTERVALDAY))
		(ConvertSupport(common::VARBINARY, common::TINYINT))
		(ConvertSupport(common::VARBINARY, common::INT))
		(ConvertSupport(common::VARBINARY, common::BIGINT))
		(ConvertSupport(common::VARBINARY, common::DECIMAL9))
		(ConvertSupport(common::VARBINARY, common::DECIMAL18))
		(ConvertSupport(common::VARBINARY, common::DECIMAL28SPARSE))
		(ConvertSupport(common::VARBINARY, common::DECIMAL38SPARSE))
		(ConvertSupport(common::VARBINARY, common::VARDECIMAL))
		(ConvertSupport(common::VARBINARY, common::DATE))
		(ConvertSupport(common::VARBINARY, common::TIME))
		(ConvertSupport(common::VARBINARY, common::TIMESTAMP))
		(ConvertSupport(common::VARBINARY, common::INTERVAL))
		(ConvertSupport(common::VARBINARY, common::FLOAT4))
		(ConvertSupport(common::VARBINARY, common::FLOAT8))
		(ConvertSupport(common::VARBINARY, common::BIT))
		(ConvertSupport(common::VARBINARY, common::VARCHAR))
		(ConvertSupport(common::VARBINARY, common::VAR16CHAR))
		(ConvertSupport(common::VARBINARY, common::VARBINARY))
		(ConvertSupport(common::VARBINARY, common::INTERVALYEAR))
		(ConvertSupport(common::VARBINARY, common::INTERVALDAY))
		(ConvertSupport(common::UINT1, common::INT))
		(ConvertSupport(common::UINT1, common::BIGINT))
		(ConvertSupport(common::UINT1, common::DECIMAL9))
		(ConvertSupport(common::UINT1, common::DECIMAL18))
		(ConvertSupport(common::UINT1, common::DECIMAL28SPARSE))
		(ConvertSupport(common::UINT1, common::DECIMAL38SPARSE))
		(ConvertSupport(common::UINT1, common::VARDECIMAL))
		(ConvertSupport(common::UINT1, common::DATE))
		(ConvertSupport(common::UINT1, common::TIME))
		(ConvertSupport(common::UINT1, common::TIMESTAMP))
		(ConvertSupport(common::UINT1, common::INTERVAL))
		(ConvertSupport(common::UINT1, common::FLOAT4))
		(ConvertSupport(common::UINT1, common::FLOAT8))
		(ConvertSupport(common::UINT1, common::BIT))
		(ConvertSupport(common::UINT1, common::VARCHAR))
		(ConvertSupport(common::UINT1, common::VAR16CHAR))
		(ConvertSupport(common::UINT1, common::VARBINARY))
		(ConvertSupport(common::UINT1, common::INTERVALYEAR))
		(ConvertSupport(common::UINT1, common::INTERVALDAY))
		(ConvertSupport(common::UINT2, common::INT))
		(ConvertSupport(common::UINT2, common::BIGINT))
		(ConvertSupport(common::UINT2, common::DECIMAL9))
		(ConvertSupport(common::UINT2, common::DECIMAL18))
		(ConvertSupport(common::UINT2, common::DECIMAL28SPARSE))
		(ConvertSupport(common::UINT2, common::DECIMAL38SPARSE))
		(ConvertSupport(common::UINT2, common::VARDECIMAL))
		(ConvertSupport(common::UINT2, common::DATE))
		(ConvertSupport(common::UINT2, common::TIME))
		(ConvertSupport(common::UINT2, common::TIMESTAMP))
		(ConvertSupport(common::UINT2, common::INTERVAL))
		(ConvertSupport(common::UINT2, common::FLOAT4))
		(ConvertSupport(common::UINT2, common::FLOAT8))
		(ConvertSupport(common::UINT2, common::BIT))
		(ConvertSupport(common::UINT2, common::VARCHAR))
		(ConvertSupport(common::UINT2, common::VAR16CHAR))
		(ConvertSupport(common::UINT2, common::VARBINARY))
		(ConvertSupport(common::UINT2, common::INTERVALYEAR))
		(ConvertSupport(common::UINT2, common::INTERVALDAY))
		(ConvertSupport(common::UINT4, common::INT))
		(ConvertSupport(common::UINT4, common::BIGINT))
		(ConvertSupport(common::UINT4, common::DECIMAL9))
		(ConvertSupport(common::UINT4, common::DECIMAL18))
		(ConvertSupport(common::UINT4, common::DECIMAL28SPARSE))
		(ConvertSupport(common::UINT4, common::DECIMAL38SPARSE))
		(ConvertSupport(common::UINT4, common::VARDECIMAL))
		(ConvertSupport(common::UINT4, common::DATE))
		(ConvertSupport(common::UINT4, common::TIME))
		(ConvertSupport(common::UINT4, common::TIMESTAMP))
		(ConvertSupport(common::UINT4, common::INTERVAL))
		(ConvertSupport(common::UINT4, common::FLOAT4))
		(ConvertSupport(common::UINT4, common::FLOAT8))
		(ConvertSupport(common::UINT4, common::BIT))
		(ConvertSupport(common::UINT4, common::VARCHAR))
		(ConvertSupport(common::UINT4, common::VAR16CHAR))
		(ConvertSupport(common::UINT4, common::VARBINARY))
		(ConvertSupport(common::UINT4, common::INTERVALYEAR))
		(ConvertSupport(common::UINT4, common::INTERVALDAY))
		(ConvertSupport(common::UINT8, common::INT))
		(ConvertSupport(common::UINT8, common::BIGINT))
		(ConvertSupport(common::UINT8, common::DECIMAL9))
		(ConvertSupport(common::UINT8, common::DECIMAL18))
		(ConvertSupport(common::UINT8, common::DECIMAL28SPARSE))
		(ConvertSupport(common::UINT8, common::DECIMAL38SPARSE))
		(ConvertSupport(common::UINT8, common::VARDECIMAL))
		(ConvertSupport(common::UINT8, common::DATE))
		(ConvertSupport(common::UINT8, common::TIME))
		(ConvertSupport(common::UINT8, common::TIMESTAMP))
		(ConvertSupport(common::UINT8, common::INTERVAL))
		(ConvertSupport(common::UINT8, common::FLOAT4))
		(ConvertSupport(common::UINT8, common::FLOAT8))
		(ConvertSupport(common::UINT8, common::BIT))
		(ConvertSupport(common::UINT8, common::VARCHAR))
		(ConvertSupport(common::UINT8, common::VAR16CHAR))
		(ConvertSupport(common::UINT8, common::VARBINARY))
		(ConvertSupport(common::UINT8, common::INTERVALYEAR))
		(ConvertSupport(common::UINT8, common::INTERVALDAY))
		(ConvertSupport(common::DECIMAL28DENSE, common::INT))
		(ConvertSupport(common::DECIMAL28DENSE, common::BIGINT))
		(ConvertSupport(common::DECIMAL28DENSE, common::DECIMAL9))
		(ConvertSupport(common::DECIMAL28DENSE, common::DECIMAL18))
		(ConvertSupport(common::DECIMAL28DENSE, common::DECIMAL28SPARSE))
		(ConvertSupport(common::DECIMAL28DENSE, common::DECIMAL38SPARSE))
		(ConvertSupport(common::DECIMAL28DENSE, common::VARDECIMAL))
		(ConvertSupport(common::DECIMAL28DENSE, common::DATE))
		(ConvertSupport(common::DECIMAL28DENSE, common::TIME))
		(ConvertSupport(common::DECIMAL28DENSE, common::TIMESTAMP))
		(ConvertSupport(common::DECIMAL28DENSE, common::INTERVAL))
		(ConvertSupport(common::DECIMAL28DENSE, common::FLOAT4))
		(ConvertSupport(common::DECIMAL28DENSE, common::FLOAT8))
		(ConvertSupport(common::DECIMAL28DENSE, common::BIT))
		(ConvertSupport(common::DECIMAL28DENSE, common::VARCHAR))
		(ConvertSupport(common::DECIMAL28DENSE, common::VAR16CHAR))
		(ConvertSupport(common::DECIMAL28DENSE, common::VARBINARY))
		(ConvertSupport(common::DECIMAL28DENSE, common::INTERVALYEAR))
		(ConvertSupport(common::DECIMAL28DENSE, common::INTERVALDAY))
		(ConvertSupport(common::DECIMAL38DENSE, common::INT))
		(ConvertSupport(common::DECIMAL38DENSE, common::BIGINT))
		(ConvertSupport(common::DECIMAL38DENSE, common::DECIMAL9))
		(ConvertSupport(common::DECIMAL38DENSE, common::DECIMAL18))
		(ConvertSupport(common::DECIMAL38DENSE, common::DECIMAL28SPARSE))
		(ConvertSupport(common::DECIMAL38DENSE, common::DECIMAL38SPARSE))
		(ConvertSupport(common::DECIMAL38DENSE, common::VARDECIMAL))
		(ConvertSupport(common::DECIMAL38DENSE, common::DATE))
		(ConvertSupport(common::DECIMAL38DENSE, common::TIME))
		(ConvertSupport(common::DECIMAL38DENSE, common::TIMESTAMP))
		(ConvertSupport(common::DECIMAL38DENSE, common::INTERVAL))
		(ConvertSupport(common::DECIMAL38DENSE, common::FLOAT4))
		(ConvertSupport(common::DECIMAL38DENSE, common::FLOAT8))
		(ConvertSupport(common::DECIMAL38DENSE, common::BIT))
		(ConvertSupport(common::DECIMAL38DENSE, common::VARCHAR))
		(ConvertSupport(common::DECIMAL38DENSE, common::VAR16CHAR))
		(ConvertSupport(common::DECIMAL38DENSE, common::VARBINARY))
		(ConvertSupport(common::DECIMAL38DENSE, common::INTERVALYEAR))
		(ConvertSupport(common::DECIMAL38DENSE, common::INTERVALDAY))
		(ConvertSupport(common::DM_UNKNOWN, common::TINYINT))
		(ConvertSupport(common::DM_UNKNOWN, common::INT))
		(ConvertSupport(common::DM_UNKNOWN, common::BIGINT))
		(ConvertSupport(common::DM_UNKNOWN, common::DECIMAL9))
		(ConvertSupport(common::DM_UNKNOWN, common::DECIMAL18))
		(ConvertSupport(common::DM_UNKNOWN, common::DECIMAL28SPARSE))
		(ConvertSupport(common::DM_UNKNOWN, common::DECIMAL38SPARSE))
		(ConvertSupport(common::DM_UNKNOWN, common::VARDECIMAL))
		(ConvertSupport(common::DM_UNKNOWN, common::DATE))
		(ConvertSupport(common::DM_UNKNOWN, common::TIME))
		(ConvertSupport(common::DM_UNKNOWN, common::TIMESTAMP))
		(ConvertSupport(common::DM_UNKNOWN, common::INTERVAL))
		(ConvertSupport(common::DM_UNKNOWN, common::FLOAT4))
		(ConvertSupport(common::DM_UNKNOWN, common::FLOAT8))
		(ConvertSupport(common::DM_UNKNOWN, common::BIT))
		(ConvertSupport(common::DM_UNKNOWN, common::VARCHAR))
		(ConvertSupport(common::DM_UNKNOWN, common::VAR16CHAR))
		(ConvertSupport(common::DM_UNKNOWN, common::VARBINARY))
		(ConvertSupport(common::DM_UNKNOWN, common::INTERVALYEAR))
		(ConvertSupport(common::DM_UNKNOWN, common::INTERVALDAY))
		(ConvertSupport(common::INTERVALYEAR, common::INT))
		(ConvertSupport(common::INTERVALYEAR, common::BIGINT))
		(ConvertSupport(common::INTERVALYEAR, common::DECIMAL9))
		(ConvertSupport(common::INTERVALYEAR, common::DECIMAL18))
		(ConvertSupport(common::INTERVALYEAR, common::DECIMAL28SPARSE))
		(ConvertSupport(common::INTERVALYEAR, common::DECIMAL38SPARSE))
		(ConvertSupport(common::INTERVALYEAR, common::VARDECIMAL))
		(ConvertSupport(common::INTERVALYEAR, common::DATE))
		(ConvertSupport(common::INTERVALYEAR, common::TIME))
		(ConvertSupport(common::INTERVALYEAR, common::TIMESTAMP))
		(ConvertSupport(common::INTERVALYEAR, common::INTERVAL))
		(ConvertSupport(common::INTERVALYEAR, common::FLOAT4))
		(ConvertSupport(common::INTERVALYEAR, common::FLOAT8))
		(ConvertSupport(common::INTERVALYEAR, common::BIT))
		(ConvertSupport(common::INTERVALYEAR, common::VARCHAR))
		(ConvertSupport(common::INTERVALYEAR, common::VAR16CHAR))
		(ConvertSupport(common::INTERVALYEAR, common::VARBINARY))
		(ConvertSupport(common::INTERVALYEAR, common::INTERVALYEAR))
		(ConvertSupport(common::INTERVALYEAR, common::INTERVALDAY))
		(ConvertSupport(common::INTERVALDAY, common::INT))
		(ConvertSupport(common::INTERVALDAY, common::BIGINT))
		(ConvertSupport(common::INTERVALDAY, common::DECIMAL9))
		(ConvertSupport(common::INTERVALDAY, common::DECIMAL18))
		(ConvertSupport(common::INTERVALDAY, common::DECIMAL28SPARSE))
		(ConvertSupport(common::INTERVALDAY, common::DECIMAL38SPARSE))
		(ConvertSupport(common::INTERVALDAY, common::VARDECIMAL))
		(ConvertSupport(common::INTERVALDAY, common::DATE))
		(ConvertSupport(common::INTERVALDAY, common::TIME))
		(ConvertSupport(common::INTERVALDAY, common::TIMESTAMP))
		(ConvertSupport(common::INTERVALDAY, common::INTERVAL))
		(ConvertSupport(common::INTERVALDAY, common::FLOAT4))
		(ConvertSupport(common::INTERVALDAY, common::FLOAT8))
		(ConvertSupport(common::INTERVALDAY, common::BIT))
		(ConvertSupport(common::INTERVALDAY, common::VARCHAR))
		(ConvertSupport(common::INTERVALDAY, common::VAR16CHAR))
		(ConvertSupport(common::INTERVALDAY, common::VARBINARY))
		(ConvertSupport(common::INTERVALDAY, common::INTERVALYEAR))
		(ConvertSupport(common::INTERVALDAY, common::INTERVALDAY));

static exec::user::ServerMeta createDefaultServerMeta() {
	exec::user::ServerMeta result;

	result.set_all_tables_selectable(false);
	result.set_blob_included_in_max_row_size(true);
	result.set_catalog_at_start(true);
	result.set_catalog_separator(s_catalogSeparator);
	result.set_catalog_term(s_catalogTerm);
	result.set_column_aliasing_supported(true);
	std::copy(s_convertMap.begin(), s_convertMap.end(),
	          google::protobuf::RepeatedFieldBackInserter(result.mutable_convert_support()));
	result.set_correlation_names_support(exec::user::CN_ANY);
	std::copy(s_dateTimeFunctions.begin(), s_dateTimeFunctions.end(),
			  google::protobuf::RepeatedFieldBackInserter(result.mutable_date_time_functions()));
	std::copy(s_dateTimeLiterals.begin(), s_dateTimeLiterals.end(),
			  google::protobuf::RepeatedFieldBackInserter(result.mutable_date_time_literals_support()));
	result.set_group_by_support(exec::user::GB_UNRELATED);
	result.set_identifier_casing(exec::user::IC_STORES_MIXED);
	result.set_identifier_quote_string(s_identifierQuoteString);
	result.set_like_escape_clause_supported(true);
	result.set_max_catalog_name_length(s_maxIdentifierSize);
	result.set_max_column_name_length(s_maxIdentifierSize);
	result.set_max_cursor_name_length(s_maxIdentifierSize);
	result.set_max_schema_name_length(s_maxIdentifierSize);
	result.set_max_table_name_length(s_maxIdentifierSize);
	result.set_max_user_name_length(s_maxIdentifierSize);
	result.set_null_collation(exec::user::NC_AT_END);
	result.set_null_plus_non_null_equals_null(true);
	std::copy(s_numericFunctions.begin(), s_numericFunctions.end(),
			  google::protobuf::RepeatedFieldBackInserter(result.mutable_numeric_functions()));
	std::copy(s_orderBySupport.begin(), s_orderBySupport.end(),
			  google::protobuf::RepeatedFieldBackInserter(result.mutable_order_by_support()));
	std::copy(s_outerJoinSupport.begin(), s_outerJoinSupport.end(),
			  google::protobuf::RepeatedFieldBackInserter(result.mutable_outer_join_support()));
	result.set_quoted_identifier_casing(exec::user::IC_STORES_MIXED);
	result.set_read_only(false);
	result.set_schema_term(s_schemaTerm);
	result.set_search_escape_string(s_searchEscapeString);
	result.set_special_characters(s_specialCharacters);
	std::copy(s_sqlKeywords.begin(), s_sqlKeywords.end(),
			  google::protobuf::RepeatedFieldBackInserter(result.mutable_sql_keywords()));
	std::copy(s_stringFunctions.begin(), s_stringFunctions.end(),
			  google::protobuf::RepeatedFieldBackInserter(result.mutable_string_functions()));
	std::copy(s_subQuerySupport.begin(), s_subQuerySupport.end(),
			google::protobuf::RepeatedFieldBackInserter(result.mutable_subquery_support()));
	std::copy(s_systemFunctions.begin(), s_systemFunctions.end(),
			  google::protobuf::RepeatedFieldBackInserter(result.mutable_system_functions()));
	result.set_table_term(s_tableTerm);
	std::copy(s_unionSupport.begin(), s_unionSupport.end(),
			  google::protobuf::RepeatedFieldBackInserter(result.mutable_union_support()));

	return result;
}

static Drill::meta::CollateSupport collateSupport(const google::protobuf::RepeatedField<google::protobuf::int32>& collateSupportList) {
	Drill::meta::CollateSupport result(Drill::meta::C_NONE);

	for(google::protobuf::RepeatedField<google::protobuf::int32>::const_iterator it = collateSupportList.begin();
		it != collateSupportList.end();
		++it) {
		switch(static_cast<exec::user::CollateSupport>(*it)) {
		case exec::user::CS_GROUP_BY:
			result |= Drill::meta::C_GROUPBY;
			break;

		// ignore unknown
		case exec::user::CS_UNKNOWN:
		default:
			break;
		}
	}
	return result;
}

static Drill::meta::CorrelationNamesSupport correlationNames(exec::user::CorrelationNamesSupport correlatioNamesSupport) {
	switch(correlatioNamesSupport) {
	case exec::user::CN_DIFFERENT_NAMES:
		return Drill::meta::CN_DIFFERENT_NAMES;

	case exec::user::CN_ANY:
		return Drill::meta::CN_ANY_NAMES;

	case exec::user::CN_NONE:
	default:
		// unknown value
		return CN_NONE;
	}
}

static Drill::meta::DateTimeLiteralSupport dateTimeLiteralsSupport(const google::protobuf::RepeatedField<google::protobuf::int32>& dateTimeLiteralsSupportList) {
	Drill::meta::DateTimeLiteralSupport result(Drill::meta::DL_NONE);

	for(google::protobuf::RepeatedField<google::protobuf::int32>::const_iterator it = dateTimeLiteralsSupportList.begin();
			it != dateTimeLiteralsSupportList.end();
			++it) {
			switch(static_cast<exec::user::DateTimeLiteralsSupport>(*it)) {
			case exec::user::DL_DATE:
				result |= Drill::meta::DL_DATE;
				break;

			case exec::user::DL_TIME:
				result |= Drill::meta::DL_TIME;
				break;

			case exec::user::DL_TIMESTAMP:
				result |= Drill::meta::DL_TIMESTAMP;
				break;

			case exec::user::DL_INTERVAL_YEAR:
				result |= Drill::meta::DL_INTERVAL_YEAR;
				break;

			case exec::user::DL_INTERVAL_YEAR_TO_MONTH:
				result |= Drill::meta::DL_INTERVAL_YEAR_TO_MONTH;
				break;

			case exec::user::DL_INTERVAL_MONTH:
				result |= Drill::meta::DL_INTERVAL_MONTH;
				break;

			case exec::user::DL_INTERVAL_DAY:
				result |= Drill::meta::DL_INTERVAL_DAY;
				break;

			case exec::user::DL_INTERVAL_DAY_TO_HOUR:
				result |= Drill::meta::DL_INTERVAL_DAY_TO_HOUR;
				break;

			case exec::user::DL_INTERVAL_DAY_TO_MINUTE:
				result |= Drill::meta::DL_INTERVAL_DAY_TO_MINUTE;
				break;

			case exec::user::DL_INTERVAL_DAY_TO_SECOND:
				result |= Drill::meta::DL_INTERVAL_DAY_TO_SECOND;
				break;

			case exec::user::DL_INTERVAL_HOUR:
				result |= Drill::meta::DL_INTERVAL_HOUR;
				break;

			case exec::user::DL_INTERVAL_HOUR_TO_MINUTE:
				result |= Drill::meta::DL_INTERVAL_HOUR_TO_MINUTE;
				break;

			case exec::user::DL_INTERVAL_HOUR_TO_SECOND:
				result |= Drill::meta::DL_INTERVAL_HOUR_TO_SECOND;
				break;

			case exec::user::DL_INTERVAL_MINUTE:
				result |= Drill::meta::DL_INTERVAL_MINUTE;
				break;

			case exec::user::DL_INTERVAL_MINUTE_TO_SECOND:
				result |= Drill::meta::DL_INTERVAL_MINUTE_TO_SECOND;
				break;

			case exec::user::DL_INTERVAL_SECOND:
				result |= Drill::meta::DL_INTERVAL_SECOND;
				break;

			// ignore unknown
			case exec::user::DL_UNKNOWN:
			default:
				break;
			}
		}

	return result;
}

static Drill::meta::GroupBySupport groupBySupport(exec::user::GroupBySupport groupBySupport) {
	switch(groupBySupport) {
	case exec::user::GB_SELECT_ONLY:
		return Drill::meta::GB_SELECT_ONLY;

	case exec::user::GB_BEYOND_SELECT:
		return Drill::meta::GB_BEYOND_SELECT;

	case exec::user::GB_NONE:
	default:
		// unknown value
		return Drill::meta::GB_NONE;
	}
}

static Drill::meta::IdentifierCase identifierCase(exec::user::IdentifierCasing identifierCasing) {
	switch(identifierCasing) {
	case exec::user::IC_STORES_LOWER:
		return Drill::meta::IC_STORES_LOWER;

	case exec::user::IC_STORES_MIXED:
		return Drill::meta::IC_STORES_MIXED;

	case exec::user::IC_STORES_UPPER:
		return Drill::meta::IC_STORES_UPPER;

	case exec::user::IC_SUPPORTS_MIXED:
		return Drill::meta::IC_SUPPORTS_MIXED;

	case exec::user::IC_UNKNOWN:
	default:
		// unknown value
		return Drill::meta::IC_UNKNOWN;
	}
}

static Drill::meta::NullCollation nullCollation(exec::user::NullCollation nullCollation) {
	switch(nullCollation) {
	case exec::user::NC_AT_END:
		return Drill::meta::NC_AT_END;

	case exec::user::NC_AT_START:
		return Drill::meta::NC_AT_START;

	case exec::user::NC_HIGH:
		return Drill::meta::NC_HIGH;

	case exec::user::NC_LOW:
		return Drill::meta::NC_LOW;

	case exec::user::NC_UNKNOWN:
	default:
		// unknown value
		return Drill::meta::NC_UNKNOWN;
	}
}

static Drill::meta::OuterJoinSupport outerJoinSupport(const google::protobuf::RepeatedField<google::protobuf::int32>& outerJoinSupportList) {
	Drill::meta::OuterJoinSupport result(Drill::meta::OJ_NONE);

	for(google::protobuf::RepeatedField<google::protobuf::int32>::const_iterator it = outerJoinSupportList.begin();
			it != outerJoinSupportList.end();
			++it) {
			switch(static_cast<exec::user::OuterJoinSupport>(*it)) {
			case exec::user::OJ_LEFT:
				result |= Drill::meta::OJ_LEFT;
				break;

			case exec::user::OJ_RIGHT:
				result |= Drill::meta::OJ_RIGHT;
				break;

			case exec::user::OJ_FULL:
				result |= Drill::meta::OJ_FULL;
				break;

			case exec::user::OJ_NESTED:
				result |= Drill::meta::OJ_NESTED;
				break;

			case exec::user::OJ_INNER:
				result |= Drill::meta::OJ_INNER;
				break;

			case exec::user::OJ_NOT_ORDERED:
				result |= Drill::meta::OJ_NOT_ORDERED;
				break;

			case exec::user::OJ_ALL_COMPARISON_OPS:
				result |= Drill::meta::OJ_ALL_COMPARISON_OPS;
				break;

			// ignore unknown
			case exec::user::OJ_UNKNOWN:
			default:
				break;
			}
		}

	return result;
}

static Drill::meta::QuotedIdentifierCase quotedIdentifierCase(exec::user::IdentifierCasing identifierCasing) {
	switch(identifierCasing) {
	case exec::user::IC_STORES_LOWER:
		return Drill::meta::QIC_STORES_LOWER;

	case exec::user::IC_STORES_MIXED:
		return Drill::meta::QIC_STORES_MIXED;

	case exec::user::IC_STORES_UPPER:
		return Drill::meta::QIC_STORES_UPPER;

	case exec::user::IC_SUPPORTS_MIXED:
		return Drill::meta::QIC_SUPPORTS_MIXED;

	case exec::user::IC_UNKNOWN:
	default:
		// unknown value
		return Drill::meta::QIC_UNKNOWN;
	}
}

static Drill::meta::SubQuerySupport subQuerySupport(const google::protobuf::RepeatedField<google::protobuf::int32>& subQuerySupportList) {
	Drill::meta::SubQuerySupport result(Drill::meta::SQ_NONE);

	for(google::protobuf::RepeatedField<google::protobuf::int32>::const_iterator it = subQuerySupportList.begin();
			it != subQuerySupportList.end();
			++it) {
			switch(static_cast<exec::user::SubQuerySupport>(*it)) {
			case exec::user::SQ_CORRELATED:
				result |= Drill::meta::SQ_CORRELATED;
				break;

			case exec::user::SQ_IN_COMPARISON:
				result |= Drill::meta::SQ_IN_COMPARISON;
				break;

			case exec::user::SQ_IN_EXISTS:
				result |= Drill::meta::SQ_IN_EXISTS;
				break;

			case exec::user::SQ_IN_INSERT:
				result |= Drill::meta::SQ_IN_INSERT;
				break;

			case exec::user::SQ_IN_QUANTIFIED:
				result |= Drill::meta::SQ_IN_QUANTIFIED;
				break;

			// ignore unknown
			case exec::user::SQ_UNKNOWN:
			default:
				break;
			}
		}

	return result;
}

static Drill::meta::UnionSupport unionSupport(const google::protobuf::RepeatedField<google::protobuf::int32>& unionSupportList) {
	Drill::meta::UnionSupport result(Drill::meta::U_NONE);

	for(google::protobuf::RepeatedField<google::protobuf::int32>::const_iterator it = unionSupportList.begin();
			it != unionSupportList.end();
			++it) {
			switch(static_cast<exec::user::UnionSupport>(*it)) {
			case exec::user::U_UNION:
				result |= Drill::meta::U_UNION;
				break;

			case exec::user::U_UNION_ALL:
				result |= Drill::meta::U_UNION_ALL;
				break;

			// ignore unknown
			case exec::user::U_UNKNOWN:
			default:
				break;
			}
		}

	return result;
}

static bool unrelatedColumnsInOrderBySupported(const google::protobuf::RepeatedField<google::protobuf::int32>& orderBySupportList) {
	for(google::protobuf::RepeatedField<google::protobuf::int32>::const_iterator it = orderBySupportList.begin();
			it != orderBySupportList.end();
			++it) {
			switch(static_cast<exec::user::OrderBySupport>(*it)) {
			case exec::user::OB_UNRELATED:
				return true;
				break;

			case exec::user::OB_EXPRESSION:
			// ignore unknown
			case exec::user::OB_UNKNOWN:
			default:
				break;
			}
		}

	return false;
}
} // anonymous namespace

const exec::user::ServerMeta DrillMetadata::s_defaultServerMeta = createDefaultServerMeta();

DrillMetadata::DrillMetadata(DrillClientImpl& client, const exec::user::ServerMeta&  serverMeta): Metadata(), m_client(client),
		m_allTablesSelectable(serverMeta.all_tables_selectable()),
		m_blobIncludedInMaxRowSize(serverMeta.blob_included_in_max_row_size()),
		m_catalogAtStart(serverMeta.catalog_at_start()),
		m_catalogSeparator(serverMeta.catalog_separator()),
		m_catalogTerm(serverMeta.catalog_term()),
		m_collateSupport(collateSupport(serverMeta.collate_support())),
		m_columnAliasingSupported(serverMeta.column_aliasing_supported()),
		m_correlationNamesSupport(correlationNames(serverMeta.correlation_names_support())),
		m_convertSupport(serverMeta.convert_support().begin(), serverMeta.convert_support().end()),
		m_dateTimeFunctions(serverMeta.date_time_functions().begin(), serverMeta.date_time_functions().end()),
		m_dateTimeLiteralsSupport(dateTimeLiteralsSupport(serverMeta.date_time_literals_support())),
		m_groupBySupport(groupBySupport(serverMeta.group_by_support())),
		m_identifierCase(identifierCase(serverMeta.identifier_casing())),
		m_identifierQuoteString(serverMeta.identifier_quote_string()),
		m_likeEscapeClauseSupported(serverMeta.like_escape_clause_supported()),
		m_maxBinaryLiteralLength(serverMeta.max_binary_literal_length()),
		m_maxCatalogNameLength(serverMeta.max_catalog_name_length()),
		m_maxCharLIteralLength(serverMeta.max_char_literal_length()),
		m_maxColumnNameLength(serverMeta.max_column_name_length()),
		m_maxColumnsInGroupBy(serverMeta.max_column_name_length()),
		m_maxColumnsInOrderBy(serverMeta.max_columns_in_order_by()),
		m_maxColumnsInSelect(serverMeta.max_columns_in_select()),
		m_maxCursorNameLength(serverMeta.max_cursor_name_length()),
		m_maxLogicalLobSize(serverMeta.max_logical_lob_size()),
		m_maxRowSize(serverMeta.max_row_size()),
		m_maxSchemaNameLength(serverMeta.max_schema_name_length()),
		m_maxStatementLength(serverMeta.max_statement_length()),
		m_maxStatements(serverMeta.max_statements()),
		m_maxTableNameLength(serverMeta.max_table_name_length()),
		m_maxTablesInSelectLength(serverMeta.max_tables_in_select()),
		m_maxUserNameLength(serverMeta.max_user_name_length()),
		m_nullCollation(nullCollation(serverMeta.null_collation())),
		m_nullPlusNonNullEqualsNull(serverMeta.null_plus_non_null_equals_null()),
		m_numericFunctions(serverMeta.numeric_functions().begin(), serverMeta.numeric_functions().end()),
		m_outerJoinSupport(outerJoinSupport(serverMeta.outer_join_support())),
		m_quotedIdentifierCase(quotedIdentifierCase(serverMeta.quoted_identifier_casing())),
		m_readOnly(serverMeta.read_only()),
		m_schemaTerm(serverMeta.schema_term()),
		m_searchEscapeString(serverMeta.search_escape_string()),
		m_selectForUpdateSupported(serverMeta.select_for_update_supported()),
		m_specialCharacters(serverMeta.special_characters()),
		m_sqlKeywords(serverMeta.sql_keywords().begin(), serverMeta.sql_keywords().end()),
		m_stringFunctions(serverMeta.string_functions().begin(), serverMeta.string_functions().end()),
		m_subQuerySupport(subQuerySupport(serverMeta.subquery_support())),
		m_systemFunctions(serverMeta.system_functions().begin(), serverMeta.system_functions().end()),
		m_tableTerm(serverMeta.table_term()),
		m_transactionSupported(serverMeta.transaction_supported()),
		m_unionSupport(unionSupport(serverMeta.union_support())),
		m_unrelatedColumnsInOrderBySupported(unrelatedColumnsInOrderBySupported(serverMeta.order_by_support()))
{
}

// Conversion scalar function support
bool DrillMetadata::isConvertSupported(common::MinorType from, common::MinorType to) const {
	return m_convertSupport.find(ConvertSupport(from,to)) != m_convertSupport.end();
}

const std::string& DrillMetadata::getServerName() const {
	return m_client.getServerInfos().name();
}
const std::string& DrillMetadata::getServerVersion() const {
	return m_client.getServerInfos().version();
}
uint32_t DrillMetadata::getServerMajorVersion() const {
	return m_client.getServerInfos().majorversion();
}

uint32_t DrillMetadata::getServerMinorVersion() const {
	return m_client.getServerInfos().minorversion();
}

uint32_t DrillMetadata::getServerPatchVersion() const {
	return m_client.getServerInfos().patchversion();
}

status_t DrillMetadata::getCatalogs(const std::string& catalogPattern, Metadata::pfnCatalogMetadataListener listener, void* listenerCtx, QueryHandle_t* qHandle) {
	DrillClientCatalogResult* result = m_client.getCatalogs(catalogPattern, m_searchEscapeString, listener, listenerCtx);
	if(result==NULL){
		*qHandle=NULL;
		return static_cast<status_t>(m_client.getError()->status);
	}
	*qHandle=reinterpret_cast<QueryHandle_t>(result);
	return QRY_SUCCESS;
}
status_t DrillMetadata::getSchemas(const std::string& catalogPattern, const std::string& schemaPattern, Metadata::pfnSchemaMetadataListener listener, void* listenerCtx, QueryHandle_t* qHandle) {
	DrillClientSchemaResult* result = m_client.getSchemas(catalogPattern, schemaPattern, m_searchEscapeString, listener, listenerCtx);
	if(result==NULL){
		*qHandle=NULL;
		return static_cast<status_t>(m_client.getError()->status);
	}
	*qHandle=reinterpret_cast<QueryHandle_t>(result);
	return QRY_SUCCESS;
}
status_t DrillMetadata::getTables(const std::string& catalogPattern, const std::string& schemaPattern, const std::string& tablePattern, const std::vector<std::string>* tableTypes, Metadata::pfnTableMetadataListener listener, void* listenerCtx, QueryHandle_t* qHandle) {
	DrillClientTableResult* result = m_client.getTables(catalogPattern, schemaPattern, tablePattern, tableTypes, m_searchEscapeString, listener, listenerCtx);
	if(result==NULL){
		*qHandle=NULL;
		return static_cast<status_t>(m_client.getError()->status);
	}
	*qHandle=reinterpret_cast<QueryHandle_t>(result);
	return QRY_SUCCESS;
}
status_t DrillMetadata::getColumns(const std::string& catalogPattern, const std::string& schemaPattern, const std:: string& tablePattern, const std::string& columnPattern, Metadata::pfnColumnMetadataListener listener, void* listenerCtx, QueryHandle_t* qHandle) {
	DrillClientColumnResult* result = m_client.getColumns(catalogPattern, schemaPattern, tablePattern, columnPattern, m_searchEscapeString, listener, listenerCtx);
	if(result==NULL){
		*qHandle=NULL;
		return static_cast<status_t>(m_client.getError()->status);
	}
	*qHandle=reinterpret_cast<QueryHandle_t>(result);
	return QRY_SUCCESS;
}
} // namespace meta
} // namespace Drill
