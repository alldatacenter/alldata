package com.netease.arctic.server.catalog;

import com.google.common.base.Preconditions;
import com.netease.arctic.ams.api.CatalogMeta;
import com.netease.arctic.ams.api.TableFormat;
import com.netease.arctic.ams.api.properties.CatalogMetaProperties;
import com.netease.arctic.server.utils.Configurations;
import com.netease.arctic.utils.CatalogUtil;
import org.apache.iceberg.CatalogProperties;

import java.util.Set;

import static com.netease.arctic.ams.api.properties.CatalogMetaProperties.CATALOG_TYPE_AMS;
import static com.netease.arctic.ams.api.properties.CatalogMetaProperties.CATALOG_TYPE_CUSTOM;
import static com.netease.arctic.ams.api.properties.CatalogMetaProperties.CATALOG_TYPE_HADOOP;
import static com.netease.arctic.ams.api.properties.CatalogMetaProperties.CATALOG_TYPE_HIVE;

public class CatalogBuilder {

  //TODO: use internal or external concepts
  public static ServerCatalog buildServerCatalog(CatalogMeta catalogMeta, Configurations serverConfiguration) {
    String type = catalogMeta.getCatalogType();
    Set<TableFormat> tableFormats = CatalogUtil.tableFormats(catalogMeta);
    TableFormat tableFormat = tableFormats.iterator().next();

    switch (type) {
      case CATALOG_TYPE_HADOOP:
        Preconditions.checkArgument(tableFormat.equals(TableFormat.ICEBERG),
            "Hadoop catalog support iceberg table only.");
        if (catalogMeta.getCatalogProperties().containsKey(CatalogMetaProperties.TABLE_FORMATS)) {
          return new IcebergCatalogImpl(catalogMeta);
        } else {
          // Compatibility with older versions
          return new MixedCatalogImpl(catalogMeta);
        }
      case CATALOG_TYPE_HIVE:
        if (tableFormat.equals(TableFormat.ICEBERG)) {
          return new IcebergCatalogImpl(catalogMeta);
        } else if (tableFormat.equals(TableFormat.MIXED_HIVE)) {
          return new MixedHiveCatalogImpl(catalogMeta);
        } else {
          throw new IllegalArgumentException("Hive Catalog support iceberg table and mixed hive table only");
        }
      case CATALOG_TYPE_AMS:
        if (tableFormat.equals(TableFormat.MIXED_ICEBERG)) {
          return new MixedCatalogImpl(catalogMeta);
        } else if (tableFormat.equals(TableFormat.ICEBERG)) {
          return new InternalIcebergCatalogImpl(catalogMeta, serverConfiguration);
        } else {
          throw new IllegalStateException("AMS catalog support iceberg/mixed-iceberg table only.");
        }
      case CATALOG_TYPE_CUSTOM:
        Preconditions.checkArgument(tableFormat.equals(TableFormat.ICEBERG),
            "Custom catalog support iceberg table only.");
        Preconditions.checkArgument(catalogMeta.getCatalogProperties().containsKey(CatalogProperties.CATALOG_IMPL),
            "Custom catalog properties must contains " + CatalogProperties.CATALOG_IMPL);
        return new IcebergCatalogImpl(catalogMeta);
      default:
        throw new IllegalStateException("unsupported catalog type:" + type);
    }
  }
}
