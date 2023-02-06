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

import { resolvePath, goTo } from 'common/utils';
import { setConfig } from 'core/config';

describe('go-to', () => {
  let open: any;
  beforeAll(() => {
    open = jest.spyOn(window, 'open').mockImplementation(() => {});
  });
  const paths = process.env.mock_pathname?.split('/') || [];
  it('resolvePath', () => {
    expect(resolvePath('aaa')).toBe(`${process.env.mock_pathname}/aaa`);
    expect(resolvePath('/aaa')).toBe('/aaa');
    expect(resolvePath('../aaa')).toBe(`${paths.slice(0, 3).join('/')}/aaa`);
  });
  it('goTo', () => {
    goTo(process.env.mock_href, { jumpOut: true });
    expect(open).toHaveBeenLastCalledWith(process.env.mock_href);
    goTo('https://www.erda.cloud/');
    expect(window.location.href).toBe('https://www.erda.cloud/');
    goTo(goTo.pages.app, {
      jumpOut: true,
      orgName: 'erda',
      projectId: 241,
      appId: 5574,
    });
    expect(open).toHaveBeenLastCalledWith('/erda/dop/projects/241/apps/5574');
    expect(goTo.resolve.projectApps({ orgName: 'erda', projectId: 241, appId: 5573 })).toBe(
      '/erda/dop/projects/241/apps',
    );
  });
});
