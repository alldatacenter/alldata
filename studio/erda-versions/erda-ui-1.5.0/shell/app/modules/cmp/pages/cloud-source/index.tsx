// Copyright (c) 2021 Terminus, Inc.
//
// This program is free software: you can use, redistribute, and/or modify
// it under the terms of the GNU Affero General Public License, version 3
// or later ("AGPL"), as published by the Free Software Foundation.
//
// This program is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// FITNESS FOR A PARTICULAR PURPOSE.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program. If not, see <http://www.gnu.org/licenses/>.

import React from 'react';
import { Spin, Card } from 'antd';
import i18n from 'i18n';
import { getFormatter } from 'charts/utils/formatter';
import cloudSourceStore from 'cmp/stores/cloud-source';
import { useEffectOnce } from 'react-use';
import { get, map, values, isEmpty, merge } from 'lodash';
import { BoardGrid } from 'common';
import { goTo } from 'common/utils';
import { colorMap } from '@erda-ui/dashboard-configurator';
import cloudAccountStore from 'cmp/stores/cloud-account';
import Guidance from 'cmp/pages/account-guidance';
import ts_svg from 'app/images/ts.svg';
import { useLoading } from 'core/stores/loading';

import './index.scss';

const CloudSource = () => {
  const [overviewData, ecsTrendingData] = cloudSourceStore.useStore((s) => [s.overviewData, s.ecsTrendingData]);
  const { getOverview, getECSTrending } = cloudSourceStore.effects;
  const [getECSTrendingLoading, getOverviewLoading] = useLoading(cloudSourceStore, ['getECSTrending', 'getOverview']);
  const { getList } = cloudAccountStore.effects;
  const accountsCount = cloudAccountStore.useStore((s) => s.list.length);

  const getResourceInfo = () => {
    getList({ pageNo: 1, pageSize: 15 }).then((accountList: IPagingResp<CLOUD_ACCOUNTS.Account>) => {
      if (accountList.total > 0) {
        getOverview();
        getECSTrending();
      }
    });
  };
  useEffectOnce(getResourceInfo);

  const parseExpireData = React.useCallback(() => {
    const metricData: any[] = [];
    const statusCountMap = {};
    const xData: string[] = [];
    let expireDays = 0;
    let total = 0;
    map(overviewData, (category) => {
      map(category.resourceTypeData, (resource: any, type: string) => {
        if (resource.expireDays) {
          // 都一样，随便取一个
          expireDays = resource.expireDays;
        }
        if (resource.statusCount) {
          xData.push(type);
          map(resource.statusCount, (v) => {
            if (v.label === 'BeforeExpired') {
              statusCountMap[v.status] = statusCountMap[v.status] || [];
              statusCountMap[v.status].push(v.count);
              total += v.count;
            }
          });
        }
      });
    });
    map(statusCountMap, (data, status) => {
      metricData.push({
        name: status,
        barGap: 0,
        data,
      });
    });
    return {
      xData,
      metricData,
      total,
      expireDays,
    };
  }, [overviewData]);

  const parseStatusData = React.useCallback(() => {
    const metricData: any[] = [];
    const statusCountMap = {};
    const xData: string[] = [];
    map(get(overviewData, 'CLOUD_SERVICE.resourceTypeData'), (resource: any, type: string) => {
      if (resource.statusCount) {
        xData.push(type);
        map(resource.statusCount, (v) => {
          statusCountMap[v.status] = statusCountMap[v.status] || [];
          statusCountMap[v.status].push(v.count);
        });
      }
    });
    map(statusCountMap, (data, status) => {
      metricData.push({
        name: status,
        barGap: 0,
        data,
      });
    });
    return {
      xData,
      metricData,
    };
  }, [overviewData]);

  const getPieStaticData = (
    list: Array<{ status?: string; chargeType?: string; count: number }>,
    key: string,
    formatter?: string,
  ) => {
    let total = 0;
    const metricData = [
      {
        name: i18n.t('publisher:proportion'),
        radius: ['50%', '70%'],
        center: ['50%', '42%'],
        label: {
          show: false,
          formatter: formatter || '{b}\n{c}',
          fontSize: '20',
          position: 'center',
        },
        emphasis: {
          label: {
            show: true,
            fontWeight: 'bold',
          },
        },
        data: map(list, (item) => {
          total += item.count;
          return { name: item[key], value: item.count };
        }),
      },
    ];
    const legendData = map(list, (item) => item[key]);
    return {
      metricData,
      legendData,
      total,
    };
  };

  const getECSTrendingData = (data: any) => {
    if (isEmpty(data) || isEmpty(data.results)) {
      return {};
    }
    return {
      time: data.time,
      metricData: map(data.results[0].data, (item) => values(item)[0]),
    };
  };

  const getLayout = React.useCallback(() => {
    const commonConfig = {
      option: {
        tooltip: {
          formatter: '{a}<br/>{b}: {d}%',
        },
        legend: {
          orient: 'horizontal',
          y: 'bottom',
          x: 'center',
          type: 'scroll',
        },
      },
    };
    const statusData = getPieStaticData(get(overviewData, 'COMPUTE.resourceTypeData.ECS.statusCount'), 'status');
    const chargeData = getPieStaticData(
      get(overviewData, 'COMPUTE.resourceTypeData.ECS.chargeTypeCount'),
      'chargeType',
    );
    const labelData = getPieStaticData(get(overviewData, 'NETWORK.resourceTypeData.VPC.labelCount'), 'label', '{c}');
    const expireData = parseExpireData();
    return [
      {
        w: 8,
        h: 8,
        x: 0,
        y: 0,
        i: 'serverStatus',
        moved: false,
        static: false,
        view: {
          title: i18n.t('cmp:ECS status distribution'),
          chartType: 'chart:pie',
          hideReload: true,
          staticData: statusData.total ? statusData : [],
          config: merge({}, commonConfig, {
            option: {
              color: [colorMap.appleGreen, colorMap.yellow, colorMap.pink, colorMap.gray],
            },
          }),
          chartProps: {
            onEvents: {
              click: () => {
                goTo(goTo.pages.cloudSourceEcs);
              },
            },
          },
        },
      },
      {
        w: 8,
        h: 8,
        x: 8,
        y: 0,
        i: 'chargeType',
        moved: false,
        static: false,
        view: {
          title: i18n.t('cmp:ESC billing method'),
          chartType: 'chart:pie',
          hideReload: true,
          staticData: chargeData.total ? chargeData : [],
          config: merge({}, commonConfig, {
            option: {
              color: [colorMap.appleGreen, colorMap.darkGreen],
            },
          }),
          chartProps: {
            onEvents: {
              click: () => {
                goTo(goTo.pages.cloudSourceEcs);
              },
            },
          },
        },
      },
      {
        w: 8,
        h: 8,
        x: 16,
        y: 0,
        i: 'vpcLabel',
        moved: false,
        static: false,
        view: {
          title: i18n.t('cmp:VPC Label'),
          chartType: 'chart:pie',
          hideReload: true,
          staticData: labelData.total ? labelData : [],
          config: commonConfig,
          chartProps: {
            onEvents: {
              click: () => {
                goTo(goTo.pages.cloudSourceVpc);
              },
            },
          },
        },
      },
      {
        w: 16,
        h: 8,
        x: 0,
        y: 10,
        i: 'will expire resource',
        moved: false,
        static: false,
        view: {
          title: i18n.t('cmp:resource expire soon'),
          hideReload: true,
          chartType: 'chart:bar',
          customRender: !expireData.total
            ? () => {
                return (
                  <div className="no-expire-tip">
                    <img src={ts_svg} alt="no-will-expire-resource" />
                    <div className="text-sub">
                      {i18n.t('cmp:No service expire within {num} days.', {
                        num: expireData.expireDays,
                      })}
                    </div>
                  </div>
                );
              }
            : undefined,
          staticData: parseExpireData(),
          chartProps: {
            onEvents: {
              click: (params: any) => {
                switch (params.name || params.value) {
                  case 'REDIS':
                    goTo(goTo.pages.cloudSourceRedis);
                    break;
                  case 'RDS':
                    goTo(goTo.pages.cloudSourceRds);
                    break;
                  case 'ROCKET_MQ':
                    goTo(goTo.pages.cloudSourceMq);
                    break;
                  case 'ECS':
                    goTo(goTo.pages.cloudSourceEcs);
                    break;

                  default:
                    break;
                }
              },
            },
          },
          config: {
            option: {
              color: [colorMap.appleGreen, colorMap.yellow, colorMap.pink, colorMap.gray],
              xAxis: [
                {
                  triggerEvent: true,
                },
              ],
              yAxis: [
                {
                  name: i18n.t('cmp:number'),
                  nameLocation: 'end',
                  nameGap: 15,
                  minInterval: 1,
                  nameTextStyle: {
                    padding: [0, 0, 0, 0],
                  },
                },
              ],
            },
          },
        },
      },
      {
        w: 8,
        h: 4,
        x: 17,
        y: 20,
        i: 'OSS',
        moved: false,
        static: false,
        view: {
          title: i18n.t('cmp:OSS Overview'),
          hideReload: true,
          customRender: () => {
            const bucket = get(overviewData, 'STORAGE.resourceTypeData.OSS_BUCKET') || {};
            return (
              <div className="cloud-count-card">
                <div
                  className="part hover-active"
                  onClick={() => {
                    goTo(goTo.pages.cloudSourceOss);
                  }}
                >
                  <div className="count">{bucket.totalCount || 0}</div>
                  <div className="name">{i18n.t('cmp:number of Bucket')}</div>
                </div>
                {
                  // ref issue: 59066
                }
                {/* <div className='part hover-active' onClick={() => { goTo(goTo.pages.cloudSourceOss); }}>
                  <div className="count">
                    {getFormatter('STORAGE', 'B').format(bucket.storageUsage || 0)}
                  </div>
                  <div className="name">
                    {i18n.t('cmp:total storage capacity')}
                  </div>
                </div> */}
              </div>
            );
          },
        },
      },
      {
        w: 8,
        h: 4,
        x: 17,
        y: 20,
        i: 'Account',
        moved: false,
        static: false,
        view: {
          title: i18n.t('cmp:Cloud Account'),
          hideReload: true,
          customRender: () => {
            return (
              <div className="cloud-count-card">
                <div
                  className="part hover-active"
                  onClick={() => {
                    goTo(goTo.pages.cloudAccounts);
                  }}
                >
                  <div className="count">{accountsCount || 0}</div>
                  <div className="name">{i18n.t('cmp:number of account')}</div>
                </div>
              </div>
            );
          },
        },
      },
      {
        w: 24,
        h: 8,
        x: 0,
        y: 30,
        i: 'cloud resource overview',
        moved: false,
        static: false,
        view: {
          title: i18n.t('cmp:cloud resource overview'),
          chartType: 'chart:bar',
          hideReload: true,
          staticData: parseStatusData(),
          chartProps: {
            onEvents: {
              click: (params: any) => {
                switch (params.name || params.value) {
                  case 'REDIS':
                    goTo(goTo.pages.cloudSourceRedis);
                    break;
                  case 'RDS':
                    goTo(goTo.pages.cloudSourceRds);
                    break;
                  case 'ROCKET_MQ':
                    goTo(goTo.pages.cloudSourceMq);
                    break;
                  case 'ECS':
                    goTo(goTo.pages.cloudSourceEcs);
                    break;

                  default:
                    break;
                }
              },
            },
          },
          config: {
            option: {
              color: [colorMap.appleGreen, colorMap.yellow, colorMap.pink, colorMap.gray],
              xAxis: [
                {
                  triggerEvent: true,
                },
              ],
              yAxis: [
                {
                  name: i18n.t('cmp:number'),
                  nameLocation: 'end',
                  nameGap: 15,
                  minInterval: 1,
                  nameTextStyle: {
                    padding: [0, 0, 0, 0],
                  },
                },
              ],
            },
          },
        },
      },
      {
        w: 24,
        h: 8,
        x: 0,
        y: 40,
        i: 'ecs-add-trending',
        moved: false,
        static: false,
        view: {
          title: i18n.t('cmp:ecs add trending'),
          chartType: 'chart:area',
          hideReload: true,
          staticData: getECSTrendingData(ecsTrendingData),
          maskMsg: getECSTrendingLoading ? i18n.t('charts:loading') : undefined,
          config: {
            optionProps: {
              isMoreThanOneDay: true,
              moreThanOneDayFormat: 'MMM',
            },
            option: {
              yAxis: [
                {
                  name: i18n.t('cmp:number'),
                  nameLocation: 'end',
                  nameGap: 15,
                  minInterval: 1,
                  nameTextStyle: {
                    padding: [0, 0, 0, 0],
                  },
                },
              ],
            },
          },
        },
      },
    ];
  }, [overviewData, parseExpireData, parseStatusData, ecsTrendingData, getECSTrendingLoading, accountsCount]);

  if (accountsCount > 0) {
    return (
      <div className="cloud-source full-spin-height">
        <Spin spinning={getOverviewLoading}>
          <BoardGrid.Pure layout={getLayout()} />
        </Spin>
      </div>
    );
  }
  return (
    <Card className="h-full flex flex-wrap justify-center items-center">
      <Guidance afterSubmit={getResourceInfo} />
    </Card>
  );
};

export default CloudSource;
