const path = require('path')

module.exports = function getPublicPath () {

    // 根据环境判断 获取cdnPath
    let cdnPath = ''
    // console.log("process.env", process.env)
    console.log("cdnPath", cdnPath)
    const publicPath = cdnPath
        ? "//"+path.join(cdnPath, process.env.BUILD_GIT_GROUP, process.env.BUILD_GIT_PROJECT) + '/'
        : './';

    return publicPath
}
