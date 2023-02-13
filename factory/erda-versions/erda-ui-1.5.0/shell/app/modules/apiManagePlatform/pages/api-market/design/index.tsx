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
import { Icon as CustomIcon, EmptyHolder, LazyRender, Ellipsis, ErdaIcon } from 'common';
import { useUpdate } from 'common/use-hooks';
import { Input, Button, Collapse, Tooltip, Popconfirm, message, Spin, Modal, Popover } from 'antd';
import i18n from 'i18n';
import apiDesignStore from 'apiManagePlatform/stores/api-design';
import { map, keys, get, filter, isEmpty, set, unset, forEach } from 'lodash';
import {
  API_METHODS,
  LIST_TITLE_MAP,
  API_MEDIA_TYPE,
  API_LOCK_WARNING,
  QUOTE_PREFIX,
  QUOTE_PREFIX_NO_EXTENDED,
} from 'app/modules/apiManagePlatform/configs.ts';
import ApiSummary from 'apiManagePlatform/pages/api-market/design/summary';
import ApiResource from 'apiManagePlatform/pages/api-market/components/api-resource/resource';
import DataTypeConfig from 'apiManagePlatform/pages/api-market/design/datatype-config';
import routeInfoStore from 'core/stores/route';
import { produce } from 'immer';
import { useMount, useUnmount } from 'react-use';
import ApiPublishModal from 'apiManagePlatform/pages/api-market/design/api-publish-modal';
import { initApiWs } from 'app/modules/apiManagePlatform/api-ws.ts';
import { WithAuth } from 'user/common';
import { Prompt, Link } from 'react-router-dom';
import { Location } from 'history';
import ApiDocAddModal from './api-doc-add-modal';
import ApiDocTree from './api-doc-tree';
import { useLoading } from 'core/stores/loading';
import { goTo } from 'common/utils';
import appStore from 'application/stores/application';
import './index.scss';
import invalidImg from 'app/images/invalid-doc.svg';

type IListKey = 'RESOURCE' | 'DATATYPE';
const { Panel } = Collapse;
const { confirm } = Modal;

const ErrorEmptyHolder = ({
  msg,
  branchName,
  docName,
  isLoading,
}: {
  msg: string;
  branchName: string;
  docName: string;
  isLoading: boolean;
}) => {
  if (isLoading) {
    return <EmptyHolder relative />;
  }
  const [branchList] = apiDesignStore.useStore((s) => [s.branchList]);

  const apiDocsLink = goTo.resolve.apiDocs({ branchName, docName });
  const apiBranchLink = goTo.resolve.app();
  const isErrorDoc = msg && branchName && docName;

  const validBranches = filter(branchList, (b) => b?.meta?.hasDoc);
  let Comp = null;

  if (isErrorDoc) {
    const iconImg = <img src={invalidImg} width="200px" />;
    Comp = (
      <EmptyHolder
        icon={iconImg}
        tip={i18n.t('dop:the document is illegal according to the rules of openapi 3.0. Please click to')}
        action={<Link to={apiDocsLink}>{i18n.t('dop:view document details')}</Link>}
      />
    );
  } else if (isEmpty(branchList)) {
    Comp = (
      <EmptyHolder
        tip={i18n.t('dop:please download from the code repository')}
        action={<Link to={apiBranchLink}>{i18n.t('dop:new branch')}</Link>}
      />
    );
  } else if (isEmpty(validBranches)) {
    Comp = <EmptyHolder tip={i18n.t('common:expand branch directory selection document or create a new document')} />;
  } else {
    Comp = <EmptyHolder relative />;
  }

  return Comp;
};

