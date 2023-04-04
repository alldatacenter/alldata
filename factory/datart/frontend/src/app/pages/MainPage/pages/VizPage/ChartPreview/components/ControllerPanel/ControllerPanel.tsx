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

import { Col, Form, Row } from 'antd';
import {
  ChartDataSectionType,
  ControllerFacadeTypes,
  ControllerVisibilityTypes,
} from 'app/constants';
import { ExecuteToken } from 'app/pages/SharePage/slice/types';
import {
  ChartConfig,
  ChartDataSectionField,
  FilterFieldAction,
} from 'app/types/ChartConfig';
import { ChartDTO } from 'app/types/ChartDTO';
import { getColumnRenderName } from 'app/utils/chartHelper';
import { updateByKey } from 'app/utils/mutation';
import { FilterSqlOperator } from 'globalConstants';
import debounce from 'lodash/debounce';
import { FC, memo, useEffect, useState } from 'react';
import styled from 'styled-components/macro';
import { FONT_SIZE_LABEL, SPACE, SPACE_MD } from 'styles/StyleConstants';
import { isEmptyArray } from 'utils/object';
import Filters, { PresentControllerFilterProps } from './components';

const AUTO_CONTROL_PANEL_COLS = {
  xs: 24,
  sm: 24,
  md: 12,
  lg: 6,
  xl: 6,
  xxl: 4,
};

const ControllerPanel: FC<{
  viewId?: string;
  view?: ChartDTO['view'];
  chartConfig?: ChartConfig;
  executeToken?: Record<string, ExecuteToken>;
  onChange: (type, payload) => void;
}> = memo(({ viewId, view, chartConfig, executeToken, onChange }) => {
  const [filters, setFilters] = useState<ChartDataSectionField[]>([]);
  useEffect(() => {
    const newFilters = (chartConfig?.datas || [])
      .filter(c => c.type === ChartDataSectionType.Filter)
      .flatMap(c => c.rows || []);
    setFilters(newFilters);
  }, [chartConfig?.datas]);

  const checkFilterVisibility = (filter?: FilterFieldAction) => {
    if (!filter?.visibility) {
      return false;
    }
    if (filter.visibility === ControllerVisibilityTypes.Show) {
      return true;
    }
    if (
      typeof filter.visibility === 'object' &&
      filter.visibility?.visibility === ControllerVisibilityTypes.Condition
    ) {
      const { fieldUid, relation, value } = filter.visibility;
      const targetFilter = filters.find(f => f.uid === fieldUid);
      if (targetFilter) {
        const targetFilterValue = targetFilter?.filter?.condition?.value;
        if (targetFilterValue && Array.isArray(targetFilterValue)) {
          const selectedValues = (
            targetFilterValue as Array<{
              isSelected: boolean;
              key: string;
            }>
          ).filter(v => v.isSelected);
          switch (relation) {
            case FilterSqlOperator.Equal:
              return !!selectedValues.find(sv => sv.key === value);
            case FilterSqlOperator.NotEqual:
              return !selectedValues.find(sv => sv.key === value);
          }
        }
      }
      return false;
    }
  };

  const handleConditionChange = debounce((condition, config, index) => {
    const newConfig = updateByKey(config, 'filter', {
      ...config?.filter,
      condition,
    });

    onChange('data', {
      ancestors: [index],
      value: newConfig,
      needRefresh: true,
    });
  }, 500);

  const renderComponentByFacade = (
    index: number,
    config?: ChartDataSectionField,
  ) => {
    const props: PresentControllerFilterProps = {
      viewId,
      view,
      executeToken,
      condition: config?.filter?.condition,
      options: config?.filter?.facade,
      onConditionChange: condition =>
        handleConditionChange(condition, config, index),
    };
    const facade =
      typeof config?.filter?.facade === 'object'
        ? config?.filter?.facade?.facade
        : config?.filter?.facade;

    switch (facade) {
      case ControllerFacadeTypes.DropdownList:
        return <Filters.DropdownListFilter {...props} />;
      case ControllerFacadeTypes.MultiDropdownList:
        return <Filters.MultiDropdownListFilter {...props} />;
      case ControllerFacadeTypes.RangeTimePicker:
        return <Filters.RangeTimePickerFilter {...props} />;
      case ControllerFacadeTypes.RangeValue:
        return <Filters.RangValueFilter {...props} />;
      case ControllerFacadeTypes.Text:
        return <Filters.TextFilter {...props} />;
      case ControllerFacadeTypes.Value:
        return <Filters.ValueFilter {...props} />;
      case ControllerFacadeTypes.Time:
        return <Filters.TimeFilter {...props} />;
      case ControllerFacadeTypes.RadioGroup:
        return <Filters.RadioGroupFilter {...props} />;
      case ControllerFacadeTypes.Slider:
        return <Filters.SliderFilter {...props} />;
      case ControllerFacadeTypes.Tree:
        return <Filters.TreeFilter {...props} />;
      default:
        return <Filters.TextFilter {...props} />;
    }
  };

  const renderControls = () => {
    const validFilters = filters
      ?.filter(config => checkFilterVisibility(config.filter))
      .map((config, index) => {
        return (
          <Col
            key={config.uid}
            {...(config.filter?.width === 'auto'
              ? { ...AUTO_CONTROL_PANEL_COLS }
              : { span: config.filter?.width })}
          >
            <Form.Item label={getColumnRenderName(config)}>
              {renderComponentByFacade(index, config)}
            </Form.Item>
          </Col>
        );
      });

    if (isEmptyArray(validFilters)) {
      return null;
    }

    return (
      <Wrapper layout="vertical">
        <ControllerBlock>{validFilters}</ControllerBlock>
      </Wrapper>
    );
  };

  return renderControls();
});

export default ControllerPanel;

const Wrapper = styled(Form)`
  padding: ${SPACE_MD};
  background-color: ${p => p.theme.componentBackground};
  border-bottom: 1px solid ${p => p.theme.borderColorSplit};

  .ant-form-item {
    margin-bottom: ${SPACE_MD};
  }

  &.ant-form-vertical .ant-form-item-label {
    padding: 0;
  }

  .ant-form-item-label > label {
    font-size: ${FONT_SIZE_LABEL};
    color: ${p => p.theme.textColor};
  }
`;

const ControllerBlock = styled(Row)`
  .ant-col {
    padding: 0 ${SPACE};
  }
`;
