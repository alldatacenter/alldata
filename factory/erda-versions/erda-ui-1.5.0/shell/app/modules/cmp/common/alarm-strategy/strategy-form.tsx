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
  map,
  isEmpty,
  isNull,
  every,
  forEach,
  uniqueId,
  filter,
  find,
  findIndex,
  fill,
  cloneDeep,
  isArray,
} from 'lodash';
import { useMount, useUnmount } from 'react-use';
import { FormInstance } from 'core/common/interface';
import { Modal, Button, Switch, Select, Input, InputNumber, Popover, Tooltip, Form } from 'antd';
import Table from 'common/components/table';
import { IActions } from 'common/components/table/interface';
import { RenderForm, ErdaIcon } from 'common';
import { useUpdate } from 'common/use-hooks';
import { goTo } from 'common/utils';
import { ColumnProps } from 'app/interface/common';
import i18n from 'i18n';
import notifyGroupStore from 'application/stores/notify-group';
import orgMemberStore from 'common/stores/org-member';
import projectMemberStore from 'common/stores/project-member';
import cmpAlarmStrategyStore from 'app/modules/cmp/stores/alarm-strategy';
import mspAlarmStrategyStore from 'app/modules/msp/alarm-manage/alarm-strategy/stores/alarm-strategy';
import {
  notifyChannelOptionsMap,
  getFinalNotifyChannelOptions,
} from 'application/pages/settings/components/app-notify/common-notify-group';
import { usePerm } from 'user/common';
import { getNotifyChannelMethods } from 'org/services/notice-channel';
import routeInfoStore from 'core/stores/route';
import { TriggerConditionSelect } from './trigger-condition-select';
import { NotifyStrategySelect } from './notify-strategy-select';
import './index.scss';

const { warning } = Modal;
const { Option } = Select;

enum ScopeType {
  ORG = 'org',
  PROJECT = 'project',
  MSP = 'msp',
}

enum SilencePeriodType {
  FIXED = 'fixed',
  DOUBLED = 'doubled',
}

const SILENCE_PERIOD_POLICY_MAP = {
  [SilencePeriodType.FIXED]: i18n.t('cmp:fixed'),
  [SilencePeriodType.DOUBLED]: i18n.t('cmp:doubled'),
};

const alarmStrategyStoreMap = {
  [ScopeType.ORG]: cmpAlarmStrategyStore,
  [ScopeType.MSP]: mspAlarmStrategyStore,
};

const memberStoreMap = {
  [ScopeType.ORG]: orgMemberStore,
  [ScopeType.MSP]: projectMemberStore,
};

const notifyGroupPage = {
  [ScopeType.ORG]: goTo.pages.cmpNotifyGroup,
  [ScopeType.MSP]: goTo.pages.mspProjectNotifyGroup,
};

const alertLevelOptions = [
  {
    key: 'Fatal',
    display: i18n.t('cmp:Fatal'),
  },
  {
    key: 'Critical',
    display: i18n.t('cmp:Critical'),
  },
  {
    key: 'Warning',
    display: i18n.t('cmp:Warning'),
  },
  {
    key: 'Notice',
    display: i18n.t('cmp:Notice'),
  },
];

const conditionOperatorOptions = [
  {
    key: 'eq',
    display: i18n.t('msp:equal'),
    type: 'single',
  },
  {
    key: 'neq',
    display: i18n.t('msp:not equal'),
    type: 'single',
  },
  {
    key: 'in',
    display: i18n.t('in'),
    type: 'multiple',
  },
  {
    key: 'notIn',
    display: i18n.t('not in'),
    type: 'multiple',
  },
  {
    key: 'match',
    display: i18n.t('msp:match'),
    type: 'input',
  },
  {
    key: 'notMatch',
    display: i18n.t('msp:not match'),
    type: 'input',
  },
  {
    key: 'all',
    display: i18n.t('msp:all'),
    type: 'none',
  },
];

