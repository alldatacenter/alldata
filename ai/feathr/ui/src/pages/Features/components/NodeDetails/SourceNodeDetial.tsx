import React from 'react'

import CardDescriptions from '@/components/CardDescriptions'
import { DataSource } from '@/models/model'
import { SourceAttributesMap } from '@/utils/attributesMapping'

export interface SourceNodeDetialProps {
  source: DataSource
}

const SourceNodeDetial = (props: SourceNodeDetialProps) => {
  const { source } = props
  const { attributes } = source
  return (
    <CardDescriptions
      title="Source Attributes"
      mapping={SourceAttributesMap}
      descriptions={attributes}
    />
  )
}

export default SourceNodeDetial
