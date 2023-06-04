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
import { Form, Select } from 'antd';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import produce from 'immer';
import React, {
  memo,
  useCallback,
  useContext,
  useEffect,
  useMemo,
} from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { StoryContext } from '../contexts/StoryContext';
import {
  makeSelectStoryPagesById,
  selectSelectedPageIds,
} from '../slice/selectors';
import { updateStoryPage } from '../slice/thunks';
import {
  EFFECT_IN_OPTIONS,
  EFFECT_OUT_OPTIONS,
  EFFECT_SPEED_OPTIONS,
  StoryBoardState,
  TransitionEffect,
} from '../slice/types';

export interface StoryPageSettingProps {}
export const StoryPageSetting: React.FC<StoryPageSettingProps> = memo(() => {
  const t = useI18NPrefix(`viz.board.setting`);
  const { storyId } = useContext(StoryContext);
  const dispatch = useDispatch();
  const selectedPageIds = useSelector(
    (state: { storyBoard: StoryBoardState }) =>
      selectSelectedPageIds(state, storyId),
  );
  const pageMap = useSelector((state: { storyBoard: StoryBoardState }) =>
    makeSelectStoryPagesById(state, storyId),
  );
  const effect = useMemo(() => {
    let effect: TransitionEffect = {
      in: 'fade-in',
      out: 'fade-out',
      speed: 'fast',
    };
    if (selectedPageIds.length) {
      if (pageMap[selectedPageIds[0]]) {
        const pageEffect = pageMap[selectedPageIds[0]].config.transitionEffect;
        effect = { ...effect, ...pageEffect };
      }
    }
    return effect;
  }, [pageMap, selectedPageIds]);
  const [form] = Form.useForm();

  useEffect(() => {
    form.setFieldsValue({ ...effect });
  }, [effect, form, selectedPageIds]);

  const onValuesChange = useCallback(
    (_, allValue) => {
      selectedPageIds.forEach(pageId => {
        const oldPage = pageMap[pageId];
        const storyPage = produce(oldPage, draft => {
          draft.config.transitionEffect = allValue;
        });
        dispatch(updateStoryPage({ storyId, storyPage }));
      });
    },
    [selectedPageIds, pageMap, dispatch, storyId],
  );

  return (
    <Form
      size="small"
      form={form}
      layout="inline"
      onValuesChange={onValuesChange}
    >
      <>
        <Form.Item name="in" label={t('cutIn')}>
          <Select style={{ width: '120px' }} placeholder="Select a option">
            {EFFECT_IN_OPTIONS.map(ele => (
              <Select.Option key={ele} value={ele}>
                {ele}
              </Select.Option>
            ))}
          </Select>
        </Form.Item>
        <Form.Item name="out" label={t('cutOut')}>
          <Select style={{ width: '120px' }} placeholder="Select a option ">
            {EFFECT_OUT_OPTIONS.map(ele => (
              <Select.Option key={ele} value={ele}>
                {ele}
              </Select.Option>
            ))}
          </Select>
        </Form.Item>
        <Form.Item name="speed" label={t('speed')}>
          <Select style={{ width: '120px' }} placeholder="Select a option ">
            {EFFECT_SPEED_OPTIONS.map(ele => (
              <Select.Option key={ele} value={ele}>
                {ele}
              </Select.Option>
            ))}
          </Select>
        </Form.Item>
      </>
    </Form>
  );
});
