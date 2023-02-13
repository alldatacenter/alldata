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

import { join } from 'path';
import fs from 'fs';
import rimraf from 'rimraf';
import { exit } from 'process';
import { logError, logInfo } from './log';
import dotenv from 'dotenv';
import pidtree from 'pidtree';

export const checkIsRoot = () => {
  if (!fs.existsSync(`${process.cwd()}/.env`)) {
    logError('Please run this command under erda-ui root directory');
    exit(1);
  }
};

export const isCwdInRoot = (params?: { currentPath?: string; alert?: boolean }) => {
  const currentDir = params?.currentPath || process.cwd();
  let isInRoot = true;
  if (!fs.existsSync(join(currentDir, 'package.json'))) {
    isInRoot = false;
  } else {
    const pkg = fs.readFileSync(join(currentDir, 'package.json'), 'utf8');
    const { name } = JSON.parse(pkg);
    if (name !== 'erda-ui') {
      isInRoot = false;
    }
  }

  if (!isInRoot && params?.alert) {
    logError('please run this command under erda-ui root directory');
    process.exit(1);
  }
  return isInRoot;
};

export const getCwdModuleName = (params?: { currentPath?: string }) => {
  const currentDir = params?.currentPath || process.cwd();
  let moduleName = null;
  let isInRoot = true;
  if (!fs.existsSync(join(currentDir, 'package.json'))) {
    isInRoot = false;
  } else {
    const pkg = fs.readFileSync(join(currentDir, 'package.json'), 'utf8');
    const { name } = JSON.parse(pkg);
    if (!name.startsWith('@erda-ui/')) {
      isInRoot = false;
    } else {
      moduleName = name.slice('@erda-ui/'.length);
    }
  }

  if (!isInRoot || !moduleName) {
    logError('please run this command under an erda-ui module root directory');
    process.exit(1);
  }
  return moduleName;
};

export const getEnvConfig = (currentDir?: string) => {
  const { parsed } = dotenv.config({ path: `${currentDir || process.cwd()}/.env` });
  if (!parsed) {
    throw Error(`.env file not exist in ${process.cwd()}`);
  }
  return parsed;
};

export const getModuleList = () => {
  const envConfig = getEnvConfig();
  return envConfig.MODULES.split(',').map((m) => m.trim());
};

export const clearPublic = async () => {
  logInfo('clear public folder');
  await rimraf.sync(`${getPublicDir()}/*`);
};

export const getCliDir = () => {
  return join(process.cwd(), 'cli');
};

export const getCoreDir = () => {
  return join(process.cwd(), 'core');
};

export const getShellDir = () => {
  return join(process.cwd(), 'shell');
};

export const getPublicDir = () => {
  return join(process.cwd(), 'public');
};

export const getSchedulerDir = () => {
  return join(process.cwd(), 'scheduler');
};

/**
 * kill all sub process of current process
 * @returns success boolean
 */
export const killPidTree = async (): Promise<boolean> => {
  let pids;
  try {
    pids = await pidtree(process.pid, { root: true });
  } catch (err) {
    if (err.message === 'No matching pid found') return true;
    throw err;
  }
  pids.forEach((el) => {
    try {
      process.kill(el);
    } catch (err) {
      if (err.code !== 'ESRCH') throw err;
    }
  });
  return true;
};

export const registryDir = 'registry.erda.cloud/erda/ui';
export const defaultRegistry = 'registry.cn-hangzhou.aliyuncs.com/terminus/erda-ui';

export const ALL_MODULES = ['core', 'shell', 'market', 'uc', 'admin', 'fdp'];
