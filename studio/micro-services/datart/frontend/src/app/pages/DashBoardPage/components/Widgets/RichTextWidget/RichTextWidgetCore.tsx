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
import { Modal } from 'antd';
import {
  CustomColor,
  QuillPalette,
} from 'app/components/ChartGraph/BasicRichText/RichTextPluginLoader/CustomColor';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { WidgetInfo } from 'app/pages/DashBoardPage/pages/Board/slice/types';
import { editBoardStackActions } from 'app/pages/DashBoardPage/pages/BoardEditor/slice';
import { Widget } from 'app/pages/DashBoardPage/types/widgetTypes';
import produce from 'immer';
import { DeltaStatic } from 'quill';
import { ImageDrop } from 'quill-image-drop-module'; // 拖动加载图片组件。
import QuillMarkdown from 'quilljs-markdown';
import 'quilljs-markdown/dist/quilljs-markdown-common-style.css';
import React, {
  useCallback,
  useContext,
  useEffect,
  useLayoutEffect,
  useMemo,
  useRef,
  useState,
} from 'react';
import ReactQuill, { Quill } from 'react-quill';
// DeltaStatic
import 'react-quill/dist/quill.snow.css';
import { useDispatch } from 'react-redux';
import styled from 'styled-components/macro';
import { SPACE_TIMES } from 'styles/StyleConstants';
import { WidgetActionContext } from '../../ActionProvider/WidgetActionProvider';
import { Formats, MarkdownOptions } from './config';

Quill.register('modules/imageDrop', ImageDrop);

const CUSTOM_COLOR_INIT = {
  background: 'transparent',
  color: '#000',
};

