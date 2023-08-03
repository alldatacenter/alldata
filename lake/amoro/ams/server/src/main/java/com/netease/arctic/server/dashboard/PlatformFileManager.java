package com.netease.arctic.server.dashboard;

import com.netease.arctic.server.dashboard.model.PlatformFileInfo;
import com.netease.arctic.server.persistence.PersistentBase;
import com.netease.arctic.server.persistence.mapper.PlatformFileMapper;

import java.util.Base64;

public class PlatformFileManager extends PersistentBase {

  /**
   * add some file
   */
  public Integer addFile(String name, String content) {
    PlatformFileInfo platformFileInfo = new PlatformFileInfo(name, content);
    doAs(PlatformFileMapper.class, e -> e.addFile(platformFileInfo));
    return platformFileInfo.getFileId();
  }

  /**
   * getRuntime file content
   */
  public String getFileContentB64ById(Integer fileId) {
    return getAs(PlatformFileMapper.class, e -> e.getFileById(fileId));
  }

  /**
   * getRuntime file content
   */
  public String getFileContentById(Integer fileId) {
    String fileContent = getAs(PlatformFileMapper.class, e -> e.getFileById(fileId));
    return new String(Base64.getDecoder().decode(fileContent));
  }
}
