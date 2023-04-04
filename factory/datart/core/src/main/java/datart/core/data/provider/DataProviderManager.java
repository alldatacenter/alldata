/*
 * Datart
 * <p>
 * Copyright 2021
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package datart.core.data.provider;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

public interface DataProviderManager {

    List<DataProviderInfo> getSupportedDataProviders();

    DataProviderConfigTemplate getSourceConfigTemplate(String type) throws IOException;

    Object testConnection(DataProviderSource source) throws Exception;

    Set<String> readAllDatabases(DataProviderSource source) throws SQLException;

    Set<String> readTables(DataProviderSource source, String database) throws SQLException;

    Set<Column> readTableColumns(DataProviderSource source, String schema, String table) throws SQLException;

    Dataframe execute(DataProviderSource source, QueryScript queryScript, ExecuteParam param) throws Exception;

    Set<StdSqlOperator> supportedStdFunctions(DataProviderSource source);

    boolean validateFunction(DataProviderSource source, String snippet);

    void updateSource(DataProviderSource source);

}