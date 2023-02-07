import json
import os
import time
import unittest

from crawlab.grpc.entity.stream_message_code_pb2 import INSERT_DATA
from crawlab.grpc.entity.stream_message_pb2 import StreamMessage
from crawlab.result import get_result_service
from crawlab.entity.result import Result

os.environ['CRAWLAB_TASK_ID'] = ''.join(['0'] * 24)
os.environ['CRAWLAB_GRPC_ADDRESS'] = 'localhost:9666'
os.environ['CRAWLAB_GRPC_AUTH_KEY'] = 'Crawlab2021!'


class ResultServiceTest(unittest.TestCase):
    basic_item = Result({'hello': 'world'})
    basic_msg = StreamMessage(
        code=INSERT_DATA,
        data=json.dumps([basic_item]).encode('utf-8'),
    )

    def test_save_item(self):
        rs = get_result_service()
        rs.save_item(self.basic_item)

    def test_save_items(self):
        rs = get_result_service()
        for i in range(10000):
            rs.save_items([self.basic_item])


if __name__ == '__main__':
    unittest.main(verbosity=1)
