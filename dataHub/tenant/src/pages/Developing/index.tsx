import React from 'react'
import { PageContainer } from '@ant-design/pro-layout'
import Developing from '@/components/Developing'
import FormValidateStatic from './FormValidateStatic'

const PageDeveloping: React.FC<{}> = () => {
  return (
    <PageContainer>
      <FormValidateStatic />
      <Developing />
    </PageContainer>
  )
}

export default PageDeveloping
