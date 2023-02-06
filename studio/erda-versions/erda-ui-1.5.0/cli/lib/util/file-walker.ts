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

/* eslint-disable no-param-reassign */
import fs from 'fs';
import path from 'path';
import { logWarn, logError } from './log';

export const walker = ({
  root,
  dealFile,
  recursive = true,
  files = { level: 0 },
}: {
  root: string;
  dealFile: (content: string, filePath: string, isEnd: boolean) => void;
  recursive?: boolean;
  files?: { level: number };
}) => {
  if (!dealFile) {
    logError('[walker] Not assign file handler');
    process.exit();
  }
  if (root.includes('node_modules')) {
    logWarn('[walker] Skip node_modules');
    return;
  }
  // add withFileTypes incase file name like Dockerfile without extension name would be mistaken treat as folder
  fs.readdir(root, { encoding: 'utf8', withFileTypes: true }, (err, data) => {
    if (err) {
      logError(`[walker] Read directory ${root} error:`, err);
      return;
    }
    data.forEach((item) => {
      const { name: fileName } = item;
      // filter hidden directories
      if (fileName.startsWith('.')) {
        return;
      }
      const subPath = path.resolve(`${root}/${fileName}`);
      if (item.isDirectory() && recursive) {
        return walker({ root: subPath, dealFile, recursive, files });
      }
      const filePath = subPath;
      files.level += 1;
      fs.readFile(filePath, 'utf8', (readErr, content) => {
        if (readErr) {
          logError(`[walker] Read file ${filePath} error:`, readErr);
          return;
        }
        dealFile(content, filePath, files.level === 1);
        files.level -= 1;
      });
    });
  });
};
