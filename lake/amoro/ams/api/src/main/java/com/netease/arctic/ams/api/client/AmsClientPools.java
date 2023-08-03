package com.netease.arctic.ams.api.client;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.netease.arctic.ams.api.ArcticTableMetastore;
import com.netease.arctic.ams.api.Constants;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TMultiplexedProtocol;
import org.apache.thrift.protocol.TProtocol;

/**
 * Client pool cache for different ams server, sharing in jvm.
 */
public class AmsClientPools {

  private static final int CLIENT_POOL_MIN = 0;
  private static final int CLIENT_POOL_MAX = 5;

  private static final LoadingCache<String, ThriftClientPool<ArcticTableMetastore.Client>> CLIENT_POOLS
      = Caffeine.newBuilder()
      .build(AmsClientPools::buildClientPool);

  public static ThriftClientPool<ArcticTableMetastore.Client> getClientPool(String metastoreUrl) {
    return CLIENT_POOLS.get(metastoreUrl);
  }

  public static void cleanAll() {
    CLIENT_POOLS.cleanUp();
  }

  private static ThriftClientPool<ArcticTableMetastore.Client> buildClientPool(String url) {
    PoolConfig poolConfig = new PoolConfig();
    poolConfig.setFailover(true);
    poolConfig.setMinIdle(CLIENT_POOL_MIN);
    poolConfig.setMaxIdle(CLIENT_POOL_MAX);
    return new ThriftClientPool<>(
        url,
        s -> {
          TProtocol protocol = new TBinaryProtocol(s);
          ArcticTableMetastore.Client tableMetastore = new ArcticTableMetastore.Client(
              new TMultiplexedProtocol(protocol, Constants.THRIFT_TABLE_SERVICE_NAME));
          return tableMetastore;
        },
        c -> {
          try {
            ((ArcticTableMetastore.Client) c).ping();
          } catch (TException e) {
            return false;
          }
          return true;
        }, poolConfig, Constants.THRIFT_TABLE_SERVICE_NAME);
  }
}
