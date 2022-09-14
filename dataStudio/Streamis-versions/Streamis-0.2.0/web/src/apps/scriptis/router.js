export default [
  {
    path: '/home',
    name: 'Home',
    meta: {
      title: 'Scriptis',
      keepAlive: false, // 缓存导致页面有多个编辑器，广播事件会触发报错
      publicPage: true, // 权限公开
    },
    component: () =>
      import('./view/home/index.vue'),
  }
]
