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
import {
  Form,
  Input,
  Select,
  InputNumber,
  Switch,
  Radio,
  Checkbox,
  Cascader,
  DatePicker,
  Tooltip,
  Divider,
} from 'antd';
import { FormInstance } from 'app/interface/common';
import classnames from 'classnames';
import { ErdaIcon, Icon as CustomIcon, Badge } from 'common';
import { TagItem } from 'common/components/tags';
import i18n from 'i18n';
import { isString } from 'lodash';
import moment, { Moment } from 'moment';

interface IProps {
  onChange?: (...args: unknown[]) => void;
}

class ClassWrapper extends React.PureComponent<IProps> {
  /**
   * @override
   * @param args
   */
  onChange = (...args: unknown[]) => {
    const { children, onChange } = this.props;
    // default onChange form item automatic inject
    onChange?.(...args);
    // child component onChange method
    children?.props?.onChange?.(...args);
  };
  render() {
    const { children, ...rest } = this.props;
    if (!children || typeof children !== 'object') {
      return children || null;
    }
    return React.cloneElement(children as any, {
      ...rest,
      onChange: this.onChange,
    });
  }
}

const FormItem = Form.Item;
const { Option, OptGroup } = Select;
const { TextArea } = Input;

const defalutFormItemLayout = {
  labelCol: {
    sm: { span: 24 },
    md: { span: 6 },
    lg: { span: 6 },
  },
  wrapperCol: {
    sm: { span: 24 },
    md: { span: 18 },
    lg: { span: 14 },
  },
};
const fullWrapperCol = { span: 24 };
const defalutTailFormItemLayout = {
  wrapperCol: {
    sm: {
      span: 24,
      offset: 0,
    },
    md: {
      span: 18,
      offset: 6,
    },
    lg: {
      span: 14,
      offset: 6,
    },
  },
};
export interface IFormItem {
  form?: FormInstance;
  label?: string;
  labelTip?: string;
  name?: string | string[];
  key?: string;
  type?: string;
  initialValue?: any;
  size?: 'default' | 'small' | 'large';
  required?: boolean;
  pattern?: RegExp | null;
  message?: string;
  itemProps?: any;
  extraProps?: object;
  rules?: any[];
  config?: object;
  options?: Array<{ name: string; value: string | number; disabled?: boolean }> | Function;
  suffix?: string | null;
  formItemLayout?: object;
  className?: string;
  tailFormItemLayout?: object;
  formLayout?: string | null;
  noColon?: boolean;
  isTailLayout?: boolean; // no label, put some offset to align right part
  onChange?: any;
  addOne?: (name: string | undefined) => void;
  dropOne?: (name: string | undefined) => void;
  getComp?: ({ form }: { form: FormInstance }) => React.ReactElement<any> | string;
}

interface IOption {
  name?: string;
  label?: string;
  icon?: string;
  status?: 'success' | 'error' | 'warning' | 'default' | 'processing';
  value: string | number;
  disabled?: boolean;
  children?: IOption[];
  fix?: boolean;
}

const renderSelectOption = (single: IOption, optionRender: (opt: IOption) => JSX.Element) => {
  if (single.children) {
    return (
      <OptGroup
        key={single.value}
        label={
          <div className="flex items-center">
            {single.icon ? <CustomIcon type={single.icon} /> : null}
            <div className="ml-1 flex-1">{single.name || single.label}</div>
          </div>
        }
      >
        {single.children.map((item: IOption) => renderSelectOption(item, optionRender))}
      </OptGroup>
    );
  }
  return (
    <Option
      className={single.fix ? 'select-fix-option' : ''}
      key={single.value}
      label={single.name || single.label}
      value={single.value}
      disabled={!!single.disabled}
    >
      {optionRender ? (
        optionRender(single)
      ) : single.status ? (
        <Badge status={single.status} text={single.name || single.label || '-'} showDot={false} />
      ) : (
        <div className="flex items-center">
          {single.icon ? <CustomIcon type={single.icon} /> : null}
          <div className="ml-1 flex-1">{single.name || single.label}</div>
        </div>
      )}
    </Option>
  );
};

