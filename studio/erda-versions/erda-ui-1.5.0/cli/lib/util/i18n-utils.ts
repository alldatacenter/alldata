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

import fs from 'fs';
import path from 'path';
import { logInfo, logSuccess, logWarn, logError } from './log';
import writeLocale from './i18n-extract';
import ora from 'ora';
import { merge, remove, unset } from 'lodash';
import chalk from 'chalk';
import inquirer from 'inquirer';
import { walker } from './file-walker';
import {
  externalLocalePathMap,
  externalModuleNamespace,
  externalSrcDirMap,
  internalLocalePathMap,
  internalSrcDirMap,
  Obj,
} from './i18n-config';

export const tempFilePath = path.resolve(process.cwd(), './temp-zh-words.json');
export const tempTranslatedWordPath = path.resolve(process.cwd(), './temp-translated-words.json');

/**
 * find folder which name matches folderName under workDir
 * @param {*} folderName
 * @returns
 */
export const findMatchFolder = (folderName: string, workDir: string): string | null => {
  let targetPath: null | string = null;
  const loopFolder = (rootPath: string) => {
    const children = fs.readdirSync(rootPath, { withFileTypes: true });
    if (children.length > 0) {
      children.some((child) => {
        const itemName = child.name;
        if (child.isDirectory() && !itemName.includes('node_modules') && !itemName.startsWith('.')) {
          const childPath = path.resolve(rootPath, itemName);
          if (itemName === folderName) {
            targetPath = childPath;
            return true;
          }
          return loopFolder(childPath);
        }
        return false;
      });
    }
  };
  loopFolder(workDir);
  return targetPath;
};

/**
 * create temp files and collect all related locale file contents
 */
export const prepareEnv = (isExternal: boolean, switchNs: boolean) => {
  let zhResource: Obj<Obj> = {};
  let enResource: Obj<Obj> = {};
  if (!switchNs && !fs.existsSync(tempFilePath)) {
    fs.writeFileSync(tempFilePath, JSON.stringify({}, null, 2), 'utf8');
  }
  if (!switchNs && !fs.existsSync(tempTranslatedWordPath)) {
    fs.writeFileSync(tempTranslatedWordPath, JSON.stringify({}, null, 2), 'utf8');
  }
  const localeMap = isExternal ? externalLocalePathMap : internalLocalePathMap;
  const localePaths = Object.values(localeMap);
  const originalLocaleResources = localePaths.reduce<Obj<[Obj<Obj>, Obj<Obj>]>>((acc, localePath) => {
    const zhJsonPath = `${localePath}/zh.json`;
    const enJsonPath = `${localePath}/en.json`;
    zhResource = JSON.parse(fs.readFileSync(zhJsonPath, 'utf8'));
    enResource = JSON.parse(fs.readFileSync(enJsonPath, 'utf8'));
    const moduleName = Object.keys(localeMap).find((key) => localeMap[key] === localePath);
    acc[moduleName!] = [zhResource, enResource];
    return acc;
  }, {});
  return originalLocaleResources;
};

/**
 * write locale files
 * @param localePath locale path to translate
 * @param workDir work directory
 */
export const writeLocaleFiles = async (isExternal: boolean) => {
  const promise = new Promise<void>((resolve) => {
    writeLocale(resolve, isExternal);
  });
  const loading = ora('writing locale file...').start();
  await promise;
  loading.stop();
  logSuccess('write locale file completed');
};

/**
 * filter the pending translation list to translated list & un-translated list
 * @param toTranslateEnWords string array that need to translate which extract from raw source code
 */
export const filterTranslationGroup = (
  toTranslateEnWords: string[],
  zhResource: Obj<Obj>,
  untranslatedWords: Set<string>,
  translatedWords: Obj,
) => {
  const notTranslatedWords = [...toTranslateEnWords]; // The English collection of the current document that needs to be translated
  // Traverse namespaces of zh.json to see if there is any English that has been translated
  Object.keys(zhResource).forEach((namespaceKey) => {
    // All translations in the current namespace
    const namespaceWords = zhResource[namespaceKey];
    toTranslateEnWords.forEach((enWord) => {
      const convertedEnWord = enWord.replace(/:/g, '&#58;');
      // When there is an existing translation and translatedWords does not contains it, add it to the translated list and remove it from the untranslated list
      if (namespaceWords[convertedEnWord] && !translatedWords[convertedEnWord]) {
        // eslint-disable-next-line no-param-reassign
        translatedWords[convertedEnWord] =
          namespaceKey === 'default'
            ? namespaceWords[convertedEnWord]
            : `${namespaceKey}:${namespaceWords[convertedEnWord]}`;
        remove(notTranslatedWords, (w) => w === enWord);
      }
    });
  });
  notTranslatedWords.forEach(untranslatedWords.add, untranslatedWords);
};

