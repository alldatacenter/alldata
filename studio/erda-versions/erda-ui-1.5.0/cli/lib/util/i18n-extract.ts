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

import vfs from 'vinyl-fs';
import path from 'path';
import fs from 'fs';
import { merge, differenceWith, isEqual, unset, mapKeys } from 'lodash';
import { logError } from './log';
import {
  internalModules,
  internalLocalePathMap,
  internalSrcDirMap,
  externalModules,
  externalSrcDirMap,
  externalLocalePathMap,
  Obj,
} from './i18n-config';

interface Resource {
  [k: string]: { [k: string]: string };
}

const scanner = require('i18next-scanner');
const flattenObjectKeys = require('i18next-scanner/lib/flatten-object-keys').default;
const omitEmptyObject = require('i18next-scanner/lib/omit-empty-object').default;

let zhWordMap = {};
let localePath: null | string = null;
let ns: string[] = [];
let originalZhJson = {};
let originalEnJson = {};
let isExternal = false;

// See options at https://github.com/i18next/i18next-scanner#options
const options = () => ({
  removeUnusedKeys: true,
  sort: true,
  func: {
    list: ['i18n.t'],
    extensions: ['.js', '.jsx', '.ts', '.tsx'],
  },
  lngs: ['en', 'zh'],
  defaultLng: 'en',
  defaultNs: ns.includes('default') ? 'default' : ns[0] || 'default',
  defaultValue: '__NOT_TRANSLATED__',
  resource: {
    savePath: `${localePath}/{{lng}}.json`,
    jsonIndent: 2,
    lineEnding: '\n',
  },
  ns,
  nsSeparator: ':', // namespace separator
  keySeparator: false, // if working with a flat json, it's recommended to set keySeparator to false
  interpolation: {
    prefix: '{{',
    suffix: '}}',
  },
});

function sortObject(unordered: Resource | { [k: string]: string }) {
  const ordered: Resource | Obj = {};
  Object.keys(unordered)
    .sort()
    .forEach((key) => {
      if (typeof unordered[key] === 'object') {
        (ordered as Resource)[key] = sortObject(unordered[key] as Obj) as Obj;
      } else {
        ordered[key] = unordered[key];
      }
    });
  return ordered;
}

function customFlush(done: () => void) {
  const enToZhWords: Obj = zhWordMap;
  // @ts-ignore api
  const { resStore } = this.parser;
  // @ts-ignore api
  const { resource, removeUnusedKeys, sort, defaultValue } = this.parser.options;

  Object.keys(resStore).forEach((lng) => {
    const namespaces = resStore[lng];
    // The untranslated English value and key are consistent
    if (lng === 'en') {
      Object.keys(namespaces).forEach((_ns) => {
        const obj = namespaces[_ns];
        Object.keys(obj).forEach((k) => {
          if (obj[k] === defaultValue) {
            obj[k] = k.replace('&#58;', ':');
          }
        });
      });
    }

    const filePath = resource.savePath.replace('{{lng}}', lng);
    let oldContent = lng === 'zh' ? originalZhJson : originalEnJson;

    // Remove obsolete keys
    if (removeUnusedKeys) {
      const namespaceKeys = flattenObjectKeys(namespaces);
      const oldContentKeys = flattenObjectKeys(oldContent);
      const unusedKeys = differenceWith(oldContentKeys, namespaceKeys, isEqual);

      for (let i = 0; i < unusedKeys.length; ++i) {
        unset(oldContent, unusedKeys[i]);
      }

      oldContent = omitEmptyObject(oldContent);
    }

    // combine old content
    let output = merge(namespaces, oldContent);
    if (sort) {
      output = sortObject(output);
    }

    // substitution zh locale
    if (lng === 'zh') {
      Object.keys(output).forEach((_ns) => {
        const obj = output[_ns];
        Object.keys(obj).forEach((k) => {
          if (obj[k] === defaultValue) {
            const zh = enToZhWords[k] || enToZhWords[`${_ns}:${k}`];
            if (zh) {
              obj[k] = zh;
            } else {
              logError(`there is untranslated content in zh.json: ${k}, please handle it manually`);
            }
          }
        });
      });
    }

    if (isExternal) {
      fs.writeFile(filePath, `${JSON.stringify(output, null, resource.jsonIndent)}\n`, 'utf8', (writeErr) => {
        if (writeErr) return logError(`writing failed:${lng}`, writeErr);
      });
    } else {
      const { default: defaultContent, ...restContent } = output;
      fs.writeFile(filePath, `${JSON.stringify(restContent, null, resource.jsonIndent)}\n`, 'utf8', (writeErr) => {
        if (writeErr) return logError(`writing failed:${lng}`, writeErr);
      });
      const defaultLocalePath = `${internalLocalePathMap.default}/${lng}.json`;

      fs.writeFile(
        defaultLocalePath,
        `${JSON.stringify({ default: defaultContent }, null, resource.jsonIndent)}\n`,
        'utf8',
        (writeErr) => {
          if (writeErr) return logError(`writing failed:${lng}`, writeErr);
        },
      );
    }
  });

  done();
}

