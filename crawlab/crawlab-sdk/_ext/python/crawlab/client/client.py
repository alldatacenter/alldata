import os
from typing import Optional

import grpc

from crawlab.auth_token import get_auth_token_interceptor
from crawlab.entity.address import new_address_from_string, Address
from crawlab.grpc.services.model_base_service_pb2_grpc import ModelBaseServiceStub
from crawlab.grpc.services.model_delegate_pb2_grpc import ModelDelegateStub
from crawlab.grpc.services.node_service_pb2_grpc import NodeServiceStub
from crawlab.grpc.services.task_service_pb2_grpc import TaskServiceStub


class Client:
    # settings
    address: Address = None
    timeout: int = 10

    # internals
    channel: grpc.Channel = None

    # dependencies
    model_delegate_stub: ModelDelegateStub = None
    model_base_service_stub: ModelBaseServiceStub = None
    node_service_stub: NodeServiceStub = None
    task_service_stub: TaskServiceStub = None

    # plugin_client: Plugin = None

    def __init__(self):
        try:
            self.address = new_address_from_string(os.getenv('CRAWLAB_GRPC_ADDRESS'))
        except Exception:
            self.address = Address(
                host=os.getenv('CRAWLAB_GRPC_ADDRESS_HOST'),
                port=os.getenv('CRAWLAB_GRPC_ADDRESS_PORT'),
            )
        _channel = grpc.insecure_channel(self.address.string())
        self.channel = grpc.intercept_channel(_channel, get_auth_token_interceptor())
        self._register()

    def _register(self):
        self.model_delegate_stub = ModelDelegateStub(self.channel)
        self.model_base_service_stub = ModelBaseServiceStub(self.channel)
        self.node_service_stub = NodeServiceStub(self.channel)
        self.task_service_stub = TaskServiceStub(self.channel)


C: Optional[Client] = None


def get_client() -> Client:
    global C
    if C is not None:
        return C
    C = Client()
    return C
