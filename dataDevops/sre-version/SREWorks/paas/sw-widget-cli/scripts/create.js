process.on('unhandledRejection', err => {
  throw err;
});
const fs = require('fs-extra');
const path = require('path');
const chalk = require('chalk');
const os = require('os');
const execSync = require('child_process').execSync;

const gitignore = `# dependencies
/node_modules
/coverage
/build
/dist
.DS_Store
npm-debug.log*
debug.log*
yarn-debug.log*
yarn-error.log*
/.idea
`;
const isInGitRepository = () => {
  try {
    execSync('git rev-parse --is-inside-work-tree', { stdio: 'ignore' });
    return true;
  } catch (e) {
    return false;
  }
}
const tryGitInit = (appPath) => {
  let didInit = false;
  try {
    execSync('git --version', { stdio: 'ignore', cwd: appPath });
    if (isInGitRepository(appPath)) {
      return false;
    }

    execSync('git init', { stdio: 'ignore', cwd: appPath });
    didInit = true;

    execSync('git add -A', { stdio: 'ignore', cwd: appPath });
    execSync('git commit -m "Initial commit from Build Tool"', {
      stdio: 'ignore',
      cwd: appPath,
    });
    return true;
  } catch (e) {
    if (didInit) {
      try {
        // unlinkSync() doesn't work on directories.
        fs.removeSync(path.join(appPath, '.git'));
      } catch (removeErr) {
        // Ignore.
      }
    }
    return false;
  }
}
const create = (appName, appPath) => {
  const ownPath = path.dirname(require.resolve(path.join(__dirname, '..', 'package.json')));
  const appPackage = {};
  appPackage.name = appName;
  appPackage.version = '0.0.1';
  appPackage.privete = true;
  appPackage.dependencies = {
    "@ant-design/compatible": "^1.0.8",
    "@ant-design/icons": "^4.6.2",
    "@ant-design/pro-card": "^1.18.19",
    "@ant-design/pro-descriptions": "^1.2.0",
    "@ant-design/pro-form": "^1.3.0",
    "@ant-design/pro-layout": "^6.9.0",
    "@ant-design/pro-table": "^2.17.0",
    "antd": "^4.17.2",
    "axios": "^0.21.1",
    "bizcharts": "^4.1.15",
    "brace": "^0.11.1",
    "classnames": "2.3.1",
    "copy-to-clipboard": "3.3.1",
    "copy-webpack-plugin": "4.6.0",
    "create-react-ref": "0.1.0",
    "eventemitter3": "4.0.7",
    "events": "^3.3.0",
    "history": "4.7.2",
    "html2canvas": "^1.4.1",
    "i18next": "^10.6.0",
    "intl-messageformat": "^2.2.0",
    "jquery": "3.3.1",
    "js-yaml": "4.1.0",
    "jsonexport": "3.2.0",
    "localforage": "1.9.0",
    "lodash": "4.17.21",
    "lodash-ex": "1.0.8",
    "moment": "2.27.0",
    "moment-duration-format": "2.3.2",
    "mustache": "4.2.0",
    "nprogress": "0.2.0",
    "numeral": "^2.0.6",
    "promise": "8.1.0",
    "prop-types": "15.7.2",
    "qs": "6.10.1",
    "rc-color-picker": "1.2.6",
    "rc-queue-anim": "^1.2.2",
    "rc-scroll-anim": "2.7.4",
    "rc-tween-one": "^1.7.3",
    "react": "16.14.0",
    "react-ace": "7.0.5",
    "react-color": "^2.19.3",
    "react-dnd": "^16.0.0",
    "react-dnd-html5-backend": "^16.0.0",
    "react-dom": "16.14.0",
    "react-grid-layout": "1.2.4",
    "react-highlight": "^0.13.0",
    "react-infinite-scroller": "1.1.3",
    "react-jsx-parser": "1.26.3",
    "react-markdown": "^4.3.1",
    "react-onclickoutside": "6.7.1",
    "react-redux": "5.0.7",
    "react-router": "4.3.1",
    "react-router-dom": "4.3.1",
    "react-router-redux": "5.0.0-alpha.6",
    "react-sizeme": "^3.0.1",
    "react-sortablejs": "6.0.0",
    "redux": "3.7.2",
    "shortid": "2.2.16",
    "sortablejs": "1.13.0",
    "uuid": "^3.3.2"
  };

  appPackage.devDependencies = {
    "antd-theme-generator": "1.2.2",
    "autoprefixer": "^10.2.5",
    "babel-core": "6.26.3",
    "babel-eslint": "7.2.3",
    "babel-jest": "20.0.3",
    "babel-loader": "7.0.0",
    "babel-plugin-add-module-exports": "^0.2.1",
    "babel-plugin-import": "^1.6.2",
    "babel-plugin-transform-decorators-legacy": "^1.3.5",
    "babel-preset-es2015": "^6.24.1",
    "babel-preset-react": "^6.24.1",
    "babel-preset-react-app": "^3.1.0",
    "babel-preset-stage-0": "^6.24.1",
    "babel-runtime": "6.26.0",
    "case-sensitive-paths-webpack-plugin": "2.1.1",
    "chalk": "1.1.3",
    "css-loader": "0.28.4",
    "dotenv": "4.0.0",
    "dva": "2.4.1",
    "dva-loading": "^1.0.4",
    "eslint": "3.19.0",
    "eslint-config-react-app": "^1.0.5",
    "eslint-loader": "3.0.4",
    "eslint-plugin-flowtype": "4.7.0",
    "eslint-plugin-import": "2.20.0",
    "eslint-plugin-jsx-a11y": "6.2.0",
    "eslint-plugin-react": "7.20.0",
    "file-loader": "4.2.0",
    "fs-extra": "3.0.1",
    "happypack": "^5.0.0",
    "html-webpack-plugin": "^4.4.1",
    "html-webpack-tags-plugin": "^2.0.17",
    "jest": "20.0.4",
    "less": "^3.10.3",
    "less-loader": "^5.0.0",
    "less-vars-to-js": "^1.2.1",
    "mini-css-extract-plugin": "^1.6.2",
    "node-sass": "4.14.1",
    "object-assign": "4.1.1",
    "optimize-css-assets-webpack-plugin": "^5.0.3",
    "postcss-flexbugs-fixes": "3.0.0",
    "postcss-loader": "2.0.6",
    "react-dev-utils": "^7.0.5",
    "react-error-overlay": "^1.0.9",
    "react-highlight-words": "^0.17.0",
    "sass-loader": "^9.0.3",
    "shelljs": "^0.8.2",
    "simple-progress-webpack-plugin": "^1.1.2",
    "source-map-explorer": "1.4.0",
    "speed-measure-webpack-plugin": "^1.5.0",
    "style-loader": "1.3.0",
    "sw-precache-webpack-plugin": "0.11.4",
    "terser-webpack-plugin": "^4.2.3",
    "terser-webpack-plugin-legacy": "^1.2.3",
    "thread-loader": "^3.0.4",
    "url-loader": "4.1.1",
    "webpack": "4.44.2",
    "webpack-bundle-analyzer": "^3.9.0",
    "webpack-cli": "^3.3.12",
    "webpack-dev-server": "^3.11.3",
    "webpack-manifest-plugin": "2.2.0",
    "webpack-parallel-uglify-plugin": "^0.4",
    "whatwg-fetch": "2.0.3",
    "yargs": "^14.2.1"
  };
  // Setup the script rules
  appPackage.scripts = {
    start: 'cross-env-shell PORT=4000 components-new start',
    build: 'components-new build',
    update: 'components-new update -l',
    umd: "node --max-old-space-size=4096 scripts/buildUmd.js",
    check: 'node ./scripts/check',
    test: 'cross-env TEST_SETUP_FILES=./scripts/setupTests.js components-new test --env=jsdom',
    'fix-stylelint': 'stylelint ./src/**/*.{css,less} --fix',
    'fix-prettier': 'prettier --write ./src/**/*.{ts,js,tsx,jsx,less,css}',
    'fix-lint': 'npm run fix-stylelint && npm run fix-prettier',
  };
  appPackage.browserslist = ['>0.2%', 'not dead', 'not ie <= 11', 'not op_mini all'];
  appPackage.jest = {
    "collectCoverageFrom": [
      "src/**/*.{js,jsx}"
    ],
    "setupFiles": [
      "<rootDir>/config/polyfills.js"
    ],
    "testMatch": [
      "<rootDir>/src/**/__tests__/**/*.js?(x)",
      "<rootDir>/src/**/?(*.)(spec|test).js?(x)"
    ],
    "testEnvironment": "node",
    "testURL": "http://localhost",
    "transform": {
      "^.+\\.(js|jsx)$": "<rootDir>/node_modules/babel-jest",
      "^.+\\.css$": "<rootDir>/config/jest/cssTransform.js",
      "^(?!.*\\.(js|jsx|css|json)$)": "<rootDir>/config/jest/fileTransform.js"
    },
    "transformIgnorePatterns": [
      "[/\\\\]node_modules[/\\\\].+\\.(js|jsx)$"
    ],
    "moduleNameMapper": {
      "^react-native$": "react-native-web"
    },
    "moduleFileExtensions": [
      "web.js",
      "js",
      "json",
      "web.jsx",
      "jsx"
    ]
  }
  appPackage.babel = {
    "presets": [
      "react-app",
      "es2015",
      "react",
      "stage-0"
    ],
    "plugins": [
      "add-module-exports",
      "transform-decorators-legacy"

    ]
  }
  appPackage.eslintConfig = {
    "extends": "react-app",
    "rules": {
      "import/no-webpack-loader-syntax": "off",
      "no-undef": "off",
      "no-restricted-globals": "off"
    }
  }
  appPackage.homePage = "./"
  appPackage.resolutions = {
    "react": "16.14.0",
    "react-dom": "16.14.0"
  }
  fs.writeFileSync(path.join(appPath, 'package.json'), JSON.stringify(appPackage, null, 2) + os.EOL);

  fs.copySync(path.join(ownPath, 'template'), appPath);
  fs.copySync(path.join(ownPath, 'config'), path.join(appPath, 'config'));
  fs.copySync(path.join(ownPath, 'scripts'), path.join(appPath, 'scripts'));

  fs.copySync(path.join(ownPath, 'webpack.config.js'), path.join(appPath, 'webpack.config.js'));
  fs.copySync(path.join(ownPath, 'abc.json'), path.join(appPath, 'abc.json'));
  fs.writeFileSync(path.join(appPath, '.gitignore'), gitignore);
  if (tryGitInit(appPath)) {
    console.log(chalk.green("create success !"));
  }
}
const args = process.argv.slice(2);
const rootDir = process.cwd();
const projectName = args[0];

if (typeof projectName === 'undefined') {
  console.error('Please specify the project directory.');
  process.exit(1);
}

const projectPath = path.join(rootDir, projectName);

fs.ensureDirSync(projectPath);

create(projectName, path.join(rootDir, projectName));

