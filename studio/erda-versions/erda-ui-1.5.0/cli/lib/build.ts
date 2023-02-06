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

import inquirer from 'inquirer';
import execa, { ExecaChildProcess } from 'execa';
import notifier from 'node-notifier';
import { logInfo, logSuccess, logError } from './util/log';
import { getModuleList, checkIsRoot, getShellDir, defaultRegistry, clearPublic, killPidTree } from './util/env';
import chalk from 'chalk';
import generateVersion from './util/gen-version';
import localIcon from './local-icon';
import dayjs from 'dayjs';
import path from 'path';
import { getGitShortSha, getBranch } from './util/git-commands';

const currentDir = process.cwd();
const externalDir = path.resolve(currentDir, '../erda-ui-enterprise');

const dirCollection: { [k: string]: string } = {
  core: `${currentDir}/core`,
  shell: `${currentDir}/shell`,
  market: `${currentDir}/modules/market`,
  uc: `${currentDir}/modules/uc`,
  fdp: '../erda-ui-enterprise/fdp',
  admin: '../erda-ui-enterprise/admin',
};

const dirMap = new Map(Object.entries(dirCollection));

const alertMessage = (outputModules: string, branch: string, externalBranch: string, release?: boolean) => `
/**************************${chalk.red('Warning Before Build')}************************************/
Here are the MODULES【${chalk.bgBlue(outputModules)}】which detected in .env file.
If any module missed or should exclude, please manually adjust MODULES config in .env and run again.

If you ${chalk.yellow("don't")} want to make a full bundle(just need ${chalk.yellow('partial built')}),
you can run erda-ui ${chalk.green('fetch-image')} command to load previous image content
and then make partial built based it

${chalk.yellow('Current Branch')}:
erda-ui: ${chalk.yellow(branch)}
erda-ui-enterprise: ${chalk.yellow(externalBranch)}

${chalk.yellow('Please make sure:')}
1. erda-ui-enterprise directory should be placed at same level as erda-ui, and ${chalk.yellow("don't")} rename it
2. your code is updated ${chalk.red('both')} erda-ui & erda-ui-enterprise repository
3. switch to target branch ${chalk.red('both')} erda-ui & erda-ui-enterprise repository
4. all ${chalk.bgRed('node_modules')} dependencies are updated
${release ? `5. since ${chalk.red('--release')} is passed, Docker should keep running & docker logged in` : ''}

Press Enter to continue.
/**********************************************************************************/
`;

const localBuildAlert = async (requireRelease?: boolean) => {
  const moduleList = getModuleList();
  const outputModules = moduleList.join(',');

  const branch = await getBranch(currentDir);
  const externalBranch = await getBranch(externalDir);

  const answer = await inquirer.prompt([
    {
      type: 'confirm',
      name: 'coveredAllModules',
      message: alertMessage(outputModules, branch, externalBranch, requireRelease),
      default: true,
    },
  ]);
  if (!answer.coveredAllModules) {
    process.exit(1);
  }
};

const buildModules = async (enableSourceMap: boolean, rebuildList: string[]) => {
  const pList: ExecaChildProcess[] = [];
  rebuildList.forEach((moduleName) => {
    const moduleDir = dirMap.get(moduleName);
    const buildPromise = execa('npm', ['run', 'build'], {
      cwd: moduleDir,
      env: {
        ...process.env,
        enableSourceMap: enableSourceMap.toString(),
      },
      stdio: 'inherit',
    });
    pList.push(buildPromise);
  });

  try {
    await Promise.all(pList);
  } catch (_error) {
    logError('build failed, will cancel all building processes');
    await killPidTree();
    process.exit(1);
  }
  logSuccess('build successfully 😁!');
};

const releaseImage = async (registry?: string) => {
  const date = dayjs().format('YYYYMMDD');
  const sha = await getGitShortSha();
  const pJson = require(`${getShellDir()}/package.json`);
  const version = pJson.version.slice(0, -2);
  const tag = `${version}-${date}-${sha}`; // 3.20-2020520-182737976

  const image = `${registry ?? defaultRegistry}:${tag}`;
  await execa('docker', ['build', '-f', 'Dockerfile', '--platform', 'linux/arm64/v8', '-t', image, '.'], {
    stdio: 'inherit',
    cwd: currentDir,
  });
  logSuccess('build image successfully');

  logInfo(`start pushing image to registry:【${registry ?? defaultRegistry}】`);
  await execa('docker', ['push', image], { stdio: 'inherit' });

  notifier.notify({
    title: 'Success',
    message: `🎉 Image 【${tag}】 pushed to registry success 🎉`,
  });
  logSuccess(`push success: 【${image}】`);
};

const copyExternalCode = async (moduleList: string[]) => {
  if (moduleList.includes('shell')) {
    const answer = await inquirer.prompt([
      {
        type: 'confirm',
        name: 'shouldCopy',
        message: 'Do you need copy external code for shell?',
        default: true,
      },
    ]);
    if (answer.shouldCopy) {
      await execa('npm', ['run', 'extra-logic'], { cwd: getShellDir(), stdio: 'inherit' });
    }
  }
};

const getBuildList = async () => {
  let rebuildList = getModuleList();
  const answer = await inquirer.prompt([
    {
      type: 'checkbox',
      name: 'buildModules',
      message: 'please choose modules to build',
      default: ['all'],
      choices: [{ value: 'all' }, ...rebuildList],
    },
  ]);

  if (!answer.buildModules.length) {
    logError('no module selected to build, exit program');
    process.exit(1);
  }
  if (!answer.buildModules.includes('all')) {
    rebuildList = answer.buildModules;
  } else {
    // clear public output
    await clearPublic();
  }
  return rebuildList;
};

export default async (options: {
  enableSourceMap?: boolean;
  release?: boolean;
  registry?: string;
  skipBuild?: boolean;
}) => {
  try {
    const { enableSourceMap = false, release, registry, skipBuild } = options;

    // check if cwd erda ui root
    checkIsRoot();
    // prompt alert before build
    await localBuildAlert(!!release);

    if (skipBuild && release) {
      await releaseImage(registry);
      return;
    }

    // get required build list
    const rebuildList = await getBuildList();

    // reminder to copy msp code from external
    await copyExternalCode(rebuildList);

    await buildModules(enableSourceMap, rebuildList);

    generateVersion();

    if (rebuildList.includes('shell')) {
      localIcon();
    }

    logSuccess('build process is done!');

    if (release) {
      await releaseImage(registry);
    }
  } catch (error) {
    logError('build exit with error:', error.message);
    process.exit(1);
  }
};
