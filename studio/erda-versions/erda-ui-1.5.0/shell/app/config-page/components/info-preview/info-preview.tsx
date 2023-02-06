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
import { map, get, isString, isArray } from 'lodash';
import { Table, Tag } from 'antd';
import { FileEditor, Title } from 'common';
import Text from '../text/text';
import './info-preview.scss';

const colorMap = {
  get: '#8DB36C',
  post: '#6CB38B',
  put: '#498E9E',
  delete: '#DE5757',
  patch: '#4E6097',
  default: '#975FA0',
};

const CP_INFO_PREVIEW = (props: CP_INFO_PREVIEW.Props) => {
  const { props: configProps, data } = props;
  const { render = [], visible = true } = configProps || {};
  const { info = {} } = data || {};

  if (!visible) return null;
  return (
    <div className="dice-cp-preview">
      {map(render, (item, idx) => {
        if (item) {
          const { dataIndex } = item;
          return (
            <RenderItem key={`${idx}`} render={item} data={dataIndex && get(info, dataIndex)} />
            // <div key={`${idx}`} className='dice-cp-preview-item'>
            //   {getRender(item, dataIndex && get(info, dataIndex))}
            // </div>
          );
        } else {
          return null;
        }
      })}
    </div>
  );
};

export default CP_INFO_PREVIEW;

interface IRenderProps {
  render: CP_INFO_PREVIEW.IRender;
  data: any;
}
const clsPrex = 'dice-cp-preview-item';
const RenderItem = (props: IRenderProps) => {
  const { render, data } = props;
  const { type, props: rProps } = render;
  let Comp = null;
  switch (type) {
    case 'Title':
      {
        const { titleExtra, ...restProps } = rProps || {};
        Comp =
          data || restProps.title ? (
            <div className={`${clsPrex} flex justify-between items-center mb-0`}>
              <div className="flex-1">
                <Title showDivider={false} title={data} {...restProps} />
              </div>
              {titleExtra ? <div className="ml-2">{titleExtra}</div> : null}
            </div>
          ) : null;
      }
      break;
    case 'Desc':
      {
        const desc = data || rProps?.desc || '';
        const title = rProps?.title;
        Comp = desc ? (
          <div className={`${clsPrex}`}>
            {title && <Title showDivider={false} title={title} level={2} />}
            <div className={'dice-cp-preview-desc'}>{desc}</div>
          </div>
        ) : null;
      }
      break;
    case 'BlockTitle':
      {
        const title = data || rProps?.title || '';
        Comp = title ? <div className={`${clsPrex} dice-cp-preview-block-title text-base`}>{title}</div> : null;
      }
      break;
    case 'API':
      {
        const { method = '', path = '' } = data || rProps?.api || {};
        Comp = path ? (
          <div className={`${clsPrex} dice-cp-preview-api`}>
            <Tag color={colorMap[method.toLowerCase()] || method.default}>{method.toUpperCase()}</Tag>
            <div className="path">{path}</div>
          </div>
        ) : null;
      }
      break;
    case 'Table':
      {
        const { title, ...rest } = rProps || {};
        const dataSource = data || rest?.data || [];
        Comp = (
          <div className={`${clsPrex}`}>
            {title ? <Title showDivider={false} title={title} level={2} /> : null}
            <Table scroll={{ x: '100%' }} defaultExpandAllRows pagination={false} dataSource={dataSource} {...rest} />
          </div>
        );
      }
      break;
    case 'FileEditor':
      {
        const { title, minHeight = 100, maxHeight = 300, ...rest } = rProps || {};
        const _data = data || rest?.data || '';
        Comp =
          _data !== undefined ? (
            <div className={`${clsPrex}`}>
              {title ? <Title title={title} level={2} /> : null}
              {_data !== undefined && _data !== '' ? (
                <FileEditor
                  fileExtension="sh"
                  value={isString(_data) ? _data : JSON.stringify(_data, null, 2)}
                  readOnly
                  style={{
                    border: '1px solid rgba(0, 0, 0, .1)',
                    maxHeight: `${maxHeight}px`,
                    minHeight: `${minHeight}px`,
                  }}
                  actions={rest?.actions}
                />
              ) : null}
            </div>
          ) : null;
      }
      break;
    case 'custom':
      {
        const { title, ...rest } = rProps || {};
        const result = data || rest?.data || null;
        Comp = (
          <div className={`${clsPrex}`}>
            {title ? <Title showDivider={false} title={title} level={2} /> : null}
            {result}
          </div>
        );
      }
      break;
    case 'Text':
      {
        const { align = 'left' } = rProps || {};
        const _className = `text-${align}`;

        Comp = (
          <div className={_className}>
            {isArray(data) ? (
              map(data, (itemProps) => (
                <span className="mr-2" key={itemProps.value}>
                  <Text type="Text" props={itemProps} />
                </span>
              ))
            ) : (
              <Text type="Text" props={data} />
            )}
          </div>
        );
      }
      break;
    default:
      break;
  }
  return Comp;
};
