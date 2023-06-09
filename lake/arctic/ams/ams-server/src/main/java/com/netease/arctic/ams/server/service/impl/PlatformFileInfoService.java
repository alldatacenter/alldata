package com.netease.arctic.ams.server.service.impl;

import com.netease.arctic.ams.server.ArcticMetaStore;
import com.netease.arctic.ams.server.config.ArcticMetaStoreConf;
import com.netease.arctic.ams.server.mapper.PlatformFileInfoMapper;
import com.netease.arctic.ams.server.model.PlatformFileInfo;
import com.netease.arctic.ams.server.service.IJDBCService;
import org.apache.ibatis.session.SqlSession;

import java.util.Base64;

public class PlatformFileInfoService extends IJDBCService {
  /**
   * add some file
   * @param name
   * @param content
   * @return
   */
  public Integer addFile(String name, String content) {
    try (SqlSession sqlSession = getSqlSession(true)) {
      PlatformFileInfoMapper platformFileInfoMapper =
              getMapper(sqlSession, PlatformFileInfoMapper.class);
      PlatformFileInfo platformFileInfo = new PlatformFileInfo(name, content);
      platformFileInfoMapper.addFile(platformFileInfo);
      if (ArcticMetaStore.conf.getString(ArcticMetaStoreConf.DB_TYPE).equals("derby")) {
        return platformFileInfoMapper.getFileId(content);
      }
      return platformFileInfo.getFileId();
    }
  }

  /**
   * get file content
   * @param fileId
   * @return
   */
  public String getFileContentB64ById(Integer fileId) {
    try (SqlSession sqlSession = getSqlSession(true)) {
      PlatformFileInfoMapper platformFileInfoMapper =
              getMapper(sqlSession, PlatformFileInfoMapper.class);
      return platformFileInfoMapper.getFileById(fileId);
    }
  }

  /**
   * get file content
   * @param fileId
   * @return
   */
  public String getFileContentById(Integer fileId) {
    try (SqlSession sqlSession = getSqlSession(true)) {
      PlatformFileInfoMapper platformFileInfoMapper =
              getMapper(sqlSession, PlatformFileInfoMapper.class);
      return new String(Base64.getDecoder().decode(platformFileInfoMapper.getFileById(fileId)));
    }
  }

}
