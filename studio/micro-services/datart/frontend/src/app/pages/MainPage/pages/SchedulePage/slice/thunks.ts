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

import { createAsyncThunk } from '@reduxjs/toolkit';
import { request2 } from 'utils/request';
import {
  DeleteScheduleParams,
  EditScheduleParamsResolve,
  ErrorLog,
  Schedule,
  ScheduleBase,
  ScheduleParamsResolve,
  ScheduleSimple,
  UpdateScheduleBaseParams,
} from './types';

export const getSchedules = createAsyncThunk<ScheduleSimple[], string>(
  'schedule/getSchedules',
  async orgId => {
    const { data } = await request2<ScheduleSimple[]>({
      url: '/schedules',
      method: 'GET',
      params: { orgId },
    });
    return data;
  },
);

export const getArchivedSchedules = createAsyncThunk<ScheduleSimple[], string>(
  'schedule/getArchivedSchedules',
  async orgId => {
    const { data } = await request2<ScheduleSimple[]>({
      url: '/schedules/archived',
      method: 'GET',
      params: { orgId },
    });
    return data;
  },
);

export const getScheduleDetails = createAsyncThunk<ScheduleSimple, string>(
  'schedule/getScheduleDetails',
  async scheduleId => {
    const { data } = await request2<ScheduleSimple>({
      url: `/schedules/${scheduleId}`,
      method: 'GET',
    });
    return data;
  },
);

export const addSchedule = createAsyncThunk<
  ScheduleSimple,
  ScheduleParamsResolve
>('schedule/addSchedule', async ({ params, resolve }) => {
  const { data } = await request2<ScheduleSimple>({
    url: '/schedules',
    method: 'POST',
    data: params,
  });
  typeof resolve === 'function' && resolve(data?.id);
  return data;
});

export const editSchedule = createAsyncThunk<
  Schedule[],
  EditScheduleParamsResolve
>('schedule/editSchedule', async ({ params, resolve, scheduleId }) => {
  const { data } = await request2<Schedule[]>({
    url: `/schedules/${scheduleId}`,
    method: 'PUT',
    data: params,
  });
  typeof resolve === 'function' && resolve();
  return data;
});

export const unarchiveSchedule = createAsyncThunk<
  null,
  UpdateScheduleBaseParams
>('schedule/unarchiveSchedule', async ({ schedule, resolve }) => {
  await request2<boolean>({
    url: `/schedules/unarchive/${schedule.id}`,
    method: 'PUT',
    params: schedule,
  });
  resolve();
  return null;
});

export const deleteSchedule = createAsyncThunk<null, DeleteScheduleParams>(
  'schedule/deleteSchedule',
  async ({ id, archive, resolve }) => {
    await request2<boolean>({
      url: `/schedules/${id}`,
      method: 'DELETE',
      params: { archive },
    });
    resolve();
    return null;
  },
);

export const getScheduleErrorLogs = createAsyncThunk<
  ErrorLog[],
  { scheduleId: string; count: number }
>('schedule/getScheduleErrorLogs', async ({ scheduleId, count }) => {
  const { data } = await request2<ErrorLog[]>({
    url: `/schedules/logs/${scheduleId}`,
    method: 'GET',
    params: { scheduleId, count },
  });
  return data;
});

export const updateScheduleBase = createAsyncThunk<
  ScheduleBase,
  UpdateScheduleBaseParams
>('schedule/updateScheduleBase', async ({ schedule, resolve }) => {
  await request2<ScheduleBase>({
    url: `/schedules/${schedule.id}/base`,
    method: 'PUT',
    data: schedule,
  });
  resolve();
  return schedule;
});
