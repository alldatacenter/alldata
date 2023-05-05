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

import { FileEditor, Icon as CustomIcon } from 'common';
import { qs } from 'common/utils';
import i18n from 'i18n';
import produce from 'immer';
import { cloneDeep, find, isArray, isEmpty, isString, map, reduce, reject, set } from 'lodash';
import { Button, Input, Popconfirm, Radio, Select, Tabs, message } from 'antd';
import React from 'react';
import './api-test.scss';

interface IProps {
  disabled?: boolean;
  value?: IApi;
  onChange: (api: IApi, adjustData?: Function) => any;
}
const formatJSON = (str: string) => {
  let res = str;
  try {
    res = JSON.stringify(JSON.parse(str), null, 2);
  } catch (e) {
    message.error(i18n.t('dop:the current input content is invalid JSON'));
  }
  return typeof res === 'string' ? res : '';
};
export interface IApi {
  headers: Array<{
    key: string;
    value: string;
    desc: string;
  }>;
  method: string;
  url: string;
  params: Array<{
    key: string;
    value: string;
    desc: string;
  }>;
  body: {
    type: string;
    content: string;
  };
  out_params: Array<{
    key: string;
    source: string;
    expression: string;
    matchIndex: number;
  }>;
  asserts: Array<{
    arg: string;
    operator: string;
    value: string;
  }>;
}
const { Option } = Select;
const { TabPane } = Tabs;
const { TextArea } = Input;
const HTTP_METHOD_LIST = ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS', 'PATCH', 'COPY', 'HEAD'];
const BODY_RAW_OPTION = ['Text', 'Text(text/plain)', 'application/json'];
export const getEmptyApi = () => {
  return {
    url: '',
    method: 'GET',
    headers: [],
    params: [],
    body: {
      type: 'none',
      content: '',
    },
    out_params: [],
    asserts: [],
  };
};
export const ApiItem = ({ value, onChange, disabled }: IProps) => {
  const [api, setValue] = React.useState(value || getEmptyApi());
  const updateApi = (k: string, v: string, adjustData?: Function) => {
    setValue((prev: IApi) => {
      const target = produce(prev, (draft) => {
        set(draft, k, v);
        switch (k) {
          // 同步url和params
          case 'url': {
            const { query } = qs.parseUrl(v, { arrayFormat: undefined }); // 使用a=b&a=c格式解析
            const paramList: any = [];
            map(query, (qv: string | string[], qk: string) => {
              if (Array.isArray(qv)) {
                qv.forEach((vs: string) => {
                  paramList.push({
                    key: qk,
                    value: vs,
                    desc: '',
                  });
                });
              } else {
                paramList.push({
                  key: qk,
                  value: qv,
                  desc: '',
                });
              }
            });
            set(draft, 'params', paramList);
            break;
          }
          case 'params': {
            const { url } = qs.parseUrl(draft.url);
            const queryStr: string[] = [];
            map(v, (item: any) => {
              if (item.key && item.value) {
                queryStr.push(`${item.key}=${item.value}`);
              }
            });
            set(draft, 'url', `${url}?${queryStr.join('&')}`);
            break;
          }
          default:
        }
        if (adjustData) {
          adjustData(draft, k);
        }
      });
      onChange(target);
      return target;
    });
  };
  return (
    <div className={'form-api-field'}>
      <div className="api-url">
        <Input
          disabled={disabled}
          addonBefore={
            <Select
              style={{ width: 110 }}
              value={api.method}
              disabled={disabled}
              onChange={(val) => updateApi('method', val as string)}
              placeholder={i18n.t('dop:please choose')}
            >
              {map(HTTP_METHOD_LIST, (method) => (
                <Option value={method} key={method}>
                  {method}
                </Option>
              ))}
            </Select>
          }
          className="url"
          placeholder={i18n.t('please enter')}
          value={api.url}
          onChange={(e) => updateApi('url', e.target.value.trim())}
        />
      </div>
      <div className="api-tabs">
        <Tabs defaultActiveKey="Params">
          {map(ApiTabComps, ({ Comp, dataKey }: any, tab) => {
            return (
              <TabPane tab={tab} key={tab}>
                <Comp
                  disabled={disabled}
                  data={
                    isArray(dataKey)
                      ? reduce(
                          dataKey,
                          (obj, k) => {
                            return { ...obj, [k]: api[k] };
                          },
                          {},
                        )
                      : api[dataKey]
                  }
                  onChange={updateApi}
                />
              </TabPane>
            );
          })}
        </Tabs>
      </div>
    </div>
  );
};
const Empty = () => null;
const dataModal = { key: '', value: '', desc: '' };
const ApiTabComps = {
  Params: {
    dataKey: 'params',
    Comp: (props: any) => {
      const { data, onChange, disabled } = props;
      return (
        <KeyValEdit
          type="params"
          data={data}
          disabled={disabled}
          dataModel={dataModal}
          dataMainKey="key"
          onChange={(val: any) => {
            onChange('params', val);
          }}
          itemMap={{
            key: {
              props: {
                placeholder: i18n.t('dop:parameter name'),
              },
              trim: true,
            },
            value: {
              props: {
                placeholder: i18n.t('dop:parameter value'),
              },
            },
            desc: {
              props: {
                placeholder: i18n.t('description'),
              },
            },
          }}
        />
      );
    },
  },
  Headers: {
    dataKey: 'headers',
    Comp: (props: any) => {
      const { data, onChange, disabled } = props;
      return (
        <KeyValEdit
          type="headers"
          data={data}
          disabled={disabled}
          dataModel={dataModal}
          dataMainKey="key"
          onChange={(val: any) => {
            onChange('headers', val);
          }}
          itemMap={{
            key: {
              props: {
                placeholder: i18n.t('dop:parameter name'),
              },
              trim: true,
            },
            value: {
              props: {
                placeholder: i18n.t('dop:parameter value'),
              },
            },
            desc: {
              props: {
                placeholder: i18n.t('description'),
              },
            },
          }}
        />
      );
    },
  },
  Body: {
    dataKey: 'body',
    Comp: (props: any) => <APIBody {...props} />,
  },
  Tests: {
    dataKey: ['out_params', 'asserts'],
    Comp: (props: any) => {
      const { data, onChange, disabled } = props;
      return (
        <div className="api-tables">
          <div className="table-title">{i18n.t('dop:output parameter')}</div>
          <div className="table-body">
            <KeyValEdit
              type="out_params"
              data={data.out_params}
              disabled={disabled}
              dataModel={{
                key: '',
                source: 'body:json',
                expression: '',
                matchIndex: '',
              }}
              dataMainKey="key"
              onChange={(val: any, adjustData?: Function) => {
                onChange('out_params', val, adjustData);
              }}
              itemMap={{
                key: {
                  props: {
                    placeholder: i18n.t('dop:output parameter name'),
                  },
                  trim: true,
                },
                source: {
                  Comp: (p: any) => {
                    const { value, onChange: onCurChange, className = '', disabled: propsDisabled } = p;
                    return (
                      <Select
                        value={value || undefined} // 没有值时显示placeholder
                        className={`${className} api-test-select`}
                        placeholder={i18n.t('dop:source')}
                        onChange={onCurChange}
                        disabled={propsDisabled}
                      >
                        <Option value="status">status</Option>
                        <Option value="header">Header:K/V</Option>
                        {/* <Option value="cookie">Cookie:K/V</Option> */}
                        <Option value="body:json">Body:JSON(body)</Option>
                      </Select>
                    );
                  },
                },
                expression: {
                  props: {
                    placeholder: 'example: .data.id',
                  },
                  getProps(record: any) {
                    if (record.source === 'status') {
                      return { disabled: true, value: 'status' };
                    }
                    return {};
                  },
                },
                matchIndex: {
                  // props: {
                  //   placeholder: '第几个匹配项',
                  // },
                  Comp: Empty,
                },
              }}
            />
          </div>
          <div className="table-title">{i18n.t('dop:assertion')}</div>
          <div className="table-body">
            <KeyValEdit
              type="asserts"
              data={data?.asserts || []}
              opList={[]}
              disabled={disabled}
              dataMainKey="arg"
              dataModel={{
                arg: '',
                operator: '',
                value: '',
              }}
              onChange={(val: any, adjustData?: Function) => {
                onChange('asserts', val, adjustData);
              }}
              itemMap={{
                arg: {
                  Comp: (p: any) => {
                    const { value, onChange: onCurChange, className = '' } = p;
                    return (
                      <Select
                        value={value || undefined} // 没有值时显示placeholder
                        className={`${className} api-test-select`}
                        placeholder={i18n.t('dop:parameter name')}
                        onChange={onCurChange}
                        disabled={disabled}
                      >
                        {data.out_params?.map((option: any) => {
                          return option.key === '' ? null : (
                            <Option key={option.key} value={option.key}>
                              {option.key}
                            </Option>
                          );
                        })}
                      </Select>
                    );
                  },
                },
                operator: {
                  Comp: (p: any) => {
                    const { value, onChange: onCurChange, className = '' } = p;
                    return (
                      <Select
                        value={value || undefined} // 没有值时显示placeholder
                        className={`${className} api-test-select`}
                        placeholder={i18n.t('dop:compare')}
                        onChange={onCurChange}
                        disabled={disabled}
                      >
                        <Option value=">">{i18n.t('dop:more than the')}</Option>
                        <Option value=">=">{i18n.t('dop:greater than or equal to')}</Option>
                        <Option value="=">{i18n.t('dop:equal to')}</Option>
                        <Option value="<=">{i18n.t('dop:less than or equal to')}</Option>
                        <Option value="<">{i18n.t('less than')}</Option>
                        <Option value="!=">{i18n.t('dop:not equal to')}</Option>
                        <Option value="contains">{i18n.t('dop:contains')}</Option>
                        <Option value="not_contains">{i18n.t('dop:does not contain')}</Option>
                        <Option value="exist">{i18n.t('dop:existence')}</Option>
                        <Option value="not_exist">{i18n.t('does not exist')}</Option>
                        <Option value="empty">{i18n.t('dop:is empty')}</Option>
                        <Option value="not_empty">{i18n.t('dop:not null')}</Option>
                        <Option value="belong">{i18n.t('dop:belongs to')}</Option>
                        <Option value="not_belong">{i18n.t('dop:does not belong to')}</Option>
                      </Select>
                    );
                  },
                },
                value: {
                  props: {
                    placeholder: i18n.t('value'),
                  },
                },
              }}
            />
          </div>
        </div>
      );
    },
  },
};
const TestJsonEditor = (props: any) => {
  const { data, updateBody, disabled }: any = props;
  const val = isString(data.content) ? `${data.content}` : '';
  const [content, setContent] = React.useState('');
  React.useEffect(() => {
    setContent(val);
  }, [val]);
  return (
    <div className="test-json-editor">
      <Button
        className="json-format-btn"
        size="small"
        onClick={() => {
          setContent(formatJSON(content));
        }}
      >
        {i18n.t('format')}
      </Button>
      <FileEditor
        fileExtension="json"
        value={content}
        readOnly={disabled}
        minLines={8}
        onChange={(value: string) => updateBody('content', value)}
        onLoad={(editor: any) => {
          editor.getSession().setUseWorker(false);
        }}
      />
    </div>
  );
};
const BasicForm = 'application/x-www-form-urlencoded';
const ValMap = {
  none: () => <div className="body-val-none">{i18n.t('dop:the current request has no body')}</div>,
  [BasicForm]: (props: any) => {
    const { data, updateBody, disabled }: any = props;
    return (
      <KeyValEdit
        type="body"
        disabled={disabled}
        data={isString(data.content) ? [] : (data.content as any)}
        dataModel={{
          key: '',
          value: '',
          desc: '',
        }}
        dataMainKey="key"
        onChange={(val: any) => {
          updateBody('content', val);
        }}
        itemMap={{
          key: {
            props: {
              placeholder: i18n.t('dop:parameter name'),
            },
            trim: true,
          },
          value: {
            props: {
              placeholder: i18n.t('dop:parameter value'),
            },
          },
          desc: {
            props: {
              placeholder: i18n.t('description'),
            },
          },
        }}
      />
    );
  },
  raw: (props: any) => {
    const { data, updateBody, disabled }: any = props;
    const val = isString(data.content) ? data.content : '';
    return (
      <TextArea disabled={disabled} rows={10} value={val} onChange={(e) => updateBody('content', e.target.value)} />
    );
  },
  'JSON(application/json)': (props: any) => <TestJsonEditor {...props} />,
};
const APIBody = (props: any) => {
  const { data, onChange, disabled } = props;
  const isRaw = !['none', BasicForm].includes(data.type);
  const realType = data.type;
  const updateBody = (key: string, val: any) => {
    const newBody: any = { ...data, [key]: val || '' };
    if (key === 'type') {
      switch (val) {
        case 'none':
          newBody.content = '';
          break;
        case BasicForm:
          newBody.content = [];
          break;
        default:
          break;
      }
    }
    onChange('body', newBody, (newData: any) => {
      const { headers = [], body } = newData;
      const adjustHeader = (action: string, headerType: any) => {
        // 按key查找
        const exist: any = find(headers, { key: headerType.key });
        if (action === 'push') {
          // 有的话更新，没有就添加
          if (exist) {
            exist.value = headerType.value;
          } else {
            headers.push(headerType);
          }
        } else if (exist && action === 'remove') {
          set(newData, 'headers', reject(headers, { key: headerType.key }));
        }
      };
      switch (body.type) {
        case 'application/json':
          adjustHeader('push', { key: 'Content-Type', value: 'application/json', desc: '' });
          break;
        case 'Text(text/plain)':
          adjustHeader('push', { key: 'Content-Type', value: 'text/plain', desc: '' });
          break;
        case 'Text':
          adjustHeader('remove', { key: 'Content-Type' });
          break;
        case BasicForm:
          adjustHeader('push', { key: 'Content-Type', value: body.type, desc: '' });
          break;
        default:
          break;
      }
    });
  };
  const changeType = (type: string) => {
    // 如果切换为raw类型，使用raw的第一个选项
    updateBody('type', type === 'raw' ? BODY_RAW_OPTION[0] : type);
  };
  const CurValueComp = ValMap[realType] || ValMap.raw;
  return (
    <div className="api-body">
      <div className="body-type-chosen mb-2 px-3">
        <Radio.Group disabled={disabled} onChange={(e) => changeType(e.target.value)} value={isRaw ? 'raw' : realType}>
          <Radio value={'none'}>none</Radio>
          <Radio value={BasicForm}>x-www-form-urlencoded</Radio>
          <Radio value={'raw'}>raw</Radio>
        </Radio.Group>
        {isRaw ? (
          <Select
            size="small"
            style={{ width: 160 }}
            className="mt-2"
            onChange={(t) => changeType(t as string)}
            value={realType}
            disabled={disabled}
            dropdownMatchSelectWidth={false}
          >
            {map(BODY_RAW_OPTION, (item) => (
              <Option key={item} value={item}>
                {item}
              </Option>
            ))}
          </Select>
        ) : null}
      </div>
      <div className="body-value-container px-3">
        {CurValueComp && <CurValueComp disabled={disabled} data={data} updateBody={updateBody} />}
      </div>
    </div>
  );
};
interface IKeyValProps {
  dataMainKey?: string;
  data: object[];
  type: string;
  dataModel: object;
  itemMap: object;
  opList?: any[];
  disabled?: boolean;
  onChange: (...args: any) => any;
}
const KeyValEdit = (props: IKeyValProps) => {
  const { data, type, dataModel, dataMainKey, itemMap, opList = [], onChange, disabled } = props;
  const [values, setValues] = React.useState(data || []);
  React.useEffect(() => {
    let newVal: any = [];
    if (isEmpty(data)) {
      newVal = [{ ...dataModel }];
    } else if (find(data, dataModel)) {
      newVal = data;
    } else {
      newVal = [...data, { ...dataModel }];
    }
    setValues(newVal);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [data]);
  const updateValue = (idx: number, key: string, val: string) => {
    if (disabled) return;
    const oldVal = cloneDeep(values);
    const newVal: any = cloneDeep(values);
    newVal[idx][key] = val;
    // source选了status时，把这一行expression设为status
    if (key === 'source') {
      switch (val) {
        case 'status':
          newVal[idx].expression = 'status';
          break;
        case 'body:json':
          newVal[idx].expression = '';
          break;
        default:
          break;
      }
    }
    setValues(newVal);
    onChange(
      // 去掉key值都为空的无效值
      newVal.filter((item: Obj) => {
        if (dataMainKey) return item[dataMainKey];
        return !Object.values(item).every((v) => !v);
      }),
      (newData: any, k: string) => {
        const { out_params = [], asserts = [] } = newData;
        if (k === 'out_params') {
          // 修改出参时修改对应断言
          const oldKey = oldVal[idx].key;
          asserts &&
            asserts.forEach((a: any) => {
              if (a.arg === oldKey) {
                set(a, 'arg', out_params[idx].key);
              }
            });
        }
        // 更新断言时同时清除小试中对应断言的结果
        if (k.startsWith('asserts')) {
          const { attemptTest } = newData;
          if (attemptTest && attemptTest.asserts && attemptTest.asserts.result.length >= asserts.length) {
            const match = attemptTest.asserts.result[idx];
            if (match) {
              match.success = undefined;
            }
          }
        }
      },
    );
  };
  const handleDelete = (num: number) => {
    if (disabled) return;
    const newVals = values.filter((item, i) => i !== num);
    setValues(newVals);
    onChange(newVals.slice(0, -1), (newData: any, k: string) => {
      const { out_params = [], asserts = [] } = newData;
      // 删除出参时删除对应断言，data为apis全部数据
      if (k === 'out_params') {
        const outParamKeys = {};
        out_params.forEach((p: any) => {
          outParamKeys[p.key] = true;
        });
        // 只保留arg没填或者在out_params有匹配的断言
        const newAsserts = asserts && asserts.filter((a: any) => a.arg === '' || outParamKeys[a.arg]);
        set(newData, 'asserts', newAsserts);
      }
      // 删除断言时同时删除小试中对应断言的结果
      if (k.startsWith('asserts')) {
        const { attemptTest } = newData;
        if (attemptTest) {
          attemptTest.asserts.result.splice(num, 1);
        }
      }
    });
  };
  const keyArr = Object.keys(dataModel);
  return (
    <div className="key-val-container">
      {map(values, (item, i) => {
        const lastItem = i === values.length - 1;
        return (
          <div className="key-val-item" key={i}>
            {map(keyArr, (key: string) => {
              const val = item[key];
              const { Comp, props: compProps, getProps, trim = false } = itemMap[key];
              const extraProps = getProps ? getProps(item) : {};
              return (
                <React.Fragment key={key}>
                  {Comp ? (
                    <Comp
                      disabled={disabled}
                      className="flex-1 width0"
                      value={val}
                      record={item}
                      onChange={(curVal: any) => updateValue(i, key, trim ? curVal?.trim() : curVal)}
                    />
                  ) : (
                    <Input
                      className="flex-1 width0"
                      placeholder={i18n.t('please enter')}
                      disabled={disabled}
                      value={val}
                      onChange={(e) => updateValue(i, key, trim ? e.target.value?.trim() : e.target.value)}
                      {...compProps}
                      {...extraProps}
                    />
                  )}
                  {Comp === Empty ? null : <div className="item-separate" />}
                </React.Fragment>
              );
            })}
            {!disabled ? (
              <div className="key-val-operation">
                {opList[i] || null}
                {type === 'out_params' ? (
                  <Popconfirm
                    title={i18n.t(
                      'dop:Deleting the output parameter will delete the corresponding parameter name assertion. Continue?',
                    )}
                    onConfirm={() => handleDelete(i)}
                  >
                    <CustomIcon type="sc1" className={lastItem ? 'hidden-del hover-active' : 'show-del hover-active'} />
                  </Popconfirm>
                ) : (
                  <CustomIcon
                    type="sc1"
                    onClick={() => {
                      handleDelete(i);
                    }}
                    className={lastItem ? 'hidden-del hover-active' : 'show-del hover-active'}
                  />
                )}
              </div>
            ) : null}
          </div>
        );
      })}
    </div>
  );
};
