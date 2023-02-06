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
 *  获取特殊标注的变量名 {{test}} => test
 *
 * @param {string} sourceStr
 * @returns
 */
export const getVariableStr = (sourceStr: string) => {
  const matchTarget = sourceStr.match(/\{\{.*?\}\}/);
  return matchTarget ? matchTarget[0] : undefined;
};
