<template>
  <view class="evaluate-box">
    <view class="eva-section" @click="toComment(goodsDetail.goodsId, goodsDetail.grade)">
      <view class="e-header">

        <view class="evaluate-title">评价</view>
        <text class="evaluate-num">{{ commDetail.total || '0' }}+</text>
        <text class="tip">好评率 {{ grade || '100' }}%</text>
      </view>
      <div v-if="commDetail && commDetail.records && commDetail.records.length > 0">
        <view class="eva-box" v-for="(commItem,commIndex) in commDetail.records.slice(0,2)" :key="commIndex">
          <view class="section-info">
            <u-avatar mode="circle" size="60" class="portrait" :src="commItem.memberProfile"></u-avatar>
            <view class="star-con">
              <text class="name">{{ commItem.memberName | noPassByName }}</text>
            </view>
          </view>
          <view class="section-contant">
            <u-read-more ref="uReadMore" :color="lightColor">
              <rich-text @load="parseLoaded" :nodes="commItem.content " class="con"></rich-text>
            </u-read-more>
            <scroll-view scroll-x class="scroll-x" v-if="commItem.image">
              <view class="img">
                <u-image border-radius="12" class="commImg" width="160rpx" height="160rpx" v-for="(item, index) in commItem.image.split(',')" :src="item" :key="index"
                  @click.stop="previewImg(commItem.image, index)"></u-image>
              </view>
            </scroll-view>
            <view class="bot">
              <text class="attr">{{ commItem.goodsName }}</text>
            </view>
          </view>
        </view>
      </div>

      <div v-else class="goodsNoMore">
        <u-empty text="该商品暂无评论" mode="message"></u-empty>
      </div>
    </view>
    <!-- 查看全部评价按钮 -->
    <view v-if="commDetail && commDetail.records && commDetail.records.length > 0" class="eva-section-btn" @click="toComment(goodsDetail.goodsId, goodsDetail.grade)">
      <text>查看全部评价</text>
    </view>
  </view>
</template>

<script>
import * as API_Members from "@/api/members.js";
export default {
  data() {
    return {
      lightColor: this.$lightColor,
      // 评论集合
      commDetail: [],
      grade: "",
      // 评论分页提交数据
      params: {
        pageNumber: 1,
        pageSize: 10,
        grade: "",
      },
    };
  },
  props: {
    goodsDetail: {
      default: {},
      type: Object,
    },
  },

  watch: {
    goodsDetail: {
      handler(val) {
        this.grade = val.grade;
        this.getGoodsCommentsMethods();
      },
      deep: true,
      immediate: true,
    },
  },
  mounted() {
  
  },
  methods: {
    parseLoaded() {
      this.$refs.uReadMore.init();
    },

    // 获取商品评论
    getGoodsCommentsMethods() {
      API_Members.getGoodsComments(this.goodsDetail.goodsId, this.params).then(
        (res) => {
          this.commDetail = res.data.result;
        }
      );
    },
    toComment(id, grade) {
      uni.navigateTo({
        url: `/pages/product/comment?id=${id}&grade=${grade}`,
      });
    },
    /**
     * 点击图片放大或保存
     */
    previewImg(url, index) {
      uni.previewImage({
        urls: url,
        indicator: "number",
        current: index,
      });
    },
  },
};
</script>

<style lang="scss" scoped>
@import "../product.scss";
.commImg {
  margin-right: 12rpx;
  margin-bottom: 10rpx;
  display: inline-block;
}
.name {
  color: #262626;
  font-size: 22rpx;
}
.eva-section-btn {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 40rpx 40rpx 50rpx;
  margin: 2px 0 20rpx 0;
  background: #fff;

  text {
    width: 200rpx;
    height: 50rpx;
    font-size: 22rpx;
    line-height: 46rpx;
    text-align: center;
    color: #262626;
    border: 2rpx solid #ededed;
    box-sizing: border-box;
    border-radius: 30px;
  }
}

.goodsNoMore {
  padding: 20rpx 0;
  text-align: center;
  color: $u-tips-color;
}
/* 评价 */
.eva-section {
  display: flex;
  flex-direction: column;

  background: #fff;

  .e-header {
    display: flex;
    align-items: baseline;

    font-size: $font-sm + 2rpx;
    color: $font-color-light;
    > .evaluate-num {
      margin-left: 10rpx;
      font-size: 24rpx;
      color: #333;
    }
    .tit {
      font-size: 32rpx;
      color: $font-color-dark;
      font-weight: 500;
      margin: 0 4rpx;
    }

    .tip {
      flex: 1;
      text-align: right;
      color: #8c8c8c;
      font-size: 24rpx;
    }

    .icon-you {
      margin-left: 10rpx;
    }
  }
}
.scroll-x {
  white-space: nowrap;
  -webkit-overflow-scrolling: touch;

  overflow-x: auto;
}
.eva-box {
  padding: 36rpx 0 30rpx 0;
  border-bottom: 2rpx solid #f2f2f2;
  .section-info {
    display: flex;
    align-items: center;

    .portrait {
      flex-shrink: 0;
      width: 80rpx;
      height: 80rpx;
      border-radius: 100px;
      margin-right: 20rpx;
    }

    > view > text {
      margin-left: 6rpx;
    }
  }

  .section-contant {
    display: flex;
    flex-direction: column;

    .con {
      font-size: 26rpx;
      line-height: 46rpx;
      color: $font-color-dark;
      color: #333;
      padding: 20rpx 0;
    }
    .bot {
      display: flex;
      justify-content: space-between;
      font-size: 22rpx;
      color: #999;
      margin-top: 20rpx;
    }
  }
}
</style>
