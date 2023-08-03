from re import L
from this import s
import xml.etree.ElementTree as ET
from xml.dom import minidom
from pathlib import Path
import subprocess
import socket
import logging
import psutil
import argparse
import os
import sys
import shutil
import time

SCRIPT_DIR = Path(os.path.realpath(__file__)).parent
logging.basicConfig(level=logging.INFO,
                    format="%(asctime)s [%(levelname)s] %(message)s",
                    handlers=[
                        logging.FileHandler(SCRIPT_DIR / 'deploy.log'),
                        logging.StreamHandler(sys.stdout)
                    ])

# root tag of the xml file
# look for ROOT/ for all tags that are updated by this script
ROOT = Path('./')


class PortGenerator:

    def __init__(self, port_list=[]):
        self.ports = []
        self.name2idx = {}
        for port_name in port_list:
            self.get(port_name)

    def get(self, idx_or_name):
        if type(idx_or_name) == str:
            name = idx_or_name
            if name not in self.name2idx:
                num = len(self.name2idx)
                self.name2idx[name] = num
                self.ports.append(self.get_free_port())
            return self.ports[self.name2idx[name]]
        else:
            assert len(self.ports
                       ) > idx_or_name, f'Port={idx_or_name} not created yet.'
            return self.ports[idx_or_name]

    def get_free_port(self):
        sock = socket.socket()
        sock.bind(('', 0))
        return str(sock.getsockname()[1])


class Config:

    def __init__(self, template_path, save_path):
        self.xml = ET.parse(template_path)
        self.save_path = save_path

    def save(self):
        self.xml.write(self.save_path)
        xml = minidom.parse(str(self.save_path))
        with open(self.save_path, 'w') as fd:
            fd.write(xml.toprettyxml())
        lines = []
        with open(self.save_path, 'r') as fd:
            for line in fd:
                if len(line.strip()) > 0:
                    lines.append(line)
        with open(self.save_path, 'w') as fd:
            fd.writelines(lines)

    def add(self, parent_tag, node):
        '''
        add a subelement under the parent_tag, node could be a xml node
        or <>xxx</> string
        '''
        parent_tag = str(parent_tag)
        logging.debug(f"Add element for node {parent_tag}")
        parent = self.get_node(parent_tag)
        assert parent is not None, f'node: {parent_tag} not exists'
        if type(node) is str:
            child = ET.fromstring(node)
            parent.append(child)
        else:
            parent.append(node)

    def remove(self, tag):
        '''remove the nodes with the given tag'''
        tag = str(tag)
        logging.debug(f"Removing all {tag}")
        fields = tag.split('/')
        parent_tag = '/'.join(fields[:-1])
        parent = self.get_node(parent_tag)
        # clear all nodes under the given parent
        for node in parent.findall(fields[-1]):
            parent.remove(node)

    def clear(self, tag):
        '''clear the content of the node with givne tag; keep this node'''
        tag = str(tag)
        logging.debug(f"Clear subelements of {tag}")
        parent = self.get_node(tag)
        parent.clear()

    def update(self, tag, text_or_node):
        '''replace the content of the given tag'''
        tag = str(tag)
        node = self.get_node(tag)
        if node is None:
            logging.warning(f'Tag {tag} content is not updated')
            return
        if isinstance(text_or_node, str) or isinstance(text_or_node, Path):
            logging.debug(f"Modifying text for {tag} ==> {text_or_node}")
            node.text = str(text_or_node)
        else:
            logging.debug(f"Modifying Node {tag}")
            node[:] = text_or_node

    def get_node(self, tag):
        '''return the xml element/node of the given tag'''
        tag = str(tag)
        node = self.xml.find(tag)
        if node is None:
            logging.warning(f'Tag {tag} not exists')
        return node


