#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License. See accompanying LICENSE file.
#

import argparse
import os,sys
import getpass
import logging
import time
import requests

s = requests.Session()

def log(msg,type):
	if type == 'info':
		logging.info(" %s",msg)
	if type == 'debug':
		logging.debug(" %s",msg)
	if type == 'warning':
		logging.warning(" %s",msg)
	if type == 'exception':
		logging.exception(" %s",msg)
	if type == 'error':
		logging.error(" %s",msg)

def isPairedSwitch(value):
	isSwitch=False
	switch_arr =["-groups", "-users","-admin","-url","-sslCertPath"]
	if value in switch_arr:
		isSwitch=True
	return isSwitch

def printUsage():
	log("[I] Note : This utility can be used to delete users or groups, To delete groups refer below given first command and to delete users refer second command.","info")
	log("[I] Usage(Group delete): deleteUserGroupUtil.py -groups <group file path> -admin <ranger admin user> -url <rangerhosturl> [-force] [-sslCertPath <cert path>] [-debug]","info")
	log("[I] Usage(User delete): deleteUserGroupUtil.py -users <user file path> -admin <ranger admin user> -url <rangerhosturl> [-force] [-sslCertPath <cert path>] [-debug]","info")
	log("[I] -groups: Delete groups specified in the given file","info")
	log("[I] -users: Delete users specified in the given file","info")
	log("[I] -admin: Ranger Admin user ID","info")
	log("[I] -force: Force delete users/groups, even if they are referenced in policies","info")
	log("[I] -url: Ranger Admin URL","info")
	log("[I] -sslCertPath: Filepath to ssl certificate to use when Ranger Admin uses HTTPS","info")
	log("[I] -debug: Enables debugging","info")
	sys.exit(1)

def processRequest(url, username, password, data, method, isHttps, certfile, isDebug):
	verify = isHttps
	if isHttps:
		verify = certfile
	if method.lower() == 'get':
		response = s.get(url, data=data, auth=(username, password),verify=verify)
	elif method.lower() == 'delete':
		response = s.delete(url, data=data, auth=(username, password),verify=verify)
	else:
		log("[E] Unsupported method"+method,'error')
		pass

	#log("[I] Request time: %s seconds" % str(response.elapsed.total_seconds()),"info")
	response_code = response.status_code
	if isDebug ==True or (response_code!=200 and response_code!=204):
		log('Request URL = ' + str(url),'debug')
		log('Response    = ' + str(response.headers),'debug')
	return response_code

def validateArgs(argv):
	if(len(argv)<7):
		log("[E] insufficient number of arguments. Found " + str(len(argv)) + "; expected at least 7","error")
		printUsage()
	if not "-users" in argv and not "-groups" in argv:
		log("[E] -users or -groups switch was missing!","error")
		printUsage()
	if not "-admin" in argv:
		log("[E] -admin switch was missing!","error")
		printUsage()
	if not "-url" in argv:
		log("[E] -url switch was missing!","error")
		printUsage()
	if "-url" in argv:
		try:
			host=str(argv[argv.index("-url")+1])
			host=host.strip()
			if host =="" or host is None or isPairedSwitch(host):
				log("[E] invalid Ranger Admin host URL","error")
				printUsage()
			if host.lower().startswith("https"):
				if not "-sslCertPath" in argv:
					log("[E] -sslCertPath switch was missing!","error")
					printUsage()
		except IndexError:
			log("[E] missing/invalid Ranger Admin host URL","error")
			printUsage()

