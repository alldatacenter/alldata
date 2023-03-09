import {
  FeatureTransformation,
  FeatureKey,
  FeatureType,
  DataSourceAttributes
} from '@/models/model'

export const TransformationMap: Array<{
  label: string
  key: keyof FeatureTransformation
}> = [
  { label: 'Expression', key: 'transformExpr' },
  { label: 'Filter', key: 'filter' },
  { label: 'Aggregation', key: 'aggFunc' },
  { label: 'Limit', key: 'limit' },
  { label: 'Group By', key: 'groupBy' },
  { label: 'Window', key: 'window' },
  { label: 'Expression', key: 'defExpr' }
]

export const FeatureKeyMap: Array<{ label: string; key: keyof FeatureKey }> = [
  { label: 'Full name', key: 'fullName' },
  { label: 'Description', key: 'description' },
  { label: 'Key column', key: 'keyColumn' },
  { label: 'Key column alias', key: 'keyColumnAlias' },
  { label: 'Key column type', key: 'keyColumnType' }
]

export const TypeMap: Array<{ label: string; key: keyof FeatureType }> = [
  { label: 'Dimension Type', key: 'dimensionType' },
  { label: 'Tensor Category', key: 'tensorCategory' },
  { label: 'Type', key: 'type' },
  { label: 'Value Type', key: 'valType' }
]

export const SourceAttributesMap: Array<{
  label: string
  key: keyof DataSourceAttributes
}> = [
  { label: 'Name', key: 'name' },
  { label: 'Type', key: 'type' },
  { label: 'Path', key: 'path' },
  { label: 'Preprocessing', key: 'preprocessing' },
  { label: 'Event Timestamp Column', key: 'event_timestamp_column' },
  { label: 'Timestamp Format', key: 'timestamp_format' },
  { label: 'Qualified Name', key: 'qualified_name' },
  { label: 'Tags', key: 'tags' }
]
