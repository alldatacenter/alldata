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
import { useMount, useLatest } from 'react-use';
import { map, set, find, cloneDeep, noop, findIndex, get, reduce, forEach } from 'lodash';
import { Spin, Tree, Popover, Select } from 'antd';
import i18n from 'i18n';
import { Icon as CustomIcon, Title } from 'common';
import { useUpdate } from 'common/use-hooks';
import { TreeProps, AntTreeNodeProps, TreeNodeNormal } from 'core/common/interface';
import { EditCategory } from './edit-category';
import { findTargetNode, getIcon, isAncestor, walkTree } from './utils';
import { WithAuth } from 'user/common';

import './tree.scss';

const { Option, OptGroup } = Select;

interface IAction {
  node: React.ReactNode;
  func: (curKey: string | number, curNode: TreeNodeNormal) => void;
}

export interface TreeNode extends Omit<TreeNodeNormal, 'title'> {
  icon?: React.ReactElement;
  disableAction?: boolean;
  children?: TreeNode[];
  title?: string | React.ReactNode;
  parentKey?: string;
  type?: string;
  className?: string;
  titleAlias: string;
  originData?: TREE.NODE;
  iconType?: string;
}

interface TreeAction {
  preset?: string;
  node: string;
  func?: (key: string, node: TreeNodeNormal, hookFn?: (nodeKey: string, isCreate: boolean) => void) => void;
}

interface IProps {
  title?: string;
  titleOperation?: Array<{ preset?: string; title?: string }>;
  treeProps?: TreeProps;
  initTreeData: TreeNode[];
  currentKey?: string;
  actions?: {
    folderActions?: TreeAction[] | ((node: TreeNode) => TreeAction[]);
    fileActions?: TreeAction[] | ((node: TreeNode) => TreeAction[]);
  };
  iconMap?: { [p: string]: JSX.Element };
  searchGroup?: { file: string; folder: string };
  loading?: boolean;
  effects: {
    loadData: (params: { pinode: string }) => Promise<TreeNode[]>;
    getAncestors: (params: { inode: string }) => Promise<TreeNode[]>;
    moveNode?: (params: { inode: string; pinode: string; isLeaf?: boolean }) => Promise<void>;
    copyNode?: (params: { inode: string; pinode: string; isLeaf?: boolean }) => Promise<void>;
    createNode?: (params: TREE.CreateNodeParams) => Promise<void>;
    updateNode?: (params: TREE.UpdateNodeParams) => Promise<TreeNode>;
    deleteNode?: (params: { inode: string }, isCurrentKeyDeleted?: boolean) => Promise<void>;
    fuzzySearch?: (params: { fuzzy: string }) => Promise<TreeNode[]>;
  };
  customNode?: (params: TreeNode) => JSX.Element | string;
  customNodeClassName?: string | ((params: TreeNode) => string);
  onSelectNode: (params: { inode: string; isLeaf: boolean }) => void;
}

const RENAME_FOLDER = 'renameFolder';
const NEW_FOLDER = 'newFolder';
const DELETE = 'delete';
const CUT = 'cut';
const PASTE = 'paste';
const COPY = 'copy';
const EDIT_KEY = 'editing-node';
const NEW_FILE = 'newFile';
const RENAME_FILE = 'renameFile';
const CUSTOM_EDIT = 'customEdit';

const newFolderOperation = (effect: ({ name }: { name: string }) => void) => ({
  title: <EditCategory onSubmit={effect} />,
});

