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
import { Select } from 'antd';
import { map, isString } from 'lodash';
import k8s_default_svg from 'app/images/resources/k8s-1.svg';
import k8s_active_svg from 'app/images/resources/k8s.svg';
import edas_default_svg from 'app/images/resources/edas-1.svg';
import edas_active_svg from 'app/images/resources/edas.svg';
import erdc_default_svg from 'app/images/resources/container-service-1.svg';
import erdc_active_svg from 'app/images/resources/container-service.svg';
import alicloud_cs_default_svg from 'app/images/resources/zyb.svg';
import alicloud_cs_active_svg from 'app/images/resources/zyb2.svg';
import alicloud_cs_managed_default_svg from 'app/images/resources/tg.svg';
import alicloud_cs_managed_active_svg from 'app/images/resources/tg2.svg';
import alicloud_ecs_default_svg from 'app/images/resources/ali-cloud-1.svg';
import alicloud_ecs_active_svg from 'app/images/resources/ali-cloud.svg';
import i18n from 'i18n';

const { Option, OptGroup } = Select;

export const clusterImgMap = {
  k8s: {
    default: k8s_default_svg,
    active: k8s_active_svg,
  },
  edas: {
    default: edas_default_svg,
    active: edas_active_svg,
  },
  erdc: {
    default: erdc_default_svg,
    active: erdc_active_svg,
  },
  'alicloud-cs': {
    default: alicloud_cs_default_svg,
    active: alicloud_cs_active_svg,
  },
  'alicloud-cs-managed': {
    default: alicloud_cs_managed_default_svg,
    active: alicloud_cs_managed_active_svg,
  },
  'alicloud-ecs': {
    default: alicloud_ecs_default_svg,
    active: alicloud_ecs_active_svg,
  },
};

export const regionMap = {
  asia: {
    name: i18n.t('Asia-pacific'),
    children: [
      { name: `${i18n.t('North China {num}', { num: 1 })}（${i18n.t('Qingdao')}）`, value: 'cn-qingdao' },
      { name: `${i18n.t('North China {num}', { num: 2 })}（${i18n.t('Beijing')}）`, value: 'cn-beijing' },
      { name: `${i18n.t('North China {num}', { num: 3 })}（${i18n.t('Zhangjiakou')}）`, value: 'cn-zhangjiakou' },
      { name: `${i18n.t('North China {num}', { num: 5 })}（${i18n.t('Huhhot')}）`, value: 'cn-huhehaote' },
      { name: `${i18n.t('East China {num}', { num: 1 })}（${i18n.t('Hangzhou')}）`, value: 'cn-hangzhou' },
      { name: `${i18n.t('East China {num}', { num: 2 })}（${i18n.t('Shanghai')}）`, value: 'cn-shanghai' },
      { name: `${i18n.t('South China', { num: 1 })}（${i18n.t('Shenzhen')}）`, value: 'cn-shenzhen' },
      { name: `${i18n.t('South China', { num: 2 })}（${i18n.t('Heyuan')}）`, value: 'cn-heyuan' },
      { name: `${i18n.t('Southwest China {num}', { num: 1 })}（${i18n.t('Chengdu')}）`, value: 'cn-chengdu' },
      { name: `${i18n.t('China')}（${i18n.t('Hong Kong')}）`, value: 'cn-hongkong' },
      { name: i18n.t('Singapore'), value: 'ap-southeast-1' },
      { name: `${i18n.t('Australia')}（${i18n.t('Sydney')}）`, value: 'ap-southeast-2' },
      { name: `${i18n.t('Malaysia')}（${i18n.t('Kuala lumpur')}）`, value: 'ap-southeast-3' },
      { name: `${i18n.t('Indonesia')}（${i18n.t('Jakarta')}）`, value: 'ap-southeast-5' },
      { name: `${i18n.t('Japan')}（${i18n.t('Tokyo')}）`, value: 'ap-northeast-1' },
    ],
  },
  western: {
    name: i18n.t('Europe & America'),
    children: [
      { name: `${i18n.t('United States')}（${i18n.t('Silicon valley')}）`, value: 'us-west-1' },
      { name: `${i18n.t('United States')}（${i18n.t('Virginia')}）`, value: 'us-east-1' },
      { name: `${i18n.t('Germany')}（${i18n.t('Frankfurt')}）`, value: 'eu-central-1' },
      { name: `${i18n.t('Britain')}（${i18n.t('London')}）`, value: 'eu-west-1' },
    ],
  },
  middleEast: {
    name: i18n.t('Middle-east & India'),
    children: [
      { name: `${i18n.t('United Arab Emirates')}（${i18n.t('Dubai')}）`, value: 'me-east-1' },
      { name: `${i18n.t('India')}（${i18n.t('Mumbai')}）`, value: 'ap-south-1' },
    ],
  },
};

