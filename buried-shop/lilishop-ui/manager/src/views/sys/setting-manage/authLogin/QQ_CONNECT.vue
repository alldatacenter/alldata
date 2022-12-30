<template>

  <div class="layout">

    <div class="row" v-if="client == item.client" v-for="(item,index) in  formValidate" :key="index">
      <div class="col">
        <Card :padding="0">
          <!-- app -->
          <div class="icon-item" v-if="item.clientType== 'APP'">
            <img class="icon" src="../../../../assets/setting/app.svg" alt="" srcset="">
          </div>
          <div class="icon-item" v-if="item.clientType== 'PC'">
            <!-- pc -->
            <img class="icon" src="../../../../assets/setting/pc.svg" alt="" srcset="">
          </div>
          <div class="icon-item" v-if="item.clientType== 'WECHAT_MP'">
            <!-- 小程序 -->
            <img class="icon" src="../../../../assets/setting/wechat_mp.svg" alt="" srcset="">
          </div>
          <div class="icon-item" v-if=" item.clientType== 'H5'">
            <!-- h5 -->
            <img class="icon" src="../../../../assets/setting/h5.svg" alt="" srcset="">
          </div>
          <div class='pay-title'> {{way[item.clientType]}}</div>
          <div>

            <Divider orientation="left">登录设置</Divider>
            <div class="pay-list">
              <Form style="width:100%;" ref="formValidate" :model="formValidate" :rules="ruleValidate" :label-width="80">
                <FormItem label="appId" prop="appId">
                  <Input @on-enter="setupSetting" class="label-appkey" v-model="item.appId" />
                </FormItem>
                <FormItem label="appKey" prop="appKey">
                  <Input @on-enter="setupSetting" v-model="item.appKey" />
                </FormItem>

              </Form>
              <Button @click="setupSetting">保存设置</Button>
            </div>

          </div>

        </Card>
      </div>

    </div>

  </div>

</template>
<script>
import { setSetting } from "@/api/index";
import { handleSubmit } from "../setting/validate";
export default {
  data() {
    return {
      ruleValidate: {}, // 验证规则
      way: { // 类型
        APP: "移动应用端",
        H5: "移动端",
        WECHAT_MP: "小程序端",
        PC: "PC端",
      },
      formValidate: {} // 表单数据
    };
  },
  props: ["res", "type"],
  created() {
    this.init();
  },
  methods: {
    submit(name) {
      let that = this;
      if (handleSubmit(that, name)) {
        this.setupSetting();
      }
    },
    // 保存
    setupSetting() {
      this.$Spin.show();
      setTimeout(() => {
        this.$Spin.hide();
        setSetting(this.type, {
          qqConnectSettingItemList: this.formValidate,
        }).then((res) => {
          if (res.success) {
            this.$Message.success("保存成功!");
            this.$Modal.remove();
          } else {
            this.$Message.error("保存失败!");
            this.$Modal.remove();
          }
        });
      }, 3000);
    },
    // 实例化数据
    async init() {
      this.formValidate = JSON.parse(this.res).qqConnectSettingItemList;
      Object.keys(this.formValidate).forEach((item) => {
        this.ruleValidate[item] = [
          {
            required: true,
            message: "请填写必填项",
            trigger: "blur",
          },
        ];
      });
    },
  },
};
</script>

<style lang="scss" scoped>
@import "../setting/style.scss";
.pay-title {
  text-align: center;
  margin: 10px 0;
}

.col {
  width: 100%;
}
.layout {
  padding: 20px;

  display: flex;
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-start;
}
.row {
  width: 350px;
  margin-right: 20px;
  display: flex;
  margin-bottom: 20px;
  /deep/ .ivu-card-body {
    padding: 0 16px !important;
  }
}

.label-item {
  display: flex;
}
.label-item {
  display: flex;
  align-items: center;
}
.pay-list {
  display: flex;
  justify-content: center;
  padding-bottom: 10px;
  flex-direction: column;
  align-items: center;
  /deep/ .ivu-btn {
    width: 100px;
  }
}
.icon-item {
  width: 100%;
  padding: 30px;
  display: flex;
  justify-content: center;
  align-items: center;
  flex-direction: column;
}
.ivu-form-item {
  display: flex;

  align-items: center;
}
.ivu-row {
  width: 100%;
}
.icon {
  width: 100px;
  height: 100px;
}
</style>
