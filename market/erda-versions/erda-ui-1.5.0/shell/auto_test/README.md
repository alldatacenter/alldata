# 自动化测试

## 使用指南

1、进入 shell 目录，更新依赖，下载 playwright

2、`shell/auto_test/auth` 目录下手动新建 `config.ts`, 内容如下，自行配置密码:

```
export default {
  url: 'https://erda.hkci.terminus.io/',
  roles: {
    Admin: {
      userName: 'admin',
      password: '',
    },
    Manager: {
      userName: 'erda-ui-team',
      password: '',
    },
    Dev: {
      userName: 'ui-dev',
      password: '',
    }
  }
}
```

3、执行 `npm run auto-test`，正常情况会自动登录，然后在 auto_test/auth 目录下生成对应角色名的登录信息文件，如果已有则会跳过。

4、恢复登录状态后开始执行测试用例，查看输出结果。错误时会在 tests/results 目录下生成截图文件

## 结构说明

auto_test 主目录

- playwright.config.ts 配置文件
- global-setup.ts 初始化逻辑
- util.ts 工具方法
- tests 用例目录
  - pages Page Model 目录
  - results 输出文件目录，包括截图等
  - dop dop 用例
    - xx.spec.ts 用例
    - xx.secret.ts 用例配套的需要保密的信息
  - cmp cmp 用例
- auth 权限目录
  - config.ts 权限配置
  - Admin.ts 角色登录信息文件
