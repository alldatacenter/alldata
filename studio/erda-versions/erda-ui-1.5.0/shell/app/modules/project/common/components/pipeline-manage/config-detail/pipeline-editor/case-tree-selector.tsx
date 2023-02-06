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
import { TreeSelect, Button, Select } from 'antd';
import { get, map, isEmpty, debounce, filter } from 'lodash';
import { getSnippetNodeDetail } from 'project/services/auto-test-case';
import { getTreeNodeDetailNew, getCategoryByIdNew, fuzzySearchNew } from 'common/services/file-tree';
import { notify } from 'common/utils';
import { useUpdate } from 'common/use-hooks';
import routeInfoStore from 'core/stores/route';
import { Form } from 'dop/pages/form-editor/index';
import { ymlDataToFormData } from 'app/yml-chart/common/in-params-drawer';
import { SCOPE_PROJECT, scopeMap } from 'project/common/components/pipeline-manage/config';
import i18n from 'i18n';

export interface IProps {
  nodeData: any;
  editing?: boolean;
  onChange: (arg: any) => void;
  closeDrawer: () => void;
  curCaseId: string;
  otherTaskAlias?: string[];
  useableScope?: any[];
  scope: string;
}

const { Option } = Select;

enum NodeType {
  dir = 'd', // 文件目录
  file = 'f', // 文件
}

const reAliasKey = 're__alias__'; // 重命名别名
const useableScopeMap = {
  projectPipeline: [scopeMap.projectPipeline, scopeMap.appPipeline, scopeMap.autoTestPlan, scopeMap.configSheet],
  autoTest: [scopeMap.autoTest, scopeMap.configSheet], // 可引用用例：配置单、测试用例
};

const getParams = (data: AUTO_TEST.ISnippetDetailRes) => {
  let params = [] as PIPELINE.IPipelineInParams[];
  map(data, (item) => {
    if (!isEmpty(item.params)) {
      params = params.concat(item.params);
    }
  });
  return params;
};

const filterNull = (data: Obj) => {
  const res = {};
  map(data, (v, k) => {
    if (v !== null && v !== undefined) {
      res[k] = v;
    }
  });
  return res;
};

const convertTreeData = (data: AUTO_TEST.ICaseDetail[]) => {
  return map(data, (item) => {
    return {
      ...item,
      key: item.inode,
      id: item.inode,
      pId: item.pinode,
      title: item.name || item.inode,
      isLeaf: item.type === NodeType.file,
      selectable: item.type === NodeType.file, // 只有叶子节点可选
      value: item.inode,
      disabled: item.type === NodeType.dir, // disable非叶子节点
    };
  });
};

