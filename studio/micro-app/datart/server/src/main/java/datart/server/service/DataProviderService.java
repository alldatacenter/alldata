package datart.server.service;


import datart.core.data.provider.*;
import datart.core.entity.Source;
import datart.server.base.params.ViewExecuteParam;
import datart.server.base.params.TestExecuteParam;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

public interface DataProviderService {


    List<DataProviderInfo> getSupportedDataProviders();

    DataProviderConfigTemplate getSourceConfigTemplate(String type) throws IOException;

    Object testConnection(DataProviderSource source) throws Exception;

    Set<String> readAllDatabases(String sourceId) throws SQLException;

    Set<String> readTables(String sourceId, String database) throws SQLException;

    Set<Column> readTableColumns(String sourceId, String schema, String table) throws SQLException;

    Dataframe testExecute(TestExecuteParam testExecuteParam) throws Exception;

    Dataframe execute(ViewExecuteParam viewExecuteParam) throws Exception;

    Dataframe execute(ViewExecuteParam viewExecuteParam, boolean checkViewPermission) throws Exception;

    Set<StdSqlOperator> supportedStdFunctions(String sourceId);

    boolean validateFunction(String sourceId, String snippet);

    String decryptValue(String value);

    void updateSource(Source source);

    DataProviderSource parseDataProviderConfig(Source source);

}
