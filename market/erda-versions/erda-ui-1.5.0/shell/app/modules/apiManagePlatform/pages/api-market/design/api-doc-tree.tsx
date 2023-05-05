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

/* eslint-disable no-param-reassign */
import React from 'react';
import { EmptyHolder, ErdaIcon } from 'common';
import { useUpdate } from 'common/use-hooks';
import { Popover, Tree, Modal } from 'antd';
import i18n from 'i18n';
import { AntTreeNodeSelectedEvent } from 'core/common/interface';
import apiDesignStore from 'apiManagePlatform/stores/api-design';
import { map, filter, isEmpty, find, forEach, some } from 'lodash';
import { API_TREE_OPERATION } from 'app/modules/apiManagePlatform/configs.ts';
import routeInfoStore from 'core/stores/route';
import { produce } from 'immer';
import { TreeTitle, BranchTitle } from './title';
import { updateSearch } from 'common/utils';
import { useMount, useUpdateEffect } from 'react-use';
import './index.scss';
import ReactDOM from 'react-dom';

const { TreeNode } = Tree;

interface IApiDocTree {
  newTreeNode: API_SETTING.IFileTree;
  treeNodeData: Obj;
  getQuoteMap: (e: any) => void;
  onSelectDoc: (docData: Obj, reset?: boolean) => void;
  popVisible: boolean;
  onVisibleChange: (val: boolean) => void;
}

