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

import React, { useState, useRef, useEffect, useImperativeHandle } from 'react';
import { isEmpty, map, includes, max, filter, remove, reduce, uniqBy, set, get } from 'lodash';
import i18n from 'i18n';
import classnames from 'classnames';
import { useMount } from 'react-use';
import { Tree } from 'antd';
import { Icon as CustomIcon } from 'common';
import { updateSearch } from 'common/utils';
import routeInfoStore from 'core/stores/route';
import testCaseStore from 'project/stores/test-case';
import testSetStore from 'project/stores/test-set';
import projectStore from 'project/stores/project';
import {
  getNodeById,
  getNodeByPath,
  rootId,
  rootKey,
  recycledId,
  recycledKey,
  recycledRoot,
  getEventKeyId,
} from './utils';
import Title from './title';
import { TEMP_MARK, TestOperation, editModeEnum } from 'project/pages/test-manage/constants';
import './index.scss';

const { DirectoryTree, TreeNode } = Tree;

interface IActionItem {
  key: string;
  name: string;
  onclick: (prop: any) => void;
}
interface IProps {
  mode: TEST_CASE.PageScope;
  needRecycled?: boolean;
  needActiveKey?: boolean;
  readOnly?: boolean;
  needBreadcrumb?: boolean;
  testPlanID?: number;
  onSelect?: (info?: any) => void;
  testSetRef: React.Ref<any>;
  customActions?: IActionItem[];
}

interface IExpandTree {
  id: number;
  children: IExpandTree[];
}

interface ITree {
  list: TEST_SET.TestSetNode[];
  key: string;
  pKey: string;
  testSetID: number;
}

const mergeTree = (source: IExpandTree[], child: IExpandTree) => {
  const curItem = source.find((item) => item.id === child.id);
  if (!curItem) {
    source.push(child);
  } else if (curItem.children && child.children) {
    child.children.forEach((childData) => mergeTree(curItem.children, childData));
  }
};

const expandKeys2tree = (keys: string[]): IExpandTree[] => {
  const temp: IExpandTree[] = [];
  keys.forEach((expandedKey, index) => {
    const expandedKeyArr = expandedKey.split('-');
    expandedKeyArr.reverse();
    expandedKeyArr.forEach((id) => {
      const obj = {
        id: +id,
        children: temp[index] ? [temp[index]] : [],
      };
      temp[index] = obj;
    });
  });
  return temp;
};

