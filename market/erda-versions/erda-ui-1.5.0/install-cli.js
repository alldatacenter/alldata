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

const pkg = require('./package.json');
const { exec } = require('child_process');
const { promisify } = require('util');

const asyncExec = promisify(exec);

const installCli = async () => {
  const currentVersion = pkg.version;

  let cliVersion = 'latest';
  if (currentVersion === '1.0.0') {
    cliVersion = '1.0.4';
  } else if (['1.1.0', '1.2.0'].includes(currentVersion)) {
    cliVersion = '1.0.16';
  }
  await asyncExec(`npm i -g @erda-ui/cli@${cliVersion}`);
  console.log(`install cli version ${cliVersion} successfully`);
};

installCli();