/**
 * 通用Tree组件
 * @title 当需要展示树外的标题时传入
 * @titleOperation 当不传title时无效，用于用户自定义大标题右侧的操作，如新建等，支持预设类型：新建一级目录
 * @initTreeData 树的初始节点数组，长度为1，只有设置了root节点，树才能展开
 * @currentKey 受控组件，在被应用的页面，由用户自行存储这个key（url或state），用来标识当前selectedKey
 * @treeProps 原生nusi tree的props，用户可自行传入任意属性来覆盖预设
 * @actions 对象，可包含folderActions和fileActions，用户可以使用预设action，也可以跟原生一样定义action
 * @iconMap icon映射，可通过type属性从iconMap取图标，实现自定义图标和一棵树中多种图标
 * @loading 是否菊花
 * @onSelectNode click节点的回调
 * @effects 传递所有操作所需的effect
 * @customNode 用户可以自己定义节点的DOM结构
 * @customNodeClassName 配合customNode使用，用来作用到节点容器的className
 *
 * @usage
   ```
   const folderActions = [
    {
      node: '新建子测试集',
      preset: 'newFolder',
    },
    {
      node: '新建用例',
      preset: 'customEdit',
      func: async (nodeKey: string, _node: TreeNode, hook?: (nodeKey: string, isCreate: boolean) => void) => {
        showFormModal(nodeKey, hook) // 用户自定义， 开启弹窗
        // 在onSubmit中，有以下逻辑
        // await createTreeNode({ pinode: nodeKey, name: nodeKey, type: 'f' }); // 自定义
        // hook && hook(nodeKey, isCreate); // isCreate决定重刷的父节点取值
      },
    },
    {
      node: '重命名',
      preset: 'renameFolder',
    },
    {
      node: '复制',
      preset: 'copy',
    },
    {
      node: '剪切',
      preset: 'cut',
    },
    {
      node: '粘贴',
      preset: 'paste',
    },
    {
      node: '删除',
      preset: 'delete',
    },
    {
      node: '添加到测试计划',
      func: () => {},
    },
  ];

  const fileActions = [
    {
      node: '编辑用例',
      preset: 'customEdit',
      func: async (nodeKey: string, node: TreeNode, hook?: (nodeKey: string, isCreate: boolean) => void) => {
        showFormModal(nodeKey,node, hook) // 用户自定义， 开启弹窗。 node相当于带了formData， 后面的内容同创建
      },
    },
    {
      node: '复制',
      preset: 'copy',
    },
    {
      node: '剪切',
      preset: 'cut',
    },
    {
      node: '删除',
      preset: 'delete',
    },
  ];

  const titleOperations = [
    {
      preset: 'newFolder',
    },
  ];

  const onSelectNode = ({ inode, isLeaf }: { inode: string, isLeaf: boolean }) => {
  };

  <TreeCategory
    title={i18n.t('common:interface test set')}
    titleOperation={titleOperations}
    onSelectNode={onSelectNode}
    initTreeData={[{ ...rootNode, disableAction: true }]}
    // currentKey={`leaf-${workflowId}`}
    effects={{
      moveNode: moveTreeNode,
      loadData: getCategoryById,
      deleteNode: deleteTreeNode,
      updateNode: updateTreeNode,
      createNode: createTreeNode,
      copyNode: copyTreeNode,
    }}
    actions={{
      folderActions,
      fileActions,
    }}
  />
  ```
 * @description
 * 支持的预设操作包括：
 * 文件夹 => 新建newFolder/重命名renameFolder/剪切cut/复制copy/粘贴paste/删除delete
 * 文件 => 剪切cut/复制copy/粘贴paste/删除delete
 * 自定义
 * 树外标题预设操作：新建文件夹 newFolder
 * 用户可以在预设preset与自定义间自由切换
 * 使用preset的前提是所有传入的effect要遵守最新的后端接口规范 https://yuque.antfin-inc.com/terminus_paas_dev/ed525g/yix4p8#8VQdd
 * TODO: 树的check框批量操作
 */
