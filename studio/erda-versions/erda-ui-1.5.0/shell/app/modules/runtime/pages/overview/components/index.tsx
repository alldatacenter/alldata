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
import { map, isEmpty } from 'lodash';
import { Row, Col, Tooltip, Button } from 'antd';
import { Responsive, ErrorBoundary, IF, ErdaIcon } from 'common';
import { goTo, getLS, setLS, qs } from 'common/utils';
import { useMediaLt } from 'common/use-hooks';
import { getMSFrontPathByKey } from 'msp/config';
import ServiceCard from './service-card';
import AddonCard from './addon-card';
import Info from './info';
import ActivityLog from './activity';
import i18n from 'i18n';
import routeInfoStore from 'core/stores/route';
import runtimeStore from 'app/modules/runtime/stores/runtime';
import PipelineLog from 'application/pages/build-detail/pipeline-log';
import './index.scss';

const RuntimeOverView = () => {
  const params = routeInfoStore.useStore((s) => s.params);
  const [runtimeDetail, addons] = runtimeStore.useStore((s) => [s.runtimeDetail, s.addons]);
  const services = {};
  const endpoints = {};
  map(runtimeDetail.services, (item, name) => {
    if (item.expose === null || item.expose?.length === 0) {
      services[name] = item;
    } else {
      endpoints[name] = item;
    }
  });

  const getAddonCardProps = (addon: ADDON.Instance) => {
    const {
      addonName,
      status,
      instanceId,
      realInstanceId,
      projectId,
      workspace,
      cluster,
      consoleUrl,
      platformServiceType,
    } = addon;
    let onClick;
    const className = 'with-link';
    const prop: any = {};
    // 0:中间件  1:微服务  2：通用平台
    if (addonName === 'terminus-roost') {
      // roost的platformServiceType是 1，特殊处理
      onClick = () => window.open(`${window.location.protocol}//${consoleUrl}`);
      return { onClick, className };
    }
    switch (platformServiceType) {
      case 0:
      case 2:
        if (instanceId && status === 'ATTACHED') {
          switch (addonName) {
            case 'jvm-profiler':
              onClick = () =>
                goTo(goTo.pages.jvmProfiler, {
                  jumpOut: true,
                  projectId,
                  instanceId,
                });
              break;
            default:
              onClick = () =>
                goTo(goTo.pages.addonPlatformOverview, {
                  jumpOut: true,
                  projectId,
                  instanceId,
                });
              break;
          }
        }
        break;
      case 1: {
        if (addonName === 'log-analytics') {
          // 3.20把日志分析移到了微服务下面，兼容旧的日志分析实例
          let tenantGroup = '';
          try {
            tenantGroup = JSON.parse(consoleUrl || '{}').tenantGroup;
          } catch (error) {
            // eslint-disable-next-line no-console
            console.error('parse consoleUrl error:', error);
            return;
          }
          // 旧的日志分析，从同项目同环境的monitor 里取consoleUrl拼接参数
          if (!tenantGroup) {
            const samePrjMonitor = addons.find((a) => a.addonName === 'monitor');
            if (samePrjMonitor) {
              try {
                tenantGroup = JSON.parse(samePrjMonitor.consoleUrl || '{}').tenantGroup;
              } catch (error) {
                // eslint-disable-next-line no-console
                console.error('parse monitor consoleUrl error:', error);
                return;
              }
            }
          }
          const pathParams = {
            projectId,
            env: workspace,
            tenantGroup,
            terminusKey: '', // 这个菜单不需要
            tenantId: realInstanceId,
            logKey: realInstanceId,
          };
          const frontPath = getMSFrontPathByKey('LogAnalyze', pathParams);
          onClick = () =>
            goTo(frontPath, { jumpOut: true, query: { query: `tags.application_id: "${params.appId}"` } });
        } else {
          if (!consoleUrl) {
            return {};
          }
          if (consoleUrl.startsWith('http')) {
            onClick = () => goTo(consoleUrl, { jumpOut: true });
          }
          const { tenantGroup, key, terminusKey, tenantId, logKey } = JSON.parse(consoleUrl);
          const pathParams = {
            projectId: String(projectId),
            env: workspace,
            tenantGroup,
            terminusKey,
            tenantId,
            logKey,
          };
          const queryObj = {
            appId: params.appId,
            runtimeId: params.runtimeId,
            az: cluster,
          };
          const frontPath = getMSFrontPathByKey(key, pathParams);
          const href = `${frontPath}?${qs.stringify(queryObj)}`;
          // 大礼包拿不到terminusKey，跳过去后从menu里取
          if (addonName === 'micro-service') {
            onClick = () => goTo(goTo.pages.mspOverview, { jumpOut: true, ...pathParams, ...queryObj });
          } else {
            onClick = () => goTo(href, { jumpOut: true });
          }
        }
        break;
      }
      default:
    }
    if (onClick) {
      prop.onClick = onClick;
      prop.className = className;
    }
    return prop;
  };

  let isFoldActivity = getLS('fold-activity');
  if (typeof isFoldActivity === 'object') {
    // 未定义时得到的是空对象
    setLS('fold-activity', false);
    isFoldActivity = false;
  }
  const [proportion, setProportion] = React.useState(isFoldActivity ? [24, 0] : [16, 8]);
  const toggleFold = (fold: boolean) => {
    setLS('fold-activity', fold);
    if (fold) {
      setProportion([24, 0]);
    } else {
      setProportion([16, 8]);
    }
  };
  const lg960 = useMediaLt(960, true);
  React.useEffect(() => {
    toggleFold(lg960);
  }, [lg960]);

  return (
    <div className="runtime-overview">
      <Info />
      <Row gutter={20} style={{ margin: 'unset' }}>
        <Col span={proportion[0]} style={{ paddingLeft: 'unset' }}>
          <ErrorBoundary>
            <IF check={!isEmpty(endpoints)}>
              <div className="overview-body-block">
                <div className="overview-body-title">{i18n.t('runtime:endpoint')}</div>
                {map(endpoints, (service, key) => {
                  return (
                    <ServiceCard
                      runtimeDetail={runtimeDetail}
                      service={service}
                      name={key}
                      key={key}
                      params={params}
                      isEndpoint
                    />
                  );
                })}
              </div>
            </IF>
            <IF check={!isEmpty(services)}>
              <div className="overview-body-block">
                <div className="overview-body-title">{i18n.t('microService')}</div>
                {map(services, (service, key) => {
                  return (
                    <ServiceCard runtimeDetail={runtimeDetail} service={service} name={key} key={key} params={params} />
                  );
                })}
              </div>
            </IF>
            <IF check={!isEmpty(addons)}>
              <div className="overview-body-block">
                <div className="overview-body-title">{i18n.t('runtime:service plugin')}</div>
                <Responsive itemWidth={260} percent={proportion[0] === 24 ? 1 : 0.66}>
                  {
                    map(addons, (item) => {
                      const { onClick, className } = getAddonCardProps(item);
                      return <AddonCard className={className} onClick={onClick} key={item.instanceId} {...item} />;
                    }) as Array<{ key: string }> | React.ReactChild
                  }
                </Responsive>
              </div>
            </IF>
            <PipelineLog
              className="runtime-deploy-logs"
              resourceId={params.runtimeId}
              resourceType="runtime"
              isBuilding
            />{' '}
            {/* runtime需要实时轮询 */}
          </ErrorBoundary>
        </Col>
        <Col span={proportion[1]} style={{ paddingRight: 'unset' }}>
          <div className="overview-body-title">
            <span className="align-middle">{i18n.t('runtime:activity')}</span>
            <Tooltip title={i18n.t('runtime:folding')}>
              <Button
                size="small"
                className="ml-1"
                shape="circle"
                icon={<ErdaIcon type="menu-unfold" size="14" />}
                onClick={() => toggleFold(true)}
              />
            </Tooltip>
          </div>
          <ErrorBoundary>
            <ActivityLog />
          </ErrorBoundary>
        </Col>
      </Row>
      <IF check={proportion[1] === 0}>
        <span className="open-activity" onClick={() => setProportion([16, 8])}>
          <Tooltip title={i18n.t('runtime:expanding activities')}>
            <Button
              size="small"
              shape="circle"
              icon={<ErdaIcon type="menu-unfold" size="14" />}
              onClick={() => toggleFold(false)}
            />
          </Tooltip>
        </span>
      </IF>
    </div>
  );
};

export default RuntimeOverView;
