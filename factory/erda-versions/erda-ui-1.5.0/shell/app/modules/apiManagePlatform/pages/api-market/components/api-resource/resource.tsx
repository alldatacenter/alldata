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
import { Icon as CustomIcon, FormBuilder, IFormExtendType } from 'common';
import { useUpdate } from 'common/use-hooks';
import { Input, Tabs, Button, message, Popconfirm, Select, Modal } from 'antd';
import i18n from 'i18n';
import apiDesignStore from 'apiManagePlatform/stores/api-design';
import { ResponseConfig } from './response-config';
import { QueryParamsConfig } from './query-params-config';
import { map, unset, isEmpty, set, get, keys, some, forEach } from 'lodash';
import {
  API_METHODS,
  INPUT_MAX_LENGTH,
  DEFAULT_PATH_PARAM,
  DEFAULT_RESPONSE,
  API_RESOURCE_TAB,
} from 'app/modules/apiManagePlatform/configs.ts';
import { produce } from 'immer';
import ResourceSummary from './resource-summary';
import ReactDOM from 'react-dom';
import './resource.scss';

const { confirm } = Modal;
const { Option } = Select;
const { Fields } = FormBuilder;
const { TabPane } = Tabs;
const pathParamReg = /{(\w+)}/g;

const ApiResource = (props: Merge<CP_API_RESOURCE.Props, API_SETTING.IResourceProps>) => {
  const { quotePathMap, onQuoteChange, apiName, apiDetail } = props;

  const [
    { currentMethod, apiMethodDetail, curTabKey, apiDisplayName, tempApiData, pathParams, open },
    updater,
    update,
  ] = useUpdate({
    currentMethod: API_METHODS.get as API_SETTING.ApiMethod,
    apiMethodDetail: {} as Obj,
    curTabKey: API_RESOURCE_TAB.Summary,
    apiDisplayName: '',
    tempApiData: {},
    pathParams: null,
    open: false,
  });

  const { apiData, execOperation, operations } = props?.data || {};
  const formRef = React.useRef<IFormExtendType>({} as any);

  const [openApiDoc, apiLockState, formErrorNum] = apiDesignStore.useStore((s) => [
    s.openApiDoc,
    s.apiLockState,
    s.formErrorNum,
  ]);
  const { updateOpenApiDoc, updateFormErrorNum } = apiDesignStore;

  const dataPath = React.useMemo(() => [apiName, currentMethod], [apiName, currentMethod]);

  React.useEffect(() => {
    if (apiData?.apiMethod) {
      // 适配组件化协议的内容
      updater.apiMethodDetail(apiData);
      updater.tempApiData(!isEmpty(tempApiData) ? tempApiData : apiData);
    } else {
      // 点击左侧api列表导致的内容变化，更新第一个不为空的method，更新resource内容，resource内容切换到summary
      let initialMethod = API_METHODS.get;
      some(API_METHODS, (method) => {
        if (!isEmpty(apiDetail[method])) {
          initialMethod = method;
          return true;
        } else {
          return false;
        }
      });
      updater.currentMethod(initialMethod);

      const _apiMethodDetail = apiDetail[initialMethod] || {};
      updater.apiMethodDetail(_apiMethodDetail);
    }
    updater.pathParams(null);
    updater.curTabKey(API_RESOURCE_TAB.Summary); // API切换后重置tab
  }, [apiDetail, apiData, updater, tempApiData]);

  React.useEffect(() => {
    let _name = '';
    if (apiData) {
      _name = tempApiData.apiName || apiData?.apiName;
    } else if (apiName) {
      _name = apiName;
    }
    if (_name) {
      updater.apiDisplayName(_name);
      setTimeout(() => {
        formRef.current.setFieldsValue({ apiName: _name });
      });
    }
  }, [apiData, apiName, tempApiData.apiName, updater]);

  React.useEffect(() => {
    if (!pathParams) return;

    const prefixPath = !apiData ? ['paths', apiName] : [];
    const tempDetail = produce(openApiDoc, (draft) => {
      if (!isEmpty(pathParams)) {
        const _pathParams = map(pathParams, (name) => {
          return {
            ...DEFAULT_PATH_PARAM,
            name,
          };
        });
        set(draft, [...prefixPath, 'parameters'], _pathParams);
      } else {
        set(draft, [...prefixPath, 'parameters'], []);
        updater.pathParams(null);
      }
    });
    updateOpenApiDoc(tempDetail);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [apiData, apiName, pathParams, updateOpenApiDoc]);

  const setFieldHandle = React.useCallback(
    (key: string, fieldData: Obj, extraProps?: Obj) => {
      const prefixPath = !apiData?.apiMethod ? ['paths', ...dataPath] : [];
      const baseFormData = !apiData?.apiMethod ? openApiDoc : tempApiData;

      if (onQuoteChange && extraProps?.typeQuotePath) {
        const newTypeQuotePath = extraProps?.typeQuotePath ? ['paths', ...dataPath, ...extraProps.typeQuotePath] : [];

        const tempQuotePathMap = produce(quotePathMap, (draft) => {
          if (extraProps?.quoteTypeName) {
            const { quoteTypeName } = extraProps;
            draft[quoteTypeName] = draft[quoteTypeName] || [];
            draft[quoteTypeName].push(newTypeQuotePath);
          }
        });
        onQuoteChange(tempQuotePathMap);
      }

      const tempDetail = produce(baseFormData, (draft) => {
        if (key !== 'responses') {
          const responseData = get(draft, [...prefixPath, 'responses']);
          // 设置默认的response
          if (!responseData) {
            set(draft, [...prefixPath, 'responses'], DEFAULT_RESPONSE);
          }

          if (key === 'summary') {
            set(draft, [...prefixPath, fieldData?.propertyName], fieldData?.propertyData);
            if (fieldData?.propertyName === 'operationId') {
              set(draft, [...prefixPath, 'summary'], fieldData?.propertyData);
            }
            if (fieldData?.newTags) {
              set(draft, 'tags', fieldData?.newTags);
              message.success(i18n.t('dop:category created successfully'));
            }
          } else if (key === 'query' || key === 'header') {
            set(draft, [...prefixPath, 'parameters'], fieldData?.parameters);
          }
        }
        if (key === 'responses' || key === 'requestBody') {
          set(draft, [...prefixPath, key], fieldData[key]);
        }
        // 设置默认的operationId
        if (!get(draft, [...prefixPath, 'operationId'])) {
          const _operationIdList: string[] = [];
          const { paths } = openApiDoc;
          forEach(keys(paths), (pathName: string) => {
            const methodData = paths[pathName];
            forEach(keys(methodData), (item) => {
              methodData[item]?.operationId && _operationIdList.push(methodData[item]?.operationId);
            });
          });
          let _operationId = 'operationId';
          while (_operationIdList.includes(_operationId)) {
            _operationId += '1';
          }
          set(draft, [...prefixPath, 'operationId'], _operationId);
        }
        // 设置默认的tags
        if (!get(draft, [...prefixPath, 'tags'])) {
          set(draft, [...prefixPath, 'tags'], ['other']);
        }
        if (!draft.tags) {
          set(draft, 'tags', [{ name: 'other' }]);
        }
      });

      if (!apiData?.apiMethod) {
        updateOpenApiDoc(tempDetail);
      } else {
        updater.tempApiData(tempDetail);
      }
    },
    [apiData, dataPath, onQuoteChange, openApiDoc, quotePathMap, tempApiData, updateOpenApiDoc, updater],
  );

  const iconClassMap = React.useMemo(() => {
    const classMap = {};
    const emptyIcon = {};
    forEach(API_METHODS, (method) => {
      const tempMethodDetail = get(openApiDoc, ['paths', apiName, method]);
      const emptyMethodClass = !tempMethodDetail || isEmpty(tempMethodDetail) ? 'btn-icon-empty' : '';
      classMap[method] = `btn-icon btn-icon-${method} ${emptyMethodClass}`;
      emptyIcon[method] = `${emptyMethodClass}`;
    });
    return { classMap, emptyIcon };
  }, [apiName, openApiDoc]);

  const deleteMethod = React.useCallback(
    (methodKey: API_METHODS) => {
      updater.open(false);
      const tempDetail = produce(openApiDoc, (draft) => {
        unset(draft, ['paths', apiName, methodKey]);
      });
      updateOpenApiDoc(tempDetail);
      if (currentMethod === methodKey) {
        updater.apiMethodDetail({});
      }
    },
    [apiName, currentMethod, openApiDoc, updateOpenApiDoc, updater],
  );

  const onApiNameChange = React.useCallback(
    (name: string) => {
      updater.apiDisplayName(name);
      props.onApiNameChange(name);

      // 获取api中的path parameters
      const _pathParams = map(name.match(pathParamReg), (item) => {
        return item.slice(1, item.length - 1);
      });
      updater.pathParams(_pathParams);

      if (onQuoteChange && !isEmpty(quotePathMap)) {
        const tempQuotePathMap = produce(quotePathMap, (draft) => {
          forEach(keys(draft), (k) => {
            forEach(draft[k], (path, i) => {
              if (path.includes(apiDisplayName)) {
                const oldPathArray = path.slice(0, path.length - 1);
                draft[k][i] = [...oldPathArray, name];
              }
            });
          });
        });
        onQuoteChange(tempQuotePathMap);
      }

      if (!apiData?.apiMethod) {
        const tempDetail = produce(openApiDoc, (draft) => {
          const apiTempData = get(draft, ['paths', apiName]);
          set(draft, ['paths', name], apiTempData);
          unset(draft, ['paths', apiName]);
        });
        updateOpenApiDoc(tempDetail);
      } else {
        const tempDetail = produce(tempApiData, (draft) => {
          set(draft, 'apiName', name);
        });
        updater.tempApiData(tempDetail);
      }
    },
    [
      apiData,
      apiDisplayName,
      apiName,
      onQuoteChange,
      openApiDoc,
      props,
      quotePathMap,
      tempApiData,
      updateOpenApiDoc,
      updater,
    ],
  );

  const hasBody = !['get', 'head'].includes(currentMethod);

  const onSaveApiData = React.useCallback(() => {
    execOperation &&
      execOperation(operations.submit, {
        apiData: { ...tempApiData, apiMethod: apiData?.apiMethod, apiName: tempApiData?.name || apiData?.apiName },
      });
  }, [apiData, execOperation, operations, tempApiData]);

  const fieldList = React.useMemo(() => {
    const existApiPathNames = keys(openApiDoc?.paths).filter((n) => n !== apiDisplayName);
    return [
      {
        type: Input,
        name: 'apiName',
        colSpan: 24,
        required: false,
        isHoldLabel: false,
        wrapperClassName: 'pl-0',
        customProps: {
          className: 'name-input',
          maxLength: INPUT_MAX_LENGTH,
          disabled: apiLockState,
          addonBefore: apiData?.apiMethod,
          placeholder: i18n.t('please enter {name}', { name: i18n.t('API path') }),
          onChange: (e: React.ChangeEvent<HTMLInputElement>) => {
            const newApiName = e.target.value;

            if (newApiName && !existApiPathNames.includes(newApiName) && newApiName.startsWith('/')) {
              onApiNameChange(newApiName);
              updateFormErrorNum(0);
            } else {
              updateFormErrorNum(1);
            }
          },
        },
        rules: [
          {
            validator: (_rule: any, value: string, callback: (msg?: string) => void) => {
              if (existApiPathNames.includes(value)) {
                callback(i18n.t('the same {key} exists', { key: i18n.t('name') }));
              } else if (!value) {
                callback(i18n.t('can not be empty'));
              } else if (!value.startsWith('/')) {
                callback(i18n.t('dop:path must start with /'));
              } else {
                callback();
              }
            },
          },
        ],
      },
    ];
  }, [apiData, apiDisplayName, apiLockState, onApiNameChange, openApiDoc, updateFormErrorNum]);

  const popconfirmRef = React.useRef(null as any);
  const selectRef = React.useRef(null) as any;

  React.useEffect(() => {
    const selectHide = (e: any) => {
      // 点击外部，隐藏选项
      // eslint-disable-next-line react/no-find-dom-node
      const el2 = ReactDOM.findDOMNode(selectRef.current) as HTMLElement;
      if (!(el2 && el2.contains(e.target))) {
        updater.open(false);
        document.body.removeEventListener('click', selectHide);
      }
    };
    if (open) {
      document.body.addEventListener('click', selectHide);
    }
  }, [open, selectRef, updater]);

  const maskClick = React.useCallback(
    (e: any) => {
      e.stopPropagation();
      updater.open(true);
    },
    [updater],
  );

  const labelClick = React.useCallback(
    (e: any, methodKey: string) => {
      e.stopPropagation();
      updater.open(false);

      const nextHandle = () => {
        const _apiMethodDetail = get(openApiDoc, ['paths', apiName, methodKey]) || {};
        update({
          currentMethod: methodKey as API_SETTING.ApiMethod,
          curTabKey: API_RESOURCE_TAB.Summary,
          apiMethodDetail: _apiMethodDetail,
        });

        updateFormErrorNum(0);
        formRef.current.setFieldsValue({ apiName });
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
    [apiName, formErrorNum, openApiDoc, update, updateFormErrorNum, updater],
  );

  const renderSelectMenu = () => {
    return (
      <div className="select-container" ref={selectRef}>
        {!apiData?.apiMethod ? (
          <Select
            style={{ marginRight: '8px', width: '141px' }}
            defaultValue={currentMethod}
            open={open}
            value={currentMethod}
          >
            {map(API_METHODS, (methodKey) => {
              const item = (
                <div className="circle-container flex flex-wrap justify-center items-center">
                  {iconClassMap.emptyIcon[methodKey] ? (
                    <div className={`${iconClassMap.classMap[methodKey]} disableIcon`} />
                  ) : (
                    <CustomIcon type="duigou" className={iconClassMap.classMap[methodKey]} />
                  )}
                </div>
              );
              return (
                <Option value={methodKey} key={methodKey}>
                  <div
                    className={`api-method-option ${currentMethod === methodKey ? 'api-method-option-active' : ''}`}
                    key={methodKey}
                  >
                    <div
                      className="btn-label"
                      onClick={(e) => {
                        labelClick(e, methodKey);
                      }}
                    >
                      {methodKey.toUpperCase()}
                    </div>
                    {get(openApiDoc, ['paths', apiName, methodKey]) ? (
                      <Popconfirm
                        title={`${i18n.t('common:confirm deletion')}?`}
                        onConfirm={() => deleteMethod(methodKey)}
                        disabled={apiLockState}
                        overlayClassName="popconfirm-container"
                        getPopupContainer={() => popconfirmRef?.current}
                        onCancel={(e: any) => {
                          e.stopPropagation();
                          updater.open(false);
                        }}
                      >
                        <span>{item}</span>
                      </Popconfirm>
                    ) : (
                      <span>{item}</span>
                    )}
                  </div>
                </Option>
              );
            })}
          </Select>
        ) : undefined}
        <div className="mask" onClick={maskClick} />
      </div>
    );
  };

  const onTabChange = (tabKey: string) => {
    const nextHandle = () => {
      updater.curTabKey(tabKey as API_RESOURCE_TAB);
      const _apiMethodDetail = get(openApiDoc, ['paths', apiName, currentMethod]) || {};
      updater.apiMethodDetail(_apiMethodDetail);
      updateFormErrorNum(0);
      formRef.current.setFieldsValue({ apiName });
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
  };

  return (
    <div className="api-resource" ref={popconfirmRef}>
      <div className="popover">
        {renderSelectMenu()}
        <FormBuilder ref={formRef} className="w-full">
          <Fields fields={fieldList} />
        </FormBuilder>
      </div>

      <div className="api-resource-tabs">
        <Tabs activeKey={curTabKey} onChange={onTabChange}>
          <TabPane tab={API_RESOURCE_TAB.Summary} key={API_RESOURCE_TAB.Summary}>
            <ResourceSummary formData={apiMethodDetail} onChange={setFieldHandle} isEditMode={!apiLockState} />
          </TabPane>
          <TabPane tab={API_RESOURCE_TAB.Params} key={API_RESOURCE_TAB.Params}>
            <QueryParamsConfig
              formData={apiMethodDetail}
              paramIn="query"
              onChange={setFieldHandle}
              isEditMode={!apiLockState}
              resourceKey={curTabKey}
            />
          </TabPane>
          <TabPane tab={API_RESOURCE_TAB.Headers} key={API_RESOURCE_TAB.Headers}>
            <QueryParamsConfig
              formData={apiMethodDetail}
              paramIn="header"
              onChange={setFieldHandle}
              isEditMode={!apiLockState}
              resourceKey={curTabKey}
            />
          </TabPane>
          <TabPane tab={API_RESOURCE_TAB.Body} key={API_RESOURCE_TAB.Body} disabled={!hasBody}>
            {hasBody && (
              <ResponseConfig
                formData={apiMethodDetail}
                paramIn="requestBody"
                onChange={setFieldHandle}
                dataPath={dataPath}
                isEditMode={!apiLockState}
                resourceKey={curTabKey}
              />
            )}
          </TabPane>
          <TabPane tab={API_RESOURCE_TAB.Response} key={API_RESOURCE_TAB.Response}>
            <ResponseConfig
              formData={apiMethodDetail}
              paramIn="responses"
              onChange={setFieldHandle}
              dataPath={dataPath}
              isEditMode={!apiLockState}
              resourceKey={curTabKey}
            />
          </TabPane>
          {apiData?.apiMethod && <TabPane tab={API_RESOURCE_TAB.Test} key={API_RESOURCE_TAB.Test} />}
        </Tabs>
        {apiData?.apiMethod && (
          <div className="flex items-center flex-wrap justify-end">
            <Button type="primary" onClick={onSaveApiData}>
              {i18n.t('save')}
            </Button>
          </div>
        )}
      </div>
    </div>
  );
};

export default ApiResource;
