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

const fs = require('fs');
const path = require('path');

// 标记结束点
let files = 0;
const walker = ({ root, dealFile, recursive = true }) => {
  if (!dealFile) {
    throw new Error('没有指定文件处理方法');
  }
  fs.readdir(root, 'utf8', (err, data) => {
    if (err) {
      console.log(`读取 ${root} 目录错误:`, err);
      return;
    }
    data.forEach((item) => {
      // 过滤隐藏目录
      if (item.startsWith('.')) {
        return;
      }
      const subPath = path.resolve(`${root}/${item}`);
      if (!item.includes('.') && recursive) {
        // console.log('目录:', item);
        return walker({ root: subPath, dealFile, recursive });
      }
      const filePath = subPath;
      files += 1;
      fs.readFile(filePath, 'utf8', (readErr, content) => {
        if (readErr) {
          console.error(`读取文件 ${filePath} 错误:`, readErr);
          return;
        }
        dealFile(content, filePath, files === 1);
        files -= 1;
      });
    });
  });
};

module.exports = {
  walker,
};
