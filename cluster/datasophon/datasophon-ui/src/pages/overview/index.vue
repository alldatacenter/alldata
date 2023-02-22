<!--
 * @Author: mjzhu
 * @Date: 2022-05-24 10:28:22
 * @LastEditTime: 2022-09-26 17:05:52
 * @FilePath: \ddh-ui\src\pages\overview\index.vue
-->
<template>
  <div class="overview-page">
    <OverViewComponent :dashboardUrl="dashboardUrl" />
  </div>
</template>

<script>
// import OverViewComponent from '@/components/overview';
const OverViewComponent = () => import("@/components/overview");
import { mapState } from "vuex";
export default {
  name: "overviewList",
  components: { OverViewComponent },
  data() {
    return {
      reloadIframe: false,
      iframeUrl: "",
    };
  },
  watch: {
  },
  computed: {
    ...mapState("setting", ["dashboardUrl"]),
  },
  methods: {
  },
  mounted() {},
};
</script>

<style lang="less" scoped>
.overview-page {
  background: #fff;
  padding: 20px;
  height: calc(100vh - 100px);
  position: relative;
}
</style>
