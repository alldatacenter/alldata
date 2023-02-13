#!/usr/bin/env node
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

import path from 'path';
import { Command } from 'commander';
import inquirer from 'inquirer';
import build from '../lib/build';
import buildOnline from '../lib/build-online';
import addLicense from '../lib/add-license';
import checkLicense from '../lib/check-license';
import init from '../lib/init';
import initOnline from '../lib/init-online';
import i18n from '../lib/i18n';
import iconLocalize from '../lib/local-icon';
import generateService from '../lib/service-generator';
import checkBuildStatus from '../lib/check-build-status';
import fetchImageContent from '../lib/fetch-image-content';

const program = new Command();

inquirer.registerPrompt('directory', require('inquirer-select-directory'));

program.version(`erda-ui/cli ${require('../../package').version}`).usage('<command> [options]');

program
  .command('init')
  .description('install dependency & initialize .env config')
  .option('-p, --port <port>', 'set scheduler port')
  .option('-b, --backendUrl <backendUrl>', 'set backend(api) url')
  .option('-o, --override', 'ignore current .env file and override')
  .option('--online', 'is online execution')
  .option('--skipInstall', 'whether to skip the installation step')
  .action(async (options) => {
    const { online, ...restOptions } = options;
    if (online) {
      initOnline();
    } else {
      init(restOptions);
    }
  });

program
  .command('build')
  .description(
    'bundle files to public directory, pass true to launch a local full compilation build, pass image sha to launch a local partial compilation build based on image',
  )
  .option('--enableSourceMap', 'generate source map, default is false')
  .option('--online', 'whether is online build, default is false')
  .option('--release', 'whether need build docker image & push, default is false')
  .option('--registry', 'docker registry address which to push')
  .option('--skipBuild', 'skip build and only run docker build & push command. Only take effect when --release is set')
  .action(async (options) => {
    const { online, ...restOptions } = options;
    if (online) {
      buildOnline();
    } else {
      build(restOptions);
    }
  });

program
  .command('fetch-image')
  .description('pull image by image tag version, run this image locally and copy the content to local folder')
  .requiredOption(
    '-i, --image <image>',
    'image full tag name. e.g. registry.cn-hangzhou.aliyuncs.com/terminus/erda-ui:1.3-20210918-release',
  )
  .action(async (options) => {
    fetchImageContent(options);
  });

program
  .command('i18n')
  .description('translate words in work dir')
  .option('--switch', 'batch switch namespace')
  .option('--external', 'handle external module i18n')
  .action(async (options) => {
    const { switch: switchNs, external } = options;
    i18n({ isSwitchNs: switchNs, isExternal: external });
  });

program
  .command('generate-service [workDir]')
  .description('generate service by API swagger')
  .action(async (_workDir) => {
    const workDir = _workDir ? path.resolve(process.cwd(), _workDir) : process.cwd();
    generateService({ workDir });
  });

program
  .command('add-license')
  .option('-t, --fileType <file_type>', 'File type', 'js,ts,jsx,tsx')
  .description('add license header in files if not exist')
  .action(({ fileType }) => {
    addLicense({ fileType });
  });

program
  .command('check-license')
  .option('-t, --fileType <file_type>', 'File type', 'js,ts,jsx,tsx')
  .option('-d, --directory <directory>', 'work directory')
  .option('-f, --filter <filter>', 'filter log', 'warn')
  .description('check license header in files')
  .action(({ fileType, directory, filter }) => {
    checkLicense({ fileType, directory, filter });
  });

program
  .command('check-build-status')
  .description(
    'compare git commit sha with previous build, if match it will skip build and reuse last built files. Only used in pipeline build',
  )
  .action(async () => {
    checkBuildStatus();
  });

program
  .command('icon-localize')
  .description('download icon resource and store at local image')
  .action(async () => {
    iconLocalize();
  });

program.parse(process.argv);
