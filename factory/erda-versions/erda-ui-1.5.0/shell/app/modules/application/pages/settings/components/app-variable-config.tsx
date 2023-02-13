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

import { useLoading } from 'core/stores/loading';
import configStore from 'app/modules/application/stores/pipeline-config';
import appStore from 'application/stores/application';
import { Copy, IF, CustomFilter, FileEditor, ErdaIcon } from 'common';
import { useUpdate } from 'common/use-hooks';
import { ColumnProps } from 'core/common/interface';
import { WORKSPACE_LIST } from 'common/constants';
import routeInfoStore from 'core/stores/route';
import i18n from 'i18n';
import { map, isEmpty } from 'lodash';
import { Button, Collapse, Popconfirm, Spin, Table, Tooltip, Input, Modal } from 'antd';
import React from 'react';
import { useEffectOnce, useUnmount } from 'react-use';
import { VariableConfigForm } from './variable-config-form';

const { Panel } = Collapse;

export const ENV_I18N = {
  default: i18n.t('common:default config'),
  dev: i18n.t('dev environment'),
  test: i18n.t('test environment'),
  staging: i18n.t('staging environment'),
  prod: i18n.t('prod environment'),
};

interface IKeyOperations {
  canDelete: boolean;
  canDownload: boolean;
  canEdit: boolean;
}

interface IKey extends PIPELINE_CONFIG.ConfigItem {
  isFromDefault?: boolean;
  namespace: string;
  operations: IKeyOperations;
}

const typeMap = {
  kv: 'kv',
  file: 'dice-file',
};
const envKeys = ['default', ...WORKSPACE_LIST].map((k) => k.toLowerCase());

const configTypeMap = {
  mobile: 'MobileConfig',
  pipeline: 'PipelineConfig',
  deploy: 'DeployConfig',
};

