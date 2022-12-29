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

import { splitRangerDateFilters } from '../time';

describe('test splitRangerDateFilters', () => {
  const rangerDateFilter = {
    aggOperator: null,
    column: ['日期'],
    sqlOperator: 'BETWEEN',
    values: [
      {
        value: '2004-01-01 00:00:00',
        valueType: 'DATE',
      },
      {
        value: '2013-01-01 00:00:00',
        valueType: 'DATE',
      },
    ],
  };
  const badRangerDateFilter = {
    aggOperator: null,
    column: ['日期'],
    sqlOperator: 'BETWEEN',
    values: [
      {
        value: '2004-01-01 00:00:00',
        valueType: 'DATE',
      },
    ],
  };
  const otherFilter1 = {
    aggOperator: null,
    column: ['日期'],
    sqlOperator: 'LT',
    values: [
      {
        value: '2011-01-01 00:00:00',
        valueType: 'DATE',
      },
    ],
  };
  const splittedDateFilters = [
    {
      aggOperator: null,
      column: ['日期'],
      sqlOperator: 'GTE',
      values: [
        {
          value: '2004-01-01 00:00:00',
          valueType: 'DATE',
        },
      ],
    },
    {
      aggOperator: null,
      column: ['日期'],
      sqlOperator: 'LT',
      values: [
        {
          value: '2013-01-01 00:00:00',
          valueType: 'DATE',
        },
      ],
    },
  ];

  test('should splitRangerDateFilters to 2 filter', () => {
    const res1 = splitRangerDateFilters([rangerDateFilter, otherFilter1]);
    expect(res1).toEqual([...splittedDateFilters, otherFilter1]);
    const res2 = splitRangerDateFilters([rangerDateFilter]);
    expect(res2).toEqual([...splittedDateFilters]);
    const res3 = splitRangerDateFilters([otherFilter1]);
    expect(res3).toEqual([otherFilter1]);
  });

  test('should splitRangerDateFilters del bad rangeDateFilter', () => {
    const res4 = splitRangerDateFilters([badRangerDateFilter]);
    expect(res4).toEqual([]);
    const res5 = splitRangerDateFilters([badRangerDateFilter, otherFilter1]);
    expect(res5).toEqual([otherFilter1]);
  });

  test('should splitRangerDateFilters not arr to []', () => {
    const res1 = splitRangerDateFilters(null as any);
    expect(res1).toEqual([]);
    const res2 = splitRangerDateFilters(undefined as any);
    expect(res2).toEqual([]);
    const res4 = splitRangerDateFilters({} as any);
    expect(res4).toEqual([]);
    const res5 = splitRangerDateFilters('string' as any);
    expect(res5).toEqual([]);
  });
});
