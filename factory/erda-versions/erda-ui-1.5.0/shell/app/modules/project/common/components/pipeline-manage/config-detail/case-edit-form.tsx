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

interface IProps {
  visible: boolean;
  onClose: () => void;
  onOk: (arg: any) => void;
  editData?: AUTO_TEST.ICaseDetail;
  scope?: string;
}

const nameMap = {
  projectPipeline: i18n.t('pipeline'),
  configSheet: i18n.t('dop:config data'),
};

const CaseEditForm = (props: IProps) => {
  const { visible, onOk, onClose, editData, scope = '' } = props;
  const fieldList = [
    {
      label: i18n.t('name'),
      name: 'name',
      itemProps: {
        maxLength: 50,
      },
    },
    {
      label: i18n.t('description'),
      name: 'desc',
      type: 'textArea',
      required: false,
      itemProps: {
        maxLength: 2000,
      },
    },
  ];

  return (
    <FormModal
      name={nameMap[scope]}
      fieldsList={fieldList}
      visible={visible}
      onOk={onOk}
      formData={editData}
      onCancel={onClose}
      modalProps={{
        destroyOnClose: true,
        maskClosable: false,
      }}
    />
  );
};

export default CaseEditForm;
