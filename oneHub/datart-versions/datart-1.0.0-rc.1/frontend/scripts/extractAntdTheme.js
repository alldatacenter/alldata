const path = require('path');
const defaultTheme = require(path.join(
  __dirname,
  '../node_modules/antd/dist/default-theme.js',
));
const { generateTheme } = require('antd-theme-generator');

const themeVariables = [];

for (let key in defaultTheme) {
  themeVariables.push('@' + key);
}

console.log('\r\nStart generating ant design theme.less file\r\n');

generateTheme({
  antDir: path.join(__dirname, '../node_modules/antd'), //node_modules中antd的路径
  stylesDir: path.join(__dirname, '../src/styles/antd'), //styles对应的目录路径
  varFile: path.join(__dirname, '../src/styles/antd/variables.less'), //less变量的入口文件
  themeVariables: themeVariables, //您要动态更改的变量列表
  outputFilePath: path.join(__dirname, '../public/antd/theme.less'), //生成的color.less文件的位置
  customColorRegexArray: [/^color\(.*\)$/],
});
