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

import i18n from '@/locales'
const { t } = i18n.global
export const getRemainTime = (remain: number): any => {
    if(!remain) return 
    let h = parseInt(remain / 60 / 60 % 24 + '')
    let m = parseInt(remain / 60 % 60 + '')
    let s = parseInt(remain % 60 + '')
    const hText = h > 0 ? `${h}${t('common.hour')}` : ''
    const mText = m > 0 ? `${m}${t('common.min')}` : ''
    const sText = s > 0 ? `${s}${t('common.second')}` : ''
    return hText + mText + sText
    
}

