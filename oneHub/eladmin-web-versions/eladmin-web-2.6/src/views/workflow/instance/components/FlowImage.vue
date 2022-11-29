<template>
  <el-dialog title="流程图" width="50%" :visible.sync="dialogVisible">
    <el-image :src="flowSrc">
      <div slot="error" class="image-slot">
        <i class="el-icon-picture-outline"></i>
      </div>
    </el-image>
    <span slot="footer" class="dialog-footer">
      <el-button @click="dialogVisible = false">取消</el-button>
    </span>
  </el-dialog>
</template>

<script>
import { flowTrack } from '@/api/workflow/instance'

export default {
  name: 'FlowImage',
  props: {
    visible: {
      type: Boolean,
      default: function() {
        return false
      }
    },
    processInstanceId: {
      type: String,
      default: function() {
        return ''
      }
    }
  },
  data() {
    return {
      flowSrc: ''
    }
  },
  computed: {
    dialogVisible: {
      get() {
        return this.visible
      },
      set(val) {
        this.$emit('update:visible', val)
      }
    }
  },
  created() {
    this.init()
  },
  methods: {
    init() {
      flowTrack(this.processInstanceId).then(response => {
        const blob = new Blob([response])
        this.flowSrc = window.URL.createObjectURL(blob)
      })
    }
  }
}
</script>

<style lang="scss" scoped>

</style>