class Entity:
    # binary executable file name, e.g., clickhouse-server; override in subclass
    BIN = ''

    def __init__(self, name, template_path, cluster_dir, port_list=[]):
        '''Base class for runnable entities in the cluster, e.g., server/worker'''
        self.name = name
        self.template_dir = template_path.parent
        self.workspace = cluster_dir / name
        self.conf = Config(template_path, self.workspace / f'{name}.xml')
        self.port_gen = PortGenerator(port_list)
        # for start_all() all entities
        self.entities = []

    def start(self, bin_dir, dry_run):
        '''start the program in a tmux window'''
        stay = "while true; do sleep 2; done"
        cmd = f"tmux new-window -n {self.name} "\
              f"'{bin_dir}/{self.BIN} --config {self.conf.save_path}; {stay}'"
        logging.info(cmd)
        if not dry_run:
            assert self.has_conf_file(
            ), f'config file {self.conf.save_path} not exists'
            subprocess.run(cmd, shell=True, check=True)

    def config(self):
        logging.info(f'Configuring for {self.name}')
        self.workspace.mkdir(parents=True, exist_ok=True)

        log_dir = self.workspace / 'log'
        log_dir.mkdir(parents=True, exist_ok=True)
        self.conf.update(ROOT / 'logger/log', log_dir / 'clickhouse.log')
        self.conf.update(ROOT / 'logger/errorlog',
                         log_dir / 'clickhouse.err.log')
        self.conf.update(ROOT / 'logger/testlog',
                         log_dir / 'clickhouse.test.log')

        self.conf.update(ROOT / 'path', self.workspace / "data")
        self.conf.update(ROOT / 'tmp_path', self.workspace / "tmp_data")
        self.conf.update(ROOT / 'user_files_path', self.workspace / "user_files")
        self.conf.update(ROOT / 'users_config', self.workspace / "cnch-users.xml")

        self.conf.update(ROOT / 'format_schema_path',
                         self.workspace / "format_schemas")

    def has_conf_file(self):
        '''has the config file saved onto disk'''
        return self.conf.save_path.exists()

    def kill(self, dry_run):
        '''kill the process of this entity and the tmux window'''
        for proc in psutil.process_iter():
            p = psutil.Process(proc.pid)
            if p.username() == os.getlogin():
                cmd = ' '.join(x for x in proc.cmdline())
                kill = False
                if self.BIN in cmd:
                    logging.info("Killing process...{}".format(cmd))
                    if not dry_run:
                        proc.kill()


class Server(Entity):
    BIN = 'clickhouse-server'
    #CATALOG_NAMESPACE = ROOT / "catalog/name_space"
    SD = ROOT / "service_discovery/server"

    TEMPLATE_HDFS_CONFIG = 'hdfs3.xml'
    TEMPLATE_USER_CONFIG = 'byconity-users.xml'

    def __init__(self, name, template_path, cluster_dir, hdfs_prefix, catalog_type):
        # the order must be the same as that in the service_discovery section
        self.port_list = [
            'tcp_port', 'rpc_port', 'http_port', 'tcp_secure_port',
            'https_port', 'exchange_port', 'exchange_status_port',
            'ha_tcp_port', 'interserver_http_port'
        ]
        super(Server, self).__init__(name, template_path, cluster_dir,
                                     self.port_list)
        self.hdfs_prefix = hdfs_prefix
        self.catalog_type = catalog_type

    def gen_sd_config(self):
        '''generate xml node of this server in service discovery section'''
        self.conf.update(f'{self.SD}/node/hostname', self.name)
        self.conf.clear(f'{self.SD}/node/ports')
        for i, port in enumerate(self.port_list[:7]):
            pval = self.port_gen.get(port)
            self.conf.add(
                f'{self.SD}/node/ports',
                f'<port><name>PORT{i}</name><value>{pval}</value></port>')
        return self.conf.get_node(self.SD / 'node')

    def config(self, sd, tso, dm):
        '''config the whole xml config file for this server'''
        super(Server, self).config()

        self.conf.update(TSO.SERVICE, tso)
        self.conf.update(DM.SERVICE, dm)
        self.conf.update(ROOT / "service_discovery", sd)

        for port in self.port_list:
            self.conf.update(ROOT / port, self.port_gen.get(port))

        hdfs_path = Path(self.hdfs_prefix) / os.getlogin()

        self.conf.clear(ROOT / 'storage_configuration/disks')
        # If path does not exist, mkdir for it
        self.conf.add(
            ROOT / 'storage_configuration/disks',
            f'<server_local_disk1><path>{self.workspace}/data/1/</path></server_local_disk1>')
        self.conf.add(
            ROOT / 'storage_configuration/disks',
            f'<server_hdfs_disk0><path>{hdfs_path}/server/data/0/</path><type>bytehdfs</type></server_hdfs_disk0>'
        )
        Path(f'{self.workspace}/data/1/').resolve().mkdir(parents=True, exist_ok=True)

        self.conf.update(ROOT / "hdfs3_config",
                         self.workspace / self.TEMPLATE_HDFS_CONFIG)
        self.conf.update(ROOT / "bytejournal/log_dir",
                         self.workspace / "logs_bytejournal")

        self.conf.update(ROOT / "tso_service/bytejournal/election_point_key",
                         "tso_election_point_" + os.getlogin())
        self.conf.update(ROOT / "bytejournal/cnch_prefix",
                         "cnch_prefix_" + os.getlogin())

        self.conf.update(ROOT / "server_leader_election/namespace",
                         "server_namespace_" + os.getlogin())
        self.conf.update(ROOT / "server_leader_election/point",
                         "server_point_" + os.getlogin())
        if self.catalog_type == 'fdb':
            fdb_path = f'{self.workspace}/fdb/cluster_config/'
            self.conf.update(ROOT / "catalog_service/fdb/cluster_file", fdb_path)
            self.conf.update(ROOT / "tso_service/fdb/cluster_file", fdb_path)
            Path(fdb_path).resolve().mkdir(parents=True, exist_ok=True)

        self.conf.save()
        shutil.copy(self.template_dir / self.TEMPLATE_USER_CONFIG,
                    self.workspace)
        shutil.copy(self.template_dir / self.TEMPLATE_HDFS_CONFIG,
                    self.workspace)


