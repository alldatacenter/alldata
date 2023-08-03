import React from 'react'

import { CopyOutlined, DatabaseOutlined, EyeOutlined, ProjectOutlined } from '@ant-design/icons'
import { Card, Col, Row, Typography } from 'antd'
import cs from 'classnames'
import { useNavigate } from 'react-router-dom'

import { observer, useStore } from '@/hooks'

import styles from './index.module.less'

const { Title, Link } = Typography
const { Meta } = Card

const features = [
  {
    icon: <ProjectOutlined style={{ color: '#177ddc' }} />,
    title: 'Projects',
    link: 'projects',
    linkText: 'See all'
  },
  {
    icon: <DatabaseOutlined style={{ color: '#219ebc' }} />,
    title: 'Sources',
    link: '/datasources',
    linkText: 'See all'
  },
  {
    icon: <CopyOutlined style={{ color: '#ffb703' }} />,
    title: 'Features',
    link: '/features',
    linkText: 'See all'
  },
  {
    icon: <EyeOutlined style={{ color: '#fb8500' }} />,
    title: 'Monitoring',
    link: '/monitoring',
    linkText: 'See all'
  }
]

const Home = () => {
  const navigate = useNavigate()

  const { globalStore } = useStore()

  const { project, setSwitchProjecModalOpen } = globalStore

  const onSeeAll = (item: any) => {
    if (item.title === 'Projects') {
      navigate('/projects')
    } else if (project) {
      navigate(`/${project}${item.link}`)
    } else {
      setSwitchProjecModalOpen(true, item.title.toLocaleLowerCase())
    }
  }

  return (
    <div className={cs('page', styles.home)}>
      <Card>
        <Title level={2}>Welcome to Feathr Feature Store</Title>
        <span>
          You can use Feathr UI to search features, identify data sources, track feature lineages
          and manage access controls.
          <a
            target="_blank"
            href="https://feathr-ai.github.io/feathr/concepts/feature-registry.html#accessing-feathr-ui"
            rel="noreferrer"
          >
            Learn More
          </a>
        </span>
      </Card>
      <Row gutter={16} style={{ marginTop: 16 }}>
        {features.map((item) => {
          return (
            <Col key={item.title} xl={6} lg={12} sm={24} xs={24} style={{ marginBottom: 16 }}>
              <Card>
                <Meta
                  title={
                    <Title ellipsis level={2}>
                      {item.title}
                    </Title>
                  }
                  description={
                    <Link
                      onClick={() => {
                        onSeeAll(item)
                      }}
                    >
                      {item.linkText}
                    </Link>
                  }
                  className={styles.cardMeta}
                  avatar={item.icon}
                />
              </Card>
            </Col>
          )
        })}
      </Row>
      <Row gutter={16}>
        <Col xl={16} lg={24} sm={24} xs={24} style={{ marginBottom: 16 }}>
          <Card>
            <Title level={2}>Need help to get started?</Title>
            Explore the following resources to get started with Feathr:
            <ul>
              <li>
                <a
                  target="_blank"
                  href="https://github.com/feathr-ai/feathr#-documentation"
                  rel="noreferrer"
                >
                  Documentation
                </a>{' '}
                provides docs for getting started
              </li>
              <li>
                <a
                  target="_blank"
                  href="https://github.com/feathr-ai/feathr#%EF%B8%8F-running-feathr-on-cloud-with-a-few-simple-steps"
                  rel="noreferrer"
                >
                  Running Feathr on Cloud
                </a>{' '}
                describes how to run Feathr to Azure with Databricks or Synapse
              </li>
              <li>
                <a
                  target="_blank"
                  href="https://github.com/feathr-ai/feathr#%EF%B8%8F-cloud-integrations-and-architecture"
                  rel="noreferrer"
                >
                  Cloud Integrations and Architecture on Cloud
                </a>{' '}
                describes Feathr architecture
              </li>
              <li>
                <a
                  target="_blank"
                  href="https://github.com/feathr-ai/feathr#-slack-channel"
                  rel="noreferrer"
                >
                  Slack Channel
                </a>{' '}
                describes how to join Slack channel for questions and discussions
              </li>
              <li>
                <a
                  target="_blank"
                  href="https://github.com/feathr-ai/feathr#-community-guidelines"
                  rel="noreferrer"
                >
                  Community Guidelines
                </a>{' '}
                describes how to contribute to Feathr
              </li>
            </ul>
            <p>
              Visit
              <a
                target="_blank"
                rel="noreferrer"
                href="https://feathr-ai.github.io/feathr/concepts/feathr-concepts-for-beginners.html"
              >
                Feathr Github Homepage
              </a>{' '}
              to learn more.
            </p>
          </Card>
        </Col>
        <Col xl={8} lg={24} sm={24} xs={24} style={{ marginBottom: 16 }}>
          <Card>
            <Title level={2}>Recent Activity</Title>
            <span>Under construction</span>
          </Card>
        </Col>
      </Row>
    </div>
  )
}

export default observer(Home)
