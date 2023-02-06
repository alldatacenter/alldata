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
import i18n from 'i18n';
import { Row, Col, Button, Input, Tooltip } from 'antd';
import { FormInstance } from 'core/common/interface';
import { theme } from 'app/themes';
import { ImageUpload, ErdaIcon, ConfirmDelete, Panel, Ellipsis, Icon as CustomIcon, FormModal } from 'common';
import { goTo, insertWhen } from 'common/utils';
import projectStore from 'app/modules/project/stores/project';
import { useQuotaFields } from 'org/pages/projects/create-project';
import layoutStore from 'layout/stores/layout';
import { removeMember } from 'common/services/index';
import routeInfoStore from 'core/stores/route';
import { updateTenantProject, deleteTenantProject } from 'msp/services';
import { HeadProjectSelector } from 'project/common/components/project-selector';
import userStore from 'app/user/stores';
import Card from 'org/common/card';
import { WORKSPACE_LIST } from 'common/constants';

// 修改项目信息后，更新左侧菜单上方的信息
let selectorKey = 1;
const reloadHeadInfo = () => {
  const detail = projectStore.getState((s) => s.info);
  layoutStore.reducers.setSubSiderInfoMap({
    key: 'project',
    detail: { ...detail, icon: theme.projectIcon },
    getHeadName: () => <HeadProjectSelector key={selectorKey} />, // 重新加载selector
  });
  selectorKey += 1;
};

const workSpaceList = ['DEV', 'TEST', 'STAGING', 'PROD'];

const resourceMap = {
  DEV: i18n.t('dev environment'),
  TEST: i18n.t('test environment'),
  STAGING: i18n.t('staging environment'),
  PROD: i18n.t('prod environment'),
};

const resourceIconMap = {
  DEV: <ErdaIcon type="dev" size={40} />,
  TEST: <ErdaIcon type="test" size={40} />,
  STAGING: <ErdaIcon type="staging" size={40} />,
  PROD: <ErdaIcon type="prod" size={40} />,
};

