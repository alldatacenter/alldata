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
import { CRUDTable, Copy, TagsRow, ErdaIcon } from 'common';
import { useUpdate } from 'common/use-hooks';
import { useLoading } from 'core/stores/loading';
import i18n from 'i18n';
import { map, get, find, keys } from 'lodash';
import { Tooltip, Dropdown, Button, Menu, notification } from 'antd';
import { MysqlFieldsConfig } from 'project/pages/third-service/components/config';
import { FormInstance } from 'core/common/interface';
import { useEffectOnce } from 'react-use';
import networksStore from 'cmp/stores/networks';
import cloudCommonStore from 'app/modules/cmp/stores/cloud-common';
import cloudServiceStore from '../../stores/cloud-service';
import { addAuthTooltipTitle } from 'app/modules/cmp/common/cloud-common';
import { goTo, isPromise } from 'common/utils';
import { SetTagForm } from 'cmp/common/components/set-tag-form';
import { ClusterLog } from 'cmp/pages/cluster-manage/cluster-log';
import {
  getCloudResourceIDNameCol,
  getCloudResourceStatusCol,
  getCloudResourceChargeTypeCol,
  getCloudResourceRegionCol,
} from 'cmp/common/components/table-col';
import { skipInfoStatusMap } from 'cmp/pages/cloud-source/config';
import { customTagColor } from 'dcos/common/config';

const specList = [...MysqlFieldsConfig.basicTypes, ...MysqlFieldsConfig.highTypes];
// rds = mysql
const RDS = () => {
  const RDSList = cloudServiceStore.useStore((s) => s.RDSList);
  const { addRDS, getRDSList } = cloudServiceStore.effects;
  const [loading] = useLoading(cloudServiceStore, ['getRDSList']);
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

  const [
    { chargeType, chosenRegion, ifSetTagFormVisible, stateChangeKey, selectedList, ifSelected, recordID },
    updater,
    update,
  ] = useUpdate({
    chargeType: 'PostPaid',
    chosenRegion: undefined as string | undefined,
    ifSetTagFormVisible: false,
    stateChangeKey: 1,
    selectedList: [],
    ifSelected: false,
    recordID: '',
  });

  React.useEffect(() => {
    chosenRegion && getVpcList({ region: chosenRegion });
  }, [chosenRegion, getVpcList]);

  const checkSelect = (selectedRows: CLOUD_SERVICE.IRDS[]) => {
    const newIfSelected = !!selectedRows.length;
    const newSelectedList = selectedRows.map(
      (item: CLOUD_SERVICE.IRDS) =>
        ({
          region: item.region,
          vendor: 'alicloud',
          resourceID: item.id,
          oldTags: Object.keys(item.tag),
        } as CLOUD.TagItem),
    );

    update({
      selectedList: newSelectedList,
      ifSelected: newIfSelected,
    });
    return newIfSelected;
  };
  const handleSelect = (selectedRowKeys: string, selectedRows: CLOUD_SERVICE.IRDS[]) => {
    checkSelect(selectedRows);
  };

  const getColumns = () => {
    const columns = [
      getCloudResourceIDNameCol('id', 'name', (id: string, record: CLOUD_SERVICE.IRDS) => {
        if (skipInfoStatusMap.rds.includes(record.status)) {
          goTo(`./${id}/info?region=${record.region || ''}`);
        } else {
          notification.warning({
            message: i18n.t('warning'),
            description: i18n.t('cmp:Instance is not ready'),
          });
        }
      }),
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
      getCloudResourceStatusCol('rds'),
      {
        title: i18n.t('tag'),
        dataIndex: 'tag',
        align: 'left',
        render: (value: Obj) => {
          const keyArray = keys(value);
          return (
            <TagsRow
              labels={keyArray.map((key) => {
                const label = get(key.split('/'), 1, '');
                return { label, color: customTagColor[label] };
              })}
            />
          );
        },
      },
      getCloudResourceRegionCol('region'),
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
            form.setFieldsValue({ vpcID: undefined });
            updater.chosenRegion(val);
          },
        },
      },
      {
        label: 'VPC',
        name: 'vpcID',
        type: 'select',
        options: map(vpcList, (item) => ({ value: item.vpcID, name: `${item.vpcName} (${item.vpcID})` })),
      },
      ...MysqlFieldsConfig.getFields(form, {
        chargeType,
        onChangeChargeType: (val) => updater.chargeType(val),
      }),
    ];
    return fieldsList;
  };

  const handleFormSubmit = (data: CLOUD_SERVICE.IRDSCreateBody) => {
    const reData = { ...data, storageSize: +data.storageSize, source: 'resource' };
    const res = addRDS(reData);
    if (isPromise(res)) {
      res.then((addRes: any) => {
        updater.recordID(get(addRes, 'data.recordID'));
      });
    }
  };

  const resetTable = () => {
    updater.stateChangeKey(stateChangeKey + 1);
    checkSelect([]);
  };

  const afterTagFormSubmit = () => {
    resetTable();
  };

  const operationButtons = [
    {
      name: `${i18n.t('set tags')}`,
      cb: () => updater.ifSetTagFormVisible(true),
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
      <CRUDTable<CLOUD_SERVICE.IRDS>
        key={stateChangeKey}
        isFetching={loading}
        getList={getRDSList}
        name="RDS"
        list={RDSList}
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
      <Copy selector=".cursor-copy" />
      <SetTagForm
        visible={ifSetTagFormVisible}
        items={selectedList}
        onCancel={() => updater.ifSetTagFormVisible(false)}
        resourceType="RDS"
        showProjectLabel
        showClustertLabel={false}
        afterSubmit={afterTagFormSubmit}
      />
      <ClusterLog recordID={recordID} onClose={() => updater.recordID('')} />
    </>
  );
};

export default RDS;
