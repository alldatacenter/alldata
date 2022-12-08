<template>
  <Card>
    <div style="margin-top: 0px">
      <div class="sign-name">
        {{id ? '修改签名' : '新增签名'}}
      </div>
      <Form ref="form" :model="form" :label-width="100" :rules="formValidate">
        <FormItem label="签名" prop="signName">
          <Input v-model="form.signName" maxlength="12" clearable style="width: 28%"
                 placeholder="仅限2-12个字符，建议使用App应用名称或是网站名/公司名"/>
        </FormItem>
        <FormItem label="签名来源" prop="signSource">
          <Select v-model="form.signSource" placeholder="请选择签名来源" style="width: 28%">
            <Option :value="0">企事业单位的全称或简称</Option>
            <Option :value="1">工信部备案网站的全称或简称</Option>
            <Option :value="2">App应用的全称或简称</Option>
            <Option :value="3">公众号或小程序的全称或简称</Option>
            <Option :value="4">电商平台店铺名的全称或简称</Option>
            <Option :value="5">商标名的全称或简称</Option>
          </Select>
        </FormItem>
        <div class="div-remark div-remark-first">
          签名来源选择工信部备案网站的全称或简称时，请在说明中添加网站域名，加快审核速度；
        </div>
        <div class="div-remark div-remark-bottom">
          如果选择APP应用的全称或简称或公众号或小程序的全称或简称，则网站、APP、小程序或公众号必须已上线；
        </div>
        <FormItem label="证明文件">
          <div style="float: left">
            <upload-pic-thumb
              v-model="form.businessLicense"
              :multiple="false"
              :max-size="2048"
            >
            </upload-pic-thumb>
          </div>
          <div style="float: left;margin-left: 20px">
            <upload-pic-thumb
              v-model="form.license"
              :max-size="2048"
              :multiple="false"
            ></upload-pic-thumb>
          </div>
        </FormItem>
        <div class="div-remark div-remark-first">
          第一张为营业执照，第二张为授权委托书，请上传签名归属方的企事业单位的企业营业执照、组织机构代码证、税务登记证三证合一的证件及授权委托书
        </div>
        <div class="div-remark div-remark-bottom">
          支持jpg、png、gif、jpeg格式的图片，每张图片不大于2MB
        </div>
        <FormItem label="申请说明" prop="remark">
          <Input v-model="form.remark" clearable type="textarea" style="width: 50%" maxlength="100"
            :autosize="{maxRows:4,minRows: 4}" show-word-limit
            placeholder="请描述您的业务使用场景，不超过100字符；如：验证码、双十一大促营销"/>
        </FormItem>
      </Form>
      <div class="footer">
        <Button type="primary" :loading="submitLoading" @click="addSignSubmit">提交</Button>
      </div>
    </div>
  </Card>
</template>

<script>
  import * as API_Setting from "@/api/setting.js";
  import uploadPicThumb from "@/views/my-components/lili/upload-pic-thumb";

  export default {
    name: "smsSign",
    components: {
      uploadPicThumb
    },
    data() {
      return {
        id: 0, // 签名id
        form: { // 表单
          signName: "",
          businessLicense: "",
          license: "",
        },
        loading: false, // 加载状态
        formValidate: { // 验证规则
          signName: [{required: true, message: "签名名称不能为空", trigger: "blur"}],
          remark: [{required: true, message: "申请说明不能为空", trigger: "blur"}],
          businessLicense: [{required: true, message: " ", trigger: "blur"}],
        },
        submitLoading: false //提交加载状态
      }

    },
    methods: {
      // 初始化数据
      init() {
        this.id = this.$route.query.id;
        if (this.id != undefined) {
          this.getSmsSignDetail();
        }
      },
      //添加短信模板
      addSignSubmit() {
        //校验证件信息
        if (this.form.businessLicense == "" || this.form.license == "") {
          this.$Message.error("请完善证件信息");
          return
        }
        //校验签名来源
        if (this.form.signSource == "") {
          this.$Message.error("请选择签名来源");
          return
        }
        this.$refs.form.validate((valid) => {
          if (valid) {
            this.loading = true;
            //新增
            if (this.id == undefined) {
              API_Setting.addSmsSign(this.form).then(res => {
                this.loading = false;
                if (res.success) {
                  this.$Message.success('添加成功');
                  this.$router.back()
                }
              }).catch(() => {
                this.loading = false;
              });
            } else {
              API_Setting.editSmsSign(this.form).then(res => {
                this.loading = false;
                if (res.success) {
                  this.$Message.success('修改成功');
                  this.$router.back()
                }
              }).catch(() => {
                this.loading = false;
              });
            }

          }
        })
      },
      //查询短信签名详细
      getSmsSignDetail() {
        API_Setting.smsSignDetail(this.id).then(res => {
          this.loading = false;
          if (res.success) {
            this.form = res.result
          }
        })
      }
    },
    mounted() {
      this.init();
    },
  };
</script>
<style lang="scss" scoped>
   .sign-name {
    margin-top: 5px;
    margin-left: 20px;
    font-size: 16px;
    margin-bottom: 30px;
    color: #333;
  }

  .div-remark {
    margin-left: 100px;
    margin-bottom: 2px;
    color: #999;
  }

  .div-remark-first {
    margin-top: -17px;
  }

  .div-remark-bottom {
    margin-bottom: 8px;
  }

  .footer {
    margin-left: 100px;
  }
</style>
