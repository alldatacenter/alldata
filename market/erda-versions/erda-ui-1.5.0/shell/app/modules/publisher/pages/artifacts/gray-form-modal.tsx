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
import publisherStore from '../../stores/publisher';
import { FormModal } from 'common';
import { useUpdate } from 'common/use-hooks';
import routeInfoStore from 'core/stores/route';
import { validators, insertWhen } from 'common/utils';
import { RadioChangeEvent } from 'core/common/interface';

interface IProps {
  visible: boolean;
  formData: PUBLISHER.IVersion;
  onCancel: () => void;
  onOk: () => void;
}

interface IQuery {
  id: number;
  versionStates: string;
  grayLevelPercent: number;
}

const GrayFormModal = (props: IProps) => {
  const { visible, onOk, onCancel, formData } = props;
  const { setGrayAndPublish } = publisherStore.effects;
  const { publisherItemId } = routeInfoStore.useStore((s) => s.params);

  const [{ selectedVersionType }, updater] = useUpdate({
    selectedVersionType: '',
  });

  React.useEffect(() => {
    updater.selectedVersionType(formData.versionStates || 'beta');
  }, [formData, updater]);

  const setGrayThenPublish = (query: IQuery) => {
    const { id, versionStates, grayLevelPercent } = query;

    setGrayAndPublish({
      versionStates,
      grayLevelPercent: grayLevelPercent ? +grayLevelPercent : undefined,
      action: 'publish',
      publishItemID: +publisherItemId,
      publishItemVersionID: +id,
    })
      .then(() => {
        onOk();
      })
      .finally(() => {
        onCancel();
      });
  };

  const fieldList = [
    {
      name: 'id',
      itemProps: {
        type: 'hidden',
      },
      required: false,
    },
    {
      label: i18n.t('publisher:version type'),
      name: 'versionStates',
      type: 'radioGroup',
      options: [
        { name: i18n.t('publisher:release version'), value: 'release' },
        { name: i18n.t('publisher:preview version'), value: 'beta' },
      ],
      itemProps: {
        onChange: (e: RadioChangeEvent) => {
          updater.selectedVersionType(e.target.value);
        },
      },
    },
    ...insertWhen(selectedVersionType === 'beta', [
      {
        label: `${i18n.t('publisher:set gray release')} (%)`,
        name: 'grayLevelPercent',
        type: 'inputNumber',
        required: false,
        rules: [
          {
            validator: validators.validateNumberRange({ min: 0, max: 100 }),
          },
        ],
        initialValue: (formData.versionStates === 'beta' && formData.grayLevelPercent) || 0,
        itemProps: {
          placeholder: i18n.t('please enter a number between {min} ~ {max}', { min: 0, max: 100 }),
          min: 0,
          max: 100,
        },
      },
    ]),
  ];

  return (
    <FormModal
      title={i18n.t('publisher:version publish configuration')}
      fieldsList={fieldList}
      formData={formData}
      visible={visible}
      onOk={setGrayThenPublish}
      onCancel={onCancel}
      modalProps={{
        destroyOnClose: true,
        maskClosable: false,
      }}
    />
  );
};

export default GrayFormModal;
