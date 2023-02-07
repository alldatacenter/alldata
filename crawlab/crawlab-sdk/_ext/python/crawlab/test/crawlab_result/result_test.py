import json
import unittest

from crawlab import save_item, save_items
from crawlab.entity.result import Result
from crawlab.grpc.entity.stream_message_code_pb2 import INSERT_DATA
from crawlab.grpc.entity.stream_message_pb2 import StreamMessage


class ResultTest(unittest.TestCase):
    basic_item = Result({'hello': 'world'})
    basic_msg = StreamMessage(
        code=INSERT_DATA,
        data=json.dumps([basic_item]).encode('utf-8'),
    )

    def test_save_item(self):
        save_item(self.basic_item)

    def test_save_items(self):
        save_items([self.basic_item])
