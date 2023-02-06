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

import { FormModal, ImageUpload } from 'common';
import i18n from 'i18n';
import React from 'react';
import { isEmpty, get } from 'lodash';
import { FormInstance } from 'core/common/interface';
import publisherStore from 'app/modules/publisher/stores/publisher';

interface IProps {
  visible: boolean;
  formData?: PUBLISHER.IPublisher;
  onCancel: () => void;
  afterSubmit?: (isUpdate?: boolean, data?: PUBLISHER.IPublisher) => any;
}

export const getPublisherFieldsList = (isEdit?: boolean) => {
  const fieldsList = [
    {
      label: i18n.t('publisher:repository name'),
      name: 'name',
      itemProps: {
        disabled: isEdit,
        maxLength: 200,
      },
    },
    {
      label: i18n.t('icon'),
      name: 'logo',
      viewType: 'image',
      required: false,
      getComp: ({ form }: { form: FormInstance }) => <ImageUpload id="logo" form={form} showHint />,
    },
    {
      label: i18n.t('description'),
      name: 'desc',
      required: false,
      type: 'textArea',
      itemProps: {
        maxLength: 1000,
      },
    },
  ];
  return fieldsList;
};

const PublisherFormModal = ({ visible, formData, onCancel, afterSubmit = () => {} }: IProps) => {
  const { addPublisherList, updatePublisher } = publisherStore.effects;
  const handelSubmit = (data: PUBLISHER.IPublisher) => {
    onCancel();
    if (isEmpty(formData)) {
      addPublisherList(data).then((res) => {
        res && afterSubmit(false, data);
      });
    } else {
      updatePublisher({ id: get(formData, 'id'), ...data }).then((res) => {
        res && afterSubmit(true, { ...formData, ...data });
      });
    }
  };
  const isEdit = !isEmpty(formData);

  return (
    <FormModal
      width={620}
      name={i18n.t('cmp:publisher info')}
      fieldsList={getPublisherFieldsList(isEdit)}
      visible={visible}
      formData={formData}
      onOk={handelSubmit}
      onCancel={onCancel}
    />
  );
};

export default PublisherFormModal;
