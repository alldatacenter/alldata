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
import { Popover, Row, Col, Form, Button, Badge } from 'antd';
import { ErdaIcon, RenderFormItem, IFormItem } from 'common';
import { cloneDeep, isEmpty } from 'lodash';
import i18n from 'i18n';
import ConfigSelector from './config-selector';

import './index.scss';

export interface IProps {
  fieldsList: Field[];
  configList: ConfigData[];
  defaultConfig: number | string;
  onFilter: (data: Obj) => void;
  onDeleteFilter: (data: Obj) => void;
  onSaveFilter: (label: string, values: Obj) => void;
  value?: Obj;
  onConfigChange?: (config: ConfigData) => void;
  processField?: (field: Field) => IFormItem;
}

interface Field {
  type: string;
  key: string;
  mode?: string;
  value: number | string;
  label: string;
  children?: Field[];
}

export interface ConfigData {
  id: number | string;
  label: string;
  values: Obj;
  isPreset?: boolean;
}

const sortObj = (obj: Obj) => {
  const values = cloneDeep(obj);
  Object.keys(values).forEach((key) => {
    if (Array.isArray(values[key])) {
      if (values[key].length !== 0) {
        values[key] = values[key].sort().join(',');
      } else {
        delete values[key];
      }
    } else if (!values[key]) {
      delete values[key];
    }
  });

  return values;
};

const getItemByValues = (val: Obj, list: Obj[]) => {
  const values = sortObj(val);

  return list?.find((item) => JSON.stringify(sortObj(item.values || {})) === JSON.stringify(values));
};

const defaultProcessField = (item: IFormItem) => {
  const { type, itemProps, defaultValue, placeholder } = item;
  const field: IFormItem = { ...item };

  field.name = item.key;
  if (type === 'select' || type === 'tagsSelect') {
    field.itemProps = {
      mode: 'multiple',
      ...itemProps,
      showArrow: true,
      allowClear: true,
      suffixIcon: <ErdaIcon type="caret-down" color="currentColor" className="text-white-400" />,
      clearIcon: <span className="p-1">{i18n.t('common:clear')}</span>,
      getPopupContainer: (triggerNode: HTMLElement) => triggerNode.parentElement as HTMLElement,
    };

    if (type === 'select') {
      field.itemProps.optionLabelProp = 'label';
    }
  } else if (type === 'dateRange') {
    field.itemProps = {
      customProps: {
        showClear: true,
      },
    };
  }

  field.itemProps = {
    defaultValue,
    placeholder,
    ...field.itemProps,
  };

  return field;
};

const ConfigurableFilter = ({
  fieldsList,
  configList,
  defaultConfig,
  value,
  onFilter: onFilterProps,
  onDeleteFilter,
  onSaveFilter,
  processField,
}: IProps) => {
  const [form] = Form.useForm();
  const [visible, setVisible] = React.useState(false);
  const [currentConfig, setCurrentConfig] = React.useState<string | number>();
  const [isNew, setIsNew] = React.useState(false);

  React.useEffect(() => {
    if (value) {
      form.setFieldsValue(value || {});
      const config = getItemByValues(value, configList);
      config?.id && setCurrentConfig(config?.id);
      setIsNew(!config);
    } else if (configList && configList.length !== 0) {
      const configData: ConfigData = configList?.find((item) => item.id === defaultConfig) || ({} as ConfigData);
      if (configData.values) {
        configData.values && form.setFieldsValue(configData.values);
        onFilterProps?.(configData.values);
      }
    }
  }, [configList, defaultConfig, form, value, onFilterProps]);

  const onConfigChange = (config: ConfigData) => {
    setCurrentConfig(config.id);
    setIsNew(false);
    form.resetFields();
    form.setFieldsValue(config.values || {});
  };

  const onValuesChange = (_, allValues: Obj) => {
    const config = getItemByValues(allValues, configList);
    if (config?.id) {
      setCurrentConfig(config?.id);
      setIsNew(false);
    } else {
      setIsNew(true);
    }
  };

  const saveFilter = (label: string) => {
    onSaveFilter(label, form.getFieldsValue());
  };

  const setAllOpen = () => {
    form.resetFields();
    const config = getItemByValues(form.getFieldsValue(), configList);
    setCurrentConfig(config?.id);
    setIsNew(false);
  };

  const onFilter = () => {
    form.validateFields().then((values) => {
      onFilterProps(values);
      setVisible(false);
    });
  };

  React.useEffect(() => {
    if (visible && value) {
      form.resetFields();
      form.setFieldsValue(value || {});
      const config = getItemByValues(value, configList);
      config?.id && setCurrentConfig(config?.id);
      setIsNew(!config);
    }
  }, [visible, form, configList, value]);

  const content = (
    <div className="erda-configurable-filter-content">
      <div className="erda-configurable-filter-header flex justify-start">
        <ConfigSelector
          list={configList}
          value={currentConfig}
          isNew={isNew}
          defaultValue={defaultConfig}
          onChange={onConfigChange}
          onDeleteFilter={onDeleteFilter}
          onSaveFilter={saveFilter}
        />

        <div className="flex-1 flex justify-end">
          <ErdaIcon
            type="guanbi"
            fill="white"
            color="white"
            size={20}
            className="text-white cursor-pointer"
            onClick={() => setVisible(false)}
          />
        </div>
      </div>

      <div className="erda-configurable-filter-body mt-3">
        <Form form={form} layout="vertical" onValuesChange={onValuesChange}>
          <Row>
            {fieldsList?.map((item, index: number) => {
              return (
                <Col span={12} className={index % 2 === 1 ? 'pl-2' : 'pr-2'}>
                  <RenderFormItem required={false} {...defaultProcessField(processField ? processField(item) : item)} />
                </Col>
              );
            })}
          </Row>
        </Form>
      </div>

      <div className="erda-configurable-filter-footer flex justify-end">
        <Button className="mx-1" onClick={() => setVisible(false)}>
          {i18n.t('cancel')}
        </Button>
        <Button className="mx-1" onClick={setAllOpen}>
          {i18n.t('dop:set it to open all')}
        </Button>
        <Button type="primary" className="mx-1" onClick={onFilter}>
          {i18n.t('common:filter')}
        </Button>
      </div>
    </div>
  );

  return (
    <div className={`flex items-center ${value && !isEmpty(value) ? 'erda-config-filter-btn-active' : ''}`}>
      <Popover
        content={content}
        visible={visible}
        trigger={['click']}
        overlayClassName="erda-configurable-filter"
        placement="bottomLeft"
        onVisibleChange={setVisible}
      >
        <div
          className={`erda-configurable-filter-btn p-1 rounded-sm leading-none cursor-pointer bg-hover`}
          onClick={() => setVisible(true)}
        >
          <Badge dot={!!(value && !isEmpty(value))}>
            <ErdaIcon type="shaixuan" color="currentColor" size={20} />
          </Badge>
        </div>
      </Popover>
      {value && !isEmpty(value) ? (
        <div
          className="erda-configurable-filter-clear-btn p-1 rounded-sm leading-none cursor-pointer"
          onClick={() => {
            setAllOpen();
            onFilter();
          }}
        >
          <ErdaIcon type="zhongzhi" color="currentColor" size={20} className="relative top-px" />
        </div>
      ) : null}
    </div>
  );
};

export default ConfigurableFilter;
