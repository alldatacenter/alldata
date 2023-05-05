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
import { Input, InputNumber, Select, Tooltip } from 'antd';
import { isEmpty, map, remove, set, cloneDeep } from 'lodash';
import { slaUnitMap } from 'apiManagePlatform/pages/access-manage/components/config';
import { ErdaIcon } from 'common';
import i18n from 'i18n';
import './sla.scss';

const { Group: InputGroup } = Input;
const { Option } = Select;
const defaultData = {
  limit: undefined,
  unit: undefined,
} as any as API_ACCESS.BaseSlaLimit;
interface IProps {
  value?: API_ACCESS.BaseSlaLimit[];
  mode: 'single' | 'multiple';
  onChange?: (data: API_ACCESS.BaseSlaLimit[]) => void;
}

const Limit = (props: IProps) => {
  const { value, mode } = props;
  const [renderData, setValue] = React.useState<API_ACCESS.BaseSlaLimit[]>([]);

  React.useEffect(() => {
    const curVal = isEmpty(value) ? [{ ...defaultData }] : (cloneDeep(value) as API_ACCESS.BaseSlaLimit[]);
    setValue(curVal);
  }, [value]);

  const handleChange = (index: number, name: 'limit' | 'unit', v: string | number | undefined) => {
    const newData = cloneDeep(renderData);
    set(newData[index], name, v);
    setValue(newData);
    props.onChange && props.onChange(newData);
  };

  const handleAddOne = () => {
    const lastItem = renderData[renderData.length - 1];
    if (lastItem.limit && lastItem.unit) {
      const newData = [...cloneDeep(renderData), { ...defaultData }];
      setValue(newData);
      props.onChange && props.onChange(newData);
    }
  };

  const handleDropOne = (index: number) => {
    const newData = cloneDeep(renderData);
    remove(newData, (_v, idx) => idx === index);
    setValue(newData);
    props.onChange && props.onChange(newData);
  };

  return (
    <>
      {renderData.map(({ limit, unit }, index) => {
        return (
          <InputGroup compact key={String(index)} className="mb-1">
            <InputNumber
              placeholder={i18n.t('please enter')}
              min={1}
              step={1}
              max={999999999999999}
              value={limit}
              style={{ width: mode === 'multiple' ? '65%' : '80%' }}
              onChange={(v) => {
                handleChange(index, 'limit', v);
              }}
            />
            <Select
              placeholder={i18n.t('please select')}
              style={{ width: '20%' }}
              value={unit}
              onChange={(v) => {
                handleChange(index, 'unit', v as string);
              }}
            >
              {map(slaUnitMap, (name, k) => (
                <Option value={k} key={k}>
                  {name}
                </Option>
              ))}
            </Select>
            {mode === 'multiple' ? (
              <div className="sla-limit-operation">
                <div className="flex justify-between items-center pl-3">
                  <Tooltip title={i18n.t('add {name}', { name: i18n.t('request limit') })}>
                    <ErdaIcon type="add-one" onClick={handleAddOne} size="20" />
                  </Tooltip>
                  {index !== 0 ? (
                    <Tooltip title={i18n.t('delete')}>
                      <ErdaIcon
                        type="reduce-one"
                        onClick={() => {
                          handleDropOne(index);
                        }}
                        size="20"
                      />
                    </Tooltip>
                  ) : null}
                </div>
              </div>
            ) : null}
          </InputGroup>
        );
      })}
    </>
  );
};

export default Limit;