class Worker(Server):
    SD = ROOT / "service_discovery/vw"
    BIN = 'clickhouse-server'

    def __init__(self, name, template_path, cluster_dir, vw_name, hdfs_prefix, catalog_type):
        super(Worker, self).__init__(name, template_path, cluster_dir,
                                     hdfs_prefix, catalog_type)
        self.vw_name = vw_name

    def gen_sd_config(self):
        '''generate xml node of this worker in service discovery sec'''
        self.conf.update(f'{self.SD}/node/vw_name', self.vw_name)
        self.conf.update(f'{self.SD}/node/hostname', self.name)
        self.conf.clear(f'{self.SD}/node/ports')
        for i, port in enumerate(self.port_list[:7]):
            pval = self.port_gen.get(port)
            self.conf.add(
                f'{self.SD}/node/ports',
                f'<port><name>PORT{i}</name><value>{pval}</value></port>')
        return self.conf.get_node(f'{self.SD}/node')


class TSO(Entity):
    SD = ROOT / "service_discovery" / "tso"
    SERVICE = ROOT / "tso-server"
    BIN = 'tso-server'

    def __init__(self, name, template_path, cluster_dir):
        super(TSO, self).__init__(name, template_path, cluster_dir,
                                  ['rpc_port', 'http_port'])

    def gen_sd_config(self):
        self.conf.update(f'{self.SD}/node/ports/port/value',
                         self.port_gen.get(0))
        return self.conf.get_node(self.SD)

    def config(self):
        super(TSO, self).config()
        self.conf.update(self.SERVICE / 'port', self.port_gen.get('rpc_port'))
        self.conf.update(self.SERVICE / 'http/port',
                         self.port_gen.get('http_port'))
        return self.conf.get_node(self.SERVICE)


class DM(TSO):
    SD = ROOT / "service_discovery" / "daemon-manager"
    SERVICE = ROOT / "daemon-manager"
    BIN = "daemon-manager"

class Client(Entity):
    BIN = 'clickhouse-client'

    def __init__(self, name, port):
        self.port = port
        self.name = name

    def start(self, bin_dir, dry_run):
        stay = "while true; do sleep 2; done"
        cmd = f"tmux new-window -n {self.name} "\
              f"'{bin_dir}/{self.BIN} -h 127.0.0.1 --port {self.port} -mn; {stay}'"
        logging.info(cmd)
        if not dry_run:
            subprocess.run(cmd, shell=True, check=True)


class Cluster(Entity):
    def __init__(self, name, template_path, cluster_dir):
        super(Cluster, self).__init__(name, template_path, cluster_dir)
        self.entities = []

    def kill(self, dry_run):
        '''kill all entities of the cluster'''
        for x in self.entities:
            x.kill(dry_run)
        for x in self.entities:
            cmd = f"tmux kill-window -t {x.name}"
            logging.info(cmd)
            if not dry_run:
                subprocess.run(cmd.split(' '))
        time.sleep(5)

    def start(self, bin_dir, dry_run=False):
        for ent in self.entities:
            ent.start(bin_dir, dry_run)
            time.sleep(3)


