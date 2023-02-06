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
import { map, uniqueId } from 'lodash';
import { FormModal, KeyValueTable, FileEditor, ErdaIcon } from 'common';
import { useUpdate } from 'common/use-hooks';
import { regRules, qs } from 'common/utils';
import monitorStatusStore from 'status-insight/stores/status';
import routeInfoStore from 'core/stores/route';
import i18n from 'i18n';
import constants from './constants';
import { Modal } from 'antd';
import './add-modal.scss';
import { Input, Select, Radio, Tabs, Form, Tooltip, Button, InputNumber } from 'antd';
import { FormInstance } from 'core/common/interface';
import { produce } from 'immer';

const { Option } = Select;
const { TextArea } = Input;
const { TabPane } = Tabs;
const { HTTP_METHOD_LIST, TIME_LIMITS, OPERATORS, CONTAINS } = constants;
// const transToRegList = (regs: any) => regs.map((item: any) => ({ name: uniqueId('reg_'), reg: item }));

interface IProps {
  formData: any;
  modalVisible: boolean;
  afterSubmit: (args?: any) => Promise<any>;
  toggleModal: (args?: any) => void;
}
interface ITrigger {
  key: string;
  operate: string;
  value: number | string;
}

interface IContent {
  _tb_key: string;
  _tb_value: string;
  uniKey: string;
}
interface IState {
  showMore: boolean;
  retry: number;
  frequency: number;
  apiMethod: string;
  body: {
    content: IContent[] | string;
    type: string;
  };
  headers: Obj;
  url: string;
  query: Obj;
  condition: ITrigger[];
  bodyType: string;
  textOrJson: string;
}

const formType = 'x-www-form-urlencoded';
const noneType = 'none';
const jsonType = 'application/json';
const textType = 'text/plain';
const raw = 'raw';
const json = 'json';
const text = 'text';

const convertType = (type: string) => {
  const newType = '';
  if (type === formType || type === noneType) {
    return newType;
  } else if (type === jsonType) {
    return json;
  } else {
    return text;
  }
};

const convertFormData = (_formData?: Obj) => {
  if (_formData) {
    const handleContent = [];
    const qsContent = qs.parse(_formData?.config?.body?.content);
    for (const [key, val] of Object.entries(qsContent)) {
      handleContent.push({
        _tb_key: key,
        _tb_value: val?.toString(),
        uniKey: uniqueId(),
      });
    }

    return {
      retry: _formData.config?.retry || 2,
      bodyType:
        _formData.config?.body?.type === jsonType || _formData.config?.body?.type === textType
          ? raw
          : _formData.config?.body?.type,
      frequency: _formData.config?.interval || TIME_LIMITS[0],
      apiMethod: _formData.config?.method || HTTP_METHOD_LIST[0],
      body:
        _formData.config?.body?.type === formType
          ? { ..._formData.config?.body, content: handleContent }
          : _formData.config?.body || {
              content: '',
              type: noneType,
            },
      headers: _formData.config?.headers || {},
      url: _formData.config?.url || '',
      query: qs.parseUrl(_formData.config?.url || '')?.query,
      condition: _formData.config?.triggering || [
        {
          key: 'http_code',
          operate: '>=',
          value: 400,
        },
      ],
      textOrJson: convertType(_formData.config?.body?.type),
    };
  } else {
    return {
      condition: [
        {
          key: 'http_code',
          operate: '>=',
          value: 400,
        },
      ],
      showMore: false,
      query: {},
      retry: 2,
      frequency: TIME_LIMITS[0],
      apiMethod: HTTP_METHOD_LIST[0],
      body: { content: '', type: 'none' },
      headers: {},
      url: '',
      textOrJson: 'text',
      bodyType: 'none',
    };
  }
};

