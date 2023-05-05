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

import { Icon as CustomIcon, ErdaIcon, Ellipsis, Copy, TagsRow } from 'common';
import { map, groupBy, uniq } from 'lodash';
import { useDrag, useDrop } from 'react-dnd';
import { useUpdateEffect } from 'react-use';
import React from 'react';
import { Tooltip, Popconfirm, Switch, Menu, Dropdown } from 'antd';
import './index.scss';

interface IBeginDragData<T> {
  item: T;
  index: number;
}

interface IEndDragData<T> {
  sourceItem: T;
  targetItem: T;
  position: -1 | 0 | 1;
  didDrop: boolean;
}

interface IDragItemOp<T> {
  icon: string;
  onClick: (data: T) => void;
}

const useDragAndDrop = ({ item, index, onBeginDrag, onEndDrag, onMove }: any) => {
  const { type, data } = item;
  const [position, setPosition] = React.useState('');

  const dragRef = React.useRef<HTMLDivElement>(null);
  const [{ isDragging }, drag, preview] = useDrag({
    item: { type, data, index },
    collect: (monitor) => ({
      isDragging: monitor.isDragging(),
    }),
    begin: () => {
      onBeginDrag && onBeginDrag({ item, index });
    },
    end: (_, monitor) => {
      const posMap = {
        top: -1,
        center: 0,
        bottom: 1,
      };
      const result = monitor.getDropResult();
      if (onEndDrag) {
        onEndDrag({
          sourceItem: data,
          targetItem: result?.data,
          position: result?.position && posMap[result.position],
          didDrop: monitor.didDrop(),
        });
      }
    },
  });

  const [{ isOver, area }, drop] = useDrop({
    accept: type,
    collect: (monitor) => {
      if (monitor.isOver({ shallow: true })) {
        // hover 的节点
        const hoverBoundingRect = dragRef.current!.getBoundingClientRect();
        // hover节点高度
        const { height } = hoverBoundingRect;
        // 获取 hover 到的区域分界线：上(25%) 中(50%) 下(25%)
        const Y_top = hoverBoundingRect.top;
        const Y_split1 = hoverBoundingRect.top + height * 0.25;
        const Y_split2 = hoverBoundingRect.top + height * 0.75;
        const Y_bottom = hoverBoundingRect.bottom;

        return {
          isOver: monitor.isOver({ shallow: true }),
          area: [Y_top, Y_split1, Y_split2, Y_bottom],
        };
      }
      return {
        isOver: false,
        area: [],
      };
    },
    hover(dragItem: SortItemData, monitor) {
      const coordinate = monitor.getClientOffset();
      if (coordinate) {
        const mouseY = coordinate.y;
        const pos = mouseY < area[1] ? 'top' : mouseY < area[2] ? 'center' : 'bottom';
        setPosition(pos);
      }
      // Don't replace items with themselves
      if (dragItem.data.id !== data.id) {
        onMove &&
          onMove({
            sourceItem: dragItem,
            targetItem: item,
            position: dragItem.index < index ? 1 : -1,
          });
      }
    },
    drop: () => {
      return { data: item.data, position };
    },
  });

  return { drag: drag(dragRef), preview, drop, position, isDragging, isOver };
};

interface SortDragItemProps<T> {
  item: SortItemData;
  index: number;
  draggable?: boolean;
  disableDropIn?: boolean;
  operations?: Obj<IDragItemOp<T>>;
  onBeginDrag?: (data: IBeginDragData<T>) => void;
  onEndDrag?: (data: IEndDragData<T>) => void;
  onMove?: (data: any) => void;
  onClick?: (data: SortItemData) => void;
}

