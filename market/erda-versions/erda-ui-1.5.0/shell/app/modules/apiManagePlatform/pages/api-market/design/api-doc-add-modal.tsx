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
import { FormModal } from 'common';
import i18n from 'i18n';
import apiDesignStore from 'apiManagePlatform/stores/api-design';
import { map, filter } from 'lodash';
import { regRules } from 'common/utils';

interface IProps {
  visible: boolean;
  onClose: () => void;
  onSubmit: (e: any) => void;
}

const ApiDocAddModal = (props: IProps) => {
  const { visible, onClose, onSubmit } = props;

  const [branchList] = apiDesignStore.useStore((s) => [s.branchList]);

  const validBranchList = React.useMemo(() => {
    return filter(branchList, (item) => !item?.meta?.readOnly);
  }, [branchList]);

  const treeFieldList = [
    {
      label: i18n.t('dop:branch'),
      name: 'pinode',
      required: true,
      type: 'select',
      itemProps: {
        placeholder: i18n.t('please select {name}', { name: i18n.t('dop:branch') }),
        optionFilterProp: 'children',
        showSearch: true,
      },
      options: map(validBranchList, (branch: API_SETTING.IFileTree) => ({ name: branch.name, value: branch.inode })),
    },
    {
      label: i18n.t('name'),
      name: 'name',
      required: true,
      itemProps: {
        placeholder: i18n.t(
          'dop:please enter service name, which needs to be consistent with the service name declared in dice.yml',
        ),
      },
      rules: [regRules.commonStr],
    },
  ];

  return (
    <FormModal
      title={i18n.t('dop:create document')}
      fieldsList={treeFieldList}
      visible={visible}
      onOk={onSubmit}
      onCancel={onClose}
      modalProps={{
        destroyOnClose: true,
        maskClosable: false,
      }}
    />
  );
};

export default ApiDocAddModal;
