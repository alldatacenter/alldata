/*
 * Copyright (c) 2021 Terminus, Inc.
 *
 * This program is free software: you can use, redistribute, and/or modify
 * it under the terms of the GNU Affero General Public License, version 3
 * or later ("AGPL"), as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

import execa from 'execa';

export const getGitShortSha = async (cwd?: string) => {
  const { stdout } = await execa('git', ['rev-parse', '--short', 'HEAD'], { cwd: cwd ?? process.cwd() });
  return stdout;
};

export const getGitDiffFiles = async (prevSha: string, headSha: string, cwd?: string) => {
  const { stdout } = await execa('git', ['diff', '--name-only', `${prevSha}`, `${headSha}`], {
    cwd: cwd ?? process.cwd(),
  });
  return stdout;
};

export const getBranch = async (cwd?: string) => {
  const { stdout } = await execa('git', ['rev-parse', '--abbrev-ref', 'HEAD'], {
    cwd: cwd ?? process.cwd(),
  });
  return stdout;
};
