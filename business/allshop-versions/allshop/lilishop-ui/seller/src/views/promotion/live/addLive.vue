<template>
  <div>
    <Card style="position: relative">
      <Spin size="large" fix v-if="spinShow"></Spin>
      <Alert type="warning">
        <template slot="desc">
          为了方便在创建直播间时从选择商品，请尽量提前提审直播商品
        </template>
      </Alert>

      <Form :model="liveForm" ref="liveForm" :rules="liveRulesForm" :label-width="120">
        <FormItem label="直播标题" prop="name">
          <Input
            :disabled="liveStatus != 'NEW'"
            v-model="liveForm.name"
            style="width: 460px"
          ></Input>
          <div class="tips">
            直播间名字，最短3个汉字，最长17个汉字，1个汉字相当于2个字符
          </div>
        </FormItem>
        <FormItem label="主播昵称" prop="anchorName">
          <Input
            :disabled="liveStatus != 'NEW'"
            v-model="liveForm.anchorName"
            style="width: 360px"
          ></Input>
          <div class="tips">
            主播昵称，最短2个汉字，最长15个汉字，1个汉字相当于2个字符
          </div>
        </FormItem>
        <FormItem label="直播时间" prop="startTime">
          <DatePicker
            :disabled="liveStatus != 'NEW'"
            format="yyyy-MM-dd HH:mm"
            type="datetimerange"
            v-model="times"
            @on-change="handleChangeTime"
            :options="optionsTime"
            placeholder="直播计划开始时间-直播计划结束时间"
            style="width: 300px"
          >
          </DatePicker>
          <div class="tips">
            直播开播时间需要在当前时间的10分钟后并且,开始时间不能在6个月后,直播计划结束时间（开播时间和结束时间间隔不得短于30分钟，不得超过24小时）
          </div>
        </FormItem>

        <FormItem label="主播微信号" prop="anchorWechat">
          <Input
            :disabled="liveStatus != 'NEW'"
            v-model="liveForm.anchorWechat"
            style="width: 360px"
            placeholder="主播微信号"
          ></Input>
          <div class="tips">
            主播微信号，如果未实名认证，需要先前往“小程序直播”小程序进行<a
              target="_black"
              href="https://res.wx.qq.com/op_res/9rSix1dhHfK4rR049JL0PHJ7TpOvkuZ3mE0z7Ou_Etvjf-w1J_jVX0rZqeStLfwh"
              >实名验证</a
            >
          </div>
        </FormItem>

        <!-- 分享卡片 -->
        <FormItem label="分享卡片封面" prop="feedsImg">
          <div class="upload-list" v-if="liveForm.feedsImg">
            <template>
              <img :src="liveForm.feedsImg" />
              <div class="upload-list-cover">
                <Icon
                  type="ios-eye-outline"
                  @click.native="handleView(liveForm.feedsImg)"
                ></Icon>
                <Icon
                  type="ios-trash-outline"
                  @click.native="handleRemove('feedsImg')"
                ></Icon>
              </div>
            </template>
          </div>
          <Upload
            v-if="liveForm.feedsImg.length == 0"
            ref="upload"
            :show-upload-list="false"
            :on-success="handleFeedsImgSuccess"
            :format="['jpg', 'jpeg', 'png']"
            :on-format-error="handleFormatError"
            :max-size="1024"
            :on-exceeded-size="handleMaxSize"
            type="drag"
            :action="action"
            :headers="accessToken"
            style="display: inline-block; width: 58px"
          >
            <div style="width: 58px; height: 58px; line-height: 58px">
              <Icon type="ios-camera" size="20"></Icon>
            </div>
          </Upload>
          <div class="tips">直播间分享图，图片规则：建议像素800*640，大小不超过1M；</div>
        </FormItem>

        <!-- 直播间背景墙 -->
        <FormItem label="直播间背景墙" prop="coverImg">
          <div class="upload-list" v-if="liveForm.coverImg">
            <template>
              <img :src="liveForm.coverImg" />
              <div class="upload-list-cover">
                <Icon
                  type="ios-eye-outline"
                  @click.native="handleView(liveForm.coverImg)"
                ></Icon>
                <Icon
                  type="ios-trash-outline"
                  @click.native="handleRemove('coverImg')"
                ></Icon>
              </div>
            </template>
          </div>
          <Upload
            v-if="liveForm.coverImg.length == 0"
            ref="upload"
            :show-upload-list="false"
            :on-success="handleCoverImgSuccess"
            :format="['jpg', 'jpeg', 'png']"
            :on-format-error="handleFormatError"
            :max-size="1024"
            :on-exceeded-size="handleMaxSize"
            type="drag"
            :action="action"
            :headers="accessToken"
            style="display: inline-block; width: 58px"
          >
            <div style="width: 58px; height: 58px; line-height: 58px">
              <Icon type="ios-camera" size="20"></Icon>
            </div>
          </Upload>
          <div class="tips">直播间背景图，图片规则：建议像素1080*1920，大小不超过1M</div>
        </FormItem>

        <!-- 直播间背景墙 -->
        <FormItem label="直播间分享图" prop="shareImg">
          <div class="upload-list" v-if="liveForm.shareImg">
            <template>
              <img :src="liveForm.shareImg" />
              <div class="upload-list-cover">
                <Icon
                  type="ios-eye-outline"
                  @click.native="handleView(liveForm.shareImg)"
                ></Icon>
                <Icon
                  type="ios-trash-outline"
                  @click.native="handleRemove('shareImg')"
                ></Icon>
              </div>
            </template>
          </div>
          <Upload
            v-if="liveForm.shareImg.length == 0"
            ref="upload"
            :show-upload-list="false"
            :on-success="handleShareImgSuccess"
            :format="['jpg', 'jpeg', 'png']"
            :on-format-error="handleFormatError"
            :max-size="1024"
            :on-exceeded-size="handleMaxSize"
            type="drag"
            :action="action"
            :headers="accessToken"
            style="display: inline-block; width: 58px"
          >
            <div style="width: 58px; height: 58px; line-height: 58px">
              <Icon type="ios-camera" size="20"></Icon>
            </div>
          </Upload>
          <div class="tips">直播间分享图，图片规则：建议像素800*640，大小不超过1M</div>
        </FormItem>

        <FormItem label="商品" v-if="$route.query.id">
          <Button
            type="primary"
            ghost
            @click="liveGoodsVisible = true"
            :disabled="liveStatus != 'NEW'"
            icon="md-add"
            >添加商品</Button
          >
          <Table class="goods-table" :columns="liveColumns" :data="liveData">
            <template slot-scope="{ row, index }" slot="goodsName">
              <div class="flex-goods">
                <Badge v-if="index == 0 || index == 1" color="volcano"></Badge>
                <img class="thumbnail" :src="row.thumbnail || row.goodsImage" />
                {{ row.goodsName || row.name }}
              </div>
            </template>
            <template slot-scope="{ row }" class="price" slot="price">
              <div>
                <div v-if="row.priceType == 1">{{ row.price | unitPrice("￥") }}</div>
                <div v-if="row.priceType == 2">
                  {{ row.price | unitPrice("￥") }}至{{ row.price2 | unitPrice("￥") }}
                </div>
                <div v-if="row.priceType == 3">
                  {{ row.price | unitPrice("￥")
                  }}<span class="original-price">{{ row.price2 | unitPrice("￥") }}</span>
                </div>
              </div>
            </template>
            <template slot-scope="{ row }" slot="quantity">
              <div>{{ row.quantity }}</div>
            </template>
            <template slot-scope="{ row, index }" slot="action">
              <div class="action">
                <Button
                  size="small"
                  type="primary"
                  :disabled="liveStatus != 'NEW'"
                  @click="deleteGoods(row, index)"
                  >删除</Button
                >
                <Button
                  size="small"
                  ghost
                  type="primary"
                  :disabled="liveStatus != 'NEW'"
                  @click="onMove(row.id, 1)"
                  >上移</Button
                >
                <Button
                  size="small"
                  ghost
                  type="primary"
                  :disabled="liveStatus != 'NEW'"
                  @click="onMove(row.id, 0)"
                  >下移</Button
                >
              </div>
            </template>
          </Table>
          <div class="tips">
            直播间商品中前两个商品将自动被选为封面，伴随直播间在直播列表中显示
          </div>
        </FormItem>

        <FormItem>
          <Button type="primary" v-if="liveStatus == 'NEW'" @click="createLives()"
            >保存</Button
          >
        </FormItem>
      </Form>
    </Card>
    <!-- 浏览图片 -->
    <Modal title="查看图片" v-model="imageVisible">
      <img :src="imageSrc" v-if="imageVisible" style="width: 100%" />
    </Modal>

    <Modal width="800" v-model="liveGoodsVisible" footer-hide>
      <liveGoods @selectedGoods="callBackData" reviewed />
    </Modal>
  </div>
