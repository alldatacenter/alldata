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
#ifndef DRILL_METADATA_H
#define DRILL_METADATA_H

#include <boost/ref.hpp>
#include <boost/unordered_set.hpp>

#include "drill/common.hpp"
#include "drill/drillClient.hpp"
#include "env.h"
#include "User.pb.h"

namespace Drill {
class DrillClientImpl;

namespace meta {
	class DrillCatalogMetadata: public meta::CatalogMetadata {
	public:
		DrillCatalogMetadata(const ::exec::user::CatalogMetadata& metadata):
			meta::CatalogMetadata(),
			m_pMetadata(metadata){
		}

	  bool hasCatalogName() const { return m_pMetadata.get().has_catalog_name(); }
	  const std::string& getCatalogName() const { return m_pMetadata.get().catalog_name(); }

	  bool hasDescription() const { return m_pMetadata.get().has_description(); }
	  const std::string& getDescription() const { return m_pMetadata.get().description(); }

	  bool hasConnect() const { return m_pMetadata.get().has_connect(); }
	  const std::string& getConnect() const { return m_pMetadata.get().connect(); }

	private:
		boost::reference_wrapper<const ::exec::user::CatalogMetadata> m_pMetadata;
	};

	class DrillSchemaMetadata: public meta::SchemaMetadata {
	public:
		DrillSchemaMetadata(const ::exec::user::SchemaMetadata& metadata):
			meta::SchemaMetadata(),
			m_pMetadata(metadata){
		}

		bool hasCatalogName() const { return m_pMetadata.get().has_catalog_name(); }
		const std::string& getCatalogName() const { return m_pMetadata.get().catalog_name(); }

		bool hasSchemaName() const { return m_pMetadata.get().has_schema_name(); }
		const std::string& getSchemaName() const { return m_pMetadata.get().schema_name(); }

		bool hasOwnerName() const { return m_pMetadata.get().has_owner(); }
		const std::string& getOwner() const { return m_pMetadata.get().owner(); }

		bool hasType() const { return m_pMetadata.get().has_type(); }
		const std::string& getType() const { return m_pMetadata.get().type(); }

		bool hasMutable() const { return m_pMetadata.get().has_mutable_(); }
		const std::string& getMutable() const { return m_pMetadata.get().mutable_(); }

	private:
		boost::reference_wrapper<const ::exec::user::SchemaMetadata> m_pMetadata;
	};

	class DrillTableMetadata: public meta::TableMetadata {
	public:
		DrillTableMetadata(const ::exec::user::TableMetadata& metadata):
			meta::TableMetadata(),
			m_pMetadata(metadata){
		}

	  bool hasCatalogName() const { return m_pMetadata.get().has_catalog_name(); }
	  const std::string& getCatalogName() const { return m_pMetadata.get().catalog_name(); }

	  bool hasSchemaName() const { return m_pMetadata.get().has_schema_name(); }
	  const std::string& getSchemaName() const { return m_pMetadata.get().schema_name(); }

	  bool hasTableName() const { return m_pMetadata.get().has_table_name(); }
	  const std::string& getTableName() const { return m_pMetadata.get().table_name(); }

	  bool hasType() const { return m_pMetadata.get().has_type(); }
	  const std::string& getType() const { return m_pMetadata.get().type(); }

	private:
	  boost::reference_wrapper<const ::exec::user::TableMetadata> m_pMetadata;
	};

	class DrillColumnMetadata: public meta::ColumnMetadata {
	public:
		DrillColumnMetadata(const ::exec::user::ColumnMetadata& metadata):
			meta::ColumnMetadata(),
			m_pMetadata(metadata){
		}

		bool hasCatalogName() const { return m_pMetadata.get().has_catalog_name(); }
		const std::string& getCatalogName() const { return m_pMetadata.get().catalog_name(); }

		bool hasSchemaName() const { return m_pMetadata.get().has_schema_name(); }
		const std::string& getSchemaName() const { return m_pMetadata.get().schema_name(); }

