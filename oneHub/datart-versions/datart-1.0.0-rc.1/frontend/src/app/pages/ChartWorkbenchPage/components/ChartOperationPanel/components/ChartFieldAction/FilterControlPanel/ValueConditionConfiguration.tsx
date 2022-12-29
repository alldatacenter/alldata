/**
 * Datart
 *
 * Copyright 2021
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { FilterConditionType } from 'app/constants';
import { I18NComponentProps } from 'app/hooks/useI18NPrefix';
import ChartFilterCondition, {
  ConditionBuilder,
} from 'app/models/ChartFilterCondition';
import MultiFilterRow from 'app/pages/ChartWorkbenchPage/components/ChartOperationPanel/components/ChartFieldAction/FilterAction/MultiFilterRow';
import SingleFilterRow from 'app/pages/ChartWorkbenchPage/components/ChartOperationPanel/components/ChartFieldAction/FilterAction/SingleFilterRow';
import { FilterSqlOperator } from 'globalConstants';
import {
  forwardRef,
  ForwardRefRenderFunction,
  useImperativeHandle,
  useState,
} from 'react';
import { isEmpty, isEmptyArray } from 'utils/object';
import { FilterOptionForwardRef } from '.';

const ValueConditionConfiguration: ForwardRefRenderFunction<
  FilterOptionForwardRef,
  {
    condition?: ChartFilterCondition;
    onChange: (condition: ChartFilterCondition) => void;
  } & I18NComponentProps
> = ({ i18nPrefix, condition, onChange: onConditionChange }, ref) => {
  const [curFilter, setCurFilter] = useState<ChartFilterCondition>(
    new ConditionBuilder(condition).asSelf(),
  );

  useImperativeHandle(ref, () => ({
    onValidate: (args: ChartFilterCondition) => {
      if (isEmpty(args?.operator)) {
        return false;
      }
      if (
        [FilterSqlOperator.Between, FilterSqlOperator.NotBetween].includes(
          args?.operator as FilterSqlOperator,
        )
      ) {
        return args?.value?.filter(v => !isEmpty(v))?.length === 2;
      } else if (
        [
          FilterSqlOperator.Equal,
          FilterSqlOperator.NotEqual,
          FilterSqlOperator.GreaterThanOrEqual,
          FilterSqlOperator.LessThanOrEqual,
          FilterSqlOperator.LessThan,
          FilterSqlOperator.GreaterThan,
        ].includes(args?.operator as FilterSqlOperator)
      ) {
        return (
          !isEmpty(args?.value) &&
          !isEmptyArray(args?.value?.filter(v => !isEmpty(v)))
        );
      } else if (
        [FilterSqlOperator.Null, FilterSqlOperator.NotNull].includes(
          args?.operator as FilterSqlOperator,
        )
      ) {
        return true;
      }
      return false;
    },
  }));

  const handleFilterChanged = (filter: ChartFilterCondition) => {
    setCurFilter(filter);
    onConditionChange && onConditionChange(filter);
  };

  const handleAddBrotherFilter = () => {
    const filter = curFilter;
    filter.appendChild();
    setCurFilter(filter);
    handleFilterChanged(filter);
  };

  const handleDeleteSelfFilter = () => {
    let newFilter = new ConditionBuilder(curFilter).asFilter();
    handleFilterChanged(newFilter);
  };

  const renderFilters = () => {
    if (curFilter?.type === FilterConditionType.Relation) {
      return (
        <MultiFilterRow
          rowName={'root'}
          condition={curFilter}
          onConditionChange={handleFilterChanged}
        />
      );
    }
    return (
      <SingleFilterRow
        rowName={'root'}
        condition={curFilter}
        onAddBrotherFilter={handleAddBrotherFilter}
        onDeleteSelfFilter={handleDeleteSelfFilter}
        onConditionChange={handleFilterChanged}
      />
    );
  };

  return renderFilters();
};

export default forwardRef(ValueConditionConfiguration);
