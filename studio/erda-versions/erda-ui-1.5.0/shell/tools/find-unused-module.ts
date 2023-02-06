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

import { resolve, normalize } from 'path';
import fastGlob from 'fast-glob';
import { traverseModule, setRequirePathResolver } from './traverse-module';

interface Options {
  cwd: string;
  entries: string[];
  includes: string[];
  resolveRequirePath?: (curDir: string, requirePath: string) => string;
  ignoreSuffix?: string[];
  ignorePath?: string[];
}

const defaultOptions: Options = {
  cwd: '',
  entries: [],
  includes: ['**/*', '!node_modules'],
};

function findUnusedModule(options: Options) {
  const { cwd, entries, includes, resolveRequirePath, ignoreSuffix, ignorePath } = { ...defaultOptions, ...options };

  const includeContentPath = includes.map((includePath) => (cwd ? `${cwd}/${includePath}` : includePath));

  const allFiles = fastGlob.sync(includeContentPath).map((item) => normalize(item));
  const entryModules: string[] = [];
  const usedModules: string[] = [];
  const exportModules: string[] = [];
  const importModules: string[] = [];

  setRequirePathResolver(resolveRequirePath);
  entries.forEach((entry) => {
    const entryPath = resolve(cwd, entry);
    entryModules.push(entryPath);
    traverseModule(entryPath, (modulePath: string, items: string[], isImport: boolean) => {
      usedModules.push(modulePath);
      if (isImport) {
        importModules.push(...items);
      } else {
        exportModules.push(...items);
      }
    });
  });

  const importSet = new Set(importModules);
  const exportSet = new Set(exportModules);
  const unusedExport = [...exportSet].filter((x) => !importSet.has(x));

  const unusedModules = allFiles.filter((filePath) => {
    const resolvedFilePath = resolve(filePath);
    return (
      !entryModules.includes(resolvedFilePath) &&
      !usedModules.includes(resolvedFilePath) &&
      !ignoreSuffix.some((item) => resolvedFilePath.endsWith(item)) &&
      !ignorePath.some((item) => resolvedFilePath.includes(item))
    );
  });
  return {
    all: allFiles,
    used: usedModules,
    unused: unusedModules,
    unusedExport,
  };
}

export default findUnusedModule;
