<!--
 * @Author: mjzhu
 * @Date: 2022-05-24 10:28:22
 * @LastEditTime: 2022-09-26 17:13:34
 * @FilePath: \ddh-ui\src\pages\serviceManage\overViewPage.vue
-->
<template>
  <div class="overview-page">
    <OverViewComponent :dashboardUrl="dashboardUrl" :reloadIframe="reloadIframe" />
  </div>
</template>

<script>
// import OverViewComponent from '@/components/overview';
const OverViewComponent = () => import ('@/components/overview')
import { mapState } from 'vuex'
export default {
  name: "overviewList",
  components: {OverViewComponent},
  data() {
    return {
      dashboardUrl: '',
      reloadIframe: false
    };
  },
  props: {
    serviceId: String,
  },
  computed: {
  },
  watch: {
    $route: {
      handler(val) {
        console.log(val)
        let url = ''
        const menuData = JSON.parse(localStorage.getItem('menuData')) || []
        const arr = menuData.filter(item => item.path === 'service-manage')
        if (arr.length > 0) {
          arr[0].children.map(item => {
            if (item.meta.params.serviceId == val.params.serviceId) url = item.meta.obj.dashboardUrl
          })
          this.analysisRouter(url)
        }
      },
      immediate: true
    }
  },
  methods: {
    analysisRouter(url) {
      console.log('xiaohui')
      // 先让iframe销毁
      this.reloadIframe = false;
      this.$nextTick(() => {
        // 赋值地址，再加载
        console.log('再来')
        this.dashboardUrl = url;
        this.reloadIframe = true;
      });
    },
  },
  mounted() {
  },
};
</script>

<style lang="less" scoped>
.overview-page {
  background: #fff;
  // padding: 20px;
  height: calc(100vh - 180px);
  position: relative;
}
</style>
