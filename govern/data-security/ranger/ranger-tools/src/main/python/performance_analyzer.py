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
"""
For single api testing. Command line arguments override config file values.
usage:
    python performance_analyzer.py --ranger_url <ranger_url> --calls <number of times to call api> --api
    <name of function of apache_ranger python client corresponding to api> --username <Auth username>
    --password <Auth password> --client_ip <client ip address>  --ssh_host <ranger host to connect for ssh>
    --ssh_user <Server user e.g. root> --ssh_password <Server password>
Example command:
    python3 performance_analyzer.py --ranger_url http://ranger_host:6080
    --calls 10 --api create_policy --username ranger_admin --password ranger_password
    --client_ip vpn_ip --ssh_host ranger_host
    --ssh_user ssh_username --ssh_password ssh_password

For multiple api testing. Uses values from config file.
usage:
    python3 performance_analyzer.py


"""
import time
import logging
import sys
import argparse

import pandas as pd
import seaborn as sns

import ranger_performance_tool.perf_globals as perf_globals
from ranger_performance_tool.ranger_perf_utils.logging_utils import SystemLogger, LogFetcher, LogParser
from ranger_performance_tool.ranger_perf_utils.dataframe_utils import DataframeUtils


def performance_analyzer_main(argv_dict):

    configparser = perf_globals.CONFIG_READER

    object_store = perf_globals.OBJECT_STORE

    if len(argv_dict.keys()) != 0:
        configparser.override_with_command_line_args(argv_dict)

    clear = configparser.get_config_value("primary", "clear")
    host = configparser.get_config_value("primary", "host_name")
    user = configparser.get_config_value("primary", "user")
    password = configparser.get_config_value("primary", "password")

    sys_logger = None
    if configparser.get_config_value("primary", "system_logger", "enabled"):
        log_file = configparser.get_config_value("primary", "system_logger", "remote_log_file_location")
        secondary_log_file = configparser.get_config_value("primary", "system_logger", "secondary_log_file_location")
        sys_logger = SystemLogger(host, user, password, log_file, secondary_log_file)

    if clear:
        if sys_logger:
            sys_logger.delete_old_logs()

    if sys_logger:
        sys_logger.start_system_log_service(configparser.get_config_value("primary", "system_logger", "sleep_seconds"),
                                            configparser.get_config_value("primary", "system_logger", "num_calls"))
        sys_logger.execute_secondary_system_log_command()

    ranger = perf_globals.RANGER_CLIENT

    api_list = configparser.get_config_value("primary", "api_list")
    for api in api_list:
        num_calls = configparser.get_config_value("primary", "api", api, "num_calls")
        sleep_seconds = configparser.get_config_value("primary", "api", api, "sleep_seconds")
        for i in range(num_calls):
            try:
                params = object_store.get_api_param_dict(api)
                print(api, i, params)
                resp = object_store.get_api(ranger, api)(**params)
                time.sleep(sleep_seconds)
                print(resp)
            except Exception as e:
                print(e)

    df_utils = DataframeUtils()

    log_fetcher = LogFetcher()

    log_parser = LogParser()

    access_log_file = log_fetcher.fetch_access_logs_from_server(host, user, password,
                                                                remote_path=configparser.get_config_value(
                                                                    "primary",
                                                                    "remote_access_log_location"
                                                                ))
    access_df = log_parser.parse_access_logs(access_log_file, configparser.get_config_value("primary", "client_ip"))
    print(access_df.to_string())

    if sys_logger:
        sys_logger.stop_system_log_service()
        sys_log_file = log_fetcher.fetch_system_logs_from_server(
            host, user, password, configparser.get_config_value("primary", "system_logger", "remote_log_file_location"))
        secondary_sys_log_file = log_fetcher.fetch_secondary_system_logs_from_server(
            host, user, password, configparser.get_config_value("primary", "system_logger",
                                                                "secondary_log_file_location"))
        metrics = configparser.get_config_value("primary", "system_logger", "metrics")
        if len(metrics) > 0:
            main_system_df = log_parser.parse_system_logs(sys_log_file, metrics)
        else:
            main_system_df = log_parser.parse_system_logs(sys_log_file)

        print("system stats = \n",main_system_df.to_string())
        secondary_sys_log_df = log_parser.parse_secondary_system_logs(secondary_sys_log_file)
        system_df = df_utils.combine_system_logs_dataframe(main_system_df, secondary_sys_log_df)

        align_with_access_logs = configparser.get_config_value("primary", "system_logger", "align_with_access_logs")

        if align_with_access_logs:
            # join and align based on time
            aligned_df = df_utils.align_dataframes(system_df, access_df, system_logs_timestamp_col_name='UTC',
                                                   access_logs_timestamp_col_name='time', merge=True)
            df_utils.rename_columns(aligned_df, LogParser.header_mapping_system_logs)
            print(aligned_df.to_string())

            statistics_df = aligned_df.describe()
            df_utils.rename_rows(statistics_df, {"25%": "25th_percentile", "50%": "median", "75%": "75th_percentile"})

            num_api_calls_received_at_server = aligned_df[~aligned_df['ip'].isnull()].shape[0]
            num_api_calls_sent_to_server = sum([configparser.get_config_value("primary", "api", api, "num_calls") for api in api_list])
            df_utils.insert_column(statistics_df, "num_api_calls_sent_to_server", num_api_calls_sent_to_server)
            df_utils.insert_column(statistics_df, "num_api_calls_received_at_server", num_api_calls_received_at_server)
            with open("statistics_report.csv", "w") as f:
                statistics_df.to_csv(f)

            with open("performance_report.csv", "w") as f:
                aligned_df.to_csv(f, index=False)

            with open("performance_report.html", "w") as f:
                cm = sns.light_palette("red", as_cmap=True)
                html = aligned_df.style.background_gradient(cmap=cm).to_html()
                f.write(html)
        else:
            aligned_df = df_utils.align_dataframes(system_df, access_df, system_logs_timestamp_col_name='UTC',
                                                   access_logs_timestamp_col_name='time', merge=False)
            print(aligned_df.to_string())

            statistics_df_access = aligned_df.describe()
            statistics_df_system = system_df.describe()

            statistics_df = pd.concat([statistics_df_access, statistics_df_system], axis=1)
            df_utils.rename_rows(statistics_df, {"25%": "25th_percentile", "50%": "median", "75%": "75th_percentile"})

            num_api_calls_received_at_server = aligned_df[~aligned_df['ip'].isnull()].shape[0]
            num_api_calls_sent_to_server = sum([configparser.get_config_value("primary", "api", api, "num_calls") for api in api_list])

            df_utils.insert_column(statistics_df, "num_api_calls_sent_to_server", num_api_calls_sent_to_server)
            df_utils.insert_column(statistics_df, "num_api_calls_received_at_server", num_api_calls_received_at_server)

            df_utils.rename_columns(statistics_df, log_parser.header_mapping_system_logs)

            with open(perf_globals.OUTPUT_DIR+"statistics_report.json", "w") as f:
                statistics_df.to_json(f)

            with open(perf_globals.OUTPUT_DIR+"statistics_report.csv", "w") as f:
                statistics_df.to_csv(f)

            with open(perf_globals.OUTPUT_DIR+"performance_report.json", "w") as f:
                aligned_df.to_json(f)

            with open(perf_globals.OUTPUT_DIR+"performance_report.csv", "w") as f:
                aligned_df.to_csv(f, index=False)

            with open(perf_globals.OUTPUT_DIR+"performance_report.html", "w") as f:
                cm = sns.light_palette("red", as_cmap=True)
                html = aligned_df.style.background_gradient(cmap=cm).to_html()
                f.write(html)


