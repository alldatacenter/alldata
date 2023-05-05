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

/** @author zxj 2021-03
 * 本次app-list未做组件化协议，
 * 但为了保持交互和视图跟项目列表的统一性，还是使用了组件化协议的方式来做，前端通过转换数据的方式实现；
 * 如此更方便后续接组件化协议
 * */
import React from 'react';
import { Spin } from 'antd';
import { useEffectOnce } from 'react-use';
import i18n from 'i18n';
import { Icon as CustomIcon, IF } from 'common';
import { goTo, ossImg, connectCube } from 'common/utils';
import { theme } from 'app/themes';
import { modeOptions, appMode } from 'application/common/config';
import DiceConfigPage from 'config-page/index';
import { get, set, compact, merge } from 'lodash';
import { removeMember } from 'common/services/index';
import userStore from 'app/user/stores';
import { useLoading } from 'core/stores/loading';
import { produce } from 'immer';
import { BlockNetworkTips } from 'dop/pages/projects/block-comp';
import permStore from 'user/stores/permission';
import moment from 'moment';

const Mapper = () => {
  const [userLoading] = useLoading(userStore, ['getJoinedApps']);
  const { getJoinedApps } = userStore.effects;
  const { clearAppList } = userStore.reducers;
  return {
    isFetching: userLoading,
    getList: getJoinedApps,
    clearList: clearAppList,
  };
};

interface IProps extends ReturnType<typeof Mapper> {
  placeHolderMsg?: string;
  isInProject?: boolean;
  getList: (p?: Obj) => Promise<any>;
}

const getBlockNetInfo = (app: IApplication) => {
  const { blockStatus, unBlockStart, unBlockEnd } = app;
  const period =
    unBlockEnd && unBlockStart
      ? `${i18n.t('dop:time period')}: ${moment(unBlockStart).format('YYYY-MM-DD HH:mm')}~${moment(unBlockEnd).format(
          'YYYY-MM-DD HH:mm',
        )}`
      : '';
  const periodInfo = period ? { icon: 'time', text: period, tooltip: period } : null;
  const statusMap = {
    blocked: [periodInfo],
    unblocking: [
      {
        icon: 'lock',
        text: i18n.t('unblocking, please wait'),
        tooltip: i18n.t('unblocking, please wait'),
        type: 'warning',
      },
      periodInfo,
    ],
    unblocked: [{ icon: 'unlock', text: i18n.t('default:unblocked'), type: 'success' }, periodInfo],
  };
  return compact(statusMap[blockStatus] || []);
};

const convertListData = (list: IApplication[], isInProject: boolean) => {
  const appPerm = permStore.getState((s) => s.app);
  return list.map((l) => {
    const {
      id,
      name,
      desc,
      logo,
      projectName,
      projectId,
      projectDisplayName,
      isPublic,
      stats,
      updatedAt,
      mode,
      pined,
    } = l;
    const extraInfos: CP_LIST.IIconInfo[] = [
      isPublic
        ? { icon: 'unlock', text: i18n.t('dop:public application') }
        : { icon: 'lock', text: i18n.t('dop:private application') },
      {
        icon: 'api-app',
        text: projectDisplayName,
        tooltip: `${i18n.t('dop:owned project')}: ${projectDisplayName}(${projectName})`,
        operations: isInProject
          ? undefined
          : {
              click: { key: 'gotoProject', reload: false },
            },
      },
      updatedAt
        ? {
            icon: 'time',
            text: moment(updatedAt).fromNow(),
            tooltip: `${i18n.t('update time')}:${moment(updatedAt).format('YYYY-MM-DD HH:mm:ss')}`,
          }
        : { icon: 'time', text: i18n.t('none') },
      {
        icon: 'category-management',
        text: (modeOptions.find((m) => m.value === mode) as { name: string })?.name,
        tooltip: i18n.t('dop:application type'),
      },
    ].concat(getBlockNetInfo(l));

    if ([appMode.MOBILE, appMode.LIBRARY, appMode.SERVICE].includes(mode)) {
      extraInfos.splice(2, 0, {
        icon: 'list-numbers',
        text: `${stats.countRuntimes}`,
        tooltip: `${i18n.t('dop:number of application instance')}`,
        ...(appPerm.runtime.read.pass
          ? {
              operations: {
                click: { key: 'gotoDeploy', reload: false },
              },
            }
          : {}),
      });
    }

    const exitOp = isInProject
      ? {}
      : {
          // 退出操作：仅在我的应用列表中展示
          exit: {
            key: 'exit',
            text: i18n.t('exit'),
            reload: true,
            confirm: i18n.t('common:exit-sub-tip {name}', { name: i18n.t('application') }),
            meta: { appId: l.id },
          },
        };

    const pinOp: Obj<CP_COMMON.Operation> = pined
      ? {
          unpinApp: {
            key: 'unpinApp',
            text: i18n.t('dop:unpin'),
            reload: true,
            meta: { appId: l.id },
          },
        }
      : {
          pinApp: {
            key: 'pinApp',
            text: i18n.t('dop:sticky'),
            reload: true,
            meta: { appId: l.id },
          },
        };

    return {
      id,
      projectId,
      title: name,
      description: desc || i18n.t('dop:edit description in application setting'),
      prefixImg: (
        <IF check={logo}>
          <img src={ossImg(logo, { w: 46 })} alt="logo" />
          <IF.ELSE />
          <CustomIcon color type={theme.appIcon} />
        </IF>
      ),
      extraInfos,
      operations: { ...pinOp, ...exitOp },
    } as CP_LIST.IListData;
  });
};

