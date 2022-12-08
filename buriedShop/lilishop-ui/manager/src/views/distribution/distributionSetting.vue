<template>
  <div style="background-color: #fff;">

    <Form ref="form" :model="form" :rules="formRule" :label-width="120" style="padding: 10px;">

      <Divider orientation="left">分销设置</Divider>
      <FormItem label="是否开启分销" prop="isOpen">
        <i-switch size="large" v-model="form.isOpen" :true-value="true" :false-value="false">
          <span slot="open">开启</span>
          <span slot="close">关闭</span>
        </i-switch>
      </FormItem>
      <FormItem label="分销关系绑定天数" prop="distributionDay">
        <InputNumber :min="1" :max="365" style="width:100px;" v-model="form.distributionDay"></InputNumber>
      </FormItem>
      <FormItem label="分销结算天数" prop="cashDay">
        <InputNumber :min="1"  :max="365"  style="width:100px;" v-model="form.cashDay"></InputNumber>
      </FormItem>
      <FormItem>
        <Button type="primary" @click="submit">保存</Button>
      </FormItem>
    </Form>
  </div>
</template>

<script>
import { setSetting, getSetting } from "@/api/index";
import { regular } from "@/utils";
export default {
  name: "distributionSetting",
  data() {
    return {
      form: {
        // 添加或编辑表单对象初始化数据
        isOpen: true,
        distributionDay: 0, //分销关系绑定天数
        cashDay: 0, //分销结算天数
      },
      formRule: {
        isOpen: [
          regular.REQUIRED
        ],
        distributionDay: [
          regular.REQUIRED
        ],
        cashDay: [
          regular.REQUIRED
        ],
      }
    };
  },
  methods: {
    // 初始化数据
    init() {
      this.getDataList();
    },
    // 获取分销设置数据
    getDataList() {
      getSetting("DISTRIBUTION_SETTING").then((res) => {
        if (res.success) {
          this.form = res.result;
        }
      });
    },
    // 提交api
    submit() {
      setSetting("DISTRIBUTION_SETTING", this.form).then((res) => {
        if (res.success) {
          this.$Message.success("操作成功");
          this.getDataList();
        }
      });
    },
  },
  mounted() {
    this.init();
  },
};
</script>
<style lang="scss" scoped>
</style>