const TreeCategory = ({
  title,
  titleOperation,
  initTreeData = [],
  treeProps,
  currentKey,
  actions,
  iconMap,
  loading,
  effects,
  searchGroup,
  onSelectNode = noop,
  customNode,
  customNodeClassName,
}: IProps) => {
  const { loadData, getAncestors, copyNode, moveNode, createNode, updateNode, deleteNode, fuzzySearch } = effects;

  const [{ treeData, expandedKeys, isEditing, rootKey, cuttingNodeKey, copyingNodeKey, filterOptions }, updater] =
    useUpdate({
      treeData: map(initTreeData || [], (data) => ({
        ...data,
        icon: getIcon(data, iconMap),
        optionProps: {
          popover: {
            trigger: 'hover',
            getPopupContainer: () => document.body,
            onVisibleChange: (visible: boolean) =>
              onPopMenuVisibleChange(data.isLeaf ? `leaf-${data.key}` : data.key, visible),
          },
        },
      })) as TreeNode[],
      expandedKeys: [] as string[],
      isEditing: false,
      rootKey: get(initTreeData, '0.key') || '0',
      cuttingNodeKey: null as null | string,
      copyingNodeKey: null as null | string,
      filterOptions: { folderGroup: [], fileGroup: [] } as {
        folderGroup: Array<{ label: string; value: string }>;
        fileGroup: Array<{ label: string; value: string }>;
      },
    });

  const latestTreeData = useLatest(treeData);

  const getClassName = (node: TreeNode) => {
    const { isLeaf } = node;
    return isLeaf ? (typeof customNodeClassName === 'function' ? customNodeClassName(node) : customNodeClassName) : '';
  };

  const onPopMenuVisibleChange = (key: string, visible: boolean) => {
    const dataCp = cloneDeep(latestTreeData.current);
    const node = findTargetNode(key, dataCp);
    set(node!, ['optionProps', 'popover', 'visible'], visible);
    updater.treeData(dataCp);
  };

  // 更新一个父节点的儿子节点
  const appendChildren = (parentKey: string, children: TreeNode[]) => {
    const dataCp = cloneDeep(latestTreeData.current);
    const parentNode = findTargetNode(parentKey, dataCp);
    if (parentNode) {
      set(
        parentNode,
        'children',
        map(children, (child) => {
          const { isLeaf, key, title: cTitle, ...rest } = child;
          let originChild = null; // 如果能找到此儿子本身存在，那么此儿子的儿子要保留
          if (parentNode.children && findIndex(parentNode.children, { key }) > -1) {
            originChild = find(parentNode.children, { key });
          }
          return {
            ...rest,
            title: customNode ? customNode(child) : cTitle,
            titleAlias: typeof cTitle === 'string' ? cTitle : undefined,
            isLeaf,
            key: isLeaf ? `leaf-${key}` : key,
            icon: getIcon({ isLeaf, type: rest.type, iconType: rest.iconType }, iconMap),
            parentKey,
            disabled: isEditing,
            children: originChild?.children,
            className: getClassName(child),
            optionProps: {
              popover: {
                trigger: 'hover',
                getPopupContainer: () => document.body,
                onVisibleChange: (visible: boolean) => onPopMenuVisibleChange(isLeaf ? `leaf-${key}` : key, visible),
              },
            },
          };
        }),
      );
      updater.treeData(dataCp);
    }
  };

  // 展开节点，调用接口
  const onLoadTreeData = async (nodeKey: string) => {
    const categories = await loadData({ pinode: nodeKey });
    appendChildren(nodeKey, categories);
  };

  useMount(async () => {
    if (currentKey) {
      // 刷新页面或首次加载时有具体文件id存在，需要自动展开树
      let key = currentKey;
      if (currentKey.startsWith('leaf-')) {
        key = currentKey.slice(5);
      }
      const ancestors = await getAncestors({ inode: key });
      const keys = map(ancestors, 'key');
      updater.expandedKeys(keys);
    } else if (initTreeData && initTreeData.length === 1) {
      // 否则自动展开第一层
      const categories = await loadData({ pinode: rootKey });
      appendChildren(rootKey, categories);
      updater.expandedKeys([rootKey]);
    }
  });

  const onExpand = (keys: string[]) => {
    updater.expandedKeys(keys);
  };

  const onClickNode = (keys: string[], info: { node: { props: AntTreeNodeProps } }) => {
    const isLeaf = !!info.node.props.isLeaf;
    onSelectNode({ inode: isLeaf ? keys[0].slice(5) : keys[0], isLeaf });
  };

  React.useEffect(() => {
    updater.treeData((oldTreeData: TreeNode[]) => {
      const dataCp = cloneDeep(oldTreeData);
      walkTree(dataCp, { disabled: isEditing }, EDIT_KEY);
      return dataCp;
    });
  }, [isEditing, updater]);

  function createTreeNode(nodeKey: string, isCreateFolder: boolean) {
    const dataCp = cloneDeep(treeData);
    const currentNode = findTargetNode(nodeKey, dataCp)!;
    updater.isEditing(true);
    const onEditFolder = async ({ name }: { name: string }) => {
      await createNode!({ name, pinode: nodeKey, type: isCreateFolder ? 'd' : 'f' });
      onLoadTreeData(nodeKey);
      onHide();
      updater.isEditing(false);
    };
    const onHide = () => {
      onLoadTreeData(nodeKey);
      updater.isEditing(false);
    };
    currentNode.children = [
      {
        key: EDIT_KEY,
        titleAlias: EDIT_KEY,
        parentKey: nodeKey,
        isLeaf: !isCreateFolder,
        icon: getIcon({ isLeaf: !isCreateFolder }, iconMap),
        disableAction: true,
        title: <EditCategory contentOnly onSubmit={onEditFolder} onHide={onHide} />,
      },
      ...(currentNode.children || []),
    ];
    !expandedKeys.includes(nodeKey) && updater.expandedKeys(expandedKeys.concat(nodeKey));
    updater.treeData(dataCp);
  }

  // 添加子文件夹（提交在组件内）
  const renameTreeNode = (nodeKey: string, isRenameFolder: boolean) => {
    const dataCp = cloneDeep(treeData);
    const currentNode = findTargetNode(nodeKey, dataCp)!;
    updater.isEditing(true);
    const restoreNode = () => {
      currentNode.key = nodeKey; // 利用闭包恢复key
      currentNode.disableAction = false;
      updater.treeData(dataCp);
      updater.isEditing(false);
    };
    const onEditFolder = async ({ name }: { name: string }) => {
      const updatedNode = await updateNode!({ name, inode: isRenameFolder ? nodeKey : nodeKey.slice(5) });
      currentNode.title = customNode ? customNode(updatedNode) : updatedNode.title;
      currentNode.titleAlias = name;
      restoreNode();
    };
    const onHide = () => {
      currentNode.title = currentNode.titleAlias; // 恢复被edit组件替代了的name
      restoreNode();
    };
    currentNode.key = EDIT_KEY;
    currentNode.disableAction = true;
    currentNode.title = (
      <EditCategory contentOnly defaultName={currentNode.title as string} onSubmit={onEditFolder} onHide={onHide} />
    );
    updater.treeData(dataCp);
  };

  const handleMoveNode = async (nodeKey: string, parentKey: string) => {
    const targetNode = findTargetNode(nodeKey, treeData)!;
    await moveNode!({
      inode: targetNode.isLeaf ? nodeKey.slice(5) : nodeKey,
      pinode: parentKey,
      isLeaf: targetNode!.isLeaf,
    });
    if (cuttingNodeKey === nodeKey) {
      updater.cuttingNodeKey(null);
    }
    await onLoadTreeData(targetNode.parentKey!); // 重新刷被剪切的父文件夹
    await onLoadTreeData(parentKey); // 重新刷被粘贴的父文件夹
    if (!targetNode.isLeaf && !!targetNode.children) {
      // 如果剪切的是文件夹，那么可能这个文件夹已经被展开了，那么刷新父文件夹之后children就丢了。所有手动粘贴一下
      const dataCp = cloneDeep(latestTreeData.current);
      const nodeInNewParent = findTargetNode(nodeKey, dataCp);
      if (nodeInNewParent) {
        nodeInNewParent.children = targetNode.children;
        updater.treeData(dataCp);
      }
    }
    if (!expandedKeys.includes(parentKey)) {
      updater.expandedKeys(expandedKeys.concat(parentKey));
    }
  };

  const onPaste = async (nodeKey: string) => {
    if (cuttingNodeKey) {
      await handleMoveNode(cuttingNodeKey!, nodeKey);
    } else if (copyingNodeKey) {
      const dataCp = cloneDeep(treeData);
      const targetNode = findTargetNode(copyingNodeKey, dataCp)!;
      copyNode &&
        (await copyNode({ inode: targetNode.isLeaf ? copyingNodeKey.slice(5) : copyingNodeKey, pinode: nodeKey }));
      targetNode.className = getClassName(targetNode);
      updater.copyingNodeKey(null);
      updater.treeData(dataCp);
      await onLoadTreeData(nodeKey);
      if (!expandedKeys.includes(nodeKey)) {
        updater.expandedKeys(expandedKeys.concat(nodeKey));
      }
    }
  };

  const onDelete = async (nodeKey: string) => {
    const currentNode = findTargetNode(nodeKey, treeData)!;
    // 检查当前删除的节点是不是currentKey的父级
    await deleteNode!(
      { inode: currentNode.isLeaf ? nodeKey.slice(5) : nodeKey },
      currentNode.isLeaf ? currentKey === nodeKey : isAncestor(treeData, currentKey, nodeKey),
    );
    onLoadTreeData(currentNode.parentKey!);
    if (nodeKey === copyingNodeKey) {
      updater.copyingNodeKey(null);
    } else if (nodeKey === cuttingNodeKey) {
      updater.cuttingNodeKey(null);
    }
  };

  const onCopyOrCut = (nodeKey: string, isCut: boolean) => {
    const dataCp = cloneDeep(treeData);
    if (copyingNodeKey) {
      // 先把上一个剪切的点取消
      const originalNode = findTargetNode(copyingNodeKey, dataCp)!;
      originalNode.className = getClassName(originalNode);
    }
    if (cuttingNodeKey) {
      const originalNode = findTargetNode(cuttingNodeKey, dataCp)!;
      originalNode.className = getClassName(originalNode);
    }
    const node = findTargetNode(nodeKey, dataCp)!;
    node.className = `border-dashed ${getClassName(node)}`;
    if (isCut) {
      updater.cuttingNodeKey(nodeKey); // 复制和剪切互斥关系
      updater.copyingNodeKey(null);
    } else {
      updater.copyingNodeKey(nodeKey);
      updater.cuttingNodeKey(null);
    }
    updater.treeData(dataCp);
  };

  const cancelCutCopyAction = (isCut: boolean) => ({
    node: isCut ? i18n.t('common:cancel cut') : i18n.t('common:cancel copy'),
    func: (key: string) => {
      const dataCp = cloneDeep(treeData);
      const node = findTargetNode(key, dataCp)!;
      node.className = getClassName(node);
      updater.cuttingNodeKey(null);
      updater.copyingNodeKey(null);
      updater.treeData(dataCp);
    },
  });

  const presetMap: {
    [p: string]: {
      node?: string | JSX.Element;
      func: (key: string) => void;
      condition?: (node: TreeNode) => boolean | IAction;
      hookFn?: (nodeKey: string, isCreate: boolean) => Promise<void>;
    } | null;
  } = {
    [NEW_FOLDER]: createNode
      ? {
          func: (key: string) => createTreeNode(key, true),
          condition: (node) => node.key !== copyingNodeKey && node.key !== cuttingNodeKey, // 本身是被复制或剪切中的节点不能创建新节点
        }
      : null,
    [RENAME_FOLDER]: updateNode
      ? {
          func: (key: string) => renameTreeNode(key, true),
          condition: (node) => node.key !== copyingNodeKey && node.key !== cuttingNodeKey, // 本身是被复制或剪切中的节点不能重命名
        }
      : null,
    [DELETE]: deleteNode
      ? {
          node: (
            <Popover trigger="click" content={i18n.t('dop:confirm to delete?')} onCancel={(e) => e.stopPropagation()}>
              <div
                onClick={(e) => {
                  e.stopPropagation();
                }}
              >
                {i18n.t('delete')}
              </div>
            </Popover>
          ),
          func: async (key: string) => {
            onDelete(key);
          },
        }
      : null,
    [CUT]: moveNode
      ? {
          func: (key: string) => onCopyOrCut(key, true),
          condition: (node) => (node.key !== cuttingNodeKey ? true : cancelCutCopyAction(true)), // 本身就是被剪切的节点要被替换成取消
        }
      : null,
    [PASTE]:
      moveNode || copyNode
        ? {
            func: (key: string) => onPaste(key),
            condition: (node) =>
              (!!cuttingNodeKey || !!copyingNodeKey) &&
              cuttingNodeKey !== node.key &&
              copyingNodeKey !== node.key &&
              !isAncestor(treeData, node.key, cuttingNodeKey) &&
              !isAncestor(treeData, node.key, copyingNodeKey), // 如果当前节点是已被剪切或复制的节点的儿子，那么不能粘贴， 否则会循环引用
          }
        : null,
    [COPY]: copyNode
      ? {
          func: (key: string) => onCopyOrCut(key, false),
          condition: (node) => (node.key !== copyingNodeKey ? true : cancelCutCopyAction(false)), // 本身就是被复制的节点要被替换成取消
        }
      : null,
    [CUSTOM_EDIT]: {
      func: noop,
      hookFn: async (nodeKey: string, isCreate: boolean) => {
        // 用户自定义编辑，这可以提供用户自定义去创建文件和文件夹， 大多数情况是弹窗，所有func由用户自定义，然后透出hook,如果是创建行为则nodeKey本身是父，就用nodeKey用来刷新父目录，如果是Update行为，nodeKey是子，则要取出父来刷
        let parentKey = nodeKey;
        if (!isCreate) {
          const node = findTargetNode(nodeKey, treeData)!;
          parentKey = node.parentKey!;
        }
        await onLoadTreeData(parentKey);
        if (!expandedKeys.includes(parentKey)) {
          onExpand(expandedKeys.concat(parentKey));
        }
      },
    },
    [NEW_FILE]: createNode
      ? {
          func: (key: string) => createTreeNode(key, false),
          condition: (node) => node.key !== copyingNodeKey && node.key !== cuttingNodeKey, // 本身是被复制或剪切中的节点不能创建新节点
        }
      : null,
    [RENAME_FILE]: updateNode
      ? {
          func: (key: string) => renameTreeNode(key, false),
          condition: (node) => node.key !== copyingNodeKey && node.key !== cuttingNodeKey, // 本身是被复制或剪切中的节点不能重命名
        }
      : null,
  };

  const generateActions = (rawActions: TreeAction[], node: TreeNode): IAction[] => {
    const _actions = reduce(
      rawActions,
      (acc: IAction[], action) => {
        const { preset, func, node: actionNode, hasAuth = true, authTip } = action;
        if (preset) {
          const defaultFn =
            (
              fn: (key: string, n: TreeNodeNormal) => Promise<void>,
              hook?: (nodeKey: string, isCreate: boolean) => Promise<void>,
            ) =>
            async (key: string, n: TreeNodeNormal) => {
              await fn(key, n);
              func && func(key, n, hook);
              onPopMenuVisibleChange(key, false);
            };
          const presetAction = presetMap[preset]; // 策略模式取具体action
          if (presetAction) {
            const { node: presetNode, func: presetFunc, condition, hookFn } = presetAction;
            const addable = condition ? condition(node) : true; // condition为true则要添加
            if (!addable) {
              return acc;
            }
            if (typeof addable !== 'boolean') {
              return acc.concat(addable);
            }
            if (preset === DELETE && !hasAuth) {
              // 没权限的时候，去除删除的确认框
              return acc.concat({
                node: (
                  <div>
                    <WithAuth pass={hasAuth} noAuthTip={authTip}>
                      <span>{actionNode}</span>
                    </WithAuth>
                  </div>
                ),
                func: noop,
              });
            }
            return acc.concat({
              node: (
                <div>
                  <WithAuth pass={hasAuth} noAuthTip={authTip}>
                    <span>{presetNode || actionNode}</span>
                  </WithAuth>
                </div>
              ),
              func: hasAuth ? defaultFn(presetFunc, hookFn) : noop,
            });
          }
          return acc;
        }
        return func
          ? acc.concat({
              func: (curKey: string, curNode: TreeNodeNormal) => {
                func(curKey, curNode);
                onPopMenuVisibleChange(curKey, false);
              },
              node: actionNode,
            })
          : acc;
      },
      [],
    );
    return _actions;
  };

  const getActions = (node: TreeNodeNormal): IAction[] => {
    const execNode = node as TreeNode;
    if (execNode.disableAction) {
      return [];
    }
    if (node.isLeaf) {
      const fileActions =
        typeof actions?.fileActions === 'function' ? actions.fileActions(execNode) : actions?.fileActions;
      return generateActions(fileActions || [], execNode);
    }
    const folderActions =
      typeof actions?.folderActions === 'function' ? actions.folderActions(execNode) : actions?.folderActions;
    return generateActions(folderActions || [], execNode);
  };

  const onDrop = async (info: { dragNode: AntTreeNodeProps; node: AntTreeNodeProps }) => {
    const { dragNode, node: dropNode } = info;
    const dragKey = dragNode.key;
    let dropKey = dropNode.key;
    if (dropNode.isLeaf) {
      dropKey = dropNode.parentKey;
    }
    await handleMoveNode(dragKey, dropKey);
  };

  const operations = () => {
    if (titleOperation && Array.isArray(titleOperation)) {
      return map(titleOperation, (operation) => {
        if (operation.preset && operation.preset === NEW_FOLDER && createNode) {
          return newFolderOperation(async ({ name }: { name: string }) => {
            await createNode({ name, pinode: rootKey, type: 'd' });
            onLoadTreeData(rootKey);
          });
        }
        return operation;
      }) as Array<{ title: string }>;
    }
    return [];
  };

  const onSearch = async (query: string) => {
    if (!query || !fuzzySearch) {
      return;
    }
    const searchResult = await fuzzySearch({ fuzzy: query });
    const folderGroup = [] as Array<{ label: string; value: string }>;
    const fileGroup = [] as Array<{ label: string; value: string }>;
    forEach(searchResult, ({ isLeaf, key, titleAlias }) => {
      if (isLeaf) {
        fileGroup.push({ label: titleAlias, value: `leaf-${key}` });
      } else {
        folderGroup.push({ label: titleAlias, value: key });
      }
    });
    updater.filterOptions({ folderGroup, fileGroup });
  };

  const handleSearchChange = async (key?: string) => {
    if (!key) {
      updater.filterOptions({ folderGroup: [], fileGroup: [] });
      return;
    }
    const isLeaf = key.startsWith('leaf');
    const ancestors = await getAncestors({ inode: isLeaf ? key.slice(5) : key });
    const keys = map(ancestors, 'key');
    if (isLeaf) {
      updater.expandedKeys(Array.from(new Set(expandedKeys.concat(keys))));
      onSelectNode({ inode: key.slice(5), isLeaf });
    } else {
      updater.expandedKeys(Array.from(new Set(expandedKeys.concat(keys).concat(key))));
    }
  };

  const generateSearchOptions = () => {
    const { fileGroup, folderGroup } = filterOptions;
    const { file, folder } = searchGroup || { file: i18n.t('file'), folder: i18n.t('common:folder') };
    const options = [];
    if (folderGroup.length > 0) {
      options.push(
        <OptGroup key="folder" label={folder}>
          {map(folderGroup, ({ value, label }) => (
            <Option key={value} value={value}>
              {label}
            </Option>
          ))}
        </OptGroup>,
      );
    }
    if (fileGroup.length > 0) {
      options.push(
        <OptGroup key="file" label={file}>
          {map(fileGroup, ({ value, label }) => (
            <Option key={value} value={value}>
              {label}
            </Option>
          ))}
        </OptGroup>,
      );
    }
    return options;
  };

  return (
    <div>
      {title ? <Title title={title} showDivider={false} operations={operations()} /> : null}
      {fuzzySearch ? (
        <Select
          showSearch
          placeholder={i18n.t('common:please enter keyword to search')}
          showArrow={false}
          filterOption
          optionFilterProp={'children'}
          notFoundContent={null}
          onSearch={onSearch}
          onChange={handleSearchChange}
          className="w-full"
          allowClear
        >
          {generateSearchOptions()}
        </Select>
      ) : null}
      <Spin spinning={!!loading}>
        <Tree
          selectedKeys={currentKey ? [currentKey] : []}
          loadData={(node) => onLoadTreeData(node.key)}
          treeData={treeData}
          expandedKeys={expandedKeys}
          className="tree-category-container"
          blockNode
          showIcon
          onExpand={onExpand}
          onSelect={onClickNode}
          titleRender={(nodeData: TreeNodeNormal) => {
            const execNode = nodeData as TreeNode;
            return (
              <span className={`inline-block truncate ${execNode.disableAction ? 'w-full' : 'has-operates'}`}>
                {nodeData.title}
                {!execNode.disableAction && (
                  <Popover
                    content={getActions(nodeData).map((item) => (
                      <div className="action-btn" onClick={() => item.func?.(nodeData.key, nodeData)}>
                        {item.node}
                      </div>
                    ))}
                    footer={false}
                  >
                    <CustomIcon type="gd" className="tree-node-action" />
                  </Popover>
                )}
              </span>
            );
          }}
          draggable={!!moveNode && !cuttingNodeKey && !copyingNodeKey} // 当有剪切复制正在进行中时，不能拖动
          onDrop={onDrop}
          {...treeProps}
        />
      </Spin>
    </div>
  );
};
export default TreeCategory;
