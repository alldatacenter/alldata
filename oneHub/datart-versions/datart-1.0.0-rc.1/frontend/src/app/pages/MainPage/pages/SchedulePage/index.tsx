import React from 'react';
import { Route } from 'react-router-dom';
import styled from 'styled-components/macro';
import { useVizSlice } from '../VizPage/slice';
import { EditorPage } from './EditorPage';
import { Sidebar } from './Sidebar';
import { useScheduleSlice } from './slice';

export function SchedulePage() {
  useScheduleSlice();
  useVizSlice();
  return (
    <Container>
      <Sidebar />
      <Route
        path="/organizations/:orgId/schedules/:scheduleId"
        component={EditorPage}
      />
    </Container>
  );
}

const Container = styled.div`
  display: flex;
  flex: 1;
`;
