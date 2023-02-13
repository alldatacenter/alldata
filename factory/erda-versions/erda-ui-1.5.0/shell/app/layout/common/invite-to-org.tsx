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
import { isEmpty } from 'lodash';
import { useMount } from 'react-use';
import { Input, Button, message } from 'antd';
import { Avatar } from 'common';
import { useUpdate } from 'common/use-hooks';
import userStore from 'app/user/stores';
import { useLoading } from 'core/stores/loading';
import layoutStore from 'layout/stores/layout';
import routeInfoStore from 'core/stores/route';
import { getOrgByDomain } from 'app/org-home/services/org';

const InviteToOrg = () => {
  const loginUser = userStore.useStore((s) => s.loginUser);
  const { inviteToOrg } = layoutStore.effects;
  const orgName = routeInfoStore.useStore((s) => s.params.orgName);
  const [inviting] = useLoading(layoutStore, ['inviteToOrg']);
  const { id, nick, name } = loginUser;

  const [{ code, domainData }, updater] = useUpdate({
    code: undefined as unknown as string,
    domainData: {},
  });

  useMount(() => {
    let domain = window.location.hostname;
    if (domain.startsWith('local')) {
      domain = domain.split('.').slice(1).join('.');
    }

    getOrgByDomain({ domain, orgName }).then((res: any) => {
      res.success && !isEmpty(res.data) && updater.domainData(res.data);
    });
  });

  const handleJoinOrg = () => {
    if (!code) {
      message.warning(i18n.t('verification code is empty!'));
      return;
    }
    domainData.id &&
      inviteToOrg({
        verifyCode: code,
        userIds: [id],
        orgId: String(domainData.id),
      }).then(() => {
        message.success(i18n.t('joined organization successfully'));
        setTimeout(() => {
          location.href = `/${orgName}`;
        }, 200);
      });
  };

  return (
    <div className="invite-to-org h-full flex flex-wrap justify-center items-center">
      <div className="invite-to-org-ct text-center">
        <Avatar className="mb-4" useLoginUser size={80} />
        <p className="mb-8 text-2xl font-bold" style={{ lineHeight: '1em' }}>{`${i18n.t('Welcome!')} ${
          nick || name
        }`}</p>
        <Input
          value={code}
          className="mb-4"
          width="100%"
          placeholder={i18n.t('please enter verification code')}
          onChange={(e: React.ChangeEvent<HTMLInputElement>) => updater.code(e.target.value.trim())}
        />
        <Button style={{ width: '100%' }} loading={inviting} type="primary" disabled={!code} onClick={handleJoinOrg}>
          {domainData.displayName ? i18n.t('join {orgName}', { orgName: domainData.displayName }) : i18n.t('join org')}
        </Button>
      </div>
    </div>
  );
};

export default InviteToOrg;
