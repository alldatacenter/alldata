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

import { cutStr } from 'common/utils';
import i18n from 'i18n';
import regionData from 'cmp/common/regionData';
import { Tooltip, Badge } from 'antd';
import React from 'react';
import moment from 'moment';
import { get } from 'lodash';
import { statusMap } from 'cmp/pages/cloud-source/config';
import { chargeTypeMap } from 'cmp/pages/cluster-manage/config';

export const getCloudResourceTagsCol = (config?: Obj) => {
  return {
    title: i18n.t('tag'),
    dataIndex: 'tags',
    render: (_val: any) => {
      if (!_val) {
        return null;
      }
      const tags = Object.keys(_val).map((key) => get(key.split('/'), 1, ''));
      const showCount = 3;
      const showMore = tags.length > showCount && tags.length > 15 * showCount;
      const oneAndMoreTag = (
        <React.Fragment>
          {tags.slice(0, showCount).map((l) => (
            <span key={l} className="tag-default">
              {cutStr(l, 15)}
            </span>
          ))}
          {showMore ? (
            <span>...&nbsp;&nbsp;</span>
          ) : (
            tags.slice(showCount).map((l) => (
              <span key={l} className="tag-default">
                {cutStr(l, 15)}
              </span>
            ))
          )}
        </React.Fragment>
      );
      const fullTags = (withCut?: boolean) =>
        tags.map((l) => (
          <span className="tag-default" key={l}>
            {withCut ? cutStr(l, 15) : l}
          </span>
        ));
      return (
        <div className="cloud-resource-tags-container">
          <Tooltip title={fullTags()} placement="top" overlayClassName="tags-tooltip">
            <span className="cloud-resource-tags-box">{showMore ? oneAndMoreTag : fullTags(true)}</span>
          </Tooltip>
        </div>
      );
    },
    ...config,
  };
};

export const getCloudResourceIDNameCol = (dataIndex = 'id', nameKey = 'name', click?: any) => {
  const linkStyle = click ? 'fake-link' : '';
  return {
    title: `ID/${i18n.t('name')}`,
    dataIndex,
    render: (val: string, record: any) => {
      return (
        <div>
          <div>
            <Tooltip title={val}>
              <div className="cursor-copy nowrap" data-clipboard-tip="ID" data-clipboard-text={val}>
                {val || i18n.t('common:none')}
              </div>
            </Tooltip>
          </div>
          <div onClick={() => click && click(val, record)} className={linkStyle}>
            {record[nameKey]}
          </div>
        </div>
      );
    },
  };
};

export const getCloudResourceTimeCol = (title = i18n.t('create time'), dataIndex = 'createTime') => {
  return {
    title,
    dataIndex,
    width: 120,
    render: (val: string) => {
      if (!val) {
        return null;
      }
      const [date, time] = moment(val).format('YYYY-MM-DD HH:mm:ss').split(' ');
      return (
        <>
          <div>{date}</div>
          <div>{time}</div>
        </>
      );
    },
  };
};

type IResourceType = 'rds' | 'ecs' | 'vpc' | 'mq' | 'vsw' | 'oss' | 'redis' | 'account';
export const getCloudResourceStatusCol = (
  resourceType: IResourceType,
  title = i18n.t('status'),
  dataIndex = 'status',
) => {
  return {
    title,
    dataIndex,
    width: 90,
    render: (value: string) => {
      return <Badge status={statusMap[resourceType][value]?.status || 'default'} text={value} />;
    },
  };
};

const chooseShowTime = (value: string, time: string, msg?: string) => {
  const formatTime = moment(time).utc().format('YYYY-MM-DD');
  return (
    <div>
      <div>{chargeTypeMap[value].name || value}</div>
      <span>
        {msg}
        {formatTime}
      </span>
    </div>
  );
};
export const getCloudResourceChargeTypeCol = (
  dataIndex = 'chargeType',
  startTime = 'startTime',
  expireTime = 'expireTime',
) => {
  return {
    title: i18n.t('cmp:payment methods'),
    dataIndex,
    width: 170,
    render: (_v: string, record: any) => {
      let val: any = _v?.toLowerCase();
      if (val === chargeTypeMap.PostPaid.value.toLowerCase()) {
        val = chooseShowTime('PostPaid', record[startTime], `${i18n.t('create time')}: `);
      } else if (val === chargeTypeMap.PrePaid.value.toLowerCase()) {
        val = chooseShowTime('PrePaid', record[expireTime], `${i18n.t('cmp:expire time')}: `);
      }

      return <Tooltip title={val}>{val}</Tooltip>;
    },
  };
};

export const getCloudResourceRegionCol = (dataIndex = 'regionID', msg?: any) => {
  return {
    title: i18n.t('region'),
    dataIndex,
    render: (value: string, record: any) => {
      let reMsg = msg;
      typeof msg === 'function' && (reMsg = msg(value, record));
      const regionObj = regionData.find((item) => item.regionID === value);
      const region = i18n.language === 'zh' ? regionObj?.localName : regionObj?.regionID;
      const val = reMsg || region;
      return <Tooltip title={val}>{val}</Tooltip>;
    },
  };
};

export const getRemarkCol = (dataIndex = 'remark') => {
  return {
    title: i18n.t('dop:remark'),
    dataIndex,
    ellipsis: {
      showTitle: false,
    },
    render: (_v: string) => {
      return _v ? <Tooltip title={_v}>{_v}</Tooltip> : '_';
    },
  };
};
