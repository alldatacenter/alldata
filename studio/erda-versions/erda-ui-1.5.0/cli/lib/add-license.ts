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
import chalk from 'chalk';
import inquirer from 'inquirer';
import { walker } from './util/file-walker';
import { logInfo, logSuccess, logError } from './util/log';
import licenseTpl from '../templates/license';

export default async ({ fileType }: { fileType: string }) => {
  const { isClean } = await inquirer.prompt([
    {
      type: 'confirm',
      name: 'isClean',
      message: 'Before this action, Please ensure your workspace is clean',
      default: false,
    },
  ]);
  if (!isClean) {
    logInfo('Nothing changed, exit');
    process.exit();
  }

  const { targetPath } = await inquirer.prompt([
    {
      type: 'directory',
      name: 'targetPath',
      message: 'Select work directory',
      basePath: process.cwd(),
    },
  ]);

  const { licenseType } = await inquirer.prompt<{ licenseType: 'GPLV3' | 'Apache2' }>([
    {
      type: 'list',
      name: 'licenseType',
      message: 'Which license type do you want to add?',
      choices: ['GPLV3', 'Apache2'],
      default: 'GPLV3',
    },
  ]);

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

  const { confirm } = await inquirer.prompt([
    {
      type: 'confirm',
      name: 'confirm',
      message: `Confirm your choices?
License: ${chalk.greenBright(licenseType)}
FileType: ${chalk.greenBright(suffixList)}
Directory: ${chalk.greenBright(targetPath)}
`,
      default: true,
    },
  ]);
  if (!confirm) {
    logInfo('Nothing changed, exit');
    process.exit();
  }

  walker({
    root: targetPath,
    dealFile(content, filePath) {
      if (!suffixMap[path.extname(filePath)]) {
        return;
      }
      let fileContent = content;
      if (!fileContent.includes('// Copyright (c) 2021 Terminus, Inc.')) {
        const header = licenseTpl[licenseType];
        if (fileContent.startsWith('#!')) {
          const [firstLine, ...rest] = fileContent.split('\n');
          fileContent = [firstLine, '\n', ...header.split('\n'), ...rest].join('\n');
        } else {
          fileContent = `${header}${fileContent}`;
        }
        fs.writeFile(filePath, fileContent, { encoding: 'utf8' }, (err) => {
          if (err) {
            logError('Write license to file failed:', filePath);
          }
        });
        logSuccess('Write license to file:', filePath);
      } else {
        logSuccess('File license check ok:', filePath);
      }
    },
  });
};
