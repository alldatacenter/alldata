import { useEffect, useRef, useState } from 'react'

import { FormInstance, Form, message } from 'antd'
import { useNavigate } from 'react-router-dom'

import { createSource } from '@/api'
import { Tag } from '@/components/AddTags'
import { JdbcAuth, NewDatasource } from '@/models/model'

export const enum SourceTypeEnum {
  HDFS = 'hdfs',
  SNOWFLAKE = 'SNOWFLAKE',
  JDBC = 'jdbc',
  COSMOSDB = 'cosmosdb',
  SPARKSQL = 'sparksql'
}

const sourceTypeOptions = [
  SourceTypeEnum.HDFS,
  SourceTypeEnum.SNOWFLAKE,
  SourceTypeEnum.JDBC,
  SourceTypeEnum.COSMOSDB,
  SourceTypeEnum.SPARKSQL
].map((value) => {
  return {
    value,
    label: value.toLocaleUpperCase()
  }
})

const jdbcAuthOptions = JdbcAuth.map((value: string) => ({
  value: value,
  label: value
}))

export const useForm = (form: FormInstance<any>, projectStr?: string) => {
  const navigate = useNavigate()

  const [createLoading, setCreateLoading] = useState<boolean>(false)

  const tagsRef = useRef<Tag[]>([])

  const project = Form.useWatch('project', form)

  const type = Form.useWatch('type', form)

  const onFinish = async (values: any) => {
    setCreateLoading(true)
    try {
      const tags = tagsRef.current.reduce((tags: any, item: any) => {
        tags[item.name.trim()] = item.value.trim() || ''
        return tags
      }, {} as any)

      const newDatasource: NewDatasource = {
        name: values.name,
        type: values.type,
        tags,
        qualifiedName: values.qualifiedName,
        preprocessing: values.preprocessing,
        timestampFormat: values.timestampFormat,
        eventTimestampColumn: values.eventTimestampColumn
      }

      switch (newDatasource.type) {
        case SourceTypeEnum.HDFS:
        case SourceTypeEnum.SNOWFLAKE:
          newDatasource.path = values.path
          break
        case SourceTypeEnum.JDBC:
          if (values.auth !== 'None') {
            newDatasource.auth = values.auth
          }
          newDatasource.url = values.url
          newDatasource[values.jdbc.type as 'dbtable' | 'query'] = values.jdbc.value
          break
        case SourceTypeEnum.COSMOSDB:
          newDatasource.type = 'generic'
          newDatasource.format = 'cosmos.oltp'
          newDatasource['spark.cosmos.accountKey'] = `$\{${values.name}_KEY}`.toLocaleUpperCase()
          newDatasource['spark.cosmos.accountEndpoint'] = values.endpoint
          newDatasource['spark.cosmos.database'] = values.dbtable
          newDatasource['spark.cosmos.container'] = values.container

          break
        case SourceTypeEnum.SPARKSQL:
          newDatasource[values.sparksql.type as 'sql' | 'table'] = values.sparksql.value
          break
        default:
          break
      }

      const { data } = await createSource(project, newDatasource)
      message.success('New datasource created')
      navigate(`/${project}/datasources/${data.guid}`)
    } catch (err: any) {
      message.error(err.detail || err.message)
    } finally {
      setCreateLoading(false)
    }
  }

  const onTabsChange = (tags: Tag[]) => {
    tagsRef.current = tags
  }

  useEffect(() => {
    form.setFieldsValue({
      project: projectStr?.trim()
    })
  }, [form])

  useEffect(() => {
    switch (type) {
      case SourceTypeEnum.JDBC:
        form.setFieldValue([SourceTypeEnum.JDBC, 'type'], 'dbtable')
        break
      case SourceTypeEnum.SPARKSQL:
        form.setFieldValue([SourceTypeEnum.SPARKSQL, 'type'], 'sql')
        break
    }
  }, [type])
  return {
    createLoading,
    sourceTypeOptions,
    jdbcAuthOptions,
    type,
    onTabsChange,
    onFinish
  }
}
