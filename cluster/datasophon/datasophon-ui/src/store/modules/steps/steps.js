/*
 * @Author: mjzhu
 * @describe: 
 * @Date: 2022-06-14 20:06:51
 * @LastEditTime: 2022-06-22 17:07:41
 * @FilePath: \ddh-ui\src\store\modules\steps\steps.js
 */

const state = {
  clusterId: '',
  steps1: {
    hosts: '',
    sshUser: '',
    sshPort: ''
  },
  steps7: {},
  commandType: 'INSTALL_SERVICE',
  commandIds: ''
}
const getters = {}
const mutations = {}
Object.keys(state).forEach((item) => {
  mutations[item] = (state, value) => {
    state[item] = value
  }
})

const actions = {
  setSteps1({
    commit
  }, obj) {
    commit('steps1', obj)
  },
  setClusterId({
    commit
  }, obj) {
    commit('clusterId', obj)
  },
  setCommandType({
    commit
  }, obj) {
    commit('commandType', obj)
  },
  setCommandIds({
    commit
  }, obj) {
    commit('commandIds', obj)
  },
}
export default {
  namespaced: 'steps',
  state,
  getters,
  actions,
  mutations
}