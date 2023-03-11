<template>
  <div class="m-sql-editor" :class="{ disabled: readOnly }" style="height: 100%; width: 100%;"></div>
</template>

<script lang="ts" setup>
import * as monaco from 'monaco-editor'
import { nextTick, onBeforeUnmount, onMounted, watch } from 'vue'

import { EDITOR_OPTIONS } from './editor-config'

interface EditorCommand {
  [commandName: string]: string | null;
}

let editor: monaco.editor.IStandaloneCodeEditor
// const value = ''
const props = defineProps<{ sqlValue: string, options: any, readOnly: boolean}>()

// @Component
// export default class MSqlEditor extends Vue {
// @Model('change', { type: String })
// private value!: string;

// @Prop({ default: () => ({}) })
// private options!: object;

// @Prop({
//   default: false
// })
// private readOnly!: boolean;

let oldValue = ''
const commandMap: EditorCommand = {}

const emit = defineEmits<{
 (e: 'save'): void,
 (e: 'update:value', val: any): void,
 (e: 'change', val: any): void,
}>()
// @Watch('value')
// private onValueChanged(val = '') {
//   if (this.oldValue !== val && this.editor) {
//     this.editor.setValue(val)
//   }
// }
watch(
  () => props.sqlValue,
  (value) => {
    if (value) {
      if (oldValue !== value && editor) {
        editor.setValue(value)
      }
    }
  }
)

window.addEventListener('resize', resize)

function resize() {
  editor && editor.layout()
}
defineExpose({
  executeCommand(command: string) {
    const cmd = commandMap[command]
    const newEditor = editor as any
    cmd && newEditor && newEditor._commandService.executeCommand(cmd)
  },
  updateOptions(options: any = {}) {
    editor && editor.updateOptions(options)
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  editor && editor.dispose()
})

onMounted(() => {
  const el: any = document.getElementsByClassName('m-sql-editor')[0]
  nextTick(() => {
    const newEditor = (editor = monaco.editor.create(el, { ...EDITOR_OPTIONS, ...props.options }))
    addCommand()

    newEditor.setValue(props.sqlValue || '')

    newEditor.onDidChangeModelContent(e => {
      const val = editor.getValue()
      emit('update:value', val)
      emit('change', val)
      oldValue = val
    })
  })
})
/**
   * Monaco Editor
   * API： https://microsoft.github.io/monaco-editor/api/modules/monaco.editor.html
   * config： https://microsoft.github.io/monaco-editor/api/interfaces/monaco.editor.ieditoroptions.html
   */

function addCommand() {
  if (editor) {
    // @ts-ignore
    const saveBinding = editor.addCommand(monaco.KeyMod.CtrlCmd | monaco.KeyCode.KEY_S, () => {
      emit('save')
    })
    commandMap.save = saveBinding
    // @ts-ignore
    const formatBinding = editor.addCommand(monaco.KeyMod.Alt | monaco.KeyMod.Shift | monaco.KeyCode.KEY_F, () => {
      formatSql()
    })
    commandMap.format = formatBinding
  }
}

function formatSql() {
  const action = editor && editor.getAction('editor.action.formatDocument')
  action && action.run()
}

</script>

<style lang="less" scoped>
.m-sql-editor {
  &.disabled {
    cursor: not-allowed !important;
    .monaco-editor .view-lines {
      cursor: not-allowed !important;
    }
  }
}
</style>
