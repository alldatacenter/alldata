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

// 此部分逻辑基本拷贝原来逻辑，方便后面如果整体删除原来代码
import React from 'react';
import { Drawer, Form, Button, Input, InputNumber, Collapse, Alert, Spin, Select, Tooltip } from 'antd';
import { getActionGroup } from 'application/services/deploy';
import { FormComponentProps } from 'core/common/interface';
import i18n from 'i18n';
import {
  cloneDeep,
  map,
  flatten,
  isEmpty,
  omit,
  pick,
  get,
  set,
  filter,
  head,
  transform,
  isEqual,
  forEach,
  find,
} from 'lodash';
import { useEffectOnce, useUpdateEffect } from 'react-use';
import VariableInput from 'application/common/components/object-input-group';
import ListInput from 'application/common/components/list-input-group';
import { Icon as CustomIcon, IF, ErdaIcon } from 'common';
import { useUpdate } from 'common/use-hooks';
import appDeployStore from 'application/stores/deploy';
import { useLoading } from 'core/stores/loading';
import ActionSelect from './action-select';
import { getResource, getDefaultVersionConfig, mergeActionAndResource } from '../utils';
import './pipeline-node-drawer.scss';
import { protocolActionForms } from 'app/config-page/components/action-form';
import ActionConfigForm from './action-config-form';

const { Item } = Form;
const { Panel } = Collapse;
const { Option } = Select;

export const i18nMap = {
  version: i18n.t('version'),
  params: i18n.t('dop:task params'),
  resources: i18n.t('dop:running resources'),
  commands: i18n.t('dop:task commands'),
  image: i18n.t('image'),
};

export interface IEditStageProps {
  nodeData: any;
  editing: boolean;
  isCreate?: boolean;
  otherTaskAlias?: string[];
  visible: boolean;
  scope?: string;
  chosenActionName: string;
  chosenAction: DEPLOY.ActionConfig;
  onSubmit?: (options: any) => void;
  actionSpec: DEPLOY.ActionConfig[];
}
const noop = () => {};

