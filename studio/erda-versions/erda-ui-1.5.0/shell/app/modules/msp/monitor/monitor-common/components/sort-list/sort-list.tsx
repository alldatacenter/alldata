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

import { map, floor } from 'lodash';
import React from 'react';
import { Spin, Tooltip } from 'antd';
import { Icon as CustomIcon, EmptyHolder } from 'common';
import './sort-list.scss';

interface IProps {
  data: {
    loading: boolean | undefined;
    list: IData[];
  };
  onClickItem?: (args: string) => void;
}

interface IData {
  name: string;
  value: string | number;
  unit?: string;
  unitType?: string;
}

const SortList = (props: IProps) => {
  const { onClickItem, data } = props;

  const { loading, list } = data || {};
  const [chosen, setChosen] = React.useState('');

  const handleClick = (name: string) => {
    if (onClickItem) {
      setChosen(name);
    }
  };

  const handleClose = (e?: any) => {
    e && e.stopPropagation();
    setChosen('');
  };

  React.useEffect(() => {
    onClickItem && onClickItem(chosen);
  }, [chosen, onClickItem]);

  const renderList = (dataList: IData[]) => {
    if (dataList && dataList.length) {
      return map(dataList, (item, index) => {
        const { name, value, unit } = item;
        let background = {};
        const isChosen = chosen === name;
        if (!isChosen && unit === '%') {
          background = {
            background: `linear-gradient(to right, rgba(146, 225, 255, 0.34) ${value}%, #ECEEF6 ${value}%)`,
          };
        }
        return (
          <Tooltip key={index} title={name} placement="right" overlayClassName="tooltip-word-break">
            <li
              onClick={() => handleClick(name)}
              className={`sort-list-item ${isChosen ? 'active' : ''} ${onClickItem === null ? '' : 'cursor-pointer'}`}
              style={background}
            >
              <span className="name">
                {index + 1}. {name}
              </span>
              <span className="value">{`${floor(value as number, 2)} ${unit}`}</span>
              {isChosen ? <CustomIcon className="close-icon" type="guanbi-fill" onClick={handleClose} /> : null}
            </li>
          </Tooltip>
        );
      });
    }
    return <EmptyHolder />;
  };

  return (
    <Spin spinning={loading}>
      <ul className="topn-list">{renderList(list)}</ul>
    </Spin>
  );
};

export default SortList;
