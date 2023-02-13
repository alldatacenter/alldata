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
import { Alert, Tooltip, Button, Table } from 'antd';
import i18n from 'i18n';
import { FormModal, ErdaIcon } from 'common';
import { useUpdate } from 'common/use-hooks';
import { useEffectOnce } from 'react-use';
import { map } from 'lodash';
import { getCloudResourceTimeCol, getRemarkCol, getCloudResourceTagsCol } from 'cmp/common/components/table-col';
import { SetTagForm } from 'cmp/common/components/set-tag-form';
import routeInfoStore from 'core/stores/route';
import cloudServiceStore from 'dcos/stores/cloud-service';
import { useLoading } from 'core/stores/loading';

const Topic = () => {
  const MQTopicList = cloudServiceStore.useStore((s) => s.MQTopicList);
  const { getMQTopicList, addMQTopic } = cloudServiceStore.effects;
  const { clearMQTopicList } = cloudServiceStore.reducers;
  const [mqID, query] = routeInfoStore.useStore((s) => [s.params.mqID, s.query]);
  const [isFetching] = useLoading(cloudServiceStore, ['getMQTopicList']);

  useEffectOnce(() => {
    getList();
    return () => clearMQTopicList();
  });

  const getList = () => {
    getMQTopicList({
      ...query,
      instanceID: mqID,
    });
  };

  const [{ formVisible, setTagDisabled, tagFormVis, items, tagFormData, stateChangeKey }, updater, update] = useUpdate({
    formVisible: false,
    setTagDisabled: true,
    tagFormVis: false,
    tagFormData: null,
    items: [] as CLOUD.TagItem[],
    stateChangeKey: 0,
  });

  const columns = [
    {
      title: 'Topic',
      dataIndex: 'topicName',
      ellipsis: {
        showTitle: false,
      },
      render: (text: string) => <Tooltip title={text}>{text}</Tooltip>,
    },
    getCloudResourceTagsCol(),
    {
      title: i18n.t('type'),
      dataIndex: 'messageType',
      ellipsis: {
        showTitle: false,
      },
      render: (text: string) => <Tooltip title={text}>{text}</Tooltip>,
    },
    {
      title: i18n.t('authority'),
      dataIndex: 'relationName',
    },
    getCloudResourceTimeCol(),
    getRemarkCol(),
    {
      title: i18n.t('operation'),
      dataIndex: 'op',
      width: 100,
      render: (_v: any, record: CLOUD_SERVICE.IMQTopic) => {
        return (
          <div className="table-operations">
            <span
              className="table-operations-btn"
              onClick={() => {
                const { tags, topicName } = record;
                update({
                  tagFormVis: true,
                  tagFormData: {
                    projects: Object.keys(tags),
                  },
                  items: [
                    {
                      vendor: 'alicloud',
                      region: query.region,
                      resourceID: topicName,
                      oldTags: Object.keys(tags),
                    },
                  ],
                });
              }}
            >
              {i18n.t('set tags')}
            </span>
          </div>
        );
      },
    },
  ];

  const allName = map(MQTopicList, 'topicName');
  const fieldsList = [
    {
      getComp: () => <Alert message={i18n.t('cmp:regions-provide-intranet-default')} type="info" />,
    },
    {
      label: 'Topic',
      name: 'topicName',
      itemProps: {
        maxLength: 64,
      },
      rules: [
        { min: 3, max: 64, message: i18n.t('length is {min}~{max}', { min: 3, max: 64 }) },
        { pattern: /^[a-zA-z0-9_-]+$/, message: i18n.t('can only contain letters, numbers, underscores and hyphens') },
        {
          validator: (rule: any, value: string, callback: Function) => {
            if (allName.includes(value)) {
              callback(i18n.t('{name} already exists', { name: 'Topic' }));
            } else if (value && (value.startsWith('CID') || value.startsWith('GID'))) {
              callback(
                `${i18n.t('cmp:CID and GID are reserved fields of Group ID. Topic cannot start with CID and GID.')}`,
              );
            } else {
              callback();
            }
          },
        },
      ],
    },
    {
      label: (
        <span className="flex">
          {i18n.t('resource:message type')}&nbsp;
          <Tooltip title={i18n.t('cmp:common-messages-suitable-for')}>
            <ErdaIcon type="help" />
          </Tooltip>
        </span>
      ),
      name: 'messageType',
      type: 'select',
      initialValue: 0,
      options: [
        {
          name: i18n.t('cmp:general message'),
          value: 0,
        },
        {
          name: i18n.t('cmp:partition order message'),
          value: 1,
        },
        {
          name: i18n.t('cmp:global order message'),
          value: 2,
        },
      ],
    },
    {
      label: i18n.t('dop:remark'),
      name: 'remark',
      type: 'textArea',
      required: false,
      itemProps: {
        placeholder: i18n.t('please enter a description'),
        maxLength: 128,
        rows: 2,
      },
    },
  ];

  const checkSelect = (selectedRows: CLOUD_SERVICE.IMQTopic[]) => {
    selectedRows.length > 0 ? updater.setTagDisabled(false) : updater.setTagDisabled(true);

    const newItems = selectedRows.map(({ tags, topicName }): any => {
      return {
        vendor: 'alicloud',
        region: query.region,
        resourceID: topicName,
        oldTags: Object.keys(tags),
      };
    });
    updater.items(newItems);
  };

  const handleSelect = (_: any, selectedRows: CLOUD_SERVICE.IMQTopic[]) => {
    checkSelect(selectedRows);
  };

  const resetTable = () => {
    getList();
    updater.stateChangeKey(stateChangeKey + 1);
    checkSelect([]);
  };

  const afterTagFormSubmit = () => {
    resetTable();
  };

  const handleCreateTopic = (formRes: any) => {
    const reFormRes = {
      ...formRes,
      messageType: +formRes.messageType,
    };
    const form = {
      region: query.region,
      vendor: 'alicloud',
      instanceID: mqID,
      source: 'resource',
      topics: [reFormRes],
    };
    const queryBody = {
      ...query,
      instanceID: mqID,
    };
    return addMQTopic(form).then(() => {
      getMQTopicList(queryBody);
      updater.formVisible(false);
    });
  };

  return (
    <div>
      <div className="text-right mb-3">
        <Button type="primary" onClick={() => updater.formVisible(true)} className="mr-2">
          {i18n.t('cmp:create Topic')}
        </Button>
        <Button type="primary" disabled={setTagDisabled} onClick={() => updater.tagFormVis(true)}>
          {i18n.t('batch labeling')}
        </Button>
      </div>
      <Table
        key={stateChangeKey}
        loading={isFetching}
        columns={columns}
        dataSource={MQTopicList}
        rowKey="topicName"
        rowSelection={{
          onChange: handleSelect,
        }}
        scroll={{ x: '100%' }}
      />
      <SetTagForm
        items={items}
        visible={tagFormVis}
        formData={tagFormData as any}
        resourceType="ONS_TOPIC"
        instanceID={mqID}
        showClustertLabel={false}
        showProjectLabel
        onCancel={() => update({ tagFormVis: false, tagFormData: null })}
        afterSubmit={afterTagFormSubmit}
      />
      <FormModal
        title={i18n.t('cmp:create Topic')}
        visible={formVisible}
        fieldsList={fieldsList}
        onCancel={() => updater.formVisible(false)}
        onOk={handleCreateTopic}
      />
    </div>
  );
};

export default Topic;