const formKeyFormat = (key: string) => {
  return key.split('.').map((keyItem) => {
    const indexArr = /^\[([^[]*)\]$/.exec(keyItem);
    return indexArr?.length ? parseInt(indexArr[1], 10) : keyItem;
  });
};

const PurePipelineNodeForm = (props: IEditStageProps & FormComponentProps) => {
  const [form] = Form.useForm();
  const {
    nodeData: propsNodeData,
    editing,
    isCreate,
    otherTaskAlias = [],
    onSubmit: handleSubmit = noop,
    chosenActionName,
    chosenAction,
    actionSpec,
  } = props;
  const [actionConfigs] = appDeployStore.useStore((s) => [s.actionConfigs]);

  const { getFieldValue } = form;
  const [{ actionConfig, resource, originType, originName, task, changeKey }, updater, update] = useUpdate({
    resource: {},
    actionConfig: {} as DEPLOY.ActionConfig,
    originType: null as null | string,
    originName: null as null | string,
    task: {} as IStageTask,
    changeKey: 0,
  });

  useEffectOnce(() => {
    handleActionSpec();
  });

  React.useEffect(() => {
    if (propsNodeData && !isEmpty(propsNodeData)) {
      update({
        originName: propsNodeData.alias,
        originType: propsNodeData.type,
        task: propsNodeData,
      });
    }
  }, [propsNodeData, update]);

  React.useEffect(() => {
    if (isCreate) {
      updater.actionConfig({} as DEPLOY.ActionConfig);
    }
  }, [isCreate, updater]);

  const taskInitName =
    originType === actionConfig.name
      ? originName
      : otherTaskAlias.includes(actionConfig.name)
      ? undefined
      : actionConfig.name;

  const taskInitVersion = task.version || actionConfig.version;
  useUpdateEffect(() => {
    const prevResource = form.getFieldValue('resource') || {};
    form.setFieldsValue({
      resource: {
        ...prevResource,
        type: chosenActionName,
        alias: taskInitName,
        version: taskInitVersion,
      },
    });
    updater.changeKey((prev: number) => prev + 1);
  }, [taskInitName, taskInitVersion, chosenActionName]);

  const handleActionSpec = () => {
    let _config;
    let _resource;
    if (propsNodeData && !isEmpty(propsNodeData)) {
      if (actionSpec.length > 0) {
        _config = propsNodeData.version
          ? actionSpec.find((c) => c.version === propsNodeData.version)
          : getDefaultVersionConfig(actionSpec);
      }
      _resource = getResource(propsNodeData, _config);
    } else {
      _config = getDefaultVersionConfig(actionSpec);
      const mergedResource = mergeActionAndResource(_config, {});
      _resource = { ...resource, ...mergedResource };
    }
    update({
      resource: _resource,
      actionConfig: _config || ({} as DEPLOY.ActionConfig),
    });
  };

  useUpdateEffect(() => {
    handleActionSpec();
  }, [actionSpec]);

  if (!isCreate && isEmpty(actionConfig)) {
    return null;
  }
  const type = actionConfig.type || getFieldValue(['resource', 'type']);

  const checkResourceName = (_rule: any, value: string, callback: any) => {
    const name = form.getFieldValue(['resource', 'alias']);

    if (!value) {
      return callback(i18n.t('dop:please enter the task name'));
    }
    if (otherTaskAlias.includes(name)) {
      return callback(i18n.t('dop:An Action with the same name exists.'));
    }
    callback();
  };

  const changeActionVersion = (version: any) => {
    const selectConfig = actionConfigs.find((config) => config.version === version) as DEPLOY.ActionConfig;
    updater.actionConfig(selectConfig);
    updater.resource(getResource(task as IStageTask, selectConfig));
  };

  const taskType = (
    <Item
      className="hidden"
      name={['resource', 'type']}
      initialValue={chosenActionName}
      rules={[
        {
          required: true,
          message: `${i18n.t('dop:please choose')}Task Type`,
        },
      ]}
    >
      <Input />
    </Item>
  );

  const loopData = (
    <Item className="hidden" name={['resource', 'loop']} initialValue={get(actionConfig, 'spec.loop')} />
  );

  const actionVersion = (
    <Item
      label={i18nMap.version}
      name={['resource', 'version']}
      initialValue={task.version || actionConfig.version}
      rules={[
        {
          required: true,
          message: `${i18n.t('dop:please choose')}Task Version`,
        },
      ]}
    >
      <Select disabled={!editing} onChange={changeActionVersion} placeholder={`${i18n.t('dop:please choose version')}`}>
        {actionConfigs.map((config) => (
          <Option key={config.version} value={config.version}>
            {config.version}
          </Option>
        ))}
      </Select>
    </Item>
  );

  let alert;
  if (!isCreate && !actionConfig.type) {
    alert = (
      <Alert
        className="addon-error-tag"
        showIcon
        message={i18n.t('dop:the current action does not exist, please re-select!')}
        type="error"
      />
    );
  }

  const taskName = (
    <Item
      label={i18n.t('dop:task name')}
      name={['resource', 'alias']}
      initialValue={taskInitName}
      rules={[
        {
          required: true,
          validator: checkResourceName,
        },
      ]}
    >
      <Input autoFocus={!type} disabled={!editing} placeholder={i18n.t('dop:please enter the task name')} />
    </Item>
  );

  const renderTaskTypeStructure = () => {
    if (isEmpty(resource)) {
      return null;
    }

    const { getFieldsValue } = form;
    const resourceForm = getFieldsValue([
      ['resource', 'alias'],
      ['resource', 'type'],
    ]);
    if (!get(resourceForm, 'resource.type')) {
      return null;
    }

    return renderResource(resource, 'resource');
  };

  const getDataValue = (dataSource: any, key: string) => {
    return dataSource ? dataSource[key] : null;
  };

  const renderResource = (resourceParam: any, parentKey?: string, dataSource?: any) => {
    if (resourceParam.data instanceof Array) {
      return resourceParam.data.map((item: any) => {
        const inputKey = parentKey ? `${parentKey}.${item.name}` : `${item.name}`;
        return renderObject(item, inputKey, getDataValue(dataSource, item.name));
      });
    }
    const { params, image, resources } = resourceParam.data;

    const parentObjectData = getDataValue(dataSource, 'params');
    const paramsContent = map(params, (value: any, itemKey: string) => {
      const inputKey = parentKey ? `${parentKey}.params.${itemKey}` : `params.${itemKey}`;
      return renderObject(value, inputKey, getDataValue(parentObjectData, itemKey));
    });

    return (
      <>
        {actionConfig.name === 'custom-script' ? (
          <div>{renderObject(image, 'resource.image', getDataValue(dataSource, 'image'))}</div>
        ) : null}
        <div>
          <div className="resource-input-group-title">{i18nMap.params}: </div>
          {paramsContent}
        </div>
        <div>{renderObject(resources, 'resource.resources', getDataValue(dataSource, 'resources'))}</div>
      </>
    );
  };

  const renderObject = (value: any, parentKey: string, dataSource?: any) => {
    if (!isObject(value.type)) {
      return renderPropertyValue(value, parentKey, dataSource);
    }

    if (value.type === 'string_array') {
      return renderStringArray(value, parentKey);
    }

    if (value.type === 'struct_array') {
      return renderStructArray(value, parentKey);
    }

    if (value.type === 'map') {
      return renderMap(value, parentKey, dataSource);
    }

    const content = renderResource({ data: value.struct }, parentKey, dataSource);
    if (!content || !Object.values(content).some((c) => c)) return null;

    return (
      <div key={parentKey}>
        <span className="resource-input-group-title">{i18nMap[value.name] || value.name}: </span>
        <div>{content}</div>
      </div>
    );
  };

  const renderMap = (value: any, parentKey: string, dataSource?: any) => {
    let initialValue = isCreate ? value.default : value.value || value.default;

    if (dataSource) {
      initialValue = dataSource;
    }

    if (!editing && !initialValue) {
      return null;
    }

    const inputField = (
      <Item
        key={parentKey}
        name={formKeyFormat(parentKey)}
        initialValue={initialValue}
        rules={[
          {
            required: value.required,
            message: i18n.t('dop:this item cannot be empty'),
          },
        ]}
      >
        <VariableInput disabled={!editing} label={getLabel(value.name, value.desc)} />
      </Item>
    );
    return inputField;
  };

  const renderStringArray = (value: any, parentKey: string) => {
    const inputField = (
      <Item
        key={parentKey}
        name={formKeyFormat(parentKey)}
        initialValue={isCreate ? value.default : value.value || value.default}
        rules={[
          {
            required: value.required,
            message: i18n.t('dop:this item cannot be empty'),
          },
        ]}
        getValueFromEvent={(val: Array<{ value: string }>) => {
          return val?.length ? val.map((v) => v.value) : val;
        }}
      >
        <ListInput disabled={!editing} label={getLabel(value.name, value.desc)} />
      </Item>
    );
    return inputField;
  };

  const renderPropertyValue = (value: any, parentKey: string, dataSource?: any) => {
    let input;
    let initialValue = isCreate ? value.default : value.value || value.default;

    if (dataSource) {
      initialValue = dataSource;
    }

    if (!editing && !initialValue) {
      return null;
    }

    const unit = value.unit ? <span>{value.unit}</span> : null;

    switch (value.type) {
      case 'float':
      case 'int':
        input = (
          <InputNumber
            disabled={!editing || value.readOnly}
            className="w-full"
            placeholder={i18n.t('dop:please enter data')}
          />
        );
        break;
      default:
        input = (
          <Input
            disabled={!editing || value.readOnly}
            placeholder={i18n.t('dop:please enter data')}
            addonAfter={unit}
          />
        );
        break;
    }

    const inputField = (
      <Item
        key={parentKey}
        label={getLabel(value.name, value.desc)}
        name={formKeyFormat(parentKey)}
        initialValue={initialValue}
        rules={[
          {
            required: value.required,
            message: i18n.t('dop:this item cannot be empty'),
          },
        ]}
      >
        {input}
      </Item>
    );
    return inputField;
  };

  const getLabel = (label: string, labelTip: string) => {
    let _label: any = label;
    if (labelTip) {
      _label = (
        <span>
          {_label}&nbsp;
          <Tooltip title={labelTip}>
            <ErdaIcon type="help" size="14" className="mr-1 align-middle text-icon" />
          </Tooltip>
        </span>
      );
    }
    return _label;
  };

  const renderStructArray = (property: any, parentKey: string) => {
    if ((!editing && !property.value) || (!editing && property.value && !property.value.length)) {
      return null;
    }

    const addBtn = editing ? (
      <ErdaIcon
        type="plus"
        className="cursor-pointer align-middle"
        onClick={() => addNewItemToStructArray(property, property.struct[0])}
      />
    ) : null;
    // getFieldDecorator(`${parentKey}-data`, { initialValue: property.value || [] });
    const data = property.value || []; // getFieldValue(`${parentKey}-data`);
    const _val = form.getFieldsValue();
    const realData = get(_val, `${parentKey}`) || [];
    const content = data.map((item: any, index: number) => {
      const keys = Object.keys(item);
      const curItem = realData[index] || item;
      const nameKey = get(property.struct, '[0].name');
      const headName = curItem[nameKey] || (typeof curItem[keys[0]] === 'string' ? curItem[keys[0]] : 'module');
      const header = (
        <div className="flex items-center justify-between">
          <span className="truncate" title={headName}>
            {headName}
          </span>
          {editing ? (
            <CustomIcon
              onClick={() => deleteItemFromStructArray(property, index, parentKey)}
              className="icon-delete"
              type="sc1"
            />
          ) : null}
        </div>
      );
      return (
        <Panel key={`${parentKey}.${item.key}-${String(index)}`} header={header} forceRender>
          {renderResource({ data: property.struct }, `${parentKey}.[${index}]`, item)}
        </Panel>
      );
    });

    return (
      <div key={`${parentKey}`}>
        <span className="resource-input-group-title">
          {property.name}:{addBtn}
        </span>
        {data.length ? (
          <Collapse className="collapse-field my-2" accordion>
            {content}
          </Collapse>
        ) : null}
      </div>
    );
  };

  const deleteItemFromStructArray = (property: any, index: number, parentKey: string) => {
    if (!property.value) {
      // eslint-disable-next-line no-param-reassign
      property.value = [];
    }
    property.value.splice(index, 1);
    updater.resource(cloneDeep(resource));

    const formDatas = form.getFieldValue(`${parentKey}`.split('.'));
    formDatas?.splice(index, 1);
    const curFormData = form.getFieldsValue();
    set(curFormData, parentKey, formDatas);
    form.setFieldsValue(curFormData);
  };

  const addNewItemToStructArray = (property: any, struct: any) => {
    if (!property.value) {
      // eslint-disable-next-line no-param-reassign
      property.value = [];
    }
    property.value.push({
      [struct.name]: `module-${property.value.length + 1}`,
    });
    updater.resource(cloneDeep(resource));
  };

  const isObject = (inputType: string) => {
    return ['map', 'string_array', 'struct_array', 'struct'].includes(inputType);
  };

  const onSubmit = () => {
    form
      .validateFields()
      .then((values: any) => {
        let data = cloneDeep(values);
        const resources = head(filter(resource.data, (item) => item.name === 'resources'));
        const originResource = transform(
          get(resources, 'struct'),
          (result, item: { name: string; default: string | number }) => {
            const { name, default: d } = item;
            // eslint-disable-next-line no-param-reassign
            result[name] = +d;
          },
          {},
        );
        const editedResources = get(data, 'resource.resources') || {};
        forEach(Object.entries(editedResources), ([key, value]) => {
          editedResources[key] = +(value as string);
        });
        const isResourceDefault = isEqual(editedResources, originResource);

        if (isResourceDefault) {
          data = omit(data, ['resource.resources']);
        }
        const _type = get(values, 'resource.type');
        if (_type === 'custom-script') {
          // 自定义任务，如果镜像值跟默认的一直，则不保存这个字段；
          const defaultImg = get(find(resource.data, { name: 'image' }), 'default');
          if (defaultImg === get(data, 'resource.image')) {
            data = omit(data, ['resource.image']);
          }
        }
        const filledFieldsData = clearEmptyField(data);
        const resData = { ...filledFieldsData, action: chosenAction } as any;
        if (data.executionCondition) resData.executionCondition = data.executionCondition;
        handleSubmit(resData);
      })
      .catch(({ errorFields }: { errorFields: Array<{ name: any[]; errors: any[] }> }) => {
        form.scrollToField(errorFields[0].name);
      });
  };

  const clearEmptyField = (ObjData: any) => {
    const filledFields: string[] = [];
    const findData = (obj: any, parentArray: string[]) => {
      Object.keys(obj).forEach((key) => {
        const currentParent = [...parentArray, key];
        const value = get(obj, key);
        if (typeof value === 'object' && value !== null) {
          findData(value, currentParent);
        } else if (value || value === 0) {
          filledFields.push(currentParent.join('.'));
        }
      });
    };
    findData(ObjData, []);
    return pick(ObjData, filledFields);
  };

  const executionCondition = (
    <Item
      label={i18n.t('common:execution conditions')}
      name={'executionCondition'}
      initialValue={get(propsNodeData, 'if') || undefined}
      rules={[
        {
          required: false,
        },
      ]}
    >
      <Input disabled={!editing} placeholder={i18n.t('common:configure execution conditions')} />
    </Item>
  );

  const onValuesChange = () => {
    // use changeKey to tigger a rerender,
    updater.changeKey((prev: number) => prev + 1);
  };

  return (
    <Form form={form} onValuesChange={onValuesChange} layout="vertical" className="edit-service-container">
      {alert}
      {taskType}
      {loopData}
      {type ? taskName : null}
      {actionVersion}
      {executionCondition}

      {renderTaskTypeStructure()}
      {editing ? (
        <Button type="primary" ghost onClick={onSubmit}>
          {i18n.t('save')}
        </Button>
      ) : null}
    </Form>
  );
};

export const PipelineNodeFormV1 = PurePipelineNodeForm;

export interface IPipelineNodeDrawerProps extends IEditStageProps {
  closeDrawer: () => void;
  visible: boolean;
}
const PipelineNodeDrawer = (props: IPipelineNodeDrawerProps) => {
  const { nodeData: propsNodeData, editing, closeDrawer, visible, isCreate } = props;
  let title = '';
  if (isCreate) {
    title = i18n.t('dop:new node');
  } else {
    title = `${editing ? i18n.t('edit') : i18n.t('common:view')} ${get(propsNodeData, 'alias') || ''}`;
  }
  const [key, setKey] = React.useState(1);

  React.useEffect(() => {
    setKey((prev) => prev + 1);
  }, [visible]);

  return (
    <Drawer className="yml-node-drawer" title={title} visible={visible} width={560} onClose={closeDrawer}>
      <PipelineNodeForm key={key} {...(props as any)} />
    </Drawer>
  );
};

export enum ActionType {
  autoTest = 'autoTest',
  configSheet = 'configSheet',
  appPipeline = 'appPipeline',
  projectLevelAppPipeline = 'projectLevelAppPipeline',
}

export const actionQuery = {
  autoTest: { labels: 'autotest:true' },
  configSheet: { labels: 'configsheet:true' },
  projectLevelAppPipeline: { labels: 'project_level_app:true' },
};

interface IPipelineNodeForm {
  editing: boolean;
  nodeData?: null | AUTO_TEST.ICaseDetail;
  visible: boolean;
  scope: string;
}

export const PipelineNodeForm = (props: IPipelineNodeForm) => {
  const { editing, nodeData: propsNodeData, visible, scope = '' } = props;
  const [{ chosenActionName, chosenAction, originActions, useProtocol, chosenActionSpec }, updater, update] = useUpdate(
    {
      chosenActionName: '',
      chosenAction: null,
      originActions: [] as any[],
      useProtocol: false,
      chosenActionSpec: [] as DEPLOY.ActionConfig[],
    },
  );

  const { getActionConfigs } = appDeployStore.effects;
  const [loading] = useLoading(appDeployStore, ['getActionConfigs']);

  useEffectOnce(() => {
    visible &&
      (getActionGroup(actionQuery[scope]) as unknown as Promise<any>).then((res: any) => {
        updater.originActions(get(res, 'data.action'));
      });
  });
  React.useEffect(() => {
    if (!visible) {
      update({ chosenActionName: '' });
    }
  }, [update, visible]);

  const curType = propsNodeData?.type;
  React.useEffect(() => {
    if (curType) {
      updater.chosenActionName(curType);
    }
  }, [updater, curType]);

  React.useEffect(() => {
    if (protocolActionForms.includes(chosenActionName)) {
      updater.useProtocol(true);
    } else if (chosenActionName) {
      getActionConfigs({ actionType: chosenActionName }).then((result: DEPLOY.ActionConfig[]) => {
        const protocolSpec = get(result, '[0].spec.useProtocol');
        update({
          chosenActionSpec: result,
          useProtocol: !!protocolSpec,
        });
      });
    } else {
      update({
        chosenActionSpec: [],
        useProtocol: false,
      });
    }
  }, [chosenActionName, updater, getActionConfigs, update]);

  React.useEffect(() => {
    const curAction = find(
      flatten(map(originActions, (item) => item.items)),
      (curItem) => curItem.name === chosenActionName,
    );
    updater.chosenAction(curAction);
  }, [chosenActionName, originActions, updater]);

  const changeResourceType = (val: string) => {
    updater.chosenActionName(val || '');
  };

  const curNodeData = chosenActionName === propsNodeData?.type ? propsNodeData : undefined;
  return (
    <Spin spinning={loading}>
      <ActionSelect
        disabled={!editing}
        label={i18n.t('task type')}
        originActions={originActions}
        onChange={changeResourceType}
        value={chosenActionName}
        placeholder={`${i18n.t('dop:please choose task type')}`}
      />
      <IF check={!isEmpty(chosenAction)}>
        <IF check={!!useProtocol}>
          {/* 使用组件化协议表单 */}
          <ActionConfigForm
            chosenAction={chosenAction}
            chosenActionName={chosenActionName}
            {...(props as any)}
            nodeData={curNodeData}
          />
          <IF.ELSE />
          <PipelineNodeFormV1
            chosenAction={chosenAction}
            chosenActionName={chosenActionName}
            {...(props as any)}
            actionSpec={chosenActionSpec}
            nodeData={curNodeData}
          />
        </IF>
      </IF>
    </Spin>
  );
};

export default PipelineNodeDrawer;
