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

import {
  Checkbox,
  Col,
  Input,
  InputNumber,
  Radio,
  Row,
  Select,
  Space,
} from 'antd';
import { FormItemEx } from 'app/components';
import { FieldFormatType } from 'app/constants';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import {
  ChartDataSectionField,
  FormatFieldAction,
} from 'app/types/ChartConfig';
import { CURRENCIES } from 'app/utils/currency';
import { updateBy } from 'app/utils/mutation';
import {
  DATE_FORMATTER,
  NumberUnitKey,
  NumericUnitDescriptions,
} from 'globalConstants';
import { FC, useState } from 'react';
import styled from 'styled-components/macro';
import { SPACE_TIMES } from 'styles/StyleConstants';

const DefaultFormatDetailConfig: FormatFieldAction = {
  type: FieldFormatType.Default,
  [FieldFormatType.Numeric]: {
    decimalPlaces: 2,
    unitKey: NumberUnitKey.None,
    useThousandSeparator: true,
    prefix: '',
    suffix: '',
  },
  [FieldFormatType.Currency]: {
    decimalPlaces: 2,
    unitKey: NumberUnitKey.None,
    useThousandSeparator: true,
    currency: '',
  },
  [FieldFormatType.Percentage]: {
    decimalPlaces: 2,
  },
  [FieldFormatType.Scientific]: {
    decimalPlaces: 2,
  },
  [FieldFormatType.Date]: {
    format: DATE_FORMATTER,
  },
  [FieldFormatType.Custom]: {
    format: '',
  },
};

const NumberFormatAction: FC<{
  config: ChartDataSectionField;
  onConfigChange: (config: ChartDataSectionField) => void;
}> = ({ config, onConfigChange }) => {
  const formItemLayout = {
    labelAlign: 'right' as any,
    labelCol: { span: 8 },
    wrapperCol: { span: 8 },
  };
  const t = useI18NPrefix(`viz.palette.data.actions`);

  const [type, setType] = useState<FieldFormatType>(
    config?.format?.type || DefaultFormatDetailConfig.type,
  );
  const [formatDetail, setFormatDetail] = useState(() => {
    return (
      config.format?.[config?.format?.type || DefaultFormatDetailConfig.type] ||
      {}
    );
  });

  const handleFormatTypeChanged = newType => {
    const defaultFormatDetail = DefaultFormatDetailConfig[newType];
    const newConfig = updateBy(config, draft => {
      draft.format = {
        type: newType,
        [newType]: defaultFormatDetail,
      };
    });
    setType(newType);
    setFormatDetail(defaultFormatDetail);
    onConfigChange?.(newConfig);
  };

  const handleFormatDetailChanged = newFormatDetail => {
    const newConfig = updateBy(config, draft => {
      draft.format = {
        type,
        [type]: newFormatDetail,
      };
    });
    setFormatDetail(newConfig?.format?.[newConfig.format?.type]);
    onConfigChange?.(newConfig);
  };

  const renderFieldFormatExtendSetting = () => {
    if (FieldFormatType.Default === type) {
      return null;
    } else {
      return (
        <Space direction="vertical">
          <FormItemEx {...formItemLayout} label={t('format.decimalPlace')}>
            <InputNumber
              min={0}
              max={99}
              step={1}
              value={formatDetail?.decimalPlaces}
              onChange={decimalPlaces => {
                handleFormatDetailChanged(
                  Object.assign({}, formatDetail, { decimalPlaces }),
                );
              }}
            />
          </FormItemEx>

          {FieldFormatType.Currency === type && (
            <>
              <FormItemEx {...formItemLayout} label={t('format.unit')}>
                <Select
                  value={formatDetail?.unitKey}
                  onChange={unitKey => {
                    handleFormatDetailChanged(
                      Object.assign({}, formatDetail, { unitKey }),
                    );
                  }}
                >
                  {Array.from(NumericUnitDescriptions.keys()).map(k => {
                    const values = NumericUnitDescriptions.get(k);
                    return (
                      <Select.Option key={k} value={k}>
                        {values?.[1] || '  '}
                      </Select.Option>
                    );
                  })}
                </Select>
              </FormItemEx>
              <FormItemEx {...formItemLayout} label={t('format.currency')}>
                <Select
                  value={formatDetail?.currency}
                  onChange={currency => {
                    handleFormatDetailChanged(
                      Object.assign({}, formatDetail, { currency }),
                    );
                  }}
                >
                  {CURRENCIES.map(c => {
                    return (
                      <Select.Option key={c.code} value={c.code}>
                        {c.code}
                      </Select.Option>
                    );
                  })}
                </Select>
              </FormItemEx>
            </>
          )}
          {FieldFormatType.Numeric === type && (
            <>
              <FormItemEx {...formItemLayout} label={t('format.unit')}>
                <Select
                  value={formatDetail?.unitKey}
                  onChange={unitKey => {
                    handleFormatDetailChanged(
                      Object.assign({}, formatDetail, { unitKey }),
                    );
                  }}
                >
                  {Array.from(NumericUnitDescriptions.keys()).map(k => {
                    const values = NumericUnitDescriptions.get(k);
                    return (
                      <Select.Option key={k} value={k}>
                        {values?.[1] || '  '}
                      </Select.Option>
                    );
                  })}
                </Select>
              </FormItemEx>
              <FormItemEx {...formItemLayout} label={t('format.useSeparator')}>
                <Checkbox
                  checked={formatDetail?.useThousandSeparator}
                  onChange={e =>
                    handleFormatDetailChanged(
                      Object.assign({}, formatDetail, {
                        useThousandSeparator: e.target.checked,
                      }),
                    )
                  }
                />
              </FormItemEx>
              <FormItemEx {...formItemLayout} label={t('format.prefix')}>
                <Input
                  value={formatDetail?.prefix}
                  onChange={e =>
                    handleFormatDetailChanged(
                      Object.assign({}, formatDetail, {
                        prefix: e?.target?.value,
                      }),
                    )
                  }
                />
              </FormItemEx>
              <FormItemEx {...formItemLayout} label={t('format.suffix')}>
                <Input
                  value={formatDetail?.suffix}
                  onChange={e =>
                    handleFormatDetailChanged(
                      Object.assign({}, formatDetail, {
                        suffix: e?.target?.value,
                      }),
                    )
                  }
                />
              </FormItemEx>
            </>
          )}
        </Space>
      );
    }
  };

  return (
    <StyledNumberFormatAction>
      <Col span={4} offset={4}>
        <Radio.Group
          onChange={e => handleFormatTypeChanged(e.target.value)}
          value={type}
        >
          <Space direction="vertical">
            <Radio value={FieldFormatType.Default}>{t('format.default')}</Radio>
            <Radio value={FieldFormatType.Numeric}>{t('format.numeric')}</Radio>
            <Radio value={FieldFormatType.Currency}>
              {t('format.currency')}
            </Radio>
            <Radio value={FieldFormatType.Percentage}>
              {t('format.percentage')}
            </Radio>
            <Radio value={FieldFormatType.Scientific}>
              {t('format.scientific')}
            </Radio>
          </Space>
        </Radio.Group>
      </Col>
      <Col span={16}>{renderFieldFormatExtendSetting()}</Col>
    </StyledNumberFormatAction>
  );
};

export default NumberFormatAction;

const StyledNumberFormatAction = styled(Row)`
  .ant-radio-wrapper {
    line-height: 32px;
  }

  .ant-input-number,
  .ant-select,
  .ant-input {
    width: ${SPACE_TIMES(50)};
  }

  .ant-space {
    width: 100%;
  }
`;
