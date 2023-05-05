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

import path from 'path';

export const externalModules = ['admin', 'fdp', 'uc'];

export const internalModules = ['shell'];

export interface Obj<T extends any = string> {
  [k: string]: T;
}

const resolveUI = (...relativePath: string[]) => path.resolve(process.cwd(), ...relativePath);
const resolveEnterprise = (...relativePath: string[]) => resolveUI('../erda-ui-enterprise', ...relativePath);

// all locale path
export const internalLocalePathMap: Obj = {
  default: resolveUI('locales'),
  shell: resolveUI('shell', 'app', 'locales'),
};

export const externalLocalePathMap: Obj = {
  fdp: resolveEnterprise('fdp', 'src', 'locales'),
  admin: resolveEnterprise('admin', 'src', 'locales'),
  uc: resolveUI('modules', 'uc', 'src', 'locales'),
};

// all source code locations
export const internalSrcDirMap: Obj<string[]> = {
  shell: [resolveUI('shell', 'app'), resolveEnterprise('msp')],
};

export const externalSrcDirMap: Obj<string[]> = {
  fdp: [resolveEnterprise('fdp', 'src')],
  admin: [resolveEnterprise('admin', 'src')],
  uc: [resolveUI('modules', 'uc', 'src')],
};

// external modules only has one namespace
export const externalModuleNamespace: Obj = {
  fdp: 'fdp',
  admin: 'admin',
  uc: 'default',
};
