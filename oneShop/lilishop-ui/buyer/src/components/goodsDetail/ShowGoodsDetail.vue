<template>
  <div style="height:auto;">
    <div class="item-intro-show">
      <div class="item-intro-detail" ref="itemIntroDetail">
        <div class="item-intro-nav item-tabs">
          <Tabs :animated="false" @on-click="tabClick">
            <TabPane label="商品介绍">
              <div class="item-intro-img" ref="itemIntroGoods">
                <div class="item-intro" v-html="skuDetail.intro" v-if="skuDetail.intro"></div>
                <div v-else style="margin:20px;">暂无商品介绍</div>
              </div>
            </TabPane>
            <TabPane label="商品评价">
              <div class="remarks-container" ref="itemGoodsComment">
                <div class="remarks-analyse-box">
                  <div class="remarks-analyse-goods">
                    <i-circle :percent="skuDetail.grade" stroke-color="#5cb85c">
                      <span class="remarks-analyse-num">{{skuDetail.grade}}%</span>
                      <p class="remarks-analyse-title">好评率</p>
                    </i-circle>
                  </div>
                </div>
                <div class="remarks-bar">
                  <span @click="viewByGrade('')" :class="{selectedBar: commentParams.grade === ''}">全部({{commentTypeNum.all}})</span>
                  <span @click="viewByGrade('GOOD')" :class="{selectedBar: commentParams.grade === 'GOOD'}">好评({{commentTypeNum.good}})</span>
                  <span @click="viewByGrade('MODERATE')" :class="{selectedBar: commentParams.grade === 'MODERATE'}">中评({{commentTypeNum.moderate}})</span>
                  <span @click="viewByGrade('WORSE')" :class="{selectedBar: commentParams.grade === 'WORSE'}">差评({{commentTypeNum.worse}})</span>
                </div>
                <div style="text-align: center;margin-top: 20px;" v-if="commentList.length === 0">
                  暂无评价数据
                </div>
                <div class="remarks-box" v-for="(item,index) in commentList" :key="index" v-else>
                  <div class="remarks-user">
                    <Avatar :src="item.memberProfile" />
                    <span class="remarks-user-name">{{item.memberName | secrecyMobile}}</span>
                  </div>
                  <div class="remarks-content-box">
                    <p>
                      <Rate disabled :value="Number(item.descriptionScore)" allow-half class="remarks-star"></Rate>
                    </p>
                    <p class="remarks-content">{{item.content}}</p>
                    <div class="comment-img" v-if="item.images">
                      <div v-for="(img, imgIndex) in item.images.split(',')"
                       @click="previewImg(img, item)"
                       :class="{borderColor:img === item.previewImg}"
                       :key="imgIndex">
                        <img :src="img" alt="">
                      </div>
                    </div>
                    <div class="preview-img"  v-if="item.previewImg"  @click.prevent="hidePreviewImg(item)">
                      <div>
                        <span @click.stop="rotatePreviewImg(0, item)"><Icon type="md-refresh" />左转</span>
                        <span @click.stop="rotatePreviewImg(1, item)"><Icon type="md-refresh" />右转</span>
                      </div>
                      <img :src="item.previewImg" :style="{transform:`rotate(${item.deg}deg)`}" width="198" alt="">
                    </div>
                    <p class="remarks-sub">
                      <span class="remarks-item">{{item.goodsName}}</span>
                      <span class="remarks-time">{{item.createTime}}</span>
                    </p>
                  </div>
                </div>
                <div class="remarks-page">
                  <Page :total="commentTotal" size="small"
                    @on-change="changePageNum"
                    @on-page-size-change="changePageSize"
                    :page-size="commentParams.pageSize"
                    ></Page>
                </div>
              </div>
            </TabPane>
            <TabPane label="商品参数">
              <template v-if="detail.goodsParamsDTOList && detail.goodsParamsDTOList.length">
                <div class="goods-params" style="height:inherit;" v-for="item in detail.goodsParamsDTOList" :key="item.groupId">
                  <span class="ml_10">{{item.groupName}}</span>
                  <table class="mb_10" cellpadding='0' cellspacing="0" >
                    <tr v-for="param in item.goodsParamsItemDTOList" :key="param.paramId">
                      <td style="text-align: center">{{param.paramName}}</td><td>{{param.paramValue}}</td>
                    </tr>
                  </table>
                </div>
              </template>
              <div v-else>暂无商品参数</div>
            </TabPane>
          </Tabs>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { goodsComment, goodsCommentNum } from '@/api/member.js';
