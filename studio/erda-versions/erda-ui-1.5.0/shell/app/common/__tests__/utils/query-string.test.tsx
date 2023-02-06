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

import { qs, mergeSearch, updateSearch, setSearch } from 'common/utils';
import { createBrowserHistory } from 'history';
import { setConfig, getConfig } from 'core/config';

describe('query-string', () => {
  beforeAll(() => {
    const browserHistory = createBrowserHistory();
    setConfig('history', browserHistory);
  });
  afterAll(() => {
    setConfig('history', undefined);
  });
  it('qs.parse', () => {
    expect(qs.parse(process.env.mock_search)).toEqual({ id: '1' });
    expect(qs.parse('ids[]=1&ids[]=2&ids[]=3', { arrayFormat: 'bracket' })).toEqual({ ids: ['1', '2', '3'] });
  });
  it('qs.parseUrl', () => {
    expect(qs.parseUrl(process.env.mock_href)).toEqual({
      query: { id: '1' },
      url: 'https://terminus-org.app.terminus.io/erda/dop/apps',
    });
  });
  it('qs.stringify', () => {
    expect(qs.stringify({ id: '1' })).toEqual('id=1');
    expect(qs.stringify({ ids: ['1', '2', '3'] }, { arrayFormat: 'bracket' })).toEqual('ids[]=1&ids[]=2&ids[]=3');
  });
  it('qs.extract', () => {
    expect(qs.extract(process.env.mock_href)).toEqual('id=1');
  });
  it('mergeSearch', () => {
    expect(mergeSearch({ orgName: 'erda' })).toEqual({ id: '1', orgName: 'erda' });
    expect(mergeSearch({ orgName: 'erda' }, true)).toBe('id=1&orgName=erda');
    expect(mergeSearch({ orgName: 'erda' }, true, true)).toBe('orgName=erda');
    expect(mergeSearch({ orgName: 'erda' }, false, true)).toEqual({ orgName: 'erda' });
  });
  it('updateSearch', () => {
    const history = getConfig('history');
    updateSearch({ orgName: 'erda' });
    expect(history.location.search).toContain('orgName=erda');
  });
  it('setSearch', () => {
    const history = getConfig('history');
    setSearch({ orgName: 'erda' });
    expect(history.location.search).toContain('orgName=erda');
  });
});