interface IApplicationRes {
  list: IApplication[] | CP_LIST.IListData[];
  total: number;
}
const defaultPaging = {
  pageNo: 1,
  pageSize: 20,
};
const getPageConfig = (isInProject = false) => {
  const filterCondition = compact([
    {
      key: 'q',
      label: i18n.t('title'),
      emptyText: i18n.t('dop:all'),
      fixed: true,
      showIndex: 2,
      placeholder: i18n.t('filter by {name}', { name: i18n.t('name') }),
      type: 'input',
    },
    isInProject
      ? {
          key: 'public',
          emptyText: i18n.t('dop:all'),
          fixed: true,
          label: i18n.t('whether to be public'),
          customProps: {
            mode: 'single',
          },
          options: [
            { label: i18n.t('dop:all'), value: 'all' },
            { label: i18n.t('dop:public application'), value: 'public' },
            { label: i18n.t('dop:private application'), value: 'private' },
          ],
          showIndex: 1,
          type: 'select',
        }
      : null,
  ]);
  return {
    scenario: {
      scenarioKey: 'app-list',
      scenarioType: 'app-list',
    },
    protocol: {
      hierarchy: {
        root: 'page',
        structure: {
          page: ['filter', 'list'],
        },
      },
      components: {
        page: { type: 'Container' },
        filter: {
          type: 'ContractiveFilter',
          state: {
            conditions: filterCondition,
            values: {
              public: 'all',
            },
          },
          operations: {
            filter: {
              key: 'filter',
              reload: true,
            },
          },
        },
        list: {
          type: 'List',
          state: {
            ...defaultPaging,
            total: 0,
          },
          props: {
            pageSizeOptions: ['10', '20', '50', '100'],
          },
          operations: {
            changePageNo: {
              key: 'changePageNo',
              reload: true,
            },
            changePageSize: {
              key: 'changePageSize',
              reload: true,
            },
          },
          data: {
            list: [],
          },
        },
      },
    },
  } as CONFIG_PAGE.RenderConfig;
};

export const PureAppList = ({ getList: _getList, isFetching, clearList, isInProject = false }: IProps) => {
  const loginUser = userStore.useStore((s) => s.loginUser);
  const { pinApp, unpinApp } = userStore.effects;

  useEffectOnce(() => {
    return clearList;
  });

  const handleOperation = (payload: Obj) => {
    const opKey = get(payload, 'event.operation');
    let query = { ...defaultPaging };
    const { pageNo: pNo, pageSize: pSize } = (get(payload, 'protocol.components.list.state') as Obj) || {};
    const filterData = get(payload, 'protocol.components.filter.state.values') || {};
    const { public: fPublic, ...filterRest } = filterData;
    const publicFilter = ['public', 'private'].includes(fPublic) ? { public: fPublic } : {};
    const filterValues = { ...filterRest, ...publicFilter };
    const requestFun = (_query: Obj) =>
      getList(_query).then((res: IApplicationRes) => {
        const reData = produce(merge(getPageConfig(isInProject), payload), (draft) => {
          set(draft, 'protocol.components.list.data.list', res.list);
          set(draft, 'protocol.components.list.state', {
            total: res.total,
            pageNo: query.pageNo,
            pageSize: query.pageSize,
          });
        });
        return reData;
      });

    switch (opKey) {
      case 'changePageNo':
      case 'changePageSize':
        query = { pageNo: pNo, pageSize: pSize, ...filterValues };
        break;
      case 'filter':
        query = { pageNo: 1, pageSize: pSize, ...filterValues };
        break;
      case 'unpinApp': {
        // 取消置顶
        const appId = get(payload, 'event.operationData.meta.appId');
        return unpinApp(appId).then(() => {
          return requestFun({ pageNo: 1, pageSize: pSize, ...filterValues });
        });
      }
      case 'pinApp': {
        // 置顶
        const appId = get(payload, 'event.operationData.meta.appId');
        return pinApp(appId).then(() => {
          return requestFun({ pageNo: 1, pageSize: pSize, ...filterValues });
        });
      }
      case 'exit': {
        // 退出
        const appId = get(payload, 'event.operationData.meta.appId');
        return removeMember({ scope: { type: 'app', id: `${appId}` }, userIds: [loginUser.id] }).then(
          (res: { success: boolean }) => {
            if (res.success) {
              // 重新reload
              return requestFun({ pageNo: 1, pageSize: pSize, ...filterValues });
            }
          },
        );
      }
      default:
        break;
    }
    return requestFun(query);
  };

  const getList = (q: Obj) => {
    return _getList(q).then((res: IApplicationRes) => {
      const { list: _list, total } = res;
      return { list: convertListData(_list as IApplication[], isInProject), total };
    });
  };

  return (
    <Spin spinning={isFetching}>
      <BlockNetworkTips />
      <DiceConfigPage
        showLoading={false}
        scenarioKey="app-list"
        scenarioType="app-list"
        useMock={handleOperation}
        forceMock
        customProps={{
          list: {
            op: {
              clickItem: (_: unknown, _data: IApplication) => {
                const { projectId, id } = _data;
                goTo(goTo.pages.app, { projectId, appId: id });
              },
              gotoProject: (_: unknown, _data: IApplication) => {
                const { projectId } = _data;
                goTo(goTo.pages.project, { projectId });
              },
              gotoDeploy: (_: unknown, _data: IApplication) => {
                const { projectId, id } = _data;
                goTo(goTo.pages.deploy, { projectId, appId: id });
              },
            },
          },
        }}
      />
    </Spin>
  );
};

export const MyAppList = connectCube(PureAppList, Mapper);