const Info = () => {
  const { updateProject, deleteProject, getLeftResources } = projectStore.effects;
  const loginUser = userStore.useStore((s) => s.loginUser);
  const orgName = routeInfoStore.useStore((s) => s.params.orgName);
  const info = projectStore.useStore((s) => s.info);
  const [confirmProjectName, setConfirmProjectName] = React.useState('');

  const [projectInfoEditVisible, setProjectInfoEditVisible] = React.useState(false);
  const [projectQuotaEditVisible, setProjectQuotaEditVisible] = React.useState(false);
  const [projectRollbackEditVisible, setProjectRollbackEditVisible] = React.useState(false);

  const { rollbackConfig } = info;

  const updatePrj = (values: Obj) => {
    const { isPublic = String(info.isPublic), resourceConfig } = values;
    if (resourceConfig) {
      Object.keys(values.resourceConfig)
        .filter((key) => resourceConfig[key])
        .forEach((key) => {
          resourceConfig[key] = {
            ...resourceConfig[key],
            cpuQuota: +resourceConfig[key].cpuQuota,
            memQuota: +resourceConfig[key].memQuota,
          };
        });
    }

    return updateProject({ ...values, isPublic: isPublic === 'true' }).then(() => {
      updateTenantProject({
        id: `${info.id}`,
        name: values.name,
        displayName: values.displayName,
        type: info.type === 'MSP' ? 'MSP' : 'DOP',
      });
      getLeftResources();
      reloadHeadInfo();
    });
  };

  const notMSP = info.type !== 'MSP';
  const fieldsListInfo = [
    {
      label: i18n.t('{name} identifier', { name: i18n.t('project') }),
      name: 'name',
      itemProps: {
        disabled: true,
      },
    },
    {
      label: i18n.t('project name'),
      name: 'displayName',
    },
    ...insertWhen(notMSP, [
      {
        label: i18n.t('whether to put {name} in public', { name: i18n.t('project') }),
        name: 'isPublic',
        type: 'radioGroup',
        options: [
          {
            name: i18n.t('public project'),
            value: 'true',
          },
          {
            name: i18n.t('dop:private project'),
            value: 'false',
          },
        ],
      },
    ]),
    {
      label: i18n.t('project icon'),
      name: 'logo',
      required: false,
      getComp: ({ form }: { form: FormInstance }) => <ImageUpload id="logo" form={form} showHint />,
      viewType: 'image',
    },
    {
      label: i18n.t('project description'),
      name: 'desc',
      type: 'textArea',
      required: false,
      itemProps: { rows: 4, maxLength: 200 },
    },
  ];

  const fieldsListQuota = useQuotaFields(true, true);

  const inOrgCenter = location.pathname.startsWith(`/${orgName}/orgCenter`);
  const onDelete = async () => {
    setConfirmProjectName('');
    await deleteProject();
    await deleteTenantProject({ projectId: info.id });
    if (inOrgCenter) {
      goTo(goTo.pages.orgCenterRoot, { replace: true });
    } else {
      goTo(goTo.pages.dopRoot, { replace: true });
    }
  };

  const exitProject = () => {
    removeMember({
      scope: { type: 'project', id: `${info.id}` },
      userIds: [loginUser.id],
    }).then(() => {
      goTo(goTo.pages.dopRoot, { replace: true });
    });
  };

  const configData = {};
  const tableData: object[] = [];
  const fieldsListRollback: object[] = [];
  const sortBy = WORKSPACE_LIST;
  sortBy.forEach((workspace) => {
    const name = workspace.toUpperCase();
    const point = rollbackConfig?.[workspace];

    tableData.push({ workspace, point });
    configData[`${name}`] = point || 5;
    fieldsListRollback.push({
      label: resourceMap[name] || name,
      name: ['rollbackConfig', name],
      type: 'inputNumber',
      itemProps: {
        max: 1000,
        min: 1,
        precision: 0,
      },
    });
  });

  return (
    <div className="project-setting-info">
      <Card
        header={
          <div>
            {i18n.t('dop:basic information')}
            {notMSP ? (
              <Tooltip title={i18n.t('projects')}>
                <CustomIcon
                  type="link1"
                  className="ml-2 hover-active"
                  onClick={() => goTo(goTo.pages.project, { projectId: info.id })}
                />
              </Tooltip>
            ) : null}
          </div>
        }
        actions={
          <span className="hover-active" onClick={() => setProjectInfoEditVisible(true)}>
            <ErdaIcon type="edit" size={16} className="mr-2 align-middle " />
          </span>
        }
      >
        <Row>
          <Col span={12} className="flex items-center h-20">
            {info.logo && <img src={info.logo} className="w-16 h-16 mr-4" />}
            <div>
              <div className="text-xl label">{info.displayName}</div>
              <div className="desc">{info.desc}</div>
            </div>
          </Col>
          <Col span={12} className="py-5">
            <Panel
              columnNum={2}
              fields={[
                {
                  label: <Ellipsis title={info.name} />,
                  value: i18n.t('project identifier'),
                },
                {
                  label: info.isPublic ? i18n.t('public project') : i18n.t('dop:private project'),
                  value: i18n.t('whether to put {name} in public', { name: i18n.t('project') }),
                },
              ]}
            />
          </Col>
        </Row>
      </Card>

      {notMSP && (
        <>
          <Card
            header={i18n.t('dop:project quota')}
            actions={
              <span className="hover-active" onClick={() => setProjectQuotaEditVisible(true)}>
                <ErdaIcon type="edit" size={16} className="mr-2 align-middle" />
              </span>
            }
          >
            {info.resourceConfig
              ? workSpaceList.map((key: string) => {
                  const resource = info.resourceConfig[key];
                  return (
                    <div className="erda-panel-list">
                      <Row>
                        <Col span={8} className="flex">
                          <div className="flex mr-3">{resourceIconMap[key]}</div>
                          <div>
                            <div className="label mb-1">{resourceMap[key]}</div>
                            <div className="text-xs">{resource.clusterName}</div>
                          </div>
                        </Col>
                        <Col span={8}>
                          <Panel
                            columnNum={4}
                            fields={[
                              {
                                value: (
                                  <div className="text-right relative top-1">
                                    <ErdaIcon type="CPU" size={34} />
                                  </div>
                                ),
                              },
                              {
                                label: `${+(+resource.cpuQuota).toFixed(3)} core`,
                                value: i18n.t('CPU quota'),
                              },
                              {
                                label: `${+(+resource.cpuRequest).toFixed(3)} core`,
                                value: i18n.t('used'),
                              },
                              {
                                label: `${+(+resource.cpuRequestRate).toFixed(3)} %`,
                                value: i18n.t('cmp:usage rate'),
                              },
                            ]}
                          />
                        </Col>
                        <Col span={8}>
                          <Panel
                            columnNum={4}
                            fields={[
                              {
                                value: (
                                  <div className="text-right relative top-1">
                                    <ErdaIcon type="GPU" size={34} />
                                  </div>
                                ),
                              },
                              {
                                label: `${+(+resource.memQuota).toFixed(3)} GiB`,
                                value: i18n.t('Memory quota'),
                              },
                              {
                                label: `${+(+resource.memRequest).toFixed(3)} GiB`,
                                value: i18n.t('used'),
                              },
                              {
                                label: `${+(+resource.memRequestRate).toFixed(3)} %`,
                                value: i18n.t('cmp:usage rate'),
                              },
                            ]}
                          />
                        </Col>
                      </Row>
                    </div>
                  );
                })
              : i18n.t('no quota')}
          </Card>
          <FormModal
            onOk={(result) => updatePrj(result).then(() => setProjectQuotaEditVisible(false))}
            onCancel={() => setProjectQuotaEditVisible(false)}
            name={i18n.t('dop:project quota')}
            visible={projectQuotaEditVisible}
            fieldsList={fieldsListQuota}
            formData={{ ...info, isPublic: `${info.isPublic || 'false'}` }}
          />
        </>
      )}

      <Card
        header={i18n.t('advanced settings')}
        actions={
          notMSP ? (
            <span className="hover-active" onClick={() => setProjectRollbackEditVisible(true)}>
              <ErdaIcon type="edit" size={16} className="mr-2 align-middle" />
            </span>
          ) : null
        }
      >
        {notMSP ? (
          <>
            <div className="label">{i18n.t('dop:rollback setting')}</div>
            <Row className="erda-panel-list">
              {Object.keys(info.rollbackConfig || {}).map((key: string) => (
                <Col span={6} className="flex">
                  <div className="flex mr-3">{resourceIconMap[key]}</div>
                  <div>
                    <div className="label">{info.rollbackConfig[key]}</div>
                    <div className="text-xs">{resourceMap[key]}</div>
                  </div>
                </Col>
              ))}
            </Row>
            <div className="label">{i18n.t('other settings')}</div>
          </>
        ) : null}
        <Row>
          <Col span={12} className="pr-2">
            <div className="erda-panel-list flex justify-between items-center">
              <div className="flex">
                <ErdaIcon type="dev" size={40} className="mr-3" />
                <div>
                  <div className="label">{i18n.t('common:exit current {name}', { name: i18n.t('project') })}</div>
                  <div className="text-xs">{i18n.t('common:exit-confirm-tip {name}', { name: i18n.t('project') })}</div>
                </div>
              </div>
              <ConfirmDelete
                confirmTip={false}
                title={i18n.t('sure to exit the current {name}?', { name: i18n.t('project') })}
                secondTitle={i18n.t('common:exit-sub-tip {name}', { name: i18n.t('project') })}
                onConfirm={exitProject}
              >
                <Button danger>{i18n.t('exit')}</Button>
              </ConfirmDelete>
            </div>
          </Col>
          <Col span={12} className="pl-2">
            <div className="erda-panel-list flex justify-between items-center">
              <div className="flex">
                <ErdaIcon type="dev" size={40} className="mr-3" />
                <div>
                  <div className="label">
                    {i18n.t('common:delete current {deleteItem}', { deleteItem: i18n.t('project') })}
                  </div>
                  <div className="text-xs">
                    {i18n.t('Permanently delete {deleteItem}. Please pay special attention to it.', {
                      deleteItem: i18n.t('project'),
                    })}
                  </div>
                </div>
              </div>
              <ConfirmDelete
                onConfirm={onDelete}
                deleteItem={`${i18n.t('project')}?`}
                onCancel={() => setConfirmProjectName('')}
                disabledConfirm={confirmProjectName !== info.displayName}
                confirmTip={false}
                secondTitle={i18n.t(
                  'dop:The project cannot be restored after deletion. Please enter {name} to confirm.',
                  {
                    name: info.displayName,
                  },
                )}
                modalChildren={
                  <Input
                    value={confirmProjectName}
                    placeholder={i18n.t('please enter {name}', { name: i18n.t('project name') })}
                    onChange={(e: React.ChangeEvent<HTMLInputElement>) => setConfirmProjectName(e.target.value)}
                  />
                }
              >
                <Button danger>{i18n.t('delete')}</Button>
              </ConfirmDelete>
            </div>
          </Col>
        </Row>
      </Card>

      <FormModal
        onOk={(result) => updatePrj(result).then(() => setProjectInfoEditVisible(false))}
        onCancel={() => setProjectInfoEditVisible(false)}
        name={i18n.t('dop:project info')}
        visible={projectInfoEditVisible}
        fieldsList={fieldsListInfo}
        formData={{ ...info, isPublic: `${info.isPublic || 'false'}` }}
      />
      <FormModal
        onOk={(result) =>
          updateProject({ ...result, isPublic: info.isPublic }).then(() => setProjectRollbackEditVisible(false))
        }
        onCancel={() => setProjectRollbackEditVisible(false)}
        name={i18n.t('dop:rollback point')}
        visible={projectRollbackEditVisible}
        fieldsList={fieldsListRollback}
        formData={{ rollbackConfig: configData }}
      />
    </div>
  );
};

export default Info;
