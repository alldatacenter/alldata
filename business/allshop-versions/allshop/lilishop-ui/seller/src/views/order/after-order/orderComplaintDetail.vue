<template>
  <div class="search">
    <Card>
      <div class="main-content">
        <div class="div-flow-left">
          <div class="div-form-default">
            <h3>投诉信息</h3>
            <dl>
              <dt>投诉商品</dt>
              <dd>{{ complaintInfo.goodsName }}</dd>
            </dl>
            <dl>
              <dt>投诉状态</dt>
              <dd v-if="complaintInfo.complainStatus =='NEW'">新投诉</dd>
              <dd v-if="complaintInfo.complainStatus =='CANCEL'">已撤销</dd>
              <dd v-if="complaintInfo.complainStatus =='WAIT_APPEAL'">待申诉</dd>
              <dd v-if="complaintInfo.complainStatus =='COMMUNICATION'">对话中</dd>
              <dd v-if="complaintInfo.complainStatus =='WAIT_ARBITRATION'">等待仲裁</dd>
              <dd v-if="complaintInfo.complainStatus =='COMPLETE'">已完成</dd>
            </dl>
            <dl>
              <dt>投诉时间</dt>
              <dd>{{ complaintInfo.createTime }}</dd>
            </dl>
            <dl>
              <dt>投诉主题</dt>
              <dd>{{ complaintInfo.complainTopic }}</dd>
            </dl>
            <dl>
              <dt>投诉内容</dt>
              <dd>{{ complaintInfo.content }}</dd>
            </dl>
            <dl>
              <dt>投诉凭证</dt>
              <dd v-if="images === ''">
                暂无投诉凭证
              </dd>
              <dd v-else>
                <div class="div-img" v-for="(item, index) in images" :key="index">
                  <img class="complain-img" :src=item>
                </div>
              </dd>
            </dl>
          </div>
          <div class="div-form-default" v-if="complaintInfo.complainStatus !== 'WAIT_APPEAL'">
            <h3>商家申诉信息</h3>
            <dl>
              <dt>申诉时间</dt>
              <dd>{{ complaintInfo.appealTime }}</dd>
            </dl>
            <dl>
              <dt>申诉内容</dt>
              <dd>{{ complaintInfo.appealContent }}</dd>
            </dl>
            <dl>
              <dt>申诉凭证</dt>
              <dd v-if="appealImages == ''">
                暂无申诉凭证
              </dd>
              <dd v-else>
                <div class="div-img" v-for="(item, index) in appealImages" :key="index">
                  <img class="complain-img" :src=item>
                </div>
              </dd>
            </dl>
          </div>

          <div class="div-form-default" v-if="complaintInfo.complainStatus === 'WAIT_APPEAL'">
            <h3>商家申诉</h3>
            <dl>
              <dt>申诉内容</dt>
              <dd>
                <Input v-model="appeal.appealContent" type="textarea" maxlength="200" :rows="4" clearable style="width:260px" />
              </dd>
            </dl>
            <dl>
              <dt>申诉凭证</dt>
              <dd>
                <div class="complain-upload-list" :key="index" v-for="(item,index) in appeal.appealImages">
                  <template v-if="item.status === 'finished'">
                    <img class="complain-img" :src="item.url">
                    <div class="complain-upload-list-cover">
                      <Icon type="ios-eye-outline" @click.native="handleView(item.url)"></Icon>
                      <Icon type="ios-trash-outline" @click.native="handleRemove(item)"></Icon>
                    </div>
                  </template>
                  <template v-else>
                    <Progress v-if="item.showProgress" :percent="item.percentage" hide-info></Progress>
                  </template>
                </div>
                <Upload ref="upload" :show-upload-list="false" :on-format-error="handleFormatError" :action="uploadFileUrl" :headers="accessToken" :on-success="handleSuccessGoodsPicture"
                  :format="['jpg','jpeg','png']" :max-size="1024" :on-exceeded-size="handleMaxSize" :before-upload="handleBeforeUpload" multiple type="drag"
                  style="display: inline-block;width:58px;">
                  <div style="width: 58px;height:58px;line-height: 58px;">
                    <Icon type="ios-camera" size="20"></Icon>
                  </div>
                </Upload>
                <Modal title="View Image" v-model="visible">
                  <img :src="imgName" v-if="visible" style="width: 100%">
                </Modal>
              </dd>
            </dl>
            <dl>
              <dt></dt>
              <dd>
                <Button type="primary" :loading="submitLoading" @click="appealSubmit()" style="margin-left: 5px">
                  提交申诉
                </Button>
              </dd>
            </dl>
          </div>

          <div class="div-form-default">
            <h3>对话详情</h3>
            <dl>
              <dt>对话记录</dt>
              <dd>
                <div class="div-content">
                  <p v-for="(item, index) in complaintInfo.orderComplaintCommunications" :key="index">
                    <span v-if="item.owner === 'STORE'">商家[{{ item.createTime }}]</span>
                    <span v-if="item.owner === 'BUYER'">买家[{{ item.createTime }}]</span>
                    <span v-if="item.owner === 'PLATFORM'">平台[{{ item.createTime }}]</span>
                    {{ item.content }}
                  </p>
                </div>

              </dd>
            </dl>
            <dl v-if="complaintInfo.complainStatus!='COMPLETE'">
              <dt>发送对话</dt>
              <dd>
                <Input v-model="params.content" type="textarea" maxlength="200" :rows="4" clearable style="width:260px" />
              </dd>
            </dl>
            <dl>
              <dt></dt>
              <dd v-if="complaintInfo.complainStatus != 'COMPLETE'">
                <div style="text-align: right;width: 45%;margin-top: 10px">
                  <Button type="primary" :loading="submitLoading" @click="handleSubmit" style="margin-left: 5px">
                    回复
                  </Button>
                  <Button type="default" :loading="submitLoading" @click="returnDataList" style="margin-left: 5px">
                    返回列表
                  </Button>
                </div>
              </dd>
            </dl>
          </div>
          <div class="div-form-default" v-if="complaintInfo.complainStatus === 'COMPLETE'">
            <h3>仲裁结果</h3>
            <dl>
              <dt>仲裁意见</dt>
              <dd>
                {{ complaintInfo.arbitrationResult }}
              </dd>
            </dl>
          </div>
        </div>
        <div class="div-flow-center">

        </div>
        <div class="div-flow-right">
          <div class="div-form-default">
            <h3>相关商品交易信息</h3>
            <dl>
              <dt>
                <img :src="complaintInfo.goodsImage" height="60px">
              </dt>
              <dd>
                <a>{{ complaintInfo.goodsName }}</a><br>
                <span>￥{{ complaintInfo.goodsPrice | unitPrice }} * {{ complaintInfo.num }}(数量)</span>
              </dd>
            </dl>

          </div>
          <div class="div-form-default">
            <h3>订单相关信息</h3>
            <dl>
              <dt>
                订单编号
              </dt>
              <dd>
                {{ complaintInfo.orderSn }}
              </dd>
            </dl>
            <dl>
              <dt>
                下单时间
              </dt>
              <dd>
                {{ complaintInfo.orderTime }}
              </dd>
            </dl>
            <dl>
              <dt>
                订单金额
              </dt>
              <dd>
                {{ complaintInfo.orderPrice | unitPrice('￥')}}
              </dd>
            </dl>

          </div>
          <div class="div-form-default">
            <h3>收件人信息</h3>
            <dl>
              <dt>
                收货人
              </dt>
              <dd>
                {{ complaintInfo.consigneeName }}
              </dd>
            </dl>
            <dl>
              <dt>
                收货地址
              </dt>
              <dd>
                {{ complaintInfo.consigneeAddressPath }}
              </dd>
            </dl>
            <dl>
              <dt>
                收货人手机
              </dt>
              <dd>
                {{ complaintInfo.consigneeMobile }}
              </dd>
            </dl>
          </div>

        </div>
      </div>
    </Card>
  </div>