interface IProps {
  envToNs: Obj<string>;
  configs: PIPELINE_CONFIG.ConfigItemMap;
  configType?: string;
  addConfig: (data: PIPELINE_CONFIG.AddConfigsBodyWithoutAppId) => Promise<any>;
  updateConfig: (data: PIPELINE_CONFIG.AddConfigsBodyWithoutAppId) => Promise<any>;
  deleteConfig: (data: Omit<PIPELINE_CONFIG.DeleteConfigQuery, 'appID'>) => Promise<any>;
  importConfig?: (data: PIPELINE_CONFIG.importConfigsBody) => Promise<any>;
  exportConfig?: (data: Pick<PIPELINE_CONFIG.AddConfigsQuery, 'namespace_name'>) => Promise<any>;
}
const VariableConfig = ({
  envToNs,
  configs,
  configType,
  addConfig,
  updateConfig,
  deleteConfig,
  importConfig,
  exportConfig,
}: IProps) => {
  const { appId } = routeInfoStore.useStore((s) => s.params);

  const [
    {
      envConfigMap,
      visible,
      importVisible,
      exportVisible,
      type,
      curEnv,
      editData,
      importValue,
      exportValue,
      activeKey,
      searchKey,
      isJsonInvalid,
    },
    updater,
    update,
  ] = useUpdate({
    envConfigMap: {},
    visible: false,
    importVisible: false,
    exportVisible: false,
    type: typeMap.kv,
    curEnv: '',
    editData: null as PIPELINE_CONFIG.ConfigItem | null,
    importValue: '',
    exportValue: '',
    activeKey: [],
    searchKey: '',
    isJsonInvalid: false,
  });

  React.useEffect(() => {
    if (isEmpty(envToNs)) {
      return;
    }
    const temp: PIPELINE_CONFIG.ConfigItemMap = {};
    envKeys.forEach((env) => {
      temp[env] = configs[envToNs[env]] || [];
    });
    const defaultConfig = temp.default.map((m: PIPELINE_CONFIG.ConfigItem) => ({ ...m, namespace: envToNs.default }));
    const _nsConfigMap = { default: defaultConfig };
    ['dev', 'test', 'staging', 'prod'].forEach((env) => {
      const duplicate = {};
      // 与默认环境数据进行合并
      _nsConfigMap[env] = [];
      [...temp[env].reverse(), ...defaultConfig.map((item) => ({ ...item, isFromDefault: true }))].forEach((item) => {
        // default的放在了后面，后面会整体反转一把，所以先反转一下temp里的
        if (duplicate[item.key]) {
          // 如果key重复，丢弃后面default里的
          return;
        }
        duplicate[item.key] = true;
        _nsConfigMap[env].push({ ...item, namespace: envToNs[env] });
      });
      _nsConfigMap[env] = _nsConfigMap[env].reverse();
    });
    updater.envConfigMap(_nsConfigMap);
    if (searchKey) {
      const expandKeys: string[] = [];
      map(_nsConfigMap, (itemList, k) => {
        const newList = itemList.filter((item) => item.key.toLowerCase().includes(searchKey.toLowerCase()));
        if (newList.length) {
          expandKeys.push(k);
        }
        _nsConfigMap[k] = newList;
      });
      updater.activeKey(expandKeys);
    }
  }, [appId, envToNs, configs, searchKey, updater]);

  const openModal = (data: IKey | null, env: string) => {
    update({
      editData: data,
      visible: true,
      curEnv: env,
      type: data ? data.type : type,
    });
  };

  const closeModal = () => {
    update({
      editData: null,
      visible: false,
      type: typeMap.kv,
    });
  };

  const openImportModal = (env: string) => {
    exportConfig &&
      exportConfig({ namespace_name: envToNs[env] }).then((data) => {
        updater.importValue(JSON.stringify(data));
      });
    update({
      importVisible: true,
      curEnv: env,
    });
  };

  const closeImportModal = () => {
    update({
      importVisible: false,
    });
  };

  const openExportModal = (env: string) => {
    update({
      exportVisible: true,
      curEnv: env,
    });
    exportConfig &&
      exportConfig({ namespace_name: envToNs[env] }).then((data) => {
        updater.exportValue(JSON.stringify(data));
      });
  };

  const closeExportModal = () => {
    update({
      exportVisible: false,
    });
  };

  const getColumns = (_env: string): Array<ColumnProps<IKey>> => [
    {
      title: 'Key',
      dataIndex: 'key',
      width: 176,
      sorter: (a: IKey, b: IKey) => a.key.charCodeAt(0) - b.key.charCodeAt(0),
      render: (text: string, { isFromDefault, source }: IKey) => (
        <div className="flex justify-between items-center">
          <span className="cursor-copy nowrap" data-clipboard-text={text} title={text}>
            {text}
          </span>
          <span>
            {source === 'certificate' && (
              <Tooltip title={i18n.t('common:from certificate push')}>
                <ErdaIcon type="info" className="ml-1 text-sub" />
              </Tooltip>
            )}
            {isFromDefault && <span className="tag tag-warning ml-1">{i18n.t('default')}</span>}
          </span>
        </div>
      ),
    },
    {
      title: 'Value',
      dataIndex: 'value',
      className: 'nowrap',
      width: 176,
      render: (text: string, record: IKey) => {
        return record.type === typeMap.kv ? (
          record.encrypt ? (
            '******'
          ) : (
            <Tooltip title={text} placement="leftTop">
              <span className="cursor-copy" data-clipboard-text={text}>
                {text}
              </span>
            </Tooltip>
          )
        ) : (
          '-'
        );
      },
    },
    {
      title: i18n.t('type'),
      dataIndex: 'type',
      width: 96,
      render: (text: string) => (text === typeMap.kv ? i18n.t('value') : i18n.t('file')),
    },
    {
      title: i18n.t('dop:remark'),
      dataIndex: 'comment',
      render: (text: string) => (text ? <Tooltip title={text}>{text.slice(0, 30)}</Tooltip> : '-'),
    },
    {
      title: i18n.t('operation'),
      dataIndex: 'operations',
      width: 200,
      fixed: 'right',
      render: (operations: IKeyOperations, record: IKey) => {
        const { canDelete, canDownload, canEdit } = operations || {};
        const { encrypt } = record;
        return (
          <div className="table-operations">
            <IF check={canDownload}>
              {encrypt ? (
                <Tooltip title={i18n.t('dop:encrypted files cannot be downloaded')}>
                  <a className="table-operations-btn disabled">{i18n.t('download')}</a>
                </Tooltip>
              ) : (
                <a className="table-operations-btn" download={record.value} href={`/api/files/${record.value}`}>
                  {i18n.t('download')}
                </a>
              )}
            </IF>
            <IF check={canEdit}>
              <span className="table-operations-btn" onClick={() => openModal(record, _env)}>
                {i18n.t('edit')}
              </span>
            </IF>
            <IF check={canDelete && !record.isFromDefault}>
              <Popconfirm
                title={`${i18n.t('dop:confirm to delete configuration')}？`}
                onConfirm={() =>
                  deleteConfig({
                    key: record.key,
                    namespace_name: record.namespace,
                  })
                }
              >
                <span className="table-operations-btn">{i18n.t('delete')}</span>
              </Popconfirm>
            </IF>
          </div>
        );
      },
    },
  ];

  const handelSubmit = (data: IKey, isAdd: boolean) => {
    const pushConfig = isAdd ? addConfig : updateConfig;
    return pushConfig({
      query: { namespace_name: envToNs[curEnv], encrypt: data.encrypt },
      configs: [data],
    }).then(() => {
      closeModal();
    });
  };

  const handleImportSubmit = () => {
    return (
      importConfig &&
      importConfig({
        query: { namespace_name: envToNs[curEnv] },
        configs: importValue,
      }).then(() => {
        closeImportModal();
        updater.importValue('');
      })
    );
  };

  const togglePanel = (keys: string | string[]) => {
    updater.activeKey(keys as string[]);
  };

  const filterConfig = React.useMemo(
    () => [
      {
        type: Input,
        name: 'key',
        customProps: {
          placeholder: i18n.t('common:search by {name}', { name: 'Key' }),
          autoComplete: 'off',
        },
      },
    ],
    [],
  );

  const isValidJsonStr = (_jsonStr: string) => {
    try {
      JSON.parse(_jsonStr);
      return true;
    } catch (e) {
      return false;
    }
  };

  const formattedImportValue = isValidJsonStr(importValue)
    ? JSON.stringify(importValue ? JSON.parse(importValue) : '{}', null, 2)
    : importValue;

  return (
    <div>
      <Spin spinning={useLoading(configStore, ['getConfigs'])[0]}>
        <CustomFilter config={filterConfig} onSubmit={(v) => updater.searchKey(v.key)} />
        <Collapse className="mb-5 nowrap" activeKey={activeKey} onChange={togglePanel}>
          {map(envKeys, (env: string) => {
            return (
              <Panel header={ENV_I18N[env]} key={env}>
                <Button type="primary" className="mb-3" ghost onClick={() => openModal(null, env)}>
                  {i18n.t('dop:add variable')}
                </Button>
                {configType === configTypeMap.deploy && (
                  <>
                    <Button
                      type="primary"
                      ghost
                      className="mr-2 float-right"
                      onClick={() => {
                        openExportModal(env);
                      }}
                    >
                      {i18n.t('export')}
                    </Button>
                    <Button type="primary" ghost className="mr-2 float-right" onClick={() => openImportModal(env)}>
                      {i18n.t('import')}
                    </Button>
                  </>
                )}
                <Table dataSource={envConfigMap[env]} columns={getColumns(env)} scroll={{ x: 800 }} />
              </Panel>
            );
          })}
        </Collapse>
      </Spin>
      <VariableConfigForm visible={visible} formData={editData} onCancel={closeModal} onOk={handelSubmit} />
      <Modal
        visible={importVisible}
        onOk={handleImportSubmit}
        okButtonProps={{ disabled: isJsonInvalid }}
        onCancel={closeImportModal}
        title={i18n.t('import configuration')}
      >
        <FileEditor
          fileExtension="json"
          minLines={8}
          value={formattedImportValue}
          onChange={(value: string) => {
            if (isValidJsonStr(value)) {
              update({
                importValue: JSON.stringify(JSON.parse(value), null, 2),
                isJsonInvalid: false,
              });
            } else {
              update({
                importValue: value,
                isJsonInvalid: true,
              });
            }
          }}
        />
        {isJsonInvalid && (
          <span className="text-danger">{i18n.t('dop:the current input content is invalid JSON')}</span>
        )}
      </Modal>
      <Modal
        visible={exportVisible}
        onOk={closeExportModal}
        onCancel={closeExportModal}
        title={i18n.t('export configuration')}
        footer={null}
      >
        <FileEditor
          fileExtension="json"
          value={JSON.stringify(exportValue ? JSON.parse(exportValue) : '{}', null, 2)}
          minLines={8}
          className="mb-5"
        />
      </Modal>
      <Copy selector=".cursor-copy" />
    </div>
  );
};