		bool hasTableName() const { return m_pMetadata.get().has_table_name(); }
		const std::string& getTableName() const { return m_pMetadata.get().table_name(); }

		bool hasColumnName() const { return m_pMetadata.get().has_column_name(); }
		const std::string& getColumnName() const { return m_pMetadata.get().column_name(); }

		bool hasOrdinalPosition() const { return m_pMetadata.get().has_ordinal_position(); }
		std::size_t getOrdinalPosition() const { return m_pMetadata.get().ordinal_position(); }

		bool hasDefaultValue() const { return m_pMetadata.get().has_default_value(); }
		const std::string& getDefaultValue() const { return m_pMetadata.get().default_value(); }

		bool hasNullable() const { return m_pMetadata.get().has_is_nullable(); }
		bool isNullable() const { return m_pMetadata.get().is_nullable(); }

		bool hasDataType() const { return m_pMetadata.get().has_data_type(); }
		const std::string& getDataType() const { return m_pMetadata.get().data_type(); }

		bool hasColumnSize() const { return m_pMetadata.get().has_column_size(); }
		std::size_t getColumnSize() const { return m_pMetadata.get().column_size(); }

		bool hasCharMaxLength() const { return m_pMetadata.get().has_char_max_length(); }
		std::size_t getCharMaxLength() const { return m_pMetadata.get().char_max_length(); }

		bool hasCharOctetLength() const { return m_pMetadata.get().has_char_octet_length(); }
		std::size_t getCharOctetLength() const { return m_pMetadata.get().char_octet_length(); }

		bool hasNumericPrecision() const { return m_pMetadata.get().has_numeric_precision(); }
		int32_t getNumericPrecision() const { return m_pMetadata.get().numeric_precision(); }

		bool hasNumericRadix() const { return m_pMetadata.get().has_numeric_precision_radix(); }
		int32_t getNumericRadix() const { return m_pMetadata.get().numeric_precision_radix(); }

		bool hasNumericScale() const { return m_pMetadata.get().has_numeric_scale(); }
		int32_t getNumericScale() const { return m_pMetadata.get().numeric_scale(); }

		bool hasIntervalType() const { return m_pMetadata.get().has_interval_type(); }
		const std::string& getIntervalType() const { return m_pMetadata.get().interval_type(); }

		bool hasIntervalPrecision() const { return m_pMetadata.get().has_interval_precision(); }
		int32_t getIntervalPrecision() const { return m_pMetadata.get().interval_precision(); }

	private:
		boost::reference_wrapper<const ::exec::user::ColumnMetadata> m_pMetadata;
	};

	struct ConvertSupportHasher {
		std::size_t operator()(const exec::user::ConvertSupport& key) const {
			std::size_t hash = 0;

			boost::hash_combine(hash, key.from());
			boost::hash_combine(hash, key.to());

			return hash;
		}
	};

	struct ConvertSupportEqualTo {
		bool operator()(exec::user::ConvertSupport const& cs1, exec::user::ConvertSupport const& cs2) const {
			return cs1.from() == cs2.from() && cs1.to() == cs2.to();
		}
	};

	typedef boost::unordered_set<exec::user::ConvertSupport, ConvertSupportHasher, ConvertSupportEqualTo> convert_support_set;

    class DrillMetadata: public Metadata {
    public:
        static const std::string s_connectorName; 
        static const std::string s_connectorVersion; 

        static const std::string s_serverName;
        static const std::string s_serverVersion;

        // Default server meta, to be used as fallback if cannot be queried
        static const exec::user::ServerMeta s_defaultServerMeta;

        DrillMetadata(DrillClientImpl& client, const exec::user::ServerMeta&  serverMeta);
        ~DrillMetadata() {}

        DrillClientImpl& client() { return m_client; }

        const std::string& getConnectorName() const { return s_connectorName; };
        const std::string& getConnectorVersion() const { return s_connectorVersion; }
        uint32_t getConnectorMajorVersion() const { return DRILL_VERSION_MAJOR; } 
        uint32_t getConnectorMinorVersion() const { return DRILL_VERSION_MINOR; } 
        uint32_t getConnectorPatchVersion() const { return DRILL_VERSION_PATCH; } 

