/*
 * Copyright (2022) Bytedance Ltd. and/or its affiliates
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#pragma once
#include <TableFunctions/ITableFunction.h>
#include <Interpreters/Cluster.h>
#include <Interpreters/StorageID.h>

namespace DB
{

class TableFunctionCnch : public ITableFunction
{
public:
    explicit TableFunctionCnch(std::string name_) : name{std::move(name_)} {}
    std::string getName() const override {return name;}
    ColumnsDescription getActualTableStructure(ContextPtr context) const override;
    bool needStructureConversion() const override { return false; }

private:
    StoragePtr executeImpl(const ASTPtr & ast_function, ContextPtr context, const std::string & table_name, ColumnsDescription cached_columns) const override;
    const char * getStorageTypeName() const override { return "Distributed"; }

    void parseArguments(const ASTPtr & ast_function, ContextPtr context) override;

    std::string name;

    String cluster_name;
    ClusterPtr cluster;
    StorageID remote_table_id = StorageID::createEmpty();

    static constexpr auto help_message = R"#(Cnch Table function requires two/three parameters.
The first parameter is cluster name.
The cluster name can have value is `server` for the cluster that contains all cnch servers
or virtual warehouse name,
virtual warehouse name could be one of following value: vw_default, vw_read, vw_write, vw_task
The second parameter could be a database name or `database.table` name.
The third parameter if have is table name. In that case the second parameter must be database name.
Example:
    select * from cnch(server, system.one)
    select * from cnch(server, system, one)
    select * from cnch(vw_default, system, one)
)#";
};

std::shared_ptr<Cluster> mockVWCluster(const Context & context, const String & vw_name);

}