export const MobileConfig = () => {
  const fullConfigs = configStore.useStore((s) => s.fullConfigs);
  const { appId } = routeInfoStore.useStore((s) => s.params);
  const envToNs: Obj<string> = {};
  const nsQuery = envKeys.map((env) => {
    envToNs[env] = `app-${appId}-${env}`;
    return {
      namespace_name: `app-${appId}-${env}`,
      decrypt: false,
    };
  });
  useEffectOnce(() => {
    configStore.getConfigs(nsQuery);

    return () => configStore.clearConfigs();
  });

  return (
    <VariableConfig
      envToNs={envToNs}
      configs={fullConfigs}
      addConfig={configStore.addConfigs}
      updateConfig={configStore.updateConfigs}
      deleteConfig={configStore.removeConfigWithoutDeploy}
    />
  );
};

export const PipelineConfig = () => {
  const fullConfigs = configStore.useStore((s) => s.fullConfigs);
  const [{ envToNs }, updater] = useUpdate({
    envToNs: {},
  });
  useEffectOnce(() => {
    configStore.getConfigNameSpaces().then((result) => {
      const temp = {};
      map(result, (item) => {
        temp[item.workspace.toLowerCase() || 'default'] = item.namespace;
        return item.namespace;
      });
      updater.envToNs(temp);

      configStore.getConfigs(
        result.map(({ namespace }) => ({
          namespace_name: namespace,
          decrypt: false,
        })),
      );
    });
    return () => configStore.clearConfigs();
  });

  return (
    <VariableConfig
      envToNs={envToNs}
      configs={fullConfigs}
      addConfig={configStore.addConfigs}
      updateConfig={configStore.updateConfigs}
      deleteConfig={configStore.removeConfigWithoutDeploy}
    />
  );
};

