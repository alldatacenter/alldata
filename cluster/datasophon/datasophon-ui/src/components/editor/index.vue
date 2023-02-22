<!--
 * @Author: mjzhu
 * @describe: 
 * @Date: 2022-06-27 16:19:45
 * @LastEditTime: 2022-07-13 15:18:24
 * @FilePath: \ddh-ui\src\components\editor\index.vue
-->
<template>
  <div ref="editorRef" style="height:700px" class="editorRef" @onload="handleCancel" ></div>
  
</template>
<script>
// 引用组件
import * as monaco from 'monaco-editor';
// import * as monaco from "monaco-editor/esm/vs/editor/editor.main.js";
import "monaco-editor/esm/vs/basic-languages/javascript/javascript.contribution";

export default {
  components: {},
  props: {
    propsCode: String,
  },
  data() {
    return {
      curCode: "",
      cmOptions: {
        value: "",
        mode: "text/javascript",
        theme: "ambiance",
      },
      timer:null
    };
  },
  watch: {
    propsCode: {
      handler(val,oldVal) {
        this.curCode = val;
        if(oldVal){
          if( this.monacoEditor) this.monacoEditor.dispose();//使用完成销毁实例
          this.init()
          this.timer =setTimeout(()=>{
            this.handleCancel()
          },100)
        }
      },
      immediate: true,
    },
  },

  mounted () {
    this.init()
    this.timer =setTimeout(()=>{
      this.handleCancel()
    },100)
  },
  update(){
  },
  methods: {
    init() {
      // 使用 - 创建 monacoEditor 对象
      this.monacoEditor = monaco.editor.create(this.$refs.editorRef, {
        // theme: 'vs-dark', // 主题
        value: this.curCode, // 默认显示的值
        language: "yaml",
        wordWrap: "on",
        folding: true, // 是否折叠
        foldingHighlight: true, // 折叠等高线
        foldingStrategy: "auto", // 折叠方式  auto | indentation
        showFoldingControls: "always", // 是否一直显示折叠 always | mouseover
        disableLayerHinting: true, // 等宽优化
        emptySelectionClipboard: false, // 空选择剪切板
        selectionClipboard: false, // 选择剪切板
        automaticLayout: true, // 自动布局
        codeLens: true, // 代码镜头
        scrollBeyondLastLine: false, // 滚动完最后一行后再滚动一屏幕
        colorDecorators: true, // 颜色装饰器
        accessibilitySupport: "auto", // 辅助功能支持  "auto" | "off" | "on"
        lineNumbers: "on", // 行号 取值： "on" | "off" | "relative" | "interval" | function
        lineNumbersMinChars: 5, // 行号最小字符   number
        enableSplitViewResizing: false,
        readOnly: true , //是否只读  取值 true | false
        minimap: {
          enabled: true, // 不要小地图
        },
      });
     
    },
    handleCancel(){
      this.$nextTick(()=>{
        let dom = document.getElementsByClassName("overflow-guard")[0]
        dom.scrollTop=dom.scrollHeight-dom.clientHeight
      })
      
    }
    
  },
};
</script>
<style lang="less" scoped>
// .editorRef{
//   overflow-x: hidden;
//   overflow-y: auto;
// }
</style>