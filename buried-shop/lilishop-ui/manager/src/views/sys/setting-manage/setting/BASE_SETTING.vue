<template>
  <div class="layout">
    <Form ref="formValidate" :label-width="150" label-position="right" :model="formValidate" :rules="ruleValidate">

      <FormItem label="站点名称" prop="siteName">
        <Input v-model="formValidate.siteName" />
      </FormItem>
      <FormItem label="icp" prop="icp">

        <Input v-model="formValidate.icp" />
      </FormItem>

      <FormItem label="后台Logo" prop="domainLogo">
        <div class="label-item-upload">

          <img v-if="formValidate.domainLogo" class="img" :src="formValidate.domainLogo" />
          <img v-else class="img" src="../../../../assets/emptyImg.png" alt="">
          <Button @click="onClickImg('domainLogo')">选择图片</Button>
        </div>
      </FormItem>
      <FormItem label="买家端Logo" prop="buyerSideLogo">
        <div class="label-item-upload">
          <img v-if="formValidate.buyerSideLogo" class="img" :src="formValidate.buyerSideLogo" />
          <img v-else class="img" src="../../../../assets/emptyImg.png" alt="">
          <Button @click="onClickImg('buyerSideLogo')">选择图片</Button>
        </div>
      </FormItem>
      <FormItem label="商家端Logo" prop="storeSideLogo">
        <div class="label-item-upload">
          <img v-if="formValidate.storeSideLogo" class="img" :src="formValidate.storeSideLogo" />
          <img v-else class="img" src="../../../../assets/emptyImg.png" alt="">
          <Button @click="onClickImg('storeSideLogo')">选择图片</Button>
        </div>
      </FormItem>

      <FormItem label="站点地址" prop="staticPageAddress">
        <Input v-model="formValidate.staticPageAddress" />
      </FormItem>
      <FormItem label="wap站点地址" prop="staticPageWapAddress">
        <Input v-model="formValidate.staticPageWapAddress" />
      </FormItem>
      <div class="label-btns">
        <Button type="primary" @click="submit('formValidate')">保存</Button>

      </div>
    </Form>
    <Modal width="1200px" v-model="picModelFlag">
      <ossManage @callback="callbackSelected" ref="ossManage" />
    </Modal>

  </div>
</template>
<script>
import { setSetting } from "@/api/index";
import { handleSubmit } from "./validate";
import ossManage from "@/views/sys/oss-manage/ossManage";
export default {
  title: "基础设置",
  props: {
    res:Object,
    type:''
  },
  components: {
    ossManage,
  },
  data() {
    return {
      handleSubmit, // 验证规则

      picModelFlag: false, // 预览图片显隐
      formValidate: { // 表单数据
        buyerSideLogo: "",
        domainLogo: "",
        icp: "",
        storeSideLogo: "",
        siteName: "",
        staticPageAddress: "",
        staticPageWapAddress: "",
      },
      selected: "", // 已选数据
      ruleValidate: {} // 验证规则
    };
  },
  created() {
    this.init();
  },
  methods: {
    // 点击图片
    onClickImg(item) {
      this.selected = item;
      this.$refs.ossManage.selectImage = true;
      this.picModelFlag = true;
    },
    submit(name) {
      let that = this;
      if (handleSubmit(that, name)) {
        this.setupSetting();
      }
    },
    // 选择回显
    callbackSelected(val) {
      this.picModelFlag = false;
      this.formValidate[this.selected] = val.url;
    },
    // 保存设置
    setupSetting() {
      setSetting(this.type, this.formValidate).then((res) => {
        if (res.success) {
          this.$Message.success("保存成功!");
          localStorage.setItem("icon", this.formValidate.domainLogo);
          window.document.title = this.formValidate.siteName + " - 运营后台";
        } else {
          this.$Message.error("保存失败!");
        }
      });
    },
    /**添加必填项 */
    init() {
      this.res = JSON.parse(this.res);

      this.$set(this, "formValidate", { ...this.res });
      Object.keys(this.res).forEach((item) => {
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
<style scoped lang="scss">
@import "./style.scss";
.label-item {
  display: flex;
  > .ivu-input {
    width: 200px;
    margin: 0 10px;
  }
}
.label-item-upload {
  display: flex;
  align-items: flex-end;
  img {
    margin-right: 10px;
    width: 100px;
    height: 100px;
  }
}
</style>
