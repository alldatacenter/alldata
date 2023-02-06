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
import { clearPublic, getPublicDir } from './util/env';
import { logInfo, logSuccess, logWarn, logError } from './util/log';

const stopDockerContainer = async () => {
  await execa('docker', ['container', 'stop', 'erda-ui-for-build'], { stdio: 'inherit' });
  await execa('docker', ['rm', 'erda-ui-for-build'], { stdio: 'inherit' });
};

/**
 * restore built content from an existing image
 */
const restoreFromDockerImage = async (image: string) => {
  try {
    // check whether docker is running
    await execa('docker', ['ps']);
  } catch (error) {
    if (error.message.includes('Cannot connect to the Docker daemon')) {
      // if not start docker and exit program, because node can't know when docker would started completely
      logInfo('Starting Docker');
      try {
        await execa('open', ['--background', '-a', 'Docker']);
      } catch (e) {
        logError('Launch Docker failed! Please start Docker manually');
      }
      logWarn('Since partial build depends on docker, please rerun this command after Docker launch completed');
      process.exit(1);
    } else {
      logError('Docker maybe crashed', error);
      process.exit(1);
    }
  }
  // check whether erda-ui-for-build container exist
  const { stdout: containers } = await execa('docker', ['container', 'ls', '-al']);
  if (containers && containers.includes('erda-ui-for-build')) {
    // if exist stop & delete it first, otherwise it will cause docker conflict
    logInfo('erda-ui container already exist, stop & delete it before next step');
    await stopDockerContainer();
    logSuccess('stop & delete erda-ui container successfully');
  }

  // start docker container names erda-ui for image provided
  await execa('docker', ['run', '-d', '--name', 'erda-ui-for-build', image]);
  logSuccess('erda-ui docker container has been launched');

  // copy built content from container
  const publicDir = getPublicDir();
  await execa('docker', ['cp', 'erda-ui-for-build:/usr/src/app/public/.', `${publicDir}/`]);
  logSuccess('finished copy image content to local');
  // stop & delete container
  stopDockerContainer();
};

export default async ({ image }: { image: string }) => {
  await clearPublic();
  logInfo(`Start pull and restore content from ${image}`);
  await restoreFromDockerImage(image);
};
