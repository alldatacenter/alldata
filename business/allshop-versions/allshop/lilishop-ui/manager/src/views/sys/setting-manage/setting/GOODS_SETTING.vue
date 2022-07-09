<template>
  <div class="layout">
    <Form ref="formValidate" :label-width="150" label-position="right" :model="formValidate" :rules="ruleValidate">
      <FormItem label="商品审核" prop="goodsCheck">
        <RadioGroup type="button" button-style="solid" v-model="formValidate.goodsCheck">
          <Radio label="true">开启</Radio>
          <Radio label="false">关闭</Radio>

        </RadioGroup>
      </FormItem>
      <div class="label-item">
        <FormItem class="label-item" label="缩略图宽" prop="abbreviationPictureWidth">
          <Input type="number" v-model="formValidate.abbreviationPictureWidth">
          <span slot="prepend">宽</span>
          <span slot="append">px</span>
          </Input>

        </FormItem>
        <FormItem class="label-item" label="缩略图高" prop="abbreviationPictureHeight">
          <Input type="number" v-model="formValidate.abbreviationPictureHeight">
          <span slot="prepend">高</span>
          <span slot="append">px</span>
          </Input>
        </FormItem>
      </div>
      <div class="label-item">
        <FormItem label="小图宽" prop="smallPictureWidth">
          <Input type="number" v-model="formValidate.smallPictureWidth">
          <span slot="prepend">宽</span>
          <span slot="append">px</span>
          </Input>

        </FormItem>
        <FormItem label="小图高" class="label-item" prop="smallPictureHeight">
          <Input type="number" v-model="formValidate.smallPictureHeight">
          <span slot="prepend">高</span>
          <span slot="append">px</span>
          </Input>
        </FormItem>
      </div>
      <div class="label-item">
        <FormItem class="label-item" label="原图宽高" prop="originalPictureWidth">
          <Input type="number" v-model="formValidate.originalPictureWidth">
          <span slot="prepend">宽</span>
          <span slot="append">px</span>
          </Input>

        </FormItem>
        <FormItem class="label-item" label="原图宽高" prop="originalPictureHeight">
          <Input type="number" v-model="formValidate.originalPictureHeight">
          <span slot="prepend">高</span>
          <span slot="append">px</span>
          </Input>
        </FormItem>
      </div>
      <div class="label-btns">
        <Button type="primary" @click="submit('formValidate')">保存</Button>
        <Button type="primary" style="margin-left: 100px" @click="createIndex()">重新生成所有商品索引</Button>
        <div class="progress-item" v-if="showProgress">
          <i-progress :percent="progressVal"></i-progress>
        </div>
      </div>
    </Form>
  </div>
</template>
<script>
import { setSetting, createIndex, getProgress } from "@/api/index";
import { handleSubmit } from "./validate";
export default {
  props: ["res", "type"],
  data() {
    return {
      formValidate: {
        // 表单数据
        goodsCheck: 1,
        smallPictureHeight: "0",
        smallPictureWidth: "0",
        abbreviationPictureWidth: "0",
        abbreviationPictureHeight: "0",
        originalPictureWidth: "0",
        originalPictureHeight: "0",
      },
      progressVal: 0,
      showProgress: false,
      intervalProgress: null,
      ruleValidate: {}, // 验证规则
    };
  },
  created() {
    this.init();
  },
  methods: {
    //保存
    submit(name) {
      let that = this;
      if (handleSubmit(that, name)) {
        this.setupSetting();
      }
    },
    //重新生成所有商品索引
    createIndex() {
      createIndex().then((res) => {
        if (res.success) {
          this.$Message.success("开始生成!");
          this.showProgress = true;
          setTimeout(() => {
            this.intervalProgress = setInterval(() => {
              getProgress().then((resp) => {
                let progressResult = resp.result;
                if (progressResult != null && progressResult.flag === 0) {
                  clearInterval(this.intervalProgress);
                  this.showProgress = false;
                  this.$Message.success("生成成功!");
                } else {
                  this.progressVal = Math.floor(
                    (progressResult.processed / progressResult.total) * 100
                  );
                }
              });
            }, 1000);
          }, 10000);
        } else if (res.code === 100000) {
          this.showProgress = true;
          this.intervalProgress = setInterval(() => {
            getProgress().then((resp) => {
              let progressResult = resp.result;
              if (progressResult != null && progressResult.flag === 0) {
                clearInterval(this.intervalProgress);
                this.showProgress = false;
                this.$Message.success("生成成功!");
              } else {
                this.progressVal = Math.floor(
                  (progressResult.processed / progressResult.total) * 100
                );
              }
            });
          }, 1000);
        }
      });
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

      Object.keys(this.res).map((item) => {
        this.res[item] += "";
      });
      this.$set(this, "formValidate", { ...this.res });
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
}
/deep/ .ivu-input {
  width: 100px !important;
}
</style>
