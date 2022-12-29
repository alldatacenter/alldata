import { createSelector } from '@reduxjs/toolkit';
import { RootState } from 'types';
import { initialState } from '.';

const selectDomain = (state: RootState) => state.schedule || initialState;

export const selectSchedules = createSelector(
  [selectDomain],
  scheduleState => scheduleState.schedules,
);

export const selectArchived = createSelector(
  [selectDomain],
  scheduleState => scheduleState.archived,
);

export const selectScheduleListLoading = createSelector(
  [selectDomain],
  scheduleState => scheduleState.scheduleListLoading,
);

export const selectArchivedListLoading = createSelector(
  [selectDomain],
  scheduleState => scheduleState.archivedListLoading,
);

export const selectEditingSchedule = createSelector(
  [selectDomain],
  scheduleState => scheduleState.editingSchedule,
);

export const selectScheduleDetailLoading = createSelector(
  [selectDomain],
  scheduleState => scheduleState.scheduleDetailsLoading,
);

export const selectSaveLoading = createSelector(
  [selectDomain],
  scheduleState => scheduleState.saveLoading,
);

export const selectDeleteLoading = createSelector(
  [selectDomain],
  scheduleState => scheduleState.deleteLoading,
);

export const selectUnarchiveLoading = createSelector(
  [selectDomain],
  scheduleState => scheduleState.unarchiveScheduleLoading,
);

export const selectScheduleLogs = createSelector(
  [selectDomain],
  scheduleState => scheduleState.logs,
);
export const selectScheduleLogsLoading = createSelector(
  [selectDomain],
  scheduleState => scheduleState.logsLoading,
);
