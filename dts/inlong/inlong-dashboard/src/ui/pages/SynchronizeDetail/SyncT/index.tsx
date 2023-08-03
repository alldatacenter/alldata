/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import React, { useState, forwardRef, useEffect } from 'react';
import { defaultSize } from '@/configs/pagination';
import { useRequest, useUpdateEffect } from '@/ui/hooks';
import SourceSinkCard from './SourceSinkCard';

const Comp = ({ inlongGroupId, readonly, mqType }) => {
  const [options, setOptions] = useState({
    pageSize: defaultSize,
    pageNum: 1,
  });

  const [inlongStreamId, setInlongStreamId] = useState('');

  const { data, run: getList } = useRequest(
    {
      url: '/stream/list',
      method: 'POST',
      data: {
        ...options,
        inlongGroupId,
      },
    },
    {
      refreshDeps: [options],
      onSuccess: result => {
        const [item] = result?.list || [];
        setInlongStreamId(item.inlongStreamId);
      },
    },
  );

  useEffect(() => {
    if (inlongGroupId !== null) {
      getList();
    }
  }, []);

  return (
    <>
      <SourceSinkCard inlongGroupId={inlongGroupId} inlongStreamId={inlongStreamId} />
    </>
  );
};

export default forwardRef(Comp);
