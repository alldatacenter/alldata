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
import i18n from 'i18n';
import { Tooltip, Badge, Button } from 'antd';
import { get, isEmpty } from 'lodash';
import { Icon as CustomIcon, CardsLayout, Holder, IF, ErdaIcon } from 'common';
import { useUpdate } from 'common/use-hooks';
import { goTo, ossImg } from 'common/utils';
import { AddonDetailDrawer } from './addon-detail-drawer';
import { PLAN_NAME, ENV_NAME } from '../configs';
import { getMSFrontPathByKey } from 'app/modules/msp/config';
import CreateLog from './create-log';
import addon_png from 'app/images/resources/addon.png';
import './addon-cards.scss';
import addonStore from 'common/stores/addon';
import CustomAddonConfigModal from 'project/pages/addon/custom-config';
import routeInfoStore from 'core/stores/route';

interface IProps {
  dataSource?: any[];
  width?: number;
  categoryRefs?: any[];
  addonDetail?: any;
  addonReferences?: any[];
  isPlatform?: boolean;
  forwardedRef?: any;
  onEitAddon?: (addon: ADDON.Instance) => void;
}

export const AddonCards = (props: IProps) => {
  const { dataSource, categoryRefs, onEitAddon } = props;
  const [{ drawerVisible, modalVisible, recordId, logoUrlMap, prjMonitorMap, customConfigVisible }, updater] =
    useUpdate({
      drawerVisible: false,
      modalVisible: false,
      recordId: '',
      logoUrlMap: {}, // 因为有条件渲染，不能放到每个CartRender内部管理
      prjMonitorMap: {},
      customConfigVisible: false,
    });
  const [addonDetail, addonReferences] = addonStore.useStore((s) => [s.addonDetail, s.addonReferences]);
  const routeParams = routeInfoStore.useStore((s) => s.params);

  React.useEffect(() => {
    if (dataSource) {
      const urlMap = {};
      if (!isEmpty(categoryRefs)) {
        dataSource.forEach((contents: any) => {
          (contents[1] as ADDON.Instance[]).forEach((item) => {
            urlMap[item.instanceId] = ossImg(item.logoUrl, { w: 40 });
            prjMonitorMap[`${item.projectId}-${item.workspace}`] = item;
          });
        });
      } else {
        dataSource.forEach((item: ADDON.Instance) => {
          urlMap[item.instanceId] = ossImg(item.logoUrl, { w: 40 });
          prjMonitorMap[`${item.projectId}-${item.workspace}`] = item;
        });
      }
      updater.logoUrlMap(urlMap);
    }
  }, [categoryRefs, dataSource, prjMonitorMap, updater]);

  const closeDrawer = () => {
    addonStore.clearAddonDetail();
    updater.drawerVisible(false);
  };

  const openDrawer = () => {
    updater.drawerVisible(true);
  };

  const onClickCard = (content: ADDON.Instance) => {
    const jumpOut = !props.isPlatform;
    const { addonName, projectId, workspace, instanceId, platformServiceType, consoleUrl } = content;
    if (addonName === 'terminus-roost') {
      // roost的platformServiceType是 1，特殊处理
      window.open(`${window.location.protocol}//${consoleUrl}`);
      return;
    }
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
        const samePrjMonitor = prjMonitorMap[`${projectId}-${workspace}`];
        if (samePrjMonitor) {
          try {
            tenantGroup = JSON.parse(samePrjMonitor.consoleUrl).tenantGroup;
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
        tenantId: content.realInstanceId,
        logKey: content.realInstanceId,
      };
      const frontPath = getMSFrontPathByKey('LogAnalyze', pathParams);
      goTo(frontPath, { jumpOut });
      return;
    }
    // 0:中间件  1:微服务  2：通用平台
    switch (platformServiceType) {
      case 0:
      case 2:
        if (instanceId) {
          switch (addonName) {
            case 'jvm-profiler':
              goTo(goTo.pages.jvmProfiler, {
                jumpOut: true,
                projectId,
                instanceId,
              });
              break;
            default:
              goTo(goTo.pages.addonPlatformOverview, {
                jumpOut: true,
                projectId,
                instanceId,
              });
          }
        }
        return;
      case 1: {
        if (!consoleUrl) {
          return;
        }
        if (consoleUrl.startsWith('http')) {
          goTo(consoleUrl, { jumpOut: true });
          return;
        }
        const { tenantGroup, key, tenantId, terminusKey, logKey } = JSON.parse(consoleUrl);
        const pathParams = { projectId, env: workspace, tenantGroup, terminusKey, tenantId, logKey };
        const frontPath = getMSFrontPathByKey(key, pathParams);
        // 大礼包拿不到terminusKey，跳过去后从menu里取
        if (addonName === 'micro-service') {
          goTo(goTo.pages.mspOverview, { jumpOut: true, ...pathParams });
        }
        goTo(frontPath, { jumpOut });
        break;
      }
      default:
    }
  };

  const CardRender = (content: ADDON.Instance) => {
    const {
      name,
      plan,
      version,
      category,
      projectName,
      reference,
      workspace,
      instanceId,
      realInstanceId,
      customAddonType,
      projectId,
      status,
      mysqlAccountState,
      tag,
    } = content;
    const onError = () => {
      updater.logoUrlMap((prev: any) => ({ ...prev, [instanceId]: addon_png }));
    };

    const mysqlAccountStateMap = {
      PRE: i18n.t('dop:account switching'),
    };
    return (
      <div
        className="addon-item-container cursor-pointer"
        key={`${instanceId || realInstanceId}-${projectId}-${workspace}`}
        onClick={() => onClickCard(content)}
      >
        <IF check={mysqlAccountStateMap[mysqlAccountState]}>
          <Tooltip title={mysqlAccountStateMap[mysqlAccountState]}>
            <ErdaIcon className="absolute right-12 top-1" type="caution" color="orange" size={16} />
          </Tooltip>
        </IF>
        <span className="env-icon font-medium" data-env={ENV_NAME[workspace].slice(0, 1).toUpperCase()}>
          <CustomIcon type="bq" color />
        </span>
        <div className="addon-item">
          <div className="addon-item-main">
            <div className="icon-wrap">
              <img src={logoUrlMap[instanceId]} style={{ width: '40px' }} alt="addon-logo" onError={onError} />
            </div>
            <div className="addon-item-info">
              <div className="nowrap">
                <IF check={status === 'ATTACHING' || status === 'ATTACHFAILED'}>
                  <Badge status={status === 'ATTACHING' ? 'processing' : 'error'} />
                </IF>
                <Tooltip title={name}>
                  <span className="title font-medium">
                    {name}
                    {tag ? `（${tag}）` : null}
                  </span>
                </Tooltip>
              </div>
              <div className="footer">
                <span>{PLAN_NAME[plan]}</span>
                <span>{version}</span>
                <IF check={customAddonType === 'cloud'}>
                  <CustomIcon className="ml-2" type="cloud" />
                </IF>
              </div>
            </div>
          </div>
          <div className="addon-item-footer flex justify-between items-center">
            <div className="info flex justify-between items-center">
              <Tooltip title={projectName}>
                <span className="nowrap project-name">
                  <CustomIcon type="overview" />
                  {projectName}
                </span>
              </Tooltip>
              <span className="nowrap">
                <CustomIcon type="js1" />
                {reference}
              </span>
            </div>
            <IF check={!props.isPlatform}>
              <div className="btn-group">
                <IF check={status === 'ATTACHING' || status === 'ATTACHFAILED'}>
                  <Tooltip title={i18n.t('log')}>
                    <span
                      className="mr-3"
                      onClick={(e) => {
                        e.stopPropagation();
                        updater.modalVisible(true);
                        updater.recordId(content.recordId);
                      }}
                    >
                      <CustomIcon type="jincheng" />
                    </span>
                  </Tooltip>
                </IF>
                {/* 云Addon 3.15 不做编辑 */}
                <IF check={onEitAddon && category === 'custom' && customAddonType !== 'cloud'}>
                  <Tooltip title={i18n.t('edit')}>
                    <span
                      className="mr-3"
                      onClick={(e) => {
                        e.stopPropagation();
                        // edit third addon
                        onEitAddon && onEitAddon(content);
                      }}
                    >
                      <CustomIcon type="bj1" />
                    </span>
                  </Tooltip>
                </IF>
                <Tooltip title={i18n.t('check detail')}>
                  <span
                    onClick={(e) => {
                      e.stopPropagation();
                      addonStore.getAddonDetail(instanceId);
                      addonStore.getAddonReferences(instanceId);
                      openDrawer();
                    }}
                  >
                    <CustomIcon type="ch" />
                  </span>
                </Tooltip>
              </div>
            </IF>
          </div>
        </div>
      </div>
    );
  };

  return (
    <>
      <Holder when={isEmpty(dataSource)}>
        {!isEmpty(categoryRefs) ? (
          <ul className="addon-list" ref={props.forwardedRef}>
            {dataSource &&
              dataSource.map((category: [string, any[]]) => {
                const [title, contents] = category;
                const liRef = categoryRefs && categoryRefs.find((x) => x[title]);
                let extra = null;
                if (get(contents, '[0].category') === 'custom' && routeParams.projectId) {
                  extra = (
                    <Button size="small" className="ml-2" onClick={() => updater.customConfigVisible(true)}>
                      {i18n.t('dop:view config')}
                    </Button>
                  );
                }
                return (
                  <li className="addon-category-section" key={title} ref={liRef[title]}>
                    <span className="content-title font-medium">
                      {title}
                      {extra}
                    </span>
                    <div className="addons-container">
                      <CardsLayout dataList={contents} contentRender={CardRender} />
                    </div>
                  </li>
                );
              })}
          </ul>
        ) : (
          <div className="addon-list">
            <CardsLayout dataList={dataSource} contentRender={CardRender} />
          </div>
        )}
      </Holder>
      <AddonDetailDrawer
        closeDrawer={closeDrawer}
        drawerVisible={drawerVisible}
        isFetching={false}
        addonDetail={addonDetail}
        addonReferences={addonReferences}
      />
      <CustomAddonConfigModal visible={customConfigVisible} onCancel={() => updater.customConfigVisible(false)} />
      <CreateLog visible={modalVisible} recordId={recordId} toggleModal={updater.modalVisible} />
    </>
  );
};
