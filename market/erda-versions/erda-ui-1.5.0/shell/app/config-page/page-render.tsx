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
import { map, isArray, isPlainObject, isEmpty, isEqual, cloneDeep, get, set, has, pickBy } from 'lodash';
import { EmptyHolder } from 'common';
import { containerMap as fullContainerMap } from './components';
import { goTo } from 'common/utils';
import routeInfoStore from 'core/stores/route';

interface IProps extends IExtraProps {
  pageConfig: CONFIG_PAGE.PageConfig;
  customProps?: Obj;
}

interface IExtraProps {
  execOperation: (
    cId: string,
    opObj: { key: string; [p: string]: any },
    updateState?: { dataKey: string; dataVal: Obj },
    extraUpdateInfo?: Obj,
  ) => void;
  changeScenario: (s: { scenarioKey: string; scenarioType: string; inParams?: Obj }) => void;
  updateState: (dataKey: string, val: Obj) => void;
}

const emptyObj = {};
const ConfigPageRender = (props: IProps) => {
  const { pageConfig, customProps, execOperation, changeScenario, updateState } = props;
  const { hierarchy, components = emptyObj } = pageConfig || {};
  const [componentsKey, setComponentsKey] = React.useState([] as string[]);
  const routeParams = routeInfoStore.useStore((s) => s.params);
  const containerMapRef = React.useRef(null as any);

  React.useEffect(() => {
    if (components) {
      const curCompKeys = Object.keys(components);
      if (!isEqual(curCompKeys, componentsKey)) {
        containerMapRef.current = getContainerMap(components);
        setComponentsKey(curCompKeys);
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [components]);

  if (isEmpty(pageConfig)) return <EmptyHolder relative />;

  const containerMap = containerMapRef.current || {};
  const { root, structure = {} } = hierarchy || {};

  const compPrefixKey = 'protocol.components';
  const reUpdateState = (_cId: string) => (val: Obj) => {
    return updateState(`${compPrefixKey}.${_cId}.state`, val);
  };

  const execCommand = (command: Obj, val: Obj) => {
    if (command) {
      const { key, state, target, jumpOut } = command;
      if (key === 'goto' && target) {
        const { params, query } = state || {};
        const str_num_params = pickBy({ ...(val || {}), ...params }, (v) => ['string', 'number'].includes(typeof v));
        const str_num_query = pickBy(query, (v) => ['string', 'number'].includes(typeof v));
        goTo(goTo.pages[target] || target, { ...routeParams, ...str_num_params, jumpOut, query: str_num_query });
        return true;
      } else if (key === 'changeScenario') {
        changeScenario(command);
        return true;
      }
    }
    return false;
  };

  const reExecOperation = (_cId: string) => (_op: any, val: any) => {
    if (!_op || isEmpty(_op)) return;
    const op = cloneDeep({ ..._op });
    let updateVal = cloneDeep(val) as any;
    if (op.fillMeta) {
      // 有fillMeta，需要手动设置op.meta
      updateVal = undefined;
      const [fillAttrs, attrVals] = isArray(op.fillMeta)
        ? [op.fillMeta, { ...val }]
        : [[op.fillMeta], { [op.fillMeta]: val }];

      map(fillAttrs, (attr) => {
        set(op, `meta.${attr}`, attrVals[attr]);
      });
    }
    if (op.reload === false) {
      if (op.command) {
        const { state, target } = op.command;
        if (execCommand(op.command, updateVal)) return;
        return execOperation(_cId, op, { dataKey: `${compPrefixKey}.${target}.state`, dataVal: state });
      }
      return;
    }
    if (op.command?.key) {
      op.callBack = () => execCommand(op.command, updateVal);
    }

    return execOperation(
      _cId,
      op,
      isEmpty(updateVal) ? undefined : { dataKey: `${compPrefixKey}.${_cId}.state`, dataVal: updateVal },
    );
  };

  if (root) {
    const renderComp = (cId: string): any => {
      const structureItem = (structure || {})[cId];
      const Comp = containerMap[cId] as any;
      if (!Comp) return null;
      const configComponent = get(pageConfig, `components.${cId}`) || {};
      const { op, props: customComponentProps, ...restCustomConfig } = customProps?.[cId] || {};
      const enhanceProps = {
        ...restCustomConfig,
        ...configComponent,
        props: { ...customComponentProps, ...configComponent.props },
        key: cId,
        cId,
        customOp: op || emptyObj,
        updateState: reUpdateState(cId),
        execOperation: reExecOperation(cId),
      };

      if (isArray(structureItem)) {
        // 数组: 包含以children方式嵌入组件
        return (
          <EnhanceCompProps {...enhanceProps}>
            <Comp>{map(structureItem, (item) => renderComp(item))}</Comp>
          </EnhanceCompProps>
        );
      } else if (!structureItem) {
        // 叶子节点，直接渲染
        return (
          <EnhanceCompProps {...enhanceProps}>
            <Comp />
          </EnhanceCompProps>
        );
      } else if (isPlainObject(structureItem)) {
        // 对象包含：以props方式嵌入组件
        const p = {};
        map(structureItem, (vKey, pKey) => {
          if (isArray(vKey)) {
            p[pKey] = [];
            map(vKey, (vItem) => {
              p[pKey].push(renderComp(vItem));
            });
          } else {
            p[pKey] = renderComp(vKey);
          }
        });
        return (
          <EnhanceCompProps {...enhanceProps}>
            <Comp {...p} />
          </EnhanceCompProps>
        );
      }
      return null;
    };
    return <>{renderComp(root)}</>;
  } else {
    // eslint-disable-next-line no-console
    console.log('dice config page: 请配置一个root节点');
    return null;
  }
};

export default ConfigPageRender;

// 根据配置中的container，获取到当前的所有的组件map
const getContainerMap = (container: Obj<CONFIG_PAGE.BaseSpec>) => {
  const conMap = {};
  map(container, (config, cId) => {
    const Comp = fullContainerMap[config.type];
    conMap[cId] = Comp || fullContainerMap.NotFound;
  });
  return conMap;
};

const EnhanceCompProps = (
  props: Merge<CONFIG_PAGE.BaseSpec, { children: React.ReactElement; options: CONFIG_PAGE.CompOptions }>,
) => {
  const { children, props: configProps, data: pData, Wrapper, options, ...rest } = props;

  const [comProps, setCompProps] = React.useState(configProps);
  const [data, setData] = React.useState(pData);

  const ignoreData = comProps?.requestIgnore?.includes('data');
  React.useEffect(() => {
    if (configProps !== null && configProps !== undefined) {
      setCompProps(configProps);
    }
  }, [configProps]);

  React.useEffect(() => {
    if (ignoreData) {
      if (!isEmpty(pData)) {
        setData(pData);
      }
    } else {
      setData(pData);
    }
  }, [pData, ignoreData]);

  if (options?.visible === false) return null;
  if (Wrapper) {
    return <Wrapper>{React.cloneElement(children, { props: comProps, data, ...rest })}</Wrapper>;
  }
  return React.cloneElement(children, { props: comProps, data, ...rest });
};
