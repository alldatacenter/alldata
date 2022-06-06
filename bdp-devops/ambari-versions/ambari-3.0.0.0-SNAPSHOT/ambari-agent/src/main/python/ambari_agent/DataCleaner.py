#!/usr/bin/env python

'''
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
'''

import AmbariConfig
import threading
import os
import time
import re
import logging

logger = logging.getLogger(__name__)

class DataCleaner(threading.Thread):
  COMMAND_FILE_NAMES_PATTERN = 'errors-\d+.txt|output-\d+.txt|site-\d+.pp|structured-out-\d+.json|command-\d+.json'
  AUTO_COMMAND_FILE_NAMES_PATTERN = \
    'auto_command-\d+.json|auto_errors-\d+.txt|auto_output-\d+.txt|auto_structured-out-\d+.json'
  FILE_NAME_PATTERN = AUTO_COMMAND_FILE_NAMES_PATTERN + "|" + COMMAND_FILE_NAMES_PATTERN

  def __init__(self, config):
    threading.Thread.__init__(self)
    self.daemon = True
    logger.info('Data cleanup thread started')
    self.config = config

    self.file_max_age = config.get('agent', 'data_cleanup_max_age', 86400)
    self.file_max_age = int(self.file_max_age) if self.file_max_age else None
    if self.file_max_age is None or self.file_max_age < 86400:       # keep for at least 24h
      logger.warn('The minimum value allowed for data_cleanup_max_age is 1 '
                  'day. Setting data_cleanup_max_age to 86400.')
      self.file_max_age = 86400

    self.cleanup_interval = config.get('agent', 'data_cleanup_interval', 3600)
    self.cleanup_interval = int(self.cleanup_interval) if self.cleanup_interval else None
    if self.cleanup_interval is None or self.cleanup_interval < 3600:    # wait at least 1 hour between runs
      logger.warn('The minimum value allowed for data_cleanup_interval is 1 '
                  'hour. Setting data_cleanup_interval to 3600.')
      self.cleanup_interval = 3600

    self.cleanup_max_size_MB = config.get('agent', 'data_cleanup_max_size_MB', 10000)
    self.cleanup_max_size_MB = int(self.cleanup_max_size_MB) if self.cleanup_max_size_MB else None
    if self.cleanup_max_size_MB is None or self.cleanup_max_size_MB > 10000:  # no more than 10 GBs
      logger.warn('The maximum value allowed for cleanup_max_size_MB is 10000 MB (10 GB). '
                        'Setting cleanup_max_size_MB to 10000.')
      self.cleanup_max_size_MB = 10000

    self.data_dir = config.get('agent','prefix')
    self.compiled_pattern = re.compile(self.FILE_NAME_PATTERN)
    self.stopped = False

  def __del__(self):
    logger.info('Data cleanup thread killed.')

  def cleanup(self):
    logger.debug("Cleaning up inside directory %s", self.data_dir)
    now = time.time()
    total_size_bytes = 0
    file_path_to_timestamp = {}
    file_path_to_size = {}

    for root, dirs, files in os.walk(self.data_dir):
      for f in files:
        file_path = os.path.join(root, f)
        if self.compiled_pattern.match(f):
          try:
            file_age = now - os.path.getmtime(file_path)
            if file_age > self.file_max_age:
              os.remove(os.path.join(file_path))
              logger.debug('Removed file: %s', file_path)
            else:
              # Since file wasn't deleted in first pass, consider it for the second one with oldest files first
              file_size = os.path.getsize(file_path)
              total_size_bytes += file_size
              file_path_to_timestamp[file_path] = file_age
              file_path_to_size[file_path] = file_size
          except Exception:
            logger.error('Error when removing file: ' + file_path)

    target_size_bytes = self.cleanup_max_size_MB * 1000000
    if len(file_path_to_timestamp) and total_size_bytes > target_size_bytes:
      logger.info("DataCleaner values need to be more aggressive. Current size in bytes for all log files is %d, "
                  "and will try to clean to reach %d bytes." % (total_size_bytes, target_size_bytes))
      # Prune oldest files first
      count = 0
      file_path_oldest_first_list = sorted(file_path_to_timestamp, key=file_path_to_timestamp.get, reverse=True)
      for file_path in file_path_oldest_first_list:
        try:
          os.remove(os.path.join(file_path))
          total_size_bytes -= file_path_to_size[file_path]
          count += 1
          if total_size_bytes <= target_size_bytes:
            # Finally reached below the cap
            break
        except Exception:
          pass
      else:
        # Did not reach below cap.
        logger.warn("DataCleaner deleted an additional %d files, currently log files occupy %d bytes." %
                    (count, total_size_bytes))
        pass

  def run(self):
    while not self.stopped:
      logger.info('Data cleanup started')
      self.cleanup()
      logger.info('Data cleanup finished')
      time.sleep(self.cleanup_interval)


def main():
  data_cleaner = DataCleaner(AmbariConfig.config)
  data_cleaner.start()
  data_cleaner.join()


if __name__ == "__main__":
  main()
