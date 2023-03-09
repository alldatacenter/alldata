import React from 'react'

import { GithubOutlined } from '@ant-design/icons'
import { useAccount, useMsal } from '@azure/msal-react'
import { Button, Layout, Typography } from 'antd'

import { observer, useStore } from '@/hooks'

import SwitchProjectModal from '../SwitchProjectModal'

import HeaderWidget from './headerWidget'

import styles from './index.module.less'

const { Link } = Typography

const Header = () => {
  const { globalStore } = useStore()
  const { project, setSwitchProjecModalOpen } = globalStore
  const { accounts } = useMsal()
  const account = useAccount(accounts[0] || {})

  const onOpen = () => {
    setSwitchProjecModalOpen(true)
  }

  return (
    <>
      <Layout.Header className={styles.header}>
        <span className={styles.title}>
          <span>{project.toLocaleUpperCase()} </span>
          {project && (
            <Button size="small" shape="round" onClick={onOpen}>
              Switch Project
            </Button>
          )}
        </span>
        <span className={styles.right}>
          <Link
            className={styles.action}
            href="https://github.com/feathr-ai/feathr"
            target="_blank"
          >
            <GithubOutlined />
          </Link>
          <HeaderWidget username={account?.username} />
        </span>
      </Layout.Header>
      <div className={styles.vacancy} />
      <SwitchProjectModal />
    </>
  )
}

export default observer(Header)
