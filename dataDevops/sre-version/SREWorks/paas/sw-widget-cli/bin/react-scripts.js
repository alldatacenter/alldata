#!/usr/bin/env node
/**
 * Copyright (c) 2015-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

const spawn = require("cross-spawn");
const args = process.argv.slice(2);

const scriptIndex = args.findIndex(x => x === 'build' || x === 'create' || x === 'update' || x === 'start' || x === 'test');
const script = scriptIndex === -1 ? args[0] : args[scriptIndex];
const nodeArgs = scriptIndex > 0 ? args.slice(0, scriptIndex) : [];

switch (script) {
  case 'build':
  case 'create':
  case 'update':
  case 'start':
  case 'test': {
    const result = spawn.sync(
      'node',
      nodeArgs.concat(require.resolve('../scripts/' + script)).concat(args.slice(scriptIndex + 1)),
      { stdio: 'inherit' },
    );
    if (result.signal) {
      if (result.signal === 'SIGKILL') {
        console.log( // eslint-disable-line
          'The build failed because the process exited too early. ' +
            'This probably means the system ran out of memory or someone called ' +
            '`kill -9` on the process.',
        );
      } else if (result.signal === 'SIGTERM') {
        console.log( // eslint-disable-line
          'The build failed because the process exited too early. ' +
            'Someone might have called `kill` or `killall`, or the system could ' +
            'be shutting down.',
        );
      }
      process.exit(1);
    }
    process.exit(result.status);
    break;
  }
  default:
    console.log('Unknown script "' + script + '".'); // eslint-disable-line
    console.log('Perhaps you need to update react-scripts?'); // eslint-disable-line
    console.log( // eslint-disable-line
      'See: https://github.com/facebookincubator/create-react-app/blob/master/packages/react-scripts/template/README.md#updating-to-new-releases', // eslint-disable-line
    );
    break;
}
