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
import { Link } from 'react-router-dom';
import networksStore from 'cmp/stores/networks';
import { useLoading } from 'core/stores/loading';
import { Select, Button, Tooltip, Dropdown, Menu } from 'antd';
import { cloudVendor } from '../common/config';
import VpcFormModal from './vpc-form-modal';
import { map } from 'lodash';
import cloudCommonStore from 'app/modules/cmp/stores/cloud-common';
import { addAuthTooltipTitle } from 'app/modules/cmp/common/cloud-common';
import i18n from 'i18n';
import {
  getCloudResourceTagsCol,
  getCloudResourceIDNameCol,
  getCloudResourceStatusCol,
  getCloudResourceRegionCol,
} from 'cmp/common/components/table-col';
import { SetTagForm } from 'cmp/common/components/set-tag-form';
import './index.scss';

const { Option } = Select;

const VPS = () => {
  const vpcList = networksStore.useStore((s) => s.vpcList);
  const { getVpcList } = networksStore.effects;
  const [loading] = useLoading(networksStore, ['getVpcList']);
  const cloudAccountExist = cloudCommonStore.useStore((s) => s.cloudAccountExist);

  const [{ formVis, tagFormVis, items, ifSelected, stateChangeKey, tagFormData }, updater, update] = useUpdate({
    formVis: false,
    tagFormVis: false,
    tagFormData: null,
    items: [] as CLOUD.TagItem[],
    ifSelected: false,
    stateChangeKey: 1,
  });

  const getColumns = () => {
    const columns = [
      getCloudResourceIDNameCol('vpcID', 'vpcName'),
      {
        title: i18n.t('cmp:CIDR'),
        dataIndex: 'cidrBlock',
        width: 150,
      },
      getCloudResourceRegionCol(),
      getCloudResourceStatusCol('vpc'),
      getCloudResourceTagsCol(),
      {
        title: i18n.t('cmp:number of VSW'),
        dataIndex: 'vswNum',
        width: 100,
        render: (v: number, record: NETWORKS.ICloudVpc) => {
          return (
            <Link to={`./vpc/${record.vpcID}/vsw`}>
              <b>{v}</b>
            </Link>
          );
        },
      },
      {
        title: i18n.t('operation'),
        dataIndex: 'op',
        width: 100,
        render: (_v: any, record: NETWORKS.ICloudVpc) => {
          return (
            <div className="table-operations">
              <span
                className="table-operations-btn"
                onClick={() => {
                  const { vpcID, vendor, tags, regionID } = record;
                  update({
                    tagFormVis: true,
                    tagFormData: {
                      tags: Object.keys(tags),
                    },
                    items: [
                      {
                        vendor,
                        region: regionID,
                        resourceID: vpcID,
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
    return columns;
  };

  const filterConfig = React.useMemo(
    () => [
      {
        type: Select,
        name: 'vendor',
        customProps: {
          className: 'w-52',
          placeholder: i18n.t('filter by {name}', { name: i18n.t('cloud vendor') }),
          options: map(cloudVendor, (item) => (
            <Option key={item.name} value={item.value}>
              {item.name}
            </Option>
          )),
          allowClear: true,
        },
      },
    ],
    [],
  );

  const operationButtons = [
    {
      name: `${i18n.t('set tags')}`,
      cb: () => updater.tagFormVis(true),
      ifDisabled: false,
    },
  ];

  const checkSelect = (selectedRows: NETWORKS.ICloudVpc[]) => {
    const newIfSelected = !!selectedRows.length;
    const newItems = selectedRows.map(({ vpcID, vendor, regionID, tags }): CLOUD.TagItem => {
      return {
        vendor,
        region: regionID,
        resourceID: vpcID,
        oldTags: Object.keys(tags),
      };
    });

    update({
      items: newItems,
      ifSelected: newIfSelected,
    });
    return newIfSelected;
  };
  const handleSelect = (selectedRowKeys: string, selectedRows: NETWORKS.ICloudVpc[]) => {
    checkSelect(selectedRows);
  };

  const handleCancel = () => {
    updater.formVis(false);
  };

  const handleOK = () => {
    updater.formVis(false);
    updater.stateChangeKey(stateChangeKey + 1);
  };

  const resetTable = () => {
    updater.stateChangeKey(stateChangeKey + 1);
    checkSelect([]);
  };
  const afterTagFormSubmit = () => {
    resetTable();
  };

  const menu = (
    <Menu>
      {operationButtons.map((button) => (
        <Menu.Item disabled={button.ifDisabled} key={button.name} onClick={button.cb}>
          {button.name}
        </Menu.Item>
      ))}
    </Menu>
  );
  return (
    <>
      <div className="top-button-group">
        {cloudAccountExist ? (
          <Button type="primary" onClick={() => updater.formVis(true)}>
            {i18n.t('add {name}', { name: i18n.t('cmp:VPC') })}
          </Button>
        ) : (
          <Tooltip placement="left" title={addAuthTooltipTitle}>
            <Button type="primary" disabled>
              {i18n.t('add {name}', { name: i18n.t('cmp:VPC') })}
            </Button>
          </Tooltip>
        )}
        <Dropdown disabled={!ifSelected} overlay={menu}>
          <Button type="primary">
            <div className="flex">
              {i18n.t('batch setting')}
              <ErdaIcon type="caret-down" className="ml-1" size="20" />
            </div>
          </Button>
        </Dropdown>
      </div>
      <CRUDTable<NETWORKS.ICloudVpc>
        key={stateChangeKey}
        isFetching={loading}
        getList={getVpcList}
        list={vpcList}
        rowKey="vpcID"
        getColumns={getColumns}
        filterConfig={filterConfig}
        tableProps={{
          rowSelection: {
            onChange: handleSelect,
          },
        }}
      />
      <VpcFormModal visible={formVis} onCancel={handleCancel} onOk={handleOK} />
      <SetTagForm
        items={items}
        visible={tagFormVis}
        formData={tagFormData as any}
        resourceType="VPC"
        onCancel={() => update({ tagFormVis: false, tagFormData: null })}
        afterSubmit={afterTagFormSubmit}
      />
      <Copy selector=".cursor-copy" />
    </>
  );
};

export default VPS;
