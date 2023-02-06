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
import { useMount, useUpdateEffect } from 'react-use';
import { isEmpty, get, set, isEqual, forEach, has } from 'lodash';
import { produce } from 'immer';
import { Spin } from 'antd';
import { notify } from 'common/utils';
import { useUpdate } from 'common/use-hooks';
import { useMock } from './mock/index';
import ConfigPageRender from './page-render';
import commonStore from 'common/stores/common';

interface ICustomProps {
  [p: string]: {
    op?: Obj;
    props?: Obj;
    Wrapper?: React.ElementType;
  };
}
interface IProps {
  inParams?: Obj;
  customProps?: ICustomProps;
  scenarioType: string;
  scenarioKey: string;
  showLoading?: boolean;
  wrapperClassName?: string;
  className?: string;
  forbiddenRequest?: boolean;
  forceUpdateKey?: string[];
  debugConfig?: CONFIG_PAGE.RenderConfig;
  onExecOp?: any;
  fullHeight?: boolean;
  forceMock?: boolean; // 使用mock
  useMock?: (params: Obj) => Promise<any>;
  updateConfig?: (params: CONFIG_PAGE.RenderConfig) => void;
  operationCallBack?: (
    reqConfig: CONFIG_PAGE.RenderConfig,
    resConfig?: CONFIG_PAGE.RenderConfig,
    op?: CP_COMMON.Operation,
  ) => void;
}

const unProduct = process.env.NODE_ENV !== 'production';

const globalOperation = {
  __AsyncAtInit__: '__AsyncAtInit__',
  __Sync__: '__Sync__',
};

