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

import os
from datetime import datetime
from collections import defaultdict

import pandas as pd


class LogFetcher:
    """
    Copies system logs and access logs from server to local machine

    Methods:
    ----------
    fetch_system_logs_from_server(host, username, password, remote_file,
                                    local_path="/tmp/") : fetches system logs from server and copies to local machine
    fetch_secondary_system_logs_from_server(host, username, password, remote_file,
                                        local_path="/tmp/") : fetches secondary system logs from server and copies to local machine
    fetch_access_logs_from_server(host, username, password, remote_path,
                                        local_path="/tmp/") : fetches access logs from server and copies to local machine
    """
    def fetch_system_logs_from_server(self, host, username, password, remote_file,
                                      local_path="/tmp/"):
        """
        Fetches system logs from server and copies to local machine
        :param host: str, ssh host
        :param username: str, ssh username
        :param password: str, ssh password
        :param remote_file: str, location of system logs on server
        :param local_path: str, local path to copy system logs to
        :return: local_file: str, local file path to system logs
        """
        local_file = f"{local_path}system_logs.txt"
        command = f"sshpass -p {password} scp -o StrictHostKeyChecking=no {username}@{host}:{remote_file} {local_file}"
        print(f"fetch command = {command}")
        os.system(command)
        return local_file

    def fetch_secondary_system_logs_from_server(
            self, host, username, password, remote_file, local_path="/tmp/"):
        """
        Fetches secondary system logs from server and copies to local machine
        :param host: str, ssh host
        :param username: str, ssh username
        :param password: str, ssh password
        :param remote_file: str, location of system logs on server
        :param local_path: str, local path to copy system logs to
        :return: local_file: str, local file path to system logs
        """
        local_file = f"{local_path}secondary_system_logs.txt"
        command = f"sshpass -p {password} scp -o StrictHostKeyChecking=no {username}@{host}:{remote_file} {local_file}"
        print(f"fetch command = {command}")
        os.system(command)
        return local_file

    def fetch_access_logs_from_server(self, host, username, password, remote_path, local_path="/tmp/"):
        """
        Fetches access logs from server and copies to local machine
        :param host: str, ssh host
        :param username: str, ssh username
        :param password: str, ssh password
        :param local_path: str, local path to copy access logs to
        :param remote_path: str, remote path to copy access logs from
        :return: local_file: str, local file path to access logs
        """
        local_file = f"{local_path}access_logs.txt"
        remote_file = f"{remote_path}access_log-{host}-{datetime.utcnow().strftime('%Y-%m-%d')}.log"
        command = f"sshpass -p {password} scp -o StrictHostKeyChecking=no {username}@{host}:{remote_file} {local_file}"
        print(f"fetch command = {command}")
        os.system(command)
        return local_file


