<template>
  <!-- 此文件路径禁止移动 -->
  <view>
    <view class="container ">
      <view class="u-skeleton" v-if="!articleData">
          <u-empty text="文章暂无内容" mode="list"></u-empty>
      </view>
      <u-parse v-else :html="articleData"></u-parse>
    </view>
  </view>
</template>

<script>
import { getArticleDetail } from "@/api/article.js";
export default {
  data() {
    return {
      // 用于接收上一级通过路径传输的数据
      routers: "", 
      // 请求文章接口后存储文章信息
      articleData: "",
    };
  },
  onLoad(val) {
    this.routers = val;
    getArticleDetail(val.id).then((res) => {
      if (res.data.result) {
        // 将请求的文章数据赋值
        this.articleData = res.data.result.content;
      }
      // 修改当前NavigationBar(标题头)为文章头部
      uni.setNavigationBarTitle({
        title: val.title,
      });
    });
  },
};
</script>
<style lang="scss" scoped>
page {
  background: #fff;
}
.container {
  padding: 32rpx;
  > p {
    margin: 20rpx;
  }
}
</style>