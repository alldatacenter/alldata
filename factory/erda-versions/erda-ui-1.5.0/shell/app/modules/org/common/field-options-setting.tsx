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
import { Table, Input } from 'antd';
import i18n from 'i18n';
import { Icon as CustomIcon } from 'common';
import { produce } from 'immer';

const defaultList = [{ name: '', index: 0 }];
interface IProps {
  value?: ISSUE_FIELD.IEnumData[];
  onChange?: (list: ISSUE_FIELD.IEnumData[]) => void;
}
const FieldOptionsSetting = (props: IProps) => {
  const { value = defaultList, onChange } = props;

  const onInputChangeHandle = React.useCallback(
    (dataValue, index) => {
      const newData = dataValue;
      const tempList: ISSUE_FIELD.IEnumData[] = produce(value, (draft: ISSUE_FIELD.IEnumData[]) => {
        draft[index].name = newData;

        if (!draft[index + 1]) {
          draft[index + 1] = { name: '', index: index + 1 };
        }
        let isEmptyData = false;

        if (!dataValue) {
          isEmptyData = true;
        }
        if (isEmptyData && index === draft.length - 2) {
          draft.pop();
        }
      });

      onChange(tempList);
    },
    [onChange, value],
  );

  const columns = [
    {
      title: i18n.t('name'),
      width: '200',
      dataIndex: 'name',
      render: (_value: string, _record: any, index: number) => {
        return (
          <Input
            value={_value}
            placeholder={i18n.t('please enter {name}', { name: i18n.t('name') })}
            onChange={(e: any) => {
              onInputChangeHandle(e.target.value, index);
            }}
          />
        );
      },
    },
    {
      title: i18n.t('operation'),
      dataIndex: 'operation',
      width: 100,
      render: (_value: number, _record: any, index: number) => {
        return index < value.length - 1 ? (
          <CustomIcon
            style={{ cursor: 'cursor-pointer' }}
            type="shanchu"
            onClick={() => {
              const tempList = produce(value, (draft: ISSUE_FIELD.IEnumData[]) => {
                draft.splice(index, 1);
              });
              const list: ISSUE_FIELD.IEnumData[] = tempList.map((item: any, i: number) => {
                return { ...item, index: i };
              });
              onChange(list);
            }}
          />
        ) : undefined;
      },
    },
  ];

  return <Table rowKey="index" dataSource={value} columns={columns} pagination={false} scroll={{ x: '100%' }} />;
};

export default FieldOptionsSetting;
