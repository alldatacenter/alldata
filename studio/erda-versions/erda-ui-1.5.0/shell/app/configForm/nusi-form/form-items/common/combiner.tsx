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
import { map, filter, cloneDeep, isPlainObject, set } from 'lodash';
import { Tooltip } from 'antd';
import { produce } from 'immer';
import i18n from 'i18n';
import { ErdaIcon } from 'common';
import './combiner.scss';

interface IProps<P, O> {
  CombinerItem: any;
  valueFixIn?: (val: P[]) => O[];
  valueFixOut?: (val: O[]) => P[];
  defaultItem?: (props: any) => O;
}

export interface ICompProps<P, O> {
  value: P[];
  onChange: (arg: P[]) => void;
}

const defaultFix = (a: any) => a;
export function createCombiner<P, O>({
  CombinerItem,
  valueFixIn = defaultFix,
  valueFixOut = defaultFix,
  defaultItem,
}: IProps<P, O>) {
  return (props: ICompProps<P, O>) => {
    const { value, onChange, disabled, ...rest } = props;
    const changeData = (val: any) => {
      onChange(valueFixOut(cloneDeep(val)));
    };

    const updateItem = (d: any, index: number) => {
      const newVal = map(valueFixIn(value), (val, idx) => {
        if (isPlainObject(val)) {
          if (index === idx) {
            const curVal = produce(val, (draft: Obj) => {
              const curKey = Object.keys(d)[0];
              set(draft, curKey, d[curKey]);
            });
            return curVal;
          }
          return val;
        } else {
          return index === idx ? d : val;
        }
      });
      changeData(newVal);
    };

    const addItem = () => {
      const _defaultItem = typeof defaultItem === 'function' ? defaultItem(props) : defaultItem;
      changeData([...valueFixIn(value), _defaultItem]);
    };

    const deleteItem = (index: number) => {
      changeData(filter(value, (_, idx) => index !== idx));
    };
    return (
      <div className="dice-form-nusi-combiner-component">
        {map(valueFixIn(value), (item, index) => (
          <CombinerItem
            {...rest}
            disabled={disabled}
            className="combiner-item"
            key={`${index}`}
            data={item}
            updateItem={(d: any) => {
              updateItem(d, index);
            }}
            operation={
              disabled ? (
                <ErdaIcon type="reduce-one" className="combiner-operation not-allowed" />
              ) : (
                <ErdaIcon type="reduce-one" className="combiner-operation" onClick={() => deleteItem(index)} />
              )
            }
          />
        ))}
        {disabled ? (
          <ErdaIcon type="add-one" className="combiner-operation not-allowed" />
        ) : (
          <Tooltip title={i18n.t('common:click to add item')}>
            <ErdaIcon type="add-one" className="combiner-operation" onClick={addItem} />
          </Tooltip>
        )}
      </div>
    );
  };
}
