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
import { ImageUpload, ConfirmDelete } from 'common';
import { goTo } from 'common/utils';
import { FormInstance } from 'core/common/interface';
import { Button, Input } from 'antd';
import { SectionInfoEdit } from 'project/common/components/section-info-edit';
import { modeOptions } from 'application/common/config';
import { usePerm } from 'app/user/common';
import { filter } from 'lodash';
import i18n from 'i18n';
import appStore from 'application/stores/application';
import layoutStore from 'layout/stores/layout';
import userStore from 'app/user/stores';
import { removeMember } from 'common/services/index';
import routeInfoStore from 'core/stores/route';
import { theme } from 'app/themes';

// 修改应用信息后，更新左侧菜单上方的信息
const reloadHeadInfo = () => {
  const detail = appStore.getState((s) => s.detail);
  layoutStore.reducers.setSubSiderInfoMap({
    key: 'application',
    detail: { ...detail, icon: theme.appIcon }, // name不可编辑，若可编辑需重新加载Selector，参考project-info
  });
};

const PureAppInfo = (): JSX.Element => {
  const appDetail = appStore.useStore((s) => s.detail);
  const loginUser = userStore.useStore((s) => s.loginUser);
  const projectId = routeInfoStore.useStore((s) => s.params.projectId);
  const { updateAppDetail, remove } = appStore.effects;
  const { protocol, host } = window.location;

  const [confirmAppName, setConfirmAppName] = React.useState('');
  const permMap = usePerm((s) => s.app.setting);
  const gitRepo = `${protocol}//${appDetail.gitRepoNew}`;
  const fieldsList = [
    {
      label: i18n.t('dop:app name'),
      name: 'name',
      itemProps: {
        disabled: true,
      },
    },
    // {
    //   label: i18n.t('dop:app name'),
    //   name: 'displayName',
    // },
    {
      label: i18n.t('dop:app types'),
      name: 'mode',
      type: 'radioGroup',
      options: filter(modeOptions, (item) => item.value !== 'ABILITY'),
      itemProps: {
        disabled: true,
      },
    },
    {
      label: i18n.t('dop:app repository address'),
      name: 'gitRepo',
      itemProps: {
        disabled: true,
      },
    },
    {
      label: i18n.t('whether to put {name} in public', { name: i18n.t('application') }),
      name: 'isPublic',
      type: 'radioGroup',
      options: [
        {
          name: i18n.t('dop:public application'),
          value: 'true',
        },
        {
          name: i18n.t('dop:private application'),
          value: 'false',
        },
      ],
    },
    {
      label: i18n.t('dop:application description'),
      name: 'desc',
      type: 'textArea',
      required: false,
      itemProps: { rows: 4, maxLength: 200 },
    },
    {
      label: i18n.t('dop:app icon'),
      name: 'logo',
      required: false,
      getComp: ({ form }: { form: FormInstance }) => <ImageUpload id="logo" form={form} showHint />,
      viewType: 'image',
    },
    // {
    //   label: i18n.t('dop:DingTalk notification address'),
    //   name: 'config.ddHookUrl',
    //   required: false,
    // },
  ];

  const exitApp = () => {
    removeMember({
      scope: { type: 'app', id: `${appDetail.id}` },
      userIds: [loginUser.id],
    }).then(() => {
      goTo(goTo.pages.project, { projectId, replace: true });
    });
  };

  const extraSectionList = [
    {
      title: i18n.t('exit {name}', { name: i18n.t('application') }),
      children: (
        <ConfirmDelete
          title={i18n.t('sure to exit the current {name}?', { name: i18n.t('application') })}
          confirmTip={i18n.t('common:exit-confirm-tip {name}', { name: i18n.t('application') })}
          secondTitle={i18n.t('common:exit-sub-tip {name}', { name: i18n.t('application') })}
          onConfirm={exitApp}
        >
          <Button danger>{i18n.t('common:exit current {name}', { name: i18n.t('application') })}</Button>
        </ConfirmDelete>
      ),
    },
  ];

  if (permMap.deleteApp.pass) {
    extraSectionList.push({
      title: i18n.t('dop:delete application'),
      children: (
        <ConfirmDelete
          deleteItem={i18n.t('application')}
          onConfirm={remove}
          secondTitle={i18n.t('dop:The application cannot be restored after deletion.Please enter {name} to confirm.', {
            name: appDetail.name,
          })}
          onCancel={() => setConfirmAppName('')}
          disabledConfirm={confirmAppName !== appDetail.name}
          modalChildren={
            <Input
              value={confirmAppName}
              placeholder={i18n.t('please enter {name}', { name: i18n.t('dop:app name') })}
              onChange={(e: React.ChangeEvent<HTMLInputElement>) => setConfirmAppName(e.target.value)}
            />
          }
        />
      ),
    });
  }

  const onUpdate = (val: Obj) => {
    const { isPublic, ...rest } = val;
    return updateAppDetail({ ...rest, isPublic: isPublic === 'true' }).then(() => {
      reloadHeadInfo();
    });
  };

  return (
    <SectionInfoEdit
      hasAuth={permMap.editApp.pass}
      data={{ ...appDetail, gitRepo, isPublic: `${appDetail.isPublic || 'false'}` }}
      fieldsList={fieldsList}
      updateInfo={onUpdate}
      extraSections={extraSectionList}
      name={i18n.t('dop:basic information')}
    />
  );
};

export { PureAppInfo as AppInfo };
