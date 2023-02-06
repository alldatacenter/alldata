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
import { ConfigurableFilter as Filter } from 'common';

const defaultProcessField = (field: CP_CONFIGURABLE_FILTER.Condition) => {
  return field;
};

const ConfigurableFilter = ({
  state,
  props,
  data,
  customOp,
  execOperation,
  updateState,
  operations,
}: CP_CONFIGURABLE_FILTER.Props) => {
  const { processField = defaultProcessField } = props || {};
  const { values = {}, selectedFilterSet } = state || {};
  const { conditions = [], filterSet = [] } = data || {};
  const { onFilterChange } = customOp || {};

  React.useEffect(() => {
    onFilterChange?.(state);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [state]);

  const onFilter = (filterValues: Obj) => {
    execOperation(
      { key: 'filter', ...operations?.filter, clientData: { values: filterValues } },
      { values: filterValues },
    );
  };

  const onDeleteFilter = (filter: Obj) => {
    execOperation({ key: 'deleteFilterSet', ...operations?.deleteFilterSet, clientData: { dataRef: filter } });
  };

  const onSaveFilter = (label: string, val: Obj) => {
    execOperation(
      { key: 'saveFilterSet', ...operations?.saveFilterSet, clientData: { values: val, label } },
      { values: val },
    );
  };

  return (
    <Filter
      value={values}
      defaultConfig={selectedFilterSet}
      fieldsList={conditions}
      configList={filterSet}
      onFilter={onFilter}
      onDeleteFilter={onDeleteFilter}
      onSaveFilter={onSaveFilter}
      processField={processField}
    />
  );
};

export default ConfigurableFilter;
