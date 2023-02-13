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

import { Alert, Button, Form, Input, Spin } from 'antd';
import React from 'react';
import { ImageUpload, RenderForm } from 'common';
import { goTo, insertWhen } from 'common/utils';
import { filter, map } from 'lodash';
import { FormInstance } from 'core/common/interface';
import { appMode, modeOptions, repositoriesTypes, RepositoryMode } from 'application/common/config';
import { AppTypeSelect } from './app-type-select';
import i18n from 'i18n';
import routeInfoStore from 'core/stores/route';
import appStore from 'application/stores/application';
import { useLoading } from 'core/stores/loading';
import orgStore from 'app/org-home/stores/org';
import { erdaEnv } from 'common/constants';
import projectStore from 'project/stores/project';
import './app-form.scss';

interface IMobile extends Omit<APPLICATION.initApp, 'applicationID'> {
  template: string;
}

const fieldExtraProps = {
  style: {
    color: 'rgba(0,0,0,0.80)',
    fontWeight: '500',
  },
};

const CreationForm = () => {
  const { params } = routeInfoStore.getState((s) => s);
  const [mode, setMode] = React.useState(appMode.SERVICE);
  const [template, setTemplate] = React.useState([{ name: i18n.t('default:none'), value: '-1' }]);
  const [tempSelected, setTempSelected] = React.useState('-1');
  const [repoType, setRepoType] = React.useState(RepositoryMode.Internal);
  const { ENABLE_BIGDATA } = erdaEnv;
  const publisherId = orgStore.getState((s) => s.currentOrg.publisherId);
  const info = projectStore.useStore((s) => s.info);
  const { clusterConfig } = info;
  const currentProjectClusters = Object.values(clusterConfig || {});
  const [isCreateApp, isInitApp] = useLoading(appStore, ['createApp', 'initApp']);
  const formRef = React.useRef(null as any);
  const repoConfigTemp = React.useRef({});
  React.useEffect(() => {
    if (mode === appMode.MOBILE && template.length === 1) {
      appStore.effects.queryTemplate({ mode }).then((res) => {
        const temps = res.map((item: string) => {
          return {
            name: item,
            value: item,
          };
        });
        temps.unshift();
        setTemplate([...template, ...temps] || []);
      });
    }
  }, [mode, template]);
  React.useEffect(() => {
    repoConfigTemp.current = {};
  }, []);
  React.useEffect(() => {
    if (repoType !== RepositoryMode.Internal && repoConfigTemp.current[repoType]) {
      formRef.current.setFieldsValue(repoConfigTemp.current[repoType]);
    }
  }, [repoType]);
  const handleSubmit = (form: FormInstance) => {
    form.validateFields().then((values: Merge<APPLICATION.createBody, IMobile>) => {
      const { mobileDisplayName, bundleID, packageName, repoConfig, template: _, ...rest } = values;
      const isExternalRepo = repoType !== RepositoryMode.Internal;
      const payload: APPLICATION.createBody = { ...rest, projectId: +params.projectId, isExternalRepo };
      if (isExternalRepo) {
        // V_3.16: 所有的外部仓库统一作为general处理，下一版本会细化
        payload.repoConfig = {
          ...(repoConfig as APPLICATION.GitRepoConfig),
          // type: 'general',
        };
      }
      appStore.effects.createApp(payload).then(({ id: applicationID }) => {
        if (mode === appMode.MOBILE && tempSelected !== '-1') {
          appStore.effects
            .initApp({ mobileDisplayName, bundleID, packageName, applicationID, mobileAppName: rest.name })
            .then(() => {
              goTo(goTo.pages.pipeline, {
                projectId: params.projectId,
                appId: applicationID,
                replace: true,
              });
            });
        } else {
          goTo('..');
        }
      });
    });
  };

  const collectionRepoTemp = (type: string) => {
    if (type !== RepositoryMode.Internal) {
      const fields = ['repoConfig.url', 'repoConfig.username', 'repoConfig.password', 'repoConfig.desc'];
      const data = formRef.current.getFieldsValue(fields);
      repoConfigTemp.current[type] = data;
    }
  };

  const useOption = filter(modeOptions, (item) => {
    // 创建时不需要展示能力应用
    const excludeOptions = [appMode.ABILITY];
    !publisherId && excludeOptions.push(appMode.MOBILE);
    ENABLE_BIGDATA === 'false' && excludeOptions.push(appMode.BIGDATA); // 为新华书店保留
    return !excludeOptions.includes(item.value);
  });

  const appOption = map(useOption, (item) => {
    if (currentProjectClusters?.length === 0 && item.value === appMode.MOBILE) {
      return { ...item, disabled: true, disabledTip: i18n.t('dop:can-not-create-mobile-app-tip') };
    }
    return item;
  });

  const fieldsList = [
    {
      label: '',
      getComp: () => <div>{i18n.t('default:basic information')}</div>,
      extraProps: fieldExtraProps,
    },
    {
      label: i18n.t('dop:app types'),
      name: 'mode',
      type: 'radioGroup',
      options: useOption,
      initialValue: 'SERVICE',
      getComp: ({ form }: { form: FormInstance }) => (
        <AppTypeSelect
          imgOptions={appOption}
          onChangeType={(value: string) => {
            const obj = { mode: value };
            form.setFieldsValue(obj);
            setMode(value);
            setTempSelected('-1');
          }}
        />
      ),
    },
    {
      label: '',
      getComp: () => {
        const cur = modeOptions.find((m) => m.value === mode);
        return <Alert className="text-desc" type="info" message={cur?.desc} />;
      },
    },
    {
      label: i18n.t('dop:application name'),
      name: 'name',
      itemProps: {
        placeholder: i18n.t('project-app-name-tip'),
        maxLength: 50,
      },
      rules: [
        {
          pattern: /^[a-z0-9]+(-[a-z0-9]+)*$/,
          message: i18n.t('project-app-name-tip'),
        },
        {
          validator: (_rule: any, value: any, callback: (message?: string) => void) => {
            if (value && value.toLowerCase().endsWith('_ability')) {
              return callback(i18n.t('The name is reserved internally. Please change the name.'));
            }
            callback();
          },
        },
      ],
    },

    // {
    //   label: i18n.t('{name} identifier', { name: i18n.t('application') }),
    //   name: 'displayName',
    // },
    ...(mode === appMode.MOBILE
      ? [
          {
            // 暮志说后面有多个模板时，前端可能要做个判断，不同的模板参数会不一样，目前模板列表其实也是一个静态数据，选择的模板不用提交
            label: i18n.t('msp:template'),
            name: 'template',
            type: 'select',
            required: false,
            initialValue: '-1',
            options: map(template, (item) => item),
            itemProps: {
              placeholder: i18n.t('dop:please choose'),
              onChange: (v) => {
                const form = formRef.current;
                // 选择模板后，只能使用内置仓库
                if (v !== '-1') {
                  collectionRepoTemp(repoType);
                  form.setFieldsValue({ 'repoConfig.type': RepositoryMode.Internal });
                  setRepoType(RepositoryMode.Internal);
                }
                setTempSelected(v);
              },
            },
          },
          ...(tempSelected !== '-1'
            ? [
                {
                  label: i18n.t('dop:displayed name'),
                  name: 'mobileDisplayName',
                  type: 'input',
                  pattern: /^[\u4e00-\u9fa5_a-zA-Z0-9]+$/,
                  itemProps: {
                    placeholder: i18n.t('dop:chinese, letters, numbers'),
                    maxLength: 30,
                  },
                },
                {
                  label: 'Bundle Id',
                  name: 'bundleID',
                  type: 'input',
                  pattern: /^[a-zA-Z][0-9a-zA-Z]*(\.[a-zA-Z][0-9a-zA-Z]*)+$/,
                  itemProps: {
                    placeholder: `${i18n.t('such as')}：io.terminus.*`,
                    maxLength: 100,
                  },
                },
                {
                  label: `${i18n.t('dop:package name')}`,
                  name: 'packageName',
                  type: 'input',
                  pattern: /^[a-zA-Z][0-9a-zA-Z_]*(\.[a-zA-Z][0-9a-zA-Z_]*)+$/,
                  itemProps: {
                    placeholder: `${i18n.t('such as')}：io.terminus.*`,
                    maxLength: 100,
                  },
                },
              ]
            : []),
        ]
      : []),
    {
      label: i18n.t('dop:application description'),
      name: 'desc',
      type: 'textArea',
      required: false,
      itemProps: { rows: 4, maxLength: 200, style: { resize: 'none' } },
    },
    {
      label: i18n.t('dop:app logo'),
      name: 'logo',
      required: false,
      getComp: ({ form }: { form: FormInstance }) => <ImageUpload id="logo" form={form} showHint />,
    },
    ...insertWhen(mode !== appMode.PROJECT_SERVICE, [
      {
        label: '',
        getComp: () => <div>{i18n.t('dop:repository information')}</div>,
        extraProps: fieldExtraProps,
      },
      {
        label: i18n.t('repository'),
        name: ['repoConfig', 'type'],
        type: 'select',
        initialValue: RepositoryMode.Internal,
        options: map(
          filter(repositoriesTypes, (v) => v.usable),
          ({ name, value }) => ({ name, value }),
        ),
        itemProps: {
          disabled: mode === appMode.MOBILE && tempSelected !== '-1',
          onChange: (v) => {
            collectionRepoTemp(repoType);
            setRepoType(v);
          },
        },
      },
    ]),
    ...(repoType === RepositoryMode.Internal || (mode === appMode.MOBILE && tempSelected !== '-1')
      ? []
      : [
          {
            label: '',
            getComp: () => <Alert type="info" message={repositoriesTypes[repoType].desc} />,
          },
          {
            label: '',
            getComp: () => (
              <Alert
                showIcon
                type="warning"
                message={i18n.t(
                  'dop:It is recommended to use sources in the same region. Otherwise it may cause request timeout.',
                )}
              />
            ),
          },
          {
            label: i18n.t('dop:repository address'),
            name: ['repoConfig', 'url'],
            rules: [
              {
                pattern: /https?:\/\/[-a-zA-Z0-9]{1,256}\.[a-zA-Z0-9]{1,256}\b([-a-zA-Z0-9()@:%_\+.~#?&//=]*)/,
                message: i18n.t('dop:please enter valid repository address'),
              },
            ],
            itemProps: {
              placeholder: i18n.t('dop:Please enter the repository address started with http or https.'),
            },
          },
          {
            label: i18n.t('default:user name'),
            name: ['repoConfig', 'username'],
            itemProps: {
              placeholder: i18n.t('default:please enter'),
            },
          },
          {
            label: i18n.t('default:password'),
            name: ['repoConfig', 'password'],
            type: 'custom',
            getComp: () => <Input.Password />,
            itemProps: {
              placeholder: i18n.t('default:please enter'),
            },
          },
          {
            label: i18n.t('dop:repository description'),
            type: 'textArea',
            name: ['repoConfig', 'desc'],
            required: false,
            itemProps: { rows: 4, maxLength: 50, style: { resize: 'none' } },
          },
        ]),
    {
      getComp: ({ form }: { form: FormInstance }) => (
        <div className="mt-5">
          <Button type="primary" onClick={() => handleSubmit(form)}>
            {i18n.t('save')}
          </Button>
          <Button className="ml-3" onClick={() => window.history.back()}>
            {i18n.t('cancel')}
          </Button>
        </div>
      ),
    },
  ];

  return (
    <Spin spinning={isCreateApp || isInitApp} className="app-form-spin">
      <RenderForm ref={formRef} className="create-app-form" layout="vertical" list={fieldsList} />
    </Spin>
  );
};

export default CreationForm as any;
