<template>
  <div class="layout">
    <Form ref="formValidate" :label-width="150" label-position="right" :model="formValidate" :rules="ruleValidate">
      <FormItem label="消费1元赠送积分" prop="consumer">
        <Input type="number" v-model="formValidate.consumer">
          <span slot="append">积分</span>
        </Input>

      </FormItem>
      <!-- <FormItem label="积分抵扣付款" prop="money">
        <Input type="number" v-model="formValidate.money">
          <span slot="prepend">积分</span>
          <span slot="append"> = 1 人民币</span>
        </Input>

      </FormItem> -->

      <FormItem label="注册账号" prop="register">
        <Input type="number" v-model="formValidate.register">
          <span slot="append">积分</span>
        </Input>
      </FormItem>
      <!-- <FormItem label="登录" class="label-item" prop="login">
        <Input type="number" v-model="formValidate.login">

        <span slot="append">积分</span>
        </Input>

      </FormItem> -->

      <FormItem label="每日签到积分" prop="signIn">
        <Input type="number" v-model="formValidate.signIn">
          <span slot="append">积分</span>
        </Input>

      </FormItem>
      <FormItem label="订单评价赠送积分" prop="comment">
        <Input type="number" v-model="formValidate.comment">
          <span slot="append">积分</span>
        </Input>

      </FormItem>

      <FormItem class="label-item" v-for="(point,index) in  formValidate.pointSettingItems" :key="index"
                :label="'签到设置'+(index+1)">
        <div class="label-item">

          <InputNumber :min="1" v-model="point.day"></InputNumber>

          <InputNumber :min="0" v-model="point.point"></InputNumber>

          <Button ghost type="error" @click="delSign(point,index)">删除</Button>
          <span class="ml_10">签到<span class="theme_color">{{ point.day }}</span>天，赠送<span
            class="theme_color">{{ point.point }}</span>积分</span>
        </div>

      </FormItem>
      <FormItem label="操作：">
        <Button @click="addSign">新增签到</Button>
      </FormItem>
      <div class="label-btns">
        <Button type="primary" @click="submit('formValidate')">保存</Button>
      </div>
    </Form>
  </div>
</template>

<script>
import {setSetting} from "@/api/index";
import {handleSubmit} from "./validate";

export default {
  data() {
    return {
      ruleValidate: {}, // 验证规则
      formValidate: {}, // 表单数据
    };
  },
  props: ["res", "type"],
  created() {
    this.init();
  },
  methods: {
    // 保存
    submit(name) {
      let that = this;
      if (handleSubmit(that, name)) {
        this.setupSetting();
      }
    },
    // 保存设置
    setupSetting() {
      setSetting(this.type, this.formValidate).then((res) => {
        if (res.success) {
          this.$Message.success("保存成功!");
        } else {
          this.$Message.error("保存失败!");
        }
      });
    },
    delSign(item, index) {
      this.formValidate.pointSettingItems.splice(index, 1);
    },
    addSign() {
      if (this.formValidate.pointSettingItems.length >= 4) {
        this.$Message.error({
          content: "最多设置4项签到设置",
        });
        return false;
      }
      this.formValidate.pointSettingItems.push({
        point: "0",
        day: this.formValidate.pointSettingItems.length,
      });
    },
    // 实例化数据
    init() {
      this.res = JSON.parse(this.res);
      Object.keys(this.res).map((item) => {
        if (item == "pointSettingItems") {
          return false;
        }
        this.res[item] += "";
      });

      this.$set(this, "formValidate", {...this.res});

      Object.keys(this.formValidate).forEach((item) => {
        this.ruleValidate[item] = [
          {
            required: true,
            message: "请填写必填项",
            trigger: "blur",
          },
          {
            validator: (rule, value, callback) => {
              if (value < 0) {
                callback(new Error("不能输入负数！"));
              } else {
                callback();
              }
            },
            trigger: "change",
          },
        ];
      });
    },
  },
};
</script>

<style lang="scss" scoped>
@import "./style.scss";

.label-item {
  display: flex;

  > .ivu-input-number {
    width: 100px;
    margin-right: 5px;
  }

  > .ivu-input-number:nth-last-of-type(1) {
    width: 150px;
    margin-right: 5px;
  }

  > .ivu-input {
    width: 100px;
    margin: 0 10px;
  }
}

/deep/ .ivu-input {
  width: 70px !important;
}

.ivu-input-wrapper {
  width: 70px;
  margin-right: 10px;
}

.label-btns {
  /deep/ .ivu-btn {
    margin-right: 10px;
  }
}
</style>