type RichTextWidgetProps = {
  widget: Widget;
  widgetInfo: WidgetInfo;
  boardEditing: boolean;
};
export const RichTextWidgetCore: React.FC<RichTextWidgetProps> = ({
  widget,
  widgetInfo,
  boardEditing,
}) => {
  const t = useI18NPrefix();
  const dispatch = useDispatch();

  const { onEditClearActiveWidgets } = useContext(WidgetActionContext);
  const initContent = useMemo(() => {
    return (widget.config.content as any).richText?.content;
  }, [widget.config.content]);
  const [quillValue, setQuillValue] = useState<DeltaStatic | undefined>(
    initContent,
  );
  const [containerId, setContainerId] = useState<string>();
  const [quillModules, setQuillModules] = useState<any>(null);

  const [customColorVisible, setCustomColorVisible] = useState<boolean>(false);
  const [customColor, setCustomColor] = useState<{
    background: string;
    color: string;
  }>({ ...QuillPalette.RICH_TEXT_CUSTOM_COLOR_INIT });
  const [customColorType, setCustomColorType] = useState<
    'color' | 'background'
  >('color');
  const [contentSavable, setContentSavable] = useState(false);

  useEffect(() => {
    if (widgetInfo.editing) {
      setQuillValue(initContent);
    }
  }, [initContent, widgetInfo.editing]);

  useEffect(() => {
    if (widgetInfo.editing === false && contentSavable && boardEditing) {
      if (quillRef.current) {
        let contents = quillRef.current?.getEditor().getContents();
        const strContents = JSON.stringify(contents);
        if (strContents !== JSON.stringify(initContent)) {
          const nextMediaWidgetContent = produce(
            widget.config.content,
            draft => {
              (draft as any).richText = {
                content: JSON.parse(strContents || '{}'),
              };
            },
          ) as any;

          dispatch(
            editBoardStackActions.changeMediaWidgetConfig({
              id: widget.id,
              mediaWidgetContent: nextMediaWidgetContent,
            }),
          );
          setContentSavable(false);
        }
      }
    }
  }, [
    boardEditing,
    dispatch,
    initContent,
    contentSavable,
    widget.config.content,
    widget.id,
    widgetInfo.editing,
  ]);

  useEffect(() => {
    const newId = `rich-text-${widgetInfo.id + new Date().getTime()}`;
    setContainerId(newId);
    const modules = {
      toolbar: {
        container: `#${newId}`,
        handlers: {
          color: function (value) {
            if (value === QuillPalette.RICH_TEXT_CUSTOM_COLOR) {
              setCustomColorType('color');
              setCustomColorVisible(true);
            }
            quillRef.current!.getEditor().format('color', value);
          },
          background: function (value) {
            if (value === QuillPalette.RICH_TEXT_CUSTOM_COLOR) {
              setCustomColorType('background');
              setCustomColorVisible(true);
            }
            quillRef.current!.getEditor().format('background', value);
          },
        },
      },
      imageDrop: true,
    };
    setQuillModules(modules);
  }, [widgetInfo.id]);

  const quillRef = useRef<ReactQuill>(null);

  useLayoutEffect(() => {
    if (quillRef.current) {
      quillRef.current
        .getEditor()
        .on('selection-change', (r: { index: number; length: number }) => {
          if (!r?.index) return;
          try {
            const index = r.length === 0 ? r.index - 1 : r.index;
            const length = r.length === 0 ? 1 : r.length;
            const delta = quillRef
              .current!.getEditor()
              .getContents(index, length);

            if (delta.ops?.length === 1 && delta.ops[0]?.attributes) {
              const { background, color } = delta.ops[0].attributes;
              setCustomColor({
                background: background || CUSTOM_COLOR_INIT.background,
                color: color || CUSTOM_COLOR_INIT.color,
              });

              const colorNode = document.querySelector(
                '.ql-color .ql-color-label',
              );
              const backgroundNode = document.querySelector(
                '.ql-background .ql-color-label',
              );
              if (color && !colorNode?.getAttribute('style')) {
                colorNode!.setAttribute('style', `stroke: ${color}`);
              }
              if (background && !backgroundNode?.getAttribute('style')) {
                backgroundNode!.setAttribute('style', `fill: ${background}`);
              }
            } else {
              setCustomColor({ ...CUSTOM_COLOR_INIT });
            }
          } catch (error) {
            console.error('selection-change callback | error', error);
          }
        });
      new QuillMarkdown(quillRef.current.getEditor(), MarkdownOptions);
    }
  }, [quillModules]);

  useEffect(() => {
    let palette: QuillPalette | null = null;
    if (quillRef.current && containerId) {
      palette = new QuillPalette(quillRef.current, {
        toolbarId: containerId,
        onChange: setCustomColor,
      });
    }

    return () => {
      palette?.destroy();
    };
  }, [containerId]);

  const ssp = e => {
    e.stopPropagation();
  };

  const quillChange = useCallback(() => {
    if (quillRef.current && quillRef.current?.getEditor()) {
      let contents = quillRef.current!.getEditor().getContents();
      setQuillValue(contents);
    }
  }, []);

  const toolbar = useMemo(
    () => QuillPalette.getToolbar({ id: containerId as string, t }),
    [containerId, t],
  );

  const customColorChange = color => {
    if (color) {
      quillRef.current!.getEditor().format(customColorType, color);
    }
    setCustomColorVisible(false);
  };

  const modalCancel = useCallback(() => {
    onEditClearActiveWidgets();
  }, [onEditClearActiveWidgets]);

  const modalOk = useCallback(() => {
    setContentSavable(true);
    modalCancel();
  }, [modalCancel]);

  return (
    <TextWrap onClick={ssp}>
      <ReactQuill
        className="react-quill"
        value={initContent}
        modules={{ toolbar: null }}
        formats={Formats}
        readOnly={true}
      />
      <CustomColor
        visible={customColorVisible}
        onCancel={() => setCustomColorVisible(false)}
        color={customColor?.[customColorType]}
        colorChange={customColorChange}
      />
      <Modal
        width={992}
        closable={false}
        maskClosable={false}
        keyboard={false}
        visible={widgetInfo.editing}
        onOk={modalOk}
        onCancel={modalCancel}
      >
        {quillModules && (
          <ModalBody>
            {toolbar}
            <ReactQuill
              ref={quillRef}
              className="react-quill"
              placeholder={t('viz.board.setting.enterHere')}
              value={quillValue}
              onChange={quillChange}
              modules={quillModules}
              formats={Formats}
              readOnly={false}
            />
          </ModalBody>
        )}
      </Modal>
    </TextWrap>
  );
};
export default RichTextWidgetCore;

const TextWrap = styled.div`
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;

  & .react-quill {
    width: 100%;
    height: 100%;
  }

  & .ql-snow {
    border: none;
  }

  & .ql-container.ql-snow {
    border: none;
  }

  & .ql-editor {
    position: absolute;
    top: 50%;
    left: 0;
    width: 100%;
    height: auto;
    max-height: 100%;
    transform: translate(0, -50%);
  }
`;

const ModalBody = styled.div`
  & .ql-editor {
    min-height: ${SPACE_TIMES(60)};
  }
`;
