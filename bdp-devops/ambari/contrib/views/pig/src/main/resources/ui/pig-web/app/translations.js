/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

Ember.I18n.translations = {
  'common':{
    'create': 'Create',
    'add':"Add",
    'edit':"Edit",
    'name':"Name",
    'path':"Path",
    'owner':"Owner",
    'save':"Save",
    'delete':"Delete",
    'created':"Created",
    'history':"History",
    'clone':"Clone",
    'cancel':"Cancel",
    'discard_changes':"Discard Changes",
    'arguments':"Arguments",
    'errorLog':"Stack Trace",
    'showErrorLog':"Show Stack Trace",
    'warning':"Warning",
    'close':"Close",
    'download':"Download",
    'show':'Show:',
    'actions':'Actions',
    'date':'Date',
    'success':'Success',
    'error':'Error',
    'copy':'Copy'
  },
  'scripts':{
    'script':"Script",
    'scripts':"Scripts",
    'newscript': "New Script",
    'title': "Name",
    'path': "Script HDFS Location (optional)",
    'not_run_message': "Not run",
    'noScripts': "No pig scripts have been created. To get started, click New Script.",
    'last_executed':'Last Executed',
    'last_results':'Last Results',
    'no_jobs_message':'This script has not been executed',
    'load_error':'Error loading scripts',
    'load_error_single':'Error loading script',
    'not_found':'Script not found',
    'modal':{
      'create_script':'New Script',
      'unsaved_changes_warning':'You have unsaved changes in script.',
      'script_title_placeholder': 'Script name',
      'file_path_placeholder':'/hdfs/path/to/pig/script',
      'file_path_hint':'Leave empty to create file automatically.',
      'copy_created_massage':'{{title}} created successfully.',
      'copy_created':'Copy Created',
      'continue_editing':'Continue Editing',
      'go_to_copy':'Go to Copy',
      'error_empty_title':'Name cannot be empty',
      'error_empty_scriptcontent':'Script content cannot be empty',
      'confirm_delete':'Confirm Delete',
      'confirm_delete_massage':'Are you sure you want to delete {{title}} script?'
    },
    'alert':{
      'file_not_found':'File not found',
      'arg_present':'Argument already present',
      'file_exist_error':'File already exist',
      'script_saved':'{{title}} saved!',
      'script_created':'{{title}} created',
      'script_deleted':'{{title}} deleted',
      'create_failed':'Failed to create script',
      'delete_failed':'Delete failed',
      'save_error':'Error while saving script',
      'save_error_reason':'{{message}}',
      'rename_unfinished':'Please rename script first.'
    }
  },
  'editor':{
    'title_updated':'Name updated',
    'pig_argument':'Pig argument',
    'pighelper':'PIG helper',
    'udfhelper':'UDF helper',
    'actions':'Actions',
    'save':'Save',
    'params':'Params',
    'arguments':'Arguments',
    'no_arguments_message':'This pig script has no arguments defined.',
    'execute':'Execute',
    'explain':'Explain',
    'syntax_check':'Syntax check',
    'execute_on_tez':'Execute on Tez',
    'toggle_fullscreen':'Toggle fullscreen (F11)'
  },
  'job':{
    'title': "Title",
    'name': "Name",
    'results':'Results',
    'logs':'Logs',
    'job_status':'Job status: ',
    'status':'Status',
    'jobId':'Job ID',
    'started':'Started',
    'noJobs': "No jobs to display",
    'kill_job': "Kill Job",
    'script_details': "Script Details",
    'script_contents': "Script contents",
    'no_arguments_message':'This job was executed without arguments.',
    'alert':{
      'job_started' :'Job started!',
      'job_killed' :'{{title}} job killed',
      'job_kill_error' :'Job kill failed',
      'start_filed' :'Job failed to start',
      'load_error' :'Error loading job. Reason: {{message}}',
      'stdout_error' :'Error loading STDOUT. \n Status: {{status}} Message: {{message}}',
      'stderr_error' :'Error loading STDERR. \n Status: {{status}} Message: {{message}}',
      'exit_error' :'Error loading EXITCODE. \n Status: {{status}} Message: {{message}}',
      'promise_error' :'Error loading file. \n Status: {{status}} Message: {{message}}',
      'job_deleted' :'Job deleted successfully',
      'delete_filed' :'Failed to delete job'
    },
    'job_results':{
      'stdout':'Stdout',
      'stderr':'Stderr',
      'exitcode':'Exit code',
      'stdout_loading':'Loading stdout...',
      'stderr_loading':'Loading stderr...',
      'exitcode_loading':'Loading exitcode...'
    },
    'modal':{
      'confirm_delete_massage':'Are you sure you want to delete {{title}} job?'
    }
  },
  'udfs':{
    'udfs':'UDFs',
    'create':'Create UDF',
    'load_error':'Error loading UDFs',
    'tooltips':{
      'path':'Path of this script file on HDFS'
    },
    'noUDFs': "No UDFs to display",
    'alert':{
      'udf_created':'{{name}} created',
      'udf_deleted':'{{name}} deleted',
      'create_failed':'Failed to create UDF',
      'delete_failed':'Delete failed'
    },
    'modal':{
      'create_udf':'Create UDF',
      'udf_name':'UDF name',
      'hdfs_path':'/hdfs/path/to/udf',
      'delete_udf':'Are you sure you want to delete {{title}} udf?'
    }
  },
  'history':{
    'duration':'Duration',
    'no_jobs_message':'No jobs was run',
    'load_error':'Error loading pig history.'
  },
  'splash':{
    'welcome':'Welcome to the Pig View',
    'please_wait':'Testing connection to services...please wait.',
    'storage_test':'Storage test',
    'hdfs_test':'HDFS test',
    'webhcat_test':'WebHCat test',
    'userhome_test':'User Home Directory test'
  }
};