class LogParser:
    """
    Parse access logs and system logs
    Attributes:
        header_mapping_system_logs: dict, mapping of system logs header to column name in dataframe

    Methods:
        get_header_mapping_system_logs: returns dict of system logs header to column name in dataframe
        parse_secondary_system_logs: parse secondary system logs and return a dataframe
        parse_system_logs: parse system logs and return a dataframe

    """
    header_mapping_system_logs = {
        'r': 'Active process count',
        'b': 'Sleeping process count',
        'swpd': 'Total virtual memory swap (MegaBytes)',
        'free': 'Total free memory (MegaBytes)',
        'buff': 'Total memory temporarily used as a data buffer (MegaBytes)',
        'cache': 'Total cache memory (MegaBytes)',
        'si': 'The rate of swapping-in memory from disk',
        'so': 'The rate of swapping-out memory to disk',
        'bi': 'Blocks received from a block device per second',
        'bo': 'Blocks sent to a block device per second',
        'in': 'The number of system interrupts',
        'cs': 'The number of context switches per second',
        'us': 'The percentage of CPU time spent on non-kernel processes',
        'sy': 'The percentage of CPU time spent on kernel processes',
        'id': 'The percentage of idle CPU',
        'wa': 'The percentage of CPU time spent waiting for Input/Output',
        'st': 'The percentage of CPU time stolen by a virtual machine',
        'UTC': 'time'
    }

    @staticmethod
    def get_header_mapping_system_logs(key):
        """
        Get the header mapping for system logs
        :param key: column name from vmstat command
        :return: header mapping for system logs for given column name
        """
        return LogParser.header_mapping_system_logs[key]

    @staticmethod
    def _parse_string(x):
        return x[1:-1]

    @staticmethod
    def _parse_datetime(x):
        dt = datetime.strptime(x[1:-7], '%d/%b/%Y:%H:%M:%S')
        return dt

    def _combine_date_time(self, line_split):
        line_split[-2] = line_split[-2] + " " + line_split[-1]
        del line_split[-1]

    def parse_secondary_system_logs(self, local_file):
        """
        Parse system logs and return a dataframe with the following columns: total_memory.
        Can be extended to other columns if needed.
        :param local_file: path to the secondary system logs file on the local machine
        :return: dataframe of system logs
        """
        print("Ensure command executed for creating secondary system logs is similar in schema produced to :: "
              "vmstat -s -S M")
        secondary_sys_logs = {}
        with open(local_file, 'r') as f:
            lines = f.readlines()
            total_memory = lines[0].split()[0]
            secondary_sys_logs['total_memory (MegaBytes)'] = [total_memory]

        secondary_df = pd.DataFrame(secondary_sys_logs)
        return secondary_df

    # skipping first line which is the header:: procs --memory---swap---io- -system--cpu----timestamp
    # Second line is header for the rest of the lines:: r/b/swpd/free/buff/cache/si/so/bi/bo/in/cs/us/sy/id/wa/st/UTC
    def parse_system_logs(self, local_file, metrics=None):
        """
        Parse system logs and return a dataframe with the following columns:
        :param local_file: path to the system logs file on the local machine
        :param metrics: list of metrics to be parsed from the system logs
        :return: dataframe with columns as specified in metrics
        """
        print("Ensure command executed for creating system logs is similar in schema produced to :: vmstat 5 -S M -n -t")
        with open(local_file, 'r') as f:
            lines = f.readlines()[1:]
            headers = lines[0].split()
            print("len headers= ", len(headers))
            dict_lists = defaultdict(list)
            print(f"header = {headers}")
            for line in lines[1:]:
                line_split = line.split()
                # assume last 2 columns (after splitting) are date and time
                self._combine_date_time(line_split)
                for i,val in enumerate(line_split):
                    dict_lists[headers[i]].append(val)
            dict_lists['UTC'] = pd.to_datetime(dict_lists['UTC'], infer_datetime_format=True)
            if metrics is not None:
                dict_lists = {key: dict_lists[key] if key in headers else Exception(f"Requested metric '{key}' not found in system logs")
                                    for key in metrics}
            df = pd.DataFrame(dict_lists)
            cols = df.columns
            for c in cols:
                if c != 'UTC':
                    try:
                        df[c] = pd.to_numeric(df[c])
                    except:
                        pass
            return df

    def parse_access_logs(self, local_file, ip_address_client):
        """
        Parse access logs and return a dataframe. Currently only supported for regular expressions related to policy API's
        :param local_file: path to access logs file
        :param ip_address_client: ip address of client which is used to filter access logs
        :return: dataframe of access logs
        """
        print("Ensure ranger.accesslog.pattern to include the %D in the access pattern so that the tomcat server "
              "also logs the api execution time")
        data = pd.read_csv(
            local_file,
            sep=r'\s(?=(?:[^"]*"[^"]*")*[^"]*$)(?![^\[]*\])',
            engine='python',
            na_values='-',
            header=None,
            usecols=[0, 3, 4, 9],
            names=['ip', 'time', 'request', 'latency'],
            converters={'time': LogParser._parse_datetime,
                        'request': LogParser._parse_string})

        data = data[data['ip'] == ip_address_client]
        data = data[data['request'].str.match('.*service/public/v2/api/policy/[0-9]*') | data['request'].str.match(
            '^POST.*service/public/v2/api/policy')]
        data['time'] = pd.to_datetime(data['time'], format="%d/%b/%Y:%H:%M:%S")
        data[['type', 'url', 'http']] = data.request.str.split(' ', expand=True)
        data = data.drop(columns=['request', 'http'])
        return data


class SystemLogger:
    """
    Class to log system metrics

    Methods:
        delete_old_logs() - deletes old system logs
        start_system_log_service() - starts system log service on the server using vmstat command
        execute_secondary_system_log_command() - executes secondary system log command once on the server using vmstat command with -s which gives detailed system metrics
        stop_system_log_service() - stops system log service started by start_system_log_service() on the server
    """
    # TODO: change os.system to subprocess.call
    def __init__(self, host, username, password, log_file, secondary_log_file):
        """
        Initializes the SystemLogger class
        :param host: ssh host
        :param username: ssh username of server
        :param password: ssh password
        :param log_file: file path where to store system logs
        :param secondary_log_file: file path where to store secondary system logs
        """
        self.log_file = log_file
        self.secondary_log_file = secondary_log_file
        self.command_prefix = f"sshpass -p {password} ssh -o StrictHostKeyChecking=no {username}@{host}"

    def delete_old_logs(self):
        """
        Deletes old system logs
        :return: None
        """
        command = f"{self.command_prefix} yes | rm {self.log_file}"
        os.system(command)

    def start_system_log_service(self, sleep_seconds, num_entries):
        """
        Starts system log service on the server using vmstat command
        :param sleep_seconds: time gap between system log entries
        :param num_entries: total number of system log entries to be stored
        :return: None
        """
        try:
            num_entries = int(num_entries)
            command = f"{self.command_prefix} 'vmstat {sleep_seconds} {num_entries} -S M -n -t > {self.log_file}' &"
        except Exception as e:
                print("num_entries must be an integer --- executing vmstat for infinite time")
                command = f"{self.command_prefix} 'vmstat {sleep_seconds} -S M -n -t > {self.log_file}' &"
        print(f"command = {command}")
        os.system(command)

    def execute_secondary_system_log_command(self):
        """
        Executes secondary system log command once on the server using vmstat command with -s which gives detailed system metrics
        :return: None
        """
        command = f"{self.command_prefix} 'vmstat -s -S M > {self.secondary_log_file}' &"
        print(f"command = {command}")
        os.system(command)

    def stop_system_log_service(self):
        """
        Stops system log service started by start_system_log_service() on the server
        :return: None
        """
        command = f"{self.command_prefix} pkill -f 'vmstat'"
        print(f"command = {command}")
        os.system(command)
