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
import networksStore from 'cmp/stores/networks';
import { useLoading } from 'core/stores/loading';
import { Select, Menu, Dropdown, Button } from 'antd';
import { VswCIDRField } from '../common/components/cidr-input';
import { getSubnetCount } from '../common/util';
import { cloudVendor, formConfig } from '../common/config';
import { map, find, get, keys } from 'lodash';
import { useEffectOnce } from 'react-use';
import { FormInstance } from 'core/common/interface';
import cloudCommonStore from 'app/modules/cmp/stores/cloud-common';
import { addAuthTooltipTitle } from 'app/modules/cmp/common/cloud-common';
import i18n from 'i18n';
import routeInfoStore from 'core/stores/route';
import {
  getCloudResourceTagsCol,
  getCloudResourceIDNameCol,
  getCloudResourceStatusCol,
  getCloudResourceRegionCol,
} from 'cmp/common/components/table-col';
import { SetTagForm } from 'cmp/common/components/set-tag-form';

const { Option } = Select;

const VSW = () => {
  const [vswList, vpcList] = networksStore.useStore((s) => [s.vswList, s.vpcList]);
  const vpcID = routeInfoStore.useStore((s) => s.params.vpcID);
  const { getVswList, getVpcList, getCloudZone, addVsw } = networksStore.effects;
  const [loading] = useLoading(networksStore, ['getVswList']);
  const cloudAccountExist = cloudCommonStore.useStore((s) => s.cloudAccountExist);

  useEffectOnce(() => {
    getVpcList({ pageNo: 1, pageSize: 20 });
  });

  const [
    { chosenVpc, zones, subnetCount, tagFormVis, tagFormData, items, ifSelected, stateChangeKey },
    updater,
    update,
  ] = useUpdate({
    vendor: undefined as string | undefined,
    chosenVpc: undefined as undefined | NETWORKS.ICloudVpc,
    zones: [] as NETWORKS.ICloudZone[],
    subnetCount: 0,
    tagFormVis: false,
    tagFormData: null,
    items: [] as CLOUD.TagItem[],
    ifSelected: false,
    stateChangeKey: 1,
  });

  React.useEffect(() => {
    const { regionID: vpcRegion, vendor: vpcVendor } = chosenVpc || {};
    vpcRegion &&
      vpcVendor &&
      getCloudZone({ region: vpcRegion, vendor: vpcVendor }).then((res) => {
        updater.zones(res);
      });
  }, [chosenVpc, getCloudZone, updater]);

  const getColumns = () => {
    const columns = [
      getCloudResourceIDNameCol('vSwitchID', 'vswName'),
      {
        title: i18n.t('cmp:CIDR'),
        width: 150,
        dataIndex: 'cidrBlock',
      },
      getCloudResourceRegionCol('region'),
      getCloudResourceStatusCol('vsw'),
      getCloudResourceTagsCol(),
      {
        title: i18n.t('operation'),
        dataIndex: 'op',
        width: 100,
        render: (_v: any, record: NETWORKS.ICloudVsw) => {
          return (
            <div className="table-operations">
              <span
                className="table-operations-btn"
                onClick={() => {
                  const { vSwitchID, vendor, region, tags } = record;
                  update({
                    tagFormVis: true,
                    tagFormData: {
                      tags: keys(tags),
                    },
                    items: [
                      {
                        vendor,
                        region,
                        resourceID: vSwitchID,
                        oldTags: keys(tags),
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

  const getFieldsList = (form: FormInstance) => {
    const fieldsList = [
      {
        label: i18n.t('name'),
        name: 'vswName',
        rules: [formConfig.rule.name],
        itemProps: {
          maxLength: 128,
        },
      },
      {
        label: i18n.t('cmp:associate vpc network'),
        name: 'vpcID',
        type: 'select',
        options: map(vpcList, (item) => ({ value: item.vpcID, name: `${item.vpcName}(${item.vpcID})` })),
        itemProps: {
          showSearch: true,
          onChange: (val: string) => {
            const curVpc = find(vpcList, { vpcID: val });
            form.setFieldsValue({ zoneID: undefined });
            updater.chosenVpc(curVpc);
          },
        },
      },
      {
        label: formConfig.label.Zone,
        name: 'zoneID',
        type: 'select',
        options: map(zones, (zone) => ({ name: `${zone.localName}(${zone.zoneID})`, value: zone.zoneID })),
      },
      {
        label: `IPv4 ${i18n.t('cmp:CIDR')}`,
        getComp: () => {
          return (
            <VswCIDRField
              form={form}
              vpcCidrBlock={get(chosenVpc, 'cidrBlock')}
              onChangeMask={(mask: number) => updater.subnetCount(getSubnetCount(mask))}
            />
          );
        },
      },
      {
        label: i18n.t('cmp:number of available IP'),
        getComp: () => `${subnetCount || 0}`,
      },
      {
        label: i18n.t('description'),
        name: 'description',
        required: false,
        type: 'textArea',
        rules: [formConfig.rule.description],
        itemProps: {
          maxLength: 256,
        },
      },
    ];
    return fieldsList;
  };

  const handleFormSubmit = (data: any) => {
    const { cidrBlock, ...rest } = data;
    return addVsw({
      ...rest,
      cidrBlock: `${cidrBlock.slice(0, 4).join('.')}/${cidrBlock.slice(4)}`,
      region: chosenVpc && chosenVpc.regionID,
    });
  };

  const checkSelect = (selectedRows: NETWORKS.ICloudVsw[]) => {
    const newIfSelected = !!selectedRows.length;
    const newItems = selectedRows.map(({ vSwitchID, vendor, region, tags }): CLOUD.TagItem => {
      return {
        vendor,
        region,
        resourceID: vSwitchID,
        oldTags: Object.keys(tags),
      };
    });

    update({
      items: newItems,
      ifSelected: newIfSelected,
    });
    return newIfSelected;
  };
  const handleSelect = (selectedRowKeys: string, selectedRows: NETWORKS.ICloudVsw[]) => {
    checkSelect(selectedRows);
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
      cb: () => {
        update({
          tagFormVis: true,
          tagFormData: [],
        });
      },
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
      <CRUDTable<NETWORKS.ICloudVsw>
        key={stateChangeKey}
        isFetching={loading}
        getList={getVswList}
        list={vswList}
        showTopAdd
        extraOperation={extraOperation}
        hasAddAuth={cloudAccountExist}
        addAuthTooltipTitle={addAuthTooltipTitle}
        name={i18n.t('cmp:VSwitches')}
        rowKey="vSwitchID"
        extraQuery={{
          vpcID,
        }}
        getColumns={getColumns}
        handleFormSubmit={handleFormSubmit}
        getFieldsList={getFieldsList}
        filterConfig={filterConfig}
        tableProps={{
          rowSelection: {
            onChange: handleSelect,
          },
        }}
      />
      <Copy selector=".cursor-copy" />
      <SetTagForm
        items={items}
        visible={tagFormVis}
        resourceType="VSWITCH"
        formData={tagFormData as any}
        onCancel={() => updater.tagFormVis(false)}
        afterSubmit={afterTagFormSubmit}
      />
    </>
  );
};

export default VSW;
