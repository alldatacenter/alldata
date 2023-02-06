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
import { Button, Select, Divider, Spin, Modal, Switch, Table, Tooltip } from 'antd';
import { isEmpty, map, find, get } from 'lodash';
import i18n from 'i18n';
import moment from 'moment';
import { useMount } from 'react-use';
import { FormModal } from 'common';
import { useSwitch, useUpdate } from 'common/use-hooks';
import { FormInstance, ColumnProps } from 'core/common/interface';
import { goTo } from 'common/utils';
import {
  notifyChannelOptionsMap,
  ListTargets,
  getFinalNotifyChannelOptions,
} from 'application/pages/settings/components/app-notify/common-notify-group';
import { useLoading } from 'core/stores/loading';
import memberStore from 'common/stores/org-member';
import notifyGroupStore from 'application/stores/notify-group';
import alarmReportStore from '../../stores/alarm-report';
import { usePerm, WithAuth } from 'user/common';
import orgStore from 'app/org-home/stores/org';
import { getNotifyChannelMethods } from 'org/services/notice-channel';

const { confirm } = Modal;

const ReportTypeMap = {
  daily: i18n.t('cmp:daily report'),
  weekly: i18n.t('cmp:weekly report'),
  monthly: i18n.t('cmp:monthly report'),
};

