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
package com.qlangtech.tis.runtime.module.action;

import com.alibaba.citrus.turbine.Context;
import com.qlangtech.tis.common.utils.Assert;
import com.qlangtech.tis.manage.PermissionConstant;
import com.qlangtech.tis.manage.biz.dal.dao.IClusterSnapshotDAO;
import com.qlangtech.tis.manage.biz.dal.pojo.ClusterSnapshot;
import com.qlangtech.tis.manage.biz.dal.pojo.ClusterSnapshotQuery;
import com.qlangtech.tis.manage.spring.aop.Func;
import org.springframework.beans.factory.annotation.Autowired;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户查询访问提供不同时段的报表
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2018年5月31日
 */
public class ClusterStateCollectAction extends BasicModule {

    private static final long serialVersionUID = 1L;

    private IClusterSnapshotDAO clusterSnapshotDAO;

    private static final Map<Integer, IClusterSnapshotQueryGetter> queryTimeSpan = new HashMap<Integer, IClusterSnapshotQueryGetter>();

    public static final ThreadLocal<SimpleDateFormat> dailyFormat = new ThreadLocal<SimpleDateFormat>() {

        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("HH:mm");
        }
    };

    public static final ThreadLocal<SimpleDateFormat> dateFormat = new ThreadLocal<SimpleDateFormat>() {

        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("MM/dd");
        }
    };

    private static final int ONE_HOUR = 60;

    public static final int TODAY = 1440;

    static {
        queryTimeSpan.put(ONE_HOUR, new IClusterSnapshotQueryGetter() {

            @Override
            public ClusterSnapshotQuery create() {
                return ClusterSnapshotQuery.hour();
            }

            @Override
            public ThreadLocal<SimpleDateFormat> getDateFormat() {
                return dailyFormat;
            }
        });
        queryTimeSpan.put(300, new IClusterSnapshotQueryGetter() {

            @Override
            public ClusterSnapshotQuery create() {
                return ClusterSnapshotQuery.fiveHour();
            }

            @Override
            public ThreadLocal<SimpleDateFormat> getDateFormat() {
                return dailyFormat;
            }
        });
        queryTimeSpan.put(TODAY, new IClusterSnapshotQueryGetter() {

            @Override
            public ClusterSnapshotQuery create() {
                return ClusterSnapshotQuery.hour24();
            }

            @Override
            public ThreadLocal<SimpleDateFormat> getDateFormat() {
                return dailyFormat;
            }
        });
        queryTimeSpan.put(7200, new IClusterSnapshotQueryGetter() {

            @Override
            public ClusterSnapshotQuery create() {
                return ClusterSnapshotQuery.days15();
            }

            @Override
            public ThreadLocal<SimpleDateFormat> getDateFormat() {
                return dateFormat;
            }
        });
        queryTimeSpan.put(43200, new IClusterSnapshotQueryGetter() {

            @Override
            public ClusterSnapshotQuery create() {
                return ClusterSnapshotQuery.last1Month();
            }

            @Override
            public ThreadLocal<SimpleDateFormat> getDateFormat() {
                return dateFormat;
            }
        });
    }

    /**
     * 收集集群状态信息
     *
     * @param context
     * @throws Exception
     */
    @Func(value = PermissionConstant.PERMISSION_CLUSTER_STATE_COLLECT, sideEffect = false)
    public void doCollect(Context context) throws Exception {
        Integer minute = this.getInt("m");
        Assert.assertNotNull(minute);
        final StatusCollectStrategy collectStrategy = getCollectStrategy(this.clusterSnapshotDAO, getAppDomain().getAppid(), minute);
        this.setBizResult(context, collectStrategy.getSnapshots());
    }

    public static StatusCollectStrategy getCollectStrategy(IClusterSnapshotDAO clusterSnapshotDAO, Integer appId, final int minute) {
        return new StatusCollectStrategy() {

            private List<ClusterSnapshot> snapshots;

            @Override
            public List<ClusterSnapshot> getSnapshots() {
                if (snapshots == null) {
                    IClusterSnapshotQueryGetter getter = queryTimeSpan.get(minute);
                    Assert.assertNotNull("time getter:" + minute + " can not be null", getter);
                    final ClusterSnapshotQuery timeSpanQuery = getter.create();
                    Assert.assertNotNull("time span:" + minute + " can not be null", timeSpanQuery);
                    timeSpanQuery.setAppId(appId);
                    List<ClusterSnapshot> snapshots = clusterSnapshotDAO.reportClusterStatus(timeSpanQuery);
                    snapshots.forEach((r) -> r.dateformat = getter.getDateFormat());
                    this.snapshots = snapshots;
                }
                return snapshots;
            }

            @Override
            public ClusterSnapshot.Summary getMetricSummary() {
                ClusterSnapshot.Summary summary = new ClusterSnapshot.Summary();
                this.getSnapshots().forEach((snapshot) -> {
                    summary.add(snapshot);
                });
                return summary;
            }
        };
    }

    public interface StatusCollectStrategy {

        List<ClusterSnapshot> getSnapshots();

        ClusterSnapshot.Summary getMetricSummary();
    }

    public static class PonitStatus {

        private String createTime;

        private String serviceName;

        private int qps;

        private long requestCount;

        private long docNumber;

        private float avgConsumeTimePerRequest;

        public float getAvgConsumeTimePerRequest() {
            return avgConsumeTimePerRequest;
        }

        public String getCreateTime() {
            return createTime;
        }

        public String getServiceName() {
            return serviceName;
        }

        public int getQps() {
            return qps;
        }

        public long getRequestCount() {
            return requestCount;
        }

        public long getDocNumber() {
            return docNumber;
        }
    }

    interface IClusterSnapshotQueryGetter {

        ClusterSnapshotQuery create();

        ThreadLocal<SimpleDateFormat> getDateFormat();
    }

    public IClusterSnapshotDAO getClusterSnapshotDAO() {
        return clusterSnapshotDAO;
    }

    @Autowired
    public void setClusterSnapshotDAO(IClusterSnapshotDAO clusterSnapshotDAO) {
        this.clusterSnapshotDAO = clusterSnapshotDAO;
    }
}
