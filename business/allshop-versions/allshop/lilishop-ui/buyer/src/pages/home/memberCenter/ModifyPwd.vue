<template>
  <div class="wrapper">
    <card _Title="账户安全"/>
    <!--  手机号验证  -->
    <Form ref="formData" :model="formData" label-position="left" :label-width="100" :rules="ruleInLines"
          v-if="(status == 2 || status == 3) && !showPwd">
      <FormItem label="手机号">
        <div class="phone">1234567890</div>
      </FormItem>
      <FormItem label="图片验证码" prop="picture" style="width: 350px">
        <i-input v-model="formData.picture" size="large" maxlength="4"></i-input>
      </FormItem>
      <FormItem label="短信验证码" prop="msg">
        <i-input v-model="formData.msg" size="large" maxlength="6" style="width: 250px">
          <span slot="append">输入验证码</span>
        </i-input>
      </FormItem>
    </Form>
    <div slot="footer" v-if="(status == 2 || status == 3) && !showPwd" style="text-align: center;width: 50%">
      <Button type="primary" class="ml_10" @click="submitRegister">下一步</Button>
    </div>

    <!--  修改  -->
    <Form ref="formRegister" :model="formRegister" :rules="ruleInline" :label-width="80" v-if="status == 1 && !showPwd">
      <FormItem label="旧密码" prop="password">
        <i-input type="password" v-model="formRegister.password" clearable size="large" placeholder="请输入旧密码"
                 style="width: 300px"
                 maxlength="12">
          <Icon type="md-lock" slot="prepend"></Icon>
        </i-input>
      </FormItem>
      <FormItem label="新密码" prop="newPassword">
        <i-input type="password" v-model="formRegister.newPassword" clearable size="large" placeholder="请输入新密码"
                 style="width: 300px"
                 maxlength="12">
          <Icon type="md-lock" slot="prepend"></Icon>
        </i-input>
      </FormItem>
      <FormItem label="确认密码" prop="againPassword">
        <i-input type="password" v-model="formRegister.againPassword" clearable size="large" placeholder="请输入确认密码"
                 style="width: 300px"
                 maxlength="12">
          <Icon type="md-lock" slot="prepend"></Icon>
        </i-input>
      </FormItem>

    </Form>
    <div slot="footer" v-if="status == 1 && !showPwd" style="width: 50%;text-align: center">
      <Button type="primary" class="ml_10" @click="handleRegist">修改</Button>
    </div>
    <!-- 设置&修改的第二种情况   -->
    <Form ref="formDataUpdate" :model="formDataUpdate" label-position="left" :label-width="100" :rules="ruleIn"
          v-if="showPwd">
      <FormItem label="新密码" prop="newPassword">
        <i-input type="password" v-model="formDataUpdate.newPassword" clearable size="large" placeholder="请输入新密码"
                 style="width: 300px"
                 maxlength="12">
          <Icon type="md-lock" slot="prepend"></Icon>
        </i-input>
      </FormItem>
      <FormItem label="确认密码" prop="againPassword">
        <i-input type="password" v-model="formDataUpdate.againPassword" clearable size="large" placeholder="请输入确认密码"
                 style="width: 300px"
                 maxlength="12">
          <Icon type="md-lock" slot="prepend"></Icon>
        </i-input>
      </FormItem>
    </Form>
    <div slot="footer" v-if="showPwd" style="text-align: center;width: 50%">
      <Button type="primary" class="ml_10" @click="setPassword">设置</Button>
    </div>
  </div>
</template>

<script>
import {
  setPwd,
  editPwd
} from '@/api/account';
import {md5} from '@/plugins/md5.js'

export default {
  name: 'modifyPwd',
  data () {
    return {
      // 1为修改    2为设置     3为提交校验的下一步
      status: '',
      showPwd: false, // 显示密码
      formData: { // 验证表单
        picture: '',
        msg: ''
      },
      formDataUpdate: { // 直接输入新密码表单
        newPassword: '',
        againPassword: ''
      },
      formRegister: { // 第三步 新密码表单
        password: '',
        againPassword: '',
        newPassword: ''
      },
      ruleInLines: { // 验证规则
        picture: [
          {required: true, message: '请输入图片验证码', trigger: 'blur'}
        ],
        msg: [
          {required: true, message: '请输入短信验证码', trigger: 'blur'}
        ]
      },
      ruleIn: { // 验证规则
        newPassword: [
          {required: true, message: '请输入新密码', trigger: 'blur'},
          {type: 'string', min: 6, message: '密码不能少于6位'}
        ],
        againPassword: [
          {required: true, message: '请输入新密码', trigger: 'blur'},
          {type: 'string', min: 6, message: '密码不能少于6位'}
        ]
      },
      ruleInline: { // 验证规则
        password: [
          {required: true, message: '请输入旧密码', trigger: 'blur'}
        ],
        newPassword: [
          {required: true, message: '请输入新密码', trigger: 'blur'},
          {type: 'string', min: 6, message: '密码不能少于6位'}
        ],
        againPassword: [
          {required: true, message: '请确认新密码', trigger: 'blur'},
          {type: 'string', min: 6, message: '密码不能少于6位'}
        ]
      }
    }
  },
  mounted () {
    this.status = this.$route.query.status
  },
  methods: {
    // 修改
    handleRegist () {
      this.$refs['formRegister'].validate((valid) => {
        if (valid) {
          const {newPassword, againPassword, password} = this.formRegister
          if (newPassword !== againPassword) {
            this.$Message.error({
              content: '新旧密码不一致'
            });
            return
          }
          const params = {newPassword, password}
          params.newPassword = md5(newPassword)
          params.password = md5(password)
          editPwd(params).then(res => {
            if (res.message === 'success' && res.result) {
              this.$Message.success('修改密码成功');
              this.$router.push('/home')
            }
          });
        }
      })
    },
    // 提交验证
    submitRegister () {
      this.$refs['formData'].validate((valid) => {
        if (valid) {
          this.showPwd = true
        }
      })
    },
    // 设置密码
    setPassword () {
      this.$refs['formDataUpdate'].validate((valid) => {
        if (valid) {
          const {newPassword, againPassword} = this.formDataUpdate
          if (newPassword === '') {
            this.$Message.error({
              content: '请输入密码'
            });
            return
          }
          if (newPassword !== againPassword) {
            this.$Message.error({
              content: '新旧密码不一致'
            });
            return
          }
          const params = {password: newPassword}
          params.password = md5(newPassword)
          setPwd(params).then(res => {
            if (res.message === 'success' && res.result) {
              this.$Message.success('支付密码设置成功');
              this.$router.push('/home')
            }
          });
        }
      })
    }
  }
}
</script>

<style scoped lang="scss">
  .wrapper {
    text-align: center;
  }
  .phone {
    text-align: left;
  }
</style>
