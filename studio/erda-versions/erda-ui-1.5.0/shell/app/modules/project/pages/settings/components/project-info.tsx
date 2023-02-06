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
import { Tooltip, Button, Input, Checkbox } from 'antd';
import { FormInstance } from 'core/common/interface';
import { theme } from 'app/themes';
import { ImageUpload, Icon as CustomIcon, ConfirmDelete } from 'common';
import { goTo, insertWhen } from 'common/utils';
import { SectionInfoEdit } from 'project/common/components/section-info-edit';
import projectStore from 'app/modules/project/stores/project';
import { useQuotaFields } from 'org/pages/projects/create-project';
import layoutStore from 'layout/stores/layout';
import { removeMember } from 'common/services/index';
import routeInfoStore from 'core/stores/route';
import { updateTenantProject, deleteTenantProject } from 'msp/services';
import { HeadProjectSelector } from 'project/common/components/project-selector';
import userStore from 'app/user/stores';

interface IProps {
  canEdit: boolean;
  canDelete: boolean;
  canEditQuota: boolean;
  showQuotaTip: boolean;
}

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

export default ({ canEdit, canDelete, canEditQuota, showQuotaTip }: IProps) => {
  const { updateProject, deleteProject, getLeftResources } = projectStore.effects;
  const loginUser = userStore.useStore((s) => s.loginUser);
  const orgName = routeInfoStore.useStore((s) => s.params.orgName);
  const info = projectStore.useStore((s) => s.info);
  const [confirmProjectName, setConfirmProjectName] = React.useState('');
  const [canGetClusterListAndResources, setCanGetClusterListAndResources] = React.useState(false);
  const [ifConfigCluster, setIfConfigCluster] = React.useState(false);
  const [ifConfigClusterDisable, setIfConfigClusterDisable] = React.useState(false);
  const updatePrj = (values: Obj) => {
    const { isPublic, resourceConfig } = values;
    if (resourceConfig) {
      Object.keys(values.resourceConfig)
        .filter((key) => resourceConfig[key])
        .forEach((key) => {
          resourceConfig[key] = {
            ...resourceConfig[key],
            cpuQuota: +resourceConfig[key].cpuQuota,
            memQuota: resourceConfig[key].memQuota,
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

  React.useEffect(() => {
    if (info.resourceConfig) {
      setIfConfigCluster(true);
      setIfConfigClusterDisable(true);
    } else {
      setIfConfigCluster(false);
      setIfConfigClusterDisable(false);
    }
  }, [info]);

  const notMSP = info.type !== 'MSP';
  const fieldsList = [
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
    ...insertWhen(notMSP, [
      {
        getComp: ({ readOnly }: { readOnly: boolean }) =>
          !readOnly ? (
            <Checkbox
              checked={ifConfigCluster}
              disabled={ifConfigClusterDisable}
              onChange={() => setIfConfigCluster(!ifConfigCluster)}
            >
              {i18n.t('cmp:need to configure project cluster resources')}
            </Checkbox>
          ) : (
            ''
          ),
      },
    ]),
    ...insertWhen(
      notMSP && ifConfigCluster,
      useQuotaFields(canEditQuota, showQuotaTip, canGetClusterListAndResources, info),
    ),
    // {
    //   label: i18n.t('dop:DingTalk notification address'),
    //   name: 'ddHook',
    //   required: false,
    // },
  ];

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

  const extraSectionList = [
    {
      title: i18n.t('exit {name}', { name: i18n.t('project') }),
      children: (
        <ConfirmDelete
          title={i18n.t('sure to exit the current {name}?', { name: i18n.t('project') })}
          confirmTip={i18n.t('common:exit-confirm-tip {name}', { name: i18n.t('project') })}
          secondTitle={i18n.t('common:exit-sub-tip {name}', { name: i18n.t('project') })}
          onConfirm={exitProject}
        >
          <Button danger>{i18n.t('common:exit current {name}', { name: i18n.t('project') })}</Button>
        </ConfirmDelete>
      ),
    },
  ];
  if (canDelete) {
    extraSectionList.push({
      title: i18n.t('dop:delete project'),
      children: (
        <ConfirmDelete
          deleteItem={i18n.t('project')}
          onConfirm={onDelete}
          secondTitle={i18n.t('dop:The project cannot be restored after deletion. Please enter {name} to confirm.', {
            name: info.displayName,
          })}
          onCancel={() => setConfirmProjectName('')}
          disabledConfirm={confirmProjectName !== info.displayName}
          modalChildren={
            <Input
              value={confirmProjectName}
              placeholder={i18n.t('please enter {name}', { name: i18n.t('project name') })}
              onChange={(e: React.ChangeEvent<HTMLInputElement>) => setConfirmProjectName(e.target.value)}
            />
          }
        />
      ),
    });
  }

  const formName = i18n.t('dop:project info');
  return (
    <SectionInfoEdit
      hasAuth={canEdit}
      setCanGetClusterListAndResources={setCanGetClusterListAndResources}
      data={{ ...info, isPublic: `${info.isPublic || 'false'}` }}
      fieldsList={fieldsList}
      updateInfo={updatePrj}
      extraSections={extraSectionList}
      name={
        info.id && inOrgCenter && notMSP ? (
          <div>
            {formName}
            <Tooltip title={i18n.t('dop:applications')}>
              <CustomIcon
                type="link1"
                className="ml-2 hover-active"
                onClick={() => goTo(goTo.pages.project, { projectId: info.id })}
              />
            </Tooltip>
          </div>
        ) : (
          formName
        )
      }
      formName={formName}
    />
  );
};
