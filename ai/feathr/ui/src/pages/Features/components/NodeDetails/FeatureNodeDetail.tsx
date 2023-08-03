import React from 'react'

import { Space } from 'antd'

import CardDescriptions from '@/components/CardDescriptions'
import { Feature } from '@/models/model'
import { TransformationMap, FeatureKeyMap, TypeMap } from '@/utils/attributesMapping'
import { getJSONMap } from '@/utils/utils'

export interface FeatureNodeDetialProps {
  feature: Feature
}

const FeatureNodeDetial = (props: FeatureNodeDetialProps) => {
  const { feature } = props

  const { attributes } = feature
  const { transformation, key, type, tags } = attributes

  const tagsMap = getJSONMap(tags)

  return (
    <Space className="display-flex" direction="vertical" size="middle" align="start">
      <CardDescriptions
        title="Transformation"
        mapping={TransformationMap}
        descriptions={transformation}
      />
      {key?.map((item, index) => {
        return (
          <CardDescriptions
            key={index}
            title={`Entity Key ${index + 1}`}
            mapping={FeatureKeyMap}
            descriptions={item}
          />
        )
      })}
      <CardDescriptions title="Type" mapping={TypeMap} descriptions={type} />
      <CardDescriptions title="Tags" mapping={tagsMap} descriptions={tags} />
    </Space>
  )
}

export default FeatureNodeDetial
