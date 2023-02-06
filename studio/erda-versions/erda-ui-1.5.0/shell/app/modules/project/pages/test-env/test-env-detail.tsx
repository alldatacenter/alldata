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

import { isPlainObject, map, forEach, isEmpty, filter, debounce } from 'lodash';
import React from 'react';
import i18n from 'i18n';
import { isValidJsonStr } from 'common/utils';
import { Input, Select, Table, Radio } from 'antd';
import { KVPair, ProtocolInput, FormModal, InputSelect, FileEditor } from 'common';
import { useUpdate } from 'common/use-hooks';
import { FormInstance, RadioChangeEvent } from 'core/common/interface';
import testEnvStore from 'project/stores/test-env';
import { scopeMap } from 'project/common/components/pipeline-manage/config';

const { Option } = Select;

const headerList = [
  'Accept',
  'Accept-Charset',
  'Accept-Encoding',
  'Accept-Language',
  'Accept-Datetime',
  'Authorization',
  'Cache-Control',
  'Connection',
  'Cookie',
  'Content-Disposition',
  'Content-Length',
  'Content-MD5',
  'Content-Type',
  'Date',
  'Expect',
  'From',
  'Host',
  'If-Match',
  'If-Modified-Since',
  'If-None-Match',
  'If-Range',
  'If-Unmodified-Since',
  'Max-Forwards',
  'Origin',
  'Pragma',
  'Proxy-Authorization',
  'Range',
  'Referer',
  'TE',
  'User-Agent',
  'Upgrade',
  'Via',
  'Warning',
  'X-Requested-With',
  'DNT',
  'X-Forwarded-For',
  'X-Forwarded-Host',
  'X-Forwarded-Proto',
  'Front-End-Https',
  'X-Http-Method-Override',
  'X-ATT-DeviceId',
  'X-Wap-Profile',
  'Proxy-Connection',
  'X-UIDH',
  'X-Csrf-Token',
];

const typeMap = {
  manual: {
    string: 'string',
    integer: 'integer',
    float: 'float',
    boolean: 'boolean',
    list: 'list',
  },
  auto: {
    string: 'string',
  },
};

const transObjToList = (obj: object) => {
  if (!isPlainObject(obj)) {
    return obj;
  }
  const list: Obj[] = [];
  map(obj, (v: string, k: string) => {
    list.push({ key: k, value: v });
  });
  return list;
};

const transHeader = (list: any[]) => {
  if (!Array.isArray(list)) {
    return list;
  }
  const obj = {};
  map(list, (item) => {
    if (item.key || item.value) {
      obj[item.key] = item.value;
    }
  });
  return obj;
};

const transGlobal = (list: any[]) => {
  if (!Array.isArray(list)) {
    return list;
  }
  const dataTemp = { type: '', value: '', desc: '' };
  const obj = {};
  map(list, (item) => {
    const { key, value, ...rest } = item || {};
    if (key) {
      let reItem = {};
      if (isPlainObject(value)) {
        reItem = { ...dataTemp, ...value, ...rest };
      } else {
        reItem = {
          ...dataTemp,
          value,
          ...rest,
        };
      }
      if (!reItem.type) reItem.type = typeMap.auto.string;
      obj[key] = reItem;
    }
  });
  return obj;
};

