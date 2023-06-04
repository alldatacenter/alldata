/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

const path = require('path');
const readline = require('readline');
const { readFile, wirteFile, getBanner } = require('./utils');

const getMetaDir = (metaType, namespace, saveAs) => {
  const dirMap = {
    group: [
      // from:
      path.resolve(
        __dirname,
        `../../inlong-manager/manager-pojo/src/main/java/org/apache/inlong/manager/pojo/group`,
        `./${namespace.toLowerCase()}/Inlong${namespace}Request.java`,
      ),
      // to:
      path.resolve(__dirname, `../src/metas/groups/${saveAs}`, `${namespace}.ts`),
    ],
    consume: [
      // from:
      path.resolve(
        __dirname,
        `../../inlong-manager/manager-pojo/src/main/java/org/apache/inlong/manager/pojo/consume`,
        `./${namespace.toLowerCase()}/Consume${namespace}Request.java`,
      ),
      // to:
      path.resolve(__dirname, `../src/metas/consumes/${saveAs}`, `${namespace}.ts`),
    ],
    node: [
      // from:
      path.resolve(
        __dirname,
        `../../inlong-manager/manager-pojo/src/main/java/org/apache/inlong/manager/pojo/node`,
        `./es/${namespace}DataNodeRequest.java`, // TODO
      ),
      // to:
      path.resolve(__dirname, `../src/metas/nodes/${saveAs}`, `${namespace}.ts`),
    ],
  };

  if (!dirMap[metaType]) {
    throw new Error(`[Error] MetaType: ${metaType} not exist.`);
  }

  return dirMap[metaType];
};

const genMetaData = (metaType, namespace, list) => {
  const MetaType = `${metaType[0].toUpperCase()}${metaType.slice(1)}`;
  const BasicInfoName = `${MetaType}Info`;
  const str = `\

import { DataWithBackend } from '@/metas/DataWithBackend';
import { RenderRow } from '@/metas/RenderRow';
import { RenderList } from '@/metas/RenderList';
import { ${BasicInfoName} } from '../common/${BasicInfoName}';

const { I18n } = DataWithBackend;
const { FieldDecorator } = RenderRow;

export default class ${namespace}${MetaType}
  extends ${BasicInfoName}
  implements DataWithBackend, RenderRow, RenderList
{
  ${list
    .map(item => {
      const { dataType, key, defaultValue } = item;
      return `
  @FieldDecorator({
    type: 'input',
    initialValue: ${defaultValue},
    props: {
    },
  })
  @I18n('${key}')
  ${key}: ${dataType};
  `;
    })
    .join('')}
}
  `;
  return str;
};

const runSync = () => {
  const rl = readline.createInterface({
    input: process.stdin,
    output: process.stdout,
  });

  rl.question(`What's your metaType? (Support: group/consume): `, metaType => {
    rl.question(`What's your namespace? (For example: Kafka): `, namespace => {
      rl.question(`Save as defaults or extends? `, saveAs => {
        rl.close();

        const [backendFilePath, outputFilePath] = getMetaDir(metaType, namespace, saveAs);
        const dataText = readFile(backendFilePath);
        const beginIndex = dataText.indexOf('class');

        if (beginIndex !== -1) {
          const arr = dataText.slice(beginIndex).match(/private.+;/g) || [];
          const list = arr.map(item => {
            if (item[item.length - 1] === ';') item = item.slice(0, -1);
            const [, dataType, key, , defaultValue] = item.split(' ');
            return { dataType, key, defaultValue };
          });

          const banner = getBanner();
          const text = genMetaData(metaType, namespace, list);
          const buffer = Buffer.from(`${banner}${text}`);

          wirteFile(outputFilePath, buffer);
          console.log(`Build ${metaType}: ${namespace} successfully! Saved at ${outputFilePath}`);
        }
      });
      rl.write('defaults');
    });
  });
};

runSync();
