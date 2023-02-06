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

import {
  groupHandler,
  sortHandler,
  slowHandler,
  errorHttpHandler,
  errorDbHandler,
  multipleDataHandler,
  multipleGroupDataHandler,
} from 'common/utils/chart-utils';

const data = {
  time: '2021-04-14',
  results: [
    {
      data: [
        {
          org: {
            name: 'org',
            data: 12,
          },
          project: {
            name: 'project',
            data: 12,
          },
        },
      ],
    },
  ],
};

describe('chart-utils', () => {
  it('groupHandler working fine', () => {
    expect(groupHandler('project')(data)).toStrictEqual({
      results: [{ data: 12, name: 'project' }],
      time: '2021-04-14',
    });
    expect(groupHandler()(data)).toStrictEqual({ results: [{ data: 12, name: 'org' }], time: '2021-04-14' });
    expect(groupHandler()()).toStrictEqual({});
  });
  it('sortHandler work fine', () => {
    expect(sortHandler('project')(data, {})).toStrictEqual({ list: [{ name: 'project', unit: '', value: 12 }] });
    expect(sortHandler()([], { extendHandler: { dataKey: 'app' } })).toStrictEqual({ list: [] });
    expect(sortHandler()(data, { extendHandler: { dataKey: 'app' } })).toStrictEqual({
      list: [
        {
          unit: '',
          name: undefined,
          value: undefined,
        },
      ],
    });
  });
  it('slowHandler working fine', () => {
    expect(slowHandler()([])).toStrictEqual({});
    expect(slowHandler(['time:project'])(data)).toStrictEqual({ list: [{ name: 'project', time: 12 }] });
  });
  it('errorHttpHandler working fine', () => {
    const data1 = {
      results: [
        {
          data: {
            ooo: {
              tag: 12,
              data: [
                {
                  sum: {
                    elapsed_count: {
                      data: 100222,
                      tag: 'hello',
                    },
                  },
                  maxFieldTimestamp: {
                    elapsed_max: {
                      data: 'www',
                    },
                  },
                  last: {
                    tags: {
                      source_application_id: {
                        data: 'a[[',
                      },
                    },
                  },
                },
              ],
            },
          },
        },
      ],
    };
    expect(errorHttpHandler()([])).toStrictEqual({});
    expect(errorHttpHandler()(data1)).toStrictEqual({
      list: [
        {
          applicationId: 'a[[',
          count: 100222,
          httpCode: 'hello',
          name: 12,
          time: 'www',
        },
      ],
    });
  });
  it('errorDbHandler working fine', () => {
    const data2 = {
      results: [
        {
          data: [
            {
              sum: {
                elapsed_count: {
                  data: 100222,
                  tag: 'hello',
                },
              },
              maxFieldTimestamp: {
                elapsed_max: {
                  data: 'www',
                },
              },
            },
          ],
        },
      ],
    };
    expect(errorDbHandler()()).toStrictEqual({});
    expect(errorDbHandler()(data2)).toStrictEqual({
      list: [
        {
          count: 100222,
          name: 'hello',
          time: 'www',
        },
      ],
    });
  });
  it('multipleDataHandler working fine', () => {
    const data3 = {
      time: '2021-04-14',
      results: [
        {
          data: [
            {
              project: 12,
              app: 1,
            },
          ],
        },
      ],
    };
    expect(multipleDataHandler()(data3)).toStrictEqual({ results: [], time: '2021-04-14' });
    expect(multipleDataHandler()()).toStrictEqual({});
    expect(multipleDataHandler(['project', 'app'])(data3)).toStrictEqual({ results: [12, 1], time: '2021-04-14' });
  });
  it('multipleGroupDataHandler working fine', () => {
    const data4 = {
      time: '2021-04-14',
      results: [
        {
          data: [
            {
              project: { name: 'project', tag: 1 },
              app: { name: 'app', tag: 1 },
            },
          ],
        },
      ],
    };
    expect(multipleGroupDataHandler()()).toStrictEqual({});
    expect(multipleGroupDataHandler()(data4)).toStrictEqual({ results: [], time: '2021-04-14' });
    expect(multipleGroupDataHandler(['project', 'app'])(data4)).toStrictEqual({
      results: [
        {
          group: 1,
          name: 'project',
          tag: 'project',
        },
        {
          group: 1,
          name: 'app',
          tag: 'app',
        },
      ],
      time: '2021-04-14',
    });
  });
});
