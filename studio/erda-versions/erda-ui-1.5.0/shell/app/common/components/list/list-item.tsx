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
import { Menu, Tooltip, Dropdown } from 'antd';
import { isString } from 'lodash';
import classnames from 'classnames';
import { Ellipsis, ErdaIcon, Icon as CustomIcon, Badge } from 'common';
import ImgMap, { getImg } from 'app/config-page/img-map';
import { iconMap } from 'common/components/erda-icon';

const getPrefixImg = (prefixImg: string, prefixImgCircle?: boolean) => {
  if (Object.keys(ImgMap).includes(prefixImg)) {
    return (
      <div>
        <img src={getImg(prefixImg)} className={`item-prefix-img ${prefixImgCircle ? 'prefix-img-circle' : ''}`} />
      </div>
    );
  } else if (Object.keys(iconMap).includes(prefixImg)) {
    return <ErdaIcon type={prefixImg} size="76" className={`${prefixImgCircle ? 'prefix-img-circle' : ''}`} />;
  } else {
    return (
      <div>
        <img src={prefixImg} className={`item-prefix-img rounded-sm ${prefixImgCircle ? 'prefix-img-circle' : ''}`} />
      </div>
    );
  }
};

const ListItem = (props: ERDA_LIST.IListItemProps) => {
  const { size = 'middle', data, alignCenter = false, noBorder = false, operations = [], onRow = {} } = props;
  const {
    prefixImg,
    title,
    status,
    titlePrifxIcon,
    prefixImgCircle,
    titlePrifxIconTip,
    titleSuffixIcon,
    titleSuffixIconTip,
    description = '',
    extraInfos,
    extraContent,
    backgroundImg,
  } = data || {};

  const itemClassNames = classnames({
    'flex items-center': alignCenter,
    'no-border': noBorder,
    [size]: size,
    'erda-list-item': true,
    'cursor-pointer': true,
    'rounded-sm': true,
  });

  const operationList = typeof operations === 'function' ? operations(data) : operations;

  const menuOverlay = operationList?.length ? (
    <Menu style={{ minWidth: 80 }}>
      {operationList.map((action) => {
        const { title: operateTitle, onClick, key } = action;
        return (
          <Menu.Item key={key} onClick={onClick}>
            {operateTitle}
          </Menu.Item>
        );
      })}
    </Menu>
  ) : null;

  React.useEffect(() => {}, [onRow, data]);

  const onRowFn = React.useMemo(() => {
    const fnObj = {};
    Object.keys(onRow).forEach((key) => {
      if (typeof onRow[key] === 'function') {
        fnObj[key] = (e: MouseEvent) => {
          onRow[key](data, e);
        };
      }
    });
    return fnObj;
  }, [onRow, data]);

  return (
    <div
      className={itemClassNames}
      {...onRow}
      {...onRowFn}
      style={backgroundImg ? { backgroundImage: `url(${getImg(backgroundImg)})` } : {}}
    >
      <div className="erda-list-item-container hover:bg-gray-block-bg relative">
        <div className="flex">
          {isString(prefixImg) ? (
            <div className="erda-list-item-prefix-img">{getPrefixImg(prefixImg, prefixImgCircle)}</div>
          ) : prefixImg ? (
            <div className="erda-list-item-prefix-img">{prefixImg}</div>
          ) : null}
          <div className="erda-list-item-body">
            <div className={'body-title'}>
              {titlePrifxIcon ? (
                <Tooltip title={titlePrifxIconTip}>
                  <CustomIcon type={titlePrifxIcon} className="title-icon mr-2" />
                </Tooltip>
              ) : null}
              <Ellipsis className="font-bold title-text" title={title} />
              {titleSuffixIcon ? (
                <Tooltip title={titleSuffixIconTip}>
                  <CustomIcon type={titleSuffixIcon} className="title-icon ml-2" />
                </Tooltip>
              ) : null}
              {status?.status && status?.text ? <Badge className="ml-2" {...status} /> : null}
            </div>
            {description ? (
              <Ellipsis className="body-description" title={description} />
            ) : (
              <div className="body-description" />
            )}
            {extraInfos ? (
              <div className="body-extra-info">
                {extraInfos.map((info) => {
                  return (
                    <Tooltip key={info.text} title={info.tooltip}>
                      <span className={`info-item type-${info.type || 'normal'}`} {...info.extraProps}>
                        {info.icon ? <ErdaIcon type={info.icon} isConfigPageIcon size="16" className="mr-1" /> : null}
                        <span className="info-text nowrap">{info.text}</span>
                      </span>
                    </Tooltip>
                  );
                })}
              </div>
            ) : null}
          </div>
        </div>
        <div className="py-1">{extraContent}</div>
        {menuOverlay ? (
          <div className="erda-list-item-operations absolute top-2 right-2" onClick={(e) => e?.stopPropagation()}>
            <Dropdown
              overlay={menuOverlay}
              overlayClassName={'erda-list-operations'}
              overlayStyle={{ zIndex: 1000 }}
              trigger={['click']}
            >
              <ErdaIcon
                type="more"
                size={20}
                className="hover-active px-2 py-1 rounded hover:bg-hover-gray-bg"
                onClick={(e) => e.stopPropagation()}
              />
            </Dropdown>
          </div>
        ) : null}
      </div>
    </div>
  );
};

export default ListItem;