interface IProps {
  scopeType: ScopeType.ORG | ScopeType.MSP;
  scopeId: string;
  commonPayload?: Obj;
}

const StrategyForm = ({ scopeType, scopeId, commonPayload }: IProps) => {
  const memberStore = memberStoreMap[scopeType];
  const params = routeInfoStore.useStore((s) => s.params);
  const { id: strategyId, projectId = '', terminusKey = '', orgName = '' } = params;
  const [form] = Form.useForm();
  const { getRoleMap } = memberStore.effects;
  const alarmStrategyStore = alarmStrategyStoreMap[scopeType];
  const [alertTypes, alertTriggerConditions, alertTriggerConditionsContent] = alarmStrategyStore.useStore((s) => [
    s.alertTypes,
    s.alertTriggerConditions,
    s.alertTriggerConditionsContent,
  ]);
  const tableRef = React.useRef<HTMLDivElement>(null);
  const channelMethods = getNotifyChannelMethods.useData() as Obj<string>;
  const {
    getAlerts,
    createAlert,
    editAlert,
    getAlertDetail,
    getAlarmScopes,
    getAlertTypes,
    getAlertTriggerConditions,
    getAlertTriggerConditionsContent,
  } = alarmStrategyStore.effects;
  const { clearAlerts } = alarmStrategyStore.reducers;
  const { getNotifyGroups } = notifyGroupStore.effects;
  const notifyGroups = notifyGroupStore.useStore((s) => s.notifyGroups);
  const orgAddNotificationGroupAuth = usePerm((s) => s.org.cmp.alarms.addNotificationGroup.pass);

  // backend support the filterMap to match data
  const triggerConditionFilters = {
    org_name: orgName,
    project_id: projectId,
    terminus_key: terminusKey,
  };

  const addNotificationGroupAuth = scopeType === ScopeType.ORG ? orgAddNotificationGroupAuth : true; // 企业中心的添加通知组，需要验证权限，项目的暂无埋点

  const [state, updater, update] = useUpdate({
    editingRules: [] as any,
    editingFormRule: {},
    activeGroupId: undefined,
    triggerConditionValueOptions: [],
    triggerCondition: [],
    notifies: [],
    notifyLevel: null,
    allChannelMethods: notifyChannelOptionsMap,
  });

  useMount(() => {
    let payload = { scopeType, scopeId };
    if (scopeType === ScopeType.MSP) {
      payload = {
        scopeType: commonPayload?.scopeType,
        scopeId: commonPayload?.scopeId,
      };
    }
    getAlerts();
    getAlarmScopes();
    getAlertTypes();
    getNotifyGroups({ ...payload, pageSize: 100 });
    getRoleMap({ scopeType, scopeId: scopeType === ScopeType.MSP ? commonPayload?.scopeId : scopeId });
    getAlertTriggerConditions(scopeType);
    getNotifyChannelMethods.fetch();
  });

  React.useEffect(() => {
    if (strategyId) {
      getAlertDetail(Number(strategyId)).then(
        ({ name, clusterNames, appIds, rules, notifies, triggerCondition }: any) => {
          updater.editingFormRule({
            id: strategyId,
            name,
            clusterName: clusterNames || [],
            appId: appIds || [],
            notifies,
          });
          form.setFieldsValue({
            name,
            silence: notifies
              ? `${notifies[0].silence.value}-${state.editingFormRule.notifies[0].silence.unit}`
              : undefined,
            silencePolicy: notifies ? `${state.editingFormRule.notifies[0].silence.policy}` : SilencePeriodType.FIXED,
          });
          updater.editingRules(
            map(rules, (rule) => ({
              key: uniqueId(),
              ...rule,
              level: rule?.level === 'WARNING' ? undefined : rule?.level,
            })),
          );
          updater.activeGroupId(notifies[0].groupId);

          updater.triggerCondition(
            (triggerCondition || []).map((x) => ({
              id: uniqueId(),
              condition: x.condition,
              operator: x.operator,
              values: x.values,
              valueOptions:
                alertTriggerConditionsContent
                  ?.find((item) => item.key === x.condition)
                  ?.options.map((y) => ({
                    key: y,
                    display: y,
                  })) ?? [],
            })),
          );

          updater.notifies(
            (notifies || []).map((x) => ({
              id: uniqueId(),
              groupId: x.groupId,
              level: x.level ? x.level?.split(',') : undefined,
              groupType: x.groupType?.split(','),
              groupTypeOptions:
                (state.allChannelMethods[x.notifyGroup.targets?.[0].type] || []).map((y) => ({
                  key: y.value,
                  display: y.name,
                })) || [],
            })),
          );
        },
      );
    } else {
      updater.notifies([
        {
          id: uniqueId(),
          groupId: undefined,
          groupType: undefined,
          level: undefined,
          groupTypeOptions: [],
        },
      ]);
      updater.editingRules([
        {
          key: uniqueId(),
          name: undefined,
          window: undefined,
          functions: [],
          isRecover: true,
          level: 'Fatal',
        },
      ]);
    }
  }, [alertTriggerConditionsContent]);

  React.useEffect(() => {
    if (alertTriggerConditions?.length) {
      const query = [] as COMMON_STRATEGY_NOTIFY.IAlertTriggerConditionQueryItem[];
      forEach(alertTriggerConditions, (item) => {
        const { index, key, filters } = item;
        const filterMap = {};
        forEach(filters, (x) => {
          if (x in triggerConditionFilters) {
            filterMap[x] = triggerConditionFilters[x];
          }
        });
        query.push({ index, condition: key, filters: filterMap });
      });
      getAlertTriggerConditionsContent(query);
    }
  }, [alertTriggerConditions]);

  React.useEffect(() => {
    updater.allChannelMethods(getFinalNotifyChannelOptions(channelMethods, true));
  }, [channelMethods, updater]);

  useUnmount(() => {
    clearAlerts();
  });

  // 获取规则枚举
  const windows = React.useMemo(() => alertTypes.windows, [alertTypes.windows]);
  const silenceMap = React.useMemo(() => {
    const result = {};
    forEach(alertTypes.silence, (item) => {
      result[`${item.value}-${item.unit.key}`] = item.unit;
    });
    return result;
  }, [alertTypes.silence]);
  const operatorMap = React.useMemo(() => {
    const result = {};
    forEach(alertTypes.operators, (item) => {
      result[item.key] = item.display;
    });
    return result;
  }, [alertTypes.operators]);
  const aggregatorMap = React.useMemo(() => {
    const result = {};
    forEach(alertTypes.aggregator, (item) => {
      result[item.key] = item.display;
    });
    return result;
  }, [alertTypes.aggregator]);
  const [alertTypeRuleMap, allRuleFieldMap, allRuleMap, allRules] = React.useMemo(() => {
    const _alertTypeRuleMap = {};
    const _allRuleMap = {};
    const _allRuleFieldMap = {};
    let _allRules: any[] = [];
    forEach(alertTypes.alertTypeRules, ({ alertType, rules }) => {
      _alertTypeRuleMap[alertType.key] = rules;
      forEach(rules, (item) => {
        _allRuleMap[item.alertIndex.key] = item.alertIndex.display;
        forEach(item.functions, (subItem) => {
          _allRuleFieldMap[subItem.field.key] = subItem.field.display;
        });
      });
      _allRules = _allRules.concat(
        map(rules, ({ alertIndex, functions, ...rest }) => ({
          level: alertLevelOptions?.[0]?.key,
          alertIndex: alertIndex.key,
          functions: map(functions, ({ field, ...subRest }) => ({ field: field.key, ...subRest })),
          ...rest,
        })),
      );
    });
    return [_alertTypeRuleMap, _allRuleFieldMap, _allRuleMap, _allRules];
  }, [alertTypes.alertTypeRules]);

  const getFunctionsValueElement = (item: any, functionIndex: number, key: string) => {
    let functionsValueElement = null;
    switch (typeof item.value) {
      case 'boolean':
        functionsValueElement = (
          <Switch
            checkedChildren="true"
            unCheckedChildren="false"
            defaultChecked={item.value}
            onClick={(v: boolean) => {
              handleEditEditingRuleField(key, functionIndex, { key: 'value', value: v });
            }}
          />
        );
        break;
      case 'string':
        functionsValueElement = (
          <Input
            className="value"
            defaultValue={item.value}
            onChange={(e: any) => {
              handleEditEditingRuleField(key, functionIndex, { key: 'value', value: e.target.value });
            }}
          />
        );
        break;
      case 'number':
        functionsValueElement = (
          <InputNumber
            className="value"
            min={0}
            defaultValue={item.value}
            onChange={(v: string | number | undefined) => {
              handleEditEditingRuleField(key, functionIndex, { key: 'value', value: Number(v) });
            }}
          />
        );
        break;
      default:
        break;
    }
    return functionsValueElement;
  };

  const columns: Array<ColumnProps<COMMON_STRATEGY_NOTIFY.IFormRule>> = [
    {
      title: i18n.t('cmp:rule name'),
      dataIndex: 'alertIndex',
      render: (value: string, { key }) => (
        <Select
          value={value}
          getPopupContainer={() => tableRef.current as HTMLElement}
          showSearch
          optionFilterProp="children"
          placeholder={i18n.t('please select')}
          onSelect={(alertIndex: any) => {
            const rules = cloneDeep(state.editingRules);
            const rule = find(allRules, { alertIndex });
            const index = findIndex(rules, { key });
            fill(rules, { key, ...rule }, index, index + 1);
            updater.editingRules(rules);
          }}
        >
          {map(allRules, ({ alertIndex, id }) => (
            <Select.Option key={id} value={alertIndex}>
              {allRuleMap[alertIndex]}
            </Select.Option>
          ))}
        </Select>
      ),
    },
    {
      title: `${i18n.t('cmp:duration')}(min)`,
      dataIndex: 'window',
      render: (value: number, { key }: COMMON_STRATEGY_NOTIFY.IFormRule) => (
        <Select
          value={value}
          placeholder={i18n.t('please select')}
          getPopupContainer={() => tableRef.current as HTMLElement}
          onSelect={(window: any) => handleEditEditingRule(key, { key: 'window', value: Number(window) })}
        >
          {map(windows, (item) => (
            <Select.Option key={item} value={item}>
              {item}
            </Select.Option>
          ))}
        </Select>
      ),
    },
    {
      title: i18n.t('cmp:aggregation rules'),
      dataIndex: 'functions',
      render: (functions: any[], { key }: COMMON_STRATEGY_NOTIFY.IFormRule) => (
        <div className="function-list">
          {functions?.length === 0 && <Input placeholder={i18n.t('cmp:please enter here')} />}
          {map(functions, (item, index) => (
            <div className="function-item flex-div flex items-center" key={item.field}>
              <Tooltip title={allRuleFieldMap[item.field]}>
                <span className="field-name mr-2 nowrap">{allRuleFieldMap[item.field]}</span>
              </Tooltip>
              <span className="aggregator mr-2">{aggregatorMap[item.aggregator]}</span>
              {/* <Select
                  className="aggregator mr-2"
                  defaultValue={item.aggregator}
                  disabled
                >
                  {map(aggregatorMap, (name, _key) => (<Select.Option key={_key} value={_key}>{name}</Select.Option>))}
                </Select> */}
              <Select
                className="operator mr-2"
                defaultValue={item.operator}
                getPopupContainer={() => tableRef.current as HTMLElement}
                onSelect={(value: any) => {
                  handleEditEditingRuleField(key, index, { key: 'operator', value: String(value) });
                }}
              >
                {map(operatorMap, (name, _key) => (
                  <Select.Option key={_key} value={_key}>
                    {name}
                  </Select.Option>
                ))}
              </Select>
              {getFunctionsValueElement(item, index, key)}
            </div>
          ))}
        </div>
      ),
    },
    {
      title: i18n.t('cmp:alarm level'),
      dataIndex: 'level',
      render: (value: string, { key }) => (
        <Select
          className="operator mr-2"
          value={value}
          getPopupContainer={() => tableRef.current as HTMLElement}
          onSelect={(level: string) => {
            handleEditEditingRule(key, { key: 'level', value: level });
          }}
        >
          {map(alertLevelOptions, (item) => (
            <Option key={item.key} value={item.key}>
              {item.display}
            </Option>
          ))}
        </Select>
      ),
    },
    {
      title: i18n.t('cmp:trigger recover'),
      dataIndex: 'isRecover',
      render: (isRecover: boolean, { key }: COMMON_STRATEGY_NOTIFY.IFormRule) => (
        <>
          <Switch
            checked={isRecover}
            onChange={(checked) => handleEditEditingRule(key, { key: 'isRecover', value: checked })}
          />
        </>
      ),
    },
  ];

  const actions: IActions<COMMON_STRATEGY_NOTIFY.IFormRule> = {
    render: (record) => [
      {
        title: i18n.t('delete'),
        onClick: () => {
          handleRemoveEditingRule(record.key);
        },
      },
    ],
  };

  const fieldsList = [
    {
      label: i18n.t('cmp:alarm name'),
      name: 'name',
      itemProps: {
        placeholder: i18n.t('cmp:please enter here'),
        disabled: !isEmpty(state.editingFormRule),
        maxLength: 50,
        style: { width: 480 },
      },
    },
    {
      label: i18n.t('cmp:filter rule'),
      name: 'triggerCondition',
      required: false,
      getComp: () => (
        <>
          <Button className="flex items-center mb-2" type="primary" ghost onClick={handleAddTriggerConditions}>
            <ErdaIcon type="plus" size="16" />
            <span>{i18n.t('cmp:add rule')}</span>
          </Button>
          {state.triggerCondition?.length > 0 && (
            <div className="p-2 bg-cultured w-min">
              {state.triggerCondition?.map((item) => (
                <TriggerConditionSelect
                  keyOptions={alertTriggerConditions}
                  key={item.id}
                  id={item.id}
                  current={state.triggerCondition?.find((x) => x.id === item.id)}
                  handleEditTriggerConditions={handleEditTriggerConditions}
                  handleRemoveTriggerConditions={handleRemoveTriggerConditions}
                  operatorOptions={conditionOperatorOptions}
                  valueOptionsList={alertTriggerConditionsContent}
                />
              ))}
            </div>
          )}
        </>
      ),
    },
    {
      label: i18n.t('cmp:alarm rule'),
      name: 'expressions',
      required: false,
      getComp: () => (
        <div ref={tableRef}>
          <div className="opportunity-header flex mb-2">
            <Popover
              placement="bottomLeft"
              trigger="click"
              content={
                <div className="alarm-rule-collection">
                  {map(alertTypes.alertTypeRules, (item) => (
                    <div
                      className="collection-item hover-active-bg"
                      key={item.alertType.key}
                      onClick={() => {
                        handleClickAlertType(item.alertType.key);
                      }}
                    >
                      {item.alertType.display}
                    </div>
                  ))}
                </div>
              }
            >
              <Button className="mr-2 flex items-center" ghost type="primary">
                <ErdaIcon type="page-template" className="mr-1" size="14" />
                <span>{i18n.t('cmp:type template')}</span>
              </Button>
            </Popover>
            <Button type="primary" className="flex items-center" ghost onClick={handleAddEditingRule}>
              <ErdaIcon type="plus" size="16" />
              <span>{i18n.t('cmp:add rule')}</span>
            </Button>
          </div>
          <Table
            hideHeader
            rowKey="key"
            actions={actions}
            className="opportunity-table"
            dataSource={state.editingRules}
            columns={columns}
          />
        </div>
      ),
    },
    {
      label: i18n.t('cmp:silence period'),
      name: 'silence',
      itemProps: {
        style: { width: 480 },
      },
      type: 'select',
      options: map(silenceMap, ({ display }, value) => ({ name: `${value.split('-')[0]}${display}`, value })),
    },
    {
      label: i18n.t('silence period policy'),
      name: 'silencePolicy',
      initialValue: state.editingFormRule.notifies
        ? `${state.editingFormRule.notifies[0].silence.policy}`
        : SilencePeriodType.FIXED,
      type: 'radioGroup',
      options: map(SILENCE_PERIOD_POLICY_MAP, (name, value) => ({ name, value })),
    },
    {
      label: i18n.t('dop:notified to'),
      required: false,
      name: 'notifies',
      getComp: () => (
        <>
          <Button type="primary" ghost className="flex items-center mb-2" onClick={handleAddNotifyStrategy}>
            <ErdaIcon type="plus" size="16" />
            <span>{i18n.t('cmp:add notification object')}</span>
          </Button>
          {state.notifies?.length > 0 && (
            <div className="p-2 bg-cultured w-min">
              {state.notifies?.map((item) => (
                <NotifyStrategySelect
                  alertLevelOptions={alertLevelOptions}
                  goToNotifyGroup={() => {
                    goTo(notifyGroupPage[scopeType], { projectId: scopeId, ...params });
                  }}
                  notifyGroups={notifyGroups}
                  notifyChannelMap={state.allChannelMethods}
                  addNotificationGroupAuth={addNotificationGroupAuth}
                  key={item.id}
                  id={item.id}
                  updater={updater.activeGroupId}
                  current={state.notifies?.find((x) => x.id === item.id)}
                  handleEditNotifyStrategy={handleEditNotifyStrategy}
                  handleRemoveNotifyStrategy={handleRemoveNotifyStrategy}
                  valueOptions={item.groupTypeOptions}
                />
              ))}
            </div>
          )}
        </>
      ),
    },
    {
      label: '',
      getComp: ({ form }: { form: FormInstance }) => {
        return (
          <div className="text-right bg-white">
            <Button className="btn-save" type="primary" onClick={() => handleSave(form)}>
              {i18n.t('save')}
            </Button>
            <Button className="ml-3" onClick={() => window.history.back()}>
              {i18n.t('cancel')}
            </Button>
          </div>
        );
      },
    },
  ];

  // 添加集合的规则
  const handleClickAlertType = (val: string) => {
    const formRules: COMMON_STRATEGY_NOTIFY.IFormRule[] = map(
      alertTypeRuleMap[val],
      (rule: COMMON_STRATEGY_NOTIFY.IDataExpression) => ({
        key: uniqueId(),
        alertIndex: rule.alertIndex.key,
        window: rule.window,
        functions: map(rule.functions, ({ field, ...rest }) => ({
          field: field.key,
          ...rest,
        })),
        isRecover: rule.isRecover,
        level: alertLevelOptions?.[0]?.key,
      }),
    );
    updater.editingRules([...formRules, ...state.editingRules]);
  };

  // 添加单条规则
  const handleAddEditingRule = () => {
    updater.editingRules([
      {
        key: uniqueId(),
        name: undefined,
        window: undefined,
        functions: [],
        isRecover: true,
        level: 'Fatal',
      },
      ...state.editingRules,
    ]);
  };

  // 移除表格编辑中的规则
  const handleRemoveEditingRule = (key: string) => {
    updater.editingRules(filter(state.editingRules, (item) => item.key !== key));
  };

  // 编辑单条规则
  const handleEditEditingRule = (key: string, item: { key: string; value: any }) => {
    const rules = cloneDeep(state.editingRules);
    const rule = find(rules, { key });
    const index = findIndex(rules, { key });

    fill(rules, { key, ...rule, [item.key]: item.value }, index, index + 1);
    updater.editingRules(rules);
  };

  // 编辑单条规则下的指标
  const handleEditEditingRuleField = (key: string, index: number, item: { key: string; value: any }) => {
    const rules = cloneDeep(state.editingRules);
    const { functions } = find(rules, { key }) || {};
    const functionItem = functions[index];

    fill(functions, { ...functionItem, [item.key]: item.value }, index, index + 1);
    handleEditEditingRule(key, { key: 'functions', value: functions });
  };

  const handleSave = (form: FormInstance) => {
    form
      .validateFields()
      .then((values) => {
        const { name, silence = '', silencePolicy } = values;
        const [value, unit] = silence.split('-');
        const payload: COMMON_STRATEGY_NOTIFY.IAlertBody = {
          name,
          domain: location.origin,
          rules: map(state.editingRules, ({ key, ...rest }) => rest),
          notifies: state.notifies.map((item) => ({
            silence: {
              value: Number(value),
              unit,
              policy: silencePolicy,
            },
            groupId: item?.groupId,
            groupType: item?.groupType?.join(','),
            level: item?.level?.join(','),
          })),
          triggerCondition: state.triggerCondition.map((x) => ({
            condition: x.condition,
            operator: x.operator,
            values: x.values,
          })),
        };
        if (beforeSubmit(values)) {
          if (!isEmpty(state.editingFormRule)) {
            editAlert({ body: payload, id: state.editingFormRule.id });
          } else {
            createAlert(payload);
          }
          window.history.back();
        }
      })
      .catch(({ errorFields }) => {
        form.scrollToField(errorFields[0].name);
      });
  };

  // 添加单条触发条件
  const handleAddTriggerConditions = () => {
    // const currentTriggerValues =
    //   alertTriggerConditionsContent
    //     ?.find((item) => item.key === alertTriggerConditions?.[0]?.key)
    //     ?.options.map((item) => ({ key: item, display: item })) ?? [];

    updater.triggerCondition([
      {
        id: uniqueId(),
        condition: undefined,
        operator: conditionOperatorOptions?.[0].key,
        values: undefined,
        valueOptions: [],
      },
      ...(state.triggerCondition || []),
    ]);
  };

  // 添加单条通知策略
  const handleAddNotifyStrategy = () => {
    // const activeGroup = notifyGroups[0];
    // const groupTypeOptions =
    //   ((activeGroup && notifyChannelOptionsMap[activeGroup.targets[0].type]) || []).map((x) => ({
    //     key: x.value,
    //     display: x.name,
    //   })) || [];
    // updater.groupTypeOptions(groupTypeOptions);
    updater.notifies([
      {
        id: uniqueId(),
        groupId: undefined,
        level: undefined,
        groupType: undefined,
        groupTypeOptions: [],
      },
      ...(state.notifies || []),
    ]);
  };

  // 移除表格编辑中的规则
  const handleRemoveTriggerConditions = (id: string) => {
    updater.triggerCondition(filter(state.triggerCondition, (item) => item.id !== id));
  };

  // 移除策略
  const handleRemoveNotifyStrategy = (id: string) => {
    updater.notifies(filter(state.notifies, (item) => item.id !== id));
  };

  // 编辑单条触发条件
  const handleEditNotifyStrategy = (id: string, item: { key: string; value: string }) => {
    const rules = cloneDeep(state.notifies);
    const rule = find(rules, { id });
    const index = findIndex(rules, { id });

    fill(rules, { id, ...rule, [item.key]: item.value }, index, index + 1);
    updater.notifies(rules);
  };

  // 编辑单条触发条件
  const handleEditTriggerConditions = (id: string, item: { key: string; value: any }) => {
    const rules = cloneDeep(state.triggerCondition);
    const rule = find(rules, { id });
    const index = findIndex(rules, { id });
    if (item.key === 'operator' && item.value === 'all') {
      fill(
        rules,
        { id, ...rule, values: state.triggerCondition.valueOptions?.map((x) => x?.key)?.join(',') },
        index,
        index + 1,
      );
    }
    fill(rules, { id, ...rule, [item.key]: item.value }, index, index + 1);
    updater.triggerCondition(rules);
  };
  const beforeSubmit = (param: any) => {
    if (state.triggerCondition?.length > 0) {
      let isIncomplete = false;
      state.triggerCondition.forEach((item) => {
        for (const key in item) {
          // the third box is input when type is 'match' or 'notMatch', valueOptions is empty array
          if (
            (!item[key] && item.operator !== 'all') ||
            (!['match', 'notMatch'].includes(item.operator) && isArray(item[key]) && item[key].length === 0)
          ) {
            isIncomplete = true;
          }
        }
      });
      if (isIncomplete) {
        warning({
          title: i18n.t('cmp:content of filter rule is missing, please complete!'),
        });
        return null;
      }
    }

    if (isEmpty(state.editingRules)) {
      warning({
        title: i18n.t('cmp:create at least one rule'),
      });
      return null;
    } else {
      let isIncomplete = false;

      state.editingRules.forEach((item: { [x: string]: string | any[] }) => {
        for (const key in item) {
          if (['functions', 'level', 'name', 'window'].includes(key)) {
            if (!item[key] || (isArray(item[key]) && item[key].length === 0)) {
              isIncomplete = true;
            }
          }
        }
      });
      if (isIncomplete) {
        warning({
          title: i18n.t('cmp:content of alarm rule is missing, please complete!'),
        });
        return null;
      }
    }

    if (isEmpty(state.notifies)) {
      warning({
        title: i18n.t('cmp:create at least one notification object'),
      });
      return null;
    } else {
      let isIncomplete = false;
      state.notifies.forEach((item) => {
        for (const key in item) {
          if (!item[key] || (isArray(item[key]) && item[key].length === 0)) {
            isIncomplete = true;
          }
        }
      });
      if (isIncomplete) {
        warning({
          title: i18n.t('content of notification object is missing, please complete!'),
        });
        return null;
      }
    }

    const isLegalFunctions = every(state.editingRules, ({ functions }) => {
      return every(functions, ({ value }) => {
        return !(isNull(value) || value === '');
      });
    });

    if (!isLegalFunctions) {
      warning({
        title: i18n.t('cmp:rule value cannot be empty'),
      });
      return null;
    }
    return param;
  };

  return (
    <div>
      <RenderForm layout="vertical" form={form} list={fieldsList} className="w-full" />
    </div>
  );
};

export function AddStrategyPageName() {
  return (
    <div>
      <ErdaIcon
        type="arrow-left"
        size="18"
        className="cursor-pointer text-gray mr-3"
        onClick={() => window.history.back()}
      />
      <ErdaIcon type="remind" size="18" className="text-white bg-primary p-2 text-bold rounded-sm mr-2" />
      <span className="font-bold text-lg">{i18n.t('cmp:new alarm strategy')}</span>
    </div>
  );
}

export function EditStrategyPageName() {
  return (
    <div>
      <ErdaIcon
        type="arrow-left"
        size="18"
        className="cursor-pointer text-gray mr-3"
        onClick={() => window.history.back()}
      />
      <ErdaIcon type="remind" size="18" className="text-white bg-primary p-2 text-bold rounded-sm mr-2" />
      <span className="font-bold text-lg">{i18n.t('cmp:edit alarm strategy')}</span>
    </div>
  );
}

export default StrategyForm;
