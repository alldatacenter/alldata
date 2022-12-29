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

import { ChartStyleSectionRow } from 'app/types/ChartConfig';
import { pickValues } from 'utils/object';
import { FormGeneratorLayoutProps, ItemLayoutProps } from './types';

const getSafeStyleProps = (
  o: Partial<ChartStyleSectionRow>,
  permitProps: string[],
) => {
  return pickValues(o, permitProps);
};

export const invokeDependencyWatcher = (
  action,
  key,
  value,
  permitProps: string[],
) => {
  if (!action) {
    return {};
  }
  const item: Partial<ChartStyleSectionRow> = action({
    [key]: value,
  });
  return getSafeStyleProps(item, permitProps);
};

export function itemLayoutComparer<T>(
  prevProps: ItemLayoutProps<T>,
  nextProps: ItemLayoutProps<T>,
) {
  if (
    prevProps.data !== nextProps.data ||
    prevProps.translate !== nextProps.translate ||
    prevProps.dataConfigs !== nextProps.dataConfigs ||
    prevProps.context !== nextProps.context
  ) {
    return false;
  }
  return true;
}

export function groupLayoutComparer<T>(
  prevProps: FormGeneratorLayoutProps<T>,
  nextProps: FormGeneratorLayoutProps<T>,
) {
  if (
    prevProps.mode !== nextProps.mode ||
    prevProps.dependency !== nextProps.dependency
  ) {
    return false;
  }
  return itemLayoutComparer(prevProps, nextProps);
}
