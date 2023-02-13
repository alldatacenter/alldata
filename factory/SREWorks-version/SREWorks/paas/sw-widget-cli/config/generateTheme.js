const path = require('path');
const { generateTheme } = require('antd-theme-generator');
const lessToJs = require('less-vars-to-js');
const fs = require('fs');

let options = {
  antDir: path.join(__dirname, '../node_modules/antd'),
  stylesDir: path.join(__dirname, '../src'),
  varFile: path.join(__dirname, '../src/themes/index.less'),
  mainLessFile: path.join(__dirname, '../src/index.less'),
  outputFilePath: path.join(__dirname, '../build/color.less'), // if provided, file will be created with generated less/styles
  // themeVariables是需要修改的antd变量值
  themeVariables: [
    '@primary-color',
    '@link-color',
    '@success-color',
    '@warning-color',
    '@error-color',
    '@font-size-base',
    '@border-radius-base',
    '@btn-height-base',
    '@box-shadow-base',
    '@layout-body-background',
    '@body-background',
    '@background-color-base',
    '@layout-sider-background',
    '@component-background',
    '@input-bg',
    '@input-height-base',
    '@btn-default-bg',
    '@btn-default-border',
    '@border-color-base',
    '@border-color-split',
    '@heading-color',
    '@text-color',
    '@text-color-secondary',
    '@table-selected-row-bg',
    '@table-expanded-row-bg',
    '@table-header-bg',
    '@table-row-hover-bg',
    '@table-selected-row-color',
    '@layout-trigger-color',
    '@layout-trigger-background',
    '@alert-message-color',
    '@item-hover-bg',
    '@item-active-bg',
    '@disabled-color',
    '@tag-default-bg',
    '@popover-bg',
    '@wait-icon-color',
    '@background-color-light',
    '@alert-info-bg-color',
    '@scrollbarBackground',
    '@scrollbarBackground2',
    '@scrollbarBorder',
    '@header-bg-color',
    '@nprogress-color'
  ],
};
// 本地开发生成样式文件
if(process.env.DEPLOY_ENV === 'local'||process.env.DEPLOY_ENV === 'mocks') {
  options.outputFilePath = path.join(__dirname, '../public/color.less')
}

// 设置主题，生成color.less文件，把所有的变量相关样式合成到这个文件下
generateTheme(options);

module.exports = {
    navyblue: lessToJs(fs.readFileSync(path.join(__dirname, '../src/themes/navyblue.less'), 'utf8')),
    dark: lessToJs(fs.readFileSync(path.join(__dirname, '../src/themes/dark.less'), 'utf8')),
    light: lessToJs(fs.readFileSync(path.join(__dirname, '../src/themes/light.less'), 'utf8'))
}