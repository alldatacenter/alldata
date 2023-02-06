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

const cp = require('child_process');
const fs = require('fs');
const fg = require('fast-glob');

const caseFolder = '__tests__';
const { collectCoverageFrom } = require('../jest.config');

const whiteList = ['app/common/index.ts'];

const typeMap = {
  D: 'Delete',
  AD: 'Delete',
  A: 'Modify',
  M: 'Modify',
  MM: 'Modify',
  AM: 'Modify',
  '??': 'NotTracked',
};

/**
 * @description obtain the test case file corresponding to the component
 * @param file {string}
 * @returns {{exist: boolean, file: string}}
 */
const caseFileExist = (file) => {
  const caseFile = file.replace('app/common', 'app/common/__tests__').split('.')[0];
  const caseFileTs = `${caseFile}.test.ts`;
  const caseFileTsx = `${caseFile}.test.tsx`;
  const res = {
    exist: false,
    file: file,
  };
  if (fs.existsSync(caseFileTsx)) {
    res.exist = true;
    res.file = caseFileTsx;
  }
  if (fs.existsSync(caseFileTs)) {
    res.exist = true;
    res.file = caseFileTs;
  }
  return res;
};

/**
 * @description get commit file
 * @return {{Delete: string[], NotTracked: string[], Modify: string[]}}
 */
const getChangeFiles = () => {
  // const fileStr = cp.execSync('git diff HEAD --name-only').toString();
  const fileStr = cp.execSync('git status -s').toString();
  const files = fileStr.split('\n').filter((file) => !!file);
  const filesMap = {
    Delete: [],
    Modify: [],
    NotTracked: [],
  };
  const allFile = fg.sync(collectCoverageFrom);
  files.forEach((file) => {
    const [type, fileName] = file.trim().split(/\s+/);
    if (allFile.includes(fileName)) {
      process.stdout.write(`${type.padStart(5, ' ')}:${fileName}\n`);
      fileName && filesMap[typeMap[type]].push(fileName);
    }
  });
  return filesMap;
};

const checkTestCase = () => {
  const { Modify, Delete, NotTracked } = getChangeFiles();
  const changeFiles = [...NotTracked, ...Modify];
  const caseFiles = [];
  const commonFiles = changeFiles.filter(
    (file) => !file.includes(caseFolder) && file.includes('app/common') && !whiteList.includes(file),
  );
  const uncoverFiles = [];
  commonFiles.forEach((file) => {
    const { exist, file: caseFile } = caseFileExist(file);
    if (!exist) {
      uncoverFiles.push(`❌   ${file}`);
    } else {
      caseFiles.push(caseFile);
    }
  });
  if (caseFiles.length) {
    // execution time is too long
    // cp.execSync(`jest ${caseFiles.join(' ')}`, { encoding: 'utf8' });
  }
  if (uncoverFiles.length) {
    process.stderr.write('> THE FOLLOWING FILES UPDATED，BUT TEST CASE IS NOT EXIST <\n\n');
    process.stderr.write(uncoverFiles.join('\n'));
    process.stderr.write('\n\n');
    process.exit(1);
  }
};
module.exports = {
  checkTestCase,
};
