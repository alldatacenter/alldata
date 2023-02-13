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

import execa, { ExecaChildProcess } from 'execa';
import { logSuccess, logError } from './util/log';
import { checkIsRoot, ALL_MODULES, clearPublic, killPidTree } from './util/env';
import generateVersion from './util/gen-version';
import localIcon from './local-icon';

const currentDir = process.cwd();

const dirCollection: { [k: string]: string } = {
  core: `${currentDir}/core`,
  shell: `${currentDir}/shell`,
  market: `${currentDir}/modules/market`,
  uc: `${currentDir}/modules/uc`,
  fdp: `${currentDir}/modules/fdp`,
  admin: `${currentDir}/modules/admin`,
};

const dirMap = new Map(Object.entries(dirCollection));

const buildModules = async (rebuildList: string[]) => {
  const pList: ExecaChildProcess[] = [];
  rebuildList.forEach((moduleName) => {
    const moduleDir = dirMap.get(moduleName);
    const promise = execa('npm', ['run', 'build'], {
      cwd: moduleDir,
      env: {
        ...process.env,
        isOnline: 'true',
      },
      stdio: 'inherit',
    });
    pList.push(promise);
  });

  try {
    await Promise.all(pList);
  } catch (_error) {
    logError('build failed, will cancel all building processes');
    await killPidTree();
    process.exit(1);
  }
  logSuccess('build successfully 😁!');
};

export default async () => {
  try {
    checkIsRoot();

    const buildList = ALL_MODULES;

    await clearPublic();

    await buildModules(buildList);

    generateVersion();

    localIcon();
  } catch (error) {
    logError('build exit with error:', error.message);
    process.exit(1);
  }
};