const RenderFormItem = ({
  form,
  label,
  labelTip,
  name,
  type,
  initialValue = null,
  size = 'default',
  required = true,
  pattern = null,
  message = i18n.t('common:please fill in the format correctly'),
  itemProps = {},
  extraProps = {},
  className = '',
  rules = [],
  config,
  options = [],
  addOne,
  dropOne,
  getComp,
  suffix = null,
  formItemLayout,
  formLayout,
  tailFormItemLayout,
  noColon = false,
  isTailLayout, // no label, put some offset to align right part
}: IFormItem) => {
  let ItemComp = null;
  const specialConfig: any = {};
  let _type = type;
  if (typeof getComp === 'function') {
    _type = 'custom';
  }
  let action = i18n.t('common:input');
  switch (_type) {
    case 'select':
      if (itemProps.mode === 'multiple') {
        specialConfig.valuePropType = 'array';
      }

      ItemComp = (
        <ClassWrapper>
          <SelectComp options={options} size={size} {...itemProps} />
        </ClassWrapper>
      );
      action = i18n.t('common:select');
      break;
    case 'tagsSelect':
      if (itemProps.mode === 'multiple') {
        specialConfig.valuePropType = 'array';
      }

      ItemComp = (
        <ClassWrapper>
          <TagsSelect options={options} size={size} {...itemProps} />
        </ClassWrapper>
      );
      action = i18n.t('common:select');
      break;
    case 'inputNumber':
      ItemComp = (
        <InputNumber {...itemProps} className={classnames('input-with-icon', itemProps.className)} size={size} />
      );
      break;
    case 'textArea':
      ItemComp = <TextArea {...itemProps} className={classnames('input-with-icon', itemProps.className)} />;
      break;
    case 'switch':
      specialConfig.valuePropName = 'checked';
      specialConfig.valuePropType = 'boolean';
      ItemComp = <Switch {...itemProps} />;
      action = i18n.t('common:select');
      break;
    case 'radioGroup':
      ItemComp = (
        <Radio.Group buttonStyle="solid" {...itemProps} size={size}>
          {typeof options === 'function'
            ? options()
            : options.map((single) => (
                <Radio.Button key={single.value} value={`${single.value}`} disabled={!!single.disabled}>
                  {single.name}
                </Radio.Button>
              ))}
        </Radio.Group>
      );
      action = i18n.t('common:select');
      break;
    case 'checkbox':
      specialConfig.valuePropName = 'checked';
      specialConfig.valuePropType = 'boolean';
      if (itemProps.options) {
        ItemComp = <Checkbox.Group {...itemProps} />;
      } else {
        const { text = '', ...checkboxProps } = itemProps;
        ItemComp = <Checkbox {...checkboxProps}>{text}</Checkbox>;
      }
      action = i18n.t('common:select');
      break;
    case 'datePicker':
      ItemComp = (
        <DatePicker className="w-full" allowClear={false} format="YYYY-MM-DD" showTime={false} {...itemProps} />
      );
      break;
    case 'dateRange':
      ItemComp = (
        <ClassWrapper>
          <DateRange {...itemProps} />
        </ClassWrapper>
      );
      break;
    case 'custom':
      // getFieldDecorator不能直接包裹FunctionalComponent，see https://github.com/ant-design/ant-design/issues/11324
      ItemComp = <ClassWrapper {...itemProps}>{(getComp as Function)({ form })}</ClassWrapper>;
      break;
    case 'cascader':
      specialConfig.valuePropType = 'array';
      ItemComp = <Cascader {...itemProps} options={options} />;
      break;
    case 'input':
    default:
      ItemComp = <Input {...itemProps} className={classnames('input-with-icon', itemProps.className)} size={size} />;
      break;
  }

  const layout =
    label === undefined
      ? fullWrapperCol
      : isTailLayout
      ? tailFormItemLayout || defalutTailFormItemLayout
      : formLayout === 'horizontal'
      ? formItemLayout || defalutFormItemLayout
      : null;

  // generate rules
  if (required && !rules.some((r) => r.required === true)) {
    if (typeof label === 'string' && label.length) {
      const hasColon = !noColon && (label.endsWith(':') || label.endsWith('：'));
      rules.push({
        required,
        message: `${i18n.t('common:please')}${action}${hasColon ? label.slice(0, label.length - 1) : label}`,
      });
    } else if (label) {
      rules.push({
        required,
        message: i18n.t('can not be empty'),
      });
    }
  }
  if (pattern && !rules.some((r) => r.pattern && r.pattern.source === pattern.source)) {
    rules.push({ pattern, message });
  }
  // generate config
  const itemConfig = {
    rules,
    ...specialConfig,
    ...config,
  };
  if (initialValue !== null) {
    switch (itemConfig.valuePropType) {
      case 'boolean':
        itemConfig.initialValue = !!initialValue;
        break;
      case 'array':
        itemConfig.initialValue = initialValue;
        break;
      default:
        itemConfig.initialValue = initialValue.toString();
    }
  }
  const _label = labelTip ? (
    <span>
      {label}&nbsp;
      <Tooltip title={labelTip}>
        <ErdaIcon type="help" className="align-middle text-icon" />
      </Tooltip>
    </span>
  ) : (
    label
  );

  return (
    <FormItem
      label={_label}
      {...layout}
      className={`${itemProps.type === 'hidden' ? 'hidden' : ''} ${className}`}
      required={required}
    >
      <FormItem
        name={typeof name === 'string' && name?.includes('.') ? name.split('.') : name}
        noStyle
        {...extraProps}
        {...itemConfig}
      >
        {ItemComp}
      </FormItem>
      {suffix}
      {addOne ? <ErdaIcon type="add-one" className="render-form-op" onClick={() => addOne(name)} /> : null}
      {dropOne ? <ErdaIcon type="reduce-one" className="render-form-op" onClick={() => dropOne(name)} /> : null}
    </FormItem>
  );
};

