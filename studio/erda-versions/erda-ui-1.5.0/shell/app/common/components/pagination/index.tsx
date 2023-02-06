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
import { Popover, Input, Button, Dropdown, Menu } from 'antd';
import { ErdaIcon } from 'common';
import i18n from 'i18n';
import { PAGINATION } from 'app/constants';

import './index.scss';

/**
 * 配置项：
    total // 总数，默认为0
    current // 当前页码，默认为1
    pageSize // 每页行数默认为PAGINATION.pageSize
    showSizeChanger // 是否选择pageSize的下拉选择框，默认为true
    onChange // 翻页或者pageSize选择时触发，第一个参数为current，第二个参数为pageSize
 */
interface IPaginationProps {
  total: number;
  current: number;
  pageSize?: number;
  onChange: (page: number, pageSize?: number) => void;
}

export interface IPaginationJumpProps {
  pagination: IPaginationProps;
  hidePopover: () => void;
}

const Pagination = (pagination: IPaginationProps) => {
  const { total = 0, current = 1, pageSize = PAGINATION.pageSize, onChange } = pagination;

  const [goToVisible, setGoToVisible] = React.useState(false);

  const paginationCenterRender = (
    <Popover
      content={<PaginationJump pagination={pagination} hidePopover={() => setGoToVisible(false)} />}
      trigger="click"
      getPopupContainer={(triggerNode) => triggerNode.parentElement as HTMLElement}
      placement="top"
      overlayClassName="pagination-jump"
      visible={goToVisible}
      onVisibleChange={setGoToVisible}
    >
      <div className="pagination-center bg-hover px-3 rounded cursor-pointer" onClick={() => setGoToVisible(true)}>
        {total ? pagination.current : 0} / {(total && pageSize && Math.ceil(total / pageSize)) || 0}
      </div>
    </Popover>
  );

  const pageSizeMenu = (
    <Menu>
      {PAGINATION.pageSizeOptions.map((item: string | number) => {
        return (
          <Menu.Item key={item} onClick={() => onChange?.(1, +item)}>
            <span className="fake-link mr-1">{i18n.t('{size} items / page', { size: item })}</span>
          </Menu.Item>
        );
      })}
    </Menu>
  );

  return (
    <div className="erda-pagination flex justify-end items-center">
      <div className="erda-pagination-total mr-2">{i18n.t('total {total} items', { total })}</div>
      <div className="erda-pagination-content inline-flex">
        <div
          className={`bg-hover p-2 leading-none ${current === 1 ? 'disabled' : 'cursor-pointer'}`}
          onClick={() => current > 1 && onChange?.(current - 1)}
        >
          <ErdaIcon type="left" size={18} color="currentColor" />
        </div>
        {paginationCenterRender}
        <div
          className={`bg-hover p-2 leading-none ${current === Math.ceil(total / pageSize) ? 'disabled' : 'cursor-pointer'}`}
          onClick={() => total && current < Math.ceil(total / pageSize) && onChange?.(current + 1)}
        >
          <ErdaIcon type="right" size={18} color="currentColor" />
        </div>
      </div>
      <Dropdown
        trigger={['click']}
        overlay={pageSizeMenu}
        align={{ offset: [0, 5] }}
        overlayStyle={{ minWidth: 120 }}
        getPopupContainer={(triggerNode) => triggerNode.parentElement as HTMLElement}
      >
        <span className="bg-hover px-3 py-1 cursor-pointer">{i18n.t('{size} items / page', { size: pageSize })}</span>
      </Dropdown>
    </div>
  );
};

const PaginationJump = ({ pagination, hidePopover }: IPaginationJumpProps) => {
  const { total = 0, pageSize = PAGINATION.pageSize, onChange } = pagination;
  const [value, setValue] = React.useState('');

  const handleChange = React.useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      const { value: val } = e.target;
      if (!isNaN(Number(val)) && +val > 0 && !val.includes('.')) {
        setValue(val);
      } else if (!val) {
        setValue('');
      }
    },
    [setValue],
  );

  const jump = () => {
    const maxCurrent = Math.ceil(total / pageSize);
    if (value) {
      if (+value <= maxCurrent) {
        onChange?.(+value);
      } else {
        onChange?.(maxCurrent);
      }
      setValue('');
      hidePopover();
    }
  };

  return (
    <div className="flex items-center" onClick={(e) => e.stopPropagation()}>
      {i18n.t('Go to page')}
      <Input className="mx-2" style={{ width: 80 }} value={value} onChange={handleChange} onPressEnter={jump} />
      <Button
        type="primary"
        size="small"
        icon={<ErdaIcon type="enter" onClick={jump} fill="white" className="relative top-0.5" />}
      />
    </div>
  );
};

export default Pagination;
