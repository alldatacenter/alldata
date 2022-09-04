import React from 'react'
import DonutChart from 'bizcharts/lib/plots/DonutChart'
import accurate from 'accurate'
import { ChartDProps } from '@/components/Charts/data'
import { SourcesColor } from '@/types/const'
import findLast from 'lodash/findLast'

const defaultData = [
  { lab_x: '微博', lab_y: 0.4 },
  { lab_x: '微信', lab_y: 0.21 },
  { lab_x: '知乎', lab_y: 0.17 },
  { lab_x: '网站', lab_y: 0.13 },
  { lab_x: '哔哩哔哩', lab_y: 0.09 },
  { lab_x: '其他', lab_y: 0.09 },
]

export default ({ height = 500, data = defaultData }: ChartDProps) => {
  return (
    <DonutChart
      // @ts-ignore
      data={data}
      // width={width}
      height={height}
      forceFit
      radius={0.8}
      colorField="lab_x"
      angleField="lab_y"
      meta={{
        lab_y: {
          formatter: (v) => {
            return `${accurate.multiply([v, [100]])}%`
          },
        },
      }}
      legend={{
        position: 'bottom-center',
      }}
      tooltip={{
        visible: true,
      }}
      label={{
        visible: true,
        type: 'spider',
        formatter: (angleField, colorField) => {
          // eslint-disable-next-line no-underscore-dangle
          return `${colorField._origin.lab_x}: ${
            // eslint-disable-next-line no-underscore-dangle
            accurate.multiply([colorField._origin.lab_y, [100]])
          }%`
        },
      }}
      color={(sourceName: string) =>
        (SourcesColor.find((i) => i.source === sourceName) || findLast(SourcesColor))?.color
      }
      statistic={{
        visible: false,
      }}
    />
  )
}
