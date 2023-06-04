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

import { InputNumber, Row, Select, Space } from 'antd';
import {
  ChartDataViewFieldCategory,
  ControllerFacadeTypes,
  ControllerRadioFacadeTypes,
  FilterConditionType,
} from 'app/constants';
import useI18NPrefix, { I18NComponentProps } from 'app/hooks/useI18NPrefix';
import ChartFilterCondition from 'app/models/ChartFilterCondition';
import { FilterFacade } from 'app/types/ChartConfig';
import { FC, memo, useEffect, useState } from 'react';
import styled from 'styled-components/macro';
import { IsKeyIn } from 'utils/object';

const isDisableSingleDropdownListFacade = condition => {
  let isDisableSingleDropdownList = true;
  if (Array.isArray(condition?.value)) {
    if (IsKeyIn(condition?.value?.[0], 'key')) {
      isDisableSingleDropdownList =
        condition?.value?.filter(n => n.isSelected)?.length > 1;
    }
  }
  return isDisableSingleDropdownList;
};

const getFacadeOptions = (condition, category) => {
  if (!condition || !category) {
    return [];
  }

  if (category === ChartDataViewFieldCategory.Field) {
    switch (condition.type) {
      case FilterConditionType.List:
        if (isDisableSingleDropdownListFacade(condition)) {
          return [ControllerFacadeTypes.MultiDropdownList];
        }
        return [
          ControllerFacadeTypes.MultiDropdownList,
          ControllerFacadeTypes.DropdownList,
          ControllerFacadeTypes.RadioGroup,
        ];
      case FilterConditionType.Customize:
        return [
          ControllerFacadeTypes.MultiDropdownList,
          ControllerFacadeTypes.DropdownList,
          ControllerFacadeTypes.RadioGroup,
        ];
      case FilterConditionType.Condition:
        return [ControllerFacadeTypes.Text];
      case FilterConditionType.RangeValue:
        return [ControllerFacadeTypes.RangeValue, ControllerFacadeTypes.Slider];
      case FilterConditionType.Value:
        return [ControllerFacadeTypes.Value];
      case FilterConditionType.RangeTime:
        return [ControllerFacadeTypes.RangeTimePicker];
      case FilterConditionType.RecommendTime:
        return [ControllerFacadeTypes.RangeTimePicker];
      case FilterConditionType.Tree:
        return [ControllerFacadeTypes.Tree];
    }
  } else if (category === ChartDataViewFieldCategory.Variable) {
    switch (condition.type) {
      case FilterConditionType.Customize:
        return [ControllerFacadeTypes.Text];
      case FilterConditionType.Value:
        return [ControllerFacadeTypes.Value];
      case FilterConditionType.Time:
        return [ControllerFacadeTypes.Time];
    }
  }
  return [];
};

const FilterFacadeConfiguration: FC<
  {
    category?: string;
    facade?: FilterFacade;
    condition?: ChartFilterCondition;
    onChange;
  } & I18NComponentProps
> = memo(
  ({ i18nPrefix, category, facade, condition, onChange: onFacadeChange }) => {
    const t = useI18NPrefix(i18nPrefix);
    const t2 = useI18NPrefix('viz.common.enum.controllerFacadeTypes');
    const [currentFacade, setCurrentFacade] = useState<string | undefined>(
      () => {
        if (typeof facade === 'string') {
          return facade as string;
        } else if (typeof facade === 'object') {
          return facade.facade as string;
        }
      },
    );
    const [radioType, setRedioType] = useState(() => {
      if (typeof facade === 'object') {
        return facade.type;
      }
      return ControllerRadioFacadeTypes.Default;
    });
    const [sliderOptions, setSliderOptions] = useState(() => {
      if (typeof facade === 'object') {
        return [facade.min, facade.max];
      }
    });
    const [facadeOptions, setFacadeOptions] = useState<ControllerFacadeTypes[]>(
      [],
    );

    useEffect(() => {
      const facades = getFacadeOptions(condition, category);
      setFacadeOptions(facades);
      if (
        !!currentFacade &&
        !facades.includes(currentFacade as ControllerFacadeTypes)
      ) {
        setCurrentFacade(undefined);
        handleFacadeChange(undefined);
      } else if (!currentFacade) {
        setCurrentFacade(facades?.[0]);
        handleFacadeChange(facades?.[0]);
      }
      // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [condition, category, currentFacade]);

    const handleFacadeChange = facade => {
      setCurrentFacade(facade);
      onFacadeChange(facade);
    };

    const handleSliderOptionChange = (min, max) => {
      setSliderOptions([min, max]);
      onFacadeChange({ facade: currentFacade, min, max });
    };

    const handleRadioTypeChange = type => {
      setRedioType(type);
      onFacadeChange({ facade: currentFacade, type });
    };

    return (
      <StyledFilterFacadeConfiguration>
        <Row>
          <Space>
            <Select value={currentFacade} onChange={handleFacadeChange}>
              {facadeOptions.map(f => (
                <Select.Option key={f} value={f}>
                  {t2(f)}
                </Select.Option>
              ))}
            </Select>
            {currentFacade === ControllerFacadeTypes.RadioGroup && (
              <Space>
                {t('radioType')}
                <Select value={radioType} onChange={handleRadioTypeChange}>
                  <Select.Option
                    key={ControllerRadioFacadeTypes.Default}
                    value={ControllerRadioFacadeTypes.Default}
                  >
                    {t(ControllerRadioFacadeTypes.Default)}
                  </Select.Option>
                  <Select.Option
                    key={ControllerRadioFacadeTypes.Button}
                    value={ControllerRadioFacadeTypes.Button}
                  >
                    {t(ControllerRadioFacadeTypes.Button)}
                  </Select.Option>
                </Select>
              </Space>
            )}
            {currentFacade === ControllerFacadeTypes.Slider && (
              <Space>
                {t('min')}
                <InputNumber
                  value={sliderOptions?.[0]}
                  onChange={value =>
                    handleSliderOptionChange(value, sliderOptions?.[1])
                  }
                />
                {t('max')}
                <InputNumber
                  value={sliderOptions?.[1]}
                  onChange={value =>
                    handleSliderOptionChange(sliderOptions?.[0], value)
                  }
                />
              </Space>
            )}
          </Space>
        </Row>
      </StyledFilterFacadeConfiguration>
    );
  },
);

export default FilterFacadeConfiguration;

const StyledFilterFacadeConfiguration = styled.div`
  .ant-select {
    width: 200px;
  }
`;
