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

/**
 *
 */
package com.qlangtech.tis.trigger.biz.dal.dao.impl;

import com.qlangtech.tis.trigger.biz.dal.dao.AppTrigger;
import com.qlangtech.tis.trigger.biz.dal.dao.IJobMetaDataDAO;
import com.qlangtech.tis.trigger.biz.dal.dao.JobConstant;
import com.qlangtech.tis.trigger.biz.dal.dao.TriggerJob;
import junit.framework.Assert;
import org.springframework.jdbc.core.support.JdbcDaoSupport;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * @date 2012-7-2
 */
public class JobMetaDataDAO extends JdbcDaoSupport implements IJobMetaDataDAO {
  //
  // public static final int FULL_DUMP = 1;
  // public static final int INCR_DUMP = 2;

  private static final String SQL_SELECT_APP = ""
    + " select aa.job_id,aa.job_type,aa.is_stop "
    + " from app_trigger_job_relation aa inner join application bb on (aa.app_id = bb.app_id)"
    + " where bb.project_name = ? ";

  public static final String SQL_UPDATE_SET_STOP = "update app_trigger_job_relation set is_stop = ? where project_name= ?";


  public TriggerJob queryJob(String appName, Integer jobtype) {

    // DataSource datasource = this.getDataSource();
    PreparedStatement statement = null;
    ResultSet result = null;
    TriggerJob job = null;
    Connection conn = null;

    try {
      conn = this.getConnection();

      statement = conn.prepareStatement(SQL_SELECT_APP
        + " and job_type= ?");
      statement.setString(1, appName);
      statement.setInt(2, jobtype);

      result = statement.executeQuery();

      if (result.next()) {
        job = new TriggerJob();
        job.setJobId(result.getLong(1));
        job.setJobType(result.getInt(2));
        job.setStop(JobConstant.STOPED.equals(result.getString(3)));

        return job;
      }

    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      try {
        result.close();
      } catch (Throwable e) {
      }
      try {
        statement.close();
      } catch (Throwable e) {
      }
      try {
        conn.close();
      } catch (Throwable e) {
      }
    }

    return null;
  }

  /**
   * 暂停一下
   */
  public void setStop(String appName, boolean stop) {
    PreparedStatement statement = null;
    Connection conn = null;

    try {
      conn = this.getConnection();
      statement = conn.prepareStatement(SQL_UPDATE_SET_STOP);

      statement.setString(1, stop ? JobConstant.STOPED
        : JobConstant.STOPED_NOT);
      statement.setString(2, appName);

      statement.execute();

    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      try {
        statement.close();
      } catch (Throwable e) {

      }
      try {
        conn.close();
      } catch (Throwable e) {
      }
    }
  }

  public AppTrigger queryJob(String appName) {

    PreparedStatement statement = null;
    ResultSet result = null;
    Connection conn = null;

    try {
      conn = this.getConnection();

      statement = conn.prepareStatement(SQL_SELECT_APP);
      statement.setString(1, appName);

      result = statement.executeQuery();

      TriggerJob full = null;
      TriggerJob incr = null;

      while (result.next()) {

        if (JobConstant.JOB_TYPE_FULL_DUMP == result.getInt(2)) {
          full = new TriggerJob();
          full.setJobId(result.getLong(1));
          full.setJobType(JobConstant.JOB_TYPE_FULL_DUMP);
          full
            .setStop(JobConstant.STOPED.equals(result
              .getString(3)));
        } else if (JobConstant.JOB_INCREASE_DUMP == result.getInt(2)) {
          incr = new TriggerJob();
          incr.setJobId(result.getLong(1));
          incr.setJobType(JobConstant.JOB_INCREASE_DUMP);
          incr
            .setStop(JobConstant.STOPED.equals(result
              .getString(3)));
        }

      }

      Assert.assertNotNull(full);
      Assert.assertNotNull(incr);
      return new AppTrigger(full, incr);
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      try {
        result.close();
      } catch (Throwable e) {
      }
      try {
        statement.close();
      } catch (Throwable e) {
      }
      try {
        conn.close();
      } catch (Throwable e) {
      }
    }

  }
}
