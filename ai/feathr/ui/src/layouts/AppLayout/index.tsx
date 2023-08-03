import React, { ReactNode, useEffect } from 'react'

import { Layout } from 'antd'
import { Outlet, useLocation, useParams } from 'react-router-dom'

import Header from '@/components/HeaderBar/header'
import SideMenu from '@/components/SiderMenu/siteMenu'
import { useStore } from '@/hooks'

import styles from './index.module.less'

export interface AppLayoutProps {
  children?: ReactNode
}

const defualtRouter = ['403', '404', 'home', 'projects', 'management']
const { Content } = Layout

const AppLayout = (props: AppLayoutProps) => {
  const { globalStore } = useStore()
  const { changeProject, setMenuKeys } = globalStore
  const location = useLocation()
  const { project } = useParams()

  useEffect(() => {
    if (!defualtRouter.includes(project || '')) {
      changeProject(project)
    }
  }, [project])

  useEffect(() => {
    const [, name, type] = location.pathname.toLocaleLowerCase().split('/')
    if (type) {
      setMenuKeys(type)
    } else if (defualtRouter.includes(name)) {
      setMenuKeys(name)
    } else {
      setMenuKeys('home')
    }
  }, [location.pathname])

  return (
    <Layout hasSider className={styles.layout}>
      <SideMenu />
      <Layout>
        <Header />
        <Content className={styles.main}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  )
}

export default AppLayout
