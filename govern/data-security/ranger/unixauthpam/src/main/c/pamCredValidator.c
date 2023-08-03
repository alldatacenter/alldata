/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

 /*
  You need to add the following (or equivalent) to the
  /etc/pam.d/ranger-remote file:
  # check authorization
  auth       required     pam_unix.so
  account    required     pam_unix.so
 */

#include <stdio.h>
#include <stdarg.h>
#include <unistd.h>
#include <stdlib.h>
#include <pwd.h>
#include <string.h>
#include <sys/types.h>
#include <security/pam_appl.h>

#define STRLEN 64

int pamconv(int num_msg, const struct pam_message **msg, struct pam_response **resp, void *appdata_ptr) {
  if (num_msg != 1 || msg[0]->msg_style != PAM_PROMPT_ECHO_OFF) {
		fprintf(stderr, "ERROR: Unexpected PAM conversation '%d/%s'\n", msg[0]->msg_style, msg[0]->msg);
		return PAM_CONV_ERR;
  }
  if (!appdata_ptr) {
		fprintf(stderr, "ERROR: No password available to conversation!\n");
		return PAM_CONV_ERR;
  }
	*resp = calloc(num_msg, sizeof(struct pam_response));
	if (!*resp) {
		fprintf(stderr, "ERROR: Out of memory!\n");
		return PAM_CONV_ERR;
  }
  (*resp)[0].resp = strdup((char *) appdata_ptr);
  (*resp)[0].resp_retcode = 0;

	return ((*resp)[0].resp ? PAM_SUCCESS : PAM_CONV_ERR);
}

struct pam_conv conv = { pamconv, NULL };

int main(int ac, char **av, char **ev)
{
	char username[STRLEN] ;
	char password[STRLEN] ;
	char line[512] ;
	char format[20];

	int retval;
	pam_handle_t *pamh = NULL;

	sprintf(format, "LOGIN:%%%ds %%%ds", STRLEN-1, STRLEN-1);
	fgets(line,512,stdin) ;
	sscanf(line, format, username,password) ;
	conv.appdata_ptr = (char *) password;

	retval = pam_start("ranger-remote", username, &conv, &pamh);
	if (retval != PAM_SUCCESS) {
		/* why expose this? */
		fprintf(stdout, "FAILED: [%s] does not exists.\n", username) ;
		if (pamh) {
			pam_end(pamh, retval);
		}
		exit(1);
	}

	retval = pam_authenticate(pamh, 0);
	if (retval != PAM_SUCCESS) {
		fprintf(stdout, "FAILED: Password did not match(%s).\n", pam_strerror(pamh, retval)) ;
		if (pamh) {
			pam_end(pamh, retval);
		}		
		exit(1);
	}

	/* authorize */
	retval = pam_acct_mgmt(pamh, 0);
	if (retval != PAM_SUCCESS) {
		fprintf(stdout, "FAILED: [%s] is not authorized.\n", username) ;
		if (pamh) {
			pam_end(pamh, retval);
		}
		exit(1);
	}

	/* establish the requested credentials */
	if ((retval = pam_setcred(pamh, PAM_ESTABLISH_CRED)) != PAM_SUCCESS) {
			fprintf(stdout, "FAILED: Error setting credentials for [%s].\n", username) ;
			if (pamh) {
				pam_end(pamh, retval);
			}
    		exit(1);
	}

	/* not opening a session, as logout has not been implemented as a remote service */
	fprintf(stdout, "OK:\n") ;

	if (pamh) {
		pam_end(pamh, retval);
	}

	exit(0) ;
}
