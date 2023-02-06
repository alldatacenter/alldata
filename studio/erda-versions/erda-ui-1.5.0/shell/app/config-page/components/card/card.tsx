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
import { Icon as CustomIcon, ErdaIcon } from 'common';
import { Dropdown, Menu, Popconfirm, Tooltip } from 'antd';
import { isString, isEmpty, map } from 'lodash';
import { WithAuth } from 'user/common';
import { useDrag } from 'react-dnd';
import classnames from 'classnames';
import './card.scss';

const fakeClick = 'fake-click';
const noop = () => {};

export const Card = (props: CP_CARD.Props) => {
  const { props: configProps, execOperation = noop, customOp = {} } = props;
  const { cardType, data, className = '', setIsDrag, CardRender } = configProps;
  const { clickCard = noop } = customOp;
  const [isHover, setIsHover] = React.useState(false);
  const { id, titleIcon, title, operations, subContent, description, extraInfo } = data || {};
  const { cardMoveTo: dragOperation, ...menuOperations } = operations || {};
  const [dragObj, drag] = useDrag({
    item: { type: cardType, data },
    canDrag: () => {
      return dragOperation && !dragOperation.disabled && !isHover;
    },
    collect: (monitor) => ({
      isDragging: monitor.isDragging(),
    }),
  });

  const opRef = React.useRef(null);

  const onClick = (k: string) => {
    if (!k.startsWith(fakeClick)) {
      execOperation({ ...((operations && operations[k]) || {}), key: k, reload: true });
    }
  };

  React.useEffect(() => {
    if (setIsDrag) {
      setIsDrag(dragObj.isDragging);
    }
  }, [dragObj.isDragging, setIsDrag]);

  const getMenu = () => {
    return (
      <Menu
        onClick={(e) => {
          e.domEvent.stopPropagation();
          onClick(e.key);
        }}
      >
        {map(menuOperations, (item, key) => {
          if (item.disabled) {
            return (
              <Menu.Item key={key}>
                <WithAuth pass={false} key={key} noAuthTip={item.disabledTip}>
                  <span>{item.text}</span>
                </WithAuth>
              </Menu.Item>
            );
          }
          if (item.confirm) {
            return (
              <Menu.Item key={`${fakeClick}-${key}`}>
                <Popconfirm title={item.confirm} onConfirm={() => onClick(key)}>
                  <span>{item.text}</span>
                </Popconfirm>
              </Menu.Item>
            );
          }
          return <Menu.Item key={key}>{item.text}</Menu.Item>;
        })}
      </Menu>
    );
  };
  const cls = classnames({
    'drag-wrap': true,
    dragging: dragObj && dragObj.isDragging,
    'dice-cp': true,
    'info-card': true,
    rounded: true,
    'hover-active-bg': true,
    'border-all': true,
  });
  return (
    <div className={`${className} ${cls}`} onClick={() => clickCard(data)}>
      <div className="info-card-content px-3 pt-2 pb-2" key={id} ref={drag}>
        {CardRender ? (
          <CardRender data={data} />
        ) : (
          <>
            <div className={'flex justify-between items-start mb-3 pt-2'}>
              {isString(titleIcon) ? (
                <CustomIcon type={titleIcon} color className="head-icon mr-1" />
              ) : (
                titleIcon || null
              )}
              <div className="flex-1 text-sm text-normal break-word">{title}</div>
              {isEmpty(menuOperations) ? (
                <ErdaIcon type="more1" className="op-icon hide-icon" onClick={(e) => e.stopPropagation()} />
              ) : (
                <span
                  ref={opRef}
                  className="pr-1"
                  onMouseEnter={() => setIsHover(true)}
                  onMouseLeave={() => setIsHover(false)}
                >
                  <Dropdown overlay={getMenu()} getPopupContainer={() => opRef.current as any}>
                    <ErdaIcon type="more1" className="op-icon" onClick={(e) => e.stopPropagation()} />
                  </Dropdown>
                </span>
              )}
            </div>
            {isString(subContent) ? <div className="text-xs text-sub mb-3">{subContent}</div> : subContent || null}
            {isString(description) ? (
              <Tooltip title={description}>
                <div className="text-xs nowrap text-desc">{description}</div>
              </Tooltip>
            ) : (
              description || null
            )}
            {extraInfo}
          </>
        )}
      </div>
    </div>
  );
};
