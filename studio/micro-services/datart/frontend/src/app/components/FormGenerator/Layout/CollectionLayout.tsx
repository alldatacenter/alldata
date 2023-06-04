/**
 * Datart
 *
 * Copyright 2021
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { ChartStyleConfig } from 'app/types/ChartConfig';
import { FC, memo, useCallback } from 'react';
import styled from 'styled-components/macro';
import { FormGeneratorLayoutProps } from '../types';
import { groupLayoutComparer } from '../utils';
import ItemLayout from './ItemLayout';

const CollectionLayout: FC<FormGeneratorLayoutProps<ChartStyleConfig>> = memo(
  ({ ancestors, translate, data, dataConfigs, flatten, onChange, context }) => {
    const getDependencyValue = useCallback((watcher, children) => {
      if (watcher?.deps) {
        // Note: only support depend on one property for now.
        const dependencyKey = watcher?.deps?.[0];
        return children?.find(r => r.key === dependencyKey)?.value;
      }
    }, []);

    return (
      <StyledCollectionLayout className="chart-config-collection-layout">
        {data?.rows
          ?.filter(r => Boolean(!r.hide))
          .map((r, index) => (
            <ItemLayout
              ancestors={ancestors.concat([index])}
              key={r.key}
              data={r}
              translate={translate}
              dependency={getDependencyValue(r.watcher, data?.rows)}
              dataConfigs={dataConfigs}
              flatten={flatten}
              onChange={onChange}
              context={context}
            />
          ))}
      </StyledCollectionLayout>
    );
  },
  groupLayoutComparer,
);

export default CollectionLayout;

const StyledCollectionLayout = styled.div``;
