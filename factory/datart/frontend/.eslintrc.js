const fs = require('fs');
const path = require('path');

const prettierOptions = JSON.parse(
  fs.readFileSync(path.resolve(__dirname, '.prettierrc'), 'utf8'),
);

module.exports = {
  extends: ['react-app', 'prettier'],
  plugins: ['prettier', 'jsdoc'],
  rules: {
    'prettier/prettier': ['error', prettierOptions],
    'no-restricted-imports': [
      'error',
      {
        paths: [
          {
            name: 'lodash',
            message: 'suggest import xxx from `lodash/xxx`',
          },
          {
            name: 'uuid',
            message: 'suggest import xxx from `uuid/dist/xxx`',
          },
        ],
      },
    ],
  },
  parserOptions: {
    ecmaVersion: 2018,
    sourceType: 'module',
    ecmaFeatures: {
      jsx: true,
    },
    project: './tsconfig.json',
    tsconfigRootDir: __dirname,
  },
  overrides: [
    {
      files: ['**/*.ts?(x)'],
      rules: { 'prettier/prettier': ['warn', prettierOptions] },
    },
  ],
  ignorePatterns: ['public/*'],
};
