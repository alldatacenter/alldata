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
import { MarkdownEditor, Title, FormBuilder, IFormExtendType } from 'common';
import { useUpdate } from 'common/use-hooks';
import { Input } from 'antd';
import i18n from 'i18n';
import apiDesignStore from 'apiManagePlatform/stores/api-design';
import { INPUT_MAX_LENGTH, TEXTAREA_MAX_LENGTH } from 'app/modules/apiManagePlatform/configs.ts';
import './index.scss';
import { produce } from 'immer';

const { Fields } = FormBuilder;

interface IApiInfo {
  title?: string;
  version?: string;
  description?: string;
}

const ApiSummary = () => {
  const [{ isErrorName, isErrorVersion }, updater] = useUpdate({
    isErrorName: false,
    isErrorVersion: false,
  });

  const formRef = React.useRef<IFormExtendType>(null as any);
  const [openApiDoc, apiLockState] = apiDesignStore.useStore((s) => [s.openApiDoc, s.apiLockState]);

  const { updateOpenApiDoc, updateFormErrorNum } = apiDesignStore;

  React.useEffect(() => {
    const info = openApiDoc.info as IApiInfo;

    setTimeout(() => {
      formRef.current?.setFieldsValue(info);
    });
  }, [openApiDoc]);

  const setField = (propertyName: string, val: any) => {
    const info = formRef.current?.getFieldsValue();
    info[propertyName] = val;
    const tempDetail = produce(openApiDoc, (draft) => {
      draft.info = info;
    });

    updateOpenApiDoc(tempDetail);
  };

  const basicFields = [
    {
      type: Input,
      label: i18n.t('name'),
      name: 'title',
      colSpan: 12,
      customProps: {
        maxLength: INPUT_MAX_LENGTH,
        disabled: apiLockState,
        onChange: (e: React.ChangeEvent<HTMLInputElement>) => {
          const newName = e.target.value;
          if (newName) {
            setField('title', newName);
            updater.isErrorName(false);
          } else {
            updater.isErrorName(true);
          }
        },
      },
    },
    {
      type: Input,
      label: i18n.t('version'),
      name: 'version',
      colSpan: 12,
      customProps: {
        disabled: apiLockState,
        maxLength: INPUT_MAX_LENGTH,
        onChange: (e: React.ChangeEvent<HTMLInputElement>) => {
          const newVersion = e.target.value;
          if (newVersion) {
            setField('version', newVersion);
            updater.isErrorVersion(false);
          } else {
            updater.isErrorVersion(true);
          }
        },
      },
    },
    {
      type: MarkdownEditor,
      label: i18n.t('description'),
      name: 'description',
      required: false,
      colSpan: 24,
      customProps: {
        readOnly: apiLockState,
        defaultMode: apiLockState ? 'html' : 'md',
        maxLength: TEXTAREA_MAX_LENGTH,
        onChange: (val: string) => setField('description', val),
      },
    },
  ];

  React.useEffect(() => {
    if (isErrorName || isErrorVersion) {
      updateFormErrorNum(1);
    } else {
      updateFormErrorNum(0);
    }
  }, [isErrorName, isErrorVersion, updateFormErrorNum]);

  return (
    <div className="api-summary">
      <FormBuilder isMultiColumn ref={formRef}>
        <Title level={1} title={i18n.t('dop:API overview')} />
        <Fields fields={basicFields} fid="basicFields" />
      </FormBuilder>
    </div>
  );
};

export default ApiSummary;