const SortDragItem = ({
  item,
  index,
  disableDropIn,
  onBeginDrag,
  onEndDrag,
  onMove,
  onClick,
  draggable = true,
}: SortDragItemProps<SortItemData>) => {
  const { data, type, isHolder } = item;
  const { drag, preview, drop, position, isDragging, isOver } = useDragAndDrop({
    item: { type, data: item },
    index,
    onBeginDrag,
    onEndDrag,
    onMove,
  });

  let hoverCls = '';
  if (!isDragging && isOver) {
    if (disableDropIn && position === 'center') {
      hoverCls = '';
    } else {
      hoverCls = `hover-position-tip hover-position-${position}`;
    }
  }

  if (isHolder) {
    return <div ref={draggable ? drop(drag) : undefined} className="sort-drag-item" />;
  }

  return (
    <div
      ref={draggable ? (node) => drop(preview(node)) : undefined}
      key={index}
      className={`flex justify-between items-center sort-drag-item ${hoverCls}`}
      onClick={() => onClick && onClick(item)}
    >
      <div
        ref={draggable ? drag : undefined}
        className="px-2 icon-block drag-handle"
        onClick={(e) => e.stopPropagation()}
      >
        <ErdaIcon className="hover" type="up-down" size="14" fill="black-200" />
      </div>
      <div className="flex-1 ml-1 nowrap cursor-pointer flex items-center sort-drag-item-title">
        <Ellipsis title={data.title}>{data.title}</Ellipsis>
        {data.tags ? <TagsRow labels={data.tags} /> : null}
      </div>
      <div className="flex items-center">
        {map(data.operations || [], (op: Obj, key) => {
          if (key === 'switch') {
            return (
              <Switch
                size="small"
                className="mx-2"
                checked={!data.disabled}
                disabled={op.disabled}
                onChange={(v) => op.onClick(item)}
              />
            );
          }
          if (op.menus) {
            const menus = (
              <Menu>
                {op.menus.map((menuItem: Obj, idx: number) => {
                  return (
                    <Menu.Item
                      key={`${idx}`}
                      onClick={(e) => {
                        e?.domEvent?.stopPropagation();
                        if (menuItem.reload) {
                          menuItem.onClick(item);
                        }
                      }}
                    >
                      {menuItem.copyText ? (
                        <Copy copyText={menuItem.copyText}>{menuItem.text}</Copy>
                      ) : (
                        <span>{menuItem.text}</span>
                      )}
                    </Menu.Item>
                  );
                })}
              </Menu>
            );
            return (
              <Dropdown overlay={menus} getPopupContainer={(e) => e?.parentNode as HTMLElement}>
                <span
                  className={`icon-block hover-active px-2 ${op.hoverShow ? 'hover-show' : ''}`}
                  onClick={(e) => e.stopPropagation()}
                >
                  <CustomIcon type={op.icon} />
                </span>
              </Dropdown>
            );
          }
          if (op.confirm) {
            return op.disabled ? (
              <span
                className={`icon-block hover-active px-2 not-allowed ${op.hoverShow ? 'hover-show' : ''}`}
                onClick={(e: any) => e && e.stopPropagation()}
              >
                <CustomIcon
                  type={op.icon}
                  // onClick={(e:any) => {
                  //   e.stopPropagation();
                  //   op.onClick(item);
                  // }}
                />
              </span>
            ) : (
              <Popconfirm
                title={op.confirm}
                onConfirm={(e) => {
                  e && e.stopPropagation();
                  op.onClick(item);
                }}
                key={op.icon}
                onCancel={(e: any) => e && e.stopPropagation()}
              >
                <span
                  onClick={(e) => {
                    e.stopPropagation();
                  }}
                  className={`icon-block hover-active px-2 ${op.hoverShow ? 'hover-show' : ''}`}
                >
                  <CustomIcon type={op.icon} />
                </span>
              </Popconfirm>
            );
          }
          return (
            <Tooltip key={op.icon} title={op.hoverTip}>
              <span
                className={`icon-block hover-active px-2 ${op.hoverShow ? 'hover-show' : ''} ${
                  op.disabled ? 'not-allowed' : ''
                }`}
                onClick={(e) => {
                  e.stopPropagation();
                  !op.disabled && op.onClick(item);
                }}
              >
                <CustomIcon
                  type={op.icon}
                  // onClick={(e:any) => {
                  //   e.stopPropagation();
                  //   op.onClick(item);
                  // }}
                />
              </span>
            </Tooltip>
          );
        })}
      </div>
    </div>
  );
};

interface SortDragGroupProps<T> {
  index: number;
  group: { id: number; children: T[]; [k: string]: any };
  list: T[];
  disableDropInItem?: boolean;
  draggable?: boolean;
  groupDraggable?: boolean;
  disableDropInGroup?: boolean;
  onBeginDragItem?: (data: IBeginDragData<T>) => void;
  onEndDragItem?: (data: IEndDragData<T>) => void;
  onMoveItem?: (data: any) => void;
  onBeginDragGroup?: (data: IBeginDragData<SortItemData>) => void;
  onEndDragGroup?: (data: IEndDragData<SortItemData>) => void;
  onClickItem?: (data: SortItemData) => void;
}
const SortDragGroup = ({
  index,
  group,
  disableDropInItem,
  disableDropInGroup,
  onBeginDragItem,
  onEndDragItem,
  onMoveItem,
  onBeginDragGroup,
  onEndDragGroup,
  onClickItem = noop,
  draggable = true,
  groupDraggable = true,
}: SortDragGroupProps<SortItemData>) => {
  const { id: groupId, children: list } = group;
  const { drag, preview, drop, position, isDragging, isOver } = useDragAndDrop({
    item: { type: 'drag-group', data: group },
    index,
    onBeginDrag: onBeginDragGroup,
    onEndDrag: onEndDragGroup,
  });

  let hoverCls = '';
  if (!isDragging && isOver) {
    if (disableDropInGroup && position === 'center') {
      hoverCls = '';
    } else {
      hoverCls = `hover-position-tip hover-position-${position}`;
    }
  }
  return (
    <div
      ref={draggable ? (node) => drop(preview(node)) : undefined}
      key={groupId}
      className={`sort-drag-group ${hoverCls}`}
    >
      {groupDraggable === false ? null : (
        <div
          ref={draggable ? drag : undefined}
          className="group-drag-handle flex flex-wrap justify-center items-center"
        >
          <CustomIcon type="up-down" />
        </div>
      )}
      <div className="flex-1">
        {list.map((item, i) => {
          return (
            <SortDragItem
              key={`${item.data.groupId}_${item.data.id}`}
              index={i}
              item={item}
              draggable={draggable}
              disableDropIn={disableDropInItem}
              onMove={onMoveItem}
              onBeginDrag={(res) => onBeginDragItem({ ...res, size: list.length })}
              onEndDrag={onEndDragItem}
              onClick={onClickItem}
            />
          );
        })}
      </div>
    </div>
  );
};

