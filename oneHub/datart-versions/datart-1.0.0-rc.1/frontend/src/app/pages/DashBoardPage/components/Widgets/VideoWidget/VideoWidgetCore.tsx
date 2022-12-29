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
import styled from 'styled-components';
import { Player } from 'video-react';
import 'video-react/dist/video-react.css';
import { WidgetContext } from '../../WidgetProvider/WidgetProvider';
import { videoWidgetToolKit } from './videoConfig';

export const VideoWidgetCore: React.FC = memo(() => {
  const widget = useContext(WidgetContext);
  let video = videoWidgetToolKit.getVideo(widget.config.customConfig.props);
  let srcWithParams = video.src;
  return (
    <WrapVideo className="WrapVideo">
      <Player>
        <source src={srcWithParams} />
      </Player>
    </WrapVideo>
  );
});

const WrapVideo = styled.div`
  width: 100%;
  height: 100%;
  overflow-y: auto;
  .wrap-form {
    padding: 6px;
    margin-bottom: 4px;
    background-color: ${p => p.theme.emphasisBackground};
  }
`;
