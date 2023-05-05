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
import { TreeCategory, EmptyHolder } from 'common';
import { useUnmount } from 'react-use';
import i18n from 'i18n';
import { get, isEmpty } from 'lodash';
import { updateSearch, insertWhen } from 'common/utils';
import fileTreeStore from 'common/stores/file-tree';
import { TreeNode } from 'common/components/tree-category/tree';
import routeInfoStore from 'core/stores/route';
import autoTestStore from 'project/stores/auto-test-case';
import CaseEditForm from './config-detail/case-edit-form';
import projectStore from 'project/stores/project';
import { scopeConfig } from './scope-config';

interface IProps {
  scope: string;
}

const FileTree = (props: IProps) => {
  const { scope } = props;
  const scopeConfigData = scopeConfig[scope];
  const [params, query] = routeInfoStore.useStore((s) => [s.params, s.query]);
  const {
    getCategoryById,
    createTreeNode,
    createRootTreeNode,
    deleteTreeNode,
    updateTreeNode,
    moveTreeNode,
    copyTreeNode,
    getAncestors,
    fuzzySearch,
  } = fileTreeStore;
  const [caseDetail] = autoTestStore.useStore((s) => [s.caseDetail]);
  const [rootNode, setRootNode] = React.useState(null as null | TreeNode);
  const [editVis, setEditVis] = React.useState(false);
  const { getCaseDetail, clearCaseDetail } = autoTestStore;
  const [editData, setEditData] = React.useState(undefined as any);
  const editHookRef = React.useRef(null as any);
  const projectInfo = projectStore.useStore((s) => s.info);

  useUnmount(() => {
    clearCaseDetail();
  });

  const getRootNode = React.useCallback(async () => {
    const rootArray = await getCategoryById({ pinode: '0', scopeID: params.projectId, scope: scopeConfigData.scope });
    if (Array.isArray(rootArray) && rootArray.length === 1) {
      setRootNode(rootArray[0]);
    } else {
      // 根节点不存在，自动创建
      const node = await createRootTreeNode({
        name: projectInfo.name,
        type: 'd',
        pinode: '0',
        scope: scopeConfigData.scope,
        scopeID: params.projectId,
      }); // name 待定义
      setRootNode(node);
    }
  }, [createRootTreeNode, getCategoryById, params.projectId, projectInfo.name, scopeConfigData.scope]);

  React.useEffect(() => {
    if (projectInfo.name !== undefined) {
      getRootNode();
    }
  }, [getRootNode, projectInfo]);

  const folderActions = (node: TreeNode) => [
    {
      node: scopeConfigData.text.addFolder,
      preset: 'newFolder',
    },
    {
      node: scopeConfigData.text.addFile,
      preset: 'customEdit',
      func: async (nodeKey: string, _node: any, hook: any) => {
        showFormModal();
        editHookRef.current = {
          hook,
          nodeKey,
        };
      },
    },
    ...insertWhen(node.key === rootNode?.key, [
      {
        node: i18n.t('dop:paste'),
        preset: 'paste',
      },
    ]),
    ...insertWhen(node.key !== rootNode?.key, [
      {
        node: i18n.t('dop:rename'),
        preset: 'renameFolder',
      },
      {
        node: i18n.t('copy'),
        preset: 'copy',
      },
      {
        node: i18n.t('dop:cut'),
        preset: 'cut',
      },
      {
        node: i18n.t('dop:paste'),
        preset: 'paste',
      },
      {
        node: i18n.t('delete'),
        preset: 'delete',
      },
      // {
      //   node: i18n.t('dop:add to test plan'),
      //   func: () => {},
      // },
    ]),
  ];

  const fileActions = [
    {
      node: i18n.t('edit'),
      preset: 'customEdit',
      func: async (nodeKey: string, node: any, hook: any) => {
        showFormModal(node);
        editHookRef.current = {
          hook,
          nodeKey,
        };
      },
    },
    {
      node: i18n.t('copy'),
      preset: 'copy',
    },
    {
      node: i18n.t('dop:cut'),
      preset: 'cut',
    },
    {
      node: i18n.t('delete'),
      preset: 'delete',
    },
  ];

  const titleOperations = [
    {
      preset: 'newFolder',
    },
  ];

  const onSelectNode = ({ inode, isLeaf }: { inode: string; isLeaf: boolean }) => {
    if (isLeaf && inode && query.caseId !== inode) {
      clearCaseDetail();
      setTimeout(() => {
        updateSearch({ caseId: inode });
      }, 0);
    }
  };

  const showFormModal = (node?: any) => {
    setEditData(get(node, 'originData'));
    setEditVis(true);
  };

  const onOk = (val: any) => {
    if (editData) {
      updateTreeNode({ ...editData, ...val }).then(() => {
        if (editHookRef.current && editHookRef.current.hook) {
          editHookRef.current.hook(editHookRef.current.nodeKey);
        }
        if (editData.inode === query.caseId) {
          getCaseDetail({ id: editData.inode });
        }
        onClose();
      });
    } else {
      createTreeNode({ ...val, type: 'f', pinode: editHookRef.current.nodeKey }).then((res: any) => {
        if (editHookRef.current && editHookRef.current.hook) {
          editHookRef.current.hook(editHookRef.current.nodeKey, true);
        }
        onClose();
        const curInode = get(res, 'originData.inode');
        if (curInode) {
          setTimeout(() => {
            updateSearch({ caseId: curInode });
          }, 0);
        }
      });
    }
  };

  const onClose = () => {
    setEditVis(false);
    editHookRef.current = null;
    setEditData(undefined);
  };

  const searchNodes = (payload: { fuzzy: string }) => {
    return fuzzySearch({ scope: scopeConfigData.scope, scopeID: params.projectId, recursive: true, ...payload });
  };

  const currentKey = get(caseDetail, 'type') === 'f' ? `leaf-${caseDetail.inode}` : undefined;
  return (
    <>
      {rootNode && !isEmpty(rootNode) ? (
        <TreeCategory
          title={scopeConfigData.text.fileTreeTitle}
          titleOperation={titleOperations}
          onSelectNode={onSelectNode}
          initTreeData={[{ ...rootNode }]}
          currentKey={currentKey}
          searchGroup={{ file: scopeConfigData.text.searchFile, folder: scopeConfigData.text.searchFolder }}
          effects={{
            moveNode: moveTreeNode,
            loadData: getCategoryById,
            deleteNode: async (key, isCurrentKeyDeleted) => {
              await deleteTreeNode(key);
              if (isCurrentKeyDeleted) {
                updateSearch({ caseId: '' });
              }
            },
            updateNode: updateTreeNode,
            createNode: createTreeNode,
            copyNode: copyTreeNode,
            getAncestors,
            fuzzySearch: searchNodes,
          }}
          actions={{
            folderActions,
            fileActions,
          }}
        />
      ) : (
        <EmptyHolder relative />
      )}
      <CaseEditForm editData={editData} visible={editVis} onOk={onOk} onClose={onClose} scope={scope} />
    </>
  );
};

export default FileTree;
