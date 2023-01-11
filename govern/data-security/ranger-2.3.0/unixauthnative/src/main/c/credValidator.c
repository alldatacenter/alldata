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
#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <pwd.h>
#include <shadow.h>
#include <string.h>
#include <sys/types.h>
#include <crypt.h>
#include <errno.h>

#define STRLEN 64

int main(int ac, char **av, char **ev)
{
	char username[STRLEN] ;
	char password[STRLEN] ;
	char line[512] ;
	char format[20];
	struct passwd *pwp;
	struct spwd *spwd ; 

	fgets(line,512,stdin) ;
	sprintf(format, "LOGIN:%%%ds %%%ds", STRLEN-1, STRLEN-1);
	sscanf(line, format, username,password) ;

	pwp = getpwnam(username) ;

	if (pwp == (struct passwd *)NULL) {
		fprintf(stdout, "FAILED: [%s] does not exists.\n", username) ;
		exit(1) ;
	}
	
	spwd = getspnam(pwp->pw_name) ;

	if (spwd == (struct spwd *)NULL) {
		fprintf(stdout, "FAILED: unable to get (shadow) password for '%s', because '%s'\n", username, strerror(errno));
		exit(1) ;
	}
	else {
		char *gen = crypt(password,spwd->sp_pwdp) ;
		if (gen == (char *)NULL) {
			fprintf(stdout, "FAILED: crypt failed with: '%s'\n", strerror(errno));
			exit(1);
		}
		if (strcmp(spwd->sp_pwdp,gen) == 0) {
			fprintf(stdout, "OK:\n") ;
			exit(0);
		}
		else {
			fprintf(stdout, "FAILED: Password did not match.\n") ;
			exit(1) ;
		}
	}
	exit(0) ;
}
