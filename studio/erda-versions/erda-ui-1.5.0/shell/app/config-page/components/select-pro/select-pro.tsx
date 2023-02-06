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

/**
 * Created by 含光<jiankang.pjk@alibaba-inc.com> on 2021/2/4 14:52.
 */
import React from 'react';
import { Select, Modal } from 'antd';
import { omit, map, debounce } from 'lodash';
import { SelectProps } from 'core/common/interface';

const { Option } = Select;
const customizeProps = ['renderType'];

const SelectPro = (props: CP_SELECT_PRO.Props) => {
  const [selectVal, setSelectVal] = React.useState('');
  const { props: configProps, state: propsState = {}, data, operations, execOperation } = props;
  const { renderType } = configProps || {};
  const { wait = 200 } = operations.onSearch || {};

  React.useEffect(() => {
    setSelectVal(propsState?.value);
  }, [propsState]);

  const handleChange = (val: any) => {
    const { confirm } = operations.onChange || {};
    if (confirm) {
      Modal.confirm({
        title: confirm?.title,
        content: confirm?.subTitle,
        onOk: () => {
          setSelectVal(val);
          execOperation(operations?.onChange, val ?? null);
        },
      });
    } else {
      setSelectVal(val);
      execOperation(operations?.onChange, val ?? null);
    }
  };

  const renderOption = React.useCallback(
    (item) => {
      let option: React.ReactNode = null;
      switch (renderType) {
        case 'apiProto':
          {
            const { id, method, assetName, version, operationID, path } = item;
            const tips = [assetName, version, operationID].filter((t) => !!t);
            option = (
              <Option value={id} key={id} label={`${method} ${path}`}>
                <div className="text-sub nowrap">{tips.join(' / ')}</div>
                <div className="text-normal">
                  {method} {path}
                </div>
              </Option>
            );
          }
          break;
        default:
          option = (
            <Option value={item.id} key={item.id}>
              {item.name}
            </Option>
          );
      }
      return option;
    },
    [renderType],
  );

  const content = React.useMemo(() => {
    return map(data?.list || [], renderOption);
  }, [data, renderOption]);

  const handleSearch = React.useCallback(
    debounce((val: string) => {
      execOperation(operations?.onSearch, val);
    }, wait),
    [wait],
  );

  const restProps = React.useMemo(() => {
    const obj: SelectProps<any> = omit(configProps, customizeProps);
    if (configProps.showSearch) {
      obj.onSearch = handleSearch;
    }
    return obj;
  }, [configProps, handleSearch]);

  return (
    <>
      <Select filterOption={false} {...restProps} value={selectVal} onChange={handleChange}>
        {content}
      </Select>
    </>
  );
};

export default SelectPro;
