# coding: utf-8

from common import checker

from warehouse import entry as warehouse_entry
from pmdb import entry as pmdb_entry
from dataset import entry as dataset_entry
from health import entry as health_entry
from job import entry as job_entry
from es import entry as es_entry

checker.check_sreworks_data_service_ready()

print("======start init warehouse======")
warehouse_entry.init()
print("======end init warehouse======")

print("======start init pmdb======")
pmdb_entry.init()
print("======end init pmdb======")

print("======start init dataset======")
dataset_entry.init()
print("======end init dataset======")

print("======start init health======")
health_entry.init()
print("======end init health======")

print("======start init es======")
es_entry.init()
print("======end init es======")

print("======start init job======")
job_entry.init()
print("======end init job======")
