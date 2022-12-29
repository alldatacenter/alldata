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

import { CalculationType } from 'globalConstants';
import { precisionCalculation } from '../number';

describe('number Test', () => {
  describe.each([
    [CalculationType.ADD, [null, 1, '1', undefined], 2],
    [CalculationType.SUBTRACT, [null, 1, '1', undefined], 0],
  ])('precisionCalculation Test - ', (type, numberList, expected) => {
    test(`The precision calculation method test, type ${type} number list ${numberList} expected ${expected}`, () => {
      expect(precisionCalculation(type, numberList as any)).toEqual(expected);
    });
  });
});
