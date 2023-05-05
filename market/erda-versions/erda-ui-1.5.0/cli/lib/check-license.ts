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
import inquirer from 'inquirer';
import { walker } from './util/file-walker';
import { logSuccess, logWarn } from './util/log';

export default async ({ fileType, directory, filter }: { fileType: string; directory: string; filter: string }) => {
  let targetPath = directory;
  if (!targetPath) {
    const answer = await inquirer.prompt([
      {
        type: 'directory',
        name: 'targetPath',
        message: 'Select work directory',
        basePath: process.cwd(),
        default: directory,
      },
    ]);
    targetPath = answer.targetPath;
  }

  let suffixList = fileType.split(',');
  if (!fileType) {
    const answer = await inquirer.prompt([
      {
        type: 'checkbox',
        name: 'suffixList',
        message: 'Which file type do you want to operate?',
        choices: ['js', 'ts', 'jsx', 'tsx', 'scss', 'sass'],
        default: ['js', 'ts', 'jsx', 'tsx'],
      },
    ]);
    suffixList = answer.suffixList;
  }

  const suffixMap: { [k: string]: boolean } = {};
  suffixList.forEach((a) => {
    suffixMap[a.startsWith('.') ? a : `.${a}`] = true;
  });

  const showWarnLog = ['all', 'warn'].includes(filter);
  const showSuccessLog = ['all', 'success'].includes(filter);

  let failCount = 0;
  walker({
    root: targetPath,
    dealFile(content, filePath, isEnd) {
      if (isEnd && failCount > 0) {
        logWarn('Failed count:', failCount);
        process.exit(1);
      }
      if (!suffixMap[path.extname(filePath)] || isEnd) {
        return;
      }
      if (!content.includes('// Copyright (c) 2021 Terminus, Inc.')) {
        showWarnLog && logWarn('License not exist:', filePath);
        failCount++;
      } else {
        showSuccessLog && logSuccess('License check ok:', filePath);
      }
    },
  });
};