const KVPairTable = (props: any) => {
  const { value, disabled } = props;
  const columns = [
    {
      title: i18n.t('name'),
      dataIndex: 'Key',
      width: 200,
    },
    {
      title: i18n.t('dop:parameter content'),
      dataIndex: 'Value',
    },
    {
      title: i18n.t('operation'),
      key: 'op',
      width: 80,
      render: (_: any, record: any) => (record.isLast ? null : record.Op),
    },
  ];

  const _value: any = map(transObjToList(value), (item) => {
    const newItem = {};
    forEach(item, (v, k) => {
      if (isPlainObject(v)) {
        forEach(v as Obj, (subV: string, subK: string) => {
          newItem[subK] = subV;
        });
      } else {
        newItem[k] = v;
      }
    });
    return newItem;
  });

  if (props.KeyDescComp) {
    columns.splice(2, 0, {
      title: i18n.t('description'),
      dataIndex: 'KeyDescComp',
      width: 150,
    });
  }
  if (props.DescComp) {
    columns.splice(1, 0, {
      title: i18n.t('dop:paramType'),
      dataIndex: 'Desc',
      width: 150,
    });
  }

  return (
    <KVPair {...props} value={_value} emptyHolder={!disabled} autoAppend={!disabled} compProps={{ disabled }}>
      {({ CompList }: any) => {
        const data = CompList.map((d: any, index: number) => ({
          index,
          ...CompList[index],
          isLast: index === CompList.length - 1,
        }));
        return <Table rowKey={'index'} columns={columns} pagination={false} dataSource={data} scroll={{ x: '100%' }} />;
      }}
    </KVPair>
  );
};

interface IProps {
  visible: boolean;
  data: TEST_ENV.Item | Obj;
  disabled: boolean;
  testType: string;
  envID: number;
  envType: TEST_ENV.EnvType;
  onCancel: () => any;
}