export default async (resolve: (value: void | PromiseLike<void>) => void, _isExternal = false) => {
  try {
    const jsonContent = fs.readFileSync(path.resolve(process.cwd(), './temp-zh-words.json'), 'utf8');
    const zhWordContent = JSON.parse(jsonContent);
    zhWordMap = mapKeys(zhWordContent, (_v, key) => {
      return key.replace(':', '&#58;');
    });
  } catch (error) {
    zhWordMap = {};
  }

  if (!_isExternal) {
    // handle internal modules
    for (const moduleName of internalModules) {
      const srcDirs = internalSrcDirMap[moduleName];
      const paths = srcDirs.map((srcDir) => `${srcDir}/**/*.{js,jsx,ts,tsx}`);
      paths.push('!**/node_modules/**');
      paths.push('!**/__tests__/**');

      localePath = internalLocalePathMap[moduleName];
      const targetLocalePath = internalLocalePathMap[moduleName];
      const zhJsonPath = `${targetLocalePath}/zh.json`;
      const enJsonPath = `${targetLocalePath}/en.json`;
      let content = fs.readFileSync(zhJsonPath, 'utf8');
      originalZhJson = JSON.parse(content);
      content = fs.readFileSync(enJsonPath, 'utf8');
      originalEnJson = JSON.parse(content);

      const defaultLocalePath = internalLocalePathMap.default;
      const defaultZhJsonPath = `${defaultLocalePath}/zh.json`;
      const defaultEnJsonPath = `${defaultLocalePath}/en.json`;
      content = fs.readFileSync(defaultZhJsonPath, 'utf8');
      originalZhJson = merge(originalZhJson, JSON.parse(content));
      content = fs.readFileSync(defaultEnJsonPath, 'utf8');
      originalEnJson = merge(originalEnJson, JSON.parse(content));

      ns = Object.keys(originalZhJson);

      const promise = new Promise<void>((_resolve) => {
        vfs
          .src(paths)
          .pipe(scanner(options(), undefined, customFlush))
          .pipe(vfs.dest('./'))
          .on('end', () => {
            _resolve();
          });
      });
      // eslint-disable-next-line no-await-in-loop
      await promise;
    }
  } else {
    isExternal = true;
    // handle external modules
    for (const moduleName of externalModules) {
      const srcDirs = externalSrcDirMap[moduleName];
      const paths = srcDirs.map((srcDir) => `${srcDir}/**/*.{js,jsx,ts,tsx}`);
      paths.push('!**/node_modules/**');
      paths.push('!**/__tests__/**');

      localePath = externalLocalePathMap[moduleName];
      const targetLocalePath = externalLocalePathMap[moduleName];
      const zhJsonPath = `${targetLocalePath}/zh.json`;
      const enJsonPath = `${targetLocalePath}/en.json`;
      let content = fs.readFileSync(zhJsonPath, 'utf8');
      originalZhJson = JSON.parse(content);
      content = fs.readFileSync(enJsonPath, 'utf8');
      originalEnJson = JSON.parse(content);

      ns = Object.keys(originalZhJson);

      const promise = new Promise<void>((_resolve) => {
        vfs
          .src(paths)
          .pipe(scanner(options(), undefined, customFlush))
          .pipe(vfs.dest('./'))
          .on('end', () => {
            _resolve();
          });
      });
      // eslint-disable-next-line no-await-in-loop
      await promise;
    }
  }

  resolve();
};
