import plugins from "./plugins";
import toobar from "./toolbar";
import { upLoadFileMethods } from "@/api/common";

export const initEditor = {
  height: "400px",
  language: "zh_CN",
  menubar: "file edit insert view format table", // 菜单:指定应该出现哪些菜单
  toolbar: toobar, // 分组工具栏控件
  plugins: plugins, // 插件(比如: advlist | link | image | preview等)
  object_resizing: false, // 是否禁用表格图片大小调整
  end_container_on_empty_block: true, // enter键 分块
  powerpaste_word_import: "merge", // 是否保留word粘贴样式  clean | merge
  code_dialog_height: 450, // 代码框高度 、宽度
  code_dialog_width: 1000,
  advlist_bullet_styles: "square", // 无序列表 有序列表
  maxSize: "2097152", // 设置图片大小
  accept: "image/jpeg, image/png", // 设置图片上传规则
  images_upload_handler: async function (blobInfo, success, failure) {
    const formData = new FormData();
    console.log("请求")
    formData.append("file", blobInfo.blob());
    try {
      const res = await upLoadFileMethods(formData);
      if (res.result) {
        success(res.result)
      } else {
        failure("上传文件有误请稍后重试");
      }
    } catch (e) {
      failure('上传出错')
    }
  },
  // init_instance_callback: function (editor) {
  //   var freeTiny = document.querySelector(".tox .tox-notification--in .tox-notification .tox-notification--warning .tox .tox-notification--warning .tox-notifications-container");
  //   freeTiny.style.display = "none";
  // },
  content_style: `
    * { padding:0; margin:0; }

    html, body height:100%; }

    img { max-width:100%; display:block;height:auto; }

    a   { text-decoration: none; }

    iframe{ width: 100%; }

    p { line-height:1.6; margin: 0px; }

    table{ word-wrap:break-word; word-break:break-all; max-width:100%; border:none; border-color:#999; }

    .mce-object-iframe{ width:100%; box-sizing:border-box; margin:0; padding:0; }

    ul,ol{ list-style-position:inside; }
    `, // 设置样式
  statusbar: false, // 隐藏编辑器底部的状态栏
  elementpath: false, // 禁用编辑器底部的状态栏
  paste_data_images: true, // 允许粘贴图像
};
