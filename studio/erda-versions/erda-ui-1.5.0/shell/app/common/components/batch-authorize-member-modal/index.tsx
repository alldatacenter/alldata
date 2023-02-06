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
import { FormModal, LoadMoreSelector } from 'common';
import { map } from 'lodash';
import { useMount } from 'react-use';
import i18n from 'i18n';
import { MemberScope } from 'common/stores/member-scope';
import appMemberStore from 'common/stores/application-member';
import { getApps } from 'common/services';

interface IProps {
  projectId: string;
  [k: string]: any;
}

const BatchAuthorizeMemberModal = ({ projectId, ...rest }: IProps) => {
  const { getRoleMap } = appMemberStore.effects;
  const roleMap = appMemberStore.useStore((s) => s.roleMap);

  useMount(() => getRoleMap({ scopeType: MemberScope.APP }));

  const _getApps = (q: any) => {
    return getApps({ ...q }).then((res: any) => res.data);
  };

  const fieldsList = React.useMemo(
    () => [
      {
        label: i18n.t('application'),
        name: 'applications',
        type: 'custom',
        getComp: () => (
          <LoadMoreSelector
            getData={_getApps}
            mode="multiple"
            placeholder={i18n.t('dop:please select application')}
            extraQuery={{ projectId }}
            dataFormatter={({ list, total }: { list: any[]; total: number }) => ({
              total,
              list: map(list, (application) => {
                const { name, id } = application;
                return {
                  ...application,
                  label: name,
                  value: id,
                };
              }),
            })}
          />
        ),
      },
      {
        label: i18n.t('role'),
        name: 'roles',
        type: 'select',
        itemProps: {
          mode: 'multiple',
          placeholder: i18n.t('dop:please set'),
        },
        options: [
          // { name: i18n.t('not member'), value: '' },
          ...map(roleMap, (v: string, k: string) => ({ name: v, value: k })),
        ],
      },
    ],
    [projectId, roleMap],
  );

  return <FormModal title={i18n.t('common:batch authorize application')} fieldsList={fieldsList} {...rest} />;
};

export default BatchAuthorizeMemberModal;
