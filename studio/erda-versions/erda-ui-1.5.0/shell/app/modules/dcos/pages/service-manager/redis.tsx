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
import networksStore from 'cmp/stores/networks';
import cloudCommonStore from 'app/modules/cmp/stores/cloud-common';
import i18n from 'i18n';
import { map, get, find } from 'lodash';
import { Tooltip, Dropdown, Button, Menu, notification } from 'antd';
import { RedisFieldConfig } from 'project/pages/third-service/components/config';
import cloudServiceStore from '../../stores/cloud-service';
import { FormInstance } from 'core/common/interface';
import { useEffectOnce } from 'react-use';
import { addAuthTooltipTitle } from 'app/modules/cmp/common/cloud-common';
import { goTo, isPromise } from 'common/utils';
import {
  getCloudResourceIDNameCol,
  getCloudResourceChargeTypeCol,
  getCloudResourceStatusCol,
  getCloudResourceTagsCol,
  getCloudResourceRegionCol,
} from 'cmp/common/components/table-col';
import { ClusterLog } from 'cmp/pages/cluster-manage/cluster-log';
import { SetTagForm } from 'cmp/common/components/set-tag-form';
import { skipInfoStatusMap } from 'cmp/pages/cloud-source/config';

const specList = [...RedisFieldConfig.spec.standard, ...RedisFieldConfig.spec.cluster];
const Redis = () => {
  const redisList = cloudServiceStore.useStore((s) => s.redisList);
  const { addRedis, getRedisList } = cloudServiceStore.effects;
  const [loading] = useLoading(cloudServiceStore, ['getRedisList']);
  const { getVpcList } = networksStore.effects;
  const { getCloudRegion } = cloudCommonStore;
  const { clearVpcList } = networksStore.reducers;
  const vpcList = networksStore.useStore((s) => s.vpcList);
  const [regions, cloudAccountExist] = cloudCommonStore.useStore((s) => [s.regions, s.cloudAccountExist]);

  useEffectOnce(() => {
    getCloudRegion();
    return () => {
      clearVpcList();
    };
  });

  const [{ chargeType, chosenRegion, tagFormVis, items, ifSelected, stateChangeKey, recordID }, updater, update] =
    useUpdate({
      chargeType: 'PostPaid',
      chosenRegion: undefined as string | undefined,
      tagFormVis: false,
      items: [] as CLOUD.TagItem[],
      ifSelected: false,
      stateChangeKey: 1,
      recordID: '',
    });

  React.useEffect(() => {
    chosenRegion && getVpcList({ region: chosenRegion });
  }, [chosenRegion, getVpcList]);

  const getColumns = () => {
    const columns = [
      getCloudResourceIDNameCol('id', 'name', (id: string, record: CLOUD_SERVICE.IRedis) => {
        if (skipInfoStatusMap.redis.includes(record.status)) {
          goTo(`./${id}/info?region=${record.region}`);
        } else {
          notification.warning({
            message: i18n.t('warning'),
            description: i18n.t('cmp:Instance is not ready'),
          });
        }
      }),
      getCloudResourceTagsCol(),
      getCloudResourceRegionCol('region'),
      getCloudResourceStatusCol('redis'),
      {
        title: i18n.t('specification'),
        dataIndex: 'spec',
        render: (val: string) => (
          <Tooltip title={val}>
            {get(
              find(specList, (item) => item.value === val),
              'specName',
            ) || val}
          </Tooltip>
        ),
      },
      {
        title: i18n.t('version'),
        dataIndex: 'version',
        width: 80,
      },
      getCloudResourceChargeTypeCol('chargeType', 'createTime'),
    ];
    return columns;
  };

  const getFieldsList = (form: FormInstance) => {
    const fieldsList = [
      {
        label: i18n.t('region'),
        name: 'region',
        type: 'select',
        options: map(regions, ({ regionID, localName }) => ({ value: regionID, name: `${localName} (${regionID})` })),
        itemProps: {
          onChange: (val: string) => {
            form.setFieldsValue({ vpc: undefined });
            updater.chosenRegion(val);
          },
        },
      },
      {
        label: 'VPC',
        name: 'vpcID',
        type: 'select',
        options: map(vpcList, (item) => ({ value: item.vpcID, name: item.vpcName })),
      },
      ...RedisFieldConfig.getFields({
        chargeType,
        onChangeChargeType: (val) => updater.chargeType(val),
      }),
    ];
    return fieldsList as any[];
  };

  const checkSelect = (selectedRows: CLOUD_SERVICE.IRedis[]) => {
    const newIfSelected = !!selectedRows.length;
    const newItems = selectedRows.map(({ region, id, tags }): CLOUD.TagItem => {
      const vendor = 'alicloud';
      return {
        vendor,
        region,
        resourceID: id,
        oldTags: Object.keys(tags),
      };
    });

    update({
      items: newItems,
      ifSelected: newIfSelected,
    });
    return newIfSelected;
  };
  const handleSelect = (selectedRowKeys: string, selectedRows: CLOUD_SERVICE.IRedis[]) => {
    checkSelect(selectedRows);
  };

  const resetTable = () => {
    updater.stateChangeKey(stateChangeKey + 1);
    checkSelect([]);
  };
  const afterTagFormSubmit = () => {
    resetTable();
  };

  const handleFormSubmit = (data: CLOUD_SERVICE.IRedisCreateBody) => {
    const reData = { ...data, source: 'resource' };
    const res = addRedis(reData);
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
      <CRUDTable<CLOUD_SERVICE.IRedis>
        key={stateChangeKey}
        isFetching={loading}
        getList={getRedisList}
        list={redisList}
        rowKey="id"
        showTopAdd
        extraOperation={extraOperation}
        getColumns={getColumns}
        getFieldsList={getFieldsList}
        handleFormSubmit={handleFormSubmit}
        addAuthTooltipTitle={addAuthTooltipTitle}
        hasAddAuth={cloudAccountExist}
        onModalClose={() => {
          update({
            chargeType: 'PostPaid',
            chosenRegion: undefined,
          });
        }}
        tableProps={{
          rowSelection: {
            onChange: handleSelect,
          },
        }}
      />
      <SetTagForm
        items={items}
        visible={tagFormVis}
        resourceType="REDIS"
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

export default Redis;
