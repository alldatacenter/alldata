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
import { map, isInteger, get } from 'lodash';
import { Table, Button, Popconfirm, Input, Modal, message, Select, Tooltip } from 'antd';
import i18n from 'i18n';
import scanRuleStore from 'project/stores/scan-rule';
import { useEffectOnce } from 'react-use';
import { WithAuth } from 'user/common';
import { useUpdate } from 'common/use-hooks';
import { ColumnProps } from 'core/common/interface';

interface IProps {
  operationAuth: boolean;
  scopeId: string;
  scopeType: string;
}

const valueTypeMap = {
  WORK_DUR: 'min',
  RATING: '',
  PERCENT: '%',
  MILLISEC: 's',
  INT: '',
  FLOAT: '',
};

const valueRateMap = {
  1: { label: 'A', value: '1' },
  2: { label: 'B', value: '2' },
  3: { label: 'C', value: '3' },
  4: { label: 'D', value: '4' },
};

const { Option } = Select;

export default function ScanRule(props: IProps) {
  const { operationAuth, scopeId, scopeType } = props;
  const [
    {
      isEdit,
      visible,
      currentId,
      appendedCurrent,
      metricValue,
      appendedRowKeys,
      optionalRowKeys,
      loading,
      operationOptionalRules,
    },
    updater,
    update,
  ] = useUpdate({
    isEdit: false,
    visible: false,
    currentId: '',
    appendedCurrent: 1,
    metricValue: '',
    appendedRowKeys: [],
    optionalRowKeys: [],
    loading: false,
    operationOptionalRules: [],
  });

  const [appendedScanRules, optionalScanRules] = scanRuleStore.useStore((s) => [
    s.appendedScanRules,
    s.optionalScanRules,
  ]);

  React.useEffect(() => {
    updater.operationOptionalRules(optionalScanRules);
  }, [optionalScanRules, updater]);

  const {
    getAppendedScanRules,
    deleteScanRule,
    getOptionalScanRules,
    updateScanRule,
    batchDeleteScanRule,
    batchInsertScanRule,
  } = scanRuleStore;

  const updateOptionalScanRules = (record: SCAN_RULE.AppendedItem) => {
    updater.operationOptionalRules((prev: any) => prev.map((p: any) => (p.id === record.id ? { ...record } : p)));
  };

  const appendedRowSelection = {
    selectedRowKeys: appendedRowKeys,
    onChange: (selectedRowKeys: string[] | number[]) => onAppendedSelectChange(selectedRowKeys),
    getCheckboxProps: (record: any) => ({
      disabled: record.scopeId === '-1',
    }),
  };

  const optionalRowSelection = {
    selectedRowKeys: optionalRowKeys,
    onChange: (rowKeys: string[] | number[]) => onOptionalSelectChange(rowKeys),
  };
  const optionalColumns: Array<ColumnProps<object>> = [
    {
      title: i18n.t('dop:rule name'),
      dataIndex: 'metricKey',
      width: 200,
    },
    {
      title: i18n.t('description'),
      dataIndex: 'metricKeyDesc',
    },
    {
      title: i18n.t('comparison operator'),
      dataIndex: 'operational',
      align: 'center',
      width: 176,
    },
    {
      title: i18n.t('dop:access control value'),
      dataIndex: 'metricValue',
      align: 'center',
      width: 160,
      render: (val: string, record: any) => {
        if (record.valueType === 'RATING') {
          return (
            <Select
              style={{ width: 120 }}
              value={val}
              onChange={(value) => handleChange(value, record)}
              getPopupContainer={() => document.body}
            >
              {map(valueRateMap, (rate) => (
                <Option value={rate.value} key={rate.value}>
                  {rate.label}
                </Option>
              ))}
            </Select>
          );
        }
        return (
          <Input
            style={{ width: 120 }}
            value={val}
            suffix={valueTypeMap[record.valueType]}
            onChange={(e) => handleChange(e.target.value, record)}
          />
        );
      },
    },
  ];

  const appendedColumns: Array<ColumnProps<SCAN_RULE.AppendedItem>> = [
    {
      title: i18n.t('dop:rule name'),
      dataIndex: 'metricKey',
      width: 200,
    },
    {
      title: i18n.t('description'),
      dataIndex: 'metricKeyDesc',
    },
    {
      title: i18n.t('comparison operator'),
      dataIndex: 'operational',
      align: 'center',
      width: 176,
    },
    {
      title: i18n.t('dop:access control value'),
      dataIndex: 'metricValue',
      width: 160,
      render: (item: string, record: SCAN_RULE.AppendedItem) => {
        const ruleType = valueTypeMap[record.valueType];
        if (isEdit && record.id === currentId) {
          if (record.valueType === 'RATING') {
            return (
              <Select
                style={{ width: 120 }}
                onChange={(value) => updater.metricValue(`${value}`)}
                defaultValue={isEdit && get(valueRateMap[item], 'label', '')}
              >
                {map(valueRateMap, (rate) => (
                  <Option value={rate.value} key={rate.value}>
                    {rate.label}
                  </Option>
                ))}
              </Select>
            );
          }
          return <Input defaultValue={item} suffix={ruleType} onChange={(e) => updater.metricValue(e.target.value)} />;
        }
        return record.valueType === 'RATING' ? get(valueRateMap[item], 'label', '') : `${item}${ruleType}`;
      },
    },
    {
      title: i18n.t('operation'),
      dataIndex: 'operation',
      align: 'center',
      width: 160,
      fixed: 'right',
      render: (_: any, record: any) => {
        if (isEdit && record.id === currentId) {
          return (
            <div className="table-operations">
              <span className="table-operations-btn" onClick={() => updater.isEdit(!isEdit)}>
                {i18n.t('cancel')}
              </span>
              <span
                className="table-operations-btn"
                onClick={async () => {
                  const { id, description, valueType, decimalScale } = record;
                  const isIllegal = checkRule([{ valueType, metricValue, decimalScale }]);
                  if (isIllegal) {
                    message.error(i18n.t('dop:please enter the correct data'));
                    return;
                  }
                  await updateScanRule({
                    id,
                    description,
                    metricValue,
                    scopeType: record.scopeType,
                    scopeId: record.scopeId,
                  });
                  reloadAppendedList();
                  updater.isEdit(!isEdit);
                }}
              >
                {i18n.t('save')}
              </span>
            </div>
          );
        }

        const isHandle = record.scopeId !== '-1';
        return (
          <div className="table-operations">
            <Tooltip
              title={isHandle ? '' : i18n.t('dop:Platform default rules. Please add custom rules if necessary.')}
            >
              <WithAuth pass={operationAuth}>
                <span
                  className={`table-operations-btn ${isHandle ? '' : 'disabled'}`}
                  onClick={() => {
                    update({
                      isEdit: isHandle,
                      currentId: record.id,
                    });
                  }}
                >
                  {i18n.t('edit')}
                </span>
              </WithAuth>
            </Tooltip>
            <Popconfirm
              disabled={!isHandle}
              title={`${i18n.t('common:confirm to delete')}?`}
              onConfirm={async () => {
                await deleteScanRule({ id: record.id, scopeId, scopeType });
                updater.appendedCurrent(1);
                reloadAppendedList();
              }}
            >
              <WithAuth pass={operationAuth}>
                <Tooltip
                  title={isHandle ? '' : i18n.t('dop:Platform default rules. Please add custom rules if necessary.')}
                >
                  <span className={`table-operations-btn ${isHandle ? '' : 'disabled'}`}>{i18n.t('delete')}</span>
                </Tooltip>
              </WithAuth>
            </Popconfirm>
          </div>
        );
      },
    },
  ];

  useEffectOnce(() => {
    getAppendedScanRules({
      scopeId,
      scopeType,
    });
  });

  function onAppendedSelectChange(selectedRowKeys: number[] | string[]) {
    updater.appendedRowKeys(selectedRowKeys);
  }

  function onOptionalSelectChange(rowKeys: number[] | string[]) {
    updater.optionalRowKeys(rowKeys);
  }

  function handleChange(value: any, record: any) {
    updateOptionalScanRules({ ...record, metricValue: value });
    if (value === '') {
      // 当输入框的值清空后自动去除CheckBox的勾选
      updater.optionalRowKeys(optionalRowKeys.filter((x: number) => x !== record.id));
    } else if (!optionalRowKeys.includes(record.id)) {
      // 当输入框有值且CheckBox未选中时自动勾选
      updater.optionalRowKeys([...optionalRowKeys, record.id]);
    }
  }

  function reloadAppendedList() {
    getAppendedScanRules({
      scopeId,
      scopeType,
    });
  }

  function handleBatchDelete() {
    Modal.confirm({
      title: i18n.t('dop:Are you sure to delete in batches?'),
      content: i18n.t('dop:batch-delete-tip-content'),
      async onOk() {
        await batchDeleteScanRule({ scopeId, scopeType, ids: appendedRowKeys });
        updater.appendedCurrent(1);
        reloadAppendedList();
        updater.appendedRowKeys([]);
      },
    });
  }

  function checkRule(metrics: any[]) {
    let isIllegal = false;

    for (const { valueType, metricValue: value, decimalScale } of metrics) {
      const rule = (!value && value !== '0') || +value < 0;

      if (valueType === 'WORK_DUR' || valueType === 'MILLISEC' || valueType === 'INT') {
        if (rule || !isInteger(+value)) {
          isIllegal = true;
          break;
        }
      }
      if (valueType === 'RATING') {
        if (!value) {
          isIllegal = true;
          break;
        }
      }
      if (valueType === 'PERCENT') {
        const isLegalLength = get(`${value}`.split('.'), '1.length', 0) <= decimalScale;
        if (rule || value[value.length - 1] === '.' || !isFinite(+value) || !isLegalLength) {
          isIllegal = true;
          break;
        }
      }
      if (valueType === 'FLOAT') {
        if (rule || !isFinite(+value)) {
          isIllegal = true;
          break;
        }
      }
    }
    return isIllegal;
  }

  const handleOk = async () => {
    const metrics = operationOptionalRules.filter((x) => {
      return optionalRowKeys.includes(x.id);
    });
    const isIllegal = checkRule(metrics);
    if (isIllegal) {
      message.error(i18n.t('dop:please enter the correct data'));
      return;
    }

    const formatMetrics = metrics.map((y) => ({
      description: y.description,
      metricKeyId: y.id,
      metricValue: y.metricValue,
    }));
    await batchInsertScanRule({ scopeType, scopeId, metrics: formatMetrics });
    update({
      visible: false,
      optionalRowKeys: [],
    });
    updater.appendedCurrent(1);
    reloadAppendedList();
  };

  return (
    <>
      <div className="mb-3">
        <WithAuth pass={operationAuth}>
          <Button
            type="primary"
            onClick={async () => {
              update({
                visible: !visible,
                loading: true,
              });
              await getOptionalScanRules({
                scopeId,
                scopeType,
              });
              updater.loading(false);
            }}
          >
            {i18n.t('dop:add access control rules')}
          </Button>
        </WithAuth>
        {appendedRowKeys.length > 0 && (
          <WithAuth pass={operationAuth}>
            <Button ghost type="primary" className="ml-2" onClick={handleBatchDelete}>
              {i18n.t('batch deletion')}
            </Button>
          </WithAuth>
        )}
      </div>
      <Table
        columns={appendedColumns}
        dataSource={appendedScanRules}
        rowKey="id"
        rowSelection={appendedRowSelection}
        pagination={{
          pageSize: 20,
          current: appendedCurrent,
          onChange: (pageNo) => updater.appendedCurrent(pageNo),
        }}
        scroll={{ y: 300, x: 900 }}
      />
      <Modal
        width={800}
        visible={visible}
        destroyOnClose
        title={i18n.t('dop:add access control rules')}
        closable={false}
        afterClose={() => updater.visible(false)}
        okButtonProps={{ disabled: optionalRowKeys.length === 0 }}
        onCancel={() => {
          update({
            visible: false,
            optionalRowKeys: [],
          });
        }}
        onOk={handleOk}
      >
        <Table
          pagination={{
            pageSize: 10,
          }}
          loading={loading}
          columns={optionalColumns}
          dataSource={operationOptionalRules}
          rowKey="id"
          rowSelection={optionalRowSelection}
          scroll={{ x: 800 }}
        />
      </Modal>
    </>
  );
}
