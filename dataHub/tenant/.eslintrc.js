module.exports = {
  extends: [require.resolve('@umijs/fabric/dist/eslint')],
  parserOptions: {
    tsconfigRootDir: __dirname,
    project: './tsconfig.json',
    createDefaultProgram: false,
  },
  globals: {
    ANT_DESIGN_PRO_ONLY_DO_NOT_USE_IN_YOUR_PRODUCTION: true,
    page: true,
    REACT_APP_ENV: true,
    API_GRAPHQL_URL: true,
  },
  rules: {
    'import/order': 'error',
    'no-restricted-syntax': 0,
  },
}
