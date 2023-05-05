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
import { Collapse, Input, Tooltip, Tag } from 'antd';
import { map, isEmpty, get } from 'lodash';
import { EmptyHolder } from 'common';
import SelectVersion from './select-version';
import './api-menu.scss';
import i18n from 'i18n';

const colorMap = {
  get: '#8DB36C',
  post: '#6CB38B',
  put: '#498E9E',
  delete: '#DE5757',
  patch: '#4E6097',
};

const { Panel } = Collapse;
interface IProps {
  list: Record<string, any>;
  onChange: (key: string, api: any) => void;
  onChangeVersion: (id: number) => void;
}
const ApiMenu = ({ list, onChange, onChangeVersion }: IProps) => {
  const [expandKey, setExpandKey] = React.useState('');
  const [menuList, setMenuList] = React.useState({});
  const [active, setActive] = React.useState('');
  const [filterKey, setFilterKey] = React.useState('');
  React.useEffect(() => {
    if (!isEmpty(list)) {
      const initKey = Object.keys(list)[0];
      const { _method, _path, ...rest } = get(list, `${initKey}[0]`, {});
      const initActive = `${_method}${_path}`;
      onChange(initActive, { _method, _path, ...rest });
      setMenuList(list);
      setExpandKey(initKey);
      setActive(initActive);
    }
    return () => {
      setMenuList({});
      setExpandKey('');
      setActive('');
    };
  }, [list, onChange]);

  const handleFilterApi = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { value } = e.target;
    setFilterKey(value);
    filterApi(value);
  };
  const filterApi = (value: string) => {
    const key = value.toLowerCase();
    const newTagMap = {};
    map(list, (apiList: any[], tagName: string) => {
      const newList = apiList.filter((api: any) => {
        return api._path.toLowerCase().includes(key) || (api.summary || '').toLowerCase().includes(key);
      });
      if (newList.length) {
        newTagMap[tagName] = newList;
      }
    });
    setMenuList(newTagMap);
    setExpandKey(Object.keys(newTagMap)[0]);
  };
  const handleSelectVersion = (id: number) => {
    onChangeVersion(id);
    setFilterKey('');
    filterApi('');
  };
  const handleClick = React.useCallback(
    (key: string, api: any) => {
      setActive(key);
      onChange && onChange(key, api);
    },
    [onChange],
  );
  const menu = React.useMemo(() => {
    if (isEmpty(menuList)) {
      return (
        <div className="mt-8">
          <EmptyHolder relative style={{ justifyContent: 'start' }} />
        </div>
      );
    }
    return (
      <Collapse
        className="api-group-list"
        accordion
        bordered={false}
        activeKey={expandKey}
        onChange={(k: any) => setExpandKey(k)}
      >
        {map(menuList, (apiList: any[], tagName: string) => {
          return (
            <Panel
              className="api-group-list-item"
              header={<span className="font-medium">{tagName}</span>}
              key={tagName}
            >
              <ul className="api-group">
                {map(apiList, (api: any) => {
                  const { _method, _path, summary } = api;
                  const key = _method + _path;
                  return (
                    <Tooltip key={key} title={_path} placement="right">
                      <li
                        onClick={() => {
                          handleClick(key, api);
                        }}
                        className={key === active ? 'active' : ''}
                      >
                        <div className="method-wrapper">
                          <Tag color={colorMap[_method] || '#975FA0'}>{_method.toUpperCase()}</Tag>
                        </div>
                        <div className="api-summary nowrap text-normal">{summary || _path}</div>
                      </li>
                    </Tooltip>
                  );
                })}
              </ul>
            </Panel>
          );
        })}
      </Collapse>
    );
  }, [active, expandKey, handleClick, menuList]);
  return (
    <>
      <div>
        <Input.Search
          className="mb-3"
          value={filterKey}
          placeholder={i18n.t('default:search by path or description')}
          onChange={handleFilterApi}
        />
        <SelectVersion onChangeVersion={handleSelectVersion} />
      </div>
      {menu}
    </>
  );
};

export default React.memo(ApiMenu);