const AddModal = (props: IProps) => {
  const { formData, modalVisible, afterSubmit, toggleModal } = props;
  const { env, terminusKey, projectId } = routeInfoStore.useStore((s) => s.params);
  const { saveService, updateMetric } = monitorStatusStore.effects;
  const [form] = Form.useForm();
  const formRef = React.useRef<FormInstance>(null);
  const [
    { showMore, retry, frequency, apiMethod, body, headers, url, query, condition, bodyType, textOrJson },
    updater,
    update,
  ] = useUpdate<IState>({
    showMore: false,
    ...convertFormData(formData),
  });

  React.useEffect(() => {
    if (!modalVisible) {
      update(convertFormData());
    } else {
      update(convertFormData(formData));
    }
  }, [modalVisible]);

  const deleteItem = (index: number) => {
    const newCondition = produce(condition, (draft) => {
      draft.splice(index, 1);
    });
    updater.condition(newCondition);
  };

  const addItem = () => {
    updater.condition([
      ...condition,
      {
        key: 'http_code',
        operate: '>=',
        value: '',
      },
    ]);
  };

  const setInputValue = (index: number, value: number | string) => {
    const newCondition = produce(condition, (draft) => {
      draft[index].value = value;
    });
    updater.condition(newCondition);
  };

  const setOperator = (index: number, operate: string) => {
    const newCondition = produce(condition, (draft) => {
      draft[index].operate = operate;
    });
    updater.condition(newCondition);
  };

  const formatBody = () => {
    try {
      body.content = JSON.stringify(JSON.parse(body.content), null, 2);
      updater.body({ ...body });
      // eslint-disable-next-line no-empty
    } catch (e) {}
  };

  const setUrlParams = (queryConfig: Obj) => {
    formRef.current?.setFieldsValue({
      config: {
        url: `${formRef.current?.getFieldValue(['config', 'url']).split('?')[0]}?${qs.stringify(queryConfig)}`,
      },
    });
  };

  const handleSubmit = (_data: MONITOR_STATUS.IMetricsBody) => {
    const { mode, name, id } = _data;
    const newObj = {};
    if (Array.isArray(body.content)) {
      body.content.forEach((item) => {
        newObj[item._tb_key] = item._tb_value;
      });
    }
    if (id) {
      updateMetric({
        id,
        env,
        mode,
        name,
        projectId,
        tenantId: terminusKey,
        config: {
          url: _data.config?.url,
          retry,
          interval: frequency,
          headers,
          body: Array.isArray(body.content) ? { ...body, content: qs.stringify(newObj) } : body,
          method: apiMethod,
          triggering: condition,
        },
      }).then(afterSubmit);
    } else {
      saveService({
        id,
        env,
        mode,
        name,
        projectId,
        tenantId: terminusKey,
        config: {
          retry,
          interval: frequency,
          headers,
          url: _data.config?.url,
          body: Array.isArray(body.content) ? { ...body, content: qs.stringify(newObj) } : body,
          method: apiMethod,
          triggering: condition,
        },
      }).then(() => {
        afterSubmit();
      });
    }
    toggleModal();
  };

  React.useEffect(() => {
    switch (textOrJson) {
      case text:
        updater.body({ ...body, type: textType });
        updater.headers({
          ...headers,
          'Content-Type': textType,
        });
        break;
      case json:
        updater.body({ ...body, type: jsonType });
        updater.headers({
          ...headers,
          'Content-Type': jsonType,
        });
        break;
      default:
        break;
    }

    switch (bodyType) {
      case noneType:
        updater.textOrJson('');
        updater.body({ content: '', type: noneType });
        break;
      case formType:
        updater.textOrJson('');
        updater.body({ ...body, type: formType });
        updater.headers({
          ...headers,
          'Content-Type': formType,
        });
        break;
      default:
        break;
    }
  }, [textOrJson, bodyType]);

  const fieldsList = [
    {
      name: 'id',
      itemProps: {
        type: 'hidden',
      },
    },
    {
      name: 'env',
      initialValue: env,
      itemProps: {
        type: 'hidden',
      },
    },
    {
      label: i18n.t('msp:checking method'),
      name: 'mode',
      type: 'radioGroup',
      options: [
        {
          value: 'http',
          name: 'http',
        },
      ],
      initialValue: 'http',
    },
    {
      label: i18n.t('name'),
      name: 'name',
    },
    {
      label: 'URL',
      name: ['config', 'url'],
      rules: [{ ...regRules.http }],
      itemProps: {
        addonBefore: (
          <Select
            value={apiMethod}
            onChange={(value) => {
              updater.apiMethod(value);
            }}
            style={{ width: 110 }}
          >
            {map(HTTP_METHOD_LIST, (method) => (
              <Option value={method} key={method}>
                {method}
              </Option>
            ))}
          </Select>
        ),
        onBlur: () => {
          updater.query(qs.parseUrl(formRef.current?.getFieldValue(['config', 'url']))?.query);
        },
      },
    },
    {
      name: 'settings',
      getComp: () => {
        return (
          <div className="h-full">
            <Tabs
              onChange={(key: string) => {
                if (key === '2' && bodyType === raw && textOrJson === text) {
                  updater.headers({ ...headers, 'Content-Type': textType });
                }
              }}
              defaultActiveKey="1"
            >
              <TabPane tab="Params" key="1">
                <KeyValueTable
                  isTextArea={false}
                  data={query}
                  form={form}
                  onChange={setUrlParams}
                  onDel={setUrlParams}
                />
              </TabPane>
              <TabPane tab="Headers" key="2">
                <KeyValueTable
                  isTextArea={false}
                  onChange={(header) => {
                    updater.headers(header);
                  }}
                  onDel={(header) => {
                    updater.headers(header);
                  }}
                  data={headers}
                  form={form}
                />
              </TabPane>
              <TabPane tab="Body" key="3">
                <Radio.Group
                  onChange={(e) => {
                    if (e.target.value === noneType) {
                      if (body.content.length > 0) {
                        Modal.confirm({
                          title: i18n.t('confirm to switch Body type?'),
                          onOk() {
                            updater.bodyType(e.target.value);
                          },
                        });
                      } else {
                        updater.bodyType(e.target.value);
                      }
                    }
                    if (e.target.value === formType) {
                      if (bodyType !== noneType && body.content !== '') {
                        Modal.confirm({
                          title: i18n.t('confirm to switch Body type?'),
                          onOk() {
                            updater.bodyType(e.target.value);
                            updater.body({ ...body, content: [] });
                          },
                        });
                      } else {
                        updater.bodyType(e.target.value);
                        updater.body({ ...body, content: [] });
                      }
                    }
                    if (e.target.value === raw) {
                      if (bodyType !== noneType && body.content.length > 0) {
                        Modal.confirm({
                          title: i18n.t('confirm to switch Body type?'),
                          onOk() {
                            updater.bodyType(e.target.value);
                            body.content = '';
                            updater.body({ ...body });
                            updater.textOrJson('text');
                          },
                        });
                      } else {
                        updater.bodyType(e.target.value);
                        body.content = '';
                        updater.body({ ...body });
                        updater.textOrJson('text');
                      }
                    }
                  }}
                  value={bodyType}
                >
                  <Radio value={'none'}>none</Radio>
                  <Radio value={'x-www-form-urlencoded'}>x-www-form-urlencoded</Radio>
                  <Radio value={'raw'}>raw</Radio>
                </Radio.Group>
                {bodyType === noneType ? (
                  <div className="p-6 text-center">{i18n.t('dop:the current request has no body')}</div>
                ) : null}
                {bodyType === formType ? (
                  <div className="mt-4">
                    <KeyValueTable
                      className="mb-2"
                      isTextArea={false}
                      onChange={(newBody: IContent[]) => {
                        updater.body({
                          ...body,
                          content: newBody,
                        });
                      }}
                      onDel={(newBody) => {
                        updater.body({
                          ...body,
                          content: newBody,
                        });
                      }}
                      data={body.content}
                      form={form}
                    />
                  </div>
                ) : null}
                {bodyType === raw ? (
                  <Select
                    value={textOrJson}
                    onChange={(v) => {
                      updater.textOrJson(v);
                    }}
                    style={{ width: 160 }}
                    className="mr-2"
                  >
                    <Option value="text">text/plain</Option>
                    <Option value="json">application/json</Option>
                  </Select>
                ) : null}
                {bodyType === raw && textOrJson === text ? (
                  <TextArea
                    value={body?.content}
                    onChange={(e) => {
                      body.content = e.target.value;
                      updater.body({ ...body });
                    }}
                    className="mt-4"
                    rows={10}
                  />
                ) : null}
                {textOrJson === json ? (
                  <Button onClick={formatBody} className="ml-2" size="small" type="primary">
                    {i18n.t('format')}
                  </Button>
                ) : null}
                {bodyType === raw && textOrJson === json ? (
                  <FileEditor
                    className="mt-4"
                    fileExtension="json"
                    value={body?.content}
                    minLines={8}
                    onChange={(v) => {
                      body.content = v;
                      updater.body({ ...body });
                    }}
                  />
                ) : null}
              </TabPane>
            </Tabs>
          </div>
        );
      },
    },
    {
      name: 'more',
      getComp: () => {
        return (
          <div>
            <span
              onClick={() => {
                updater.showMore(!showMore);
              }}
            >
              {i18n.t('advanced settings')}
              {showMore ? (
                <ErdaIcon className="align-middle" type="up" size="16" />
              ) : (
                <ErdaIcon className="align-middle" type="down" size="16" />
              )}
            </span>
            <div className={`p-2.5 mt-2 h-full ${showMore ? '' : 'hidden'}`}>
              <div className="flex">
                <h4 className="mb-2">{i18n.t('msp:anomaly check')}</h4>
                <Tooltip title={i18n.t('msp:exception check prompt')}>
                  <ErdaIcon type="help" size="14" className="ml-1 mb-2" />
                </Tooltip>
              </div>
              <div>
                {condition.length > 0
                  ? map(condition, (item, index) => {
                      return (
                        <div className="flex items-center mb-2">
                          <Select
                            value={item?.key}
                            onChange={(v) => {
                              if (v === 'http_code') {
                                const newCondition = produce(condition, (draft) => {
                                  draft[index].operate = '>=';
                                  draft[index].key = v;
                                });
                                updater.condition(newCondition);
                              } else {
                                const newCondition = produce(condition, (draft) => {
                                  draft[index].operate = 'contains';
                                  draft[index].key = v;
                                });
                                updater.condition(newCondition);
                              }
                            }}
                            style={{ width: 110 }}
                            className="mr-2"
                          >
                            <Option value="http_code">{i18n.t('cmp:state code')}</Option>
                            <Option value="body">{i18n.t('response body')}</Option>
                          </Select>
                          {item.key === 'http_code' ? (
                            <>
                              <Select
                                onChange={(v) => setOperator(index, v)}
                                style={{ width: 150 }}
                                value={item?.operate}
                                className="mr-2"
                                placeholder={i18n.t('dop:compare')}
                              >
                                {map(OPERATORS, (value, key) => (
                                  <Option value={key}>{value}</Option>
                                ))}
                              </Select>
                              <InputNumber
                                value={item?.value}
                                className="flex-1"
                                onChange={(v) => setInputValue(index, v)}
                              />
                            </>
                          ) : (
                            <>
                              <Select
                                onChange={(v) => setOperator(index, v)}
                                style={{ width: 150 }}
                                value={item?.operate}
                                className="mr-2"
                                placeholder={i18n.t('dop:compare')}
                              >
                                {map(CONTAINS, (val, key) => (
                                  <Option value={key}>{val}</Option>
                                ))}
                              </Select>
                              <Input
                                className="flex-1"
                                value={item?.value}
                                onChange={(e) => setInputValue(index, e.target.value)}
                              />
                            </>
                          )}
                          <div className="ml-4 delete-row-btn table-operations">
                            <span onClick={() => deleteItem(index)} className="table-operations-btn">
                              {i18n.t('delete')}
                            </span>
                          </div>
                        </div>
                      );
                    })
                  : null}
              </div>

              <Button ghost className="mt-4" size="small" type="primary" onClick={addItem}>
                {i18n.t('common:add')}
              </Button>
              <h4 className="mt-4 mb-3 text-sm">{i18n.t('msp:number of retries')}</h4>
              <InputNumber
                precision={0}
                value={retry}
                min={0}
                max={10}
                defaultValue={2}
                onChange={(v) => updater.retry(v)}
              />
              <h4 className="mt-5 mb-3 text-sm">{i18n.t('msp:monitoring frequency')}</h4>
              <Select
                style={{ width: 100 }}
                onChange={(v) => {
                  updater.frequency(v);
                }}
                value={frequency}
                defaultValue={TIME_LIMITS[0]}
              >
                {map(TIME_LIMITS, (time) => (
                  <Radio className="pr-10" value={time} key={time}>
                    {time < 60 ? `${time}${i18n.t('common:second(s)')}` : `${time / 60}${i18n.t('common:minutes')}`}
                  </Radio>
                ))}
              </Select>
            </div>
          </div>
        );
      },
    },
  ];

  return (
    <FormModal
      ref={formRef}
      className="h-4/5"
      width={620}
      title={formData ? i18n.t('msp:edit monitoring') : i18n.t('msp:add monitoring')}
      fieldsList={fieldsList}
      visible={modalVisible}
      formData={formData}
      onOk={handleSubmit}
      onCancel={toggleModal}
      modalProps={{
        destroyOnClose: true,
      }}
    />
  );
};

export default AddModal;
