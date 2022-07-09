<template>
  <div>
    <Card>
      <Form :model="liveForm" ref="liveForm" :rules="liveRulesForm" :label-width="120">
        <FormItem label="直播标题" prop="name">
          <Input disabled v-model="liveForm.name" style="width:460px"></Input>
          <div class="tips">直播间名字，最短3个汉字，最长17个汉字，1个汉字相当于2个字符</div>
        </FormItem>
        <FormItem label="主播昵称" prop="anchorName">
          <Input disabled v-model="liveForm.anchorName" style="width:360px"></Input>
          <div class="tips">主播昵称，最短2个汉字，最长15个汉字，1个汉字相当于2个字符</div>
        </FormItem>
        <FormItem label="直播时间" prop="startTime">

          <DatePicker disabled format="yyyy-MM-dd HH:mm" type="datetimerange" v-model="times" @on-change="handleChangeTime" :options="optionsTime" placeholder="直播计划开始时间-直播计划结束时间" style="width: 300px">
          </DatePicker>
          <div class="tips">直播开播时间需要在当前时间的10分钟后 并且 开始时间不能在 6 个月后</div>
        </FormItem>

        <FormItem label="主播微信号" prop="anchorWechat">
          <Input disabled v-model="liveForm.anchorWechat" style="width:360px" placeholder="主播微信号"></Input>
          <div class="tips">主播微信号，如果未实名认证，需要先前往“小程序直播”小程序进行<a target="_black" href="https://res.wx.qq.com/op_res/9rSix1dhHfK4rR049JL0PHJ7TpOvkuZ3mE0z7Ou_Etvjf-w1J_jVX0rZqeStLfwh">实名验证</a></div>
        </FormItem>

        <!-- 分享卡片 -->
        <FormItem label="分享卡片封面" prop="feedsImg">
          <div class="upload-list" v-if="liveForm.feedsImg">
            <template>
              <img :src="liveForm.feedsImg">
              <div class="upload-list-cover">
                <Icon type="ios-eye-outline" @click.native="handleView(liveForm.feedsImg)"></Icon>

              </div>
            </template>

          </div>
          <Upload v-if="liveForm.feedsImg.length ==0" ref="upload" :show-upload-list="false" :on-success="handleFeedsImgSuccess" :default-file-list="defaultImgList" :format="['jpg','jpeg','png']"
            :on-format-error="handleFormatError" :max-size="1024" :on-exceeded-size="handleMaxSize" type="drag" :action="action" :headers="accessToken" style="display: inline-block;width:58px;">
            <div style="width: 58px;height:58px;line-height: 58px;">
              <Icon type="ios-camera" size="20"></Icon>
            </div>
          </Upload>
          <div class="tips">
            直播间分享图，图片规则：建议像素800*640，大小不超过1M；
          </div>
        </FormItem>

        <!-- 直播间背景墙 -->
        <FormItem label="直播间背景墙" prop="coverImg">

          <div class="upload-list" v-if="liveForm.coverImg">
            <template>
              <img :src="liveForm.coverImg">
              <div class="upload-list-cover">
                <Icon type="ios-eye-outline" @click.native="handleView(liveForm.coverImg)"></Icon>

              </div>
            </template>
          </div>
          <Upload v-if="liveForm.coverImg.length ==0" ref="upload" :show-upload-list="false" :on-success="handleCoverImgSuccess" :default-file-list="defaultImgList" :format="['jpg','jpeg','png']"
            :on-format-error="handleFormatError" :max-size="1024" :on-exceeded-size="handleMaxSize" type="drag" :action="action" :headers="accessToken" style="display: inline-block;width:58px;">
            <div style="width: 58px;height:58px;line-height: 58px;">
              <Icon type="ios-camera" size="20"></Icon>
            </div>
          </Upload>
          <div class="tips"> 直播间背景图，图片规则：建议像素1080*1920，大小不超过1M</div>
        </FormItem>

        <!-- 直播间背景墙 -->
        <FormItem label="直播间分享图" prop="shareImg">

          <div class="upload-list" v-if="liveForm.shareImg">
            <template>
              <img :src="liveForm.shareImg">
              <div class="upload-list-cover">
                <Icon type="ios-eye-outline" @click.native="handleView(liveForm.shareImg)"></Icon>

              </div>
            </template>
          </div>
          <Upload v-if="liveForm.shareImg.length ==0" ref="upload" :show-upload-list="false" :on-success="handleShareImgSuccess" :default-file-list="defaultImgList" :format="['jpg','jpeg','png']"
            :on-format-error="handleFormatError" :max-size="1024" :on-exceeded-size="handleMaxSize" type="drag" :action="action" :headers="accessToken" style="display: inline-block;width:58px;">
            <div style="width: 58px;height:58px;line-height: 58px;">
              <Icon type="ios-camera" size="20"></Icon>
            </div>
          </Upload>
          <div class="tips"> 直播间分享图，图片规则：建议像素800*640，大小不超过1M</div>
        </FormItem>

        <FormItem label="商品" v-if="$route.query.id">
          <Table class="goods-table" :columns="liveColumns" :data="liveData">
            <template slot-scope="{ row,index }" slot="goodsName">
              <div class="flex-goods">
                <Badge v-if="index == 0 || index ==1" color="volcano"></Badge>
                <img class="thumbnail" :src="row.thumbnail || row.goodsImage">
                {{ row.goodsName || row.name }}
              </div>
            </template>
            <template slot-scope="{ row }" class="price" slot="price">
              <div>
                <div v-if="row.priceType == 1">{{row.price | unitPrice('￥')}}</div>
                <div v-if="row.priceType == 2">{{row.price | unitPrice('￥')}}至{{row.price2 | unitPrice('￥')}}</div>
                <div v-if="row.priceType == 3">{{row.price | unitPrice('￥')}}<span class="original-price">{{row.price2 | unitPrice('￥')}}</span></div>
              </div>
            </template>
            <template slot-scope="{ row }" slot="quantity">
              <div>{{row.quantity}}</div>
            </template>
          </Table>
          <div class="tips">
            直播间商品中前两个商品将自动被选为封面，伴随直播间在直播列表中显示
          </div>
        </FormItem>

        <FormItem>
          <Button type="primary" @click="createLives()">保存</Button>

        </FormItem>
      </Form>
    </Card>
    <!-- 浏览图片 -->
    <Modal title="查看图片" v-model="imageVisible">
      <img :src="imageSrc" v-if="imageVisible" style="width: 100%">
    </Modal>

  </div>
