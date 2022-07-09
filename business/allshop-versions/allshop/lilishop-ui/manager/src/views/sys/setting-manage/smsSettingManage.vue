
<template>
  <Card class="card">

    <div class="cardBox">
      <div class="methodItem">
        <img src="../../../assets/aliyun.png" height="172" width="440"/>
        <h4>阿里云短信</h4></div>
      <div class="bar">
        <div class="status" style="color: rgb(53, 189, 129);">已启用</div>
        <div><a class="links">编辑</a></div>
      </div>
    </div>
    <!-- 阿里云的短信参数 -->
    <Modal
      :title="modalTitle"
      v-model="modalVisible"
      :mask-closable="false"
      :width="500"
    >
      <Form ref="form" :model="form" :label-width="100" :rules="formValidate">
        <FormItem label="accessKeyId" prop="addressName">
          <Input v-model="form.accessKeyId" clearable style="width: 100%"/>
        </FormItem>
        <FormItem label="accessSecret" prop="accessSecret">
          <Input v-model="form.accessSecret" clearable style="width: 100%"/>
        </FormItem>
        <FormItem label="regionId" prop="regionId">
          <Input v-model="form.regionId" clearable style="width: 100%"/>
        </FormItem>
        <FormItem label="signName" prop="signName">
          <Input v-model="form.signName" clearable style="width: 100%"/>
        </FormItem>
      </Form>
      <div slot="footer">
        <Button type="text" @click="modalVisible = false">取消</Button>
        <Button type="primary" :loading="submitLoading" @click="saveSetting"
        >提交
        </Button
        >
      </div>
    </Modal>

  </Card>
</template>
<script>
  import {getSetting, setSetting} from "@/api/index.js";
  import template from "./template";
  export default {
    name: "smsSettingManage",
    components: {
      template
    },

    data() {
      return {
        submitLoading: false, // 提交状态
        template, // 模板
        selected: "", // 已选数据
        settingData: "", // 设置数据
        modalTitle: '设置', // modal标题
        modalVisible: false, // modal显隐
        form: {}, // 表单数据
      }
    },
    methods: {
      // 初始化数据
      init() {
        settingInfo("SMS_SETTING")
      },
      // 保存设置
      saveSetting() {
        setSetting("SMS_SETTING", this.form).then((res) => {
          if (res.success) {
            this.$Message.success("保存成功!");
          } else {
            this.$Message.error("保存失败!");
          }
          this.modalVisible = false
        });
      },
      // 查看
      settingInfo(v) {
        alert()
        this.selected = v
        getSetting(v).then((res) => {
          if (res.result) {
            console.log(res)
            this.modalVisible = true
            this.form = res
          }
        });
      }
    },
    mounted() {
      this.init();
    },
  };
</script>
<style lang="scss">
  .card {
    width: 100%;
    height: 100%;
    position: fixed;
  }
  .cardBox {
    display: inline-block;
    border-radius: 2px;
    line-height: 1.5;
    margin-right: 20px;
    width: 300px;
    border: 1px solid #eee;
    padding: 10px;
  }

  .methodItem {
    width: 100%;
    border: 1px solid #f5f5f5;
    text-align: center;
    padding: 20px 0;
  }

  methodItem img {
    width: 220px;
    height: 86px;
  }

  methodItem h4 {
    font-size: 14px;
    color: #333;
    margin-top: 5px;
  }

  .methodItem img {
    width: 220px;
    height: 86px;
  }

  .bar {
    -webkit-flex-direction: row;
    -ms-flex-direction: row;
    flex-direction: row;
    display: -webkit-box;
    display: -webkit-flex;
    display: -ms-flexbox;
    display: flex;
    -webkit-box-pack: justify;
    -webkit-justify-content: space-between;
    -ms-flex-pack: justify;
    justify-content: space-between;
    -webkit-align-items: center;
    -webkit-box-align: center;
    -ms-flex-align: center;
    align-items: center;
    padding: 10px 8px 0;
  }
</style>