export const CaseTreeSelector = (props: IProps) => {
  const {
    onChange: propsOnChange,
    nodeData,
    closeDrawer,
    editing = false,
    curCaseId,
    otherTaskAlias = [],
    scope,
  } = props;
  const projectId = routeInfoStore.getState((s) => s.params.projectId);
  const useableScope = useableScopeMap[scope];
  const formRef = React.useRef(null as any);

  const getCurCase = (_nodeData?: any) => {
    if (!isEmpty(_nodeData)) {
      const label = _nodeData.alias;
      const dataScope = get(_nodeData, 'snippet_config.labels.snippet_scope') || SCOPE_PROJECT;
      return {
        chosenCase: { ..._nodeData, label: label || get(_nodeData, 'snippet_config.name') },
        chosenType: dataScope,
      };
    }
    return {
      chosenCase: null,
      chosenType: !isEmpty(useableScope) ? get(useableScope, '[0].scope') : scopeMap[scope].scope,
    };
  };

  const _curCase = getCurCase(nodeData);

  const [{ value, chosenCase, fields, formValue, searchValue, dataList, chosenType }, updater, update] = useUpdate({
    value: undefined as any,
    chosenCase: _curCase.chosenCase as any,
    fields: [] as PIPELINE.IPipelineInParams[],
    formValue: undefined as any,
    searchValue: undefined as undefined | string,
    dataList: [] as any[],
    chosenType: _curCase.chosenType as string,
  });

  React.useEffect(() => {
    if (!isEmpty(nodeData)) {
      const label = nodeData.alias;
      const dataScope = get(nodeData, 'snippet_config.labels.snippet_scope') || SCOPE_PROJECT;
      update({
        chosenCase: { ...nodeData, label: label || get(nodeData, 'snippet_config.name') },
        chosenType: dataScope,
      });
    }
  }, [nodeData, update]);

  React.useEffect(() => {
    if (!isEmpty(chosenCase)) {
      if (!isEmpty(chosenCase.params)) {
        updater.formValue(filterNull(chosenCase.params));
      }
      const curCase = { ...chosenCase } as any;
      const snippet_config = get(curCase, 'snippet_config');
      if (!isEmpty(snippet_config)) {
        const snippetConfigs = [
          {
            alias: curCase.alias,
            source: get(curCase, 'snippet_config.source'),
            name: get(curCase, 'snippet_config.name'),
            labels: {
              ...(get(curCase, 'snippet_config.labels') || {}),
              projectID: projectId,
            },
          },
        ];
        let renameFields = [] as any[];
        if (otherTaskAlias.includes(chosenCase.alias)) {
          renameFields = [
            {
              label: i18n.t('dop:case name'),
              key: reAliasKey,
              type: 'input',
              component: 'input',
              required: true,
              componentProps: {
                placeholder: i18n.t('dop:Name already existed. Please rename it.'),
                maxLength: 50,
              },
              rules: [
                {
                  validator: (v: string) => {
                    return [!otherTaskAlias.includes(v), i18n.t('{name} already exists', { name: i18n.t('name') })];
                  },
                },
              ],
            },
          ];
        }

        (getSnippetNodeDetail({ snippetConfigs }) as unknown as Promise<any>)
          .then((res: any) => {
            const _inParams = getParams(res.data);
            if (!isEmpty(_inParams)) {
              // 节点无入参
              const _f = [
                ...renameFields,
                {
                  component: 'custom',
                  getComp: () => {
                    return <div className="font-medium border-bottom">{i18n.t('dop:node params')}</div>;
                  },
                },
              ];

              const _inParamsFields = map(ymlDataToFormData(_inParams), (item: any) => {
                return editing ? item : { ...item, disabled: true };
              });
              updater.fields([..._f, ..._inParamsFields]);
            } else {
              updater.fields([...renameFields]);
            }
          })
          .catch((e: any) => {
            updater.chosenCase(null);
          });
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [chosenCase]);

  React.useEffect(() => {
    if (formRef && formRef.current) {
      formRef.current.setFields(fields);
    }
  }, [fields]);

  const changeType = (val: any) => {
    update({
      chosenType: val,
      chosenCase: null,
      fields: [],
      formValue: undefined,
      searchValue: undefined,
    });
  };

  const onSubmit = () => {
    if (formRef && formRef.current) {
      // 有表单
      formRef.current.onSubmit((val: Obj) => {
        const { [reAliasKey]: reAalias } = val;
        propsOnChange &&
          propsOnChange({
            ...chosenCase,
            params: val,
            ...(reAalias ? { alias: reAalias } : {}),
          });
      });
    } else {
      propsOnChange && propsOnChange(chosenCase);
    }
  };

  const onLoadData = (treeNode?: any) => {
    const pinode = get(treeNode, 'props.id') || '0'; // 根目录为0
    return (getCategoryByIdNew({ pinode, scopeID: projectId, scope: chosenType }) as unknown as Promise<any>).then(
      (res: any) => {
        const _list = convertTreeData(res.data);
        const allList = pinode === '0' ? [..._list] : [...dataList, ..._list];

        const folders = allList.filter((node) => !node.isLeaf);
        const files = allList.filter((node) => node.isLeaf);
        const collator = new Intl.Collator(undefined, { numeric: true, sensitivity: 'base' });

        folders.sort((x, y) => collator.compare(x.title, y.title));
        files.sort((x, y) => collator.compare(x.title, y.title));

        updater.dataList([...folders, ...files]);
      },
    );
  };

  const onChange = (chosenOne: any, _: any, extra: any) => {
    const chosenId = chosenOne.value;
    const curScope = get(extra, 'triggerNode.props.scope');
    chosenId &&
      curScope &&
      (getTreeNodeDetailNew({ id: chosenId, scope: curScope, scopeID: projectId }) as unknown as Promise<any>).then(
        (res: any) => {
          const node = res.data as AUTO_TEST.ICaseDetail;
          const snippet_config = get(node, 'meta.snippetAction.snippet_config') || {};
          if (isEmpty(snippet_config)) {
            // 没有snippet_config，为无效用例，不可引用
            notify('error', i18n.t('dop:this use case is invalid, please refine it before citation'));
          } else if (curCaseId === node.inode) {
            notify('error', i18n.t('dop:cannot refer to itself'));
          } else {
            updater.chosenCase({
              ...(get(node, 'meta.snippetAction') || {}),
              alias: `${node.name}`,
              label: chosenOne.label,
            });
          }
        },
      );
  };

  const onSearch = (searchKey: string) => {
    updater.searchValue(searchKey);
  };

  const search = React.useCallback(
    debounce((q?: string) => {
      if (q) {
        (
          fuzzySearchNew({
            fuzzy: q,
            scopeID: projectId,
            scope: chosenType,
            recursive: true,
          }) as unknown as Promise<any>
        ).then((res: any) => {
          updater.dataList(filter(convertTreeData(res.data), (item) => item.isLeaf));
        });
      } else {
        updater.dataList([]);
        onLoadData();
      }
    }, 1000),
    [chosenType],
  );

  React.useEffect(() => {
    search();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [chosenType]);

  React.useEffect(() => {
    search(searchValue);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [search, searchValue]);

  React.useEffect(() => {
    if (!isEmpty(chosenCase)) {
      const v = get(chosenCase, 'snippet_config.name');
      updater.value({ label: chosenCase.label || v, value: v });
    } else {
      updater.value(undefined);
    }
  }, [chosenCase, updater]);
  return (
    <div className="h-full auto-test-tree-selector">
      {isEmpty(useableScope) ? null : (
        <>
          <div className="pb-2 text-desc">{i18n.t('please select {name}', { name: i18n.t('type') })}</div>
          <Select value={chosenType} onChange={changeType} className="w-full">
            {map(useableScope, (item) => (
              <Option key={item.scope} value={item.scope}>
                {item.name}
              </Option>
            ))}
          </Select>
        </>
      )}
      <div className="py-2 text-desc">{i18n.t('please select {name}', { name: i18n.t('node') })}</div>
      <TreeSelect
        searchValue={searchValue}
        showSearch
        labelInValue
        autoClearSearchValue={false}
        onSearch={onSearch}
        treeDataSimpleMode
        filterTreeNode={false}
        className="w-full mb-4"
        value={value}
        listHeight={500}
        placeholder={i18n.t('please select')}
        disabled={!editing}
        onChange={onChange}
        loadData={onLoadData}
        treeData={map(dataList, (item) => ({
          ...item,
          disabled: item.isLeaf && item.inode === curCaseId ? true : item.disabled,
        }))}
      />
      {!isEmpty(fields) ? (
        <div className="mb-3">
          <Form fields={fields} value={formValue} formRef={formRef} />
        </div>
      ) : null}
      {editing ? (
        <div className="footer">
          <Button onClick={closeDrawer} className="mr-2">
            {i18n.t('cancel')}
          </Button>
          <Button type="primary" disabled={isEmpty(chosenCase)} onClick={() => onSubmit()}>
            {i18n.t('save')}
          </Button>
        </div>
      ) : null}
    </div>
  );
};
