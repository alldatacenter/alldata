package com.netease.arctic.ams.api.client;

import org.apache.curator.utils.ZookeeperFactory;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.admin.ZooKeeperAdmin;
import org.apache.zookeeper.client.ZKClientConfig;

public class ArcticZookeeperFactory implements ZookeeperFactory {

  private final ZKClientConfig config;

  public ArcticZookeeperFactory(ZKClientConfig config) {
    this.config = config;
  }

  @Override
  public ZooKeeper newZooKeeper(String connectString, int sessionTimeout, Watcher watcher, boolean canBeReadOnly)
      throws Exception {
    return new ZooKeeperAdmin(connectString, sessionTimeout, watcher, canBeReadOnly, config);
  }
}
