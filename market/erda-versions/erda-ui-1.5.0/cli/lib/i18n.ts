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
import inquirer from 'inquirer';
import chalk from 'chalk';
import ora from 'ora';
import { doTranslate as googleTranslate } from './util/google-translate';
import { doTranslate as yuodaoTranslate } from './util/youdao-translate';
import { logInfo, logSuccess, logError } from './util/log';
import { isCwdInRoot } from './util/env';
import {
  extractAllI18nD,
  prepareEnv,
  tempFilePath,
  tempTranslatedWordPath,
  writeI18nTToSourceFile,
  writeLocaleFiles,
  batchSwitchNamespace,
} from './util/i18n-utils';
import path from 'path';
import { Obj } from './util/i18n-config';

const configFilePath = path.resolve(process.cwd(), '.translaterc');

export default async ({ isSwitchNs = false, isExternal = false }: { isSwitchNs?: boolean; isExternal?: boolean }) => {
  try {
    // check if cwd erda ui root
    isCwdInRoot({ alert: true });

    const originalResources = prepareEnv(isExternal, isSwitchNs);
    // switch namespace
    if (isSwitchNs) {
      if (isExternal) {
        logError('external module has only one namespace, no need to switch');
        return;
      }
      await batchSwitchNamespace(originalResources);
      return;
    }

    const untranslatedWords = new Set<string>(); // Untranslated collection
    const translatedWords: Obj = {};

    // extract all i18n.d
    await extractAllI18nD(isExternal, originalResources, translatedWords, untranslatedWords);

    if (untranslatedWords.size === 0 && Object.keys(translatedWords).length === 0) {
      logInfo('sort current locale files & remove unused translation');
      await writeLocaleFiles(isExternal);
      logInfo('no content needs to be translated is found, program exits');
      return;
    }

    if (Object.keys(translatedWords).length > 0) {
      await inquirer.prompt({
        name: 'confirm',
        type: 'confirm',
        message: `Please carefully check whether the existing translation of ${chalk.green(
          '[temp-translated-words.json]',
        )} is suitable, if you are not satisfied, please move the content into ${chalk.green(
          '[temp-zh-words.json]',
        )}, no problem or after manual modification press enter to continue`,
      });
    }

    const tempWords = JSON.parse(fs.readFileSync(tempFilePath, { encoding: 'utf-8' }));
    const _untranslatedWords = Object.keys(tempWords);
    // The second step is to call google/youdao Translate to automatically translate
    if (_untranslatedWords.length > 0) {
      const isUsingYoudao = fs.existsSync(configFilePath);
      const spinner = ora(`${isUsingYoudao ? 'youdao' : 'google'} automatic translating...`).start();
      const translateMethod = isUsingYoudao ? yuodaoTranslate : googleTranslate;
      await translateMethod();
      spinner.stop();
      logSuccess('automatic translation completed');
      // The third step, manually checks whether there is a problem with the translation
      await inquirer.prompt({
        name: 'confirm',
        type: 'confirm',
        message: `Please double check whether the automatic translation of ${chalk.green(
          '[temp-zh-words.json]',
        )} is suitable, no problem or after manual modification then press enter to continue`,
      });
    }

    const reviewedZhMap = JSON.parse(fs.readFileSync(tempFilePath, { encoding: 'utf-8' }));
    let translatedMap: Obj = {};

    if (Object.keys(translatedWords).length > 0) {
      translatedMap = JSON.parse(fs.readFileSync(tempTranslatedWordPath, { encoding: 'utf-8' }));
    }
    let ns = '';
    // The fourth step is to specify the namespace
    if (reviewedZhMap && Object.keys(reviewedZhMap).length && !isExternal) {
      const nsList = Object.values(originalResources).reduce<string[]>((acc, resource) => {
        const [zhResource] = resource;
        return acc.concat(Object.keys(zhResource));
      }, []);
      const { targetNs } = await inquirer.prompt({
        name: 'targetNs',
        type: 'list',
        message: 'please select the new namespace name',
        choices: nsList.map((_ns) => ({ value: _ns, name: _ns })),
      });
      logInfo('Specify the namespace as', targetNs);
      ns = targetNs;
    }
    // The fifth step, i18n.t writes back the source file
    const spinner = ora('replacing source file...').start();
    await writeI18nTToSourceFile(isExternal, ns, translatedMap, reviewedZhMap);
    spinner.stop();
    logSuccess('replacing source file completed');
    // The sixth step, write the locale file
    if (reviewedZhMap && Object.keys(reviewedZhMap).length > 0) {
      await writeLocaleFiles(isExternal);
    }
  } finally {
    if (!isSwitchNs) {
      fs.unlinkSync(tempFilePath);
      fs.unlinkSync(tempTranslatedWordPath);
      logSuccess('clearing of temporary files completed');
    }
  }
  logInfo('i18n process is completed, see you👋');
};