interface SelectCompProps {
  size?: 'small' | 'large';
  options: IOption[] | Function;
  value: Array<number | string>;
  onChange: (value: Array<number | string>) => void;
  itemProps: Obj;
  optionRender: (option: IOption) => JSX.Element;
}

const SelectComp = ({ value, onChange, options, size, optionRender, ...restItemProps }: SelectCompProps) => {
  const fixOptions = options.filter?.((item: IOption) => item.fix) || [];
  return (
    <Select
      {...restItemProps}
      value={value}
      onChange={onChange}
      size={size}
      filterOption={(input, option) =>
        typeof option?.label === 'string' && option?.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
      }
      dropdownRender={(menu) => (
        <div>
          {fixOptions?.length !== 0 ? (
            <>
              <div className="p-2 text-white-400">
                {fixOptions.map((item: IOption) => (
                  <div className="px-1 text-purple-deep" onClick={() => onChange([item.value])}>
                    {item.label}
                  </div>
                ))}
              </div>
              <Divider className="border-white-06" style={{ margin: '4px 0' }} />
            </>
          ) : null}
          {menu}
        </div>
      )}
    >
      {typeof options === 'function'
        ? options()
        : options.filter((item: IOption) => !item.fix).map((item: IOption) => renderSelectOption(item, optionRender))}
    </Select>
  );
};