export const clusterSpecMap = {
  'alicloud-ecs': {
    Standard: {
      name: i18n.t('standard'),
      value: 'Standard',
      tip: (
        <>
          {i18n.t('cmp:masters occupy 3 nodes')}
          <br />
          {i18n.t('cmp:lb occupies 2 nodes')}
          <br />
          {i18n.t('cmp:platform occupies 2 nodes')}
          <br />
        </>
      ),
    },
    Small: { name: i18n.t('small'), value: 'Small', tip: i18n.t('cmp:masters, lb and platform share 3 nodes') },
    Test: { name: i18n.t('demo'), value: 'Test', tip: i18n.t('cmp:masters, LB and platform share 1 node') },
  },
  'alicloud-cs': {
    Standard: { name: i18n.t('standard'), value: 'Standard', tip: i18n.t('cmp:3 machines') },
  },
  'alicloud-cs-managed': {
    Standard: { name: i18n.t('standard'), value: 'Standard', tip: i18n.t('cmp:2 machines') },
  },
  erdc: {
    test: { name: i18n.t('test'), value: 'test' },
    prod: { name: i18n.t('prod'), value: 'prod' },
  },
};

export const chargeTypeMap = {
  PrePaid: { name: i18n.t('cmp:Subscription'), value: 'PrePaid' },
  PostPaid: { name: i18n.t('cmp:Pay-As-You-Go'), value: 'PostPaid' },
};

export const chargePeriodMap = [
  { name: i18n.t('{num} month(s)', { num: 1 }), value: 1 },
  { name: i18n.t('{num} month(s)', { num: 2 }), value: 2 },
  { name: i18n.t('{num} month(s)', { num: 3 }), value: 3 },
  { name: i18n.t('{num} month(s)', { num: 6 }), value: 6 },
  { name: i18n.t('{num} year(s)', { num: 1 }), value: 12 },
  { name: i18n.t('{num} year(s)', { num: 2 }), value: 24 },
  { name: i18n.t('{num} year(s)', { num: 3 }), value: 36 },
  { name: i18n.t('{num} year(s)', { num: 4 }), value: 48 },
  { name: i18n.t('{num} year(s)', { num: 5 }), value: 60 },
];

export const diskTypeMap = {
  cloud_ssd: { name: i18n.t('cmp:SSD cloud'), value: 'cloud_ssd' },
  cloud_efficiency: { name: i18n.t('cmp:efficiency cloud'), value: 'cloud_efficiency' },
};

export const groupOptions = (list: any[], optionRender?: (arg: any) => React.ReactChild) => {
  return map(list, (item) => {
    const { name, children } = item;
    return (
      <OptGroup label={name} key={name}>
        {map(children, (subItem: any) => {
          if (optionRender) return optionRender(subItem);
          if (isString(subItem)) {
            return (
              <Option key={subItem} value={subItem}>
                {subItem}
              </Option>
            );
          } else {
            const { name: cName, value: cValue } = subItem;
            return (
              <Option key={cName} value={cValue}>
                {cName}
              </Option>
            );
          }
        })}
      </OptGroup>
    );
  });
};

const optionsMap = {
  region: regionMap,
  chargeType: chargeTypeMap,
  chargePeriod: chargePeriodMap,
  diskType: diskTypeMap,
};
export const getOptions = (type: string) => {
  if (optionsMap[type]) {
    if (type === 'region' || type === 'label') {
      return () => groupOptions(optionsMap[type]);
    } else {
      return map(optionsMap[type], ({ name, value }) => ({ name, value }));
    }
  }
  return [];
};

export const TYPE_K8S_AND_EDAS = ['k8s', 'edas'];
export const EMPTY_CLUSTER = '_';
export const replaceContainerCluster = (v: string) => {
  const { search, pathname } = location;
  const [p1, p2] = pathname.split('/cmp/container/');
  if (p2) {
    const newUrl = `${p1}/cmp/container/${v}/${p2.split('/').slice(1).join('/')}${search ? `?${search}` : ''}`;
    return newUrl;
  }
  return `${pathname}${search ? `?${search}` : ''}`;
};
