<template>
  <div class="layout">
    <Form ref="formValidate" :label-width="150" label-position="right" :model="formValidate" :rules="ruleValidate">

      <FormItem label="endPoint" prop="endPoint">
        <RadioGroup v-model="formValidate.type" type="button">
          <Radio label="ALI_OSS">阿里OSS</Radio>
          <Radio label="MINIO">MINIO</Radio>
        </RadioGroup>
      </FormItem>
      <!--      阿里云存储-->
      <FormItem v-if="formValidate.type==='ALI_OSS'" label="endPoint" prop="endPoint">
        <Input v-model="formValidate.endPoint"/>
      </FormItem>
      <FormItem v-if="formValidate.type==='ALI_OSS'" label="bucketName" class="label-item" prop="bucketName">
        <Input v-model="formValidate.bucketName"/>
      </FormItem>
      <FormItem v-if="formValidate.type==='ALI_OSS'" label="picLocation" prop="bucketName">
        <Input v-model="formValidate.picLocation"/>
      </FormItem>
      <FormItem v-if="formValidate.type==='ALI_OSS'" label="accessKeyId" prop="accessKeyId">
        <Input v-model="formValidate.accessKeyId"/>
      </FormItem>
      <FormItem v-if="formValidate.type==='ALI_OSS'" label="accessKeySecret" prop="accessKeySecret">
        <Input v-model="formValidate.accessKeySecret"/>
      </FormItem>


      <!--      MINIO存储-->

      <FormItem v-if="formValidate.type==='MINIO'" label="访问地址" prop="m_frontUrl">
        <Input v-model="formValidate.m_frontUrl"/>
        <span class="desc">配置MINIO nginx前端访问转发地址，一般为完整域名，例如：https://minio.pickmall.cn</span>
      </FormItem>
      <FormItem v-if="formValidate.type==='MINIO'" label="endpoint" prop="m_endpoint">
        <Input v-model="formValidate.m_endpoint"/>
      </FormItem>

      <FormItem v-if="formValidate.type==='MINIO'" label="accessKey" class="label-item" prop="m_accessKey">
        <Input v-model="formValidate.m_accessKey"/>
      </FormItem>
      <FormItem v-if="formValidate.type==='MINIO'" label="secretKey" prop="bucketName">
        <Input v-model="formValidate.m_secretKey"/>
      </FormItem>
      <FormItem v-if="formValidate.type==='MINIO'" label="bucketName" prop="accessKeyId">
        <Input v-model="formValidate.m_bucketName"/>
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
      formValidate: { // 表单数据
        type: "",
        accessKeyId: "",
        accessKeySecret: "",
        bucketName: "",
        picLocation: "",
        endPoint: "",
        m_endpoint: "",
        m_accessKey: "",
        m_secretKey: "",
        m_bucketName: "",
        m_frontUrl: ""
      },
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
        this.setupSetting()
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
    // 实例化数据
    init() {
      this.res = JSON.parse(this.res);

      this.$set(this, "formValidate", {...this.res});
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
@import "./style.scss";

.label-item {
  display: flex;
}

/deep/ .ivu-input {
  width: 300px !important;
  margin: 0 10px;
}

.ivu-input-wrapper {
  width: 300px;
  margin-right: 10px;
}
.desc {
  margin-left: 5px;
  font-size: 12px;
  color: #999;
}
</style>