const TestSet = ({
  needRecycled = false,
  needActiveKey = true,
  readOnly = false,
  needBreadcrumb = false,
  onSelect: oldOnSelect,
  mode,
  testPlanID,
  testSetRef,
  customActions = [],
}: IProps) => {
  const [hasExpand, setHasExpand] = useState(false);
  const [activeKey, setActiveKey] = useState(rootKey);
  const [copyOrClipKey, setCopyOrClipKey] = useState('');
  const [editMode, setEditMode] = useState('' as editModeEnum);
  const [expandedKeys, setExpandedKeys] = useState([] as string[]);
  const [treeData, setTreeData] = useState([] as TEST_SET.TestSetNode[]);
  const firstBuild = useRef(true);
  const query = routeInfoStore.useStore((s) => s.query);
  const [projectTestSet, modalTestSet, tempTestSet, reloadTestSetInfo, activeOuter] = testSetStore.useStore((s) => [
    s.projectTestSet,
    s.modalTestSet,
    s.tempTestSet,
    s.reloadTestSetInfo,
    s.activeOuter,
  ]);
  const { getProjectTestSets, getTestSetChildren, updateBreadcrumb, subSubmitTreeCollection } = testSetStore.effects;
  const { emptyReloadTestSet, clearActiveOuter } = testSetStore.reducers;
  const projectInfo = projectStore.useStore((s) => s.info);
  const { getCases } = testCaseStore.effects;
  const { triggerChoosenAll: resetChoosenAll } = testCaseStore.reducers;

  const testSet: { [k in TEST_CASE.PageScope]: any } = {
    testCase: projectTestSet,
    testPlan: projectTestSet,
    caseModal: modalTestSet,
    temp: tempTestSet,
  };
  const currentTestSet: TEST_SET.TestSet[] = testSet[mode];

  useMount(() => {
    getProjectTestSets({ testPlanID, mode, recycled: false, parentID: rootId, forceUpdate: true });
  });

  const loadTreeNode = (arr: string[], isInRecycleBin = false) => {
    arr.reduce(async (prev, curr) => {
      await prev;
      const o = fetchData(curr.split('-').reverse()[0], curr, isInRecycleBin);
      return o;
    }, Promise.resolve());
  };

  const addNodeFromOuter = (data: TEST_SET.TestSet) => {
    const newNode: TEST_SET.TestSetNode = {
      title: data.name,
      key: `${rootKey}-${data.id}`,
      recycled: false,
      ...data,
    };
    const parent = getNodeByPath({ treeData, eventKey: rootKey });
    parent.children = [newNode, ...(parent.children || [])];
    setTreeData([...treeData]);
  };
  const reloadLoadData = (id: number, eventKey: string, recycled: boolean) => {
    if (+id === 0) {
      getProjectTestSets({ testPlanID, mode, recycled: false, parentID: rootId, forceUpdate: true });
    } else {
      fetchData(id, eventKey, recycled);
    }
  };

  const updateTree = (tree: IExpandTree[], data: ITree[], newTreeData: TEST_SET.TestSetNode[]) => {
    tree.forEach(({ id, children }) => {
      const newTree = data.find((item) => item.testSetID === id);
      if (!isEmpty(newTree)) {
        const { key, list } = newTree as ITree;
        if (key === rootKey) {
          set(newTreeData, ['0', 'children'], list);
        } else {
          const parent = getNodeByPath({ treeData: newTreeData, eventKey: key });
          if (!isEmpty(parent)) {
            set(parent, 'children', list);
          }
        }
      }
      if (children.length) {
        updateTree(children, data, newTreeData);
      }
    });
  };

  const [expandTree, expandIds] = React.useMemo<
    [IExpandTree[], Array<{ id: number; key: string; pKey: string }>]
  >(() => {
    const result: IExpandTree[] = [];
    // 展开节点ID，需去重，防止一个节点请求多次
    const temp: Array<{ id: number; key: string; pKey: string }> = [];
    // 最深层级路径
    const deepestPath: string[] = [];
    const keys = [...expandedKeys];
    // 将expandedKeys倒序
    keys.sort((a, b) => b.split('-').length - a.split('-').length);
    // 获取不重复的最深路径
    keys.forEach((expandedKey) => {
      const flag = deepestPath.some((s) => s.includes(expandedKey));
      if (!flag) {
        deepestPath.push(expandedKey);
      }
    });
    deepestPath.forEach((str) => {
      const idArr = str.split('-').map((id) => +id);
      const keyTemp: number[] = [];
      idArr.forEach((id) => {
        const pKey = keyTemp.join('-');
        keyTemp.push(id);
        const key = keyTemp.join('-');
        temp.push({
          id,
          pKey,
          key,
        });
      });
    });
    // 最深层级转换为tree
    const tree = expandKeys2tree(deepestPath);
    // 合并tree
    tree.forEach((child) => {
      mergeTree(result, child);
    });
    return [result, uniqBy(temp, 'id')];
  }, [expandedKeys]);

  const removeMenu = (ids: number[]) => {
    if (ids.length === 0) {
      getCases();
    } else {
      let newActiveKey = rootKey;
      let tempIndex = 0;
      const promiseArr: Array<
        PromiseLike<{
          testSetID: number;
          key: string;
          pKey: string;
          list: TEST_SET.TestSet[];
        }>
      > = [];
      expandIds.forEach(({ id, key, pKey }) => {
        promiseArr.push(
          getTestSetChildren({ testPlanID, recycled: false, parentID: id, mode }).then((res) => ({
            testSetID: id,
            key,
            pKey,
            list: res || [],
          })),
        );
      });
      // 请求所有展开的节点
      Promise.all(promiseArr).then((data) => {
        const activeKeys = activeKey.split('-');
        const trees: ITree[] = [];
        data.forEach(({ list, key, pKey, testSetID }) => {
          const targetIndex = activeKeys.findIndex((t) => t === `${testSetID}`);
          if (targetIndex !== -1 && !!list.length) {
            tempIndex = Math.max(targetIndex, tempIndex);
          }
          trees.push({
            testSetID,
            pKey,
            key,
            list: (list || []).map(({ id: childId, name, recycled, parentID }) => ({
              id: childId,
              title: name,
              key: `${key}-${childId}`,
              isLeaf: false,
              recycled,
              parentID,
              children: [],
            })),
          });
        });
        const newTree = [...treeData];
        // 递归更新展开的节点
        updateTree(expandTree, trees, newTree);
        setTreeData(newTree);
        newActiveKey = activeKeys.slice(0, tempIndex + 1).join('-');
        setActiveKey(newActiveKey);
        onSelect([newActiveKey]);
      });
    }
  };

  useImperativeHandle(testSetRef, () => ({
    addNodeFromOuter,
    reloadLoadData,
    removeMenu,
  }));

  useEffect(() => {
    const expandId = (query.eventKey || '').split('-');
    if (firstBuild.current && !isEmpty(treeData)) {
      if (!(query.caseId || expandId.length > 1)) {
        // 当 query 不为空的时候，就保持当前的 query 值
        onSelect([rootKey], { keepCurrentSearch: !isEmpty(query) });
      }
      // onSelect([rootKey]);
      firstBuild.current = false;
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [treeData, query.caseId, query.eventKey]);

  useEffect(() => {
    if (activeOuter) {
      onAddNode(rootKey);
      clearActiveOuter();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeOuter]);

  useEffect(() => {
    const normalNodes: TEST_SET.TestSetNode[] = currentTestSet.map(({ id, name, recycled, parentID }: any) => ({
      title: name,
      key: `${rootKey}-${id}`,
      id,
      recycled,
      parentID,
      isLeaf: false,
    }));
    const nextActiveKey = firstBuild.current && query.eventKey && needActiveKey ? query.eventKey : rootKey;
    setActiveKey(nextActiveKey);
    expandedKeys.length === 0 && setExpandedKeys([rootKey]);
    setTreeData([
      {
        title: projectInfo.name,
        key: rootKey,
        id: rootId,
        iconType: 'project',
        iconClass: 'text-blue',
        parentID: rootId,
        isLeaf: false,
        recycled: false,
        children: needRecycled ? normalNodes.concat([{ ...recycledRoot }]) : normalNodes,
      },
    ]);

    if (needBreadcrumb) {
      updateBreadcrumb({
        pathName: projectInfo.name,
        testSetID: rootId,
        testPlanID,
      });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentTestSet, projectInfo]);
  useEffect(() => {
    const expandId: string[] = (query.eventKey || '').split('-');
    // 是否为回收站内的case
    const isInRecycleBin = expandId.includes('recycled');
    // 测试计划测试用例需要打开分享链接
    if (!['testCase', 'testPlan'].includes(mode)) {
      return;
    }
    const hasCaseId = hasExpand ? query.caseId : query.caseId || expandId.length > 1;
    if (hasCaseId && treeData[0] && treeData[0].children) {
      // 第二级节点key值
      const secondLevelKey = expandId.slice(0, 2).join('-');
      // 所有展开节点的key值
      const eventKeys: string[] = expandId.reduce((previousValue, _currentValue, index, arr) => {
        const last = [...arr].splice(0, index + 1);
        return [...previousValue, last.join('-')];
      }, [] as string[]);
      getCases({ testSetID: +query.testSetID, pageNo: 1, testPlanID, recycled: isInRecycleBin, scope: mode });
      setExpandedKeys(eventKeys || [rootKey]);
      setActiveKey(query.eventKey);
      const secondLevel = treeData[0].children.find((t) => t.key === secondLevelKey) || {};
      const firstChildren = get(secondLevel, 'children');
      if (!isEmpty(secondLevel) && !firstChildren) {
        // 逐级请求节点
        loadTreeNode(eventKeys.splice(1), isInRecycleBin);
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [treeData, query.caseId, query.eventKey, mode, query.testSetID, getCases, testPlanID]);

  // 复制/移动/引入测试集
  useEffect(() => {
    if (!isEmpty(reloadTestSetInfo)) {
      const { isMove, reloadParent, parentID, testSetID } = reloadTestSetInfo;
      const currentNode =
        getNodeById({ treeData, id: testSetID as number, recycled: false }) || ({} as TEST_SET.TestSetNode);
      let parentNode = null;
      if (reloadParent && currentNode) {
        // 当前节点的父级是否存在
        parentNode = getNodeById({ treeData, id: currentNode.parentID, recycled: false });
      } else if (parentID || parentID === 0) {
        // 传入的父级是否存在
        parentNode = getNodeById({ treeData, id: parentID, recycled: false });
      }
      if (reloadParent && !parentNode) {
        // 那就根节点, 因为添加层级较深的测试集节点时parentId存在但parentNode不存在，见#139377
        parentNode = getNodeById({ treeData, id: 0, recycled: false });
      }
      if (parentNode) {
        // 目标节点存在，则更新
        fetchData(parentNode.id, parentNode.key, parentNode.recycled);
        emptyReloadTestSet();
      }
      if (isMove && currentNode) {
        // 移动后,需要清空发起节点
        onRemoveNode(currentNode.key);
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [reloadTestSetInfo]);

  const renderTreeNodes = (data: any[]) =>
    data.map((item) => {
      const isRootNode = item.key === rootKey;
      const titleProps = {
        name: item.title,
        readOnly,
        id: item.id,
        editMode,
        className: isRootNode ? 'root-tree-node' : '',
        recycled: item.recycled,
        eventKey: item.key,
        copyOrClipKey,
        customActions,
        onOperateNode,
        onUpdateNode,
        onRemoveNode,
      };
      const icon = (
        <CustomIcon
          type={item.iconType || 'wjj1'}
          className={item.iconClass || (!isRootNode && item.recycled ? 'text-danger' : 'text-yellow')}
        />
      );
      const className = classnames({
        active: activeKey === item.key,
        copy: copyOrClipKey === item.key && editMode === 'copy',
        clip: copyOrClipKey === item.key && editMode === 'clip',
      });
      if (!isEmpty(item.children)) {
        return (
          <TreeNode key={item.key} className={className} icon={icon} title={<Title {...titleProps} />} dataRef={item}>
            {renderTreeNodes(item.children)}
          </TreeNode>
        );
      }
      return <TreeNode {...item} title={<Title {...titleProps} />} className={className} icon={icon} dataRef={item} />;
    });

  const getChildrenById = (id: number, parentKey: string, isRecycled = false) => {
    return getTestSetChildren({
      testPlanID,
      recycled: isRecycled,
      parentID: id,
      mode,
    }).then((children) =>
      (children || []).map(({ id: childId, name, recycled, parentID }) => ({
        id: childId,
        title: name,
        key: `${parentKey}-${childId}`,
        isLeaf: false,
        recycled,
        parentID,
        children: [],
      })),
    );
  };

  // 加载数据
  const fetchData = (id: number | string, eventKey: string, recycled = false) => {
    const isRoot = id === rootId;
    const newId = id === recycledId ? 0 : (id as number);
    return getChildrenById(newId, eventKey, recycled).then((pureChildren: any) => {
      const children = map(pureChildren, (single) => {
        // 依旧保留原有子级
        const existNode = getNodeByPath({ treeData, eventKey: single.key });
        if (existNode) {
          return {
            ...single,
            parentID: newId === 0 && recycled ? recycledId : single.parentID,
            children: existNode.children,
          };
        } else {
          return { ...single, parentID: newId === 0 && recycled ? recycledId : single.parentID };
        }
      });
      if (isRoot) {
        const recycledNode = getNodeByPath({ treeData, eventKey: recycledKey });
        if (recycledNode) {
          treeData[0].children = [...children, recycledNode];
        } else {
          treeData[0].children = children;
        }
      } else {
        const current = getNodeByPath({ treeData, eventKey });
        current.children = children;
        current.isLeaf = !children.length;
      }
      setTreeData([...treeData]);
    });
  };

  const loadData = (treeNode: any) => {
    const { id, key, recycled } = treeNode.props.dataRef;
    if (includes(id, TEMP_MARK)) {
      return Promise.resolve();
    }
    return fetchData(id, key, recycled);
  };

  // 新建测试集
  const onAddNode = (eventKey: string) => {
    const parent = getNodeByPath({ treeData, eventKey });
    const newId: number =
      (max(
        map(
          filter(parent.children, ({ id }) => includes(id, TEMP_MARK)),
          ({ id }) => parseInt(id.substring(3, id.length), 10),
        ),
      ) as number) || 0;
    const idPrefix = `${TEMP_MARK}${newId + 1}`;
    const tempNode = {
      title: i18n.t('dop:new test set'),
      id: idPrefix,
      key: `${parent.key}-${idPrefix}`,
      recycled: false,
      parentID: parent.id,
      isLeaf: false,
    };
    if (includes(expandedKeys, eventKey)) {
      // 展开过
      parent.children = [tempNode, ...(parent.children || [])];
      setTreeData([...treeData]);
      return;
    }
    getChildrenById(parent.id, parent.key).then((children: any) => {
      parent.children = [tempNode, ...(children || [])];
      setExpandedKeys([eventKey, ...expandedKeys]);
    });
  };

  const onOperateNode = (eventKey: string, action: string, data?: Record<string, any>) => {
    switch (action) {
      case TestOperation.add:
        onAddNode(eventKey);
        break;
      case TestOperation.copy:
        setCopyOrClipKey(eventKey);
        setEditMode(TestOperation.copy);
        break;
      case TestOperation.clip:
        setCopyOrClipKey(eventKey);
        setEditMode(TestOperation.clip);
        break;
      case TestOperation.paste:
        subSubmitTreeCollection({
          parentID: getEventKeyId(eventKey),
          action: editMode,
          testSetID: getEventKeyId(copyOrClipKey),
        });
        setCopyOrClipKey('');
        setEditMode('');
        break;
      case TestOperation.delete:
        onMoveToRecycled(eventKey);
        break;
      case TestOperation.recover:
        onRecoverFromRecycled(eventKey, data as TEST_SET.RecoverQuery);
        break;
      case TestOperation.deleteEntirely:
        onRemoveNode(eventKey);
        break;
      default:
        break;
    }
  };

  // 移除测试集
  const onRemoveNode = (eventKey: string) => {
    const id = eventKey.split('-').reverse()[0];
    const parent = getNodeByPath({ treeData, eventKey: eventKey.replace(`-${id}`, '') });
    remove(parent.children, ({ key }) => key === eventKey);
    parent.children = [...parent.children];
    setTreeData([...treeData]);
    if (parent) {
      // 节点移动/还原/删除/彻底删除时，选中父级节点，以解决面包屑/用例列表的更新问题
      onSelect([parent.key]);
    }
  };

  // 更新测试集信息，比如新建测试集后
  const onUpdateNode = (eventKey: string, newId: number, newName: string) => {
    const id = eventKey.split('-').reverse()[0];
    const current = getNodeByPath({ treeData, eventKey });
    current.id = newId;
    current.key = eventKey.replace(`-${id}`, `-${newId}`);
    current.title = newName;
    current.recycled = false;
    setTreeData([...treeData]);
  };

  // 移动到回收站
  const onMoveToRecycled = (eventKey: string) => {
    fetchData(recycledId, recycledKey, true);
    remove(expandedKeys, (key) => key === eventKey);
    onRemoveNode(eventKey);
  };

  // 从回收站还原
  const onRecoverFromRecycled = (eventKey: string, { recoverToTestSetID }: TEST_SET.RecoverQuery) => {
    // 获取恢复至的节点
    const targetNode = getNodeById({ treeData, id: recoverToTestSetID, recycled: false }) as TEST_SET.TestSetNode;
    if (isEmpty(targetNode) || !includes(expandedKeys, targetNode.key)) {
      // 如果父级没有展示出来，或者没有展开过，那么此时无需更新父级节点
      onRemoveNode(eventKey);
      return;
    }
    // 还原后前端自动插入改为请求后端数据
    fetchData(recoverToTestSetID, targetNode.key, false);
    onRemoveNode(eventKey);
  };

  const onExpand = (nextExpandedKeys: string[], { nativeEvent }: any) => {
    setHasExpand(true);
    let eventPath = nativeEvent.path;
    // In Firefox, MouseEvent hasn't path and needs to be hacked, bubbling from the target to the root node
    if (!eventPath) {
      eventPath = [];
      let currentEle = nativeEvent.target;
      while (currentEle) {
        eventPath.push(currentEle);
        currentEle = currentEle.parentElement;
      }
      if (!(eventPath.includes(window) || eventPath.includes(document))) {
        eventPath.push(document);
      }
      if (!eventPath.includes(window)) {
        eventPath.push(window);
      }
    }

    nativeEvent.stopPropagation();
    remove(nextExpandedKeys, (key) => includes(key, TEMP_MARK));
    setExpandedKeys(nextExpandedKeys);
  };

  const onSelect = (selectedKeys: string[], _extra?: any) => {
    // extra && extra.nativeEvent.stopPropagation();
    const eventKey = selectedKeys[selectedKeys.length - 1];

    const isRecycledNode = eventKey === recycledKey;
    const current = getNodeByPath({ treeData, eventKey }) as TEST_SET.TestSetNode;
    const recycled = current && (current.recycled as boolean);
    const testSetID = (isRecycledNode ? 0 : current && current.id) || rootId;
    if (oldOnSelect) {
      // 复制、移动时，弹框的选中
      if (eventKey) {
        oldOnSelect({ testSetID, parentID: current.parentID, recycled });
      } else {
        oldOnSelect();
      }
    }
    if (eventKey) {
      setActiveKey(eventKey);
    }
    resetChoosenAll({ isAll: false, scope: mode });
    // 1、取消选中时 2、无需面包屑时 3、点击新建的测试集时
    if (!eventKey || !needBreadcrumb || includes(eventKey, TEMP_MARK)) {
      return;
    }
    const list = eventKey.split('-');
    let pathName = '';
    reduce(
      list,
      (oldKey: string, tempKey: string) => {
        const newKey = oldKey ? `${oldKey}-${tempKey}` : tempKey;
        const currentTitle = getNodeByPath({ treeData, eventKey: newKey, valueKey: 'title' });
        pathName += pathName ? `/${currentTitle}` : currentTitle;
        return newKey;
      },
      '',
    );
    // 新建case时用到
    updateBreadcrumb({
      pathName,
      testSetID,
      testPlanID,
    });

    // 页面刚刚进来时保持当前 query 不进行更新
    if (!_extra?.keepCurrentSearch) {
      updateSearch({ pageNo: 1, recycled, testSetID, eventKey });
    }

    getCases({ testSetID, pageNo: 1, testPlanID, recycled, scope: mode });
  };

  return (
    <DirectoryTree
      className="case-tree"
      blockNode
      loadData={loadData}
      onExpand={onExpand}
      expandedKeys={expandedKeys}
      onSelect={onSelect}
      loadedKeys={expandedKeys}
      selectedKeys={[activeKey]}
    >
      {renderTreeNodes(treeData)}
    </DirectoryTree>
  );
};

export default React.forwardRef((props: Omit<IProps, 'testSetRef'>, ref) => {
  return <TestSet {...props} testSetRef={ref} />;
});
