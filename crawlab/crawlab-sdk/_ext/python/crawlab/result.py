import json
from typing import List, Optional, Dict

from crawlab.client import get_client, Client
from crawlab.config import get_task_id

from crawlab.entity.result import Result
from crawlab.grpc.entity.stream_message_code_pb2 import INSERT_DATA
from crawlab.grpc.entity.stream_message_pb2 import StreamMessage
from crawlab.grpc.services.task_service_pb2_grpc import TaskServiceStub


class ResultService:
    # internal
    c: Client = None
    task_stub: TaskServiceStub = None

    def __init__(self):
        self.c = get_client()
        self.task_stub = self.c.task_service_stub

    def save_item(self, *items: Dict):
        self.save(list(items))

    def save_items(self, items: List[Dict]):
        self.save(items)

    def save(self, items: List[Dict]):
        _items: List[Dict] = []
        for i, item in enumerate(items):
            _items.append(item)
            if i > 0 and i % 50 == 0:
                self._save(_items)
                _items = []
        if len(_items) > 0:
            self._save(_items)

    def _save(self, items: List[Dict]):
        # task id
        tid = get_task_id()
        if tid is None:
            return

        records = []
        for item in items:
            result = Result(item)
            result.set_task_id(tid)
            records.append(result)

        data = json.dumps({
            "task_id": tid,
            "data": records,
        }).encode('utf-8')

        msg = StreamMessage(
            code=INSERT_DATA,
            data=data,
        )
        self.task_stub.Subscribe(iter([msg]))


RS: Optional[ResultService] = None


def get_result_service() -> ResultService:
    global RS
    if RS is not None:
        return RS
    RS = ResultService()
    return RS


def save_item(*items: Dict):
    get_result_service().save_item(*items)


def save_items(items: List[Dict]):
    get_result_service().save_items(items)
