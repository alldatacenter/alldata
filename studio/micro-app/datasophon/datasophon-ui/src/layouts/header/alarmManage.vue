<template>
  <div class="cluster-setting mgr10 mgl10" style="margin-top: -1px;"   @click="showManage">
      <svg-icon class="icon-gj" icon-class="gaojing" ></svg-icon>
  </div>
</template>

<script>
import {mapMutations ,mapState} from 'vuex'
import alarmModal from '@/components/alarmModal'
export default {
  name: "AlarmManage",

  data() {
    return {

    };
  },

  mounted() {},
  computed:{
    ...mapState('setting', ['alarmManageVisible'])
  
  },
  methods: {
    ...mapMutations('setting',["showAlarmManageVisible"]),
    showManage(){
      //this.alarmManageVisible?this.showAlarmManageVisible(false):this.showAlarmManageVisible(true)
      let width = 1000;
      let title = "告警详情";
      let content = (
        <alarmModal alarmAll={true} />
      );
      this.$confirm({
        width: width,
        title: title,
        content: content,
        closable: true,
        icon: () => {
          return <div />;
        },
      });
    }
  },
};
</script>

<style lang="less" scoped>
.cluster-setting {
  .icon-gj{
     color: #fff;
     width: 16px;
     height: 16px;
    cursor: pointer;
  }
}
</style>
