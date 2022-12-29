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
import { DeleteOutlined } from '@ant-design/icons';
import { Upload } from 'antd';
import { BoardContext } from 'app/pages/DashBoardPage/components/BoardProvider/BoardProvider';
import { convertImageUrl } from 'app/pages/DashBoardPage/utils';
import { memo, useCallback, useContext, useMemo } from 'react';
import { useDispatch } from 'react-redux';
import styled from 'styled-components/macro';
import { SPACE_MD } from 'styles/StyleConstants';
import { uploadBoardImage } from '../../../../slice/thunk';

export const UploadDragger: React.FC<{
  value: string;
  onChange?: any;
  placeholder: string;
}> = memo(({ value, onChange, placeholder }) => {
  const dispatch = useDispatch();
  const { boardId } = useContext(BoardContext);

  const beforeUpload = useCallback(
    async info => {
      const formData = new FormData();
      formData.append('file', info);
      dispatch(
        uploadBoardImage({
          boardId,
          fileName: info.name,
          formData: formData,
          resolve: onChange,
        }),
      );
      return false;
    },
    [boardId, dispatch, onChange],
  );
  const getImageError = useCallback(() => {
    console.warn('get BackgroundImageError');
  }, []);
  const delImageUrl = useCallback(
    e => {
      e.stopPropagation();
      onChange('');
    },
    [onChange],
  );
  const imgUrl = useMemo(() => convertImageUrl(value), [value]);
  return (
    <StyleUpload
      name={'upload-image'}
      className="datart-ant-upload"
      beforeUpload={beforeUpload}
      multiple={false}
    >
      {value ? (
        <div className="image-box">
          <img className="image" src={imgUrl} alt="" onError={getImageError} />
          <DeleteOutlined className="del-button" onClick={delImageUrl} />
        </div>
      ) : (
        <Placeholder>{placeholder}</Placeholder>
      )}
    </StyleUpload>
  );
});
const StyleUpload = styled(Upload.Dragger)`
  .image-box {
    position: relative;
    padding: 0 ${SPACE_MD};
    margin: auto;
    overflow: hidden;
  }

  .del-button {
    position: absolute;
    top: 50%;
    left: 50%;
    display: none;
    font-size: 1.5rem;
    color: ${p => p.theme.textColorDisabled};
    transform: translate(-50%, -50%);
  }

  .image-box:hover .del-button {
    display: block;
  }

  .image {
    width: 100%;
    height: auto;
  }
`;

const Placeholder = styled.p`
  color: ${p => p.theme.textColorLight};
`;
