// 全局组件配置

import empty from './empty/Main' // 空状态组件
import drawer from './drawer/Main' // 右侧bar
import Header from '@/components/header/Header'; // 头部组件
import FixedTopPage from '@/components/advertising/FixedTop'; // 顶部广告
import Footer from '@/components/footer/Footer'; // 底部栏
import Search from '@/components/Search' // 搜索框
import card from '@/components/card' // 个人中心 卡片
import cateNav from '@/components/nav/CateNav' // 个人中心 卡片

empty.install = function (Vue) {
  Vue.component('empty', empty);
};

drawer.install = function (Vue) {
  Vue.component('drawer', drawer);
};

Header.install = function (Vue) {
  Vue.component('BaseHeader', Header);
};

FixedTopPage.install = function (Vue) {
  Vue.component('FixedTopPage', FixedTopPage);
};

Footer.install = function (Vue) {
  Vue.component('BaseFooter', Footer);
};

Search.install = function (Vue) {
  Vue.component('Search', Search);
};

card.install = function (Vue) {
  Vue.component('card', card)
}

cateNav.install = function (Vue) {
  Vue.component('cateNav', cateNav)
}

// 引用本js中所有的组件
export function InstallAll (Vue) {
  Vue.use(empty)
  Vue.use(drawer)
  Vue.use(Header)
  Vue.use(FixedTopPage)
  Vue.use(Footer)
  Vue.use(Search)
  Vue.use(card)
  Vue.use(cateNav)
}
