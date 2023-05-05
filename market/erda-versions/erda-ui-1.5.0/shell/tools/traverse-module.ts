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
import * as parser from '@babel/parser';
import traverse, { NodePath } from '@babel/traverse';
import fs from 'fs';
import { resolve, dirname, join, extname } from 'path';
import chalk from 'chalk';

const JS_EXTS = ['.js', '.jsx', '.ts', '.tsx', '.d.ts'];
const CSS_EXTS = ['.css', '.scss'];
const JSON_EXTS = ['.json'];

let requirePathResolver: (curDir: string, requirePath: string) => string;

const MODULE_TYPES = {
  JS: 0,
  CSS: 1,
  JSON: 2,
};

function isDirectory(filePath) {
  try {
    return fs.statSync(filePath).isDirectory();
  } catch (e) {
    return false;
  }
}

const visitedModules = new Set<string>();

function moduleResolver(curModulePath: string, requirePath: string): [string, boolean] {
  let checkingFile = requirePath;
  if (typeof requirePathResolver === 'function') {
    const res = requirePathResolver(dirname(curModulePath), checkingFile);
    if (typeof res === 'string') {
      checkingFile = res;
    }
  }

  // filter node_modules
  if (checkingFile.includes('node_modules')) {
    return ['', true];
  }

  checkingFile = resolve(dirname(curModulePath), checkingFile);
  checkingFile = completeModulePath(checkingFile);

  if (visitedModules.has(checkingFile)) {
    return [checkingFile, true];
  } else {
    visitedModules.add(checkingFile);
  }
  return [checkingFile, false];
}

/**
 * try to fulfil a module file path
 *
 * @param {string} modulePath
 * @returns
 */
function completeModulePath(modulePath: string) {
  const EXTS = [...JSON_EXTS, ...JS_EXTS];
  if (modulePath.match(/\.[a-zA-Z]+$/)) {
    return modulePath;
  }

  function tryCompletePath(resolvePath) {
    for (let i = 0; i < EXTS.length; i++) {
      const tryPath = resolvePath(EXTS[i]);
      if (fs.existsSync(tryPath)) {
        return tryPath;
      }
    }
  }

  function reportModuleNotFoundError(errorPath: string) {
    console.log(chalk.red(`module not found: ${errorPath}`));
  }

  if (isDirectory(modulePath)) {
    // e.g. './utils' is folder then try to find './utils/index.ts(js)'
    const tryModulePath = tryCompletePath((ext: string) => join(modulePath, `index${ext}`));
    if (!tryModulePath) {
      reportModuleNotFoundError(modulePath);
    } else {
      return tryModulePath;
    }
  } else if (!EXTS.some((ext) => modulePath.endsWith(ext))) {
    // e.g. 'core/nusi' is not a folder then try to find 'core/nusi.tsx(jsx/js/ts)'
    const tryModulePath = tryCompletePath((ext: string) => `${modulePath}${ext}`);
    if (!tryModulePath) {
      reportModuleNotFoundError(modulePath);
    } else {
      return tryModulePath;
    }
  }
  return modulePath;
}

const resolveBabelSyntaxPlugins = (modulePath: string) => {
  const plugins = [];
  if (['.tsx', '.jsx'].some((ext) => modulePath.endsWith(ext))) {
    plugins.push('jsx');
  }
  if (['.ts', '.tsx'].some((ext) => modulePath.endsWith(ext))) {
    plugins.push('typescript');
  }
  return plugins;
};

function getModuleType(modulePath: string) {
  const moduleExt = extname(modulePath);
  if (JS_EXTS.some((ext) => ext === moduleExt)) {
    return MODULE_TYPES.JS;
  } else if (CSS_EXTS.some((ext) => ext === moduleExt)) {
    return MODULE_TYPES.CSS;
  } else if (JSON_EXTS.some((ext) => ext === moduleExt)) {
    return MODULE_TYPES.JSON;
  }
}

const exportFromMap = {}; // to record `export {a,b,c} from 'd'`

