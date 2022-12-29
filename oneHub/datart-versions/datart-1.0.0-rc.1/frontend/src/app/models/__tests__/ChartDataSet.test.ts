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

import { ChartDataSectionType, SortActionType } from 'app/constants';
import { ChartDataSet, ChartDataSetRow } from '../ChartDataSet';

describe('ChartDataSet Tests', () => {
  test('should get ChartDataSet Model', () => {
    const columns = [
      [1, 2],
      [3, 4],
    ];
    const metas = [{ name: 'column1' }, { name: 'column2' }];
    const fields = [{ colName: 'column1' }, { colName: 'column2' }] as any[];

    const dataset = new ChartDataSet(columns, metas, fields);

    expect(dataset.length).toEqual(2);
    expect(dataset[0][0]).toEqual(1);
    expect(dataset[0][1]).toEqual(2);
    expect(dataset[1][0]).toEqual(3);
    expect(dataset[1][1]).toEqual(4);

    expect(dataset[0]).toBeInstanceOf(ChartDataSetRow);
  });

  test('should get ChartDataSet Model when columns only one', () => {
    const columns = [[1, 2]];
    const metas = [{ name: 'column1' }, { name: 'column2' }];
    const fields = [{ colName: 'column1' }, { colName: 'column2' }] as any[];

    const dataset = new ChartDataSet(columns, metas, fields);

    expect(dataset.length).toEqual(1);
    expect(dataset[0][0]).toEqual(1);
    expect(dataset[0][1]).toEqual(2);

    expect(dataset[0]).toBeInstanceOf(ChartDataSetRow);
  });

  test('should get value from dataset methods', () => {
    const columns = [
      [1, 2],
      [3, 4],
    ];
    const metas = [{ name: 'column1' }, { name: 'column2' }];
    const fields = [{ colName: 'column1' }, { colName: 'column2' }] as any[];

    const dataset = new ChartDataSet(columns, metas, fields);

    expect(dataset.getFieldKey({ colName: 'columnName' } as any)).toEqual(
      'COLUMNNAME',
    );
    expect(
      dataset.getFieldKey({ colName: 'columnName', aggregate: 'AVG' } as any),
    ).toEqual('AVG(COLUMNNAME)');
    expect(dataset.getFieldIndex({ colName: 'column1' } as any)).toEqual(0);
    expect(
      dataset.getFieldIndex({ colName: 'column1', aggregate: 'AVG' } as any),
    ).toEqual(undefined);
  });

  test('should sort dataset rows by one config custom sort values', () => {
    const columns = [
      [1, 2],
      [3, 4],
    ];
    const metas = [{ name: 'column1' }, { name: 'column2' }];
    const fields = [{ colName: 'column1' }, { colName: 'column2' }] as any[];

    const dataset = new ChartDataSet(columns, metas, fields);
    dataset.sortBy([
      {
        type: ChartDataSectionType.Aggregate,
        rows: [
          {
            colName: 'column1',
            sort: { type: SortActionType.Customize, value: [4, 3, 2, 1] },
          },
        ],
      },
    ] as any);

    /* order by column1 desc
     * [3] [4]
     * [1] [2]
     */
    expect(dataset.length).toEqual(2);
    expect(dataset[0][0]).toEqual(3);
    expect(dataset[0][1]).toEqual(4);
    expect(dataset[1][0]).toEqual(1);
    expect(dataset[1][1]).toEqual(2);

    expect(dataset[0]).toBeInstanceOf(ChartDataSetRow);
  });

  test('should not sort dataset rows when no valid configs', () => {
    const columns = [
      [1, 2],
      [3, 4],
    ];
    const metas = [{ name: 'column1' }, { name: 'column2' }];
    const fields = [{ colName: 'column1' }, { colName: 'column2' }] as any[];

    const dataset = new ChartDataSet(columns, metas, fields);
    dataset.sortBy([
      {
        type: ChartDataSectionType.Mixed,
        rows: [
          {
            colName: 'column1',
            sort: { type: SortActionType.Customize, value: [4, 3, 2, 1] },
          },
        ],
      },
    ] as any);

    expect(dataset[0][0]).toEqual(1);
    expect(dataset[0][1]).toEqual(2);
    expect(dataset[1][0]).toEqual(3);
    expect(dataset[1][1]).toEqual(4);
  });

  test('should not sort dataset rows when order type is not custom', () => {
    const columns = [
      [1, 2],
      [3, 4],
    ];
    const metas = [{ name: 'column1' }, { name: 'column2' }];
    const fields = [{ colName: 'column1' }, { colName: 'column2' }] as any[];

    const dataset = new ChartDataSet(columns, metas, fields);
    dataset.sortBy([
      {
        type: ChartDataSectionType.Mixed,
        rows: [
          {
            colName: 'column1',
            sort: { type: SortActionType.ASC },
          },
        ],
      },
    ] as any);

    expect(dataset[0][0]).toEqual(1);
    expect(dataset[0][1]).toEqual(2);
    expect(dataset[1][0]).toEqual(3);
    expect(dataset[1][1]).toEqual(4);
  });

  test('should group by dataset', () => {
    const columns = [
      [1, 100],
      [1, 101],
      [2, 201],
      [3, 301],
      [3, 302],
    ];
    const metas = [{ name: 'column1' }, { name: 'column2' }];
    const fields = [{ colName: 'column1' }, { colName: 'column2' }] as any[];

    const dataset = new ChartDataSet(columns, metas, fields);
    const groupedDataset = dataset.groupBy({
      colName: 'column1',
    } as any);

    expect(Object.keys(groupedDataset).length).toEqual(3);
    expect(groupedDataset['1'][0]).toEqual(
      new ChartDataSet(
        [
          [1, 100],
          [1, 101],
        ],
        metas,
        fields,
      )[0],
    );
    expect(groupedDataset['1'][1]).toEqual(
      new ChartDataSet(
        [
          [1, 100],
          [1, 101],
        ],
        metas,
        fields,
      )[1],
    );
  });
});
