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
import { ConfigLayout, ReadonlyForm } from 'common';
import { useUpdate } from 'common/use-hooks';
import PublisherFormModal, { getPublisherFieldsList } from './publisher-form-modal';
import { Button } from 'antd';
import i18n from 'i18n';

export const PublisherInfo = ({
  data,
  getDetail = () => {},
}: {
  data: PUBLISHER.IPublisher | undefined;
  getDetail: () => void;
}) => {
  const [{ formModalVis, editData }, updater] = useUpdate({
    formModalVis: false,
    editData: undefined as PUBLISHER.IPublisher | undefined,
  });

  const openFormModal = () => {
    updater.editData(data);
    updater.formModalVis(true);
  };

  const closeFormModal = () => {
    updater.editData(undefined);
    updater.formModalVis(false);
  };
  const afterSubmit = () => {
    closeFormModal();
    getDetail();
  };

  const sectionList: any[] = [
    {
      title: i18n.t('basic information'),
      titleOperate: (
        <Button type="primary" ghost onClick={openFormModal}>
          {i18n.t('edit')}
        </Button>
      ),
      children: <ReadonlyForm fieldsList={getPublisherFieldsList()} data={data} />,
    },
  ];

  return (
    <>
      <ConfigLayout sectionList={sectionList} />
      <PublisherFormModal
        visible={formModalVis}
        onCancel={closeFormModal}
        formData={editData}
        afterSubmit={afterSubmit}
      />
    </>
  );
};