const HOLDER_ID = -1;
const HOLDER_GROUP_ID = -1;
const getHolder = (type: string) => {
  return {
    type,
    data: {
      id: HOLDER_ID,
      groupId: HOLDER_GROUP_ID,
      title: '',
    },
    isHolder: true,
  };
};

interface SortItemData {
  type: string;
  draggable: boolean;
  data: {
    id: number;
    groupId: number;
    title: string;
    operations?: Obj<{
      icon: string;
      hoverShow?: boolean;
      hoverTip?: string;
      onClick: (item: SortItemData) => void;
    }>;
    [k: string]: any;
  };
  isHolder?: boolean;
}

interface SortGroupData {
  id: number;
  children: SortItemData[];
}

interface IProps {
  value: SortItemData[];
  disableDropInItem?: boolean;
  disableDropInGroup?: boolean;
  draggable?: boolean;
  groupDraggable?: boolean;
  onChange?: (data: any) => void;
  onMoveItem?: (data: any) => void;
  onMoveGroup?: (data: any) => void;
  onClickItem?: (data: SortItemData['data']) => void;
}
const noop = () => {};
const SortDragGroupList: React.FC<IProps> = ({
  value,
  disableDropInItem,
  disableDropInGroup,
  onMoveItem,
  onMoveGroup,
  onClickItem,
  draggable = true,
  groupDraggable = true,
}) => {
  const [list, setList] = React.useState(value);
  const [groupList, setGroupList] = React.useState([] as any[]);

  useUpdateEffect(() => {
    setList(value);
  }, [value]);

  React.useEffect(() => {
    const groupIdSort = uniq(map(list, 'data.groupId'));
    const groupMap = groupBy(list, (item) => item.data.groupId);
    const newList: any[] = [];
    map(groupIdSort, (groupId) => {
      newList.push({
        id: +groupId,
        children: groupMap[groupId],
      });
    });
    setGroupList(newList);
  }, [list]);

  const addHolder = () => {
    setList((prev) => (prev.find((a) => a.isHolder) ? prev : prev.concat(getHolder(list[0].type))));
  };

  const onBeginDragItem = ({ size }: IBeginDragData<SortItemData>) => {
    const holderItem = list.find((a) => a.isHolder);
    // 拖拽节点所在组只有拖拽节点时，所在组其实就是一个 holder，不用再添加
    if (!holderItem && size > 1) {
      addHolder();
    }
  };

  const onEndDragItem = ({ sourceItem, targetItem, position }: IEndDragData<SortItemData>) => {
    setList((prev) => {
      const newData = prev.filter((a) => !a.isHolder);
      // onChange && onChange(newData);
      return newData;
    });
    if (targetItem && onMoveItem && sourceItem.data.id !== targetItem.data.id) {
      onMoveItem({
        dragKey: sourceItem.data.id,
        dragGroupKey: sourceItem.data.groupId,
        dropKey: targetItem.data.id,
        dropGroupKey: targetItem.data.groupId,
        position,
      });
    }
  };

  const onEndDragGroup = ({ sourceItem, targetItem, position }: IEndDragData<SortGroupData>) => {
    // setList(prev => {
    //   const newData = prev.filter((a) => !a.isHolder);
    //   // onChange && onChange(newData);
    //   return newData;
    // });
    if (targetItem && onMoveGroup && sourceItem.id !== targetItem.id) {
      onMoveGroup({
        dragGroupKey: sourceItem.id,
        dropGroupKey: targetItem.id,
        position,
      });
    }
  };

  return (
    <>
      {map(groupList, (group, i) => (
        <SortDragGroup
          key={group.id}
          index={i}
          group={group}
          draggable={draggable}
          groupDraggable={groupDraggable}
          disableDropInItem={disableDropInItem}
          disableDropInGroup={disableDropInGroup}
          onBeginDragItem={onBeginDragItem}
          onEndDragItem={onEndDragItem}
          // onMoveItem={onMoveItem}
          onClickItem={onClickItem}
          onEndDragGroup={onEndDragGroup}
        />
      ))}
    </>
  );
};

export default SortDragGroupList;
