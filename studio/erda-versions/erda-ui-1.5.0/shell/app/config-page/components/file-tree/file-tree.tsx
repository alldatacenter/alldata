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
import { Tree, Popover, Popconfirm, Input, Tooltip } from 'antd';
import { map, noop, isEmpty, get, filter, isArray, uniq, compact, find, isEqual } from 'lodash';
import { useUpdateEffect } from 'react-use';
import { Icon as CustomIcon, EmptyHolder, Ellipsis, ErdaIcon } from 'common';
import { useUpdate } from 'common/use-hooks';
import { WithAuth } from 'user/common';
import { TreeNodeNormal, AntTreeNodeProps } from 'core/common/interface';
import i18n from 'i18n';
import './file-tree.scss';

interface IAction {
  node: React.ReactNode;
  func: (curKey: string | number, curNode: TreeNodeNormal) => void;
}

// 获取当前有查询key是的所有match key
const getAllMatchKeys = (_data: CP_FILE_TREE.INode[], searchKey?: string) => {
  let matchSearchKeys = [] as string[];
  const getMatchKeys = (p: CP_FILE_TREE.INode, pKeys: string[], isMatch = false) => {
    const { title, children } = p;
    const curKeys = [...pKeys, p.key];
    let curMatch = isMatch;
    if ((searchKey && (title as string).toLowerCase().indexOf(searchKey) > -1) || !searchKey) {
      // 记录所有match的key，以及他们的父
      matchSearchKeys = matchSearchKeys.concat(curKeys);
      curMatch = true;
    } else if (curMatch) {
      matchSearchKeys = matchSearchKeys.concat(p.key);
    }
    map(children, (cItem) => getMatchKeys(cItem, curKeys, curMatch));
  };
  map(_data, (item) => getMatchKeys(item, [item.key]));
  return uniq(matchSearchKeys);
};

const insertTemp = {
  _dataType: 'insertNode',
  selectable: false,
  isLeaf: true,
  disabled: true,
};
const getInsertData = (_data: any[]) => {
  const newData = [] as any[];
  _data.forEach((d) => {
    const { addBefore, addAfter } = d.operations || {};

    if (addBefore && addBefore.disabled !== true) {
      newData.push({ ...insertTemp, draggable: false, key: `${d.key}_before`, operations: { click: addBefore } });
    }
    newData.push({ ...d, __position: newData.length });
    if (addAfter && addAfter.disabled !== true) {
      newData.push({ ...insertTemp, draggable: false, key: `${d.key}_after`, operations: { click: addAfter } });
    }
  });
  return newData;
};

