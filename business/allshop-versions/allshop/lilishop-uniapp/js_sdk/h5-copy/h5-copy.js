export  function h5Copy(content) {
  
  if (!document.queryCommandSupported('copy')) {
    // 不支持
    return false
  }
  
  let textarea = document.createElement("textarea")
  textarea.value = content
  textarea.readOnly = "readOnly"
  document.body.appendChild(textarea)
  textarea.select() // 选择对象
  textarea.setSelectionRange(0, content.length) //核心
  let result = document.execCommand("copy") // 执行浏览器复制命令
  textarea.remove()
  return result
  
}



/**
 * 获取系统剪贴板内容
 */
 export function getClipboardData() {
  return new Promise((success, fail) => {
    // #ifndef H5
    uni.getClipboardData({
      success: ({ data }) => success(data),
      fail
    })
    // #endif

    // #ifdef H5
    try {
      navigator.clipboard.readText().then(success).catch(fail)
    } catch (error) {
      fail(error)
    }
    // #endif
  })
}