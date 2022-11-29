import flowEdit from './flowEdit'
import flowDel from './flowDel'

const install = function(Vue) {
  Vue.directive('flowEdit', flowEdit)
  Vue.directive('flowDel', flowDel)
}

if (window.Vue) {
  window['flowEdit'] = flowEdit
  window['flowDel'] = flowDel
  Vue.use(install) // eslint-disable-line
}

export default install
