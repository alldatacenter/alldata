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

import { Input, Select } from 'antd';
import classNames from 'classnames';
import routeInfoStore from 'core/stores/route';
import { debounce, isEmpty, isEqual, map } from 'lodash';
import moment from 'moment';
import React from 'react';
import BaseFilter, { FilterItemConfig } from '../filter/base-filter';

interface IFilterProps {
  prefixCls?: string;
  className?: string | string[];
  instanceKey?: string;
  config: FilterItemConfig[];
  title?: string;
  width?: number;
  showSolutionConfig?: boolean;
  isConnectQuery?: boolean;
  showCollapse?: boolean;
  onSubmit?: (value: Obj) => void;
  onReset?: (value: Obj) => void;
  actions?: JSX.Element[];
  wrappedComponentRef?: React.MutableRefObject<any>;
  skipInit?: boolean; // 当有多个Filter实例的情况下， 如果两个Filter同时init，其中一个不需要初始化拿url的参数
  showButton?: boolean;
}

const CustomFilter = (props: IFilterProps) => {
  const {
    config,
    className,
    onReset,
    onSubmit,
    skipInit,
    isConnectQuery = false,
    showButton,
    actions,
    ...restProps
  } = props;
  const actionsHasPadding = config.some((t) => t.label);

  const [query] = routeInfoStore.useStore((s) => [s.query]);
  const filterRef: any = React.useRef(null as any);
  const [prevQuery, setPrevQuery] = React.useState({});

  const search = React.useCallback(
    debounce(() => {
      filterRef && filterRef.current && filterRef.current.search();
    }, 1000),
    [],
  );

  const transformConfig = React.useCallback(
    (c: FilterItemConfig) => {
      const { type, ...cRest } = c;
      let _type = type;
      const Comp = type;
      if (!showButton) {
        switch (_type) {
          case Input:
          case Input.Search:
            _type = React.forwardRef((inputProps: any, ref) => {
              const { onChange, ...rest } = inputProps;
              return (
                <Comp
                  ref={ref}
                  onChange={(e: any) => {
                    onChange(e.target.value || undefined);
                    search();
                  }}
                  allowClear
                  {...rest}
                />
              );
            });
            break;
          case Select:
            _type = React.forwardRef((selectProps: any, ref) => {
              const { onChange, options, ...rest } = selectProps;
              return (
                <Select
                  ref={ref}
                  onChange={(v: string) => {
                    onChange(v);
                    filterRef && filterRef.current && filterRef.current.search();
                  }}
                  allowClear
                  {...rest}
                >
                  {typeof options === 'function' ? options() : options}
                </Select>
              );
            });
            break;
          default:
            _type = React.forwardRef((compProps: any, ref) => {
              const { onChange, ...rest } = compProps;
              return (
                <Comp
                  forwardedRef={ref}
                  onChange={(v: any) => {
                    onChange(v);
                    filterRef && filterRef.current && filterRef.current.search();
                  }}
                  {...rest}
                />
              );
            });
            break;
        }
      }
      return { type: _type, ...cRest, isHoldLabel: false };
    },
    [filterRef, search, showButton],
  );

  const fields = filterRef && filterRef.current && filterRef.current.form.getFieldsValue();

  // 受控Filter，受控于query
  React.useEffect(() => {
    const curFilterForm = filterRef.current && filterRef.current.form;
    if (!isConnectQuery || isEqual(prevQuery, query) || isEmpty(fields)) return;
    setPrevQuery(query);
    map(config, (item) => {
      const { name, customTransformer, valueType = 'string' } = item;
      // const _type = transformConfig(item);

      if (valueType === 'range') {
        let startName = `${name}From`;
        let endName = `${name}To`;
        const rangeNames = name.split(',');
        if (rangeNames.length === 2) {
          [startName, endName] = rangeNames;
        }

        const startValue = query[startName];
        const endValue = query[endName];
        if (startValue && endValue) {
          curFilterForm &&
            curFilterForm.setFieldsValue({
              [name]: [
                moment(isNaN(+startValue) ? startValue : +startValue), // 如果是时间戳字符串，应转为number类型
                moment(isNaN(+endValue) ? endValue : +endValue),
              ],
            });
        } else {
          curFilterForm && curFilterForm.setFieldsValue([]);
        }
        return;
      }

      if (query[name] !== undefined) {
        curFilterForm &&
          curFilterForm.setFieldsValue({
            [name]: customTransformer
              ? customTransformer(query[name], query)
              : valueType === 'number'
              ? Number(query[name])
              : valueType === 'boolean'
              ? !!query[name]
              : query[name],
          });
      } else {
        // 置为空
        curFilterForm && curFilterForm.setFieldsValue({ [name]: undefined });
      }
    });
  }, [query, config, fields, isConnectQuery, setPrevQuery, prevQuery]);

  const realConfig = React.useMemo<FilterItemConfig[]>(() => {
    // const initFilterConfig = !skipInit ? initConfig(config) : config;
    return map(config, (item) => {
      return transformConfig(item);
    });
  }, [config, transformConfig]);

  const filterClassName = classNames({
    'erda-custom-filter': true,
    'actions-no-padding': !actionsHasPadding,
    'my-3': true,
    className,
  });

  return (
    <BaseFilter
      className={filterClassName}
      config={realConfig}
      onReset={onReset}
      onSubmit={onSubmit}
      ref={filterRef}
      actions={showButton ? actions || undefined : []}
      {...restProps}
    />
  );
};

export default CustomFilter;