const emptyData = [] as any;
export const FileTree = (props: CP_FILE_TREE.Props) => {
  const {
    updateState,
    customOp,
    state: propsState,
    execOperation,
    operations,
    data: propsData,
    props: configProps,
  } = props;
  const { searchable = false, draggable = false, ...rest } = configProps || {};
  const data = propsData?.treeData || emptyData;
  const expandOpRef = React.useRef(null as any);

  // 重置数据：处理icon、title，以及根据searchKey过滤（此处为静态过滤）
  const handleData = (_data: CP_FILE_TREE.INode[], curSearchKey?: string) => {
    const staticSearch = operations?.search ? '' : curSearchKey; // 当有动态请求的时候，不用searchKey去过滤数据
    const matchKeys = staticSearch && getAllMatchKeys(_data, (staticSearch || '').toLowerCase());
    const _clickableNodes = {};
    const resetData = (d: CP_FILE_TREE.INode): any => {
      const { isLeaf, clickToExpand, icon, isColorIcon, children } = d;
      if (d.operations?.click && d._dataType !== insertTemp._dataType) {
        _clickableNodes[d.key] = { ...d.operations.click };
      }
      if (d._dataType === insertTemp._dataType) {
        const clickInsert = (e: any) => {
          e.stopPropagation();
          execOperation(d.operations?.click);
        };
        return {
          ...d,
          className: 'insert-node',
          title: <div className="cursor-pointer insert-node-title" onClick={clickInsert} />,
          icon: <ErdaIcon type="add-one" className="insert-node-icon cursor-pointer" onClick={clickInsert} />,
        };
      }
      if (staticSearch && !(matchKeys || []).includes(d.key)) {
        return null;
      }

      return {
        ...d,
        title:
          !isLeaf && clickToExpand ? (
            <div onClick={() => expandOpRef.current?.toggleExpand(d.key, d)}>
              <Ellipsis title={d.title} align={{ offset: [26, 0] }} placement="right">
                {d.title}
              </Ellipsis>
            </div>
          ) : d.operations?.click?.disabled ? (
            <Tooltip title={d.operations.click.disabledTip}>
              <span className="file-tree-disabled-node not-allowed w-full nowrap">{d.title}</span>
            </Tooltip>
          ) : (
            <div className="file-tree-title">
              <Ellipsis title={d.title} align={{ offset: [26, 0] }} placement="right">
                {d.title}
              </Ellipsis>
            </div>
          ),
        ...(icon
          ? {
              icon: (
                <CustomIcon
                  style={{ height: '16px' }}
                  type={icon as string}
                  color={isColorIcon}
                  className={`text-sub ${d.operations?.click?.disabled ? 'not-allowed' : ''}`}
                />
              ),
            }
          : {}),
        ...(children ? { children: compact(map(getInsertData(children), (cItem) => resetData(cItem))) } : {}),
      };
    };
    const _useData = compact(map(getInsertData(_data), (item) => resetData(item)));
    return {
      useData: _useData,
      clickableNodes: _clickableNodes,
    };
  };

  const handleState = (_stateObj?: Obj) => {
    return {
      // searchKey: _stateObj?.searchKey || undefined,
      expandedKeys: _stateObj?.expandedKeys || [],
      selectedKeys: _stateObj?.selectedKeys
        ? isArray(_stateObj.selectedKeys)
          ? _stateObj?.selectedKeys
          : [_stateObj?.selectedKeys]
        : undefined,
    };
  };

  const [state, updater, update] = useUpdate({
    ...handleState(propsState),
    ...handleData(data, propsState?.searchKey),
    searchKey: '',
  });

  const { expandedKeys, selectedKeys, searchKey, clickableNodes, useData } = state;

  useUpdateEffect(() => {
    update({
      ...handleState(propsState),
    });
    if (customOp?.onStateChange) {
      customOp.onStateChange(propsState);
    }
  }, [propsState, update]);

  useUpdateEffect(() => {
    update({
      ...handleData(data, searchKey),
    });
  }, [update, data, searchKey]);

  React.useEffect(() => {
    expandOpRef.current = {
      toggleExpand: (k: string, d: any) => {
        if (expandedKeys.includes(k)) {
          updater.expandedKeys((prev: string[]) => filter([...prev], (pItem) => pItem !== k));
        } else if (isEmpty(d.children) && d.operations?.click) {
          execOperation(d.operations?.click, { selectedKeys: state.selectedKeys, expandedKeys: state.expandedKeys });
        } else {
          updater.expandedKeys((prev: string[]) => [...prev, k]);
        }
      },
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [expandedKeys, updater]);

  const getActions = (node: TreeNodeNormal): IAction[] => {
    const { operations: nodeOperations } = node as CP_FILE_TREE.INode;
    const _actions = [] as any[];
    map(nodeOperations, (item) => {
      const { disabled, disabledTip, text, confirm, show } = item;
      if (show !== false) {
        if (disabled) {
          _actions.push({
            node: (
              <div>
                <WithAuth pass={false} noAuthTip={disabledTip}>
                  <span>{text}</span>
                </WithAuth>
              </div>
            ),
            func: noop,
          });
        } else if (confirm) {
          _actions.push({
            node: (
              <Popconfirm
                title={confirm}
                arrowPointAtCenter
                onConfirm={(e) => {
                  e && e.stopPropagation();
                  execOperation(item, { selectedKeys: state.selectedKeys, expandedKeys: state.expandedKeys });
                }}
                onCancel={(e) => e && e.stopPropagation()}
              >
                <div
                  onClick={(e) => {
                    e.stopPropagation();
                  }}
                >
                  {text}
                </div>
              </Popconfirm>
            ),
            func: noop,
          });
        } else {
          _actions.push({
            node: text,
            func: () => execOperation(item, { selectedKeys: state.selectedKeys, expandedKeys: state.expandedKeys }),
          });
        }
      }
    });
    return _actions;
  };

  const onExpand = (_expandedKeys: Array<string | number>, expandObj: any) => {
    const curKey = get(expandObj, 'node.props.eventKey');
    const curData = find(data, { key: curKey }) || ({} as Obj);
    const curDataChildren = get(curData, 'children');
    const shouldClickToExpand =
      (curData.operations?.expand && curData.operations?.expand.disabled !== true) ||
      (curData.clickToExpand && curData.operations?.click && curData.operations.click.disabled !== true);
    if (expandObj.expanded && shouldClickToExpand && (!curDataChildren || isEmpty(curDataChildren))) {
      execOperation(curData.operations?.expand || curData.operations?.click, {
        selectedKeys: state.selectedKeys,
        expandedKeys: state.expandedKeys,
      });
    } else {
      update({
        expandedKeys: _expandedKeys,
      });
    }
  };

  React.useEffect(() => {
    if (customOp?.onClickNode) {
      customOp.onClickNode(get(selectedKeys, '[0]'));
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedKeys]);

  const onClickNode = (_selectedKeys: React.ReactText[]) => {
    if (!configProps?.multiple && !_selectedKeys?.length) return;
    if (!isEqual(_selectedKeys, selectedKeys)) {
      const curClickableNode = get(clickableNodes, _selectedKeys[0]);
      // 节点上自带了click的operation，则执行自身的operation，否则执行默认的选中key操作
      if (curClickableNode) {
        !curClickableNode.disabled &&
          execOperation(curClickableNode, { selectedKeys: _selectedKeys, expandedKeys: state.expandedKeys });
      } else {
        updater.selectedKeys(_selectedKeys);
      }
    }
  };

  const onSearch = () => {
    if (operations?.search) {
      // 动态search：需要请求后端
      execOperation(operations?.search, state);
    } else {
      // 静态search
      update({
        ...handleData(data, searchKey),
      });
    }
  };

  const onChangeSearch = (e: any) => {
    updater.searchKey(e.target.value);
  };

  const onDrop = (info: {
    dragNode: AntTreeNodeProps;
    dropPosition: number;
    dropToGap: boolean;
    node: AntTreeNodeProps;
  }) => {
    const { dragNode, dropPosition, dropToGap } = info;
    let { node: dropNode } = info;
    // 落在自己、或落在叶子节点上无效
    if (dragNode.key === dropNode.key || (dropNode.isLeaf && !dropToGap)) return;
    if (operations?.drag) {
      let position = dropNode.__position === dropPosition ? 0 : dropNode.__position > dropPosition ? -1 : 1;

      // 落在展开且有子节点的枝干节点的首个位置时
      if (!dropToGap && dropNode.children && dropNode.children.length !== 0) {
        // 当自己就是该枝干节点的首个位置时不做操作
        if (dropNode.children[0].key === dragNode.key) {
          return;
        }

        dropNode = dropNode.children[0];
        position = -1;
      }

      execOperation(operations?.drag, {
        dragParams: {
          dragKey: dragNode.key,
          dragType: dragNode.type,
          dropType: dropNode.type,
          dropKey: dropNode.key,
          position,
        },
      });
    }
  };

  let dragProps = {};
  if (draggable) {
    dragProps = {
      onDrop,
    };
  }

  return (
    <div className="dice-cp file-tree h-full">
      {
        // 默认带上search
        searchable ? (
          <Input
            onChange={onChangeSearch}
            value={searchKey}
            placeholder={i18n.t('press enter to search')}
            onPressEnter={onSearch}
          />
        ) : null
      }
      <Tree
        {...rest}
        {...dragProps}
        draggable={draggable}
        treeData={useData}
        onExpand={onExpand}
        onSelect={onClickNode}
        className="file-tree-container"
        selectedKeys={selectedKeys}
        expandedKeys={expandedKeys}
        titleRender={(nodeData: TreeNodeNormal) => {
          const actions = getActions(nodeData);
          return (
            <span>
              {nodeData.title}
              {actions?.length ? (
                <Popover
                  content={actions.map((item) => (
                    <div className="action-btn" onClick={() => item.func?.(nodeData.key, nodeData)}>
                      {item.node}
                    </div>
                  ))}
                  footer={false}
                >
                  <ErdaIcon type="more1" className="tree-node-action" />
                </Popover>
              ) : null}
            </span>
          );
        }}
        blockNode
        showIcon
      />
      {isEmpty(useData) ? <EmptyHolder relative /> : null}
    </div>
  );
};
