// 适配 Nginx 反向代理
const baseUrl = process.env.VUE_APP_BASE_API === '/' ? '' : process.env.VUE_APP_BASE_API
const api = {
  state: {
    // 部署包上传
    deployUploadApi: baseUrl + '/system/api/deploy/upload',
    // SQL脚本上传
    databaseUploadApi: baseUrl + '/system/api/database/upload',
    // 图片上传
    imagesUploadApi: baseUrl + '/system/api/localStorage/pictures',
    // 修改头像
    updateAvatarApi: baseUrl + '/system/api/users/updateAvatar',
    // 上传文件到七牛云
    qiNiuUploadApi: baseUrl + '/system/api/qiNiuContent',
    // Sql 监控
    sqlApi: baseUrl + '/druid/index.html',
    // swagger
    swaggerApi: baseUrl + '/doc.html',
    // 文件上传
    fileUploadApi: baseUrl + '/system/api/localStorage',
    // baseUrl，
    baseApi: baseUrl
  }
}

export default api