const DateRange = ({
  value,
  onChange,
  customProps,
}: {
  value: number[];
  onChange: (values: Obj) => void;
  customProps: Obj;
}) => {
  const [startOpen, setStartOpen] = React.useState(false);
  const [endOpen, setEndOpen] = React.useState(false);
  const [_startDate, _endDate] = value || [];
  const startDate = typeof _startDate === 'string' ? +_startDate : _startDate;
  const endDate = typeof _endDate === 'string' ? +_endDate : _endDate;
  const { borderTime, disabled, required, showClear } = customProps || {};

  const disabledDate = (isStart: boolean) => (current: Moment | undefined) => {
    return (
      !!current &&
      (isStart
        ? endDate
          ? (borderTime ? current.startOf('dates') : current) > moment(endDate)
          : false
        : startDate
        ? (borderTime ? current.endOf('dates') : current) < moment(startDate)
        : false)
    );
  };

  const getTimeValue = (v: any[]) => {
    if (borderTime) {
      const startVal = v[0]
        ? moment(isString(v[0]) ? +v[0] : v[0])
            .startOf('dates')
            .valueOf()
        : v[0];
      const endVal = v[1]
        ? moment(isString(v[1]) ? +v[1] : v[1])
            .endOf('dates')
            .valueOf()
        : v[1];
      return [startVal, endVal];
    }
    return v;
  };

  return (
    <div className={`erda-form-date-range relative ${startOpen || endOpen ? 'erda-form-date-range-open' : ''}`}>
      <DatePicker
        size="small"
        bordered={false}
        disabled={disabled}
        value={startDate ? moment(startDate) : undefined}
        disabledDate={disabledDate(true)}
        format={'YYYY/MM/DD'}
        allowClear={!required}
        onChange={(v) => onChange(getTimeValue([v?.startOf?.('day').valueOf(), endDate]))}
        placeholder={i18n.t('common:startDate')}
        open={startOpen}
        onOpenChange={setStartOpen}
      />
      <span className="divider mx-1">—</span>
      <DatePicker
        size="small"
        bordered={false}
        disabled={disabled}
        allowClear={!required}
        value={endDate ? moment(endDate) : undefined}
        disabledDate={disabledDate(false)}
        format={'YYYY/MM/DD'}
        placeholder={i18n.t('common:endDate')}
        onChange={(v) => onChange(getTimeValue([startDate, v?.endOf?.('day').valueOf()]))}
        open={endOpen}
        onOpenChange={setEndOpen}
      />
      {showClear && (_startDate || _endDate) ? (
        <div
          className={`erda-form-date-range-clear absolute -top-3/4 pb-1 right-0 text-xs cursor-pointer opacity-0 ${
            startOpen || endOpen ? 'opacity-100' : ''
          }`}
          onClick={() => onChange([])}
        >
          清除
        </div>
      ) : null}
    </div>
  );
};

const renderTagsSelectOption = (single: Obj) => {
  return (
    <Option key={single.value} label={single.name || single.label} value={single.value} disabled={!!single.disabled}>
      <TagItem label={{ label: single.name || single.label, color: single.color }} checked={single.checked} readOnly />
    </Option>
  );
};

interface TagsSelectProps {
  size?: 'small' | 'large';
  options: IOption[] | Function;
  value: Array<number | string>;
  onChange: (value: Array<number | string>) => void;
  itemProps: Obj;
}

const TagsSelect = ({ size, options, value = [], onChange, ...restItemProps }: TagsSelectProps) => {
  const [open, setOpen] = React.useState(false);

  return (
    <Select
      {...restItemProps}
      value={value}
      onChange={onChange}
      className="erda-tags-select"
      size={size}
      open={open}
      onDropdownVisibleChange={setOpen}
      onFocus={() => setOpen(true)}
      filterOption={(input, option) =>
        typeof option?.label === 'string' && option?.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
      }
      listItemHeight={0}
    >
      {typeof options === 'function'
        ? options()
        : options.map((item) => renderTagsSelectOption({ ...item, checked: value.includes(item.value) }))}
    </Select>
  );
};

export default RenderFormItem;
