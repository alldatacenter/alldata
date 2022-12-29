import { createSlice } from '@reduxjs/toolkit';
import { useInjectReducer } from 'utils/@reduxjs/injectReducer';
import {
  addSchedule,
  deleteSchedule,
  editSchedule,
  getArchivedSchedules,
  getScheduleDetails,
  getScheduleErrorLogs,
  getSchedules,
  unarchiveSchedule,
} from './thunks';
import { ScheduleState } from './types';

export const initialState: ScheduleState = {
  schedules: [],
  archived: [],
  scheduleListLoading: false,
  archivedListLoading: false,
  editingSchedule: null,
  scheduleDetailsLoading: false,
  saveLoading: false,
  unarchiveScheduleLoading: false,
  deleteLoading: false,
  logs: [],
  logsLoading: false,
};

const slice = createSlice({
  name: 'schedule',
  initialState,
  reducers: {
    clearEditingSchedule(state) {
      state.editingSchedule = null;
    },
    setEditingScheduleActive(state, { payload: active }) {
      if (state.editingSchedule) {
        state.editingSchedule.active = active;
      }
    },
    clearLogs(state) {
      state.logs = [];
    },
  },
  extraReducers: builder => {
    // getSchedules
    builder.addCase(getSchedules.pending, state => {
      state.scheduleListLoading = true;
    });
    builder.addCase(getSchedules.fulfilled, (state, action) => {
      state.scheduleListLoading = false;
      state.schedules = action.payload;
    });
    builder.addCase(getSchedules.rejected, state => {
      state.scheduleListLoading = false;
    });
    // getArchivedSchedules
    builder.addCase(getArchivedSchedules.pending, state => {
      state.archivedListLoading = true;
    });
    builder.addCase(getArchivedSchedules.fulfilled, (state, action) => {
      state.archivedListLoading = false;
      state.archived = action.payload;
    });
    builder.addCase(getArchivedSchedules.rejected, state => {
      state.archivedListLoading = false;
    });
    // getScheduleDetails
    builder.addCase(getScheduleDetails.pending, state => {
      state.scheduleDetailsLoading = true;
    });
    builder.addCase(getScheduleDetails.fulfilled, (state, action) => {
      state.scheduleDetailsLoading = false;
      state.editingSchedule = action.payload;
    });
    builder.addCase(getScheduleDetails.rejected, state => {
      state.scheduleDetailsLoading = false;
    });
    // addSchedule
    builder.addCase(addSchedule.pending, state => {
      state.saveLoading = true;
    });
    builder.addCase(addSchedule.fulfilled, state => {
      state.saveLoading = false;
    });
    builder.addCase(addSchedule.rejected, state => {
      state.saveLoading = false;
    });

    // editSchedule
    builder.addCase(editSchedule.pending, state => {
      state.saveLoading = true;
    });
    builder.addCase(editSchedule.fulfilled, state => {
      state.saveLoading = false;
    });
    builder.addCase(editSchedule.rejected, state => {
      state.saveLoading = false;
    });

    // unarchiveSchedule
    builder.addCase(unarchiveSchedule.pending, state => {
      state.unarchiveScheduleLoading = true;
    });
    builder.addCase(unarchiveSchedule.fulfilled, (state, action) => {
      state.unarchiveScheduleLoading = false;
      state.archived = state.archived.filter(
        ({ id }) => id !== action.meta.arg.id,
      );
    });
    builder.addCase(unarchiveSchedule.rejected, state => {
      state.unarchiveScheduleLoading = false;
    });

    // deleteSchedule
    builder.addCase(deleteSchedule.pending, state => {
      state.deleteLoading = true;
    });
    builder.addCase(deleteSchedule.fulfilled, (state, action) => {
      state.deleteLoading = false;
      if (action.meta.arg.archive) {
        state.schedules = state.schedules.filter(
          ({ id }) => id !== action.meta.arg.id,
        );
      } else {
        state.archived = state.archived.filter(
          ({ id }) => id !== action.meta.arg.id,
        );
      }
    });
    builder.addCase(deleteSchedule.rejected, state => {
      state.deleteLoading = false;
    });

    // getScheduleErrorLogs
    builder.addCase(getScheduleErrorLogs.pending, state => {
      state.logsLoading = true;
    });
    builder.addCase(getScheduleErrorLogs.fulfilled, (state, action) => {
      state.logsLoading = false;
      state.logs = action.payload;
    });
    builder.addCase(getScheduleErrorLogs.rejected, state => {
      state.logsLoading = false;
    });
  },
});

export const { actions: sourceActions, reducer } = slice;

export const useScheduleSlice = () => {
  useInjectReducer({ key: slice.name, reducer: slice.reducer });
  return { actions: slice.actions };
};
