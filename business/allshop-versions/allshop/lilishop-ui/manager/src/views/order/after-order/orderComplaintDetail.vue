<template>
  <div class="search">
    <Card>
      <div class="main-content">
        <div class="div-flow-left">
          <div class="div-form-default">
            <h3>投诉信息</h3>
            <dl>
              <dt>投诉商品</dt>
              <dd>{{complaintInfo.goodsName}}</dd>
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
              <dd>{{complaintInfo.createTime}}</dd>
            </dl>
            <dl>
              <dt>投诉主题</dt>
              <dd>{{complaintInfo.complainTopic}}</dd>
            </dl>
            <dl>
              <dt>投诉内容</dt>
              <dd>{{complaintInfo.content}}</dd>
            </dl>
            <dl>
              <dt>投诉凭证</dt>
              <dd v-if="images == ''">
                暂无投诉凭证
              </dd>
              <dd v-else>
                <div class="div-img" v-for="(item, index) in images" :key="index">
                  <img class="complain-img" :src=item>
                </div>
              </dd>
            </dl>
          </div>
          <div class="div-form-default" v-if="complaintInfo.appealContent">
            <h3>商家申诉信息</h3>
            <dl>
              <dt>申诉时间</dt>
              <dd>{{complaintInfo.appealTime}}</dd>
            </dl>
            <dl>
              <dt>申诉内容</dt>
              <dd>{{complaintInfo.appealContent}}</dd>
            </dl>
            <dl>
              <dt>申诉凭证</dt>
              <dd v-if="complaintInfo.appealImagesList.length == 0">
                暂无申诉凭证
              </dd>
              <dd v-else>
                <div class="div-img" v-for="(item, index) in complaintInfo.appealImagesList" :key="index">
                  <img class="complain-img" :src="item">
                </div>
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
                      <span v-if="item.owner == 'STORE'">商家[{{ item.createTime }}]</span>
                      <span v-if="item.owner == 'BUYER'">买家[{{ item.createTime }}]</span>
                      <span v-if="item.owner == 'PLATFORM'">平台[{{ item.createTime }}]</span>
                      {{ item.content }}
                    </p>
                  </div>

                </dd>
              </dl>
              <dl v-if="complaintInfo.complainStatus!='COMPLETE'">
                <dt>发送对话</dt>
                <dd>
                  <Input
                    v-model="params.content"
                    type="textarea"
                    maxlength="200"
                    :rows="4"
                    clearable
                    style="width:260px"
                  />
                </dd>
              </dl>
              <dl v-if="complaintInfo.complainStatus != 'COMPLETE'">
                <dt></dt>
                <dd>
                  <div style="text-align: right;width: 45%;margin-top: 10px">
                    <Button type="primary" :loading="submitLoading" @click="handleSubmit" style="margin-left: 5px">
                      回复
                    </Button>
                    <Button type="primary" :loading="submitLoading" @click="returnDataList" style="margin-left: 5px">
                      返回列表
                    </Button>
                  </div>
                </dd>
              </dl>
            </div>
          <div class="div-form-default" v-if="complaintInfo.complainStatus == 'COMPLETE'">
            <h3>仲裁结果</h3>
            <dl>
              <dt>仲裁意见</dt>
              <dd>
                {{complaintInfo.arbitrationResult}}
              </dd>
            </dl>
          </div>
          <div class="div-form-default" v-if="complaintInfo.complainStatus != 'COMPLETE'">
            <h3>平台仲裁</h3>
            <dl v-if="arbitrationResultShow == true">
              <dt>仲裁</dt>
              <dd>
                <Input v-model="arbitrationParams.arbitrationResult" type="textarea" maxlength="200" :rows="4" clearable style="width:260px" />
              </dd>
            </dl>
            <dl>
              <dt></dt>
              <dd style="text-align:right;display:flex; justify-content: space-between;">
                <Button type="primary" ghost :loading="submitLoading" v-if="arbitrationResultShow == false" @click="arbitrationHandle">
                  直接仲裁结束投诉流程
                </Button>
                <Button :loading="submitLoading" v-if="complaintInfo.complainStatus == 'NEW'" @click="handleStoreComplaint">
                  交由商家申诉
                </Button>
                <Button type="primary" :loading="submitLoading" v-if="arbitrationResultShow == true" @click="arbitrationHandleSubmit">
                  提交仲裁
                </Button>
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
                <a>{{complaintInfo.goodsName}}</a><br>
                <span>￥{{complaintInfo.goodsPrice}} * {{complaintInfo.num}}(数量)</span>
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
                {{complaintInfo.orderSn}}
              </dd>
            </dl>
            <dl>
              <dt>
                下单时间
              </dt>
              <dd>
                {{complaintInfo.createTime}}
              </dd>
            </dl>
            <dl>
              <dt>
                订单金额
              </dt>
              <dd>
                {{complaintInfo.orderPrice}}
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
                {{complaintInfo.consigneeName}}
              </dd>
            </dl>
            <dl>
              <dt>
                收货地址
              </dt>
              <dd>
                {{complaintInfo.consigneeAddressPath}}
              </dd>
            </dl>
            <dl>
              <dt>
                收货人手机
              </dt>
              <dd>
                {{complaintInfo.consigneeMobile}}
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

export default {
  name: "orderComplaintDetail",
  data() {
    return {
      id: 0, // 投诉id
      complaintInfo: "", // 投诉信息
      images: [], //会员申诉图片
      appealImages: [], //商家申诉的图片
      submitLoading: false, // 添加或编辑提交状态
      //管理端回复内容
      params: {
        content: "",
        complainId: "",
      },
      //仲裁结果
      arbitrationParams: {
        arbitrationResult: "",
      },
      //是否显示仲裁框
      arbitrationResultShow: false,
    };
  },
  methods: {
    // 交给商家申诉
    handleStoreComplaint() {
      API_Order.storeComplain({
        complainStatus: "WAIT_APPEAL",
        complainStatus: "WAIT_APPEAL",
        complainId: this.complaintInfo.id,
      }).then((res) => {
        if (res.success) {
          this.$Message.success("操作成功");
          this.getDetail();
        }
      });
    },
    // 初始化数据
    init() {
      this.getDataList();
    },
    // 获取投诉详情
    getDetail() {
      this.loading = true;
      API_Order.getOrderComplainDetail(this.id).then((res) => {
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
    //仲裁
    arbitrationHandle() {
      this.arbitrationResultShow = true;
    },
    //仲裁结果提交
    arbitrationHandleSubmit() {
      if (this.arbitrationParams.arbitrationResult == "") {
        this.$Message.error("请填写仲裁内容");
        return;
      }
      API_Order.orderComplete(this.id, this.arbitrationParams).then((res) => {
        this.submitLoading = false;
        if (res.success) {
          this.$Message.success("仲裁成功");
          this.arbitrationParams.arbitrationResult = "";
          this.getDetail();
        }
      });
      this.arbitrationResultShow = true;
    },
    //回复
    handleSubmit() {
      if (this.params.content == "") {
        this.$Message.error("请填写对话内容");
        return;
      }
      this.params.complainId = this.id;
      API_Order.addOrderCommunication(this.params).then((res) => {
        this.submitLoading = false;
        if (res.success) {
          this.$Message.success("对话成功");
          this.params.content = "";
          this.getDetail();
        }
      });
    },
  },
  mounted() {
    this.id = this.$route.query.id;
    this.getDetail();
  },
};
</script>
<style lang="scss" scoped>

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
