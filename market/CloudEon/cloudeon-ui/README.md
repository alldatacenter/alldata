# 前端项目

# 使用到的技术
1. [Ant Design Pro](https://pro.ant.design)
2. [Umi3](https://v3.umijs.org/)
3. [typescript](https://www.typescriptlang.org/zh/docs/)
4. [React](https://zh-hans.reactjs.org/)
5. [node](https://github.com/nodejs/node)，建议使用16.13.1版本 

# 启动项目
1. 第一次启动，如果有node_modules和src/.umi文件夹，建议删除node_modules和src/.umi文件夹
2. 第一次启动，安装tyarn：
```bash
npm install yarn tyarn -g
```
3. 第一次启动，用tyarn安装依赖：
```bash
tyarn
```
4. 问后端接口地址，本地环境启动修改接口地址
   在config/apiConfig.ts文件修改：
   ```bash
    devHost = "XX.XX.XX.XX:7700" // 修改成要代理的地址
   ```
5. 启动项目的命令
```bash
npm start
```

6. 生产环境打包
```bash
npm run build
```

