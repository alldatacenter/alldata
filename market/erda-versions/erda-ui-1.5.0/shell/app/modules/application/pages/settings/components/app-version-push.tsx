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
import { SectionInfoEdit } from 'project/common/components/section-info-edit';
import applicationStore from 'application/stores/application';
import { useEffectOnce } from 'react-use';
import { FormInstance } from 'core/common/interface';
import { getPublisherList, getArtifactsList } from 'publisher/services/publisher';
import { LoadMoreSelector } from 'common';
import { useUpdate } from 'common/use-hooks';
import orgStore from 'app/org-home/stores/org';
import i18n from 'i18n';

const AppVersionPush = () => {
  const { getVersionPushConfig, updateVersionPushConfig } = applicationStore.effects;
  const { clearVersionPushConfig } = applicationStore.reducers;
  const versionPushConfig = applicationStore.useStore((s) => s.versionPushConfig);
  const orgId = orgStore.getState((s) => s.currentOrg.id);
  const [chosenPublisehId, setChosenPublisehId] = React.useState('');
  useEffectOnce(() => {
    getVersionPushConfig();
    return () => {
      clearVersionPushConfig();
    };
  });

  const save = (form: FormInstance) => {
    return updateVersionPushConfig({
      DEV: +form.DEV,
      TEST: +form.TEST,
      STAGING: +form.STAGING,
      PROD: +form.PROD,
    });
  };

  const [state, updater, update] = useUpdate({
    publisherId: '',
    publisherName: '',
    DEV: '',
    TEST: '',
    STAGING: '',
    PROD: '',
    DEV_name: '',
    TEST_name: '',
    STAGING_name: '',
    PROD_name: '',
  });

  const getPublishers = (q: any) => {
    return getPublisherList({ ...q, orgId }).then((res: any) => res.data);
  };
  const getArtifacts = (q: any) => {
    if (!chosenPublisehId) return;
    return getArtifactsList({ ...q, publisherId: chosenPublisehId }).then((res: any) => res.data);
  };

  const chosenItemConvert =
    ({ value, name }: { value: string; name: string }) =>
    (val: { label: string; value: string }) => {
      return val && !val.label && `${val.value}` === `${value}` ? { ...val, label: name } : val;
    };

  React.useEffect(() => {
    if (versionPushConfig) {
      const { DEV, TEST, STAGING, PROD } = versionPushConfig;
      let newData = {};
      if (DEV) {
        newData = {
          ...newData,
          publisherId: String(DEV.publisherId),
          publisherName: DEV.publisherName,
          DEV: String(DEV.publishItemId),
          DEV_name: DEV.publishItemName,
        };
        setChosenPublisehId(String(DEV.publisherId));
      }
      if (TEST) {
        newData = {
          ...newData,
          TEST: String(TEST.publishItemId),
          TEST_name: TEST.publishItemName,
        };
      }
      if (STAGING) {
        newData = {
          ...newData,
          STAGING: String(STAGING.publishItemId),
          STAGING_name: STAGING.publishItemName,
        };
      }
      if (PROD) {
        newData = {
          ...newData,
          PROD: String(PROD.publishItemId),
          PROD_name: PROD.publishItemName,
        };
      }
      update({ ...newData });
    }
  }, [update, updater, versionPushConfig]);

  const getFieldsList = (form: FormInstance) => [
    {
      label: i18n.t('publisher:publisher'),
      name: 'publisherId',
      hideWhenReadonly: true,
      itemProps: {
        onChange: (val: string) => {
          setChosenPublisehId(val);
          form.setFieldsValue({
            DEV: '',
            TEST: '',
            STAGING: '',
            PROD: '',
          });
        },
      },
      getComp: () => {
        return (
          <LoadMoreSelector
            getData={getPublishers}
            chosenItemConvert={chosenItemConvert({ value: state.publisherId, name: state.publisherName })}
          />
        );
      },
    },
    {
      label: i18n.t('publisher:DEV associated content'),
      name: 'DEV',
      type: 'custom',
      customRender: () => state.DEV_name,
      getComp() {
        return (
          <LoadMoreSelector
            getData={getArtifacts}
            extraQuery={{ publisherId: chosenPublisehId }}
            chosenItemConvert={chosenItemConvert({ value: state.DEV, name: state.DEV_name })}
          />
        );
      },
    },
    {
      label: i18n.t('publisher:TEST associated content'),
      name: 'TEST',
      type: 'custom',
      customRender: () => state.TEST_name,
      getComp() {
        return (
          <LoadMoreSelector
            getData={getArtifacts}
            extraQuery={{ publisherId: chosenPublisehId }}
            chosenItemConvert={chosenItemConvert({ value: state.TEST, name: state.TEST_name })}
          />
        );
      },
    },
    {
      label: i18n.t('publisher:STAGING associated content'),
      name: 'STAGING',
      type: 'custom',
      customRender: () => state.STAGING_name,
      getComp() {
        return (
          <LoadMoreSelector
            getData={getArtifacts}
            extraQuery={{ publisherId: chosenPublisehId }}
            chosenItemConvert={chosenItemConvert({ value: state.STAGING, name: state.STAGING_name })}
          />
        );
      },
    },
    {
      label: i18n.t('publisher:PROD associated content'),
      name: 'PROD',
      type: 'custom',
      customRender: () => state.PROD_name,
      getComp() {
        return (
          <LoadMoreSelector
            getData={getArtifacts}
            extraQuery={{ publisherId: chosenPublisehId }}
            chosenItemConvert={chosenItemConvert({ value: state.PROD, name: state.PROD_name })}
          />
        );
      },
    },
  ];
  return (
    <SectionInfoEdit
      hasAuth
      data={state}
      formName={i18n.t('dop:version push')}
      fieldsList={getFieldsList}
      updateInfo={save}
    />
  );
};

export default AppVersionPush;