        const std::string& getServerName() const;
        const std::string& getServerVersion() const;
        uint32_t getServerMajorVersion() const;
        uint32_t getServerMinorVersion() const;
        uint32_t getServerPatchVersion() const;

        status_t getCatalogs(const std::string& catalogPattern, Metadata::pfnCatalogMetadataListener listener, void* listenerCtx, QueryHandle_t* qHandle);
        status_t getSchemas(const std::string& catalogPattern, const std::string& schemaPattern, Metadata::pfnSchemaMetadataListener listener, void* listenerCtx, QueryHandle_t* qHandle);
        status_t getTables(const std::string& catalogPattern, const std::string& schemaPattern, const std::string& tablePattern, const std::vector<std::string>* tableTypes, Metadata::pfnTableMetadataListener listener, void* listenerCtx, QueryHandle_t* qHandle);
        status_t getColumns(const std::string& catalogPattern, const std::string& schemaPattern, const std:: string& tablePattern, const std::string& columnPattern, Metadata::pfnColumnMetadataListener listener, void* listenerCtx, QueryHandle_t* qHandle);

        bool areAllTableSelectable() const { return m_allTablesSelectable; }
        bool isCatalogAtStart() const { return m_catalogAtStart; }
        const std::string& getCatalogSeparator() const { return m_catalogSeparator; }
        const std::string& getCatalogTerm() const { return m_catalogTerm; }
        bool isColumnAliasingSupported() const { return m_columnAliasingSupported; }
        bool isNullPlusNonNullNull() const { return m_nullPlusNonNullEqualsNull; }
        bool isConvertSupported(common::MinorType from, common::MinorType to) const;
        meta::CorrelationNamesSupport getCorrelationNames() const { return m_correlationNamesSupport; }
        bool isReadOnly() const { return m_readOnly; }
        meta::DateTimeLiteralSupport getDateTimeLiteralsSupport() const { return m_dateTimeLiteralsSupport; }

        meta::CollateSupport getCollateSupport() const { return m_collateSupport; }
        meta::GroupBySupport getGroupBySupport() const { return m_groupBySupport; }
        meta::IdentifierCase getIdentifierCase() const { return m_identifierCase; }

        const std::string& getIdentifierQuoteString() const { return m_identifierQuoteString; }
        const std::vector<std::string>& getSQLKeywords() const { return m_sqlKeywords; }
        bool isLikeEscapeClauseSupported() const { return m_likeEscapeClauseSupported; }
        std::size_t getMaxBinaryLiteralLength() const { return m_maxBinaryLiteralLength; }
        std::size_t getMaxCatalogNameLength() const { return m_maxCatalogNameLength; }
        std::size_t getMaxCharLiteralLength() const { return m_maxCharLIteralLength; }
        std::size_t getMaxColumnNameLength() const { return m_maxColumnNameLength; }
        std::size_t getMaxColumnsInGroupBy() const { return m_maxColumnsInGroupBy; }
        std::size_t getMaxColumnsInOrderBy() const { return m_maxColumnsInOrderBy; }
        std::size_t getMaxColumnsInSelect() const { return m_maxColumnsInSelect; }
        std::size_t getMaxCursorNameLength() const { return m_maxCursorNameLength; }
        std::size_t getMaxLogicalLobSize() const { return m_maxLogicalLobSize; }
        std::size_t getMaxStatements() const { return m_maxStatements; }
        std::size_t getMaxRowSize() const { return m_maxRowSize; }
        bool isBlobIncludedInMaxRowSize() const { return m_blobIncludedInMaxRowSize; }
        std::size_t getMaxSchemaNameLength() const { return m_maxSchemaNameLength; }
        std::size_t getMaxStatementLength() const { return m_maxStatementLength; }
        std::size_t getMaxTableNameLength() const { return m_maxTableNameLength; }
        std::size_t getMaxTablesInSelect() const { return m_maxTablesInSelectLength; }
        std::size_t getMaxUserNameLength() const { return m_maxUserNameLength; }
        meta::NullCollation getNullCollation() const { return m_nullCollation; }
        const std::vector<std::string>& getNumericFunctions() const { return m_numericFunctions; }
        meta::OuterJoinSupport getOuterJoinSupport() const { return m_outerJoinSupport; }
        bool isUnrelatedColumnsInOrderBySupported() const { return m_unrelatedColumnsInOrderBySupported; }
        meta::QuotedIdentifierCase getQuotedIdentifierCase() const { return m_quotedIdentifierCase; }
        const std::string& getSchemaTerm() const { return m_schemaTerm; }
        const std::string& getSearchEscapeString() const { return m_searchEscapeString; }
        const std::string& getSpecialCharacters() const { return m_specialCharacters; }
        const std::vector<std::string>& getStringFunctions() const { return m_stringFunctions; }
        meta::SubQuerySupport getSubQuerySupport() const { return m_subQuerySupport; }
        const std::vector<std::string>& getSystemFunctions() const { return m_systemFunctions; }
        const std::string& getTableTerm() const { return m_tableTerm; }
        const std::vector<std::string>& getDateTimeFunctions() const { return m_dateTimeFunctions; }
        bool isTransactionSupported() const { return m_transactionSupported; }
        meta::UnionSupport getUnionSupport() const { return m_unionSupport; }
        bool isSelectForUpdateSupported() const { return m_selectForUpdateSupported; }