const ErrorPopover = ({ msg, branchName, docName }: { msg: string; branchName: string; docName: string }) => {
  const { projectId, appId } = routeInfoStore.useStore((s) => s.params);

  const gotoDetail = React.useCallback(() => {
    goTo(goTo.pages.apiDocs, { projectId, appId, branchName, docName });
  }, [appId, branchName, docName, projectId]);

  const content = (
    <div>
      <div>
        <CustomIcon type="warnfill" className="text-warning" />
        <span>{i18n.t('dop:the document is illegal according to the rules of openapi 3.0. Please click to')}</span>
        <span className="text-link" onClick={gotoDetail}>
          {' '}
          {i18n.t('dop:view document details')}
        </span>
      </div>
      <div>{msg}</div>
    </div>
  );
  return (
    <Popover placement="bottom" content={content} trigger="hover">
      <div className="ml-2">
        <CustomIcon type="tishi" />
        <span>{i18n.t('dop:document is illegal')}</span>
      </div>
    </Popover>
  );
};

const ApiDesign = () => {
  const [
    {
      contentKey,
      dataTypeFormData,
      filterKey,
      apiResourceList,
      apiDataTypeList,

      quotePathMap,
      treeModalVisible,
      apiModalVisible,
      curTreeNodeData,
      curApiName,
      curDataType,
      newTreeNode,
      popVisible,
      apiDetail,
    },
    updater,
    update,
  ] = useUpdate({
    contentKey: 'SUMMARY',
    dataTypeFormData: {},
    filterKey: '',
    apiResourceList: [] as string[],
    apiDataTypeList: [] as string[],

    quotePathMap: {} as Obj,
    treeModalVisible: false,
    apiModalVisible: false,
    curTreeNodeData: {},
    curApiName: '',

    curDataType: '',
    newTreeNode: {} as API_SETTING.IFileTree,
    popVisible: false,
    apiDetail: {},
  });

  const { inode: inodeQuery, pinode: pinodeQuery } = routeInfoStore.useStore((s) => s.query);

  React.useEffect(() => {
    const [key] = contentKey.split('&DICE&');
    if (key === 'RESOURCE') {
      curApiName && updater.contentKey(`${key}&DICE&${curApiName}`);
    } else {
      curDataType && updater.contentKey(`${key}&DICE&${curDataType}`);
    }
  }, [curApiName, contentKey, updater, curDataType]);
  const { isExternalRepo, repoConfig } = appStore.useStore((s) => s.detail);

  const [openApiDoc, apiWs, apiLockState, isDocChanged, wsQuery, formErrorNum, isApiReadOnly, lockUser, docValidData] =
    apiDesignStore.useStore((s) => [
      s.openApiDoc,
      s.apiWs,
      s.apiLockState,
      s.isDocChanged,
      s.wsQuery,
      s.formErrorNum,
      s.isApiReadOnly,
      s.lockUser,
      s.docValidData,
    ]);

  const {
    updateOpenApiDoc,
    createTreeNode,
    commitSaveApi,
    getApiDetail,
    publishApi,
    updateFormErrorNum,
    resetDocValidData,
  } = apiDesignStore;

  const [getApiDocDetailLoading, commitSaveApiLoading, getTreeListLoading] = useLoading(apiDesignStore, [
    'getApiDetail',
    'commitSaveApi',
    'getTreeList',
  ]);

  useMount(() => {
    window.addEventListener('beforeunload', beforeunload);
  });

  useUnmount(() => {
    updateOpenApiDoc({});
    apiWs && apiWs.close();
    window.removeEventListener('beforeunload', beforeunload);
  });

  const changeRef = React.useRef(null as any);

  React.useEffect(() => {
    changeRef.current = isDocChanged;
  }, [isDocChanged]);

  const beforeunload = React.useCallback((e) => {
    const msg = `${i18n.t('dop:not saved yet, confirm to leave')}?`;
    if (changeRef.current) {
      // eslint-disable-next-line no-param-reassign
      (e || window.event).returnValue = msg;
    }

    return msg;
  }, []);

  const apiResourceMap = React.useMemo(() => {
    const tempMap = openApiDoc?.paths || {};
    const fullKeys = keys(tempMap);
    let tempList = [];
    if (filterKey) {
      tempList = filter(keys(tempMap), (name) => name.indexOf(filterKey) > -1);
    } else {
      tempList = fullKeys;
    }
    updater.apiResourceList(tempList);
    return tempMap;
  }, [filterKey, openApiDoc, updater]);

  const apiDataTypeMap = React.useMemo(() => {
    const tempMap = openApiDoc?.components?.schemas || {};
    const fullKeys = keys(tempMap);
    let tempList = [];
    if (filterKey) {
      tempList = filter(fullKeys, (name) => name.indexOf(filterKey) > -1);
    } else {
      tempList = fullKeys;
    }
    updater.apiDataTypeList(tempList);
    return tempMap;
  }, [filterKey, openApiDoc, updater]);

  const onCreateDoc = (values: { name: string; pinode: string }) => {
    createTreeNode(values).then((res) => {
      updater.newTreeNode(res);
    });
    updater.treeModalVisible(false);
  };

  const onContentChange = React.useCallback(
    (contentName: string) => {
      const nextHandle = () => {
        updateFormErrorNum(0);
        const [, name] = contentName.split('&DICE&');
        updater.contentKey(contentName);
        if (contentName.startsWith('RESOURCE') && name) {
          updater.curApiName(name);
          const tempApiDetail = get(openApiDoc, ['paths', name]) || {};
          updater.apiDetail(tempApiDetail);
        }
        if (contentName.startsWith('DATATYPE')) {
          const _fromData = apiDataTypeMap[name] || { type: 'string', example: 'Example', 'x-dice-name': name };
          updater.dataTypeFormData({ ..._fromData, name });
          updater.curDataType(name);
        }
      };

      if (formErrorNum > 0) {
        confirm({
          title: i18n.t('dop:Are you sure to leave, with the error message not saved?'),
          onOk() {
            nextHandle();
          },
        });
      } else {
        nextHandle();
      }
    },
    [apiDataTypeMap, formErrorNum, openApiDoc, updateFormErrorNum, updater],
  );

  const dataTypeNameMap = React.useMemo(() => {
    return keys(get(openApiDoc, ['components', 'schemas']));
  }, [openApiDoc]);

  const apiNameMap = React.useMemo(() => {
    return keys(openApiDoc.paths || {});
  }, [openApiDoc]);

  const onAddHandle = (addKey: IListKey) => {
    let newData = {};
    let newName = `/api/new${apiResourceList.length}`;
    while (apiNameMap.includes(newName)) {
      newName += '1';
    }
    let dataPath = ['paths', newName];

    if (addKey === 'DATATYPE') {
      newName = `NewDataType${apiDataTypeList.length}`;
      newData = { type: 'string', example: 'Example', 'x-dice-name': newName };
      dataPath = ['components', 'schemas', newName];
    }

    const tempDocDetail = produce(openApiDoc, (draft) => set(draft, dataPath, newData));
    updateOpenApiDoc(tempDocDetail);

    onContentChange(`${addKey}&DICE&${newName}`);
  };

  const onDeleteHandle = (itemKey: string) => {
    const [key, name] = itemKey.split('&DICE&');

    if (key === 'DATATYPE') {
      const newQuoteMap = getQuoteMap(openApiDoc);
      if (newQuoteMap[name]?.length) {
        message.warning(i18n.t('dop:this type is referenced and cannot be deleted'));
        return;
      }
    } else if (key === 'RESOURCE') {
      const paths = keys(openApiDoc.paths);
      if (paths.length === 1) {
        message.warning(i18n.t('dop:at least one API needs to be kept'));
        return;
      }
    }
    const dataPath = key === 'RESOURCE' ? ['paths', name] : ['components', 'schemas', name];
    const tempDocDetail = produce(openApiDoc, (draft) => {
      unset(draft, dataPath);
    });
    updateOpenApiDoc(tempDocDetail);
    onContentChange('SUMMARY');
  };

  // 左侧列表头部渲染
  const renderPanelHead = (titleKey: IListKey) => (
    <div className="list-panel-head inline-flex justify-between items-center">
      <span className="font-bold">{LIST_TITLE_MAP[titleKey]}</span>
      {!apiLockState && (
        <ErdaIcon
          type="plus"
          className="mr-0 cursor-pointer"
          size="16px"
          onClick={(e) => {
            e.stopPropagation();
            onAddHandle(titleKey);
          }}
        />
      )}
    </div>
  );

  // 左侧列表渲染
  const renderListItem = (listKey: IListKey, name: string) => {
    const apiData = apiResourceMap[name] || {};
    const key = `${listKey}&DICE&${name}`;
    return (
      <LazyRender key={name} minHeight={listKey === 'RESOURCE' ? '58px' : '37px'}>
        <div
          className={`list-title ${contentKey === key ? 'list-title-active' : ''}`}
          onClick={() => onContentChange(key)}
        >
          <div className="flex justify-between items-center">
            <Ellipsis title={name}>
              <div className="list-title-name w-full nowrap mr-1">{name}</div>
            </Ellipsis>
            <Popconfirm
              title={`${i18n.t('common:confirm to delete')}?`}
              onConfirm={(e: any) => {
                e.stopPropagation();
                onDeleteHandle(key);
              }}
              onCancel={(e: any) => e.stopPropagation()}
            >
              {!apiLockState && (
                <CustomIcon
                  type="shanchu"
                  className="list-title-btn cursor-pointer"
                  onClick={(e) => e?.stopPropagation()}
                />
              )}
            </Popconfirm>
          </div>
          {listKey === 'RESOURCE' && (
            <div className="method-list">
              {map(API_METHODS, (methodKey: API_SETTING.ApiMethod) => {
                const methodIconClass = !isEmpty(apiData[methodKey]) ? `method-icon-${methodKey}` : '';
                return (
                  <Tooltip title={methodKey} key={methodKey}>
                    <div className={`method-icon mr-2 ${methodIconClass}`} />
                  </Tooltip>
                );
              })}
            </div>
          )}
        </div>
      </LazyRender>
    );
  };

  // 获取所有引用的pathMap
  const getQuoteMap = React.useCallback(
    (data: Obj) => {
      const getQuotePath = (innerData: Obj, prefixPath: Array<number | string>, pathMap: Obj) => {
        const refTypePath = get(innerData, [QUOTE_PREFIX, 0, '$ref']) || innerData[QUOTE_PREFIX_NO_EXTENDED];
        if (refTypePath) {
          const _type = refTypePath.split('/').slice(-1)[0];
          // eslint-disable-next-line no-param-reassign
          !pathMap[_type] && (pathMap[_type] = []);
          if (!pathMap[_type].includes(prefixPath)) {
            pathMap[_type].push(prefixPath);
          }
        }
        if (innerData?.properties) {
          forEach(keys(innerData.properties), (item) => {
            getQuotePath(innerData.properties[item], [...prefixPath, 'properties', item], pathMap);
          });
        }
        if (innerData?.items) {
          getQuotePath(innerData.items, [...prefixPath, 'items'], pathMap);
        }
      };

      const tempMap = {};
      const pathMap = data.paths;
      forEach(keys(pathMap), (path) => {
        const pathData = pathMap[path];
        forEach(keys(pathData), (method) => {
          const methodData = pathData[method];
          const _path = ['paths', path, method];

          forEach(API_MEDIA_TYPE, (mediaType) => {
            // responses
            const responsePath = ['responses', '200', 'content', mediaType, 'schema'];
            const responseData = get(methodData, responsePath) || {};
            getQuotePath(responseData, [..._path, ...responsePath], tempMap);

            // requestBody;
            const requestBodyPath = ['requestBody', 'content', mediaType, 'schema'];
            const requestBody = get(methodData, requestBodyPath) || {};
            getQuotePath(requestBody, [..._path, ...requestBodyPath], tempMap);
          });

          // parameters
          const parametersData = methodData.parameters || [];
          forEach(parametersData, (pData, index) => {
            getQuotePath(pData, [..._path, 'parameters', index], tempMap);
          });
        });
      });

      // datatype中的引用
      const dataTypeData = data?.components?.schemas || {};
      forEach(keys(dataTypeData), (dataTypeName) => {
        getQuotePath(dataTypeData[dataTypeName], ['components', 'schemas', dataTypeName], tempMap);
      });
      updater.quotePathMap(tempMap);
      return tempMap;
    },
    [updater],
  );

  const onQuotePathMapChange = React.useCallback(
    (pathMap: Obj) => {
      updater.quotePathMap(pathMap);
    },
    [updater],
  );

  const onApiNameChange = React.useCallback(
    (name: string) => {
      updater.curApiName(name);
    },
    [updater],
  );

  const renderContent = (key: string) => {
    if (key.startsWith('RESOURCE')) {
      return (
        <ApiResource
          onQuoteChange={onQuotePathMapChange}
          onApiNameChange={onApiNameChange}
          quotePathMap={quotePathMap}
          apiName={curApiName}
          apiDetail={apiDetail}
        />
      );
    } else if (key.startsWith('DATATYPE')) {
      return (
        <DataTypeConfig
          quotePathMap={quotePathMap}
          dataTypeNameMap={dataTypeNameMap}
          formData={dataTypeFormData}
          key={dataTypeFormData?.name}
          dataType={curDataType}
          onQuoteNameChange={onQuotePathMapChange}
          onDataTypeNameChange={(name) => updater.curDataType(name)}
          isEditMode={!apiLockState}
        />
      );
    } else {
      return <ApiSummary />;
    }
  };

  const isDocLocked = React.useMemo(() => {
    return wsQuery?.sessionID && apiLockState;
  }, [apiLockState, wsQuery]);

  const LockTipVisible = React.useMemo(() => isApiReadOnly || isDocLocked, [isApiReadOnly, isDocLocked]);

  const docLockTip = React.useMemo(() => {
    if (isApiReadOnly) {
      return i18n.t('dop:protect branch, not editable');
    } else if (isDocLocked) {
      return lockUser + API_LOCK_WARNING;
    } else {
      return '';
    }
  }, [isApiReadOnly, isDocLocked, lockUser]);

  const errorData = React.useMemo(() => {
    return {
      branchName: curTreeNodeData.branchName,
      docName: `${curTreeNodeData.apiDocName}.yaml`,
      msg: docValidData.msg,
    };
  }, [curTreeNodeData, docValidData]);

  const onEditDocHandle = () => {
    if (!apiWs) {
      initApiWs({ inode: inodeQuery, pinode: pinodeQuery });
    } else if (isDocLocked) {
      message.warning(lockUser + API_LOCK_WARNING);
    }
  };

  const onPublishApi = React.useCallback(
    (values: any) => {
      publishApi(values).then(() => {
        apiWs && apiWs.close();
        getApiDetail(inodeQuery as string).then((data: any) => {
          getQuoteMap(data.openApiDoc);
          updater.curTreeNodeData({
            ...curTreeNodeData,
            asset: data.asset,
          });
        });
      });
    },
    [apiWs, curTreeNodeData, getApiDetail, getQuoteMap, inodeQuery, publishApi, updater],
  );

  const onSelectDoc = React.useCallback(
    (nodeData, reset) => {
      if (reset) {
        updateOpenApiDoc({});
        resetDocValidData();
      }
      onContentChange('Summary');
      update({
        contentKey: 'SUMMARY',
        curTreeNodeData: nodeData,
        newTreeNode: {} as API_SETTING.IFileTree,
        filterKey: '',
      });
    },
    [onContentChange, resetDocValidData, update, updateOpenApiDoc],
  );

  const onToggleTreeVisible = React.useCallback(
    (val: boolean) => {
      updater.popVisible(val);
    },
    [updater],
  );

  const onConfirmPublish = React.useCallback(() => {
    if (isDocChanged) {
      confirm({
        title: i18n.t('dop:The current document has not been saved. Publish the saved document?'),
        onOk() {
          updater.apiModalVisible(true);
        },
      });
    } else {
      updater.apiModalVisible(true);
    }
  }, [isDocChanged, updater]);

  const showErrorDocTip = React.useMemo(() => {
    return !docValidData.valid && !isDocChanged && !isEmpty(openApiDoc);
  }, [docValidData.valid, isDocChanged, openApiDoc]);

  return isExternalRepo === undefined ? (
    <EmptyHolder relative />
  ) : (
    <div className="api-design">
      <div className="top-button-group">
        <Button type="primary" onClick={() => updater.treeModalVisible(true)}>
          {i18n.t('dop:new document')}
        </Button>
      </div>
      <div className="api-design-wrap">
        <div className="search-wrap mb-4 flex items-center justify-start">
          <ApiDocTree
            treeNodeData={curTreeNodeData}
            newTreeNode={newTreeNode}
            getQuoteMap={getQuoteMap}
            onSelectDoc={onSelectDoc}
            popVisible={popVisible}
            onVisibleChange={onToggleTreeVisible}
          />
          {LockTipVisible && (
            <span className="ml-4">
              <CustomIcon type="lock" />
              {docLockTip}
            </span>
          )}
          {showErrorDocTip && <ErrorPopover {...errorData} />}
          {inodeQuery && !isEmpty(curTreeNodeData) && (
            <div className="flex items-center flex-wrap justify-end flex-1">
              {!apiWs || isDocLocked ? (
                <WithAuth pass={!isApiReadOnly && docValidData.valid}>
                  <Button type="ghost" onClick={onEditDocHandle}>
                    {i18n.t('edit')}
                  </Button>
                </WithAuth>
              ) : (
                <Button type="ghost" disabled={formErrorNum > 0} onClick={() => commitSaveApi()}>
                  {i18n.t('save')}
                </Button>
              )}
              <WithAuth pass={inodeQuery && docValidData.valid}>
                <Button type="primary" className="ml-2" onClick={onConfirmPublish}>
                  {i18n.t('publisher:release')}
                </Button>
              </WithAuth>
            </div>
          )}
        </div>
        <Spin spinning={getApiDocDetailLoading || commitSaveApiLoading || getTreeListLoading}>
          {isEmpty(openApiDoc) ? (
            <ErrorEmptyHolder {...errorData} isLoading={getTreeListLoading} />
          ) : (
            <div className="api-design-content">
              <div className="api-design-content-list flex flex-col justify-start">
                <Input
                  placeholder={i18n.t('search by keyword')}
                  className="mx-2 my-3 api-filter-input"
                  prefix={<ErdaIcon type="search1" size="14" className="mr-0.5 mt-0.5" />}
                  onInput={(e: React.ChangeEvent<HTMLInputElement>) => updater.filterKey(e.target.value)}
                />

                <div
                  className={`list-title py-3 border-bottom font-bold ${
                    contentKey === 'SUMMARY' ? 'list-title-active' : ''
                  }`}
                  onClick={() => onContentChange('SUMMARY')}
                >
                  {i18n.t('dop:API overview')}
                </div>
                <div className="panel-list">
                  <Collapse
                    accordion
                    bordered={false}
                    defaultActiveKey={['RESOURCE']}
                    className="api-overview-collapse"
                  >
                    <Panel header={renderPanelHead('RESOURCE')} key="RESOURCE">
                      {!isEmpty(apiResourceList) ? (
                        map(apiResourceList, (name) => renderListItem('RESOURCE', name))
                      ) : (
                        <EmptyHolder relative />
                      )}
                    </Panel>
                    <Panel header={renderPanelHead('DATATYPE')} key="DATATYPE">
                      {!isEmpty(apiDataTypeList) ? (
                        map(apiDataTypeList, (name) => renderListItem('DATATYPE', name))
                      ) : (
                        <EmptyHolder relative />
                      )}
                    </Panel>
                  </Collapse>
                </div>
              </div>
              <div className="api-design-content-detail px-4 py-3">{renderContent(contentKey)}</div>
            </div>
          )}
        </Spin>
        <ApiDocAddModal
          visible={treeModalVisible}
          onClose={() => updater.treeModalVisible(false)}
          onSubmit={onCreateDoc}
        />
        <ApiPublishModal
          visible={apiModalVisible}
          treeNodeData={curTreeNodeData as API_SETTING.ITreeNodeData}
          onSubmit={onPublishApi}
          onClose={() => updater.apiModalVisible(false)}
        />
      </div>
      <Prompt
        when={isDocChanged}
        message={(location: Location) => {
          if (location.pathname.endsWith('apiDesign')) {
            return false;
          }
          return `${i18n.t('dop:not saved yet, confirm to leave')}?`;
        }}
      />
    </div>
  );
};

export default ApiDesign;
