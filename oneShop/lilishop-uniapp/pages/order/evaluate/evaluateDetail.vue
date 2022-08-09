<template>
  <view>
    <view class="exaluate-member-view">
      <view class="member-view">
        <view class="member-img">
          <u-image width="82rpx" style="border: 1px solid #ededed" height="82rpx" shape="circle" :src="comment.memberProfile || '/static/missing-face.png'"></u-image>
        </view>
        <view class="member-info">
          <view class="memName">{{ comment.memberName }}</view>
          <view class="creName">{{ comment.createTime }}</view>
        </view>
      </view>
      <view class="goods-view">
        <view class="goods-title">商品评价: {{ gradeList[comment.grade] }}</view>
        <view class="goods-subtitle">
          {{ comment.content }}
        </view>
        <!-- 如果有图片则会循环显示评价的图片 -->
        <view class="goods-imgs-view" v-if="comment.images != null && comment.images.length != 0">
          <view class="img-view" v-for="(img, imgIndex) in comment.images.split(',')" :key="imgIndex">
            <u-image @click.native="preview(comment.images.split(','),imgIndex)" width="160rpx" height="160rpx" :src="img"></u-image>
          </view>
        </view>
        <view class="goods-name">
          {{ comment.goodsName }}
        </view>
        <view class="goods-subtitle"></view>
        <view class="commentStyle" v-if="comment.reply">
          商家回复：
          <span class="addCommentSpan">{{ comment.reply }}</span>
          <view class="img">
            <!-- 循环出商家回复评价的图片 -->
            <u-image width="140rpx" height="140rpx" v-if="comment.replyImage" v-for="(replyImg, replyIndex) in splitImg(comment.replyImage)" :src="replyImg" :key="replyIndex"
              @click="preview(splitImg( comment.replyImage), index)">
            </u-image>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      comment: {}, //评论信息
      gradeList: {
        //评价grade
        GOOD: "好评",
        MODERATE: "中评",
        WORSE: "差评",
        haveImage: "有图",
      },
    };
  },
  onLoad(options) {
    this.comment = JSON.parse(decodeURIComponent(options.comment));
  },
  methods: {
    /**
     * 切割图像
     */
    splitImg(val) {
      if (val && val.split(",")) {
        return val.split(",");
      } else if (val) {
        return val;
      } else {
        return false;
      }
    },
    /**
     * 点击图片放大或保存
     */
    preview(urls, index) {
      uni.previewImage({
        current: index,
        urls: urls,
        longPressActions: {
          itemList: ["保存图片"],
          success: function (data) {},
          fail: function (err) {},
        },
      });
    },
  },
};
</script>

<style lang="scss" scoped>
.commentStyle {
  margin-top: 16rpx;
  padding: 14rpx 26rpx;
  background: #f5f5f5;
  border-radius: 6px;
  font-size: 22rpx;
  font-weight: 700;
  text-align: left;
  line-height: 40rpx;
}
.img {
  display: flex;
  flex-wrap: wrap;
  /* height: 140rpx; */
  overflow: hidden;
  margin: 10rpx 0;

  image {
    width: 166rpx;
    height: 166rpx;
    margin: 0 15rpx 15rpx 0;

    &:nth-of-type(3n + 0) {
      margin: 0 0 15rpx 0;
    }
  }
}

.addCommentSpan {
  color: $u-tips-color !important;
  padding-left: 20rpx;
}
.memName {
  font-size: 28rpx;
}

.goods-name {
  border-bottom: 1px solid #ededed;
  padding-bottom: 30rpx;
}
.creName,
.goods-name {
  font-size: 24rpx;
  color: $u-tips-color;
}
page,
.content {
  background: $page-color-base;
  height: 100%;
}

.exaluate-member-view {
  background-color: #fff;
  margin-top: 12rpx;
  padding: 20rpx;
  .member-view {
    display: flex;
    flex-direction: row;
    align-items: center;
    .member-img {
      width: 100rpx;
      margin: 20rpx;
    }
    .member-info {
      margin-left: 15rpx;
    }
  }
  .goods-view {
    margin-left: 15rpx;
  }
}
.border-bottom {
  padding-bottom: 20rpx;
  border-bottom: 1px solid #ededed;
}
.goods-title {
  margin-bottom: 10rpx;
}
.goods-subtitle {
  margin-bottom: 20rpx;
  color: #909399;
}
.goods-imgs-view {
  margin: 20rpx 0;
  display: flex;
  flex-direction: row;
  align-items: center;
  .img-view {
    margin-right: 15rpx;
  }
}
</style>
