export default {
  inserted(el, binding, vnode) {
    const { value } = binding
    // 工作流状态（1待提交，2已退回，3审核中，4通过，5不通过，6已撤销）
    if (value) {
      if (value === '1' || value === '6') {
        el.disabled = false
      } else {
        el.disabled = true
        el.classList.add('is-disabled')
      }
    } else {
      throw new Error('请设置流程权限标签值')
    }
  }
}
