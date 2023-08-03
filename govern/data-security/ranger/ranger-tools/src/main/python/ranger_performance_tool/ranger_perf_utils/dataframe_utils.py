#!/usr/bin/env python

#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import pandas as pd


class DataframeUtils:
    """
    Utilities for dataframe manipulation
    Methods
    ----------
    truncate_dataframe(data, start_time, end_time, timestamp_col_name='time')
        truncates dataframe to given time range
    align_dataframes(system_logs_df, access_logs_df, system_logs_timestamp_col_name = 'time',
                        access_logs_timestamp_col_name = 'time', timestamp_truncation=True, merge=False)
        aligns two dataframes based on timestamp column name
    combine_system_logs_dataframe(main_system_logs_df, secondary_system_logs_df)
        combines two system logs dataframes
    insert_column(dataframe, column_name, column_data)
        inserts column to dataframe
    rename_rows(dataframe, row_name_mapping)
        renames rows in dataframe
    rename_columns(dataframe, column_name_mapping)
        renames columns in dataframe
    """
    def __init__(self):
        pass

    def truncate_dataframe(self, data, start_time, end_time, timestamp_col_name='time'):
        """
        truncates dataframe to given time range
        :param data: Pandas dataframe to be truncated
        :param start_time: First time to be included in the dataframe
        :param end_time: Last time to be included in the dataframe
        :param timestamp_col_name: Column name of the timestamp column
        :return: Pandas dataframe with truncated data
        """
        mask = (pd.to_datetime(data[timestamp_col_name]) >= pd.to_datetime(start_time, infer_datetime_format=True)) & (
                    pd.to_datetime(data[timestamp_col_name]) <= pd.to_datetime(end_time, infer_datetime_format=True))
        return data.loc[mask]

    def align_dataframes(self, system_logs_df, access_logs_df, system_logs_timestamp_col_name = 'time',
                         access_logs_timestamp_col_name = 'time', timestamp_truncation=True, merge=False):
        """
        aligns two dataframes based on timestamp column name
        :param system_logs_df: Pandas dataframe of system logs
        :param access_logs_df: Pandas dataframe of access logs
        :param system_logs_timestamp_col_name: Name of the timestamp column in system logs dataframe
        :param access_logs_timestamp_col_name: Name of the timestamp column in access logs dataframe
        :param timestamp_truncation: bool, True if timestamp truncation is to be done
        :param merge: bool, True if dataframes are to be merged
        :return: Pandas dataframe. Based on merge and timestamp truncation parameters
        """
        if system_logs_timestamp_col_name != 'time':
            system_logs_df.rename(columns={system_logs_timestamp_col_name: 'time'}, inplace=True)
            system_logs_timestamp_col_name = 'time'

        if access_logs_timestamp_col_name != 'time':
            access_logs_df.rename(columns={access_logs_timestamp_col_name: 'time'}, inplace=True)
            access_logs_timestamp_col_name = 'time'

        if timestamp_truncation:
            start_time = system_logs_df[system_logs_timestamp_col_name].min()
            end_time = system_logs_df[system_logs_timestamp_col_name].max()
            system_logs_df = self.truncate_dataframe(system_logs_df, start_time, end_time,
                                                     timestamp_col_name=system_logs_timestamp_col_name)
            access_logs_df = self.truncate_dataframe(access_logs_df, start_time, end_time,
                                                     timestamp_col_name=access_logs_timestamp_col_name)
        if merge:
            aligned_df = pd.merge(access_logs_df, system_logs_df, on=access_logs_timestamp_col_name, how='outer', sort=True)
        else:
            aligned_df = access_logs_df
        return aligned_df

    def combine_system_logs_dataframe(self, main_system_logs_df, secondary_system_logs_df):
        """
        Concats two system logs dataframes
        :param main_system_logs_df: Pandas dataframe of system logs
        :param secondary_system_logs_df: Pandas dataframe of secondary system logs
        :return: Pandas dataframe of combined system logs
        """
        secondary_system_logs_df = pd.concat([secondary_system_logs_df]*main_system_logs_df.shape[0], ignore_index=True)
        combined_df = pd.concat([main_system_logs_df, secondary_system_logs_df], axis=1)
        return combined_df

    def insert_column(self, dataframe, column_name, column_data):
        """
        Inserts column to dataframe
        :param dataframe: Pandas dataframe to be modified
        :param column_name: Name of the column to be inserted
        :param column_data: Data to be inserted in the column
        :return: Pandas dataframe with inserted column
        """
        last_index = len(dataframe.columns)
        dataframe.insert(last_index, column_name, column_data)
        return dataframe

    def rename_rows(self, dataframe, row_name_mapping):
        """
        Renames rows in dataframe
        :param dataframe: Pandas dataframe to be modified
        :param row_name_mapping: dict, mapping of old row names to new row names
        :return: void, modifies dataframe in place
        """
        dataframe.rename(index=row_name_mapping, inplace=True)

    def rename_columns(self, dataframe, column_name_mapping):
        """
        Renames columns in dataframe
        :param dataframe: Pandas dataframe to be modified
        :param column_name_mapping: dict, mapping of old column names to new column names
        :return: void, modifies dataframe in place
        """
        dataframe.rename(columns=column_name_mapping, inplace=True)