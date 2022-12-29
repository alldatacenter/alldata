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
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import React, { memo } from 'react';
import { MaxValueName, MinValueName } from '../..';
import { NumberSetForm } from '../NumberSetter/NumberSetFrom';

export interface NumberSetterProps {
  label: string;
}
export const MaxValueSetter: React.FC<NumberSetterProps> = memo(({ label }) => {
  const itemProps: FormItemProps<any> = {
    preserve: true,
    name: MaxValueName,
    label: label,
    required: true,
  };
  return <NumberSetForm {...itemProps} />;
});
export const MinValueSetter: React.FC<NumberSetterProps> = memo(({ label }) => {
  const itemProps: FormItemProps<any> = {
    preserve: true,
    name: MinValueName,
    label: label,
    required: true,
  };
  return <NumberSetForm {...itemProps} />;
});

export const MaxAndMinSetter: React.FC<{}> = memo(() => {
  const filterT = useI18NPrefix('viz.common.filter');
  return (
    <>
      <MaxValueSetter label={filterT('max')} />
      <MinValueSetter label={filterT('min')} />
    </>
  );
});
