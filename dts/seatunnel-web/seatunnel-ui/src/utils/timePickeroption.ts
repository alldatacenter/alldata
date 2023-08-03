/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

export const getNowDate = (): any => {
  return [
    new Date(new Date().toLocaleDateString()).getTime(),
    new Date().getTime()
  ]
}

function getTimeRange(time: any) {
  const cur = new Date(new Date().toLocaleDateString()).getTime()
  return [cur - time * 24 * 60 * 60 * 1000, cur] as const
}

export const getDataRange = (item: any) => {
  let data
  if (item == 0) {
    data = getNowDate()
  } else {
    data = getTimeRange(item)
  }
  return data
}

export function getRangeShortCuts(t: any) {
  // const { t } = useI18n()
  const rangeShortCuts = {} as any
  const rangeMap = {
    today: 0,
    last_day: 1,
    last_three_day: 3,
    last_week: 7,
    last_month: 30
  } as any
  Object.keys(rangeMap).forEach((item: string) => {
    rangeShortCuts[`${t(`project.workflow.${item}`)}` as string] = getDataRange(
      rangeMap[item]
    )
  })
  return rangeShortCuts
}