class CNCHCluster(Cluster):
    def __init__(self, num_servers, num_read_workers, num_write_workers,
                 num_default_workers, template_paths, cluster_dir,
                 hdfs_prefix, catalog_type):
        super(CNCHCluster, self).__init__('cnchcluster', template_paths[0],
                                          cluster_dir)
        self.servers = [
            Server(f'Server-{i}', template_paths[0], cluster_dir, hdfs_prefix, catalog_type)
            for i in range(num_servers)
        ]
        self.read_workers = [
            Worker(f'ReadWorker-{i}', template_paths[1], cluster_dir,
                   'vw_read', hdfs_prefix, catalog_type) for i in range(num_read_workers)
        ]
        self.write_workers = [
            Worker(f'WriteWorker-{i}', template_paths[1], cluster_dir,
                   'vw_write', hdfs_prefix, catalog_type) for i in range(num_write_workers)
        ]
        self.default_workers = [
            Worker(f'DefaultWorker-{i}', template_paths[1], cluster_dir,
                   'vw_default', hdfs_prefix, catalog_type)
            for i in range(num_default_workers)
        ]

        self.workers = self.read_workers + self.write_workers + self.default_workers
        self.tso = TSO('tso', template_paths[0], cluster_dir)
        self.dm = DM('dm', template_paths[0], cluster_dir)

        self.client = Client(self.servers[0].name + '-client',
                             self.servers[0].port_gen.get('tcp_port'))

        self.entities = [self.tso] + self.workers + self.servers + [self.dm, self.client]

    def config(self, overwrite=False):
        if not overwrite:
            for ent in self.servers + self.workers:
                assert ent.has_conf_file(
                ), f'Overwrite=False, but {ent.template_path} not exists'
            return

        # combine the sd section of all entities to get the complete sd section
        self.conf.remove(Server.SD / 'node')
        for server in self.servers:
            self.conf.add(Server.SD, server.gen_sd_config())
        self.conf.remove(Worker.SD / 'node')
        for worker in self.workers:
            self.conf.add(Worker.SD, worker.gen_sd_config())
        self.conf.update(ROOT / "service_discovery" / 'tso-server',
                         self.tso.gen_sd_config())
        self.conf.update(ROOT / "service_discovery" / 'daemon-manager',
                         self.dm.gen_sd_config())
        csd = self.conf.get_node(ROOT / "service_discovery")

        # get xml nodes of other entities
        ctso = self.tso.config()
        cdm = self.dm.config()
        # to config and write server/worker xml files
        for ent in self.servers + self.workers:
            ent.config(csd, ctso, cdm)

        shutil.copy(self.servers[0].conf.save_path, self.tso.conf.save_path)
        shutil.copy(self.servers[0].conf.save_path, self.dm.conf.save_path)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(
        description='Config and launch CNCH/CE cluster')

    parser.add_argument('--program_dir',
                        help='The path where all the clickhouse binary exists')

    parser.add_argument('--cluster_dir',
                        default='cluster',
                        help='The directory where to put all configs and logs')

    parser.add_argument('--template_paths',
                        nargs = '+',
                        help='server and worker templates for cnch;')

    parser.add_argument('-c',
                        '--catalog_type',
                        default='bytekv',
                        choices=['bytekv', 'fdb'],
                        help="Type of catalog to use")

    parser.add_argument('-r',
                        '--num_read_worker',
                        action='store',
                        type=int,
                        default=0,
                        help="Number of read workers in this cluster")

    parser.add_argument('-w',
                        '--num_write_worker',
                        action='store',
                        type=int,
                        default=1,
                        help="Number of write workers in this cluster")

    parser.add_argument('-d',
                        '--num_default_worker',
                        action='store',
                        type=int,
                        default=1,
                        help="Number of default workers in this cluster")

    parser.add_argument('-s',
                        '--num_server',
                        action='store',
                        type=int,
                        default=1,
                        help="Number of servers or shards in this cluster")

    parser.add_argument('--hdfs_prefix',
                        default='/data/cnch/default',
                        help='prefix of the hdfs path')

    parser.add_argument('--dry_run',
                        action='store_true',
                        help="Only generate, do not run")

    parser.add_argument('--overwrite',
                        action='store_false',
                        help="Overwrite the old configs")

    opts = parser.parse_args()

    cluster_dir = Path(opts.cluster_dir).resolve()
    template_paths = [Path(x) for x in opts.template_paths]

    cluster = CNCHCluster(opts.num_server, opts.num_read_worker,
                            opts.num_write_worker, opts.num_default_worker,
                            template_paths, cluster_dir, opts.hdfs_prefix, opts.catalog_type)

    cluster.config(opts.overwrite)
    cluster.kill(opts.dry_run)
    cluster.start(Path(opts.program_dir), opts.dry_run)
