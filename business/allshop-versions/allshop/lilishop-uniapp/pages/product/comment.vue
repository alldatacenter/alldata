<template>
  <view class="comment">
    <view class="top-tab">
      <view class="tab-btn" :v-if="commentDetail">
        <view @click="select(0)" :class="{ cur: selectIndex == 0 }">全部</view>
        <view @click="select(1)" :class="{ cur: selectIndex == 1 }">好评{{ commentDetail.good }}</view>
        <view @click="select(2)" :class="{ cur: selectIndex == 2 }">中评{{ commentDetail.moderate }}</view>
        <view @click="select(3)" :class="{ cur: selectIndex == 3 }">差评{{ commentDetail.worse }}</view>
        <view @click="select(4)" :class="{ cur: selectIndex == 4 }">有图{{ commentDetail.haveImage }}</view>
      </view>
    </view>
    <!-- 评价 -->
    <div class="goodsBoxOver">
      <div class="scoll-page">
        <view class="eva-section">
          <div class="empty" v-if="commDetail.length < 1">
            <view>
              <u-empty mode="message" text="赞无评论"></u-empty>
            </view>
          </div>
          <view class="eva-box" v-for="(item, index) in commDetail" :key="index">
            <view class="section-info">
              <image class="portrait" :src="item.memberProfile || '/static/missing-face.png'" mode="aspectFill"></image>
              <view class="star-content">
                <text class="name">{{ item.memberName | noPassByName }}</text>
                <text class="time">{{ item.createTime }}</text>
              </view>
              <view class="stars">
                <text :class="{ star: item.deliveryScore > 0 }"></text>
                <text :class="{ star: item.deliveryScore > 1 }"></text>
                <text :class="{ star: item.deliveryScore > 2 }"></text>
                <text :class="{ star: item.deliveryScore > 3 }"></text>
                <text :class="{ star: item.deliveryScore > 4 }"></text>
              </view>
            </view>
            <view class="section-contant">
              <div class="content">{{ item.content }}</div>
              <view class="img">
                <!-- 循环出用户评价的图片 -->
                <u-image width="140rpx" height="140rpx" v-if="item.images" v-for="(img, i) in splitImg(item.images)" :src="img" :key="i" @click="preview(splitImg(item.images), i)">
                </u-image>
              </view>
              <view class="bot">
                <text class="attr">{{ item.goodsName }} - {{ gradeList[item.grade] }}</text>
              </view>
            </view>
            <view class="commentStyle" v-if="item.reply">
              商家回复：
              <span class="addCommentSpan">{{ item.reply }}</span>
              <view class="img">
                <!-- 循环出商家回复评价的图片 -->
                <u-image width="140rpx" height="140rpx" v-if="item.replyImage" v-for="(replyImg, replyIndex) in splitImg(item.replyImage)" :src="replyImg" :key="replyIndex"
                  @click="preview(splitImg( item.replyImage), index)">
                </u-image>
              </view>
            </view>
          </view>
          <u-loadmore bg-color="transparent" style="margin:40rpx 0" :status="status" @loadmore="loadmore()" icon-type="iconType" />
        </view>
      </div>
    </div>
  </view>
</template>

<script>
// import { getGoodsDetail } from '@/api/goods.js';
import * as membersApi from "@/api/members.js";

