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
import { Tooltip, Dropdown, Menu, Radio, Badge } from 'antd';
import { map, isArray, find, get } from 'lodash';
import { Icon as CustomIcon, ErdaIcon } from 'common';
import { useUpdate } from 'common/use-hooks';

const handleState = (_stateObj?: Obj) => {
  return {
    ..._stateObj,
    value: _stateObj?.value,
    childrenValue: _stateObj?.childrenValue,
  };
};

const CP_RADIO = (props: CP_RADIO.Props) => {
  const { updateState, customOp, execOperation, operations, state: propsState, props: configProps, data } = props;
  const { radioType, options: propsOptions, visible, ...rest } = configProps || {};
  const RadioItem = radioType === 'button' ? Radio.Button : Radio;
  const [state, updater, update] = useUpdate(handleState(propsState));

  React.useEffect(() => {
    update(handleState(propsState));
  }, [propsState, update]);

  React.useEffect(() => {
    customOp?.onStateChange && customOp.onStateChange(state);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [state]);

  const onChange = (val: any) => {
    operations?.onChange && execOperation(operations?.onChange, { ...state, ...val });
    update(val);
  };

  if (visible === false) {
    return null;
  }

  const options = data?.options || propsOptions;

  return (
    <Radio.Group {...rest} value={state.value} onChange={(e: any) => onChange({ value: e.target.value })}>
      {map(options, (mItem) => {
        const { children, key, prefixIcon, disabled, text, tooltip, width, status, operations: itemOp } = mItem;

        if (isArray(children)) {
          const curChildrenVal = get(state, `childrenValue.${key}`) || (get(children, '[0].key') as string);
          const childName = get(find(children, { key: curChildrenVal }), 'text');
          const getMenu = () => {
            return (
              <Menu
                onClick={(e: any) => onChange({ value: key, childrenValue: { ...state.childrenValue, [key]: e.key } })}
              >
                {map(children, (g) => {
                  const extraProps = {} as Obj;
                  if (itemOp && itemOp[g.key]) {
                    extraProps.onClick = () => {
                      execOperation(itemOp[g.key]);
                    };
                  }
                  return (
                    <Menu.Item
                      className={`${curChildrenVal === g.key ? 'text-primary bg-light-active' : ''}`}
                      key={g.key}
                      {...extraProps}
                    >
                      {g.text}
                    </Menu.Item>
                  );
                })}
              </Menu>
            );
          };
          return (
            <Tooltip key={key} title={tooltip}>
              <Dropdown overlay={getMenu()}>
                <RadioItem value={key} key={key} disabled={disabled}>
                  <div className="flex justify-between items-center">
                    {prefixIcon ? <CustomIcon type={prefixIcon} className="mr-1" /> : null}
                    <span className="nowrap" style={{ ...(width ? { width } : {}) }}>
                      {childName}
                    </span>
                    <ErdaIcon size="18" type="caret-down" className="ml-1" />
                  </div>
                </RadioItem>
              </Dropdown>
            </Tooltip>
          );
        } else {
          const extraProps = {} as Obj;
          if (itemOp?.click) {
            extraProps.onClick = () => {
              execOperation(itemOp.click);
            };
          }
          return (
            <Tooltip key={key} title={tooltip}>
              <RadioItem value={key} key={key} disabled={disabled} {...extraProps}>
                <div className="flex justify-between items-center">
                  {prefixIcon ? <CustomIcon type={prefixIcon} className="mr-1" /> : null}
                  {status ? <Badge status={status || 'default'} className="mr-1" /> : null}
                  {text}
                </div>
              </RadioItem>
            </Tooltip>
          );
        }
      })}
    </Radio.Group>
  );
};

export default CP_RADIO;
