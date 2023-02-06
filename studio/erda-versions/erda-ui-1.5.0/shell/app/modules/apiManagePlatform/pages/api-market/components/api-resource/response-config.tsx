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
import { Title, FormBuilder, IFormExtendType } from 'common';
import { useUpdate } from 'common/use-hooks';
import { produce } from 'immer';
import { keys, set, isEmpty, get } from 'lodash';
import { PropertyItemForm } from 'apiManagePlatform/pages/api-market/design/basic-params-config';
import apiDesignStore from 'apiManagePlatform/stores/api-design';
import { API_FORM_KEY, API_MEDIA, API_RESOURCE_TAB } from 'app/modules/apiManagePlatform/configs.ts';

const DATA_PATH_MAP = {
  responses: ['responses', '200', 'content'],
  requestBody: ['requestBody', 'content'],
};

interface IProps {
  paramIn: 'responses' | 'requestBody';
  formData: Obj;
  dataPath: string[];
  resourceKey: API_RESOURCE_TAB;
  isEditMode?: boolean;
  onChange: (id: string, data: Obj, extraProps?: Obj) => void;
}

interface IFieldProps {
  propertyKey: string;
  propertyData: string;
}

export const ResponseConfig = React.memo((props: IProps) => {
  const [{ propertyFormData, mediaType }, updater] = useUpdate({
    propertyFormData: {} as Obj,
    mediaType: 'application/json',
  });

  const { onChange, formData, paramIn, dataPath, isEditMode = true, resourceKey } = props;

  const dataPathKey = React.useMemo(() => DATA_PATH_MAP[paramIn], [paramIn]);

  const [openApiDoc] = apiDesignStore.useStore((s) => [s.openApiDoc]);
  const { updateFormErrorNum } = apiDesignStore;

  const formRef = React.useRef<IFormExtendType>(null);

  React.useEffect(() => {
    const responseContent = get(formData, dataPathKey) || {};
    const _mediaType = keys(responseContent)[0] || 'application/json';

    const detailFormData = get(responseContent, [_mediaType, 'schema']) || {};
    const tempData = {
      properties: {},
      ...detailFormData,
    };
    tempData[API_FORM_KEY] = 'schema';
    tempData[API_MEDIA] = _mediaType;
    updater.propertyFormData(tempData);
    updater.mediaType(_mediaType);
  }, [updater, paramIn, formData, dataPathKey, resourceKey]);

  const extraDataTypes = React.useMemo(() => {
    return get(openApiDoc, 'components.schemas') || {};
  }, [openApiDoc]);

  const onFormChange = React.useCallback(
    (_formKey: string, _formData: any, extraProps?: Obj) => {
      const [name, method] = dataPath;
      const prefixPath = [...dataPathKey, mediaType];

      const _typeQuotePath = extraProps?.typeQuotePath ? extraProps.typeQuotePath.split('.') : ['schema'];

      const typeQuotePath =
        extraProps?.typeQuotePath && extraProps.typeQuotePath === extraProps?.quoteTypeName
          ? [...prefixPath, 'schema']
          : [...prefixPath, ..._typeQuotePath];

      const _extraProps = { typeQuotePath, quoteTypeName: extraProps?.quoteTypeName };

      if (isEmpty(formData?.responses) && paramIn === 'responses') {
        const tempDetail = produce(openApiDoc, (draft) => {
          const methodData = get(draft, ['paths', ...dataPath]) || {};
          draft.paths[name][method] = {
            ...methodData,
            responses: {
              200: {
                content: {
                  [mediaType]: {
                    schema: { description: '', ..._formData } || { description: '' },
                  },
                },
                description: _formData?.description || '',
              },
            },
          };
        });
        onChange(paramIn, get(tempDetail, ['paths', ...dataPath]), _extraProps);
      } else if (isEmpty(formData?.requestBody) && paramIn === 'requestBody') {
        const tempDetail = produce(openApiDoc, (draft) => {
          const methodData = get(draft, ['paths', ...dataPath]) || {};
          draft.paths[name][method] = {
            ...methodData,
            requestBody: {
              content: {
                [mediaType]: {
                  schema: { description: '', ..._formData } || { description: '' },
                },
              },
              required: false,
              description: _formData?.description || '',
            },
          };
        });

        onChange(paramIn, get(tempDetail, ['paths', ...dataPath]), _extraProps);
      } else {
        const tempData = produce(formData, (draft) => {
          set(draft, [...dataPathKey, mediaType, 'schema'], _formData);
          if (paramIn === 'responses') {
            set(draft, 'responses.200.description', _formData?.description || '');
          } else {
            set(draft, 'requestBody.description', _formData?.description || '');
          }
        });
        onChange(paramIn, tempData, _extraProps);
      }

      // eslint-disable-next-line react-hooks/exhaustive-deps
    },
    [formData, updater, mediaType],
  );

  const onSetMediaType = ({ propertyKey, propertyData }: IFieldProps) => {
    if (propertyKey === API_MEDIA) {
      const tempFormData: Obj = get(openApiDoc, ['paths', ...dataPath, ...DATA_PATH_MAP[paramIn], mediaType]) || {};

      const _tempFormData = produce(tempFormData, (draft) => {
        if (draft?.schema) {
          draft.schema[API_MEDIA] = propertyData;
        } else {
          draft.schema = {};
          draft.schema[API_MEDIA] = propertyData;
        }
      });
      const tempContent = {
        [propertyData]: _tempFormData,
      };

      const _responseContent = produce(formData, (draft) => {
        if (paramIn === 'responses') {
          draft.responses = {
            200: {
              content: tempContent,
              description: _tempFormData?.schema?.description || '',
            },
          };
        } else {
          draft.requestBody = {
            content: tempContent,
            required: false,
            description: _tempFormData?.schema?.description || '',
          };
        }
      });
      updater.mediaType(propertyData);
      updater.propertyFormData(_tempFormData.schema);
      onChange(paramIn, _responseContent);
    }
  };

  return (
    <div className="basic-params-config">
      <Title level={1} title="Body" />
      <FormBuilder isMultiColumn className="param-config-form" ref={formRef}>
        <PropertyItemForm
          onChange={onFormChange}
          onFormErrorNumChange={updateFormErrorNum}
          onSetMediaType={onSetMediaType}
          detailVisible
          formType={paramIn === 'responses' ? 'Response' : 'Body'}
          formData={propertyFormData}
          extraDataTypes={extraDataTypes}
          isEditMode={isEditMode}
        />
      </FormBuilder>
    </div>
  );
});