const i18nDRegex = /i18n\.d\(["'](.+?)["']\)/g;

export const extractAllI18nD = async (
  isExternal: boolean,
  originalResource: Obj<[Obj<Obj>, Obj<Obj>]>,
  translatedWords: Obj,
  untranslatedWords: Set<string>,
) => {
  const dirMap = isExternal ? externalSrcDirMap : internalSrcDirMap;
  const promises = Object.values(dirMap)
    .flat()
    .map((srcPath) => {
      return new Promise<void>((resolve) => {
        const moduleName = Object.keys(dirMap).find((key) => dirMap[key].includes(srcPath));
        const [zhResource] = originalResource[moduleName!];
        // first step is to find out the content that needs to be translated, and assign the content to two parts: untranslated and translated
        walker({
          root: srcPath,
          dealFile: (...args) => {
            extractUntranslatedWords.apply(null, [
              ...args,
              !isExternal ? merge(zhResource, originalResource.default[0]) : zhResource,
              translatedWords,
              untranslatedWords,
              resolve,
            ]);
          },
        });
      });
    });
  await Promise.all(promises);

  // After all files are traversed, notTranslatedWords is written to temp-zh-words in its original format
  if (untranslatedWords.size > 0) {
    const enMap: Obj = {};
    untranslatedWords.forEach((word) => {
      enMap[word] = '';
    });
    fs.writeFileSync(tempFilePath, JSON.stringify(enMap, null, 2), 'utf8');
    logSuccess(`Finish writing to the temporary file ${chalk.green('[temp-zh-words.json]')}`);
  }
  // translatedWords write to [temp-translated-words.json]
  if (Object.keys(translatedWords).length > 0) {
    fs.writeFileSync(tempTranslatedWordPath, JSON.stringify(translatedWords, null, 2), 'utf8');
    logSuccess(`Finish writing to the temporary file ${chalk.green('[temp-translated-words.json]')}`);
  }
};

/**
 * extract i18n.d and write filtered content to two temp files
 * @param content raw file content
 * @param filePath file path
 * @param isEnd is traverse done
 * @param zhResource origin zh.json content
 * @param translatedWords translated collection
 * @param untranslatedWords untranslated collection
 * @param resolve promise resolver
 */
export const extractUntranslatedWords = (
  content: string,
  filePath: string,
  isEnd: boolean,
  zhResource: Obj<Obj>,
  translatedWords: Obj,
  untranslatedWords: Set<string>,
  resolve: (value: void | PromiseLike<void>) => void,
) => {
  // Only process code files
  if (!['.tsx', '.ts', '.js', '.jsx'].includes(path.extname(filePath)) && !isEnd) {
    return;
  }
  let match = i18nDRegex.exec(content);
  const toTransEnglishWords = []; // Cut out all the English that are packaged by i18n.d in the current file
  while (match) {
    if (match) {
      toTransEnglishWords.push(match[1]);
    }
    match = i18nDRegex.exec(content);
  }
  if (!isEnd && !toTransEnglishWords.length) {
    return;
  }

  // English list that needs to be translated, mark sure it does not appear in notTranslatedWords and translatedWords
  filterTranslationGroup(
    toTransEnglishWords.filter((enWord) => !untranslatedWords.has(enWord) && !translatedWords[enWord]),
    zhResource,
    untranslatedWords,
    translatedWords,
  );
  if (isEnd) {
    resolve();
  }
};

/**
 * i18n.d => i18n.t for all source files
 * @param isExternal is external module
 * @param ns target namespace
 * @param translatedMap is translated resource
 * @param reviewedZhMap is newly translated resource
 */
export const writeI18nTToSourceFile = async (
  isExternal: boolean,
  ns: string,
  translatedMap: Obj,
  reviewedZhMap: Obj,
) => {
  const dirMap = isExternal ? externalSrcDirMap : internalSrcDirMap;
  const promises = Object.values(dirMap)
    .flat()
    .map((srcPath) => {
      let namespace = ns;
      if (isExternal) {
        const moduleName = Object.keys(dirMap).find((name) => dirMap[name].includes(srcPath));
        namespace = externalModuleNamespace[moduleName!];
      }

      const generatePromise = new Promise((resolve) => {
        walker({
          root: srcPath,
          dealFile: (...args) => {
            restoreSourceFile.apply(null, [...args, namespace, translatedMap, reviewedZhMap, resolve]);
          },
        });
      });
      return generatePromise;
    });
  await Promise.all(promises);
};

/**
 * restore raw file i18n.d => i18n.t with namespace
 * @param content raw file content
 * @param filePath file path with extension
 * @param isEnd is traverse done
 * @param ns is target namespace
 * @param translatedMap is translated resource
 * @param reviewedZhMap is newly translated resource
 * @param resolve resolver of promise
 */
export const restoreSourceFile = (
  content: string,
  filePath: string,
  isEnd: boolean,
  ns: string,
  translatedMap: Obj,
  reviewedZhMap: Obj,
  resolve: (value: void | PromiseLike<void>) => void,
) => {
  if (!['.tsx', '.ts', '.js', '.jsx'].includes(path.extname(filePath)) && !isEnd) {
    return;
  }
  let match = i18nDRegex.exec(content);
  let newContent = content;
  let changed = false;
  while (match) {
    if (match) {
      const [fullMatch, enWord] = match;
      let replaceText;
      const convertedEnWord = enWord.replace(/:/g, '&#58;');
      if (reviewedZhMap?.[enWord]) {
        // Replace if found the translation in [temp-zh-words.json]
        const i18nContent = ns === 'default' ? `i18n.t('${convertedEnWord}')` : `i18n.t('${ns}:${convertedEnWord}')`;
        replaceText = i18nContent;
      } else if (translatedMap?.[convertedEnWord]) {
        // Replace if find the translation in [temp-translated-words.json]
        const nsArray = translatedMap?.[convertedEnWord].split(':');
        replaceText =
          nsArray.length === 2 ? `i18n.t('${nsArray[0]}:${convertedEnWord}')` : `i18n.t('${convertedEnWord}')`;
      } else {
        logWarn(convertedEnWord, 'not yet translated');
      }
      if (replaceText) {
        newContent = newContent.replace(fullMatch, replaceText);
        changed = true;
      }
    }
    match = i18nDRegex.exec(content);
  }
  if (changed) {
    fs.writeFileSync(filePath, newContent, 'utf8');
  }
  if (isEnd) {
    resolve();
  }
};

const i18nRRegex = /i18n\.r\(\s*('|")([^'"]+)(?:'|")([^)\n]*)\s*\)/g;

/**
 * extract i18n.r content and replace it with i18n.t
 * @param content raw file content
 * @param filePath file path
 * @param isEnd is traverse done
 * @param ns is target namespace
 * @param toSwitchWords is words waiting to switch
 * @param resolve promise resolver
 */
export const extractPendingSwitchContent = (
  content: string,
  filePath: string,
  isEnd: boolean,
  ns: string,
  toSwitchWords: Set<string>,
  resolve: (value: void | PromiseLike<void>) => void,
) => {
  // Only process code files
  if (!['.tsx', '.ts', '.js', '.jsx'].includes(path.extname(filePath)) && !isEnd) {
    return;
  }
  let match = i18nRRegex.exec(content);
  let replacedText = content;
  let changed = false;
  while (match) {
    if (match) {
      const matchedText = match[2];
      const quote = match[1];
      toSwitchWords.add(matchedText);
      const wordArr = matchedText.split(':');
      const enWord = wordArr.length === 2 ? wordArr[1] : matchedText;
      const newWordText = ns === 'default' ? enWord : `${ns}:${enWord}`;
      replacedText = replacedText.replace(match[0], `i18n.t(${quote}${newWordText}${quote}${match[3] || ''})`);
      changed = true;
    }
    match = i18nRRegex.exec(content);
  }
  if (changed) {
    fs.writeFileSync(filePath, replacedText, 'utf8');
  }
  if (!isEnd && toSwitchWords.size === 0) {
    return;
  }

  if (isEnd) {
    resolve();
  }
};

/**
 * switch raw file i18n.t => i18n.t with namespace
 * @param content raw file content
 * @param filePath file path with extension
 * @param isEnd is traverse done
 * @param ns target namespace
 * @param toSwitchWords pending switch words
 * @param resolve resolver of promise
 */
export const switchSourceFileNs = (
  content: string,
  filePath: string,
  isEnd: boolean,
  ns: string,
  toSwitchWords: Set<string>,
  resolve: (value: void | PromiseLike<void>) => void,
) => {
  if (!['.tsx', '.ts', '.js', '.jsx'].includes(path.extname(filePath)) && !isEnd) {
    return;
  }
  let newContent = content;
  let changed = false;
  toSwitchWords.forEach((wordWithNs) => {
    // /i18n\.r\(\s*('|")([^'"]+)(?:'|")([^)\n]*)\s*\)/g
    const matchTextRegex = new RegExp(`i18n\\.t\\(\\s*('|")${wordWithNs}(?:'|")([^\\)\\n]*)\\s*\\)`, 'g');
    let match = matchTextRegex.exec(content);
    while (match) {
      changed = true;
      const matchedText = match[0];
      const quote = match[1];
      const wordArr = wordWithNs.split(':');
      const enWord = wordArr.length === 2 ? wordArr[1] : wordWithNs;
      const newWordText = ns === 'default' ? enWord : `${ns}:${enWord}`;
      newContent = newContent.replace(matchedText, `i18n.t(${quote}${newWordText}${quote}${match[2] || ''})`);
      match = matchTextRegex.exec(content);
    }
  });
  if (changed) {
    fs.writeFileSync(filePath, newContent, 'utf8');
  }
  if (isEnd) {
    resolve();
  }
};

const getNamespaceModuleName = (originalResources: Obj<[Obj<Obj>, Obj<Obj>]>, currentNs: string) => {
  let result = null;
  Object.entries(originalResources).some(([moduleName, content]) => {
    const [zhResource] = content;
    if (zhResource[currentNs]) {
      result = moduleName;
      return true;
    }
    return false;
  });
  return result;
};

/**
 * batch switch namespace
 * @param originalResources original locale content
 */
export const batchSwitchNamespace = async (originalResources: Obj<[Obj<Obj>, Obj<Obj>]>) => {
  const toSwitchWords = new Set<string>();
  const nsList = Object.values(originalResources).reduce<string[]>((acc, resource) => {
    const [zhResource] = resource;
    return acc.concat(Object.keys(zhResource));
  }, []);
  const { targetNs } = await inquirer.prompt({
    name: 'targetNs',
    type: 'list',
    message: 'Please select the new namespace name',
    choices: nsList.map((ns) => ({ value: ns, name: ns })),
  });
  // extract all i18n.r
  const promises = Object.values(internalSrcDirMap)
    .flat()
    .map((srcDir) => {
      return new Promise<void>((resolve) => {
        walker({
          root: srcDir,
          dealFile: (...args) => {
            extractPendingSwitchContent.apply(null, [...args, targetNs, toSwitchWords, resolve]);
          },
        });
      });
    });
  await Promise.all(promises);
  if (toSwitchWords.size) {
    const restorePromises = Object.values(internalSrcDirMap)
      .flat()
      .map((srcDir) => {
        return new Promise<void>((resolve) => {
          walker({
            root: srcDir,
            dealFile: (...args) => {
              switchSourceFileNs.apply(null, [...args, targetNs, toSwitchWords, resolve]);
            },
          });
        });
      });
    await Promise.all(restorePromises);
    // restore locale files
    for (const wordWithNs of toSwitchWords) {
      const wordArr = wordWithNs.split(':');
      const [currentNs, enWord] = wordArr.length === 2 ? wordArr : ['default', wordWithNs];
      const currentModuleName = getNamespaceModuleName(originalResources, currentNs);
      const targetModuleName = getNamespaceModuleName(originalResources, targetNs);
      if (!currentModuleName || !targetModuleName) {
        logError(`${currentModuleName} or ${targetModuleName} does not exist in locale files`);
        return;
      }

      // replace zh.json content
      const targetNsContent = originalResources[targetModuleName][0][targetNs];
      const currentNsContent = originalResources[currentModuleName][0][currentNs];
      if (!targetNsContent[enWord] || targetNsContent[enWord] === currentNsContent[enWord]) {
        targetNsContent[enWord] = currentNsContent[enWord];
      } else {
        // eslint-disable-next-line no-await-in-loop
        const confirm = await inquirer.prompt({
          name: 'confirm',
          type: 'confirm',
          message: `${chalk.red(enWord)} has translation in target namespace ${targetNs} with value ${chalk.yellow(
            targetNsContent[enWord],
          )}, Do you want to override it with ${chalk.yellow(currentNsContent[enWord])}?`,
        });
        if (confirm) {
          targetNsContent[enWord] = currentNsContent[enWord];
        }
      }
      currentNs !== targetNs && unset(currentNsContent, enWord);

      // replace en.json content
      const targetNsEnContent = originalResources[targetModuleName][1][targetNs];
      const currentNsEnContent = originalResources[currentModuleName][1][currentNs];
      if (!targetNsEnContent[enWord]) {
        targetNsEnContent[enWord] = currentNsEnContent[enWord];
      }
      currentNs !== targetNs && unset(currentNsEnContent, enWord);
    }
    for (const moduleName of Object.keys(originalResources)) {
      const [zhResource, enResource] = originalResources[moduleName];
      const localePath = internalLocalePathMap[moduleName];
      fs.writeFileSync(`${localePath}/zh.json`, JSON.stringify(zhResource, null, 2), 'utf8');
      fs.writeFileSync(`${localePath}/en.json`, JSON.stringify(enResource, null, 2), 'utf8');
    }
    logInfo('sort current locale files & remove unused translation');
    await writeLocaleFiles(false);
    logSuccess('switch namespace done.');
  } else {
    logWarn(`no ${chalk.red('i18n.r')} found in source code. program exit`);
  }
};