export const DeployConfig = () => {
  const appDetail = appStore.useStore((s) => s.detail);
  const { appId } = routeInfoStore.useStore((s) => s.params);
  const fullConfigs = configStore.useStore((s) => s.fullConfigs);
  const envToNs = React.useRef({});
  React.useEffect(() => {
    if (appDetail.workspaces) {
      const namespaceParams = appDetail.workspaces.map((item) => {
        envToNs.current[item.workspace.toLowerCase()] = item.configNamespace;
        return {
          namespace_name: item.configNamespace,
          decrypt: false,
        };
      });
      configStore.getConfigs(namespaceParams, 'configmanage');
    }
  }, [appDetail.workspaces, appId, envToNs]);

  useUnmount(() => {
    configStore.clearConfigs();
  });

  return (
    <VariableConfig
      configType={configTypeMap.deploy}
      envToNs={envToNs.current}
      configs={fullConfigs}
      addConfig={(data) => configStore.addConfigs(data, 'configmanage')}
      updateConfig={(data) => configStore.updateConfigs(data, 'configmanage')}
      deleteConfig={(data) => configStore.removeConfigWithoutDeploy(data, 'configmanage')}
      importConfig={(data) => configStore.importConfigs(data, 'configmanage')}
      exportConfig={configStore.exportConfigs}
    />
  );
};
