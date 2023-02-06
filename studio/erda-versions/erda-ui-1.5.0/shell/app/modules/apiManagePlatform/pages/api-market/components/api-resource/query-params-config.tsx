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
import { EmptyHolder } from 'common';
import { useUpdate } from 'common/use-hooks';
import { produce } from 'immer';
import { map, filter, set, values, isEmpty, findIndex, forEach } from 'lodash';
import { PropertyItemForm } from 'apiManagePlatform/pages/api-market/design/basic-params-config';
import { API_FORM_KEY, API_PROPERTY_REQUIRED, API_RESOURCE_TAB } from 'app/modules/apiManagePlatform/configs.ts';
import apiDesignStore from 'apiManagePlatform/stores/api-design';

interface IExtraProps {
  quoteTypeName: string;
  typeQuotePath: Array<string | number>;
}
interface IProps {
  paramIn: 'query' | 'header';
  formData: Obj;
  isEditMode?: boolean;
  resourceKey: API_RESOURCE_TAB;
  onChange: (id: string, data: Obj, extraProps: IExtraProps) => void;
}

export const QueryParamsConfig = (props: IProps) => {
  const [{ paramsData }, updater] = useUpdate({
    paramsData: {},
  });

  const { onChange, formData, paramIn, isEditMode, resourceKey } = props;

  const { updateFormErrorNum } = apiDesignStore;

  React.useEffect(() => {
    const _paramList = formData.parameters || {};
    const properties = {};
    forEach(_paramList, (item) => {
      if (item?.in === paramIn) {
        properties[item[API_FORM_KEY] || item.name] = item?.schema || item;
      }
    });
    const tempObj = {
      type: 'object',
      properties,
    };
    tempObj[API_FORM_KEY] = 'parameters';
    updater.paramsData(tempObj);
  }, [updater, formData, paramIn, resourceKey]);

  const updateParamList = React.useCallback(
    (formKey: string, _formData: any, extraProps?: Obj) => {
      const { typeQuotePath, quoteTypeName } = extraProps || {};

      const _extraProps = { quoteTypeName, typeQuotePath: [] } as IExtraProps;
      if (formKey) {
        const tempDetail = produce(formData, (draft) => {
          const newParameters = map(values(_formData?.properties), (item) => {
            const tempItem = {
              in: paramIn,
              name: item[API_FORM_KEY],
              required: item[API_PROPERTY_REQUIRED],
              schema: item,
            };
            tempItem[API_FORM_KEY] = item[API_FORM_KEY];
            return tempItem;
          });
          const parametersWithoutChange = filter(formData?.parameters, (item) => item?.in !== paramIn) || [];
          const allParameters = [...newParameters, ...parametersWithoutChange];

          if (typeQuotePath && quoteTypeName) {
            const targetParamName = typeQuotePath.split('.')[2];
            const targetIndex = findIndex(allParameters, { [API_FORM_KEY]: targetParamName });
            const newPath = ['parameters', targetIndex];
            _extraProps.typeQuotePath = newPath;
          }

          set(draft, 'parameters', allParameters);
        });
        onChange(paramIn, tempDetail, _extraProps);
      }
    },
    [formData, onChange, paramIn],
  );

  return (
    <div className="basic-params-config">
      {!isEmpty(paramsData) && (
        <PropertyItemForm
          onFormErrorNumChange={updateFormErrorNum}
          formData={paramsData}
          detailVisible
          formType="Parameters"
          onChange={updateParamList}
          isEditMode={isEditMode}
        />
      )}
      {!isEditMode && isEmpty(paramsData?.properties) && <EmptyHolder relative />}
    </div>
  );
};
