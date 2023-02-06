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

interface IForm {
  instanceIds: string[];
  region: string;
  vendor: string;
}

interface IProps {
  title: string;
  hint?: React.ReactElement;
  content?: React.ReactElement;
  formData: {
    selectedList: CLOUD.TagItem[];
  };
  fieldList: any[];
  onClose: () => void;
  onSubmit: (formDate: IForm) => void;
  visible: boolean;
}

export const EcsCloudOperationForm = ({ title, formData, fieldList, content, visible, onClose, onSubmit }: IProps) => {
  const onOk = (body: any) => {
    const currentBody = { ...body };
    const form: IForm = {
      instanceIds: [],
      region: 'cn-hangzhou',
      vendor: 'alicloud',
    };
    formData.selectedList.forEach((item) => form.instanceIds.push(item.resourceID));
    if (body.duration) {
      currentBody.duration = +body.duration;
    }
    onSubmit({ ...form, ...currentBody });
  };
  return (
    <>
      <FormModal title={title} visible={visible} fieldsList={fieldList} onCancel={onClose} onOk={onOk}>
        {content}
      </FormModal>
    </>
  );
};
