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

/* eslint-disable no-console */
import findUnusedModule from './find-unused-module';
import path from 'path';

// eslint-disable-next-line @typescript-eslint/no-require-imports
const pkg = require(path.resolve(__dirname, '../package.json'));
const { devDependencies, dependencies } = pkg;
const nodeModules = Object.keys(dependencies)
  .concat(Object.keys(devDependencies))
  .concat(['path', 'prop-types', 'history']);

// eslint-disable-next-line @typescript-eslint/no-require-imports
const webpackConfig = require(path.resolve(__dirname, '../webpack.config.js'));
const aliasMap = webpackConfig().resolve.alias;
const aliasKeys = Object.keys(aliasMap);

const noneJs = ['zh-cn'];
const ignoreSuffix = ['.md', '.d.ts', 'mock.ts'];
const ignorePath = [
  '__tests__',
  'config-page',
  'app/static',
  'app/views',
  'bootstrap.js',
  'app/index.js',
  'app/styles',
];

const detect = () => {
  const result = findUnusedModule({
    cwd: `${path.resolve(__dirname, '../app')}`,
    entries: [`${path.resolve(__dirname, '../app/App.tsx')}`],
    includes: ['**/*', '!node_modules'],
    resolveRequirePath: (curDir: string, requirePath: string) => {
      const [prefix, ...rest] = requirePath.split('/');
      if (nodeModules.some((item) => requirePath.startsWith(item))) {
        return 'node_modules';
      } else if (aliasKeys.includes(prefix)) {
        const realPath = aliasMap[prefix];
        return path.resolve(realPath, rest.join('/'));
      } else if (requirePath.startsWith('core/')) {
        return 'node_modules';
      } else if (noneJs.some((item) => requirePath.endsWith(item))) {
        return 'node_modules';
      }
      return path.resolve(curDir, requirePath);
    },
    ignoreSuffix,
    ignorePath,
  });
  console.log(`unused files count: ${result.unused.length}`);
  (result.unused || []).forEach((item) => {
    console.log(item);
  });
  console.log(`unused export count: ${result.unusedExport.length}`);
  (result.unusedExport || []).forEach((item) => {
    console.log(item);
  });
};

detect();

export default detect;
