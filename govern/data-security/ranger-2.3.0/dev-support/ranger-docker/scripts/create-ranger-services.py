from apache_ranger.model.ranger_service import RangerService
from apache_ranger.client.ranger_client import RangerClient
from json import JSONDecodeError

ranger_client = RangerClient('http://ranger:6080', ('admin', 'rangerR0cks!'))


def service_not_exists(service):
    try:
        ranger_client.get_service(service.name)
    except JSONDecodeError:
        return 1
    return 0


hdfs = RangerService({'name': 'dev_hdfs', 'type': 'hdfs',
                      'configs': {'username': 'hdfs', 'password': 'hdfs',
                                  'fs.default.name': 'hdfs://ranger-hadoop:9000',
                                  'hadoop.security.authentication': 'simple',
                                  'hadoop.security.authorization': 'true'}})

hive = RangerService({'name': 'dev_hive', 'type': 'hive',
                      'configs': {'username': 'hive', 'password': 'hive',
                                  'jdbc.driverClassName': 'org.apache.hive.jdbc.HiveDriver',
                                  'jdbc.url': 'jdfb:hive2://ranger-hadoop:10000',
                                  'hadoop.security.authorization': 'true'}})

kafka = RangerService({'name': 'dev_kafka', 'type': 'kafka',
                       'configs': {'username': 'kafka', 'password': 'kafka',
                                   'zookeeper.connect': 'ranger-zk.example.com:2181'}})

knox = RangerService({'name': 'dev_knox', 'type': 'knox',
                      'configs': {'username': 'knox', 'password': 'knox', 'knox.url': 'http://ranger-hadoop:8088'}})

yarn = RangerService({'name': 'dev_yarn', 'type': 'yarn',
                      'configs': {'username': 'yarn', 'password': 'yarn',
                                  'yarn.url': 'http://ranger-hadoop:8088'}})

hbase = RangerService({'name': 'dev_hbase', 'type': 'hbase',
                       'configs': {'username': 'hbase', 'password': 'hbase',
                                   'hadoop.security.authentication': 'simple',
                                   'hbase.security.authentication': 'simple',
                                   'hadoop.security.authorization': 'true',
                                   'hbase.zookeeper.property.clientPort': '16181',
                                   'hbase.zookeeper.quorum': 'ranger-hbase',
                                   'zookeeper.znode.parent': '/hbase'}})

if service_not_exists(hdfs):
    ranger_client.create_service(hdfs)
    print('HDFS service created!')
if service_not_exists(yarn):
    ranger_client.create_service(yarn)
    print('Yarn service created!')
if service_not_exists(hive):
    ranger_client.create_service(hive)
    print('Hive service created!')
if service_not_exists(hbase):
    ranger_client.create_service(hbase)
    print('HBase service created!')
if service_not_exists(kafka):
    ranger_client.create_service(kafka)
    print('Kafka service created!')
if service_not_exists(knox):
    ranger_client.create_service(knox)
    print('Knox service created!')
