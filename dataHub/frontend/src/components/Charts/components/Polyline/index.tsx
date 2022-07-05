import React, { useEffect, useState } from 'react'
import { ChartDProps, DataItem, LabelProps } from '@/components/Charts/data'
import { regFunction } from '@/utils/utils'
import { Table } from 'antd'
import { ColumnsType } from 'antd/es/table'
import isEmpty from 'lodash/isEmpty'

import Chart from 'bizcharts/lib/components/Chart'
import Geom from 'bizcharts/lib/geometry/index'
import Axis from 'bizcharts/lib/components/Axis'
import Tooltip from 'bizcharts/lib/components/Tooltip'
import Legend from 'bizcharts/lib/components/Legend'
import DataMarker from 'bizcharts/lib/components/Annotation/dataMarker'
import Interaction from 'bizcharts/lib/components/Interaction'
import { registerInteraction } from '@antv/g2/lib/interaction'

import styles from './index.less'

export default ({ height = 600, data }: ChartDProps) => {
  const [chart, setChart] = useState<DataItem[]>()
  const [point, setPoint] = useState({ x: '', y: 0, content: '' })

  useEffect(() => {
    setChart(data?.map((i) => ({ ...i, lab_y: Number(i.lab_y) })))
    data?.forEach((i: any) => {
      if (!isEmpty(i.desc)) {
        setPoint({ x: i.lab_x, y: Number(i.lab_y), content: i.desc })
      }
    })
  }, [data])

  // 鼠标点击plot,锁定Tooltip，再次点击解锁
  registerInteraction('locked-tooltip', {
    start: [
      {
        trigger: 'plot:click',
        action: (context) => {
          const locked = context.view.isTooltipLocked()
          if (locked) {
            context.view.unlockTooltip()
          } else {
            context.view.lockTooltip()
          }
        },
      },
      { trigger: 'plot:mousemove', action: 'tooltip:show' },
    ],
    end: [{ trigger: 'plot:mouseleave', action: 'tooltip:hide' }],
  })

  registerInteraction('active-region-click', {
    start: [{ trigger: 'plot:click', action: 'active-region:show' }],
    end: [{ trigger: 'tooltip:mouseleave', action: 'active-region:hide' }],
  })

  const columns: ColumnsType<LabelProps> = [
    {
      title: '传播节点',
      dataIndex: 'lab_z',
      align: 'left',
      render: (_, record) => (
        <a className={styles.name} href={record.url} target="_blank" rel="noreferrer">
          {record.lab_z}
        </a>
      ),
    },
    {
      title: '声量',
      dataIndex: 'lab_y',
      align: 'right',
      render: (_, record) => regFunction(record.lab_y) || '-',
    },
  ]

  return (
    <>
      <Chart
        height={height}
        data={chart}
        autoFit
        // scale={{
        //   lab_y: {
        //     max:
        //       accurate.add(
        //         max(data?.map((i) => i.lab_y)),
        //         accurate.divide(max(data?.map((i) => i.lab_y)), 4),
        //       ) || max(data?.map((i) => i.lab_y)),
        //   },
        // }}
      >
        <Legend visible={false} />
        <Axis name="lab_x" label={{ autoRotate: true }} />
        <Axis name="lab_y" />
        <Tooltip follow enterable crosshairs={{ type: 'y' }}>
          {(title, items: any) => {
            const dataSource = items[0].data.lab_z.map((i: LabelProps, key: number) => ({
              ...i,
              key,
            }))
            return (
              <>
                <Table
                  className={styles.polylineTable}
                  size="small"
                  rowKey="key"
                  bordered={false}
                  columns={columns}
                  dataSource={dataSource}
                  pagination={false}
                />
                <Interaction type="active-region-click" />
              </>
            )
          }}
        </Tooltip>
        <Interaction type="locked-tooltip" />
        <Interaction type="active-region" />
        <Geom type="line" position="lab_x*lab_y" size={2} color="#1890ff" shape="smooth" />
        {data && data?.some((i) => !isEmpty(i.desc)) && (
          <DataMarker
            position={[point.x, point.y]}
            autoAdjust
            direction="downward"
            point={{ style: { stroke: '#ff4d4f' } }}
            line={{ style: { color: '#ccc' }, length: 10 }}
            text={{
              style: { fontSize: 13, textAlign: 'right' },
              content: point.content,
            }}
          />
        )}
      </Chart>
    </>
  )
}