def main(argv):
	start_time = time.time()
	FORMAT = '%(asctime)-15s %(message)s'
	logging.basicConfig(format=FORMAT, level=logging.DEBUG)
	inputPath=""
	certfile=""
	tail=""
	password=""
	isHttps=False
	isUser=False
	isGroup=False
	isDebug=False
	if "-usage" in argv or "-help" in argv:
		printUsage()
	validateArgs(argv)
	for i in range(1, len(argv)) :
		if str(argv[i])== "-groups" :
			restpath= "/groups/groupName/"
			try:
				inputPath=str(argv[i+1])
				inputPath=inputPath.strip()
				if inputPath =="" or not os.path.exists(inputPath) or not os.path.isfile(inputPath) or not os.access(inputPath, os.R_OK) or isPairedSwitch(inputPath):
					log("[E] File '"+inputPath+"' does not exist or could not be read","error")
					sys.exit(1)
				else:
					isGroup=True
				if os.stat(inputPath).st_size == 0:
					log("[E] File '"+inputPath+"' is empty!","error")
					sys.exit(1)
			except IndexError:
				log("[E] missing filename after '-groups' argument","error")
				sys.exit(1)
			continue
		elif str(argv[i])== "-users" :
			restpath= "/users/userName/"
			try:
				inputPath=str(argv[i+1])
				inputPath=inputPath.strip()
				if inputPath =="" or not os.path.exists(inputPath) or not os.path.isfile(inputPath) or not os.access(inputPath, os.R_OK) or isPairedSwitch(inputPath):
					log("[E] File '"+inputPath+"' does not exist or could not be read","error")
					sys.exit(1)
				else:
					isUser=True
				if os.stat(inputPath).st_size == 0:
					log("[E] File '"+inputPath+"' is empty!","error")
					sys.exit(1)
			except IndexError:
				log("[E] missing filename after '-users' argument","error")
				sys.exit(1)
			continue

		if str(argv[i])=="-admin" :
			try:
				user=str(argv[i+1])
				if user =="" or user is None or isPairedSwitch(user):
					log("[E] missing/invalid Ranger Admin login ID for argument '-admin'","error")
					sys.exit(1)
				continue
			except IndexError:
				log("[E] missing/invalid Ranger Admin login ID for argument '-admin'","error")
				sys.exit(1)
		if str(argv[i])=="-url" :
			try:
				host=str(argv[i+1])
				host=host.strip()
				if host =="" or host is None or isPairedSwitch(host):
					log("[E] invalid Ranger Admin host URL","error")
					sys.exit(1)
				if host.lower().startswith("https"):
					isHttps=True
				continue
			except IndexError:
				log("[E] missing/invalid Ranger Admin host URL","error")
				sys.exit(1)
		if str(argv[i])=="-force" :
			tail="?forceDelete=true"
			continue
		if str(argv[i])=="-debug" :
			isDebug=True
			continue
		if isHttps == True and str(argv[i])== "-sslCertPath" :
			try:
				certfile=str(argv[i+1])
				certfile=certfile.strip()
				if certfile =="" or not os.path.exists(certfile) or not os.path.isfile(certfile) or not os.access(certfile, os.R_OK) or isPairedSwitch(certfile):
					log("[E] Certificate File '"+certfile+"' does not exist or could not be read","error")
					sys.exit(1)
			except IndexError:
				log("[E] missing/invalid SSL certificate path for argument '-sslCertPath'","error")
				sys.exit(1)
			continue
	if isUser==True and isGroup==True:
		log("[E] -users and -groups both option were provided, only one is allowed.","error")
		printUsage()
	io_time = 0
	if password =="" :
		io_start_time = time.time()
		password=getpass.getpass("Enter Ranger Admin password : ")
		io_end_time = time.time()
		io_time = io_end_time - io_start_time
	url=host+'/service/xusers/secure/users/roles/userName/'+user
	response_code=0
	try:
		response_code=processRequest(url,user,password,None,'get',isHttps,certfile,False)
	except Exception as e:
		log("[E] request error: %s:" % (e), "error")
		sys.exit(1)
	if response_code == 302 or response_code==401 or response_code==403:
		log("[E] Authentication Error:Please try with valid credentials!","error")
		sys.exit(1)
	if response_code != 200:
		log("[E] Failed to contact Ranger Admin with given parameters. Please review the parameters","error")
		printUsage()
		sys.exit(1)
	f=open(inputPath,'r')
	processedRows=0
	for line in f:
		line=line.strip()
		if line == "" or line is None:
			continue
		if isUser==True and line==user:
			log("[I] Skipping deletion of user : "+line+", Self account deletion is restricted!","info")
			continue
		url=host+'/service/xusers'+restpath+line+tail
		method='delete'
		data=None
		response_code=processRequest(url,user,password,data,method,isHttps,certfile,isDebug)
		if response_code==302 or response_code==401:
			if isUser==True:
				log("[E] failed while deleting user '" + line + "'. Please verify the parameters","error")
			elif isGroup==True:
				log("[E] failed while deleting group '" + line + "'. Please verify the parameters","error")
			break
		elif response_code==204:
			if isUser==True:
				log("[I] Deleted user : "+line,"info")
			elif isGroup==True:
				log("[I] Deleted group : "+line,"info")
			processedRows=processedRows+1
		elif response_code==404:
			if isUser==True:
				log("[E] failed while deleting user '" + line + "'. Please verify the parameters","error")
			elif isGroup==True:
				log("[E] failed while deleting group '" + line + "'. Please verify the parameters","error")
		elif response_code==400:
			if isUser==True:
				log("[I] User not found : "+line,"info")
			elif isGroup==True:
				log("[I] Group not found : "+line,"info")

	f.close()
	if processedRows==0:
		if isUser==True:
			log("[I] No valid user found to delete!","info")
		elif isGroup==True:
			log("[I] No valid group found to delete!","info")
	else:
		if isUser==True:
			log("[I] Number of user deleted : "+str(processedRows),"info")
		elif isGroup==True:
			log("[I] Number of group deleted : "+str(processedRows),"info")
		end_time = time.time()
		log("[I] Total time for io : "+str(io_time),"info")
		log("[I] Total time taken for execution : "+str(end_time-start_time)+" seconds","info")
		log("[I] Averge time taken for execution : "+str(float(end_time-start_time)/float(processedRows))+" seconds","info")
		log("[I] Total time taken for deletion : "+str(end_time-start_time-io_time)+" seconds","info")
		log("[I] Averge time taken for deletion : "+str(float(end_time-start_time-io_time)/float(processedRows))+" seconds","info")
main(sys.argv)
