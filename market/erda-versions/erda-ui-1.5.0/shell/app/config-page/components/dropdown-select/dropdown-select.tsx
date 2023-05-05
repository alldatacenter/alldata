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

import { Dropdown, Menu, Input } from 'antd';
import { ErdaIcon } from 'common';
import React from 'react';
import { map, get } from 'lodash';
import i18n from 'i18n';
import './dropdown-select.scss';

const DropdownSelect = (props: CP_DROPDOWN_SELECT.Props) => {
  const { execOperation, props: configProps, state: propsState, customProps = {} } = props;
  const { options, showLimit = 12, quickSelect = [], overlay, trigger, visible, ...restProps } = configProps;
  const { onChange } = customProps;
  let _overlay = overlay;
  const { value } = propsState;
  const [filterValue, setFilterValue] = React.useState('');
  const [active, setActive] = React.useState(false);

  const label = React.useMemo(
    () =>
      get(
        options?.find((item) => item.value === value),
        'label',
        '',
      ),
    [options, value],
  );

  const gotoSpecificPage = (item: CP_DROPDOWN_SELECT.IQuickSelect) => {
    item?.operations && item?.operations?.click && execOperation(item.operations.click);
  };

  React.useEffect(() => {
    // 控制点击外部关闭 dropdown
    const handleCloseDropdown = (e: MouseEvent) => {
      const dropdowns = Array.from(document.querySelectorAll('.dropdown-select'));
      const dropdownButton = document.querySelector('.dropdown-select-button');
      const node = e.target as Node;
      const inner = dropdowns.some((wrap) => wrap.contains(node));

      if (!inner && node !== dropdownButton) {
        setActive(false);
      }
    };

    document.body.addEventListener('click', handleCloseDropdown);

    return () => document.body.removeEventListener('click', handleCloseDropdown);
  }, []);

  if (options) {
    _overlay = (
      <Menu>
        <Menu.Item className="dropdown-select">
          <Input
            autoFocus
            size="small"
            placeholder={i18n.t('search')}
            prefix={<ErdaIcon type="search1" size="16" />}
            value={filterValue}
            onChange={(e) => setFilterValue(e.target.value)}
          />
        </Menu.Item>

        <Menu.Divider key="divider1" />
        {map(options.slice(0, showLimit), (item: CP_DROPDOWN_SELECT.IOptionItem) => {
          // 前端搜索
          if (!item.label.toLowerCase().includes(filterValue)) {
            return null;
          }

          return (
            <Menu.Item
              key={item.value}
              disabled={item.disabled}
              className="hover-active"
              onClick={() => {
                setActive(false);
                if (item.operations?.click) {
                  execOperation(item.operations.click, item);
                  onChange?.();
                }
              }}
            >
              <div className="flex justify-between items-center w-full">
                <span>
                  {item.prefixIcon ? <ErdaIcon type={item.prefixIcon} /> : null}
                  {item.prefixImgSrc ? (
                    <img src={item.prefixImgSrc} className="cp-dice-dropdown-select-image mr-2" />
                  ) : null}
                  {item.label}
                </span>
                <span className="flex">{value === item.value ? <ErdaIcon type="check" className="ml-2" /> : null}</span>
              </div>
            </Menu.Item>
          );
        })}
        <Menu.Divider key="divider2" />
        {quickSelect.length > 0
          ? map(quickSelect, (item, index) => (
              <Menu.Item key={`${index}`} className="hover-active" onClick={() => gotoSpecificPage(item)}>
                {item?.label || null}
              </Menu.Item>
            ))
          : null}
      </Menu>
    );
  }

  if (!visible) {
    return null;
  }

  return (
    <Dropdown
      className="cp-dice-dropdown-select"
      overlay={_overlay}
      visible={active}
      trigger={trigger || ['click']}
      {...restProps}
    >
      <span
        className="flex items-center justify-center dropdown-select-button hover-active"
        onClick={() => setActive(!active)}
      >
        {propsState?.label || label}
        <ErdaIcon type="caret-down" className="ml-0.5" size="18" />
      </span>
    </Dropdown>
  );
};

export default DropdownSelect;
