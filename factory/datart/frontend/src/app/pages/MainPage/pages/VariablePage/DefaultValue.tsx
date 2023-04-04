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

import { CheckOutlined } from '@ant-design/icons';
import {
  Button,
  DatePicker,
  Input,
  InputNumber,
  Select,
  Space,
  Tag,
} from 'antd';
import { DateFormat } from 'app/constants';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { TIME_FORMATTER } from 'globalConstants';
import moment from 'moment';
import { memo, useCallback, useEffect, useState } from 'react';
import styled from 'styled-components/macro';
import { SPACE, SPACE_TIMES } from 'styles/StyleConstants';
import { VariableValueTypes } from './constants';

interface DefaultValueProps {
  type: VariableValueTypes;
  expression: boolean;
  disabled?: boolean;
  value?: null | any[];
  dateFormat?: DateFormat;
  hasDateFormat?: boolean;
  onChangeDateFormat?: (value) => void;
  onChange?: (value) => void;
}

export const DefaultValue = memo(
  ({
    type,
    expression,
    disabled,
    value = [],
    dateFormat,
    hasDateFormat = true,
    onChange,
    onChangeDateFormat,
  }: DefaultValueProps) => {
    const [inputValue, setInputValue] = useState<any>(void 0);
    const t = useI18NPrefix('variable');

    useEffect(() => {
      setInputValue(void 0);
    }, [type]);

    const saveRegular = useCallback(
      (selectedValue?) => {
        let validValue;
        switch (type) {
          case VariableValueTypes.String:
            if (inputValue && (inputValue as string).trim()) {
              validValue = inputValue;
            }
            break;
          case VariableValueTypes.Number:
            if (inputValue !== null && !Number.isNaN(inputValue)) {
              validValue = inputValue;
            }
            break;
          case VariableValueTypes.Date:
            validValue = selectedValue;
            break;
        }

        if (validValue !== void 0) {
          onChange && onChange(value ? value.concat(validValue) : [validValue]);
          setInputValue(void 0);
        }
      },
      [value, type, inputValue, onChange],
    );

    const saveExpression = useCallback(
      e => {
        onChange && onChange([e.target.value]);
      },
      [onChange],
    );

    const inputChange = useCallback(e => {
      setInputValue(e.target.value);
    }, []);

    const inputNumberChange = useCallback(val => {
      setInputValue(val);
    }, []);

    const datePickerConfirm = useCallback(
      val => {
        saveRegular(val);
      },
      [saveRegular],
    );

    const tagClose = useCallback(
      index => e => {
        e.preventDefault();
        onChange && onChange(value ? value.filter((_, i) => index !== i) : []);
      },
      [value, onChange],
    );

    let conditionalInputComponent;

    switch (type) {
      case VariableValueTypes.Number:
        conditionalInputComponent = (
          <InputNumber
            placeholder={t('enterToAdd')}
            value={inputValue}
            className="input"
            disabled={!!disabled}
            onChange={inputNumberChange}
            onPressEnter={saveRegular}
          />
        );
        break;
      case VariableValueTypes.Date:
        conditionalInputComponent = (
          <DatePicker
            format={TIME_FORMATTER}
            className="input"
            disabled={!!disabled}
            onOk={datePickerConfirm}
            showNow
            showTime
          />
        );
        break;
      default:
        conditionalInputComponent = (
          <Input
            placeholder={t('enterToAdd')}
            value={inputValue}
            className="input"
            disabled={!!disabled}
            onChange={inputChange}
            onPressEnter={saveRegular}
          />
        );
        break;
    }

    return (
      <Wrapper direction="vertical">
        {expression || type === VariableValueTypes.Expression ? (
          <Input.TextArea
            placeholder={t('enterExpression')}
            autoSize={{ minRows: 4, maxRows: 8 }}
            value={value ? value[0] : void 0}
            disabled={!!disabled}
            onChange={saveExpression}
          />
        ) : (
          <>
            {value && value.length > 0 && (
              <ValueTags key="valueTags">
                {value?.map((val, index) => {
                  const label =
                    type !== VariableValueTypes.Date
                      ? val
                      : moment(val).format(dateFormat);
                  return (
                    <Tag
                      key={label}
                      className="tag"
                      closable
                      onClose={tagClose(index)}
                    >
                      {label}
                    </Tag>
                  );
                })}
              </ValueTags>
            )}
            <Space key="actions">
              {conditionalInputComponent}
              {type !== VariableValueTypes.Date && (
                <Button
                  size="small"
                  icon={<CheckOutlined />}
                  type="link"
                  onClick={saveRegular}
                />
              )}
            </Space>
          </>
        )}
        {type === VariableValueTypes.Date && hasDateFormat && (
          <Select
            placeholder="选择日期格式"
            className="input"
            value={dateFormat}
            onChange={onChangeDateFormat}
          >
            {Object.values(DateFormat).map(format => {
              return (
                <Select.Option value={format} key={format}>
                  {format}
                </Select.Option>
              );
            })}
          </Select>
        )}
      </Wrapper>
    );
  },
);

const Wrapper = styled(Space)`
  width: 100%;

  .add-btn {
    padding: ${SPACE} 0;
  }

  .input {
    width: ${SPACE_TIMES(50)};
  }
`;

const ValueTags = styled.div`
  .tag {
    margin: ${SPACE};
  }
`;
