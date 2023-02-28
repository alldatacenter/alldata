/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.qlangtech.tis.manage.biz.dal.dao.impl;

import com.qlangtech.tis.manage.biz.dal.dao.IResourceParametersDAO;
import com.qlangtech.tis.manage.biz.dal.dao.ISnapshotDAO;
import com.qlangtech.tis.manage.biz.dal.dao.ISnapshotViewDAO;
import com.qlangtech.tis.manage.biz.dal.dao.IUploadResourceDAO;
import com.qlangtech.tis.manage.biz.dal.pojo.Snapshot;
import com.qlangtech.tis.manage.biz.dal.pojo.SnapshotCriteria;
import com.qlangtech.tis.manage.biz.dal.pojo.UploadResource;
import com.qlangtech.tis.manage.common.BasicDAO;
import com.qlangtech.tis.manage.common.SnapshotDomain;
import com.qlangtech.tis.manage.common.TisUTF8;
import com.qlangtech.tis.utils.MD5Utils;
import junit.framework.Assert;
import org.apache.commons.beanutils.DynaBean;
import org.apache.commons.beanutils.LazyDynaBean;
import org.apache.commons.io.IOUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * snapshot 的视图查询dao
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2012-3-27
 */
public class SnapshotViewImplDAO extends BasicDAO<SnapshotDomain, SnapshotCriteria> implements ISnapshotViewDAO {

  public static final ThreadLocal<MergeData> mergeDataContext = new ThreadLocal<MergeData>() {
    //    @Override
//    public MergeData get() {
//      return super.get();
//    }
    @Override
    protected MergeData initialValue() {
//      System.out.println("initialValue");
      MergeData initMergeData = new MergeData();//super.initialValue();
      initMergeData.put(KEY_MIN_GRAM_SIZE, 2);
      initMergeData.put(KEY_MAX_GRAM_SIZE, 7);
      return initMergeData;
    }
  };

  private ISnapshotDAO snapshotDAO;

  private IUploadResourceDAO uploadResourceDao;

  private IResourceParametersDAO resourceParametersDAO;

  @Override
  public String getEntityName() {
    return "snapshot_view";
  }

  @Override
  public SnapshotDomain getView(Integer snId, boolean mergeContextParams) {
    // 实现懒加载
    Assert.assertNotNull("param snId ", snId);
    final Snapshot snapshot = snapshotDAO.loadFromWriteDB(snId);
    if (snapshot == null) {
      throw new IllegalArgumentException("snid:" + snId + " relevant record is not exist");
    }
    SnapshotDomain domain = new SnapshotDomain() {

      UploadResource solrConfig;
      UploadResource schema;

      @Override
      public Snapshot getSnapshot() {
        return snapshot;
      }

      @Override
      public Integer getAppId() {
        return snapshot.getAppId();
      }

      @Override
      public UploadResource getSolrConfig() {
        try {
          if (this.solrConfig == null && snapshot.getResSolrId() != null) {
            solrConfig = uploadResourceDao.loadFromWriteDB(snapshot.getResSolrId());
            if (mergeContextParams) {
              mergeSystemParameter(solrConfig);
            }
          }
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
        return solrConfig;
      }

      @Override
      public UploadResource getSolrSchema() {
        if (this.schema == null && snapshot.getResSchemaId() != null) {
          schema = uploadResourceDao.loadFromWriteDB(snapshot.getResSchemaId());
          if (mergeContextParams) {
            mergeSystemParameter(schema);
          }
        }
        return schema;
      }
    };
    return domain;
  }

  // @Override
  // public SnapshotDomain getView(Integer snId, RunEnvironment runtime) {
  // return getView(snId, runtime, true);
  // }
  public IResourceParametersDAO getResourceParametersDAO() {
    return resourceParametersDAO;
  }

  public void setResourceParametersDAO(IResourceParametersDAO resourceParametersDAO) {
    this.resourceParametersDAO = resourceParametersDAO;
  }

  private VelocityContext createContext() {
    VelocityContext velocityContext = new VelocityContext();
    DynaBean cfg = new LazyDynaBean();
    MergeData mergeData = mergeDataContext.get();
    for (Map.Entry<String, Object> entry : mergeData.entrySet()) {
      cfg.set(entry.getKey(), entry.getValue());
    }
    velocityContext.put("cfg", cfg);
//        ResourceParametersCriteria criteria = new ResourceParametersCriteria();
//        List<ResourceParameters> params = resourceParametersDAO.selectByExample(criteria, 1, 100);
//        for (ResourceParameters param : params) {
//            velocityContext.put(param.getKeyName(), GlobalConfigServlet.getParameterValue(param, runtime));
//        }
    return velocityContext;
  }

  private static final VelocityEngine velocityEngine;

  static {
    try {
      velocityEngine = new VelocityEngine();
      Properties prop = new Properties();
      prop.setProperty("runtime.log.logsystem.class", "org.apache.velocity.runtime.log.NullLogChute");
      velocityEngine.init(prop);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * 将文档内容和系统参数合并
   *
   * @param resource
   * @throws UnsupportedEncodingException
   * @throws IOException
   */
  private void mergeSystemParameter(UploadResource resource) {
    OutputStreamWriter writer = null;
    try {
      VelocityContext context = createContext();
      byte[] content = resource.getContent();
      ByteArrayOutputStream converted = new ByteArrayOutputStream();
      // StringWriter writer = new StringWriter(converted);
      writer = new OutputStreamWriter(converted, TisUTF8.get());
      // Velocity.evaluate(context, writer, resource.getResourceType(),
      // new String(content, encode));
      velocityEngine.evaluate(context, writer, resource.getResourceType(), new String(content, TisUTF8.get()));
      writer.flush();
      resource.setContent(converted.toByteArray());
      resource.setMd5Code(MD5Utils.md5file(resource.getContent()));
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      IOUtils.closeQuietly(writer);
    }
  }

  public static void main(String[] args) {
    SnapshotViewImplDAO dao = new SnapshotViewImplDAO();
    UploadResource resource = new UploadResource();
    resource.setResourceType("test");
    String content = "${cfg.minGramSize}";
    resource.setContent(content.getBytes(TisUTF8.get()));
    dao.mergeSystemParameter(resource);
    System.out.println(new String(resource.getContent(), TisUTF8.get()));
  }

  public void setUploadResourceDao(IUploadResourceDAO uploadResourceDao) {
    this.uploadResourceDao = uploadResourceDao;
  }

  // @Autowired
  public void setSnapshotDAO(ISnapshotDAO snapshotDAO) {
    this.snapshotDAO = snapshotDAO;
  }

  public static class MergeData extends HashMap<String, Object> {

  }
}
