import { Split } from 'app/components';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { useSplitSizes } from 'app/hooks/useSplitSizes';
import { dispatchResize } from 'app/utils/dispatchResize';
import { useCallback, useState } from 'react';
import { Route } from 'react-router-dom';
import styled from 'styled-components/macro';
import { useVizSlice } from '../VizPage/slice';
import { EditorPage } from './EditorPage';
import { SaveForm } from './SaveForm';
import { SaveFormContext, useSaveFormContext } from './SaveFormContext';
import { Sidebar } from './Sidebar';
import { useScheduleSlice } from './slice';

export function SchedulePage() {
  const tg = useI18NPrefix('global');
  const saveFormContextValue = useSaveFormContext();
  useScheduleSlice();
  useVizSlice();

  const [sliderVisible, setSliderVisible] = useState<boolean>(false);

  const { sizes, setSizes } = useSplitSizes({
    limitedSide: 0,
    range: [256, 768],
  });

  const [isDragging, setIsDragging] = useState(false);

  const siderDragEnd = useCallback(
    sizes => {
      setSizes(sizes);
      dispatchResize();
      setIsDragging(false);
    },
    [setSizes, setIsDragging],
  );

  const siderDragStart = useCallback(() => {
    if (!isDragging) setIsDragging(true);
  }, [setIsDragging, isDragging]);

  const handleSliderVisible = useCallback(
    (status: boolean) => {
      setSliderVisible(status);
      setTimeout(() => {
        dispatchResize();
      }, 300);
    },
    [setSliderVisible],
  );

  return (
    <SaveFormContext.Provider value={saveFormContextValue}>
      <Container
        sizes={sizes}
        minSize={[256, 0]}
        maxSize={[768, Infinity]}
        gutterSize={0}
        onDragStart={siderDragStart}
        onDragEnd={siderDragEnd}
        className="datart-split"
        sliderVisible={sliderVisible}
      >
        <Sidebar
          width={sizes[0]}
          isDragging={isDragging}
          sliderVisible={sliderVisible}
          handleSliderVisible={handleSliderVisible}
        />
        <EditorPageWrapper className={sliderVisible ? 'close' : ''}>
          <Route
            path="/organizations/:orgId/schedules/:scheduleId"
            component={EditorPage}
          />
        </EditorPageWrapper>
        <SaveForm
          formProps={{
            labelAlign: 'left',
            labelCol: { offset: 1, span: 8 },
            wrapperCol: { span: 13 },
          }}
          okText={tg('button.save')}
        />
      </Container>
    </SaveFormContext.Provider>
  );
}

const Container = styled(Split)<{ sliderVisible: boolean }>`
  display: flex;
  flex: 1;
  min-width: 0;
  min-height: 0;
  .gutter-horizontal {
    display: ${p => (p.sliderVisible ? 'none' : 'block')};
  }
`;

const EditorPageWrapper = styled.div`
  display: flex;
  flex: 1;
  min-width: 0;
  min-height: 0;
  &.close {
    width: calc(100% - 30px) !important;
    min-width: calc(100% - 30px) !important;
    padding-left: 30px;
  }
`;