export default {
  name: 'ShowGoodsDetail',
  props: {
    detail: { // 商品详情
      type: Object,
      default: null
    }
  },
  data () {
    return {
      commentList: [], // 评论列表
      commentParams: { // 评论传参
        pageNumber: 1,
        pageSize: 10,
        grade: '',
        goodsId: ''
      },
      commentTypeNum: {}, // 评论数量，包括好中差分别的数量
      commentTotal: 0, // 评论总数
      onceFlag: true // 只调用一次
    };
  },
  computed: {
    // 商品详情
    skuDetail () {
      return this.detail.data;
    }
  },
  methods: {
    changeHeight (name) { // 设置商品详情高度
      let heightCss = window.getComputedStyle(this.$refs[name]).height;
      heightCss = parseInt(heightCss.substr(0, heightCss.length - 2)) + 89;
      this.$refs.itemIntroDetail.style.height = heightCss + 'px';
    },
    changePageNum (val) { // 修改评论页码
      this.commentParams.pageNumber = val;
      this.getList();
    },
    changePageSize (val) { // 修改评论页数
      this.commentParams.pageNumber = 1;
      this.commentParams.pageSize = val;
      this.getList();
    },
    getList () { // 获取评论列表
      this.commentParams.goodsId = this.skuDetail.goodsId;
      goodsComment(this.commentParams).then(res => {
        if (res.success) {
          this.commentList = res.result.records;
          this.commentTotal = res.result.total;
        }
      });
      goodsCommentNum(this.skuDetail.goodsId).then(res => {
        if (res.success) {
          this.commentTypeNum = res.result;
        }
      });
    },
    viewByGrade (grade) { // 好中差评切换
      this.$set(this.commentParams, 'grade', grade);
      this.commentParams.pageNumber = 1;
      this.getList();
    },
    tabClick (name) { // 商品详情和评价之间的tab切换
      if (name === 0) {
        this.$nextTick(() => {
          this.changeHeight('itemIntroGoods')
        });
      } else {
        this.$nextTick(() => {
          this.changeHeight('itemGoodsComment')
        });
      }
    },
    previewImg (img, item) { // 预览图片
      this.$set(item, 'previewImg', img);
      this.$nextTick(() => {
        this.changeHeight('itemGoodsComment')
      });
    },
    hidePreviewImg (item) { // 隐藏预览图片
      this.$set(item, 'previewImg', '');
      this.$nextTick(() => {
        this.changeHeight('itemGoodsComment')
      });
    },
    rotatePreviewImg (type, item) { // 图片旋转
      if (type) {
        if (item.deg) {
          this.$set(item, 'deg', item.deg + 90);
        } else {
          this.$set(item, 'deg', 90);
        }
      } else {
        if (item.deg) {
          this.$set(item, 'deg', item.deg - 90);
        } else {
          this.$set(item, 'deg', -90);
        }
      }
    },
    handleScroll () { // 监听页面滚动
      if (this.onceFlag) {
        this.$nextTick(() => {
          this.changeHeight('itemIntroGoods')
        });
        this.onceFlag = false
      }
    }
  },
  mounted () {
    this.$nextTick(() => { // 手动设置详情高度，解决无法撑开问题
      setTimeout(this.changeHeight('itemIntroGoods'), 2000);
    });
    window.addEventListener('scroll', this.handleScroll)
    this.getList();
    if (this.skuDetail.grade === null || this.skuDetail.grade === undefined) {
      this.skuDetail.grade = 100
    }
  }
};
</script>