const ConfigPage = React.forwardRef((props: IProps, ref: any) => {
  const {
    fullHeight = true,
    inParams = {},
    customProps = {},
    scenarioType,
    wrapperClassName = '',
    className = '',
    scenarioKey,
    showLoading = true,
    forbiddenRequest,
    forceUpdateKey,
    useMock: _useMock,
    forceMock,
    debugConfig,
    onExecOp,
    updateConfig,
    operationCallBack,
  } = props;
  const [{ pageConfig, fetching }, updater] = useUpdate({
    pageConfig:
      debugConfig ||
      ({
        scenario: {
          scenarioType,
          scenarioKey,
        },
        inParams,
      } as CONFIG_PAGE.RenderConfig),
    fetching: false,
  });
  const opIndexRef = React.useRef(0);
  const timerRef = React.useRef<any>();
  // 在非生产环境里，url中带useMock
  const useMockMark = forceMock || (unProduct && location.search.includes('useMock'));
  const changeScenario = (s: { scenarioKey: string; scenarioType: string; inParams?: Obj }) => {
    const { scenarioType: newType, scenarioKey: newKey, inParams: newInParams } = s;
    newKey &&
      queryPageConfig({
        scenario: {
          scenarioType: newType,
          scenarioKey: newKey,
        },
        inParams: {
          ...inParamsRef.current,
          ...(newInParams || {}),
        },
      });
  };
  const pageConfigRef = React.useRef<CONFIG_PAGE.RenderConfig>(null as any);
  const inParamsRef = React.useRef<Obj>(inParams);
  const fetchingRef = React.useRef(false);
  // 此处需要使用store，因为接口中有userInfo需要被解析
  const { getRenderPageLayout } = commonStore.effects;

  useMount(() => {
    queryPageConfig(undefined, false, undefined, (config: CONFIG_PAGE.RenderConfig) => {
      // if there is any component marked as asyncAtInit, fetch again to load async component
      const comps = config?.protocol?.components || {};
      const asyncComponents: string[] = [];
      Object.keys(comps).forEach((k) => {
        if (comps[k].options?.asyncAtInit) {
          asyncComponents.push(k);
        }
      });
      if (asyncComponents.length) {
        // wait for first fetch promise finally finished, which set fetching to false
        setTimeout(() => {
          execOperation('', {
            key: globalOperation.__AsyncAtInit__,
            reload: true,
            components: asyncComponents,
          });
        }, 0);
      }
    });
  });

  React.useEffect(() => {
    if (debugConfig) {
      updater.pageConfig(debugConfig);
    }
  }, [debugConfig, updater]);

  React.useEffect(() => {
    pageConfigRef.current = pageConfig;
    if (pageConfig?.protocol?.options?.syncIntervalSecond) {
      timerRef.current = setInterval(() => {
        execOperation('', {
          key: globalOperation.__Sync__,
          async: true,
          reload: true,
        });
      }, pageConfig?.protocol?.options?.syncIntervalSecond * 1000);
    }
    return () => {
      clearInterval(timerRef.current);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pageConfig]);

  React.useEffect(() => {
    if (!isEqual(inParams, inParamsRef.current) && !isEmpty(inParams)) {
      inParamsRef.current = inParams;
    }
  }, [inParams]);
  React.useEffect(() => {
    if (ref) {
      ref.current = {
        reload: (extra: Obj) => {
          const { inParams: reInParams, ...extraRest } = extra || {};
          queryPageConfig({
            scenario: {
              scenarioType,
              scenarioKey,
            },
            inParams: { ...inParamsRef.current, ...reInParams },
            ...extraRest,
          });
        },
        getPageConfig: () => pageConfigRef.current,
      };
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [ref, _useMock]);

  const inParamsStr = JSON.stringify(inParams);
  useUpdateEffect(() => {
    if (forceUpdateKey?.includes('inParams')) {
      queryPageConfig();
    }
  }, [inParamsStr]);

  const queryPageConfig = (
    p?: CONFIG_PAGE.RenderConfig,
    partial?: boolean,
    op?: CP_COMMON.Operation,
    callBack?: Function,
  ) => {
    if (fetchingRef.current || forbiddenRequest) return; // forbidden request when fetching
    // 此处用state，为了兼容useMock的情况
    if (!op?.async) {
      updater.fetching(true);
      fetchingRef.current = true;
    }
    const curConfig = p || pageConfig;
    const reqConfig = { ...curConfig, inParams: { ...inParamsRef.current, ...curConfig?.inParams } };
    ((useMockMark && _useMock) || getRenderPageLayout)(reqConfig)
      .then((res: CONFIG_PAGE.RenderConfig) => {
        if (partial) {
          const _curConfig = pageConfigRef.current;
          const newConfig = produce(_curConfig, (draft) => {
            if (draft.protocol?.components) {
              draft.protocol.components = { ...draft.protocol.components, ...comps };
            }
          });
          updateConfig ? updateConfig(newConfig) : updater.pageConfig(newConfig);
          pageConfigRef.current = newConfig;
          operationCallBack?.(reqConfig, newConfig, op);
          callBack?.(newConfig);
        } else {
          // if (op?.index === undefined || (op.index && opIndexRef.current === op.index))  {
          // }
          // Retain the response data that matches the latest operation
          updateConfig ? updateConfig(res) : updater.pageConfig(res);
          pageConfigRef.current = res;
          operationCallBack?.(reqConfig, res, op);
          callBack?.(res);
        }
        if (op?.successMsg) notify('success', op.successMsg);
      })
      .catch(() => {
        if (op?.errorMsg) notify('error', op.errorMsg);
      })
      .finally(() => {
        updater.fetching(false);
        fetchingRef.current = false;
      });
  };

  const updateState = (dataKey: string, dataVal: Obj, callBack?: Function) => {
    const _curConfig = pageConfigRef.current;
    const newConfig = produce(_curConfig, (draft) => {
      if (dataKey) {
        const curData = get(draft, dataKey) || {};
        set(draft, dataKey, { ...curData, ...dataVal });
      }
    });
    callBack?.(newConfig);
    updateConfig ? updateConfig(newConfig) : updater.pageConfig(newConfig);
  };

  const execOperation = (
    cId: string,
    op: CP_COMMON.Operation,
    updateInfo?: { dataKey: string; dataVal: Obj },
    extraUpdateInfo?: Obj,
  ) => {
    const { key, reload, skipRender, partial, ..._rest } = op;
    const loadCallBack = (_pageData: CONFIG_PAGE.RenderConfig) => {
      op?.callBack?.();
      onExecOp && onExecOp({ cId, op, reload, updateInfo, pageData: _pageData });
    };
    opIndexRef.current += 1;
    const needRender = reload ?? !skipRender;

    if (needRender) {
      // 需要请求后端接口
      const _curConfig = pageConfigRef.current;
      const newConfig = produce(_curConfig, (draft) => {
        if (extraUpdateInfo && !isEmpty(extraUpdateInfo)) {
          // 数据不为空,先更新后请求
          const { dataKey, dataVal } = extraUpdateInfo;
          if (dataKey) {
            const curData = get(draft, dataKey) || {};
            set(draft, dataKey, { ...curData, ...dataVal });
          }
        }
        if (updateInfo && !isEmpty(updateInfo)) {
          // 数据不为空,先更新后请求
          const { dataKey, dataVal } = updateInfo;
          if (dataKey) {
            const curData = get(draft, dataKey) || {};
            set(draft, dataKey, { ...curData, ...dataVal });
          }
        }
        draft.event = {
          component: cId,
          operation: key,
          operationData: _rest,
        };
      });

      const formatConfig = clearLoadMoreData(newConfig);
      queryPageConfig(formatConfig, partial, { ...op, index: opIndexRef.current }, loadCallBack);
    } else if (updateInfo) {
      updateState(updateInfo.dataKey, updateInfo.dataVal, loadCallBack);
    }
  };

  const clearLoadMoreData = (newConfig: CONFIG_PAGE.RenderConfig) => {
    const formatConfig = produce(newConfig, (draft) => {
      const comps = get(draft, 'protocol.components');
      forEach(comps, (comp, compName) => {
        const ignoreData = {};
        const ignoreKeys: string[] = comp?.props?.requestIgnore || [];
        if (comp?.props?.isLoadMore) ignoreKeys.push('data');
        ignoreKeys.forEach((_key) => {
          ignoreData[_key] = undefined;
        });
        comps[compName] = { ...comp, ...ignoreData };
      });
    });

    return formatConfig;
  };

  const pageProtocol = React.useMemo(() => get(pageConfig, 'protocol'), [pageConfig]);
  const Content = React.useMemo(
    () => {
      return (
        <ConfigPageRender
          pageConfig={pageProtocol}
          updateState={updateState}
          changeScenario={changeScenario}
          execOperation={execOperation}
          customProps={customProps}
        />
      );
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [pageProtocol],
  );

  return showLoading ? (
    <Spin
      spinning={showLoading && fetching}
      wrapperClassName={`${fullHeight ? 'full-spin-height' : ''} ${wrapperClassName}`}
    >
      <div className={`h-full overflow-auto ${className}`}>{Content}</div>
    </Spin>
  ) : (
    Content
  );
});

export { useMock };
export default React.memo(ConfigPage);