</template>

<script>
import { getLiveInfo } from "@/api/promotion";
export default {
  data() {
    return {
      imageVisible: false, //查看图片的dailog
      imageSrc: "", //查看图片的路径
      liveForm: {
        name: "", //直播标题
        anchorName: "", //主播昵称
        anchorWechat: "", //主播微信号
        feedsImg: "", //分享卡片封面
        coverImg: "", //直播间背景墙
        shareImg: "", //分享图
        startTime: "",
      },

      times: [], //接收直播时间数据
      // 直播商品表格表头
      liveColumns: [
        {
          title: "商品",
          slot: "goodsName",
        },
        {
          title: "价格",
          slot: "price",
        },
        {
          title: "库存",
          slot: "quantity",
          width: 100,
        },
        {
          title: "操作",
          slot: "action",
          width: 250,
        },
      ],
      liveData: [], //直播商品集合
      commodityList: "", //商品集合
    };
  },
  mounted() {
    /**
     * 如果query.id有值说明是查看详情
     * liveStatus 可以判断当前直播状态 从而区分数据 是否是未开始、已开启、已关闭、
     */
    if (this.$route.query.id) {
      // 获取直播间详情
      this.getLiveDetail();
    }
    this.accessToken = {
      accessToken: this.getStore("accessToken"),
    };
  },

  methods: {
    /**
     * 上传图片查看图片
     */
    handleView(src) {
      this.imageVisible = true;
      this.imageSrc = src;
    },
    // 上传文件超过大小限制
    handleMaxSize(file) {
      this.$Notice.warning({
        title: "文件大小过大",
        desc: "所选文件大小过大, 不得超过 1M.",
      });
    },
    /**
     * 获取直播间详情
     */
    async getLiveDetail() {
      let result = await getLiveInfo(this.$route.query.id);

      // 将数据回调到liveform里面
      if (result.success) {
        let data = result.result;
        for (let key in data) {
          this.liveForm[key] = data[key];
        }
        // 将选择的商品回调给表格

        this.liveData = data.commodityList;
        this.commodityList = data.commodityList;

        // 将时间格式化
        this.$set(
          this.times,
          [0],
          this.$options.filters.unixToDate(data.startTime, "yyyy-MM-dd hh:mm")
        );
        this.$set(
          this.times,
          [1],
          this.$options.filters.unixToDate(data.endTime, "yyyy-MM-dd hh:mm")
        );
        this.liveStatus = data.status;
      }
    },
  },
};
</script>

<style lang="scss" scoped>
.action {
  display: flex;
  /deep/ .ivu-btn {
    margin: 0 5px !important;
  }
}
.original-price {
  margin-left: 10px;
  color: #999;
  text-decoration: line-through;
}
.thumbnail {
  width: 50px;
  height: 50px;
  border-radius: 0.4em;
}
.flex-goods {
  margin: 10px;
  display: flex;

  align-items: center;
  > img {
    margin-right: 10px;
  }
}
.tips {
  color: #999;
  font-size: 12px;
}
.goods-table {
  width: 1000px;
  margin: 10px 0;
}
.upload-list {
  display: inline-block;
  width: 60px;
  height: 60px;
  text-align: center;
  line-height: 60px;
  border: 1px solid transparent;
  border-radius: 4px;
  overflow: hidden;
  background: #fff;
  position: relative;
  box-shadow: 0 1px 1px rgba(0, 0, 0, 0.2);
  margin-right: 4px;
}
.upload-list img {
  width: 100%;
  height: 100%;
}
.upload-list-cover {
  display: none;
  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;
  right: 0;
  background: rgba(0, 0, 0, 0.6);
}
.upload-list:hover .upload-list-cover {
  display: block;
}
.upload-list-cover i {
  color: #fff;
  font-size: 20px;
  cursor: pointer;
  margin: 0 2px;
}
</style>
