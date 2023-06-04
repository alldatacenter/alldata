import { prefixI18N } from 'app/hooks/useI18NPrefix';
import Quill from 'quill';
import { ImageDrop } from 'quill-image-drop-module';
import { FC } from 'react';
import ReactQuill from 'react-quill';
import 'react-quill/dist/quill.snow.css';
import styled from 'styled-components/macro';
Quill.register('modules/imageDrop', ImageDrop);

export const Formats = [
  'header',
  'bold',
  'italic',
  'underline',
  'strike',
  'blockquote',
  'list',
  'bullet',
  'indent',
  'link',
  'color',
  'tag',
  'calcfield',
  'mention',
  'image',
];
interface CommonRichTextProps {
  value?: string;
  onChange?: (v?: string) => void;
  placeholder?: string;
}
export const CommonRichText: FC<CommonRichTextProps> = ({
  children,
  placeholder = prefixI18N(
    'schedule.editor.emailSettingForm.commonRichText.pleaseEnter',
  ),
  ...restProps
}) => {
  return (
    <ReactQuillWrapper>
      <ReactQuill
        theme="snow"
        placeholder={placeholder}
        formats={Formats}
        modules={{
          toolbar: [
            [{ header: [1, 2, false] }],
            ['bold', 'italic', 'underline', 'strike', 'blockquote'],
            [
              { list: 'ordered' },
              { list: 'bullet' },
              { indent: '-1' },
              { indent: '+1' },
            ],
            ['link', 'image'],
            ['clean'],
          ],
        }}
        {...restProps}
      />
    </ReactQuillWrapper>
  );
};

const ReactQuillWrapper = styled.div`
  .ql-container {
    min-height: 260px;
  }
`;
