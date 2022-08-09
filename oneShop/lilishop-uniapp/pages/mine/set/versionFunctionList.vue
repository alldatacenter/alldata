<template>
  <div>
    <u-collapse v-if="versionData.length !=0">
      <u-collapse-item class="version-item" :title="item.versionName" v-for="(item, index) in versionData" :key="index">
        <!-- {{item.body}} -->

        {{item.content}}
      </u-collapse-item>

    </u-collapse>
    <u-empty class="empty" v-else text="暂无版本信息" mode="list"></u-empty>
  </div>
</template>

<script>
import { getAppVersionList } from "@/api/message";
export default {
  data() {
    return {
      versionData: [],
      appType: "",
      params: {
        pageNumber: 1,
        pageSize: 10,
      },
    };
  },
  mounted() {
    const platform = uni.getSystemInfoSync().platform;
    /**
     * 获取是否是安卓
     */
    if (platform === "android") {
      this.appType = "ANDROID";
    } else {
      this.IosWhether = true;
      this.appType = "IOS";
    }
    this.getVersionList();
  },
  methods: {
    async getVersionList() {
      let res = await getAppVersionList(this.appType, this.params);
      if (res.data.success) {
        this.versionData = res.data.result.records;
      }
    },
  },
};
</script>

<style scoped lang="scss">
.version-item{
    padding: 10rpx;
    background: #fff;
}

</style>