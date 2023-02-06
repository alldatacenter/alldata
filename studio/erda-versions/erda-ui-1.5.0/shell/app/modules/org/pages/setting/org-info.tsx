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
import { insertWhen, goTo } from 'common/utils';
import { Button } from 'antd';
import { FormInstance } from 'core/common/interface';
import { SectionInfoEdit } from 'project/common/components/section-info-edit';
import userStore from 'app/user/stores';
import orgStore from 'app/org-home/stores/org';

import { removeMember } from 'common/services/index';
import i18n from 'i18n';

export const OrgInfo = () => {
  const currentOrg = orgStore.useStore((s) => s.currentOrg);
  const loginUser = userStore.useStore((s) => s.loginUser);
  const [isPublisher, setIsPublisher] = React.useState(false);
  const isEnterprise = currentOrg.type === 'ENTERPRISE';
  const fieldsList = [
    {
      name: 'id',
      itemProps: { type: 'hidden' },
    },
    {
      label: i18n.t('{name} identifier', { name: i18n.t('organization') }),
      name: 'name',
      itemProps: {
        maxLength: 50,
        disabled: true,
      },
      rules: [
        { required: true, message: i18n.t('cmp:Please enter the organization identifier.') },
        {
          pattern: /^[a-z0-9-]*$/,
          message: i18n.t('cmp:consist of lowercase letters, numbers and hyphens'),
        },
      ],
    },
    {
      label: i18n.t('cmp:org name'),
      name: 'displayName',
    },
    ...insertWhen(!currentOrg.publisherId && isEnterprise, [
      {
        label: i18n.t('cmp:become a publisher'),
        required: false,
        type: 'switch',
        itemProps: {
          onChange: (v: boolean) => setIsPublisher(v),
        },
      },
    ]),
    ...insertWhen(isPublisher && isEnterprise, [
      {
        label: i18n.t('publisher:repository name'),
        name: 'publisherName',
        itemProps: {
          maxLength: 50,
        },
      },
    ]),
    {
      label: i18n.t('cmp:notice language'),
      name: 'locale',
      type: 'select',
      itemProps: {
        placeholder: i18n.t('cmp:used for site messages and mails'),
      },
      options: [
        { value: 'zh-CN', name: i18n.t('cmp:Chinese') },
        { value: 'en-US', name: i18n.t('cmp:English') },
      ],
    },
    {
      label: i18n.t('whether to put {name} in public', { name: i18n.t('organization') }),
      name: 'isPublic',
      type: 'radioGroup',
      options: [
        {
          name: i18n.t('cmp:public org'),
          value: 'true',
        },
        {
          name: i18n.t('cmp:private org'),
          value: 'false',
        },
      ],
    },
    {
      label: i18n.t('cmp:org logo'),
      name: 'logo',
      required: false,
      getComp: ({ form }: { form: FormInstance }) => <ImageUpload id="logo" form={form} showHint />,
      viewType: 'image',
    },
    {
      label: i18n.t('cmp:org description'),
      name: 'desc',
      itemProps: {
        type: 'textarea',
        maxLength: 500,
      },
    },
    // {
    //   label: 'smsKeyID',
    //   name: 'config.smsKeyID',
    //   required: false,
    //   itemProps: {
    //     maxLength: 500,
    //   },
    // },
    // {
    //   label: 'smsKeySecret',
    //   name: 'config.smsKeySecret',
    //   required: false,
    //   itemProps: {
    //     maxLength: 500,
    //   },
    // },
    // {
    //   label: 'smsSignName',
    //   name: 'config.smsSignName',
    //   required: false,
    //   itemProps: {
    //     maxLength: 500,
    //   },
    // },
    // {
    //   label: 'smtpHost',
    //   name: 'config.smtpHost',
    //   required: false,
    //   itemProps: {
    //     maxLength: 500,
    //   },
    // },
    // {
    //   label: 'smtpIsSSL',
    //   name: 'config.smtpIsSSL',
    //   required: false,
    //   initialValue: true,
    //   type: 'checkbox',
    // },
    // {
    //   label: 'smtpPassword',
    //   name: 'config.smtpPassword',
    //   required: false,
    //   itemProps: {
    //     maxLength: 500,
    //   },
    // },
    // {
    //   label: 'smtpPort',
    //   name: 'config.smtpPort',
    //   required: false,
    //   type: 'inputNumber',
    //   initialValue: 465,
    //   itemProps: {
    //     min: 0,
    //   },
    // },
    // {
    //   label: 'smtpUser',
    //   name: 'config.smtpUser',
    //   required: false,
    //   itemProps: {
    //     maxLength: 500,
    //   },
    // },
  ];

  const updateInfo = (values: Obj) => {
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const { isPublisher: _isPublisher, isPublic, ...rest } = values;
    orgStore.effects.updateOrg({ ...rest, isPublic: isPublic === 'true' });
  };

  const exitOrg = () => {
    removeMember({
      scope: { type: 'org', id: `${currentOrg.id}` },
      userIds: [loginUser.id],
    }).then(() => {
      goTo(goTo.pages.orgRoot, { orgName: '-' });
    });
  };

  return (
    <SectionInfoEdit
      hasAuth // 系统管理员默认有权限
      data={{ ...currentOrg, isPublic: `${currentOrg.isPublic || 'false'}` }}
      fieldsList={fieldsList}
      extraSections={[
        {
          title: i18n.t('exit {name}', { name: i18n.t('org') }),
          children: (
            <ConfirmDelete
              title={i18n.t('sure to exit the current {name}?', { name: i18n.t('org') })}
              confirmTip={i18n.t('common:exit-confirm-tip {name}', { name: i18n.t('org') })}
              secondTitle={i18n.t('common:exit-sub-tip {name}', { name: i18n.t('org') })}
              onConfirm={exitOrg}
            >
              <Button danger>{i18n.t('common:exit current {name}', { name: i18n.t('org') })}</Button>
            </ConfirmDelete>
          ),
        },
      ]}
      updateInfo={updateInfo}
      name={i18n.t('cmp:org info')}
    />
  );
};
