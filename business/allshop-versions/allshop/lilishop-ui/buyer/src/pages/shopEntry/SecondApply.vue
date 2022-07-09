<template>
  <div class="photo-msg">
    <Form ref="secondForm" :model="form" :rules="rules" :label-width="140">
      <h4>基础信息</h4>
      <FormItem prop="settlementBankAccountName" label="银行开户名">
        <Input
          type="text"
          v-model="form.settlementBankAccountName"
          placeholder="请填写银行开户名称"
        />
      </FormItem>
      <FormItem prop="settlementBankAccountNum" label="银行账号">
        <Input
          type="text"
          v-model="form.settlementBankAccountNum"
          placeholder="请填写银行账号"
        />
      </FormItem>
      <FormItem prop="settlementBankBranchName" label="开户银行支行名称">
        <Input
          type="text"
          v-model="form.settlementBankBranchName"
          placeholder="请填写开户银行支行名称"
        />
      </FormItem>
      <FormItem prop="settlementBankJointName" label="支行联行号">
        <Input
          type="text"
          v-model="form.settlementBankJointName"
          placeholder="请填写支行联行号"
        />
      </FormItem>

      <FormItem>
        <Button @click="$emit('change', 0)">返回</Button>
        <Button type="primary" :loading="loading" @click="next"
          >填写其他信息</Button
        >
      </FormItem>
    </Form>
  </div>
</template>
<script>
import { applySecond } from '@/api/shopentry';
export default {
  props: {
    content: {
      default: {},
      type: Object
    }
  },
  data () {
    return {
      loading: false, // 加载状态
      form: {}, // 表单数据
      rules: { // 验证规则
        settlementBankAccountName: [
          { required: true, message: '请填写银行开户名称' }
        ],
        settlementBankAccountNum: [
          { required: true, message: '请填写银行账号' }
        ],
        settlementBankBranchName: [
          { required: true, message: '请填写开户银行支行名称' }
        ],
        settlementBankJointName: [
          { required: true, message: '请填写支行联行号' }
        ]
      }
    };
  },
  methods: {
    // 下一步
    next () {
      this.$refs.secondForm.validate((valid) => {
        if (valid) {
          this.loading = true;
          applySecond(this.form)
            .then((res) => {
              this.loading = false;
              if (res.success) this.$emit('change', 2);
            })
            .catch(() => {
              this.loading = false;
            });
        } else {
          console.log('error');
        }
      });
    }
  },
  mounted () {
    if (this.content != {}) {
      this.form = JSON.parse(JSON.stringify(this.content));
      this.$forceUpdate();
    }
    this.$refs.secondForm.resetFields()
  }
};
</script>
<style lang="scss" scoped>
h4 {
  margin-bottom: 10px;
  padding: 0 10px;
  border: 1px solid #ddd;
  background-color: #f8f8f8;
  font-weight: bold;
  color: #333;
  font-size: 14px;
  line-height: 40px;
  text-align: left;
}
.ivu-input-wrapper {
  width: 300px;
}
</style>
