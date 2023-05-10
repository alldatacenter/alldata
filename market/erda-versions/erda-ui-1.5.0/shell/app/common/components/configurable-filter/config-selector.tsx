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
import { Popover, Divider, Form, Input, Button, Modal } from 'antd';
import { ErdaIcon, Ellipsis, Badge } from 'common';
import { ConfigData } from './index';
import i18n from 'i18n';

export interface IProps {
  list: ConfigData[];
  value?: number | string;
  isNew: boolean;
  defaultValue: number | string;
  onDeleteFilter: (config: ConfigData) => void;
  onSaveFilter: (label: string) => void;
  onChange: (config: ConfigData) => void;
}

const ConfigSelector = ({ list, defaultValue, value, onChange, onDeleteFilter, onSaveFilter, isNew }: IProps) => {
  const [form] = Form.useForm();
  const configSelectorRef = React.useRef();

  const [defaultData, setDefaultData] = React.useState<ConfigData[]>([]);
  const [customData, setCustomData] = React.useState<ConfigData[]>([]);
  const [addVisible, setAddVisible] = React.useState(false);
  const [configListVisible, setConfigListVisible] = React.useState(false);

  React.useEffect(() => {
    setDefaultData(list.filter((item: ConfigData) => item.isPreset));
    setCustomData(list.filter((item: ConfigData) => !item.isPreset));
  }, [list]);

  const onConfigChange = (config: ConfigData) => {
    onChange(config);
    setConfigListVisible(false);
  };

  const deleteFilter = (item: ConfigData) => {
    Modal.confirm({
      title: i18n.t('dop:whether to delete the {name}', { name: item.label }),
      zIndex: 9999,
      getContainer: configSelectorRef.current,
      onOk() {
        onDeleteFilter(item);
      },
    });
  };

  const configItemContent = (item: ConfigData) => {
    return (
      <div className="px-2 py-3">
        {/* <div className="py-1 px-2 bg-hover">设为默认</div>
        <div className="py-1 px-2 bg-hover">重命名</div> */}
        <div className="py-1 px-2 bg-hover text-red" onClick={() => deleteFilter(item)}>
          {i18n.t('delete')}
        </div>
      </div>
    );
  };

  const configItemMore = (item: ConfigData) => {
    return (
      <Popover
        content={configItemContent(item)}
        trigger={['click']}
        overlayClassName="erda-configurable-filter-config-operation"
        placement="bottomLeft"
        getPopupContainer={(triggerNode) => triggerNode.parentElement as HTMLElement}
      >
        <ErdaIcon
          type="gengduo"
          size={20}
          className="config-item-icon absolute right-2"
          onClick={(e) => e.stopPropagation()}
        />
      </Popover>
    );
  };

  const currentConfig = list.find((item) => item.id === value);

  const renderConfigList = (configList: ConfigData[], showMore: boolean) =>
    configList.map((item) => (
      <div
        className={`config-item bg-hover pl-2 py-1 pr-7 flex items-center relative ${
          item.id === currentConfig?.id ? 'active' : ''
        }`}
        onClick={() => onConfigChange(item)}
      >
        <Ellipsis title={item.label} />
        {item.id === defaultValue ? <span className="default-text ml-3 mr-1 whitespace-nowrap">默认</span> : null}
        {showMore ? configItemMore(item) : null}
      </div>
    ));

  const configContent = (
    <div>
      <div className="default-config">{renderConfigList(defaultData, false)}</div>
      <Divider className="my-1" />
      <div className="custom-config">
        <div className="config-title px-2 pb-2">{i18n.t('dop:custom filter')}</div>
        {renderConfigList(customData, true)}
      </div>
    </div>
  );

  const addCancel = () => {
    form.resetFields();
    setAddVisible(false);
  };

  const saveFilter = () => {
    form.validateFields().then(({ label }) => {
      onSaveFilter?.(label);
      setAddVisible(false);
    });
  };

  const addConfigContent = (
    <div>
      <Form form={form} layout="vertical" className="p-4">
        <Form.Item
          label={i18n.t('dop:filter name')}
          name="label"
          rules={[
            { required: true, message: i18n.t('please enter {name}', { name: i18n.t('dop:filter name') }) },
            { max: 10, message: i18n.t('dop:within {num} characters', { num: 10 }) },
          ]}
        >
          <Input placeholder={i18n.t('dop:please enter, within {num} characters', { num: 10 })} />
        </Form.Item>
        <div className="mt-3">
          <Button type="primary" onClick={saveFilter}>
            {i18n.t('ok')}
          </Button>
          <span className="text-white ml-3 cursor-pointer" onClick={addCancel}>
            {i18n.t('cancel')}
          </span>
        </div>
      </Form>
    </div>
  );
  return (
    <>
      <Popover
        content={configContent}
        trigger={['click']}
        overlayClassName="erda-configurable-filter-config"
        placement="bottomLeft"
        getPopupContainer={(triggerNode) => triggerNode.parentElement as HTMLElement}
        visible={configListVisible}
        onVisibleChange={setConfigListVisible}
      >
        <div
          className="erda-configurable-filter-config-select flex items-center cursor-pointer"
          ref={configSelectorRef}
        >
          <ErdaIcon type="shaixuan" fill="white" className="mr-1" size={16} />
          {currentConfig?.label || i18n.t('dop:filter criteria are not saved')}
          {value && isNew ? (
            <Badge text={i18n.t('dop:changed')} status="processing" showDot={false} className="ml-2" />
          ) : null}
          <ErdaIcon type="caret-down" className="ml-1 text-white-800" />
        </div>
      </Popover>
      {isNew || !value ? (
        <Popover
          content={addConfigContent}
          visible={addVisible}
          onVisibleChange={setAddVisible}
          trigger={['click']}
          overlayClassName="erda-configurable-filter-add"
          placement="bottomLeft"
          getPopupContainer={(triggerNode) => triggerNode.parentElement as HTMLElement}
        >
          <div
            className="add-filter-config hover:bg-white-200 cursor-pointer rounded-sm py-1 px-3 ml-2 flex items-center"
            onClick={() => setAddVisible(true)}
          >
            <ErdaIcon size={16} fill="white" type="baocun" className="mr-1" /> {i18n.t('dop:new filter')}
          </div>
        </Popover>
      ) : null}
    </>
  );
};

export default ConfigSelector;
