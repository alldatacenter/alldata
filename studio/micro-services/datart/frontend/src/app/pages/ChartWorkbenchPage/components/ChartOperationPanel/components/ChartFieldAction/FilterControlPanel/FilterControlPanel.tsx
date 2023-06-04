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

import { Input, Select } from 'antd';
import { FormItemEx } from 'app/components';
import {
  AggregateFieldActionType,
  ChartDataViewFieldCategory,
  ControllerVisibilityTypes,
  DataViewFieldType,
} from 'app/constants';
import useI18NPrefix, { I18NComponentProps } from 'app/hooks/useI18NPrefix';
import { ConditionBuilder } from 'app/models/ChartFilterCondition';
import { ChartDataConfig, ChartDataSectionField } from 'app/types/ChartConfig';
import ChartDataSetDTO from 'app/types/ChartDataSet';
import ChartDataView from 'app/types/ChartDataView';
import { getColumnRenderName } from 'app/utils/chartHelper';
import { updateBy } from 'app/utils/mutation';
import { CONTROLLER_WIDTH_OPTIONS } from 'globalConstants';
import { FC, memo, useRef, useState } from 'react';
import styled from 'styled-components/macro';
import { FilterOptionForwardRef } from '.';
import CategoryConditionConfiguration from './CategoryConditionConfiguration';
import DateConditionConfiguration from './DateConditionConfiguration';
import FilterAggregateConfiguration from './FilterAggregateConfiguration';
import FilterFacadeConfiguration from './FilterFacadeConfiguration';
import FilterVisibilityConfiguration from './FilterVisibilityConfiguration';
import ValueConditionConfiguration from './ValueConditionConfiguration';

const FilterControlPanel: FC<
  {
    config: ChartDataSectionField;
    dataset?: ChartDataSetDTO;
    dataView?: ChartDataView;
    dataConfig?: ChartDataConfig;
    aggregation?: boolean;
    onConfigChange: (
      config: ChartDataSectionField,
      needRefresh?: boolean,
    ) => void;
    fetchDataByField?: (fieldId) => Promise<string[]>;
    form;
  } & I18NComponentProps
