const path = require('path');
const { generateTheme } = require('antd-theme-generator');
const lessToJs = require('less-vars-to-js');
const fs = require('fs');

module.exports = {
    navyblue: lessToJs(fs.readFileSync(path.join(__dirname, '../src/themes/navyblue.less'), 'utf8')),
    dark: lessToJs(fs.readFileSync(path.join(__dirname, '../src/themes/dark.less'), 'utf8')),
    light: lessToJs(fs.readFileSync(path.join(__dirname, '../src/themes/light.less'), 'utf8'))
}