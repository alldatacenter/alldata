#!/usr/bin/env node
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
import { logInfo } from './log';

const { translate } = require('@paiva/translation-google');
// Filter and replace words that will cause i118next translation errors, such as dots and colons
const filterInvalidWord = (enWord: string) => {
  return enWord.replace(/:/g, '&#58;');
};

const tempFilePath = path.resolve(process.cwd(), './temp-zh-words.json');

/**
 * translate En words to Zh words
 * */
export const doTranslate = async () => {
  const rawFile = fs.readFileSync(tempFilePath);
  const wordList = JSON.parse(rawFile.toString());

  const toTransList = Object.keys(wordList);
  if (toTransList.length === 0) {
    return;
  }

  const promises = toTransList.map(async (word) => {
    const result = await translate(word, {
      tld: 'en',
      to: 'zh-cn',
    });
    return { zh: result.text, en: word };
  });
  const translatedList = await Promise.allSettled(promises);

  translatedList
    .filter((item) => item.status === 'fulfilled')
    .forEach((result) => {
      const { zh, en } = (result as PromiseFulfilledResult<{ zh: string; en: string }>).value;
      const zhWord = filterInvalidWord(zh);
      wordList[en] = zhWord;
      logInfo(`${en}: ${zhWord}`);
    });
  fs.writeFileSync(tempFilePath, JSON.stringify(wordList, null, '  '));
};
