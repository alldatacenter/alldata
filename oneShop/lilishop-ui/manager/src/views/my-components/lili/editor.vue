<template>
  <div>
    <div style="position:relative">
      <div :id="id" style="text-align: left;width:850px"></div>
      <div v-if="showExpand">
        <div class="e-menu e-code" @click="editHTML">
          <Icon type="md-code-working" size="22" />
        </div>
        <div class="e-menu e-preview" @click="fullscreenModal=true">
          <Icon type="ios-eye" size="24" />
        </div>
        <div class="e-menu e-trash" @click="clear">
          <Icon type="md-trash" size="18" />
        </div>
      </div>
    </div>

    <Modal title="编辑html代码" v-model="showHTMLModal" :mask-closable="false" :width="900" :fullscreen="full">
      <Input v-if="!full" v-model="dataEdit" :rows="15" type="textarea" style="max-height:60vh;overflow:auto;" />
      <Input v-if="full" v-model="dataEdit" :rows="32" type="textarea" />
      <div slot="footer">
        <Button @click="full=!full" icon="md-expand">全屏开/关</Button>
        <Button @click="editHTMLOk" type="primary" icon="md-checkmark-circle-outline">确定保存</Button>
      </div>
    </Modal>
    <Modal title="预览" v-model="fullscreenModal" fullscreen>
      <div v-html="data">{{data}}</div>
      <div slot="footer">
        <Button @click="fullscreenModal=false">关闭</Button>
      </div>
    </Modal>
  </div>
</template>

<script>
import { uploadFile } from "@/api/index";
import E from "wangeditor";
import xss from "xss";
// 表情包配置 自定义表情可在该js文件中统一修改
import { sina } from "@/libs/emoji";
var editor = null;
export default {
  name: "editor",
  props: {
    id: {
      type: String,
      default: "editor",
    },
    value: String,
    base64: {
      type: Boolean,
      default: false,
    },
    showExpand: {
      type: Boolean,
      default: true,
    },
    openXss: {
      type: Boolean,
      default: false,
    },
  },
  data() {
    return {
      editor: null, // 富文本编辑器初始化
      data: '', // 富文本数据
      dataEdit: "", // 编辑数据
      showHTMLModal: false, // 显示html
      full: false, // html全屏开关
      fullscreenModal: false, // 显示全屏预览
    };
  },
  methods: {

    initEditor() {
      let that = this;
      // 详见wangeditor3官网文档 https://www.kancloud.cn/wangfupeng/wangeditor3/332599
      editor = new E(`#${this.id}`);
      // 编辑内容绑定数据
      editor.config.onchange = (html) => {
        this.data = html;
        this.$emit("input", this.data);
        this.$emit("on-change", this.data);
      };
      editor.config.showFullScreen = false;
      // z-index
      editor.config.zIndex = 100;
      if (this.base64) {
        // 使用 base64 保存图片
        editor.config.uploadImgShowBase64 = true;
      } else {
        // 配置上传图片服务器端地址
        editor.config.uploadImgServer = uploadFile;
        // lili如要header中传入token鉴权
        editor.config.uploadImgHeaders = {
          accessToken: that.getStore("accessToken"),
        };
        editor.config.uploadFileName = "file";
        editor.config.uploadImgHooks = {
          before: function (xhr, editor, files) {
            // 图片上传之前触发
          },
          success: function (xhr, editor, result) {
            // 图片上传并返回结果，图片插入成功之后触发
          },
          fail: function (xhr, editor, result) {
            // 图片上传并返回结果，但图片插入错误时触发
            that.$Message.error("上传图片失败");
          },
          error: function (xhr, editor) {
            // 图片上传出错时触发
            that.$Message.error("上传图片出错");
          },
          timeout: function (xhr, editor) {
            // 图片上传超时时触发
            that.$Message.error("上传图片超时");
          },
          // 如果服务器端返回的不是 {errno:0, data: [...]} 这种格式，可使用该配置
          customInsert: function (insertImg, result, editor) {
            if (result.success == true) {
              console.log(insertImg,result,editor);
              let url = result.result;
              insertImg(url);
              that.$Message.success("上传图片成功");
            } else {
              that.$Message.error(result.message);
            }
          },
        };
      }

      editor.config.customAlert = function (info) {
        // info 是需要提示的内容
        // that.$Message.info(info);
      };
      // 字体
      editor.config.fontNames = ["微软雅黑", "宋体", "黑体", "Arial"];
      // 表情面板可以有多个 tab ，因此要配置成一个数组。数组每个元素代表一个 tab 的配置
      editor.config.emotions = [
        {
          // tab 的标题
          title: "新浪",
          // type -> 'emoji' / 'image'
          type: "image",
          // content -> 数组
          content: sina,
        },
      ];
      editor.create();
      if (this.value) {
        if (this.openXss) {
          editor.txt.html(xss(this.value));
        } else {
          editor.txt.html(this.value);
        }
      }
    },
    // 编辑html
    editHTML() {
      this.dataEdit = this.data;
      this.showHTMLModal = true;
    },
    // 保存
    editHTMLOk() {
      console.log(this.dataEdit);
      editor.txt.html(this.dataEdit);
      this.$emit("input", this.data);
      this.$emit("on-change", this.data);
      this.showHTMLModal = false;
    },
    // 清空编辑器
    clear() {
      this.$Modal.confirm({
        title: "确认清空",
        content: "确认要清空编辑器内容？清空后不能撤回",
        onOk: () => {
          this.data = "";
          editor.txt.html(this.data);
          this.$emit("input", this.data);
          this.$emit("on-change", this.data);
        },
      });
    },
    // 回显数据
    setData(value) {
      if (!editor) {
        this.initEditor();
      }
      this.data = value;
      editor.txt.html(this.data);
      this.$emit("input", this.data);
      this.$emit("on-change", this.data);
    },
  },
 watch: {
    value: {
      handler: function (val) {
        // 赋值给富文本
        this.setData(this.$options.filters.enCode(val));
      },
    },
  },
  mounted() {
    this.initEditor();
  },
};
</script>

<style lang="scss" scoped>
.e-menu {
  z-index: 101;
  position: absolute;
  cursor: pointer;
  color: #999;
  :hover {
    color: #333;
  }
}
.e-code {
  top: 6px;
  left: 818px;
}
.e-preview {
  top: 46px;
  left: 174px;
}
.e-trash {
  top: 46px;
  left: 215px;
}
</style>

