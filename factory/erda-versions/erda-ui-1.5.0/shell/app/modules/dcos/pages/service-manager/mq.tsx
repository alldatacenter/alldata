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
import { CRUDTable, Copy, ErdaIcon } from 'common';
import { useUpdate } from 'common/use-hooks';
import { useLoading } from 'core/stores/loading';
import { regRules } from 'common/utils/index';
import cloudCommonStore from 'app/modules/cmp/stores/cloud-common';
import i18n from 'i18n';
import { map, keys, get } from 'lodash';
import { Menu, Dropdown, Button, notification } from 'antd';
import cloudServiceStore from '../../stores/cloud-service';
import { useEffectOnce } from 'react-use';
import {
  getCloudResourceIDNameCol,
  getCloudResourceStatusCol,
  getCloudResourceTagsCol,
  getCloudResourceRegionCol,
} from 'cmp/common/components/table-col';
import { goTo, isPromise } from 'common/utils';
import { ClusterLog } from 'cmp/pages/cluster-manage/cluster-log';
import { SetTagForm } from 'cmp/common/components/set-tag-form';
import { skipInfoStatusMap } from 'cmp/pages/cloud-source/config';

// mq = ons
const MQ = () => {
  const MQList = cloudServiceStore.useStore((s) => s.MQList);
  const { addMQ, getMQList } = cloudServiceStore.effects;
  const [loading] = useLoading(cloudServiceStore, ['getMQList']);
  const { getCloudRegion } = cloudCommonStore;
  const regions = cloudCommonStore.useStore((s) => s.regions);

  const [{ tagFormVis, items, ifSelected, stateChangeKey, recordID }, updater, update] = useUpdate({
    subnetCount: 0,
    tagFormVis: false,
    items: [] as CLOUD.TagItem[],
    ifSelected: false,
    stateChangeKey: 1,
    recordID: '',
  });

  useEffectOnce(() => {
    getCloudRegion();
  });

  const getColumns = () => {
    const columns = [
      getCloudResourceIDNameCol('id', 'name', (id: string, record: CLOUD_SERVICE.IMQ) => {
        if (skipInfoStatusMap.mq.includes(record.status)) {
          goTo(`./${id}/info?region=${record.region || ''}`);
        } else {
          notification.warning({
            message: i18n.t('warning'),
            description: i18n.t('cmp:Instance is not ready'),
          });
        }
      }),
      {
        title: i18n.t('type'),
        dataIndex: 'instanceType',
        width: 100,
      },
      getCloudResourceRegionCol('region'),
      getCloudResourceTagsCol(),
      getCloudResourceStatusCol('mq'),
    ];
    return columns;
  };

  const getFieldsList = () => {
    const fieldsList = [
      {
        label: i18n.t('region'),
        name: 'region',
        type: 'select',
        options: map(regions, ({ regionID, localName }) => ({ value: regionID, name: `${localName} (${regionID})` })),
      },
      {
        label: i18n.t('resource:instance name'),
        name: 'name',
        itemProps: {
          min: 3,
          max: 64,
        },
        rules: [regRules.commonStr],
      },
      {
        label: i18n.t('description'),
        name: 'remark',
        type: 'textArea',
      },
    ];
    return fieldsList as any[];
  };

  const checkSelect = (selectedRows: CLOUD_SERVICE.IMQ[]) => {
    const newIfSelected = !!selectedRows.length;
    const newItems = selectedRows.map(({ id, tags, region }): CLOUD.TagItem => {
      const vendor = 'alicloud';
      return {
        vendor,
        region,
        resourceID: id,
        oldTags: keys(tags),
      };
    });

    update({
      items: newItems,
      ifSelected: newIfSelected,
    });
    return newIfSelected;
  };
  const handleSelect = (selectedRowKeys: string, selectedRows: CLOUD_SERVICE.IMQ[]) => {
    checkSelect(selectedRows);
  };

  const resetTable = () => {
    updater.stateChangeKey(stateChangeKey + 1);
    checkSelect([]);
  };
  const afterTagFormSubmit = () => {
    resetTable();
  };

  const handleFormSubmit = (data: CLOUD_SERVICE.IMQCreateBody) => {
    const reData = { ...data, source: 'resource' };
    const res = addMQ(reData);
    if (isPromise(res)) {
      res.then((addRes: any) => {
        updater.recordID(get(addRes, 'data.recordID'));
      });
    }
  };

  const operationButtons = [
    {
      name: `${i18n.t('set tags')}`,
      cb: () => updater.tagFormVis(true),
      ifDisabled: false,
    },
  ];

  const menu = (
    <Menu>
      {operationButtons.map((button) => (
        <Menu.Item disabled={button.ifDisabled} key={button.name} onClick={button.cb}>
          {button.name}
        </Menu.Item>
      ))}
    </Menu>
  );

  const extraOperation = () => (
    <Dropdown disabled={!ifSelected} overlay={menu}>
      <Button type="primary">
        <div className="flex">
          {i18n.t('batch setting')}
          <ErdaIcon type="caret-down" className="ml-1" size="20" />
        </div>
      </Button>
    </Dropdown>
  );

  return (
    <>
      <CRUDTable<CLOUD_SERVICE.IMQ>
        key={stateChangeKey}
        isFetching={loading}
        getList={getMQList}
        list={MQList}
        rowKey="id"
        showTopAdd
        extraOperation={extraOperation}
        getColumns={getColumns}
        handleFormSubmit={handleFormSubmit}
        getFieldsList={getFieldsList}
        tableProps={{
          rowSelection: {
            onChange: handleSelect,
          },
        }}
      />
      <SetTagForm
        items={items}
        visible={tagFormVis}
        resourceType="ONS"
        showProjectLabel
        showClustertLabel={false}
        onCancel={() => updater.tagFormVis(false)}
        afterSubmit={afterTagFormSubmit}
      />
      <Copy selector=".cursor-copy" />
      <ClusterLog recordID={recordID} onClose={() => updater.recordID('')} />
    </>
  );
};

export default MQ;
