<template>
  <div class="wrapper">
    <card _Title="用户信息" />
    <Form :model="formItem" :rules="rules" ref="form" :label-width="80">
      <FormItem label="头像">
        <Avatar v-if="formItem.face" :src="formItem.face" size="80"/>
        <Avatar v-else icon="ios-person" size="80"/>
        <Upload
          :show-upload-list="false"
          :on-success="handleSuccess"
          :format="['jpg','jpeg','png']"
          :action="action"
          :headers="accessToken"
          >
          <Button class="mt_10">上传头像</Button>
        </Upload>
      </FormItem>
      <FormItem label="昵称" prop="nickName">
        <Input class="wrapper-user-name" style="width:187px" v-model="formItem.nickName" placeholder="" />
      </FormItem>

      <FormItem label="生日">
        <DatePicker type="date" placeholder="选择您的生日" v-model="formItem.birthday"></DatePicker>
      </FormItem>
      <FormItem label="性别">
        <RadioGroup v-model="formItem.sex" type="button" button-style="solid">
          <Radio :label="1">男</Radio>
          <Radio :label="0">女</Radio>
        </RadioGroup>
      </FormItem>
      <FormItem>
        <Button type="primary" @click="save">确认修改</Button>

      </FormItem>
    </Form>
  </div>
</template>

<script>
import storage from '@/plugins/storage.js';
import { editMemberInfo } from '@/api/account.js';
import { commonUrl } from '@/plugins/request.js';
export default {
  name: 'Profile',
  data () {
    return {
      rules: { // 验证规则
        nickName: [{required: true, message: '用户昵称不能为空'}, { max: 16, message: '用户昵称不能超过15个字符' }]
      },
      formItem: {}, // 表单数据
      action: commonUrl + '/common/common/upload/file', // 上传接口
      accessToken: {} // 验证token
    }
  },
  mounted () {
    this.formItem = JSON.parse(storage.getItem('userInfo'))
    this.accessToken.accessToken = storage.getItem('accessToken');
  },
  methods: {
    save () { // 保存
      this.$refs.form.validate(valid => {
        if (valid) {
          let params = {
            birthday: this.$options.filters.unixToDate(this.formItem.birthday / 1000, 'yyyy-MM-dd'),
            face: this.formItem.face,
            nickName: this.formItem.nickName,
            sex: this.formItem.sex
          }
          editMemberInfo(params).then(res => {
            if (res.success) {
              this.$Message.success('修改个人资料成功')
              storage.setItem('userInfo', res.result)
            }
          })
        }
      })
    },
    handleSuccess (res, file) { // 上传成功
      this.$set(this.formItem, 'face', res.result)
    },
  }

}
</script>

<style scoped lang="scss">

</style>
