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

# #contains some of the code/features that were developed initially but were removed from the main codebase
#
# from tinydb import TinyDB, Query
#
# from ranger_performance_tool.ranger_perf_utils.logging_utils import LogParser
#
#
# class DatabaseStore:
#
#     def __init__(self, db_type='tiny_db', db_file='/tmp/ranger_tiny_db.json'):
#         if db_type == 'tiny_db':
#             self.db = TinyDBStore(db_file)
#         else:
#             raise Exception('Unknown database type: {}'.format(db_type))
#
#     def insert(self, obj):
#         return self.db.insert(obj)
#
#     def get_all(self):
#         return self.db.get_all()
#
#     def get_one(self):
#         return self.db.get_one()
#
#     def delete_one(self):
#         return self.db.delete_one()
#
#     def delete_all(self):
#         return self.db.delete_all()
#
#
# class TinyDBStore:
#
#     def __init__(self, db_file):
#         self.db = TinyDB(db_file)
#
#     def insert(self, obj):
#         self.db.insert(obj)
#
#     def get_all(self):
#         return self.db.all()
#
#     def get_one(self):
#         all = self.db.all()
#         if len(all) == 0:
#             raise Exception('No elements in database for get_one. Please insert an element first')
#         return all[0]
#
#     def delete_one(self):
#         all = self.db.all()
#         if len(all) == 0:
#             raise Exception('No elements in database for delete_one. Please insert an element first')
#         ele = all[0]
#         self.db.remove(doc_ids=[ele.doc_id])
#         return ele
#
#     def delete_all(self):
#         self.db.truncate()
#
# class DataframeMetrics:
#
#     def get_top_threshold(self, dataframe, column, percentile):
#         threshold = dataframe[column].quantile(percentile / 100)
#         return threshold
#
#     def get_bottom_threshold(self, dataframe, column, percentile):
#         threshold = dataframe[column].quantile(1 - percentile / 100)
#         return threshold
#
#
# def add_additional_statistic(self, dataframe, statistic_name, to_remove_column_name=None):
#     try:
#         if statistic_name == 'memory_used_percentage':
#             column_name = LogParser().get_header_mapping_system_logs('free')
#             dataframe['memory_used_percentage'] = (dataframe['total_memory'] - dataframe[column_name])/dataframe['total_memory']
#         else:
#             raise Exception(f"Unimplemented statistic: {statistic_name}")
#
#     except:
#         Exception(f"Error while adding statistic {statistic_name}")
#     return dataframe