> = memo(
  ({
    config,
    dataset,
    dataView,
    i18nPrefix,
    dataConfig,
    aggregation,
    onConfigChange,
    fetchDataByField,
    form,
  }) => {
    const formItemStyles = {
      labelCol: { span: 4 },
      wrapperCol: { span: 20 },
    };

    const filterOptionRef = useRef<FilterOptionForwardRef>(null);
    const customizeI18NPrefix = !!i18nPrefix ? i18nPrefix : 'viz.common.filter';
    const t = useI18NPrefix(customizeI18NPrefix);
    const [alias, setAlias] = useState(config.alias);
    const [aggregate, setAggregate] = useState(() => {
      if (Boolean(dataConfig?.disableAggregate) || aggregation === false) {
        return AggregateFieldActionType.None;
      }
      if (
        config.category === ChartDataViewFieldCategory.AggregateComputedField ||
        config.category === ChartDataViewFieldCategory.ComputedField
      ) {
        return AggregateFieldActionType.None;
      }
      if (config.aggregate) {
        return config.aggregate;
      } else if (
        config.type === DataViewFieldType.STRING ||
        config.type === DataViewFieldType.DATE
      ) {
        return AggregateFieldActionType.None;
      } else if (config.type === DataViewFieldType.NUMERIC) {
        return AggregateFieldActionType.Sum;
      }
    });

    const [filter, setFilter] = useState(
      config.filter || {
        visibility: ControllerVisibilityTypes.Hide,
        condition: new ConditionBuilder()
          .setName(config.colName)
          .setSqlType(config.type)
          .asSelf(),
        facade: undefined,
        width: CONTROLLER_WIDTH_OPTIONS[0].value,
      },
    );

    const handleConfigChange = (alias, aggregate, filter) => {
      const newConfig = updateBy(config, draft => {
        draft.alias = alias;
        draft.aggregate = aggregate;
        draft.filter = filter;
      });
      onConfigChange(newConfig);
    };

    const handleNameChange = name => {
      const newAlias = updateBy(config.alias || {}, draft => {
        draft.name = name;
      });
      setAlias(newAlias);
      handleConfigChange(newAlias, aggregate, filter);
    };

    const handleAggregateTypeChange = aggregate => {
      setAggregate(aggregate);
      const newFilter = updateBy(filter || {}, draft => {
        draft.condition = undefined;
      });
      setFilter(newFilter);
      handleConfigChange(alias, aggregate, newFilter);
      form?.setFields([
        {
          name: 'filterOption',
          value: undefined,
        },
      ]);
    };

    const handleWidthOptionChange = width => {
      const newFilter = updateBy(filter || {}, draft => {
        draft.width = width;
      });
      setFilter(newFilter);
      handleConfigChange(alias, aggregate, newFilter);
    };

    const handleVisibilityChange = visibility => {
      const newFilter = updateBy(filter || {}, draft => {
        draft.visibility = visibility;
      });
      setFilter(newFilter);
      handleConfigChange(alias, aggregate, newFilter);
    };

    const handleFacadeChange = facade => {
      const newFilter = updateBy(filter || {}, draft => {
        draft.facade = facade;
      });
      setFilter(newFilter);
      handleConfigChange(alias, aggregate, newFilter);
    };

    const handleConditionFilterChange = condition => {
      const newFilter = updateBy(filter || {}, draft => {
        draft.condition = condition;
      });
      setFilter(newFilter);
      handleConfigChange(alias, aggregate, newFilter);
    };

    const getVisibilityOtherFilters = () => {
      return (
        dataConfig?.rows?.filter(
          c => c.uid !== config.uid && c.type === DataViewFieldType.STRING,
        ) || []
      );
    };

    const renderConditionConfigurationByModel = () => {
      const filterProps = {
        colName: config?.colName,
        ref: filterOptionRef,
        dataset,
        dataView,
        condition: new ConditionBuilder(filter?.condition).asSelf(),
        onChange: handleConditionFilterChange,
      };
      if (
        config.type === DataViewFieldType.STRING &&
        aggregate === AggregateFieldActionType.None
      ) {
        return (
          <CategoryConditionConfiguration
            {...filterProps}
            i18nPrefix={customizeI18NPrefix + '.category'}
            fetchDataByField={fetchDataByField}
          />
        );
      } else if (
        config.type === DataViewFieldType.STRING &&
        aggregate !== AggregateFieldActionType.None
      ) {
        return (
          <ValueConditionConfiguration
            {...filterProps}
            i18nPrefix={customizeI18NPrefix + '.value'}
          />
        );
      } else if (config.type === DataViewFieldType.NUMERIC) {
        return (
          <ValueConditionConfiguration
            {...filterProps}
            i18nPrefix={customizeI18NPrefix + '.value'}
          />
        );
      } else if (config.type === DataViewFieldType.DATE) {
        return (
          <DateConditionConfiguration
            {...filterProps}
            i18nPrefix={customizeI18NPrefix + '.date'}
          />
        );
      }
    };

    const filterOptionValidator = args => {
      // NOTE: should be return null to trigger NOT NULL validation when failed
      return filterOptionRef.current?.onValidate(args) ? args : null;
    };

    return (
      <StyledFilterController>
        <FormItemEx
          {...formItemStyles}
          label={t('filterName')}
          name="filterName"
          rules={[{ required: true }]}
          initialValue={getColumnRenderName(config)}
        >
          <Input onChange={e => handleNameChange(e.target?.value)} />
        </FormItemEx>
        {config.category === ChartDataViewFieldCategory.Field && aggregation && (
          <FormItemEx
            {...formItemStyles}
            label={t('filterAggregate')}
            name="filterAggregate"
            rules={[{ required: true }]}
            initialValue={aggregate}
          >
            <FilterAggregateConfiguration
              config={config}
              aggregate={aggregate}
              onChange={handleAggregateTypeChange}
            />
          </FormItemEx>
        )}
        <FormItemEx
          {...formItemStyles}
          label={t('filterOption')}
          name="filterOption"
          initialValue={filter?.condition?.value}
          getValueFromEvent={filterOptionValidator}
          rules={[{ required: true }]}
        >
          {renderConditionConfigurationByModel()}
        </FormItemEx>
        <FormItemEx
          {...formItemStyles}
          label={t('filterVisibility')}
          name="filterVisibility"
          rules={[{ required: true }]}
          initialValue={filter?.visibility}
          getValueFromEvent={args => {
            return typeof args === 'object' ? args?.value : args;
          }}
        >
          <FilterVisibilityConfiguration
            visibility={filter?.visibility}
            otherFilters={getVisibilityOtherFilters()}
            onChange={handleVisibilityChange}
          />
        </FormItemEx>
        {filter?.visibility !== ControllerVisibilityTypes.Hide && (
          <>
            <FormItemEx
              {...formItemStyles}
              label={t('facade')}
              name="filterFacade"
              rules={[{ required: true }]}
              initialValue={filter?.facade}
            >
              <FilterFacadeConfiguration
                i18nPrefix={customizeI18NPrefix}
                category={config.category as string}
                condition={new ConditionBuilder(filter?.condition).asSelf()}
                facade={filter?.facade}
                onChange={handleFacadeChange}
              />
            </FormItemEx>
            <FormItemEx
              {...formItemStyles}
              label={t('widthOption')}
              name="filterWidth"
              rules={[{ required: true }]}
              initialValue={filter?.width}
            >
              <Select value={filter?.width} onChange={handleWidthOptionChange}>
                {CONTROLLER_WIDTH_OPTIONS.map(({ label, value }) => (
                  <Select.Option key={value} value={value}>
                    {label}
                  </Select.Option>
                ))}
              </Select>
            </FormItemEx>
          </>
        )}
      </StyledFilterController>
    );
  },
);

export default FilterControlPanel;

const StyledFilterController = styled.div`
  & > .ant-row {
    align-items: center;
    padding: 5px 5px 5px 0;

    > .ant-col {
      padding-left: 40px;

      .ant-select,
      input {
        width: 200px;
      }
    }
  }
`;