<style scoped lang="scss">
.item-intro{
  >img{
    display:block;
  }
}
/***************商品详情介绍和推荐侧边栏开始***************/
.item-intro-show{

  width: 1200px;
  margin: 15px auto;
  display: flex;
  overflow-x: hidden;
  flex-direction: row;

}
.item-intro-recommend{
  width: 200px;
  display: flex;
  flex-direction: column;
}
.item-intro-recommend-column{
  display: flex;
  flex-direction: column;
  box-shadow: 0px 0px 5px #999;
}
.item-recommend-title{
  width: 100%;
  height: 38px;
  font-size: 16px;
  line-height: 38px;
  color: #fff;
  background-color: $theme_color;
  box-shadow: 0px 0px 5px $theme_color;
  text-align: center;
}
.item-recommend-column{
  margin-top: 15px;
}
.item-recommend-intro{
  padding: 5px 15px;
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  font-size: 12px;
  color: #999;
  cursor: pointer;
}
.item-recommend-img{
  width: 80%;
  margin: 0px auto;
  cursor: pointer;
}
.item-recommend-img img{
  width: 100%;
}
.item-recommend-top-num{
  color: #fff;
  margin: 0px 2px;
  padding: 1px 5px;
  border-radius: 12px;
  background-color: $theme_color;
}
.item-recommend-price{
  color: $theme_color;
  font-weight: bolder;
}
.item-intro-detail{
  margin: 0  30px;
  width: 100%;
}
.item-intro-nav{
  width: 100%;
  height: 38px;
  background-color: #F7F7F7;
}
.item-intro-nav ul{
  margin: 0px;
  padding: 0px;
  list-style: none;
}
.item-intro-nav li{
  float: left;
  height: 100%;
  width: 120px;
  line-height: 38px;
  text-align: center;
  color: $theme_color;
}
.item-intro-nav li:first-child{
  background-color: $theme_color;
  color: #fff;
}
.item-intro-img {
  width: 100%;
  min-height: 300px;
  /deep/ img{
    margin:0 auto;
  }
}
.item-intro-img img{
  max-width: 1000px;
}
/************* 商品参数 *************/
.item-param-container {
  display: flex;
  flex-wrap: wrap;
  flex-direction: row;
  justify-content: space-between;
}
.item-param-box {
  padding: 5px;
  padding-left: 30px;
  width: 240px;
  height: 36px;
  font-size: 14px;
}
.item-param-title {
  color: #232323;
}
.item-param-content {
  color: #999;
}
.remarks-title {
  padding-left: 15px;
  height: 36px;
  font-size: 16px;
  font-weight: bolder;
  line-height: 36px;
  color: #666666;
  background-color: #F7F7F7;
}
.remarks-analyse-box {
  padding: 15px;
  display: flex;
  align-items: center;
}
.remarks-analyse-goods {
  margin-left: 15px;
  margin-right: 15px;
}
.remarks-analyse-num {
  font-size: 26px;
}
.remarks-analyse-title {
  font-size: 12px;
  line-height: 20px;
}
.remarks-bar {
  padding-left: 15px;
  height: 36px;
  line-height: 36px;
  color: #666666;
  background-color: #F7F7F7;
  .selectedBar{
    color: $theme_color;
  }
}
.remarks-bar span {
  margin-right: 15px;
  &:hover{
    color: $theme_color;
    cursor: pointer;
  }
}
.remarks-box {
  padding: 15px;
  display: flex;
  flex-direction: row;
  border-bottom: 1px #ccc dotted;
}
.remarks-user {
  width: 180px;
}
.remarks-user-name {
  padding-left: 15px;
}
.remarks-content-box {
  width: calc(100% - 180px);
  .comment-img{
    display: flex;
    .borderColor{
      border-color: $theme_color;
    }
    div{
      border: 1px solid #999;
      margin-right: 5px;
      width: 50px;
      height: 50px;
      overflow: hidden;
      img{width: 100%;height: 100%;}
    }
  }
  .preview-img{
    position: relative;
    border: 1px solid #eee;
    margin: 10px 0;
    width: 200px;

    div{
      position: absolute;
      top: 3px;
      left: 3px;
      z-index: 3;
      span{
        display: inline-block;
        background-color: rgba(0,0,0,.5);
        padding:3px 5px;
        color: #fff;
        border-radius: 4px;
        cursor: pointer;
      }
      span:nth-child(1) .ivu-icon {
        transform: rotateY(180deg);
      }
    }

  }
}

.remarks-content {
  font-size: 14px;
  color: #232323;
  line-height: 28px;
}
.remarks-sub {
  margin-top: 5px;
  color: #ccc;
}
.remarks-time {
  margin-left: 15px;
}
.remarks-page {
  margin: 15px;
  display: flex;
  justify-content:flex-end;
}
/***************商品详情介绍和推荐侧边栏结束***************/
/* 改变便签页样式 */
.ivu-tabs-ink-bar {
  background-color: $theme_color !important;
}
/deep/.ivu-tabs-bar{
  border: none;
}
.item-tabs > .ivu-tabs > .ivu-tabs-bar .ivu-tabs-tab{
  border-radius: 0px;
  color: #999;
  height: 38px;
  // background: #F7F7F7;
}
.item-tabs > .ivu-tabs > .ivu-tabs-bar .ivu-tabs-tab-active{
  color: #fff;
  background-color: $theme_color;
}
.item-tabs > .ivu-tabs > .ivu-tabs-bar .ivu-tabs-tab-active:before{
  content: '';
  display: block;
  width: 100%;
  height: 1px;
  color: #fff;
  background: #F7F7F7;
  position: absolute;
  top: 0;
  left: 0;
}
.ivu-rate-star-full:before, .ivu-rate-star-half .ivu-rate-star-content:before {
  color: $theme_color;
}
table{
  border-color:#efefef;
  color: #999;
  min-width: 30%;
  margin-left: 30px;
  font-size: 12px;
  tr{
    td:nth-child(1){
      width: 100px;
    }
    td:nth-child(2){
      padding-left: 20px;
    }
  }
  td{
    padding: 6px;
  }
}
.goods-params {
  display: flex;
  border-bottom: 1px solid #eee;
  margin-left: 30px;
  span{color:#999}
}
</style>
