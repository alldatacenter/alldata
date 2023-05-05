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

module.exports = {
  env: {
    browser: true,
    es6: true,
  },
  globals: {
    Cypress: 'readonly',
    cy: 'readonly',
  },
  extends: ['eslint-config-ali/typescript/react', 'prettier', 'prettier/@typescript-eslint', 'prettier/react'],
  parserOptions: {
    ecmaVersion: 2020, // specify the version of ECMAScript syntax you want to use: 2015 => (ES6)
    sourceType: 'module', // Allows for the use of imports
    ecmaFeatures: {
      jsx: true, // enable JSX
      impliedStrict: true, // enable global strict mode
    },
    project: ['./core/tsconfig.json', './shell/tsconfig.json'], // Specify it only for TypeScript files
    tsconfigRootDir: __dirname,
  },
  rules: {
    'no-param-reassign': ['error', { props: true, ignorePropertyModificationsFor: ['draft', 'state', 'acc'] }],
    'import/prefer-default-export': 'off',
    // 'react/jsx-filename-extension': [2, { 'extensions': ['.js', '.jsx', '.ts', '.tsx'] }],
    'react/prop-types': 'off',
    'arrow-body-style': 'off',
    'max-len': 'off',
    'no-nested-ternary': 'off',
    'react/no-multi-comp': 'off',
    'jsx-first-prop-new-line': 'off',
    'no-unused-vars': 'off',
    'import/no-named-as-default': 'off',
    'import/no-named-as-default-member': 'off',
    'react-hooks/rules-of-hooks': 'error',
    'no-console': 2,
    '@typescript-eslint/ban-ts-ignore': 'off',
    '@typescript-eslint/explicit-function-return-type': 'off',
    '@typescript-eslint/no-explicit-any': 'off',
    '@typescript-eslint/no-unused-vars': [1, { argsIgnorePattern: '^_', varsIgnorePattern: '^ignored?$' }],
    '@typescript-eslint/interface-name-prefix': 'off',
    indent: 0,
    'react/jsx-no-undef': 0,
    '@typescript-eslint/consistent-type-assertions': [0, { objectLiteralTypeAssertions: 'allow-as-parameter' }],
    'import/no-anonymous-default-export': 1,
  },
  overrides: [
    {
      files: ['**/*.js'], // none ts script like webpack config or legacy node scripts
      rules: {
        '@typescript-eslint/no-var-requires': 'off',
        '@typescript-eslint/no-require-imports': 'off',
        'no-console': 'off',
      },
    },
    {
      files: ['**/*/services/*.ts'],
      rules: {
        '@typescript-eslint/consistent-type-definitions': 'off',
      },
    },
  ],
};