</template>

<script>
import { uploadFile } from "@/libs/axios";
import {
  addLive,
  addLiveGoods,
  editLive,
  getLiveInfo,
  delRoomLiveGoods,
} from "@/api/promotion";
import liveGoods from "./liveGoods";
export default {
  components: {
    liveGoods,
  },
  data() {
    return {
      spinShow: false, // loading加载
      liveGoodsVisible: false, //选择商品
      imageVisible: false, //查看图片的dailog
      imageSrc: "", //查看图片的路径
      action: uploadFile, // 上传地址
      accessToken: {}, // 验证token
      liveStatus: "NEW", //当前直播状态
      // 不能选择今天以前的时间
      optionsTime: {
        disabledDate(date) {
          return date && date.valueOf() < Date.now() - 86400000;
        },
      },
      // 直播间数据上传规则
      liveRulesForm: {
        name: [
          { required: true, message: "请输入直播标题", trigger: "blur" },
          { max: 17, min: 3, message: "直播间名字最短3个汉字，最长17个汉字" },
        ],
        anchorName: [
          { required: true, message: "请输入主播昵称", trigger: "blur" },
          { max: 15, min: 2, message: "主播昵称最短2个汉字，最长15个汉字" },
        ],
        anchorWechat: [{ required: true, message: "请输入主播微信号", trigger: "blur" }],
        startTime: [
          {
            required: true,
            message: "请正确输入开始时间以及结束时间",
          },
        ],
        feedsImg: [{ required: true, message: "分享卡片封面不能为空", trigger: "blur" }],
        coverImg: [{ required: true, message: "直播间背景墙不能为空", trigger: "blur" }],
        shareImg: [{ required: true, message: "直播间分享图不能为空", trigger: "blur" }],
      },
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
     * 删除直播间商品
     */
    async deleteGoods(val, index) {
      this.$Spin.show();
      let res = await delRoomLiveGoods(this.liveForm.roomId, val.liveGoodsId);
      if (res.success) {
        this.$Message.success("删除成功!");
        this.liveData.splice(index, 1);
        this.$Spin.hide();
      } else {
        this.$Spin.hide();
      }
    },
    /**
     * 获取直播间详情
     */
    async getLiveDetail() {
      let result = await getLiveInfo(this.$route.query.id);

      // 将数据回调到liveform里面
      if (result.success) {
        console.log(result);
        let data = result.result;
        for (let key in data) {
          this.liveForm[key] = data[key];
        }
        // 将选择的商品回调给表格

        this.liveData = data.commodityList;

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
    /**
     * 上下移动功能
     * dir 1为上 0为下
     */
    onMove(code, dir) {
      let moveComm = (curIndex, nextIndex) => {
        let arr = this.liveData;
        arr[curIndex] = arr.splice(nextIndex, 1, arr[curIndex])[0];
        return arr;
      };
      this.liveData.some((val, index) => {
        if (val.id === code) {
          if (dir === 1 && index === 0) {
            this.$message.Warning("已在顶部！");
          } else if (dir === 0 && index === this.liveData.length - 1) {
            this.$message.Warning("已在底部！");
          } else {
            let nextIndex = dir === 1 ? index - 1 : index + 1;
            this.liveData = moveComm(index, nextIndex);
          }
          return true;
        }
        return false;
      });
    },
    /**
     * 回调的商品选择数据
     */
    callBackData(way) {
      console.log(way);
      this.liveGoodsVisible = false;
      this.$Spin.show();
      addLiveGoods({
        roomId: this.$route.query.roomId,
        liveGoodsId: way.liveGoodsId,
      }).then((res) => {
        if (res.success) {
          this.liveData.push(way);
          this.$Spin.hide();
          console.log(this.liveData);
        } else {
          this.$Spin.hide();
        }
      });
    },

    /**
     * 上传图片查看图片
     */
    handleView(src) {
      this.imageVisible = true;
      this.imageSrc = src;
    },

    /**
     * 删除上传的图片
     */
    handleRemove(type) {
      if (this.liveStatus == "NEW") {
        this.liveForm[type] = "";
      } else {
        this.$Message.error("当前状态禁止修改删除!");
      }
    },
    /**
     * 直播间背景图上传成功回调
     */
    handleCoverImgSuccess(res) {
      this.liveForm.coverImg = res.result;
    },
    /**
     * 直播间分享图上传成功回调
     */
    handleShareImgSuccess(res) {
      console.log(res);
      this.liveForm.shareImg = res.result;
    },

    /**
     * 分享卡片封面上传成功回调
     */
    handleFeedsImgSuccess(res) {
      this.liveForm.feedsImg = res.result;
    },

    /**
     * 直播间背景图
     */
    handleCoverImgSuccess(res) {
      this.liveForm.coverImg = res.result;
    },

    tipsDateError() {
      this.$Message.error({
        content:
          "直播开播时间需要在当前时间的10分钟后并且,开始时间不能在6个月后,直播计划结束时间（开播时间和结束时间间隔不得短于30分钟，不得超过24小时）",
        duration: 5,
      });
    },

    /**
     * 选择时间后的回调
     */
    handleChangeTime(daterange) {
      /**
       * 直播开播时间需要在当前时间的10分钟后
       * 此处设置默认为15分钟方便调整
       */
      let siteTime = new Date().getTime() / 1000;
      let selectTime = new Date(daterange[0]).getTime() / 1000;
      let currentTime = this.$options.filters.unixToDate(siteTime);
      /**
       * 开播时间和结束时间间隔不得短于30分钟，不得超过24小时
       * 判断用户设置的结束时间
       */
      let endTime = new Date(daterange[1]).getTime() / 1000;
      if (selectTime <= siteTime + 15 * 60) {
        this.tipsDateError();
        return false;
      } else if (selectTime + 30 * 60 >= endTime) {
        // 不能小于30分钟

        this.tipsDateError();
        return false;
      } else if (selectTime + 24 * 60 * 60 <= endTime) {
        // 不能超过24小时

        this.tipsDateError();
        return false;
      } else if (
        // 不能超过6个月
        siteTime >=
        new Date().getTime() + 6 * 31 * 24 * 3600 * 1000 + 86400000
      ) {
        this.tipsDateError();
        return false;
      } else {
        this.$set(this.times, [0], currentTime);
        this.times[1] = daterange[1];

        // this.times = daterange;
        this.$set(this.liveForm, "startTime", new Date(daterange[0]).getTime() / 1000);
        this.$set(this.liveForm, "endTime", new Date(daterange[1]).getTime() / 1000);
      }
    },

    /**
     * 对图片错误进行回调
     */
    handleFormatError(file) {
      this.$Notice.warning({
        title: "请上传正确的图片格式！",
        desc: file.name + " 格式不为 jpg or png.",
      });
    },

    /**
     * 对图片的大小进行处理回调
     */
    handleMaxSize(file) {
      this.$Notice.warning({
        title: "图片超过限制大小！",
        desc: "图片超过规定限制大小，请重新上传",
      });
    },

    /**
     * 限制只能上传一张图片
     */
    handleBeforeUpload(type) {
      const check = this.liveForm[type].length < 1;
      if (!check) {
        this.$Notice.warning({
          title: "最多上传一张图片",
        });
      }
      return check;
    },

    /**
     * 添加直播间 /broadcast/studio/edit
     */
    createLives() {
      this.$refs["liveForm"].validate((valid) => {
        if (valid) {
          // 需判断当前是否是添加商品
          if (this.$route.query.id) {
            this.spinShow = true;
            this.liveForm.commodityList = JSON.stringify(this.liveForm.commodityList);
            delete this.liveForm.updateTime;
            // 将当前直播间修改
            editLive(this.liveForm).then((res) => {
              if (res.success) {
                this.$Message.success("修改成功!");

                this.$router.push({ path: "/promotion/live" });
              }
              this.spinShow = false;
            });
          } else {
            // 此处为创建直播
            this.spinShow = true;
            addLive(this.liveForm).then((res) => {
              if (res.success) {
                this.$Message.success("添加成功!");

                this.$router.push({ path: "/promotion/live" });
              }
              this.spinShow = false;
            });
          }
        }
      });
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