function traverseJsModule(curModulePath: string, callback: (str: string, items: string[], isImport: boolean) => void) {
  const moduleFileContent = fs.readFileSync(curModulePath, {
    encoding: 'utf-8',
  });

  const ast = parser.parse(moduleFileContent, {
    sourceType: 'unambiguous',
    plugins: resolveBabelSyntaxPlugins(curModulePath),
  });

  const collectImportItems = (path: NodePath, subModulePath: string) => {
    const importItems: string[] = [];
    const specifiers = path.get('specifiers');
    if (Array.isArray(specifiers) && specifiers.length) {
      for (let i = 0; i < specifiers.length; i++) {
        const importItem = path.get(`specifiers.${i}`).toString();
        const subItem = `${subModulePath}-${importItem}`;
        if (exportFromMap[subItem]) {
          importItems.push(exportFromMap[subItem]);
        }
        importItems.push(subItem);
      }
    }
    callback(subModulePath, importItems, true);
  };

  traverse(ast, {
    ImportDeclaration(path) {
      // handle syntax `import react from 'react'`
      const node = path.get('source.value');
      const [subModulePath, visited] = moduleResolver(curModulePath, Array.isArray(node) ? '' : node.node.toString());

      if (!subModulePath || visited) {
        collectImportItems(path, subModulePath);
        return;
      }
      traverseModule(subModulePath, callback);
      collectImportItems(path, subModulePath);
    },
    CallExpression(path) {
      if (path.get('callee').toString() === 'require') {
        // handle syntax `const lodash = require('lodash')`
        const [subModulePath, visited] = moduleResolver(
          curModulePath,
          path.get('arguments.0').toString().replace(/['"]/g, ''),
        );
        if (!subModulePath) {
          return;
        }
        callback(subModulePath, [], visited);
        traverseModule(subModulePath, callback);
      }
      if (path.get('callee').type === 'Import') {
        // handle syntax dynamic import `const entry = import('layout/entry')`
        try {
          const importItem = path.get('arguments.0').toString().replace(/['"]/g, '');
          if (importItem && !importItem.includes('`')) {
            const [subModulePath, visited] = moduleResolver(curModulePath, importItem);
            if (!subModulePath || visited) {
              return;
            }
            callback(subModulePath, [], true);
            traverseModule(subModulePath, callback);
          }
        } catch (error) {
          console.log('parse import error', error);
        }
      }
    },
    ExportNamedDeclaration(path) {
      // handle syntax `export { Copy } from './components/copy'`
      if (path.get('source').node) {
        const node = path.get('source.value');
        const [subModulePath, visited] = moduleResolver(
          curModulePath,
          Array.isArray(node) ? '' : node.node.toString().replace(/['"]/g, ''),
        );
        if (!subModulePath || visited) {
          return;
        }
        const specifiers = path.get('specifiers');
        const exportItems: string[] = [];
        if (Array.isArray(specifiers) && specifiers.length) {
          for (let i = 0; i < specifiers.length; i++) {
            const importItem = path.get(`specifiers.${i}`).toString();
            exportFromMap[`${curModulePath}-${importItem}`] = `${subModulePath}-${importItem}`;
            exportItems.push(`${curModulePath}-${importItem}`);
          }
        }
        callback(subModulePath, exportItems, false);
        traverseModule(subModulePath, callback);
      } else if (path.get('declaration').node) {
        // handle export { a, b, c };
        const declarations = path.get('declaration.declarations');
        const exportItems: string[] = [];
        if (Array.isArray(declarations) && declarations.length) {
          for (let i = 0; i < declarations.length; i++) {
            exportItems.push(`${curModulePath}-${path.get(`declaration.declarations.${i}.id`).toString()}`);
          }
        }
        callback('', exportItems, false);
      }
    },
  });
}

export const traverseModule = (
  curModulePath: string,
  callback: (str: string, items: string[], isImport: boolean) => void,
) => {
  const modulePath = completeModulePath(curModulePath);
  const moduleType = getModuleType(modulePath);

  if (moduleType === MODULE_TYPES.JS) {
    traverseJsModule(modulePath, callback);
  }
};

export const setRequirePathResolver = (resolver: (curDir: string, requirePath: string) => string) => {
  requirePathResolver = resolver;
};
