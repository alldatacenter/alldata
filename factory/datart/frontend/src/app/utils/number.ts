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

import currency from 'currency.js';
import { CalculationType } from 'globalConstants';
import isFinite from 'lodash/isFinite';
import { isEmpty } from 'utils/object';

export function toPrecision(value: any, precision: number) {
  if (isNaN(+value)) {
    return value;
  }
  if (precision < 0 || precision > 100) {
    return value;
  }

  return (+value).toFixed(precision);
}

export function toSeperator(value: any, useThousandSeparator: boolean) {
  if (isNaN(+value) || !useThousandSeparator) {
    return value;
  }

  const parts = value.toString().split('.');
  parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ',');
  const formatted = parts.join('.');
  return formatted;
}

export function toUnit(value: any, unit?: number) {
  if (isEmpty(unit)) {
    return value;
  }

  if (isNaN(+value)) {
    return value;
  }

  return +value / unit!;
}

export function toUnitDesc(value: any, desc: string) {
  return `${value} ${desc}`;
}

export function toExponential(value: any, precision: number) {
  if (isNaN(+value)) {
    return value;
  }
  return (+value).toExponential(precision);
}

export function isNumber(value: any) {
  return !isEmpty(value) && !isNaN(value) && isFinite(value) && value !== '';
}

function getPrecision(num: string | number) {
  return typeof num === 'number'
    ? num.toString().split('.')?.[1]?.length || 0
    : num.split('.')?.[1]?.length || 0;
}

export function precisionCalculation(
  type: CalculationType,
  numberList: Array<string | number>,
): number {
  switch (type) {
    default:
      return 0;
    case CalculationType.ADD:
    case CalculationType.SUBTRACT:
      return numberList.reduce((acc, cur) => {
        const num = isNaN(Number(cur)) ? 0 : Number(cur);
        const precision = Math.max(getPrecision(acc), getPrecision(num));
        const result = Number(currency(num, { precision })[type](acc).value);
        return isNaN(result) ? 0 : result;
      }, 0) as number;
  }
}
