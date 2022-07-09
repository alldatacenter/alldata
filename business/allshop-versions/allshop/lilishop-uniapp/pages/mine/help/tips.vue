<template>
  <div class="wrapper">
    <u-parse v-html="res.content"></u-parse>

  </div>
</template>
<script>
import { getArticleDetail } from "@/api/article";
export default {
  data() {
    return {
      res: "",
      way: {
        user: {
          title: "用户协议",
          type: "USER_AGREEMENT",
        },
        privacy: {
          title: "隐私政策",
          type: "PRIVACY_POLICY",
        },
        message: {
          title: "证照信息",
          type: "LICENSE_INFORMATION",
        },
        about: {
          title: "关于我们",
          type: "ABOUT",
        },
      },
    };
  },
  mounted() {},
  onLoad(option) {
    uni.setNavigationBarTitle({
      title: this.way[option.type].title,
    });
    this.init(option);
  },

  methods: {
    init(option) {
      getArticleDetail(this.way[option.type].type).then((res) => {
        if (res.data.success) {
          this.res = res.data.result;
        }
      });
    },
  },
};
</script>
<style lang="scss" scoped>
.wrapper {
  padding: 16rpx;
}
</style>