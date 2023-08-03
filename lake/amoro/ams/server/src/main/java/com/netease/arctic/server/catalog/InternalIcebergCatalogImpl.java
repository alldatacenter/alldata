package com.netease.arctic.server.catalog;

import com.netease.arctic.ams.api.CatalogMeta;
import com.netease.arctic.catalog.IcebergCatalogWrapper;
import com.netease.arctic.io.ArcticFileIO;
import com.netease.arctic.io.ArcticFileIOAdapter;
import com.netease.arctic.server.ArcticManagementConf;
import com.netease.arctic.server.IcebergRestCatalogService;
import com.netease.arctic.server.iceberg.InternalTableOperations;
import com.netease.arctic.server.persistence.mapper.TableMetaMapper;
import com.netease.arctic.server.table.TableMetadata;
import com.netease.arctic.server.utils.Configurations;
import com.netease.arctic.server.utils.IcebergTableUtil;
import com.netease.arctic.table.ArcticTable;
import org.apache.iceberg.BaseTable;
import org.apache.iceberg.CatalogProperties;
import org.apache.iceberg.TableOperations;
import org.apache.iceberg.catalog.TableIdentifier;
import org.apache.iceberg.io.FileIO;
import org.apache.iceberg.rest.RESTCatalog;

public class InternalIcebergCatalogImpl extends InternalCatalog {

  private static final String URI = "uri";

  final int httpPort;
  final String exposedHost;

  protected InternalIcebergCatalogImpl(CatalogMeta metadata, Configurations serverConfiguration) {
    super(metadata);
    this.httpPort = serverConfiguration.getInteger(ArcticManagementConf.HTTP_SERVER_PORT);
    this.exposedHost = serverConfiguration.getString(ArcticManagementConf.SERVER_EXPOSE_HOST);
  }


  @Override
  public CatalogMeta getMetadata() {
    CatalogMeta meta = super.getMetadata();
    if (!meta.getCatalogProperties().containsKey(URI)) {
      meta.putToCatalogProperties(URI, defaultRestURI());
    }
    meta.putToCatalogProperties(CatalogProperties.CATALOG_IMPL, RESTCatalog.class.getName());
    return meta.deepCopy();
  }

  @Override
  public void updateMetadata(CatalogMeta metadata) {
    String defaultUrl = defaultRestURI();
    String uri = metadata.getCatalogProperties().getOrDefault(URI, defaultUrl);
    if (defaultUrl.equals(uri)) {
      metadata.getCatalogProperties().remove(URI);
    }
    super.updateMetadata(metadata);
  }

  @Override
  public ArcticTable loadTable(String database, String tableName) {
    TableMetadata tableMetadata = getAs(TableMetaMapper.class, mapper ->
        mapper.selectTableMetaByName(getMetadata().getCatalogName(), database, tableName));
    if (tableMetadata == null) {
      return null;
    }
    FileIO io = IcebergTableUtil.newIcebergFileIo(getMetadata());
    ArcticFileIO fileIO = new ArcticFileIOAdapter(io);
    TableOperations ops = InternalTableOperations.buildForLoad(tableMetadata, io);
    BaseTable table = new BaseTable(ops, TableIdentifier.of(database, tableName).toString());
    return new IcebergCatalogWrapper.BasicIcebergTable(
        com.netease.arctic.table.TableIdentifier.of(name(), database, tableName),
        table, fileIO, getMetadata().getCatalogProperties()
    );
  }


  private String defaultRestURI() {
    return "http://" + exposedHost + ":" + httpPort + IcebergRestCatalogService.ICEBERG_REST_API_PREFIX;
  }
}