export default {
  data() {
    return {
      status: "loadmore", //底部刷新状态
      commentDetail: "", //评价详情
      selectIndex: "0", //检索条件
      params: {  // 评论分页提交数据
        pageNumber: 1,
        pageSize: 10,
        grade: "",
      },
      gradeList: {
        GOOD: "好评",
        MODERATE: "中评",
        WORSE: "差评",
        HAVEIMAGE: "有图",
      },
      // 评论详情
      commDetail: [],
      dataTotal: 0, //评论的总total数量
      opid: "", //上级传参id
    };
  },
  async onLoad(options) {
    this.getGoodsCommentsFun(options.id);
    this.getGoodsCommentsNum(options.id);
    this.opid = options.id;
  },
  
  /**
   * 触底加载
   */
  onReachBottom() {
    this.params.pageNumber++;
    this.getGoodsCommentsFun(this.opid);
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
     * 获取商品评论
     */
    getGoodsCommentsFun(id) {
      this.status = "loading";
      // getGoodsComments
      membersApi.getGoodsComments(id, this.params).then((res) => {
        if (
          res.data.result.records == [] ||
          res.data.result.records == "" ||
          res.data.result.records == null
        ) {
          this.status = "noMore";
          return false;
        }
        this.commDetail = this.commDetail.concat(res.data.result.records);
        this.dataTotal = res.data.result.total;
        this.status = "loadmore";
      });
    },

    /**
     * 获取商品评论数
     */
    getGoodsCommentsNum(id) {
      membersApi.getGoodsCommentsCount(id).then((res) => {
        if (res.statusCode === 200) {
          this.commentDetail = res.data.result;
        }
      });
    },

    /**
     * 顶部筛选条件
     */
    select(index) {
      this.selectIndex = index;
      this.params.grade = ["", "GOOD", "MODERATE", "WORSE", ""][
        this.selectIndex
      ];
      this.selectIndex == 4 ? (this.params.haveImage = 1) : true;
      this.params.pageNumber = 1;
      this.params.pageSize = 10;
      this.commDetail = [];
      if (this.selectIndex == 0) {
        this.params = {
          pageNumber: 1,
          pageSize: 10,
          grade: "",
        };
      }
      // 重新加载评论
      this.getGoodsCommentsFun(this.opid);
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
          success: function (data) {
            uni.showToast({
              title: "保存成功",
              duration: 2000,
              icon: "none",
            });
          },
          fail: function (err) {
            uni.showToast({
              title: "保存失败",
              duration: 2000,
              icon: "none",
            });
          },
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

.addCommentSpan {
  color: $u-tips-color !important;
  padding-left: 20rpx;
}
.img {
  display: flex;
  flex-wrap: wrap;
  /* height: 140rpx; */
  overflow: hidden;

  image {
    width: 166rpx;
    height: 166rpx;
    margin: 0 15rpx 15rpx 0;

    &:nth-of-type(3n + 0) {
      margin: 0 0 15rpx 0;
    }
  }
}

.goodsBoxOver {
  overflow-y: scroll;
}

page {
  background: #f7f7f7;
}
.comment {
  color: #333;
  background: #f7f7f7;

  overflow: hidden;

  .top-tab {
    background: #fff;
    margin-bottom: 10rpx;
    border-radius: 20rpx;
    display: flex;
    flex-direction: column;
    padding: 0 30rpx 0 30rpx;
    font-size: 24rpx;

    .tab-btn {
      margin-top: 20rpx;
      display: flex;
      flex-wrap: wrap;

      view {
        min-width: 118rpx;
        text-align: center;
        height: 50rpx;
        line-height: 50rpx;
        padding: 0 10rpx;
        background: #f8f8fe;
        border-radius: 25rpx;
        margin: 0 20rpx 30rpx 0;

        &.cur {
          background: $aider-light-color;
          color: #fff;
        }
      }
    }
  }

  .eva-section {
    padding: 20rpx 0;

    .eva-box {
      padding: 40rpx;
      margin-bottom: 10rpx;
      background: #fff;
      border-radius: 20rpx;
      /* star */
      .star-content {
        display: flex;
        flex-direction: column;

        view {
          flex: 1;
          display: flex;
          align-items: center;
        }

        .time {
          font-size: 24rpx;
          color: #999;
        }
      }

      .section-info {
        display: flex;

        .stars {
          flex: 1;
          display: flex;
          justify-content: flex-end;
          align-items: center;

          .star {
            width: 30rpx;
            height: 30rpx;
            background: url("/static/star.png");
            background-size: 100%;
          }
        }
        .portrait {
          flex-shrink: 0;
          width: 80rpx;
          height: 80rpx;
          border-radius: 100px;
          margin-right: 20rpx;
        }
      }
      .section-contant {
        display: flex;
        flex-direction: column;

        .content {
          font-size: 24rpx;
          line-height: 46rpx;
          font-weight: 400;
          color: $font-color-dark;
          color: #333;
          padding: 26rpx 0;
        }

        .img {
          display: flex;
          flex-wrap: wrap;
          /* height: 140rpx; */
          overflow: hidden;
          > * {
            margin-right: 16rpx;
          }
        }

        .bot {
          display: flex;
          justify-content: space-between;
          font-size: $font-sm;
          color: $font-color-light;
          margin-top: 20rpx;

       
        }
      }
    }
  }
}

.empty {
  padding-top: 300rpx;
  color: #999999;
  text-align: center;
}
</style>
