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
const { walker } = require('./file-walker');

const commentPrefix = '//';
const tsPrefix = '// @ts';
const splitFlag = '// ---';
const sourceDirPath = path.resolve(__dirname, '../snippets');
const targetDirPath = path.resolve(__dirname, '../.vscode');

function text2snippet(text) {
  const comments = [];
  const body = [];
  const snippet = {};
  const lines = text.split('\n');
  for (let i = 0; i < lines.length; i++) {
    const curLine = lines[i];
    if (curLine.startsWith(commentPrefix)) {
      if (!curLine.startsWith(tsPrefix)) {
        comments.push(curLine);
      }
    } else if (curLine.length) {
      body.push(curLine);
    }
  }
  let prev;
  let key;
  for (let line of comments) {
    line = line.slice(commentPrefix.length);
    const m = /\s*@\w+\s*/.exec(line);
    if (m) {
      prev = m[0];
      key = prev.trim().slice(1);
      snippet[key] = (snippet[key] || '') + line.slice(prev.length);
    } else if (prev) {
      snippet[key] += `\n${line.replace(/^\s+/, '')}`;
    }
  }
  snippet.body = body;
  return snippet;
}

const dealFile = (content, filePath, isEnd) => {
  const { name } = path.parse(filePath);
  const snippetPath = path.resolve(targetDirPath, `${name}.code-snippets`);
  const result = {};
  content.split(splitFlag).forEach((part) => {
    const sni = text2snippet(part);
    result[sni.prefix] = sni;
  });

  if (!fs.existsSync(targetDirPath)) {
    console.log(`目录 ${targetDirPath} 不存在，开始创建`);
    fs.mkdirSync(targetDirPath, '0777');
  }

  fs.writeFile(snippetPath, JSON.stringify(result, null, 2), 'utf8', (writeErr) => {
    if (writeErr) return console.error(`写入文件：${snippetPath}错误`, writeErr);
  });

  if (isEnd) {
    console.log('snippets 生成完毕');
  }
};

walker({
  root: sourceDirPath,
  dealFile,
});
