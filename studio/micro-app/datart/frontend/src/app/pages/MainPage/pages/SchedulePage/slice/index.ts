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
  updateScheduleBase,
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
  updateLoading: false,
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
      state.schedules = action.payload.map(v => ({
        ...v,
        deleteLoading: false,
      }));
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
      state.archived = action.payload.map(v => ({
        ...v,
        deleteLoading: false,
      }));
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
      state.editingSchedule = { ...action.payload, deleteLoading: false };
    });
    builder.addCase(getScheduleDetails.rejected, state => {
      state.scheduleDetailsLoading = false;
    });
    // addSchedule
    builder.addCase(addSchedule.pending, state => {
      state.saveLoading = true;
    });
    builder.addCase(addSchedule.fulfilled, (state, action) => {
      state.saveLoading = false;
      state.schedules?.unshift({ ...action.payload, deleteLoading: false });
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
        ({ id }) => id !== action.meta.arg.schedule.id,
      );
    });
    builder.addCase(unarchiveSchedule.rejected, state => {
      state.unarchiveScheduleLoading = false;
    });

    // deleteSchedule
    builder.addCase(deleteSchedule.pending, (state, action) => {
      const schedule =
        state.schedules?.find(({ id }) => id === action.meta.arg.id) ||
        state.archived?.find(({ id }) => id === action.meta.arg.id);
      if (schedule) {
        schedule.deleteLoading = true;
      }
    });
    builder.addCase(deleteSchedule.fulfilled, (state, action) => {
      state.schedules = state.schedules.filter(
        ({ id }) => id !== action.meta.arg.id,
      );
      state.archived = state.archived.filter(
        ({ id }) => id !== action.meta.arg.id,
      );
    });
    builder.addCase(deleteSchedule.rejected, (state, action) => {
      const schedule =
        state.schedules?.find(({ id }) => id === action.meta.arg.id) ||
        state.archived?.find(({ id }) => id === action.meta.arg.id);
      if (schedule) {
        schedule.deleteLoading = false;
      }
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

    // updateScheduleBase
    builder.addCase(updateScheduleBase.pending, state => {
      state.updateLoading = true;
    });
    builder.addCase(updateScheduleBase.fulfilled, (state, action) => {
      state.updateLoading = false;
      state.schedules = state.schedules.map(v =>
        v.id === action.payload.id
          ? {
              ...v,
              ...action.payload,
              parentId: action.payload.parentId || null,
              deleteLoading: false,
            }
          : v,
      );
    });
    builder.addCase(updateScheduleBase.rejected, state => {
      state.updateLoading = false;
    });
  },
});

export const { actions: sourceActions, reducer } = slice;

export const useScheduleSlice = () => {
  useInjectReducer({ key: slice.name, reducer: slice.reducer });
  return { actions: slice.actions };
};
