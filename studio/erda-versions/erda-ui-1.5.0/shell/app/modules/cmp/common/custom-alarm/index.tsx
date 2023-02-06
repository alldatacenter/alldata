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
import i18n from 'i18n';
import {
  map,
  filter,
  uniqueId,
  reduce,
  cloneDeep,
  find,
  findIndex,
  fill,
  concat,
  isEmpty,
  omit,
  some,
  toString,
  uniqBy,
  debounce,
  keyBy,
  get,
  merge,
} from 'lodash';
import { Spin, Button, Switch, Select, Input, InputNumber, message, Modal, Tooltip } from 'antd';
import Table from 'common/components/table';
import { IActions } from 'common/components/table/interface';
import { Badge, FormModal, MarkdownEditor, RenderPureForm, IF, BoardGrid } from 'common';
import { useUpdate } from 'common/use-hooks';
import { useMount } from 'react-use';
import { FormInstance } from 'core/common/interface';
import { useLoading } from 'core/stores/loading';
import orgCustomAlarmStore from 'app/modules/cmp/stores/custom-alarm';
import mspCustomAlarmStore from 'msp/alarm-manage/alarm-strategy/stores/custom-alarm';
import orgMonitorMetaDataStore from 'app/modules/cmp/stores/analysis-monitor-metadata';
import mspMonitorMetaDataStore from 'app/modules/msp/alarm-manage/alarm-strategy/stores/analysis-monitor-metadata';
import { createLoadDataFn } from 'cmp/common/custom-dashboard/data-loader';

import './index.scss';

enum DataType {
  STRING = 'string',
  STRING_ARRAY = 'string_array',
  BOOL = 'bool',
  BOOL_ARRAY = 'bool_array',
  NUMBER = 'number',
  NUMBER_ARRAY = 'number_array',
}

const customAlarmStoreMap = {
  org: orgCustomAlarmStore,
  msp: mspCustomAlarmStore,
};

const monitorMetaDataStoreMap = {
  org: orgMonitorMetaDataStore,
  msp: mspMonitorMetaDataStore,
};

const formItemLayout = {
  labelCol: {
    sm: { span: 6 },
    md: { span: 6 },
    lg: { span: 6 },
  },
  wrapperCol: {
    sm: { span: 18 },
    md: { span: 18 },
    lg: { span: 18 },
  },
};

const { confirm } = Modal;

