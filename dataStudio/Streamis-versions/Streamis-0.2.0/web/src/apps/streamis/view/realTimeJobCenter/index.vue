<template>
  <div class="container">
    <coreIndex ref="coreIndex"/>
    <div class="divider"/>
    <jobList @refreshCoreIndex="refreshCoreIndex"/>
  </div>
</template>
<script>
import coreIndex from '@/apps/streamis/module/coreIndex';
import jobList from '@/apps/streamis/module/jobList';
export default {
  components: {
    coreIndex: coreIndex.component,
    jobList: jobList.component
  },
  data() {
    return {
      navHeight: 0,
    };
  },
  mounted() {
    // this.init();
  },
  methods: {
    resize(height) {
      this.navHeight = height;
    },
    refreshCoreIndex(){
      this.$refs.coreIndex.getIndexData();
    }
  },
};
</script>
<style lang="scss" scoped>
.container{
  padding: 10px 30px 0;
}
.divider{
  height: 30px;
}
</style>
