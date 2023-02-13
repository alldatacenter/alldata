/**
 * Datart
 *
 * Copyright 2021
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { FormItemProps } from 'antd';
import { ControllerFacadeTypes } from 'app/constants';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import {
  ALL_SQL_OPERATOR_OPTIONS,
  SQL_OPERATOR_OPTIONS_TYPES,
} from 'app/pages/DashBoardPage/constants';
import { FilterSqlOperator } from 'globalConstants';
import React, { memo } from 'react';
import { FixedSqlOperatorTypes } from '../../../constants';
import { SqlOperatorForm } from './SqlOperatorForm';

export const SqlOperatorName = ['config', 'sqlOperator'];

export interface SqlOperatorProps {
  controllerType: ControllerFacadeTypes;
}
export const SqlOperator: React.FC<SqlOperatorProps> = memo(
  ({ controllerType }) => {
    const tc = useI18NPrefix(`viz.control`);
    const hideForm = FixedSqlOperatorTypes.includes(controllerType);
    const itemProps: FormItemProps<any> = {
      preserve: true,
      name: SqlOperatorName,
      label: tc('sqlOperator'),
      hidden: hideForm,
    };
    const optionKeys: FilterSqlOperator[] =
      SQL_OPERATOR_OPTIONS_TYPES[controllerType] || [];
    const options = ALL_SQL_OPERATOR_OPTIONS.filter(it =>
      optionKeys.includes(it.value),
    );

    return <SqlOperatorForm options={options} {...itemProps} />;
  },
);