const CustomAlarm = ({ scopeType }: { scopeType: string }) => {
  const customAlarmStore = customAlarmStoreMap[scopeType];
  const monitorMetaDataStore = monitorMetaDataStoreMap[scopeType];
  const [switchCustomAlarmLoading, getPreviewMetaDataLoading, getCustomAlarmsLoading, getCustomAlarmDetailLoading] =
    useLoading(customAlarmStore, [
      'switchCustomAlarm',
      'getPreviewMetaData',
      'getCustomAlarms',
      'getCustomAlarmDetail',
    ]);
  const [extraLoading] = useLoading(monitorMetaDataStore, ['getMetaData']);
  const [metaGroups, metaConstantMap, metaMetrics] = monitorMetaDataStore.useStore((s: any) => [
    s.metaGroups,
    s.metaConstantMap,
    s.metaMetrics,
  ]);
  const { getMetaGroups, getMetaData } = monitorMetaDataStore.effects;
  const {
    fields,
    tags,
    metric,
    filters: defaultFilters,
  } = React.useMemo(() => (metaMetrics || [])[0] || {}, [metaMetrics]);
  const { types, filters } = React.useMemo(() => metaConstantMap, [metaConstantMap]);
  const fieldsMap = React.useMemo(() => keyBy(fields, 'key'), [fields]);

  const [customAlarms, customAlarmPaging, customMetricMap, customAlarmDetail, customAlarmTargets] =
    customAlarmStore.useStore((s: any) => [
      s.customAlarms,
      s.customAlarmPaging,
      s.customMetricMap,
      s.customAlarmDetail,
      s.customAlarmTargets,
    ]);
  const {
    getCustomAlarms,
    switchCustomAlarm,
    deleteCustomAlarm,
    getCustomMetrics,
    getCustomAlarmDetail,
    getCustomAlarmTargets,
    createCustomAlarm,
    getPreviewMetaData,
    editCustomAlarm,
  } = customAlarmStore.effects;
  const { clearCustomAlarmDetail } = customAlarmStore.reducers;
  const { total, pageSize, pageNo } = customAlarmPaging;

  useMount(() => {
    getMetaGroups();
    getCustomMetrics();
    getCustomAlarmTargets();
  });

  const [
    { modalVisible, editingFilters, editingFields, selectedMetric, activedFormData, previewerKey, layout },
    updater,
    update,
  ] = useUpdate({
    layout: [],
    modalVisible: false,
    editingFilters: [],
    editingFields: [],
    selectedMetric: undefined as any,
    activedFormData: {},
    previewerKey: undefined,
  });

  React.useEffect(() => {
    updater.selectedMetric(metric);
  }, [metric, updater]);

  React.useEffect(() => {
    if (isEmpty(customAlarmDetail)) return;
    const { rules } = customAlarmDetail;
    const { activedMetricGroups } = rules[0];
    getMetaData({ groupId: activedMetricGroups[activedMetricGroups.length - 1] });
  }, [customAlarmDetail, getMetaData]);

  React.useEffect(() => {
    const { rules, notifies } = customAlarmDetail;
    if (isEmpty(rules) || isEmpty(notifies)) return;

    const { functions } = rules[0];
    update({
      editingFields: map(functions, (item) => {
        const aggregations = get(types[get(fieldsMap[item.field], 'type')], 'aggregations');
        return {
          ...item,
          uniKey: uniqueId(),
          aggregations,
          aggregatorType: get(find(aggregations, { aggregation: item.aggregator }), 'result_type'),
        };
      }),
    });
  }, [customAlarmDetail, fieldsMap, types, update]);

  React.useEffect(() => {
    const { name, rules, notifies, id } = customAlarmDetail;
    if (isEmpty(rules) || isEmpty(notifies)) return;

    const { window, metric: _metric, filters: _filters, group, activedMetricGroups } = rules[0];
    const { title, content, targets } = notifies[0];
    update({
      editingFilters: map(_filters, (item) => ({ ...item, uniKey: uniqueId() })),
      activedFormData: {
        id,
        name,
        rule: {
          activedMetricGroups,
          window,
          metric: _metric,
          group,
        },
        notify: {
          title,
          content,
          targets: filter(targets, (target) => target !== 'ticket'),
        },
      },
      selectedMetric: _metric,
    });
  }, [customAlarmDetail, update]);

  React.useEffect(() => {
    getCustomAlarms();
  }, []);

  const handlePageChange = (paging: { current: number; pageSize?: number }) => {
    const { current, pageSize: size } = paging;
    getCustomAlarms({ pageNo: current, pageSize: size });
  };
  const handleDeleteAlarm = (id: number) => {
    confirm({
      title: i18n.t('are you sure you want to delete this item?'),
      content: i18n.t('the item will be permanently deleted!'),
      onOk() {
        deleteCustomAlarm(id);
      },
    });
  };

  const columns = [
    {
      title: i18n.t('name'),
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: i18n.t('status'),
      dataIndex: 'enable',
      render: (enable: boolean) => (
        <Badge text={enable ? i18n.t('enable') : i18n.t('unable')} status={enable ? 'success' : 'default'} />
      ),
    },
    {
      title: i18n.t('indicator'),
      dataIndex: 'metric',
      key: 'metric',
    },
    {
      title: i18n.t('period'),
      dataIndex: 'window',
      key: 'window',
      render: (value: number) => `${value} ${i18n.t('min')}`,
    },
    {
      title: i18n.t('notification method'),
      dataIndex: 'notifyTargets',
      key: 'notifyTargets',
      render: (value: string[]) => `${value.join('、')}`,
    },
  ];

  const filterColumns = [
    {
      title: i18n.t('tag'),
      dataIndex: 'tag',
      render: (value: string, { uniKey }: COMMON_CUSTOM_ALARM.Filter) => (
        <Select
          dropdownMatchSelectWidth={false}
          defaultValue={value}
          className="w-full"
          onSelect={(tag) => {
            handleEditEditingFilters(uniKey, [
              { key: 'tag', value: tag },
              { key: 'value', value: undefined },
            ]);
          }}
        >
          {map(tags, ({ key, name }) => (
            <Select.Option key={key} value={key}>
              {name}
            </Select.Option>
          ))}
        </Select>
      ),
    },
    {
      title: i18n.t('operate'),
      dataIndex: 'operator',
      render: (value: string, { uniKey }: COMMON_CUSTOM_ALARM.Filter) => (
        <Select
          dropdownMatchSelectWidth={false}
          defaultValue={value}
          className="w-full"
          onSelect={(operator) => {
            handleEditEditingFilters(uniKey, [{ key: 'operator', value: operator }]);
          }}
        >
          {map(filters, ({ operation, name }) => (
            <Select.Option key={operation}>{name}</Select.Option>
          ))}
        </Select>
      ),
    },
    {
      title: i18n.t('cmp:expected value'),
      dataIndex: 'value',
      render: (value: any, { uniKey }: COMMON_CUSTOM_ALARM.Filter) => {
        let expectedValEle = (
          <Input
            defaultValue={value}
            onBlur={(e: any) => {
              handleEditEditingFilters(uniKey, [{ key: 'value', value: e.target.value }]);
            }}
          />
        );
        const selectedFilter = find(editingFilters, { uniKey }) || ({} as any);
        const { values: _values } = find(tags, { key: selectedFilter.tag }) || ({} as any);
        if (!isEmpty(_values)) {
          expectedValEle = (
            <Select
              dropdownMatchSelectWidth={false}
              showSearch
              className="w-full"
              value={value}
              onSelect={(v: any) => {
                handleEditEditingFilters(uniKey, [{ key: 'value', value: v }]);
              }}
            >
              {map(_values, ({ value: v, name }) => (
                <Select.Option key={v} value={v}>
                  {name}
                </Select.Option>
              ))}
            </Select>
          );
        }
        return expectedValEle;
      },
    },
  ];

  const filteredTableActions: IActions<COMMON_CUSTOM_ALARM.Filter> = {
    render: (record) => [
      {
        title: i18n.t('delete'),
        onClick: () => {
          handleRemoveEditingFilter(record.uniKey);
        },
      },
    ],
  };

  const getFieldColumns = (form: FormInstance) => [
    {
      title: i18n.t('field'),
      dataIndex: 'field',
      render: (value: string, { uniKey }: COMMON_CUSTOM_ALARM.Field) => (
        <Select
          dropdownMatchSelectWidth={false}
          defaultValue={value}
          className="w-full"
          onSelect={(field: any) => {
            handleEditEditingFields(uniKey, [
              { key: 'field', value: field },
              { key: 'aggregations', value: get(types[get(fieldsMap[field], 'type')], 'aggregations') },
            ]);
          }}
        >
          {map(fields, ({ key, name }) => (
            <Select.Option key={key} value={key}>
              <Tooltip title={name}>{name}</Tooltip>
            </Select.Option>
          ))}
        </Select>
      ),
    },
    {
      title: i18n.t('cmp:alias'),
      dataIndex: 'alias',
      render: (value: string, { uniKey }: COMMON_CUSTOM_ALARM.Field) => (
        <Input
          defaultValue={value}
          onBlur={(e: any) => {
            handleEditEditingFields(uniKey, [{ key: 'alias', value: e.target.value }]);
          }}
        />
      ),
    },
    {
      title: i18n.t('cmp:aggregator'),
      dataIndex: 'aggregator',
      render: (value: string, { uniKey, aggregations }: COMMON_CUSTOM_ALARM.Field) => (
        <Select
          dropdownMatchSelectWidth={false}
          defaultValue={value}
          className="w-full"
          onSelect={(aggregator: any) => {
            handleEditEditingFields(uniKey, [
              { key: 'aggregator', value: aggregator },
              { key: 'aggregatorType', value: get(find(aggregations, { aggregation: aggregator }), 'result_type') },
            ]);
          }}
        >
          {map(aggregations, ({ aggregation, name }) => (
            <Select.Option key={aggregation}>{name}</Select.Option>
          ))}
        </Select>
      ),
    },
    {
      title: i18n.t('operate'),
      dataIndex: 'operator',
      render: (value: string, { uniKey, aggregatorType }: COMMON_CUSTOM_ALARM.Field) => (
        <Select
          dropdownMatchSelectWidth={false}
          defaultValue={value}
          className="w-full"
          onSelect={(operator) => {
            handleEditEditingFields(uniKey, [{ key: 'operator', value: operator }]);
          }}
        >
          {map(get(types[aggregatorType], 'operations'), ({ operation, name }) => (
            <Select.Option key={operation}>{name}</Select.Option>
          ))}
        </Select>
      ),
    },
    {
      title: i18n.t('cmp:default threshold'),
      dataIndex: 'value',
      fixed: 'right',
      render: (value: any, { uniKey, aggregatorType }: COMMON_CUSTOM_ALARM.Field) => {
        let valueEle = null;
        switch (aggregatorType) {
          case DataType.STRING:
          case DataType.STRING_ARRAY:
            valueEle = (
              <Input
                defaultValue={value}
                onBlur={(e: any) => {
                  handleEditEditingFields(uniKey, [{ key: 'value', value: e.target.value }]);
                }}
              />
            );
            break;
          case DataType.NUMBER:
          case DataType.NUMBER_ARRAY:
            valueEle = (
              <InputNumber
                min={0}
                defaultValue={value}
                onChange={(v: any) => {
                  debounceEditEditingFields(uniKey, [{ key: 'value', value: v }]);
                }}
              />
            );
            break;
          case DataType.BOOL:
          case DataType.BOOL_ARRAY:
            valueEle = (
              <Switch
                checkedChildren="true"
                unCheckedChildren="false"
                defaultChecked={value}
                onClick={(v: boolean) => {
                  handleEditEditingFields(uniKey, [{ key: 'value', value: v }]);
                }}
              />
            );
            break;
          default:
            break;
        }
        return valueEle;
      },
    },
    // {
    //   title: i18n.t('operate'),
    //   fixed: 'right',
    //   width: 150,
    //   render: ({ uniKey }: any) => {
    //     const isPreviewing = uniKey === previewerKey;
    //     return (
    //       <div className="table-operations">
    //         <span
    //           className="table-operations-btn"
    //           onClick={() => {
    //             handleRemoveEditingField(uniKey);
    //             isPreviewing && updater.previewerKey(undefined);
    //           }}
    //         >
    //           {i18n.t('delete')}
    //         </span>

    //         The interface data is returned incorrectly. The back-end suggests to hide the preview button temporarily
    //         <IF check={isPreviewing}>
    //           <span
    //             className="table-operations-btn"
    //             onClick={() => {
    //               handlePreview(form, uniKey);
    //             }}
    //           >
    //             {i18n.t('refresh')}
    //           </span>
    //         </IF>
    //         <span
    //           className="table-operations-btn"
    //           onClick={() => {
    //             if (isPreviewing) {
    //               updater.previewerKey(undefined);
    //             } else {
    //               handlePreview(form, uniKey);
    //             }
    //           }}
    //         >
    //           {isPreviewing ? i18n.t('cancel') : i18n.t('preview')}
    //         </span>
    //       </div>
    //     );
    //   },
    // },
  ];

  const fieldsTableActions: IActions<COMMON_CUSTOM_ALARM.Field> = {
    render: (record) => [
      {
        title: i18n.t('delete'),
        onClick: () => {
          handleRemoveEditingField(record.uniKey);
        },
      },
    ],
  };

  const handleAddEditingFilters = () => {
    updater.editingFilters([
      {
        uniKey: uniqueId(),
        // tag: customMetricMap.metricMap[selectedMetric].tags[0].tag.key,
        tag: undefined,
        // operator: keys(customMetricMap.filterOperatorMap)[0],
        operator: undefined,
      },
      ...editingFilters,
    ]);
  };

  const handleAddEditingFields = () => {
    updater.editingFields([
      {
        uniKey: uniqueId(),
        field: undefined,
        alias: undefined,
        aggregator: undefined,
        operator: undefined,
      },
      ...editingFields,
    ]);
  };

  const editRule = (rules: any, uniKey: any, items: Array<{ key: string; value: any }>) => {
    if (!uniKey) return;
    const _rules = cloneDeep(rules);
    const rule = find(_rules, { uniKey });
    const index = findIndex(_rules, { uniKey });
    const rest = reduce(items, (acc, { key, value }) => ({ ...acc, [key]: value }), {});
    const newRule = {
      uniKey,
      ...rule,
      ...rest,
    } as any;

    // // 标签、字段对应不同的 value 类型，改变标签或字段就重置 value
    // if (['tag', 'field'].includes(item.key)) {
    //   newRule = { ...newRule, value: undefined };
    // }

    fill(_rules, newRule, index, index + 1);

    return _rules;
  };

  const handleShowNotifySample = () => {
    Modal.info({
      title: i18n.t('cmp:template sample'),
      content: <span className="prewrap">{customMetricMap.notifySample}</span>,
    });
  };

  const handleEditEditingFilters = (uniKey: any, items: Array<{ key: string; value: any }>) => {
    updater.editingFilters(editRule(editingFilters, uniKey, items));
  };

  const handleEditEditingFields = (uniKey: any, items: Array<{ key: string; value: any }>) => {
    updater.editingFields(editRule(editingFields, uniKey, items));
  };

  const debounceEditEditingFields = debounce(handleEditEditingFields, 500);

  const handleRemoveEditingFilter = (uniKey: string | undefined) => {
    updater.editingFilters(filter(editingFilters, (item) => item.uniKey !== uniKey));
  };

  const handleRemoveEditingField = (uniKey: string | undefined) => {
    updater.editingFields(filter(editingFields, (item) => item.uniKey !== uniKey));
  };

  const extraKeys = ['uniKey', 'aggregations', 'aggregatorType'];
  const handlePreview = (form: FormInstance, uniKey: any) => {
    const { rule } = form.getFieldsValue();
    const payload = {
      rules: [
        {
          ...rule,
          metric: selectedMetric,
          functions: [omit(find(editingFields, { uniKey }), extraKeys)],
          filters: map(editingFilters, (item) => omit(item, extraKeys)),
        },
      ],
    };
    updater.previewerKey(uniKey);
    getPreviewMetaData(payload).then((metaData: any) => {
      const apiInfo = merge({}, metaData.api, {
        query: {
          ...reduce(defaultFilters, (acc, { tag, op, value }) => ({ ...acc, [`${op}_${tag}`]: value }), {}),
        },
      });
      const _layout = [
        {
          w: 24,
          h: 9,
          x: 0,
          y: 0,
          i: 'custom-rule-preview',
          moved: false,
          static: false,
          view: {
            ...metaData,
            hideReload: true,
            loadData: createLoadDataFn(apiInfo, metaData.chartType),
          },
        },
      ];
      updater.layout(_layout);
    });
  };

  const openModal = (id?: number) => {
    id && getCustomAlarmDetail(id);
    updater.modalVisible(true);
  };

  const closeModal = () => {
    updater.editingFields([]);
    updater.editingFilters([]);
    updater.activedFormData({});
    updater.modalVisible(false);
    updater.previewerKey(undefined);
    clearCustomAlarmDetail();
  };

  const someValueEmpty = (data: any[], key: string) => {
    return some(data, (item) => isEmpty(toString(item[key])));
  };

  const beforeSubmit = (data: any) => {
    return new Promise((resolve, reject) => {
      if (isEmpty(editingFields)) {
        message.warning(i18n.t('cmp:field rules are required'));
        return reject();
      }
      if (someValueEmpty(editingFilters, 'value')) {
        message.warning(i18n.t('cmp:The expected value of filter rule is required.'));
        return reject();
      }
      if (someValueEmpty(editingFields, 'alias')) {
        message.warning(i18n.t('cmp:field rule alias is required'));
        return reject();
      }
      if (uniqBy(editingFields, 'alias').length !== editingFields.length) {
        message.warning(i18n.t('cmp:field rule alias cannot be repeated'));
        return reject();
      }
      if (someValueEmpty(editingFields, 'value')) {
        message.warning(i18n.t('cmp:field rule threshold is required'));
        return reject();
      }
      resolve(data);
    });
  };

  const handleUpdateCustomAlarm = (value: { name: string; rule: any; notify: any }) => {
    const _notify = merge({}, value.notify, { targets: [...(value.notify.targets || []), 'ticket'] });
    const payload = {
      name: value.name,
      rules: [
        {
          ...value.rule,
          metric: selectedMetric,
          functions: map(editingFields, (item) => omit(item, extraKeys)),
          filters: map(editingFilters, (item) => omit(item, extraKeys)),
        },
      ],
      notifies: [_notify],
    };
    if (isEmpty(activedFormData)) {
      createCustomAlarm(payload);
    } else {
      editCustomAlarm({ id: activedFormData.id, ...payload });
    }
    closeModal();
  };

  const BasicForm = ({ form }: { form: FormInstance }) => {
    const fieldsList = [
      {
        label: i18n.t('name'),
        name: 'name',
        itemProps: {
          maxLength: 50,
        },
      },
    ];
    return <RenderPureForm list={fieldsList} form={form} formItemLayout={formItemLayout} />;
  };

  const RuleForm = ({ form }: { form: FormInstance }) => {
    let fieldsList = [
      {
        label: `${i18n.t('period')} (${i18n.t('min')})`,
        name: ['rule', 'window'],
        type: 'inputNumber',
        itemProps: {
          min: 0,
          precision: 0,
          className: 'w-full',
        },
      },
      {
        label: i18n.t('indicator'),
        name: ['rule', 'activedMetricGroups'],
        type: 'cascader',
        options: metaGroups,
        itemProps: {
          className: 'w-full',
          showSearch: true,
          placeholder: i18n.t('cmp:please select index group'),
          onChange: (v: any) => {
            getMetaData({ groupId: v[v.length - 1] }).then(() => {
              form.setFieldsValue({
                rule: {
                  group: undefined,
                },
              });
              update({
                editingFilters: [],
                editingFields: [],
                previewerKey: undefined,
              });
            });
          },
        },
      },
    ];
    if (selectedMetric) {
      fieldsList = concat(
        fieldsList,
        {
          label: i18n.t('cmp:filter rule'),
          name: ['rule', 'filters'],
          required: false,
          getComp: () => (
            <>
              <Button
                ghost
                className="mb-2"
                type="primary"
                disabled={someValueEmpty(editingFilters, 'value')}
                onClick={handleAddEditingFilters}
              >
                {i18n.t('cmp:add filter rules')}
              </Button>
              <Table
                hideHeader
                className="filter-rule-table"
                rowKey="uniKey"
                dataSource={editingFilters}
                columns={filterColumns}
                actions={filteredTableActions}
                scroll={undefined}
              />
            </>
          ),
        },
        {
          label: i18n.t('cmp:grouping rules'),
          name: ['rule', 'group'],
          required: true,
          type: 'select',
          options: map(tags, ({ key, name }) => ({ value: key, name })),
          itemProps: {
            mode: 'multiple',
            allowClear: true,
            className: 'w-full',
          },
        },
        {
          label: i18n.t('cmp:field rule'),
          name: ['rule', 'functions'],
          required: false,
          getComp: () => (
            <>
              <Button
                className="mb-2"
                type="primary"
                ghost
                disabled={someValueEmpty(editingFields, 'value')}
                onClick={handleAddEditingFields}
              >
                {i18n.t('cmp:add field rules')}
              </Button>
              <Table
                hideHeader
                className="field-rule-table"
                rowKey="uniKey"
                dataSource={editingFields}
                actions={fieldsTableActions}
                columns={getFieldColumns(form)}
                scroll={undefined}
              />
            </>
          ),
        },
      );
    }
    return <RenderPureForm list={fieldsList} form={form} formItemLayout={formItemLayout} />;
  };

  const NotifyForm = ({ form }: { form: FormInstance }) => {
    const Comp = () => (
      <>
        <Button
          className="mb-2"
          type="primary"
          ghost
          disabled={isEmpty(customMetricMap.notifySample)}
          onClick={handleShowNotifySample}
        >
          {i18n.t('cmp:template sample')}
        </Button>
        <MarkdownEditor
          value={form.getFieldValue(['notify', 'content'])}
          onBlur={(value) => {
            form.setFieldsValue({
              notify: {
                ...(form.getFieldValue('notify') || {}),
                content: value,
              },
            });
          }}
          placeholder={i18n.t('cmp:refer to template sample to input')}
          maxLength={512}
        />
      </>
    );

    const fieldsList = [
      {
        label: i18n.t('cmp:optional notification methods'),
        name: ['notify', 'targets'],
        type: 'select',
        required: false,
        options: map(
          filter(customAlarmTargets, ({ key }) => key !== 'ticket'),
          ({ key, display }) => ({ value: key, name: display }),
        ),
        itemProps: {
          mode: 'multiple',
          allowClear: true,
          className: 'w-full',
        },
      },
      {
        label: i18n.t('cmp:message title rules'),
        name: ['notify', 'title'],
        itemProps: {
          maxLength: 128,
          placeholder: '【{{application_name}}应用{{service_name}}服务异常告警】',
        },
      },
      {
        label: i18n.t('cmp:message content rules'),
        name: ['notify', 'content'],
        getComp: () => <Comp />,
      },
    ];
    return <RenderPureForm list={fieldsList} form={form} formItemLayout={formItemLayout} />;
  };

  const CustomAlarmForm = ({ form }: any) => {
    if (isEmpty(customMetricMap) || isEmpty(customAlarmTargets)) return null;
    return (
      <div className="custom-alarm-form">
        <BasicForm form={form} />
        <div className="title font-bold text-base">{i18n.t('cmp:trigger rules')}</div>
        <RuleForm form={form} />
        <div className="title font-bold text-base">{i18n.t('cmp:message template')}</div>
        <NotifyForm form={form} />
      </div>
    );
  };

  const customRender = (content: JSX.Element) => (
    <div className="flex justify-between items-center">
      <div className="flex-1">{content}</div>
      <IF check={!!previewerKey}>
        <div className="custom-alarm-previewer px-4">
          <Spin spinning={getPreviewMetaDataLoading}>
            <BoardGrid.Pure layout={layout} />
          </Spin>
        </div>
      </IF>
    </div>
  );

  const actions: IActions<COMMON_CUSTOM_ALARM.CustomAlarms> = {
    render: (record: COMMON_CUSTOM_ALARM.CustomAlarms) => renderMenu(record),
  };

  const renderMenu = (record: COMMON_CUSTOM_ALARM.CustomAlarms) => {
    const { editAlarmRule, deleteAlarmRule } = {
      editAlarmRule: {
        title: i18n.t('edit'),
        onClick: () => openModal(record.id),
      },
      deleteAlarmRule: {
        title: i18n.t('delete'),
        onClick: () => handleDeleteAlarm(record.id),
      },
    };

    return [editAlarmRule, deleteAlarmRule];
  };

  return (
    <div className="custom-alarm">
      <div className="top-button-group">
        <Button type="primary" onClick={() => openModal()}>
          {i18n.t('cmp:create custom rule')}
        </Button>
      </div>
      <Spin spinning={getCustomAlarmsLoading}>
        <Table
          dataSource={customAlarms}
          columns={columns}
          rowKey="id"
          onChange={handlePageChange}
          pagination={{ current: pageNo, pageSize, total }}
          actions={actions}
        />
      </Spin>
      <FormModal
        name={i18n.t('cmp:custom rule')}
        loading={getCustomAlarmDetailLoading || extraLoading}
        visible={modalVisible}
        width={1200}
        modalProps={{ bodyStyle: { height: '550px', overflow: 'auto' } }}
        PureForm={CustomAlarmForm}
        formData={activedFormData}
        customRender={customRender}
        onOk={handleUpdateCustomAlarm}
        beforeSubmit={beforeSubmit}
        onCancel={closeModal}
      />
    </div>
  );
};

export default CustomAlarm;
