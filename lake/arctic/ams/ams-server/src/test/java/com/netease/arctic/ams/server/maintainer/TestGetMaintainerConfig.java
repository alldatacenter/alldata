package com.netease.arctic.ams.server.maintainer;

import org.junit.Assert;
import org.junit.Test;

public class TestGetMaintainerConfig {

  @Test
  public void testGetConfig() {
    MaintainerConfig maintainerConfig = MaintainerMain.getMaintainerConfig(new String[]{
        "thrift://localhost:1260/my_catalog",
        "src/test/resources/test.yaml"
    });
    Assert.assertEquals("my_catalog", maintainerConfig.getCatalogName());
    Assert.assertEquals("thrift://localhost:1260", maintainerConfig.getThriftUrl());

    MaintainerConfig maintainerConfig2 = MaintainerMain.getMaintainerConfig(new String[]{
        "thrift://localhost:1260",
        "src/test/resources/test.yaml"
    });
    Assert.assertEquals(null, maintainerConfig2.getCatalogName());
    Assert.assertEquals("thrift://localhost:1260", maintainerConfig2.getThriftUrl());

  }

}