</template>

<script>
import * as API_Order from "@/api/order";
import { uploadFile } from "@/libs/axios";
export default {
  name: "orderComplaint",
  data() {
    return {
      //展示图片层
      visible: false,
      //上传图片路径
      uploadFileUrl: uploadFile,
      accessToken: "", // 验证token
      id: 0, // 投诉单id
      complaintInfo: "", // 投诉信息
      images: [], //会员申诉图片
      appealImages: [], //商家申诉的图片
      applyAppealImages: [], //商家申诉表单填写的图片
      submitLoading: false, // 添加或编辑提交状态
      //商家回复内容
      params: {
        content: "",
        complainId: "",
      },
      //投诉
      appeal: {
        orderComplaintId: "",
        appealContent: "",
        appealImages: [],
      },
    };
  },
  watch: {
    $route() {
      this.getDetail();
    },
  },
  methods: {
    // 预览图片
    handleView(name) {
      this.imgName = name;
      this.visible = true;
    },
    // 移除回复图片
    handleRemove(file) {
      this.appeal.appealImages = this.appeal.appealImages.filter(
        (i) =>  i.url !== file.url
      );
    },
    // 上传成功回调
    handleSuccessGoodsPicture(res, file) {
      if (file.response) {
        file.url = file.response.result;

        this.appeal.appealImages.push(file);
      }
    },
    // 上传之前钩子
    handleBeforeUpload() {
      const check =
        this.images.images !== undefined && this.images.images.length > 5;
      if (check) {
        this.$Notice.warning({
          title: "Up to five pictures can be uploaded.",
        });
      }
      return !check;
    },
    // 上传格式错误
    handleFormatError(file) {
      this.$Notice.warning({
        title: "图片格式不正确",
        desc:
          "File format of " +
          file.name +
          " is incorrect, please select jpg or png.",
      });
    },
    // 上传大小限制
    handleMaxSize(file) {
      this.$Notice.warning({
        title: "超过文件大小限制",
        desc: "图片不能超过1mb",
      });
    },
    // 获取详情
    getDetail() {
      this.loading = true;
      API_Order.getComplainDetail(this.id).then((res) => {
        this.loading = false;
        if (res.success) {
          this.complaintInfo = res.result;
          this.images = (res.result.images || "").split(",");
          this.appealImages = (res.result.appealImages || "").split(",");
        }
      });
    },
    //返回列表
    returnDataList() {
      this.$router.push({
        name: "orderComplaint",
      });
    },
    //回复
    handleSubmit() {
      if (this.params.content === "") {
        this.$Message.error("请填写对话内容");
        return;
      }
      this.params.complainId = this.id;
      API_Order.addOrderComplaint(this.params).then((res) => {
        this.submitLoading = false;
        if (res.success) {
          this.$Message.success("对话成功");
          this.params.content = "";
          this.getDetail();
        }
      });
    },
    //申诉
    appealSubmit() {

      if (this.appeal.appealContent === "") {
        this.$Message.error("请填写内容");
        return;
      }
      this.appeal.appealImages = this.appeal.appealImages.map(item=> item.url)
      this.appeal.orderComplaintId = this.id;
      API_Order.appeal(this.appeal).then((res) => {
        this.submitLoading = false;
        if (res.success) {
          this.$Message.success("申诉成功");
          this.getDetail();
        }
      });
    },
  },
  mounted () {
    this.id = this.$route.query.id;
    this.getDetail();
    this.accessToken = {
      accessToken: this.getStore("accessToken"),
    };
  },
  // 如果是从详情页返回列表页，修改列表页keepAlive为true，确保不刷新页面
  beforeRouteLeave(to, from, next){
    if(to.name === 'orderComplaint') {
      to.meta.keepAlive = true
    }
    next()
  }
};
</script>
<style lang="scss" scoped>
/deep/ .ivu-col {
  width: 100% !important;
}

