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

/* eslint-disable import/no-extraneous-dependencies */
import { GlobalWithFetchMock } from 'jest-fetch-mock';
import { TextDecoder, TextEncoder } from 'util';
import replaceAllInserter from 'string.prototype.replaceall';

replaceAllInserter.shim();

jest.mock('i18n', () => {
  return {
    t: (str, data) => {
      let token;
      let strFormat = str.replace(/\S+\:/, '').trim();
      if (!data) {
        return strFormat;
      }
      const reg = /\{(\S+?)}/;
      while ((token = reg.exec(strFormat))) {
        strFormat = strFormat.replace(token[0], data[token[1]]);
      }
      return strFormat;
    },
    getCurrentLocale: () => ({
      moment: 'en',
    }),
  };
});

const mock = (data) => {
  const temp = {};
  Object.keys(data).forEach((key) => {
    temp[`mock_${key}`] = data[key];
  });
  return temp;
};
const mockLocation = {
  href: 'https://terminus-org.app.terminus.io/erda/dop/apps?id=1#123',
  host: 'terminus-org.app.terminus.io',
  hostname: 'terminus-org.app.terminus.io',
  origin: 'https://terminus-org.app.terminus.io',
  pathname: '/erda/dop/apps',
  hash: '#123',
  search: '?id=1',
};
process.env = Object.assign(process.env, {
  mainVersion: 'mainVersion',
  ...mock(mockLocation),
});
Object.defineProperty(window.document, 'cookie', {
  writable: true,
  value: 'OPENAPI-CSRF-TOKEN=OPENAPI-CSRF-TOKEN;ID=123;ORG=erda',
});
Object.defineProperty(window, 'location', {
  value: mockLocation,
});
Object.defineProperty(navigator, 'userAgent', {
  value:
    'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.85 Safari/537.36',
});
document.body.innerHTML = '<script></script>';

const customGlobal: GlobalWithFetchMock = global as unknown as GlobalWithFetchMock;
customGlobal.TextDecoder = TextDecoder;
customGlobal.TextEncoder = TextEncoder;
