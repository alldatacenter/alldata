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
#ifndef FIELDMETA_H
#define FIELDMETA_H

#include "drill/common.hpp"
#include "drill/protobuf/Types.pb.h"

namespace exec{
    namespace shared{
        class SerializedField;
    };
    namespace user{
        class ResultColumnMetadata;
    };
};


namespace Drill {

class DECLSPEC_DRILL_CLIENT FieldMetadata{
    public:
        enum ColumnSearchability { UNKNOWN_SEARCHABILITY = 0, NONE = 1, CHAR = 2, NUMBER = 3, ALL = 4 };
        enum ColumnUpdatability { UNKNOWN_UPDATABILITY = 0, READ_ONLY = 1, WRITABLE = 2 };

        FieldMetadata(){};
        void set(const exec::shared::SerializedField& f);
        void set(const exec::user::ResultColumnMetadata& m);
        const std::string& getName() const{ return m_name;}
        common::MinorType getMinorType() const{ return m_minorType;}
        common::DataMode getDataMode() const{return m_dataMode;}
        uint32_t getValueCount() const{return m_valueCount;}
        uint32_t getScale() const{return m_scale;}
        uint32_t getPrecision() const{return m_precision;}
        uint32_t getBufferLength() const{return m_bufferLength;}
        const std::string& getCatalogName() const{return m_catalogName;}
        const std::string& getSchemaName() const{return m_schemaName;}
        const std::string& getTableName() const{return m_tableName;}
        const std::string& getLabel() const{return m_label;}
        const std::string& getSQLType() const{return m_sqlType;}
        bool isNullable() const{return m_nullable;}
        bool isSigned() const{return m_signed;}
        uint32_t getDisplaySize() const{return m_displaySize;}
        bool isAliased() const{return m_aliased;}
        ColumnSearchability getSearchability() const{return m_searchability;}
        ColumnUpdatability getUpdatability() const{return m_updatability;}
        bool isAutoIncremented() const{return m_autoIncremented;}
        bool isCaseSensitive() const{return m_caseSensitive;}
        bool isSortable() const{return m_sortable;}
        bool isCurrency() const{return m_currency;}
        void copy(Drill::FieldMetadata& f){
            m_name=f.m_name;
            m_minorType=f.m_minorType;
            m_dataMode=f.m_dataMode;
            m_valueCount=f.m_valueCount;
            m_scale=f.m_scale;
            m_precision=f.m_precision;
            m_bufferLength=f.m_bufferLength;
            m_catalogName=f.m_catalogName;
            m_schemaName=f.m_schemaName;
            m_tableName=f.m_tableName;
            m_label=f.m_label;
            m_sqlType=f.m_sqlType;
            m_nullable=f.m_nullable;
            m_signed=f.m_signed;
            m_displaySize=f.m_displaySize;
            m_aliased=f.m_aliased;
            m_searchability=f.m_searchability;
            m_updatability=f.m_updatability;
            m_autoIncremented=f.m_autoIncremented;
            m_caseSensitive=f.m_caseSensitive;
            m_sortable=f.m_sortable;
            m_currency=f.m_currency;
            m_columnSize=f.m_columnSize;
        }

    private:
        std::string m_name;
        common::MinorType m_minorType;
        common::DataMode m_dataMode;
        uint32_t m_valueCount;
        uint32_t m_scale;
        uint32_t m_precision;
        uint32_t m_bufferLength;
        std::string m_catalogName;
        std::string m_schemaName;
        std::string m_tableName;
        std::string m_label;
        std::string m_sqlType;
        bool m_nullable;
        bool m_signed;
        uint32_t m_displaySize;
        bool m_aliased;
        ColumnSearchability m_searchability;
        ColumnUpdatability m_updatability;
        bool m_autoIncremented;
        bool m_caseSensitive;
        bool m_sortable;
        bool m_currency;
        uint32_t m_columnSize;

};
} // namespace

#endif

