const fs = require('fs-extra');
const path = require('path');
const paths = require('./paths')
const getPackageJson = () => {
    var _packageJson = fs.readFileSync('./package.json')
    return JSON.parse(_packageJson)
}
const getDependencyVesion = (packagejson, dependency) => {
    if (!packagejson || !dependency) {
        return ''
    }
    var initVesrion = '';
    var dependencies = packagejson['dependencies']
    initVesrion = dependencies[dependency];
    if (initVesrion.startsWith('^') || initVesrion.startsWith("~")) {
        return initVesrion.slice(1);
    }
    return initVesrion
}

var packagejson = getPackageJson();
var antdPath = getDependencyVesion(packagejson, 'antd');
var momentPath = getDependencyVesion(packagejson, 'moment');
var reactPath = getDependencyVesion(packagejson, 'react');
var react_dom_path = getDependencyVesion(packagejson, 'react-dom');
var systemjsPath = getDependencyVesion(packagejson, 'systemjs');
var bizchartsPath = getDependencyVesion(packagejson, 'bizcharts');

const dependency_arr = [{
    from: paths.appNodeModules + '/antd/dist/antd.min.js',
    to: paths.appBuild + '/common_vendor/antd/' + antdPath + '/antd.min.js'
},
{
    from: paths.appNodeModules + '/react/umd/react.production.min.js',
    to: paths.appBuild + '/common_vendor/react/' + reactPath + '/react.production.min.js'
},
{
    from: paths.appNodeModules + '/react-dom/umd/react-dom.production.min.js',
    to: paths.appBuild + '/common_vendor/react-dom/' + react_dom_path + '/react-dom.production.min.js'
},
{
    from: paths.appNodeModules + '/moment/min/moment.min.js',
    to: paths.appBuild + '/common_vendor/moment/' + momentPath + '/moment.min.js'
},
{
    from: paths.appNodeModules + '/systemjs/dist/system.min.js',
    to: paths.appBuild + '/common_vendor/systemjs/' + systemjsPath + '/system.min.js'
}]
// {
//     from: paths.appNodeModules + '/bizcharts/umd/BizCharts.min.js',
//     to: paths.appBuild + '/common_vendor/bizcharts/' + bizchartsPath + '/BizCharts.min.js'
// }
const dependency_arr_pre = [{
    from: paths.appNodeModules + '/antd/dist/antd.min.js',
    to: paths.appPublic + '/common_vendor/antd/' + antdPath + '/antd.min.js'
},
{
    from: paths.appNodeModules + '/react/umd/react.production.min.js',
    to: paths.appPublic + '/common_vendor/react/' + reactPath + '/react.production.min.js'
},
{
    from: paths.appNodeModules + '/react-dom/umd/react-dom.production.min.js',
    to: paths.appPublic + '/common_vendor/react-dom/' + react_dom_path + '/react-dom.production.min.js'
},
{
    from: paths.appNodeModules + '/moment/min/moment.min.js',
    to: paths.appPublic + '/common_vendor/moment/' + momentPath + '/moment.min.js'
},
{
    from: paths.appNodeModules + '/systemjs/dist/system.min.js',
    to: paths.appPublic + '/common_vendor/systemjs/' + systemjsPath + '/system.min.js'
}]
console.log("init runtime paths successfull")
// 替换本地依赖版本
let indexHtml = fs.readFileSync(paths.appPublic + '/index.html', 'utf8')
indexHtml = indexHtml.replace(/moment\/.([\s\S]){1,}\/moment.min.js/gm, `moment/${momentPath}/moment.min.js`).replace(/antd\/([\s\S]){1,}\/antd.min.js/gm, `antd/${antdPath}/antd.min.js`).replace(/react\/.([\s\S]){1,}\/react.production.min.js/gm, `react/${reactPath}/react.production.min.js`).replace(/react-dom\/.([\s\S]){1,}\/react-dom.production.min.js/gm, `react-dom/${react_dom_path}/react-dom.production.min.js`).replace(/systemjs\/([\s\S]){1,}\/system.min.js/gm, `systemjs/${systemjsPath}/system.min.js`)
fs.writeFileSync(paths.appPublic + '/index.html', indexHtml, 'utf8')
module.exports = {
    antdPath,
    momentPath,
    reactPath,
    react_dom_path,
    systemjsPath,
    bizchartsPath,
    dependency_arr,
    dependency_arr_pre
}
