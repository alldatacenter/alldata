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

import logging
from multiprocessing import Process, Queue

logger = logging.getLogger()

SUCCESS = "SUCCESS"
FAILED = "FAILED"

class PrallelProcessResult(object):
    def __init__(self, element, status, result):
        self.result = result
        self.status = status
        self.element = element

class ParallelProcess(Process):


    def __init__(self, function, element, params, queue):
        self.function = function
        self.element = element
        self.params = params
        self.queue = queue
        super(ParallelProcess, self).__init__()

    def return_name(self):
        ## NOTE: self.name is an attribute of multiprocessing.Process
        return "Process running function '%s' for element '%s'" % (self.function, self.element)

    def run(self):
        try:
            result = self.function(self.element, self.params)
            self.queue.put(PrallelProcessResult(self.element, SUCCESS, result))
        except Exception as e:
            self.queue.put(PrallelProcessResult(self.element, FAILED,
                            "Exception while running function '%s' for '%s'. Reason : %s" % (self.function, self.element, str(e))))
        return

def execute_in_parallel(function, array, params, wait_for_all = False):
    logger.info("Started running %s for %s" % (function, array))
    processs = []
    q = Queue()
    counter = len(array)
    results = {}

    for element in array:
        process = ParallelProcess(function, element, params, q)
        process.start()
        processs.append(process)

    while counter > 0:
        tmp = q.get()
        counter-=1
        results[tmp.element] = tmp
        if tmp.status == SUCCESS and not wait_for_all:
            counter = 0

    for process in processs:
        process.terminate()

    logger.info("Finished running %s for %s" % (function, array))

    return results

def func (elem, params):
    if elem == 'S':
        return "lalala"
    else :
        raise Exception('Exception')

if __name__ == "__main__":
    results = execute_in_parallel(func, ['F', 'BF', 'S'], None)
    for result in results:
        print results[result].element
        print results[result].status
        print results[result].result