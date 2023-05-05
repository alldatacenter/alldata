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
import { ConfigLayout } from 'common';
import { ArtifactsTypeMap } from './config';
import { get, map } from 'lodash';
import { Row, Col, Tooltip, Avatar } from 'antd';
import i18n from 'i18n';
import { insertWhen } from 'common/utils';
// import routeInfoStore from 'core/stores/route';

interface IInfoBlockField {
  label: string;
  name?: string;
  value?: string | React.ReactNode;
  viewType?: string;
  render?: (val: any) => string;
}
export const getInfoBlock = (fieldsList: IInfoBlockField[], data: any) => {
  const layout = {
    lg: { span: 6 },
    md: { span: 12 },
    sm: { span: 24 },
  };
  return (
    <Row gutter={20}>
      {map(fieldsList, (item) => {
        let val = item.value || (item.name && get(data, item.name));
        if (val !== undefined && val !== null && val !== '') {
          val = item.render ? item.render(val) : val;
          return (
            <Col {...layout} key={item.name} className="mb-8">
              <div className="text-desc mb-2">{item.label}</div>
              <div className="nowrap">
                {item.viewType === 'image' ? (
                  <Avatar shape="square" src={val} size={100}>
                    {i18n.t('none')}
                  </Avatar>
                ) : item.viewType === 'images' ? (
                  map(val, (url) => (
                    <span key={url} className="mr-2">
                      <Avatar shape="square" src={url} size={100}>
                        {i18n.t('none')}
                      </Avatar>
                    </span>
                  ))
                ) : (
                  <Tooltip title={val}>{val}</Tooltip>
                )}
              </div>
            </Col>
          );
        }
        return null;
      })}
    </Row>
  );
};

export const ArtifactsInfo = ({ data }: { data: PUBLISHER.IArtifacts }) => {
  // const { mode } = routeInfoStore.useStore(s => s.params);

  const basicFieldsList = [
    {
      label: i18n.t('publisher:publisher content name'),
      name: 'name',
    },
    {
      label: i18n.t('publisher:publisher display name'),
      name: 'displayName',
    },
    {
      label: i18n.t('type'),
      name: 'type',
      render: (val: string) => get(ArtifactsTypeMap, `${val}.name`),
    },
    {
      label: i18n.t('description'),
      name: 'desc',
    },
    {
      label: i18n.t('icon'),
      name: 'logo',
      viewType: 'image',
    },
    {
      label: 'ak',
      name: 'ak',
    },
    {
      label: 'ai',
      name: 'ai',
    },
    // {
    //   label: i18n.t('publisher:background image'),
    //   name: 'backgroundImage',
    //   viewType: 'image',
    // },
    // {
    //   label: i18n.t('publisher:preview image'),
    //   name: 'previewImages',
    //   viewType: 'images',
    // },
  ];
  const safetyFieldsList = [
    {
      label: i18n.t('publisher:jail break control'),
      name: 'noJailbreak',
      render: (val: boolean) => (val ? i18n.t('enable') : i18n.t('disable')),
    },
    {
      label: i18n.t('publisher:geographical fence control'),
      name: 'isGeofence',
      render: (val: boolean) => (val ? i18n.t('enable') : i18n.t('disable')),
    },
  ];

  if (data.geofenceLon) {
    safetyFieldsList.push(
      ...([
        {
          label: i18n.t('publisher:longitude of center coordinates'),
          name: 'geofenceLon',
        },
        {
          label: i18n.t('publisher:latitude of center coordinates'),
          name: 'geofenceLat',
        },
        {
          label: i18n.t('publisher:radius from center'),
          name: 'geofenceRadius',
          render: (val: string) => `${val}${i18n.t('meters')}`,
        },
      ] as any[]),
    );
  }

  const sectionList: any[] = [
    {
      title: i18n.t('basic information'),
      children: getInfoBlock(basicFieldsList, data),
    },
    ...insertWhen(data.type === ArtifactsTypeMap.MOBILE.value, [
      {
        title: i18n.t('safety information'),
        children: getInfoBlock(safetyFieldsList, { ...data, isGeofence: !!data.geofenceLon }),
      },
    ]),
  ];

  return (
    <>
      <ConfigLayout sectionList={sectionList} />
    </>
  );
};