def log(msg, type):
    if type == 'info':
        logging.info(" %s", msg)
    if type == 'debug':
        logging.debug(" %s", msg)
    if type == 'warning':
        logging.warning(" %s", msg)
    if type == 'exception':
        logging.exception(" %s", msg)
    if type == 'error':
        logging.error(" %s", msg)


def print_usage():
    print("usage:: python performance_analyzer.py --ranger_url <ranger_url> --calls <number of times to call api> --api"
          " <name of function of python client corresponding to api> --username <Auth username> "
          "--password <Auth password> --client_ip <client ip address>  --ssh_host <ranger host to connect for ssh> "
          "--ssh_user <Server user e.g. root> --ssh_password <Server password>")


def main(argv):
    parser = argparse.ArgumentParser()
    parser.add_argument("--ranger_url", help="ranger url")
    parser.add_argument("--calls", help="number of times to call api")
    parser.add_argument("--api", help="name of function of python client corresponding to api")
    parser.add_argument("--username", help="Auth username")
    parser.add_argument("--password", help="Auth password")
    parser.add_argument("--client_ip", help="client ip address")
    parser.add_argument("--ssh_host", help="ranger host to connect for ssh")
    parser.add_argument("--ssh_user", help="Server user e.g. root")
    parser.add_argument("--ssh_password", help="Server password")
    ns = parser.parse_args(argv)
    commandline_argument_dict = vars(ns)
    try:
        if None in commandline_argument_dict.values() and all(i is None for i in commandline_argument_dict.values()):
            commandline_argument_dict = {}
        if None in commandline_argument_dict.values() and not all(i is None for i in commandline_argument_dict.values()):
            commandline_argument_dict = {}
            print_usage()
            raise ValueError("Either all the commandline arguments are provided or none are provided to run the script."
                             "Ignoring the provided arguments and reading from the config files.Continuing with execution.\n")
    except ValueError as e:
        print(e)

    performance_analyzer_main(commandline_argument_dict)


if __name__ == '__main__':
    main(sys.argv[1:])
