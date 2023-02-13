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
import { EditList } from 'common';
import { map, last } from 'lodash';
import { useUpdateEffect } from 'react-use';

const empty = [] as any[];
const CP_EDIT_LIST = (props: CP_EDIT_LIST.Props) => {
  const { props: configProps, state, execOperation, operations, updateState } = props;
  const { visible = true, temp = empty, ...rest } = configProps || {};
  const { list = empty } = state || {};
  const [useTemp, setUseTemp] = React.useState([] as any[]);
  const [value, setValue] = React.useState(list as any[]);
  const [saved, setSaved] = React.useState(true);
  const savedRef = React.useRef(null as any);

  React.useEffect(() => {
    savedRef.current = saved;
  }, [saved]);

  useUpdateEffect(() => {
    setValue(list || []);
  }, [list]);

  React.useEffect(() => {
    setUseTemp(
      map(temp, (item) => {
        const { render } = item;
        if (render?.type === 'inputSelect') {
          const p = {} as Obj;
          const { operations: rOps, valueConvertType, props: rProps, ...renderRest } = render || {};
          if (rOps?.onSelectOption) {
            p.onLoadData = (_selectOpt: any) => {
              execOperation(rOps.onSelectOption, _selectOpt);
            };
          }
          p.valueConvert = (str: string[]) => {
            let v = str.join('');
            switch (valueConvertType) {
              case 'last':
                v = last(str) as string;
                break;
              default:
                break;
            }
            return v;
          };

          return {
            ...item,
            render: { ...renderRest, props: { ...rProps, ...p } },
          };
        }
        return { ...item };
      }),
    );
  }, [execOperation, temp]);

  if (!visible) return null;

  const onSave = (val: Obj[]) => {
    if (!savedRef.current && operations?.save && operations.save?.disabled !== true) {
      execOperation(operations.save, { list: val });
      setSaved(true);
    }
  };

  const onChange = (val: Obj[]) => {
    setValue(val);
    setSaved(false);
    updateState({ list: val });
  };

  return (
    <div className="dice-cp">
      <EditList {...rest} value={value} dataTemp={useTemp} onChange={onChange} onBlurSave={onSave} />
    </div>
  );
};

export default CP_EDIT_LIST;