    private:
        DrillClientImpl& m_client;

		bool m_allTablesSelectable;
		bool m_blobIncludedInMaxRowSize;
		bool m_catalogAtStart;
        std::string m_catalogSeparator;
        std::string m_catalogTerm;
		Drill::meta::CollateSupport m_collateSupport;
		bool m_columnAliasingSupported;
		Drill::meta::CorrelationNamesSupport m_correlationNamesSupport;
		convert_support_set m_convertSupport;
        std::vector<std::string> m_dateTimeFunctions;
		Drill::meta::DateTimeLiteralSupport m_dateTimeLiteralsSupport;
		Drill::meta::GroupBySupport m_groupBySupport;
		Drill::meta::IdentifierCase m_identifierCase;
        std::string m_identifierQuoteString;
		bool m_likeEscapeClauseSupported;
		std::size_t m_maxBinaryLiteralLength;
		std::size_t m_maxCatalogNameLength;
		std::size_t m_maxCharLIteralLength;
		std::size_t m_maxColumnNameLength;
		std::size_t m_maxColumnsInGroupBy;
		std::size_t m_maxColumnsInOrderBy;
		std::size_t m_maxColumnsInSelect;
		std::size_t m_maxCursorNameLength;
		std::size_t m_maxLogicalLobSize;
		std::size_t m_maxRowSize;
		std::size_t m_maxSchemaNameLength;
		std::size_t m_maxStatementLength;
		std::size_t m_maxStatements;
		std::size_t m_maxTableNameLength;
		std::size_t m_maxTablesInSelectLength;
		std::size_t m_maxUserNameLength;
		Drill::meta::NullCollation m_nullCollation;
		bool m_nullPlusNonNullEqualsNull;
        std::vector<std::string> m_numericFunctions;
		Drill::meta::OuterJoinSupport m_outerJoinSupport;
		Drill::meta::QuotedIdentifierCase m_quotedIdentifierCase;
		bool m_readOnly;
        std::string m_schemaTerm;
        std::string m_searchEscapeString;
		bool m_selectForUpdateSupported;
        std::string m_specialCharacters;
        std::vector<std::string> m_sqlKeywords;
        std::vector<std::string> m_stringFunctions;
		Drill::meta::SubQuerySupport m_subQuerySupport;
        std::vector<std::string> m_systemFunctions;
        std::string m_tableTerm;
		bool m_transactionSupported;
		Drill::meta::UnionSupport m_unionSupport;
		bool m_unrelatedColumnsInOrderBySupported;
};
} // namespace meta
} // namespace Drill

#endif // DRILL_METADATA
