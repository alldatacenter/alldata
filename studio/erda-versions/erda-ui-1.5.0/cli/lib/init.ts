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
import { logInfo, logSuccess } from './util/log';
import dotenv from 'dotenv';
import execa from 'execa';
import { EOL } from 'os';
import { ALL_MODULES, isCwdInRoot } from './util/env';
import ora from 'ora';

export default async ({
  hostName,
  port,
  override,
  backendUrl,
  skipInstall,
}: {
  hostName?: string;
  port?: string;
  override?: boolean;
  backendUrl?: string;
  skipInstall?: boolean;
}) => {
  const currentDir = process.cwd();
  isCwdInRoot({ currentPath: currentDir, alert: true });

  logInfo('start local env initialization');
  if (!skipInstall) {
    let spinner = ora('installing lerna pnpm & commitizen...').start();
    const { stdout: msg } = await execa('npm', ['i', '-g', '--force', 'pnpm', 'commitizen', 'lerna']);
    logInfo(msg);
    logSuccess('installed pnpm, commitizen globally successfully😁');
    spinner.stop();

    spinner = ora('installing dependencies...').start();
    const { stdout: installMsg } = await execa('pnpm', ['i']);
    logInfo(installMsg);
    logSuccess('finish installing dependencies.');
    spinner.stop();
  }

  const envConfigPath = `${currentDir}/.env`;
  const { parsed: fullConfig } = dotenv.config({ path: envConfigPath });

  if (!fullConfig || override) {
    const newConfig: dotenv.DotenvParseOutput = {};
    newConfig.BACKEND_URL = backendUrl || 'https://erda.dev.terminus.io';
    newConfig.UC_BACKEND_URL = hostName || 'https://erda.dev.terminus.io';
    newConfig.MODULES = ALL_MODULES.join(',');
    newConfig.SCHEDULER_PORT = port || '3000';
    newConfig.SCHEDULER_URL = hostName || 'https://local.erda.dev.terminus.io';

    const newFullConfig: string[] = [];
    Object.keys(newConfig).forEach((k) => {
      newFullConfig.push(`${k}=${newConfig[k]}`);
    });
    fs.writeFileSync(envConfigPath, newFullConfig.join(EOL), 'utf8');
    logSuccess('update erda-ui/.env file');
  } else {
    logInfo('.env config is not empty, skip env config initialize step, or you can override with option --override');
  }
};
