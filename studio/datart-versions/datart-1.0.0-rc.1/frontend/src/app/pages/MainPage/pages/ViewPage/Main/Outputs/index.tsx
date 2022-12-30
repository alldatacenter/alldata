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

import { GithubOutlined } from '@ant-design/icons';
import { Alert, Button, Popover, Space, Spin } from 'antd';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import useResizeObserver from 'app/hooks/useResizeObserver';
import { selectSystemInfo } from 'app/slice/selectors';
import { transparentize } from 'polished';
import React, { memo, useCallback } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import styled from 'styled-components/macro';
import { SPACE_MD, SPACE_TIMES, SPACE_XS } from 'styles/StyleConstants';
import { newIssueUrl } from 'utils/utils';
import { ViewViewModelStages } from '../../constants';
import { useViewSlice } from '../../slice';
import { selectCurrentEditingViewAttr } from '../../slice/selectors';
import { Error } from './Error';
import { Results } from './Results';

export const Outputs = memo(() => {
  const { actions } = useViewSlice();
  const dispatch = useDispatch();
  const systemInfo = useSelector(selectSystemInfo);
  const t = useI18NPrefix('view');

  const error = useSelector(state =>
    selectCurrentEditingViewAttr(state, { name: 'error' }),
  ) as string;
  const stage = useSelector(state =>
    selectCurrentEditingViewAttr(state, { name: 'stage' }),
  ) as ViewViewModelStages;
  const warnings = useSelector(state =>
    selectCurrentEditingViewAttr(state, { name: 'warnings' }),
  ) as string[];

  const { width, height, ref } = useResizeObserver({
    refreshMode: 'debounce',
    refreshRate: 200,
  });

  const removeViewWarnings = useCallback(() => {
    dispatch(
      actions.changeCurrentEditingView({
        warnings: null,
      }),
    );
  }, [dispatch, actions]);

  const submitIssue = useCallback(
    type => {
      let params: any = {
        type: type,
        title: 'Sql parse bug',
      };
      if (type === 'github') {
        params.body = `version: ${systemInfo?.version}\n` + warnings.join('');
      } else {
        params.description =
          `version: ${systemInfo?.version}\n` + warnings.join('');
      }
      let url = newIssueUrl(params);
      window.open(url);
    },
    [warnings, systemInfo],
  );

  return (
    <Wrapper ref={ref}>
      {warnings && (
        <Alert
          className="warningBox"
          message=""
          description={
            <p>
              {t('sqlRunWraning')}
              <Popover
                trigger={['click']}
                placement="top"
                overlayStyle={{ width: SPACE_TIMES(96) }}
                content={t('warningDescription')}
              >
                <Button className="detail" type="link" size="small">
                  {t('detail')}
                </Button>
              </Popover>
            </p>
          }
          type="warning"
          closable={false}
          action={
            <Space>
              <Button
                type="primary"
                icon={<GithubOutlined />}
                onClick={() => submitIssue('github')}
              >
                Github
              </Button>
              <Button type="primary" onClick={() => submitIssue('gitee')}>
                Gitee
              </Button>
              <Button onClick={removeViewWarnings}>{t('close')}</Button>
            </Space>
          }
        />
      )}

      <Results width={width} height={height} />
      {error && <Error />}
      {stage === ViewViewModelStages.Running && (
        <LoadingMask>
          <Spin />
        </LoadingMask>
      )}
    </Wrapper>
  );
});

const Wrapper = styled.div`
  position: relative;
  display: flex;
  flex-direction: column;
  border-top: 1px solid ${p => p.theme.borderColorSplit};

  .warningBox {
    padding: ${SPACE_XS} ${SPACE_MD};

    .detail {
      padding: 0;
    }
  }
`;

const LoadingMask = styled.div`
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: ${p => transparentize(0.5, p.theme.componentBackground)};
`;
