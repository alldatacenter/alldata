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
import { ContractiveFilter } from 'common';
import { useMount } from 'react-use';

const CP_Filter = (props: CP_FILTER.Props) => {
  const { state, execOperation, operations, props: configProps, customOp } = props;
  const { delay, visible = true, fullWidth = false, className } = configProps || {};

  const [conditions, setConditions] = React.useState([] as CP_FILTER.Condition[]);
  const conditionsRef = React.useRef(null as any);

  const onConditionsChange = React.useCallback((c: CP_FILTER.Condition[]) => {
    setConditions(c);
  }, []);

  React.useEffect(() => {
    conditionsRef.current = conditions;
  }, [conditions]);

  const { values, conditions: stateConditions } = state || {};

  useMount(() => {
    customOp?.onFilterChange && customOp.onFilterChange(state);
  });

  React.useEffect(() => {
    customOp?.onFilterChange && customOp.onFilterChange(state);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [values]);

  const onChange = (value: Obj, changedKey?: string) => {
    execOperation(operations?.filter, { values: value, conditions: conditionsRef.current, changedKey });
    customOp?.onFilterChange && customOp.onFilterChange(value, changedKey);
  };

  const onQuickOperation = ({ key, value }: { key: string; value: any }) => {
    const curOperation = operations?.[key];
    if (curOperation.fillMeta) {
      execOperation(curOperation, value);
    } else {
      execOperation(curOperation, { values, conditions: conditionsRef.current });
    }
  };

  if (!visible) {
    return null;
  }

  return (
    <ContractiveFilter
      conditions={stateConditions as any}
      values={values}
      delay={delay || 1000}
      onChange={onChange}
      onQuickOperation={onQuickOperation}
      onConditionsChange={onConditionsChange}
      fullWidth={fullWidth}
      className={className}
    />
  );
};

export default CP_Filter;