const AlarmReport = () => {
  const roleMap = memberStore.useStore((s) => s.roleMap);
  const [reportTasks, reportTaskPaging, systemDashboards, reportTypes] = alarmReportStore.useStore((s) => [
    s.reportTasks,
    s.reportTaskPaging,
    s.systemDashboards,
    s.reportTypes,
  ]);
  const {
    createReportTask,
    getReportTasks,
    updateReportTask,
    deleteReportTask,
    switchReportTask,
    getSystemDashboards,
    getReportTypes,
  } = alarmReportStore.effects;
  const { getNotifyGroups } = notifyGroupStore.effects;
  const notifyGroups = notifyGroupStore.useStore((s) => s.notifyGroups);
  const [loading] = useLoading(alarmReportStore, ['getReportTasks']);
  const orgId = orgStore.getState((s) => s.currentOrg.id);
  const [activeGroupId, setActiveGroupId] = React.useState(0);

  const [modalVisible, openModal, closeModal] = useSwitch(false);
  const [{ editingTask, allChannelMethods }, updater] = useUpdate({
    editingTask: {},
    allChannelMethods: notifyChannelOptionsMap,
  });
  const channelMethods = getNotifyChannelMethods.useData() as Obj<string>;
  const addNotificationGroupAuth = usePerm((s) => s.org.cmp.alarms.addNotificationGroup.pass); // 企业中心的添加通知组，需要验证权限，项目的暂无埋点

  const { pageNo, pageSize, total } = reportTaskPaging;

  useMount(() => {
    getReportTasks({ pageNo, pageSize });
    getReportTypes();
    getSystemDashboards();
    getNotifyGroups({ scopeType: 'org', scopeId: String(orgId), pageSize: 100 });
    getNotifyChannelMethods.fetch();
  });

  React.useEffect(() => {
    updater.allChannelMethods(getFinalNotifyChannelOptions(channelMethods, true));
  }, [channelMethods, updater]);

  const handleCloseModal = () => {
    closeModal();
    updater.editingTask({});
  };

  const getFieldsList = (form: FormInstance) => {
    const activeGroup = find(notifyGroups, { id: activeGroupId });
    const fieldsList = [
      {
        label: i18n.t('cmp:report name'),
        name: 'name',
        itemProps: {
          maxLength: 50,
        },
      },
      {
        label: i18n.t('cmp:report type'),
        name: 'type',
        type: 'radioGroup',
        itemProps: { disabled: !isEmpty(editingTask) },
        options: reportTypes,
        initialValue: 'daily',
      },
      {
        label: i18n.t('cmp:report template'),
        name: 'dashboardId',
        type: 'select',
        options: map(systemDashboards, ({ name, id }) => ({ name, value: id })),
        initialValue: 'daily',
      },
      {
        label: i18n.t('cmp:select group'),
        name: ['notifyTarget', 'groupId'],
        getComp: () => (
          <Select
            onSelect={(id: number) => {
              form.setFieldsValue({
                notifyTarget: {
                  groupId: id,
                  groupType: [],
                },
              });
              setActiveGroupId(id);
            }}
            dropdownRender={(menu) => (
              <div>
                {menu}
                <Divider className="my-1" />
                <div className="text-xs px-2 py-1 text-desc" onMouseDown={(e) => e.preventDefault()}>
                  <WithAuth pass={addNotificationGroupAuth}>
                    <span
                      className="hover-active"
                      onClick={() => {
                        goTo(goTo.pages.cmpNotifyGroup);
                      }}
                    >
                      {i18n.t('cmp:add more notification groups')}
                    </span>
                  </WithAuth>
                </div>
              </div>
            )}
          >
            {map(notifyGroups, ({ id, name }) => (
              <Select.Option key={id} value={id}>
                {name}
              </Select.Option>
            ))}
          </Select>
        ),
      },
      {
        label: i18n.t('notification method'),
        name: ['notifyTarget', 'groupType'],
        type: 'select',
        initialValue: get(editingTask, 'notifyTarget.groupType'),
        options: (activeGroup && allChannelMethods[activeGroup.targets[0].type]) || [],
        itemProps: { mode: 'multiple' },
      },
    ];
    return fieldsList;
  };

  const handleDelete = (id: number) => {
    confirm({
      title: i18n.t('cmp:are you sure you want to delete this task?'),
      content: i18n.t('cmp:the task will be permanently deleted'),
      onOk() {
        deleteReportTask(id);
      },
    });
  };

  const handleEdit = (item: COMMON_ALARM_REPORT.ReportTaskQuery) => {
    const {
      notifyTarget: { groupType, ...subRest },
      ...rest
    } = item;
    updater.editingTask({
      notifyTarget: {
        groupType: groupType.split(','),
        ...subRest,
      },
      ...rest,
    });
    openModal();
  };

  const handleSubmit = ({ notifyTarget: { groupType, ...subRest }, ...rest }: any) => {
    const payload = {
      notifyTarget: {
        type: 'notify_group',
        groupType: groupType.join(','),
        ...subRest,
      },
      ...rest,
    };
    if (!isEmpty(editingTask)) {
      updateReportTask({ ...payload, id: editingTask.id });
    } else {
      createReportTask(payload);
    }
    closeModal();
  };

  const handlePageChange = (no: number) => {
    getReportTasks({ pageNo: no, pageSize });
  };

  const columns: Array<ColumnProps<COMMON_ALARM_REPORT.ReportTask>> = [
    {
      title: i18n.t('cmp:report name'),
      dataIndex: 'name',
    },
    {
      title: i18n.t('cmp:report type'),
      dataIndex: 'type',
      width: 100,
      render: (text) => ReportTypeMap[text],
    },
    {
      title: i18n.t('default:notification target'),
      dataIndex: 'notifyTarget',
      className: 'notify-info',
      render: (notifyTarget) => {
        const targets = get(notifyTarget, 'notifyGroup.targets', []);
        const tip = i18n.t('cmp:Notification group does not exist or has been remove. Please change one.');
        return (
          <div className="flex">
            {isEmpty(targets) ? (
              <Tooltip title={tip}>
                <span className="text-sub">{tip}</span>
              </Tooltip>
            ) : (
              <ListTargets roleMap={roleMap} targets={targets} />
            )}
          </div>
        );
      },
    },
    {
      title: i18n.t('default:create time'),
      dataIndex: 'createdAt',
      width: 180,
      render: (text) => moment(text).format('YYYY-MM-DD HH:mm:ss'),
    },
    {
      title: i18n.t('default:operation'),
      dataIndex: 'id',
      width: 150,
      render: (id, record) => {
        return (
          <div className="table-operations">
            <span
              className="table-operations-btn"
              onClick={(e) => {
                e.stopPropagation();
                handleEdit(record);
              }}
            >
              {i18n.t('edit')}
            </span>
            <span
              className="table-operations-btn"
              onClick={(e) => {
                e.stopPropagation();
                handleDelete(id);
              }}
            >
              {i18n.t('delete')}
            </span>
            <span
              onClick={(e) => {
                e.stopPropagation();
              }}
            >
              <Switch
                size="small"
                defaultChecked={record.enable}
                onChange={() => {
                  switchReportTask({
                    id,
                    enable: !record.enable,
                  });
                }}
              />
            </span>
          </div>
        );
      },
    },
  ];

  return (
    <>
      <div className="top-button-group">
        <Button
          type="primary"
          onClick={() => {
            openModal();
          }}
        >
          {i18n.t('cmp:new report task')}
        </Button>
        <FormModal
          visible={modalVisible}
          onCancel={handleCloseModal}
          name={i18n.t('report task')}
          fieldsList={getFieldsList}
          formData={editingTask}
          modalProps={{ destroyOnClose: true }}
          onOk={handleSubmit}
        />
      </div>
      <Spin spinning={loading}>
        <Table
          rowKey="id"
          className="common-notify-list"
          dataSource={reportTasks}
          columns={columns}
          pagination={{
            current: pageNo,
            pageSize,
            total,
            onChange: handlePageChange,
          }}
          onRow={({ id }: COMMON_ALARM_REPORT.ReportTask) => {
            return {
              onClick: () => {
                goTo(`./${id}`);
              },
            };
          }}
          scroll={{ x: '100%' }}
        />
      </Spin>
    </>
  );
};

export default AlarmReport;
