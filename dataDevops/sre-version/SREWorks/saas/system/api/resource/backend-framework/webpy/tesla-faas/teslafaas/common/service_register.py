from nacos.client import NacosClient
import socket
import logging
import time
import threading


class ServiceRegister(object):

    def __init__(self, context):
        self.context = context
        self.enabled = False
        if context.get_value("nacos.enabled") is not None and context.get_value("nacos.enabled") == 'true':
            self.enabled = True
        self._nacos_client = None
        if self.enabled:
            self._nacos_client = NacosClient(server_addresses=context.get_value_with_required('nacos.server_addr'), namespace=context.get_value_with_required('nacos.namespace'))
        logging.info("service register enable status is %s", self.enabled)

    def registerd(self):
        if not self.enabled:
            return
        self.register_service()
        thread = threading.Thread(target=self.send_heartbeat, name='nacos_heart_beat')
        thread.start()

    @classmethod
    def get_host_ip(cls):
        host_name = socket.gethostname()
        return socket.gethostbyname(host_name)

    def register_service(self):
        if not self.enabled:
            return
        ip = self.get_host_ip()
        server_name = self.context.get_value_with_required('application.server_name')
        server_port = self.context.get_value_with_required('application.server_port')
        logging.info("register server, service_name=%s, ip=%s, port=%s", server_name, ip, server_port)
        self._nacos_client.add_naming_instance(server_name,
                                               cluster_name='DEFAULT', ip=ip,
                                               port=server_port,
                                               ephemeral=True, group_name='DEFAULT_GROUP')

    def send_heartbeat(self):
        """
        sen heart beat
        """
        if not self.enabled:
            return
        ip = self.get_host_ip()
        server_name = self.context.get_value_with_required('application.server_name')
        server_port = self.context.get_value_with_required('application.server_port')
        time.sleep(10)
        count = 0
        while True:
            try:
                if count % 120 == 0:
                    logging.debug("send heartbeat to nacos, service_name=%s, ip=%s, port=%s", server_name, ip, server_port)
                self._nacos_client.send_heartbeat(server_name,
                                                  cluster_name='DEFAULT', ip=ip,
                                                  port=server_port,
                                                  ephemeral=True, group_name='DEFAULT_GROUP')
            except Exception as e:
                logging.error('send heartbeat failed, error=%s' % str(e))
            time.sleep(5)
            count += 1