.complain-upload-list {
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
.complain-upload-list img {
  width: 100%;
  height: 100%;
}
.complain-upload-list-cover {
  display: none;
  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;
  right: 0;
  background: rgba(0, 0, 0, 0.6);
}
.complain-upload-list:hover .complain-upload-list-cover {
  display: block;
}
.complain-upload-list-cover i {
  color: #fff;
  font-size: 20px;
  cursor: pointer;
  margin: 0 2px;
}

.main-content {
  min-height: 600px;
  padding: 10px;
}

.div-flow-left {
  width: 49%;
  letter-spacing: normal;
  display: inline-block;
  border-right: solid #f5f5f5 1px;

  .div-form-default {
    width: 97%;

    h3 {
      font-weight: 600;
      line-height: 22px;
      background-color: #f5f5f5;
      padding: 6px 0 6px 12px;
      border-bottom: solid 1px #e7e7e7;
    }

    dl {
      font-size: 0;
      line-height: 30px;
      clear: both;
      padding: 0;
      margin: 0;
      border-bottom: dotted 1px #e6e6e6;
      overflow: hidden;

      dt {
        display: inline-block;
        width: 13%;
        vertical-align: top;
        text-align: right;
        padding: 15px 1% 15px 0;
        margin: 0;
        font-size: 12px;
      }

      dd {
        display: inline-block;
        width: 84%;
        padding: 15px 0 15px 1%;
        margin: 0;
        border-left: 1px solid #f0f0f0;
        font-size: 12px;
      }
    }
  }
}

.div-img {
  width: 130px;
  height: 130px;
  text-align: center;
  float: left;
}

.div-flow-center {
  width: 2%;
  display: inline-block;
}

.div-flow-right {
  width: 49%;
  vertical-align: top;
  word-spacing: normal;
  display: inline-block;

  .div-form-default {
    width: 97%;

    h3 {
      font-weight: 600;
      line-height: 22px;
      background-color: #f5f5f5;
      padding: 6px 0 6px 12px;
      border-bottom: solid 1px #e7e7e7;
    }

    dl {
      font-size: 0;
      line-height: 30px;
      clear: both;
      padding: 0;
      margin: 0;
      border-bottom: dotted 1px #e6e6e6;
      overflow: hidden;

      dt {
        display: inline-block;
        width: 13%;
        vertical-align: top;
        text-align: right;
        padding: 15px 1% 15px 0;
        margin: 0;
        font-size: 12px;
      }

      dd {
        display: inline-block;
        width: 84%;
        padding: 15px 0 15px 1%;
        margin: 0;
        border-left: 1px solid #f0f0f0;
        font-size: 12px;
      }
    }
  }
}

.complain-img {
  width: 120px;
  height: 120px;
  text-align: center;
}

.div-content {
  overflow-y: auto;
  overflow-x: auto;
  height: 150px;
}
</style>
