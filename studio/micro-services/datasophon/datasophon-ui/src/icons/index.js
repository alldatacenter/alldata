/*
 * @Author: mjzhu
 * @Date: 2021-11-22 10:26:37
 * @LastEditTime: 2022-06-07 11:32:16
 * @FilePath: \ddh-ui\src\icons\index.js
 */
import Vue from 'vue'
import SvgIcon from './SvgIcon'// svg component

// register globally
Vue.component('svg-icon', SvgIcon)
const common = require.context('./common', false, /\.svg$/)
const colony = require.context('./colony', false, /\.svg$/)
const requireAll = requireContext => requireContext.keys().map(requireContext)
requireAll(common)
requireAll(colony)
