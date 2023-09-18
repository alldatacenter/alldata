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
import { memo, useContext } from 'react';
import styled from 'styled-components/macro';
import { WidgetContext } from '../../WidgetProvider/WidgetProvider';
import { iframeWidgetToolKit } from './iframeConfig';

export const IframeWidgetCore: React.FC<{}> = memo(() => {
  const widget = useContext(WidgetContext);
  // getWidgetIframe;
  const iframeVal = iframeWidgetToolKit.getIframe(
    widget.config.customConfig.props,
  );

  return (
    <Wrapper>
      <iframe
        title=" "
        src={iframeVal.src}
        frameBorder="0"
        allow="autoplay"
        style={{ width: '100%', height: '100%' }}
      ></iframe>
    </Wrapper>
  );
});
const Wrapper = styled.div`
  display: flex;
  flex-direction: column;
  width: 100%;

  .wrap-form {
    padding: 4px;
    margin-bottom: 4px;
    background-color: ${p => p.theme.emphasisBackground};
  }
`;
