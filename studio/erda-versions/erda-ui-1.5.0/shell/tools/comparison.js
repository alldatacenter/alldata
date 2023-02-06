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

/**
 * Compare the specified branch on Github with zh.json locally
 * @param branch specified branch
 */
const fs = require('fs');
const path = require('path');
const http = require('superagent');

const branch = process.argv.splice(2)[0] || 'master';

http
  .get(`https://raw.githubusercontent.com/erda-project/erda-ui/${branch}/shell/app/locales/zh.json`)
  .then((response) => {
    if (response.status == 200 && response.text) {
      const textBefore = JSON.parse(response.text);
      const text = JSON.parse(fs.readFileSync(path.resolve('../app/locales/zh.json'), 'utf-8'));

      let differences = '中文;nameSpace;上次翻译;这次翻译\n';

      Object.keys(text).forEach((i) => {
        Object.keys(text[i]).forEach((key) => {
          const preText = Object.keys(textBefore[i]).find((preKey) => textBefore[i][preKey] === text[i][key]);
          if (preText) {
            preText !== key && (differences += `${text[i][key]};${i};${preText};${key}\n`);
          } else {
            preText !== key && (differences += `${text[i][key]};${i};;${key}\n`);
          }
        });
      });

      console.log(differences, 'csv表格已生成');

      fs.writeFileSync('./平台国际化文案.csv', differences);
    }
  })
  .catch((err) => {
    console.log('获取文件失败：', err);
    return false;
  });
