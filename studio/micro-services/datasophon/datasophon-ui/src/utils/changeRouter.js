/*
 * @Author: mjzhu
 * @describe: 
 * @Date: 2022-06-21 19:51:16
 * @LastEditTime: 2022-07-27 17:06:53
 * @FilePath: \ddh-ui\src\utils\changeRouter.js
 */
import store from '@/store'

function changeRouter(resData, clusterId) {
  let menuData = JSON.parse(localStorage.getItem('menuData'))
  menuData.forEach((item) => {
    if (item.path === "service-manage") {
      item.children = [];
      resData.map((serviceItem) => {
        item.children.push({
          name: serviceItem.serviceName,
          label: serviceItem.label,
          meta: {
            notAlive: true,
            isCluster: 'isCluster',
            params: {
              serviceId: serviceItem.id
            },
            obj: serviceItem,
            authority: {
              permission: "*",
            },
            permission: [{
              permission: "*",
            },
            {
              permission: "*",
            },
            ],
          },
          fullPath: `/service-manage/service-list/${serviceItem.id}`,
          path: `service-list/${serviceItem.id}`,
          component: () => import("@/pages/serviceManage/index"),
        });
      });
    }
  });
  store.commit('setting/setClusterId', clusterId)
  // localStorage.setItem("menuData", JSON.stringify(menuData));
  // localStorage.setItem("isCluster", 'isCluster');
  // localStorage.setItem("clusterId", clusterId);
  store.commit('setting/setMenuData', menuData)
  store.commit('setting/setIsCluster', 'isCluster')
  // 首次进入拿到菜单等数据 然后再去做刷新的操作
  setTimeout(() => {
    store.dispatch('setting/getRunningClusterList')
  }, global.intervalTime)
}
export {
  changeRouter
}