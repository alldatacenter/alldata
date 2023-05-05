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

const { forEach } = require('lodash');
const tsconfig = require('./tsconfig.json');
const moduleNameMapper = require('tsconfig-paths-jest')(tsconfig);

const moduleMapper = {};
const excludeModules = ['interface', 'common', 'layout', 'user', 'configForm', 'charts', 'application'];
forEach(moduleNameMapper, (t, k) => {
  if (!excludeModules.includes(k)) {
    moduleMapper[`^${k}`] = t;
  }
});

module.exports = {
  verbose: true,
  automock: false,
  clearMocks: true,
  coverageDirectory: 'coverage',
  coveragePathIgnorePatterns: ['/node_modules/'],
  collectCoverage: false,
  collectCoverageFrom: [
    'app/common/**/*.{js,jsx,ts,tsx}',
    '!app/common/**/*.d.ts',
    '!app/common/stores/*.{js,jsx,ts,tsx}',
    '!app/common/services/*.{js,jsx,ts,tsx}',
    '!app/common/utils/style-constants.ts',
  ],
  globals: {
    'ts-jest': {
      diagnostics: false,
      isolatedModules: true,
    },
  },
  moduleFileExtensions: ['tsx', 'ts', 'jsx', 'js'],
  transform: {
    '^.+\\.(t|j)sx?$': 'ts-jest',
    '^.+\\js$': 'babel-jest',
  },
  moduleNameMapper: {
    '\\.(jpg|ico|jpeg|png|gif|eot|otf|webp|svg|ttf|woff|woff2|mp4|webm|wav|mp3|m4a|aac|oga)$': 'identity-obj-proxy',
    ...moduleMapper,
    '^core/agent$': '<rootDir>/../core/src/agent.ts',
    '^core/(.*)': '<rootDir>/../core/src/$1',
    '^cube$': '<rootDir>/../core/src/cube.ts',
    '^i18next$': '<rootDir>/../core/node_modules/i18next',
    '^antd$': '<rootDir>/../core/node_modules/antd/lib/index.js',
    '^antd/dist/.*': '<rootDir>/node_modules/identity-obj-proxy/src/index.js',
    i18n: '<rootDir>/app/i18n.ts',
    'app/constants': '<rootDir>/app/constants.ts',
    'app/user/stores(.*)': '<rootDir>/app/user/stores/$1',
    'app/layout/stores(.*)': '<rootDir>/app/layout/stores/$1',
    'core/cube': '<rootDir>/core/cube.ts',
    'app/global-space': '<rootDir>/app/global-space.ts',
    '^agent$': '<rootDir>/app/agent.js',
    '^common$': '<rootDir>/app/common/index.ts',
    '^layout(.*)': '<rootDir>/app/layout/$1',
    '^common/utils(.*)': '<rootDir>/app/common/utils/$1',
    'common/stores(.*)': '<rootDir>/app/common/stores/$1',
    '^configForm(.*)': '<rootDir>/app/configForm/$1',
    '^charts$': '<rootDir>/app/charts/index.js',
    // mock for @erda-ui/dashboard-configuratort iconfont
    'iconfont.js$': 'identity-obj-proxy',
    '\\.(css|less|scss)$': 'identity-obj-proxy',
  },
  preset: 'ts-jest/presets/js-with-ts',
  setupFiles: ['<rootDir>/test/setupJest.ts', '<rootDir>/test/setupEnzyme.ts', 'jest-canvas-mock'],
  setupFilesAfterEnv: ['./node_modules/jest-enzyme/lib/index.js'],
  testEnvironmentOptions: {
    enzymeAdapter: 'react16',
  },
  transformIgnorePatterns: [
    // 'node_modules/(?!@erda-ui/dashboard-configurator/.*)',
    // dashboardRealPath,
  ],
  testMatch: ['**/__tests__/**/*.test.+(tsx|ts|jsx|js)'],
  testPathIgnorePatterns: [
    // '/node_modules/',
  ],
  testURL: 'http://localhost',
};
