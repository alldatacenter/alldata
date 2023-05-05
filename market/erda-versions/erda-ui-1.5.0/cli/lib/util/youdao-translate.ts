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

import crypto from 'crypto';
import fs from 'fs';
import axios from 'axios';
import path from 'path';
import { logInfo } from './log';
import dotenv from 'dotenv';

const tempFilePath = path.resolve(process.cwd(), './temp-zh-words.json');
const configFilePath = path.resolve(process.cwd(), '.translaterc');

// Filter and replace words that will cause i118next translation errors, such as dots and colons
const filterInvalidWord = (enWord: string) => {
  return enWord.replace(/:/g, '&#58;');
};

const truncate = (q: string) => {
  const len = q.length;
  if (len <= 20) return q;
  return `${q.substring(0, 10)}${len}${q.substring(len - 10, len)}`;
};

const translate = (config: { appKey: string; secretKey: string }) => async (q: string) => {
  const hash = crypto.createHash('sha256'); // sha256
  const { appKey, secretKey: key } = config;
  const salt = new Date().getTime();
  const curtime = Math.round(salt / 1000);
  const signStr = `${appKey}${truncate(q)}${salt}${curtime}${key}`;

  hash.update(signStr);

  const sign = hash.digest('hex');

  const res = await axios.get<{ translation: string[] }>('https://openapi.youdao.com/api', {
    params: {
      from: 'en',
      to: 'zh-CHS',
      q,
      appKey,
      salt,
      curtime,
      signType: 'v3',
      sign,
    },
  });
  return res.data.translation[0];
};

/**
 * translate En words to Zh words by youdao api
 * */
export const doTranslate = async () => {
  const rawFile = fs.readFileSync(tempFilePath);
  const wordList = JSON.parse(rawFile.toString());

  const { parsed } = dotenv.config({ path: configFilePath });

  const toTransList = Object.keys(wordList);
  if (!toTransList.length) {
    return;
  }

  const invokeTranslate = translate(parsed as { appKey: string; secretKey: string });
  const result = await invokeTranslate(toTransList.join('\n'));
  const translationResult = result.split('\n');
  const translatedList = toTransList.reduce<Array<{ zh: string; en: string }>>((acc, word, i) => {
    acc.push({ zh: translationResult[i], en: word });
    return acc;
  }, []);

  translatedList.forEach((translatedWord) => {
    const { zh, en } = translatedWord;
    const zhWord = filterInvalidWord(zh);
    wordList[en] = zhWord;
    logInfo(`${en}: ${zhWord}`);
  });
  fs.writeFileSync(tempFilePath, JSON.stringify(wordList, null, 2));
};
