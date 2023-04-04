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

import { Skeleton } from 'antd';
import { ListTitle, ListTitleProps } from 'app/components';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { FC, memo, ReactNode } from 'react';
import styled from 'styled-components/macro';
import { SPACE_TIMES } from 'styles/StyleConstants';

interface ContainerProps extends ListTitleProps {
  title: string;
  loading?: boolean;
  children?: ReactNode;
}

const Container: FC<ContainerProps> = memo(props => {
  const t = useI18NPrefix('view.properties');
  const { title, children, loading, ...rest } = props;

  return (
    <StyledContainer>
      <ListTitle title={t(title)} {...rest} />
      <Skeleton active loading={loading}>
        {children}
      </Skeleton>
    </StyledContainer>
  );
});

export default Container;

const StyledContainer = styled.div`
  display: flex;
  flex: 1;
  flex-direction: column;
  width: ${SPACE_TIMES(100)};
  min-height: 0;
  border-left: 1px solid ${p => p.theme.borderColorSplit};
`;
