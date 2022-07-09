<template>
  <div class="wrapper">
    <card _Title="账户安全"/>
    <div class="safeList">
      <!-- 密码 -->
      <Row class="safeItem">
        <Col :span="2">
          <Icon size="40" type="md-key"/>
        </Col>
        <Col :span="16">
          <div class="setDivItem">登录密码</div>
          <div class="setDivItem theme">互联网账号存在被盗风险，建议您定期更改密码以保护账户安全。</div>
        </Col>
        <Col :span="4">
          <Button @click="modifyPwd">修改密码</Button>
        </Col>
      </Row>
    </div>
  </div>
</template>

<script>
import { getPwdStatus } from '@/api/account';
export default {
  name: 'AccountSafe',
  data () {
    return {
      pwdStatus: '' // 密码状态
    }
  },
  mounted () {
    this.getPwdStatus()
  },
  methods: {
    // 设置支付密码
    goModifyPwd () {
      this.$router.push({name: 'ModifyPwd', query: { status: 2 }})
    },
    modifyPwd () { // 修改密码
      this.$router.push({name: 'ModifyPwd', query: { status: 1 }})
    },
    // 获取密码状态
    getPwdStatus () {
      getPwdStatus().then(res => {
        if (res) {
          this.pwdStatus = '修改密码'
        } else {
          this.pwdStatus = '设置密码'
        }
      });
    }
  }
}
</script>

<style scoped lang="scss">
  /deep/ .ivu-col-span-2, .ivu-col-span-4 {
    text-align: center;
    color: $theme_color;
  }

  .theme {
    color: $theme_color;
  }

  .setDivItem {
    line-height: 1.75;
  }

  .safeItem {
    border-bottom: 1px solid $border_color;
    padding: 16px 0;

    /deep/ .ivu-col {
      padding: 8px 0;

    }
  }
</style>
