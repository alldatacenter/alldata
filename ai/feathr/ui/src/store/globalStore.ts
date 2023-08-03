import { action, makeObservable, observable, runInAction } from 'mobx'
import type { NavigateFunction } from 'react-router-dom'

import { fetchProjects } from '@/api'
import { Project } from '@/models/model'

class GlobalStore {
  project = ''

  routeName = ''

  menuKeys: string[] = []

  projectList: Project[] = []

  openSwitchProjectModal = false

  navigate?: NavigateFunction

  constructor() {
    makeObservable(this, {
      menuKeys: observable,
      project: observable,
      projectList: observable,
      openSwitchProjectModal: observable,
      routeName: observable,
      changeProject: action.bound,
      setMenuKeys: action.bound,
      fetchProjects: action.bound,
      setSwitchProjecModalOpen: action.bound,
      setProjectList: action.bound
    })
    setTimeout(() => {
      this.fetchProjects()
    }, 1000)
  }

  changeProject(project = '', isforce?: boolean) {
    if (project || isforce) {
      if (this.project !== project) {
        this.fetchProjects()
      }
      this.project = project
    }
  }

  setMenuKeys(key: string) {
    this.menuKeys = [key]
  }

  setSwitchProjecModalOpen(open: boolean, routeName = 'lineage') {
    this.openSwitchProjectModal = open
    this.routeName = routeName
  }

  setProjectList(list: string[]) {
    this.projectList = list.map((item) => ({
      name: item
    }))
  }

  async fetchProjects() {
    try {
      const result = await fetchProjects()
      runInAction(() => {
        this.setProjectList(result)
      })
    } catch {
      //
    }
  }
}

export default new GlobalStore()