const ApiDocTree = React.memo((props: IApiDocTree) => {
  const [{ treeList }, updater] = useUpdate({
    treeList: [] as API_SETTING.IFileTree[],
  });

  const { getQuoteMap, onSelectDoc, newTreeNode, treeNodeData, popVisible, onVisibleChange } = props;

  const { appId, projectId, orgName } = routeInfoStore.useStore((s) => s.params);
  const { inode: inodeRouter, pinode: pinodeRouter } = routeInfoStore.useStore((s) => s.query);
  let inodeQuery = inodeRouter;
  let pinodeQuery = pinodeRouter;

  const [branchList, apiWs, isDocChanged, isSaved, wsQuery] = apiDesignStore.useStore((s) => [
    s.branchList,
    s.apiWs,
    s.isDocChanged,
    s.isSaved,
    s.wsQuery,
  ]);

  const { getTreeList, getApiDetail, deleteTreeNode, renameTreeNode, setSavedState, commitSaveApi } = apiDesignStore;

  useMount(() => {
    if (!inodeRouter && !pinodeRouter) {
      inodeQuery = localStorage.getItem(`apim-${orgName}-${projectId}-${appId}-inode`);
      pinodeQuery = localStorage.getItem(`apim-${orgName}-${projectId}-${appId}-pinode`);
    }
    initBranchTree();
  });

  useUpdateEffect(() => {
    if (!inodeRouter && !pinodeRouter) {
      inodeQuery = localStorage.getItem(`apim-${orgName}-${projectId}-${appId}-inode`);
      pinodeQuery = localStorage.getItem(`apim-${orgName}-${projectId}-${appId}-pinode`);
      jumpToNewDoc({ inode: inodeQuery, pinode: pinodeQuery, branches: branchList });
    }
  }, [inodeRouter, pinodeRouter]);

  const onSelectTreeNode = (_selectedKeys: string[], { node }: AntTreeNodeSelectedEvent) => {
    const { eventKey, isLeaf, pinode, readOnly, name } = node.props;
    onVisibleChange(false);
    const _branch = find(branchList, { inode: pinode });
    const tempNodeData = { inode: eventKey, pinode, branchName: _branch?.name, apiDocName: name, readOnly };

    const onSelectHandle = () => {
      apiWs && apiWs.close();
      onSelectDoc(tempNodeData, true);
      jumpToNewDoc({ inode: eventKey as string, pinode });
    };

    if (isLeaf) {
      if (isDocChanged) {
        Modal.confirm({
          title: `${i18n.t('dop:not saved yet, confirm to leave')}?`,
          onOk: onSelectHandle,
        });
      } else {
        onSelectHandle();
      }
    }
  };

  const treeListRef = React.useRef({});

  React.useEffect(() => {
    if (!isEmpty(treeList)) {
      treeListRef.current = treeList;
    }
  }, [treeList]);

  const jumpToNewDoc = React.useCallback(
    ({ inode, branches, pinode }: { inode: string; pinode: string; branches?: API_SETTING.IFileTree[] }) => {
      if (!inode || !pinode) return;

      apiWs && apiWs.close();
      const _branchList = branches || branchList;

      getApiDetail(inode).then((data) => {
        getQuoteMap(data.openApiDoc);
        updateSearch({ inode, pinode });
        localStorage.setItem(`apim-${orgName}-${projectId}-${appId}-inode`, inode);
        localStorage.setItem(`apim-${orgName}-${projectId}-${appId}-pinode`, pinode);

        const _branch = find(_branchList, { inode: pinode });
        const _curNodeData = { inode, pinode, branchName: _branch?.name, asset: data?.asset, apiDocName: data?.name };

        onSelectDoc(_curNodeData);
      });
    },
    [apiWs, branchList, getApiDetail, getQuoteMap, onSelectDoc, orgName, projectId, appId],
  );

  React.useEffect(() => {
    if (isSaved) {
      setSavedState(false);
      const { inode, pinode } = newTreeNode;
      if (inode && pinode) {
        jumpToNewDoc({ inode, pinode });
      }
    }
  }, [isSaved, jumpToNewDoc, newTreeNode, setSavedState]);

  const getTitleProps = (titleData: any) => {
    const { name, pinode, inode, meta } = titleData;
    return {
      name,
      inode,
      pinode,
      readOnly: meta?.readOnly,
      execOperation: (operationKey: string, extraParam?: Obj) => {
        if (operationKey === API_TREE_OPERATION.delete) {
          deleteTreeNode(titleData).then(() => {
            const tempData = produce(treeListRef.current, (draft) => {
              forEach(draft, (d: any) => {
                if (d.key === pinode) {
                  d.children = filter(d.children, (item) => item.name !== name);
                }
              });
            });
            updater.treeList(tempData as API_SETTING.IFileTree[]);
          });
        } else {
          renameTreeNode({ ...titleData, name: extraParam?.name }).then((res) => {
            const tempData = produce(treeListRef.current, (draft) => {
              forEach(draft, (d: any) => {
                if (d.key === pinode) {
                  forEach(d.children, (c: any) => {
                    if (c.key === inode) {
                      c.key = res.inode;
                      c.name = res.name;
                      c.title = <TreeTitle {...getTitleProps(res)} key={res.inode} popToggle={onVisibleChange} />;
                    }
                  });
                }
              });
            });
            updater.treeList(tempData as API_SETTING.IFileTree[]);
          });
        }
      },
    };
  };

  React.useEffect(() => {
    if (!isEmpty(newTreeNode)) {
      const { name, pinode, inode } = newTreeNode;

      const titleProps = getTitleProps({ ...newTreeNode });
      const newNode = {
        title: <TreeTitle {...titleProps} key={inode} popToggle={onVisibleChange} />,
        key: inode,
        isLeaf: true,
        name,
        pinode,
      };

      const oldBranch = find(treeList, { key: pinode });

      const tempTreeData = produce(treeList, (draft) => {
        if (!oldBranch) {
          const newBranch: API_SETTING.IFileTree = find(branchList, { inode: pinode });
          if (newBranch) {
            const newBranchNode = {
              title: <BranchTitle name={newBranch.name} />,
              key: newBranch.inode,
              isLeaf: false,
              selectable: false,
              showIcon: true,
              name,
              children: [newNode],
            };
            draft.push(newBranchNode);
          }
        } else {
          some(draft, (item) => {
            if (item.key === pinode && item.children) {
              item.children.push(newNode);
              return true;
            } else {
              return false;
            }
          });
        }
      });
      updater.treeList(tempTreeData);

      const isConnectedWs = wsQuery && wsQuery.sessionID;
      if (isEmpty(treeNodeData)) {
        jumpToNewDoc({ inode, pinode });
      } else {
        const confirmTitle = isConnectedWs
          ? `${i18n.t('dop:whether to save and jump to the newly created document')}?`
          : `${i18n.t('dop:whether to jump to the newly created document')}?`;
        Modal.confirm({
          title: confirmTitle,
          onOk: () => {
            if (isConnectedWs) {
              commitSaveApi();
            } else {
              jumpToNewDoc({ inode, pinode });
            }
          },
        });
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [newTreeNode]);

  const onLoadData = (treeNode: any) => {
    return new Promise((resolve) => {
      if (treeNode.props.children) {
        resolve(true);
        return;
      }
      const curPinode = treeNode?.props?.eventKey;
      getTreeList({
        pinode: curPinode,
      }).then((res) => {
        const tempList = map(res, (item) => {
          const { name, inode, meta } = item;
          return {
            title: <TreeTitle {...getTitleProps(item)} key={inode} popToggle={onVisibleChange} />,
            key: inode,
            isLeaf: true,
            name,
            pinode: curPinode,
            readOnly: meta?.readOnly || false,
          };
        });

        treeNode.props.dataRef.children = tempList;
        updater.treeList(treeList);
        resolve(true);
      });
    });
  };

  const renderTreeNodes = (data: any[]) => {
    return map(data, (item) => {
      if (item.children) {
        return (
          <TreeNode title={item.title} key={item.key} dataRef={item}>
            {renderTreeNodes(item.children)}
          </TreeNode>
        );
      }
      return <TreeNode {...item} dataRef={item} />;
    });
  };

  const treeRef = React.useRef(null);

  const content = (
    <React.Fragment>
      <div className="list-wrap">
        {isEmpty(treeList) ? (
          <EmptyHolder relative />
        ) : (
          <Tree
            className="api-tree"
            blockNode
            defaultExpandedKeys={[pinodeQuery]}
            selectedKeys={[inodeQuery]}
            loadData={onLoadData}
            onSelect={onSelectTreeNode}
            ref={treeRef}
          >
            {renderTreeNodes(treeList)}
          </Tree>
        )}
      </div>
    </React.Fragment>
  );

  const initBranchTree = () => {
    if (!isEmpty(treeList)) return;
    getTreeList({
      pinode: '0',
      scope: 'application',
      scopeID: +appId,
    }).then((res: API_SETTING.IFileTree[]) => {
      const validBranches: API_SETTING.IFileTree[] = res || [];

      const tempList = map(validBranches, ({ name, inode }) => {
        return {
          title: <BranchTitle name={name} />,
          key: inode,
          isLeaf: false,
          selectable: false,
          showIcon: true,
          name,
        };
      });
      updater.treeList(tempList);

      if (pinodeQuery && inodeQuery) {
        jumpToNewDoc({ inode: inodeQuery, pinode: pinodeQuery, branches: validBranches });
      }
    });
  };

  React.useEffect(() => {
    const popoverHide = (e: any) => {
      // 点击外部，隐藏选项
      // eslint-disable-next-line react/no-find-dom-node
      const el2 = ReactDOM.findDOMNode(treeRef.current) as HTMLElement;
      if (!(el2 && el2.contains(e.target))) {
        onVisibleChange(false);
        document.body.removeEventListener('click', popoverHide);
      }
    };
    if (popVisible) {
      document.body.addEventListener('click', popoverHide);
    }
  }, [onVisibleChange, popVisible]);

  return (
    <Popover
      title={i18n.t('dop:please select a document under the branch')}
      overlayClassName="branch-doc-select-popover"
      trigger="hover"
      placement="bottomLeft"
      autoAdjustOverflow={false}
      content={content}
      visible={popVisible}
    >
      <button
        onClick={() => onVisibleChange(!popVisible)}
        className={`api-file-select ${!treeNodeData?.apiDocName ? 'text-desc' : ''}`}
      >
        <span>{i18n.t('document')}：</span>
        <span className="name nowrap">
          {treeNodeData?.branchName
            ? `${treeNodeData?.branchName}/${treeNodeData?.apiDocName}`
            : i18n.t('common:expand branch directory selection document')}
        </span>
        <ErdaIcon type="caret-down" size="20" color="black-400" />
      </button>
    </Popover>
  );
});

export default ApiDocTree;
