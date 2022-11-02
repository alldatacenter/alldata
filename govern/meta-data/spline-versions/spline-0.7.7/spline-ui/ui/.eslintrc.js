/*
 * Copyright 2021 ABSA Group Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

module.exports = {
    'env': {
        'browser': true,
        'node': true,
        'es6': true,
        'es2017': true
    },
    'overrides': [
        {
            'files': [
                '*.spec.ts',
                '**/testing/**/*.ts'
            ]
        },
        {
            'files': ['*.ts'],
            'parser': '@typescript-eslint/parser',
            'parserOptions': {
                'ecmaVersion': 2020,
                'sourceType': 'module',
                'tsconfigRootDir': __dirname,
                'project': './tsconfig.json'
            },
            'plugins': [
                '@typescript-eslint',
                '@typescript-eslint/tslint',
                '@angular-eslint',
                'import',
            ],
            'extends': [
                'eslint:recommended',
                'plugin:@typescript-eslint/eslint-recommended',
                'plugin:@typescript-eslint/recommended',
                'plugin:@typescript-eslint/recommended-requiring-type-checking',
            ],
            'rules': {
                'import/newline-after-import': ['error', {'count': 2}],
                '@typescript-eslint/no-use-before-define': ['warn', {functions: false, classes: false}],
                '@typescript-eslint/no-empty-function': 0,
                '@typescript-eslint/no-var-requires': 0,
                '@typescript-eslint/no-explicit-any': 0,
                '@typescript-eslint/no-unnecessary-type-assertion': 1,
                '@typescript-eslint/unbound-method': [
                    'error',
                    {
                        'ignoreStatic': true
                    }
                ],
                '@typescript-eslint/no-non-null-assertion': 0,
                '@typescript-eslint/tslint/config': [
                    'error',
                    {
                        'lintFile': __dirname + '/tslint.json'
                    }
                ],
                '@typescript-eslint/no-namespace': 0,
                '@typescript-eslint/explicit-function-return-type': 0,
                'no-inner-declarations': 0,
                'max-lines': 0,
                'brace-style': [
                    'error',
                    'stroustrup'
                ],
                'curly': [
                    'error',
                    'all'
                ],
                'semi': ['error', 'never'],
                'indent': [
                    'error',
                    4,
                    {
                        'SwitchCase': 1,
                        'FunctionDeclaration': {
                            'parameters': 'first'
                        }
                    }
                ],
                '@typescript-eslint/array-type': 'off',
                '@typescript-eslint/member-delimiter-style': [
                    'error', {
                        'multiline': {
                            'delimiter': 'none',
                        },
                        'overrides': {
                            'interface': {
                                'multiline': {
                                    'delimiter': 'none',
                                }
                            }
                        }
                    }],
                'arrow-parens': 'off',
                '@angular-eslint/component-class-suffix': 'error',
                '@angular-eslint/contextual-lifecycle': 'error',
                '@angular-eslint/directive-class-suffix': 'error',
                '@angular-eslint/directive-selector': [
                    'error',
                    {
                        'type': 'attribute',
                        'prefix': ['spline', 'app'],
                        'style': 'camelCase'
                    }
                ],
                '@angular-eslint/component-selector': [
                    'error',
                    {
                        'type': 'element',
                        'prefix': ['spline', 'app'],
                        'style': 'kebab-case'
                    }
                ],
                'no-restricted-imports': [
                    'error',
                    {
                        'paths': [
                            {
                                'name': 'rxjs/Rx',
                                'message': 'Please import directly from \'rxjs\' instead'
                            }
                        ]
                    }
                ],
                '@typescript-eslint/interface-name-prefix': 'off',
                'max-classes-per-file': 'off',
                'max-len': [
                    'error',
                    {
                        'code': 140
                    }
                ],
                '@typescript-eslint/explicit-member-accessibility': [
                    'error',
                    {
                        'accessibility': 'no-public'
                    }
                ],
                '@typescript-eslint/member-ordering': 'warn',
                'no-multiple-empty-lines': 'off',
                'no-empty': 'off',
                '@typescript-eslint/no-inferrable-types': [
                    'error',
                    {
                        'ignoreParameters': true
                    }
                ],
                'no-fallthrough': 'error',
                'quote-props': [
                    'error',
                    'as-needed'
                ],
                'sort-keys': 'off',
                'quotes': [
                    'error',
                    'single'
                ],
                'comma-dangle': ['error', 'only-multiline'],
                '@angular-eslint/no-conflicting-lifecycle': 'error',
                '@angular-eslint/no-host-metadata-property': 'error',
                '@angular-eslint/no-input-rename': 'error',
                '@angular-eslint/no-inputs-metadata-property': 'error',
                '@angular-eslint/no-output-native': 'error',
                '@angular-eslint/no-output-on-prefix': 'error',
                '@angular-eslint/no-output-rename': 'error',
                '@angular-eslint/no-outputs-metadata-property': 'error',
                '@angular-eslint/use-lifecycle-interface': 'warn',
                '@angular-eslint/use-pipe-transform-interface': 'error',
                '@typescript-eslint/no-unsafe-return': 'warn',
                '@typescript-eslint/no-unsafe-member-access': 'warn',
                '@typescript-eslint/no-unsafe-assignment': 'off',
                '@typescript-eslint/no-unsafe-call': 'warn',

            }
        },
        {
            'files': [
                '*.component.html'
            ],
            'parser': '@angular-eslint/template-parser',
            'plugins': [
                '@angular-eslint/template'
            ],
            'rules': {
                '@angular-eslint/template/no-negated-async': 'error'
            }
        }
    ]
};