const headerListOption = headerList.map((o) => ({ label: o, value: o }));
export const TestEnvDetail = (props: IProps) => {
  const { data, disabled, visible, onCancel, envID, envType, testType } = props;
  const [{ headerMode, globalMode, headerJsonValid, globalJsonValid }, updater, update] = useUpdate({
    headerMode: 'form',
    globalMode: 'form',
    headerJsonValid: true,
    globalJsonValid: true,
  });

  React.useEffect(() => {
    if (!visible) {
      update({
        headerMode: 'form',
        globalMode: 'form',
        headerJsonValid: true,
        globalJsonValid: true,
      });
    }
  }, [update, visible]);

  const formRef = React.useRef({}) as React.MutableRefObject<FormInstance>;
  const HeaderKeyComp = ({ record, update: _update, ...rest }: any) => {
    return <InputSelect options={headerListOption} value={record.key} disabled={disabled} onChange={_update} />;
  };

  const GlobalKeyComp = ({ record, update: _update, ...rest }: any) => (
    <Input value={record.key} onChange={(e) => _update(e.target.value.trim())} maxLength={500} {...rest} />
  );

  const GlobalDescComp = ({ record, update: _update, ...rest }: any) => (
    <Select value={record.type || typeMap[testType].string} allowClear onChange={_update} {...rest}>
      {map(typeMap[testType], (value, key) => (
        <Option key={key} value={value}>
          {value}
        </Option>
      ))}
    </Select>
  );

  const KeyDescComp = ({ record, keyDesc, update: _update, ...rest }: any) => (
    <Input value={record[keyDesc]} onChange={(e) => _update(e.target.value)} {...rest} />
  );

  const ValueComp = ({ record, valueName, update: _update, ...rest }: any) => (
    <Input value={record[valueName]} onChange={(e) => _update(e.target.value)} {...rest} />
  );

  const getFieldsList = (_type: string, _headMode: string, _globalMode: string) => {
    const headFieldsStatus = _headMode === 'form' ? ['', 'hidden'] : ['hidden', ''];
    const globalFieldsStatus = _globalMode === 'form' ? ['', 'hidden'] : ['hidden', ''];

    const fieldMap = {
      auto: [
        {
          label: i18n.t('name'),
          name: 'displayName',
          itemProps: {
            maxLength: 191,
            disabled,
          },
        },
        {
          label: i18n.t('description'),
          name: 'desc',
          type: 'textArea',
          itemProps: {
            maxLength: 512,
            disabled,
          },
        },
      ],
      manual: [
        {
          label: i18n.t('dop:environment name'),
          name: 'name',
          itemProps: {
            maxLength: 50,
            disabled,
          },
        },
      ],
    };

    return [
      ...fieldMap[_type],
      {
        label: i18n.t('dop:environmental domain name'),
        name: 'domain',
        getComp: () => <ProtocolInput disabled={disabled} />,
        required: false,
      },
      {
        getComp: () => (
          <div className="flex justify-between items-center">
            <div>
              <span className="font-bold">Header</span>
            </div>
            <Radio.Group
              value={headerMode}
              onChange={(e: RadioChangeEvent) => {
                const _mode = e.target.value;
                const curForm = formRef.current;
                const curFormData = curForm.getFieldsValue();
                if (_mode === 'form') {
                  formRef.current.setFieldsValue({
                    header: JSON.parse(curFormData.headerStr),
                  });
                } else {
                  const isObj = isPlainObject(curFormData.header);
                  formRef.current.setFieldsValue({
                    headerStr: JSON.stringify(
                      filter(
                        map(curFormData.header, (item, k) => {
                          let reItem = {};
                          if (isObj) {
                            reItem = { value: item, key: k };
                          } else {
                            reItem = { key: item.key, value: item.value };
                          }
                          return reItem;
                        }),
                        (item) => item.key,
                      ),
                      null,
                      2,
                    ),
                  });
                }
                updater.headerMode(e.target.value);
              }}
            >
              <Radio.Button disabled={!headerJsonValid} value="form">
                {disabled ? i18n.t('common:form') : i18n.t('common:form edit')}
              </Radio.Button>
              <Radio.Button value="code">{disabled ? i18n.t('common:text') : i18n.t('common:text edit')}</Radio.Button>
            </Radio.Group>
          </div>
        ),
        extraProps: {
          className: 'mb-2',
        },
      },
      {
        name: 'header',
        required: false,
        getComp: () => <KVPairTable disabled={disabled} KeyComp={HeaderKeyComp} ValueComp={ValueComp} />,
        itemProps: {
          type: headFieldsStatus[0],
        },
      },
      {
        name: 'headerStr',
        required: false,
        getComp: () => <JsonFileEditor readOnly={disabled} />,
        itemProps: {
          type: headFieldsStatus[1],
        },
      },
      {
        getComp: () => (
          <div className="flex justify-between items-center">
            <span className="font-bold">Global</span>
            <Radio.Group
              value={globalMode}
              onChange={(e: RadioChangeEvent) => {
                const _mode = e.target.value;
                const curForm = formRef.current;
                const curFormData = curForm.getFieldsValue();
                if (_mode === 'form') {
                  formRef.current.setFieldsValue({
                    global: JSON.parse(curFormData.globalStr),
                  });
                } else {
                  const isObj = isPlainObject(curFormData.global);
                  formRef.current.setFieldsValue({
                    globalStr: JSON.stringify(
                      filter(
                        map(curFormData.global, (item, k) => {
                          const { desc = '', value = '', type = '' } = item;
                          const reItem = { desc, value, type, key: isObj ? k : item.key };
                          if (!reItem.type) reItem.type = typeMap.auto.string;
                          return reItem;
                        }),
                        (item) => item.key,
                      ),
                      null,
                      2,
                    ),
                  });
                }
                updater.globalMode(e.target.value);
              }}
            >
              <Radio.Button disabled={!globalJsonValid} value="form">
                {disabled ? i18n.t('common:form') : i18n.t('common:form edit')}
              </Radio.Button>
              <Radio.Button value="code">{disabled ? i18n.t('common:text') : i18n.t('common:text edit')}</Radio.Button>
            </Radio.Group>
          </div>
        ),
        extraProps: {
          className: 'mb-2',
        },
      },
      {
        name: 'global',
        required: false,
        getComp: () => (
          <KVPairTable
            disabled={disabled}
            KeyComp={GlobalKeyComp}
            DescComp={GlobalDescComp}
            descName="type"
            KeyDescComp={KeyDescComp}
            keyDesc="desc"
          />
        ),
        itemProps: {
          type: globalFieldsStatus[0],
        },
      },
      {
        name: 'globalStr',
        required: false,
        getComp: () => <JsonFileEditor readOnly={disabled} />,
        itemProps: {
          type: globalFieldsStatus[1],
        },
      },
    ];
  };

  const fieldsList = getFieldsList(testType, headerMode, globalMode);

  const onUpdateHandle = React.useCallback(
    (values, header, global) => {
      if (testType === 'manual') {
        testEnvStore.updateTestEnv({ ...values, id: data.id, header, global, envType, envID }, { envType, envID });
      } else {
        testEnvStore.updateAutoTestEnv({
          apiConfig: {
            domain: values.domain,
            name: values.name,
            header,
            global,
          },
          scope: scopeMap.autoTest.scope,
          scopeID: String(envID),
          ns: data.ns,
          displayName: values.displayName,
          desc: values.desc,
        });
      }
    },
    [data, envID, envType, testType],
  );

  const onCreateHandle = React.useCallback(
    (values, header, global) => {
      if (testType === 'manual') {
        testEnvStore.createTestEnv({ ...values, header, global, envType, envID }, { envType, envID });
      } else {
        testEnvStore.createAutoTestEnv(
          {
            apiConfig: {
              ...values,
              header,
              global,
            },
            scope: scopeMap.autoTest.scope,
            scopeID: String(envID),
            displayName: values.displayName,
            desc: values.desc,
          },
          { scope: scopeMap.autoTest.scope, scopeID: envID },
        );
      }
    },
    [envID, envType, testType],
  );

  const handleSubmit = (values: any) => {
    if (!globalJsonValid || !headerJsonValid) {
      return Promise.reject();
    }
    if (disabled) {
      onCancel();
      return;
    }
    const { headerStr, globalStr, ..._rest } = values;
    const curHeader = headerMode === 'form' ? values.header : JSON.parse(headerStr || '[]');
    const curGlobal = globalMode === 'form' ? values.global : JSON.parse(globalStr || '[]');
    const header = transHeader(curHeader);
    const global = transGlobal(curGlobal);

    if (!isEmpty(data)) {
      onUpdateHandle(_rest, header, global);
    } else {
      onCreateHandle(_rest, header, global);
    }
    onCancel();
  };

  const onValuesChange = React.useCallback(
    debounce((_formRef: { form: FormInstance }, changeValues: Obj, allValues: Obj) => {
      if (changeValues.headerStr) {
        const curHeaderValid = isValidJsonStr(changeValues.headerStr);
        updater.headerJsonValid(curHeaderValid);
      }
      if (changeValues.globalStr) {
        const curGlobalValid = isValidJsonStr(changeValues.globalStr);
        updater.globalJsonValid(curGlobalValid);
      }
    }, 300),
    [],
  );

  return (
    <FormModal
      name={i18n.t('dop:parameter configuration')}
      visible={visible}
      width={900}
      modalProps={{
        destroyOnClose: true,
      }}
      formOption={{ onValuesChange }}
      formData={data}
      ref={formRef}
      fieldsList={fieldsList}
      onOk={handleSubmit}
      onCancel={onCancel}
      formProps={{ layout: 'vertical' }}
    />
  );
};

interface JsonFileProps {
  value?: string;
  onChange?: (v: string) => void;
  readOnly?: boolean;
}
const JsonFileEditor = (p: JsonFileProps) => {
  const { value, onChange, readOnly } = p;
  const _isValid = isValidJsonStr(value);
  return (
    <>
      <FileEditor
        fileExtension="json"
        minLines={4}
        readOnly={readOnly}
        className="rounded border-all"
        maxLines={10}
        actions={{
          copy: true,
          format: true,
        }}
        onChange={(val: string) => {
          onChange && onChange(val);
        }}
        value={value}
      />
      {_isValid ? null : <span className="text-danger">{i18n.t('dop:JSON format error')}</span>}
    </>
  );
};
