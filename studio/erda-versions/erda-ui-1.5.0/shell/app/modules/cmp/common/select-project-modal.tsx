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
import { map } from 'lodash';
import { FormModal, LoadMoreSelector } from 'common';
import { getJoinedProjects } from 'user/services/user';
import i18n from 'i18n';

interface IProps {
  visible: boolean;
  toggleModal: () => void;
  onOk: (projectId: number) => void;
}

export default ({ visible, toggleModal, onOk }: IProps) => {
  const _getProjectList = (q: any) => {
    return getJoinedProjects({ ...q }).then((res: any) => res.data);
  };

  const fieldList = [
    {
      label: i18n.t('project'),
      name: 'project',
      type: 'custom',
      getComp: () => (
        <LoadMoreSelector
          getData={_getProjectList}
          placeholder={i18n.t('dop:please select project')}
          dataFormatter={({ list, total }: { list: any[]; total: number }) => ({
            total,
            list: map(list, (project) => {
              const { name, id } = project;
              return {
                ...project,
                label: name,
                value: id,
              };
            }),
          })}
        />
      ),
    },
  ];

  const handleSubmit = ({ project }: { project: number }) => onOk(project);

  return (
    <FormModal
      title={i18n.t('cmp:select project')}
      fieldsList={fieldList}
      visible={visible}
      onOk={handleSubmit}
      onCancel={toggleModal}
    />
  );
};
