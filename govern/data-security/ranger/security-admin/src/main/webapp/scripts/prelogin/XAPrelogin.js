/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

 
//Define indexOf for IE
if (!Array.indexOf) {
	Array.prototype.indexOf = function(obj, start) {
		for ( var i = (start || 0); i < this.length; i++) {
			if (this[i] == obj) {
				return i;
			}
		}
		return -1;
	};
}


function doLogin() {
	
	var userName = $('#username').val().trim();
	var passwd 	 = $('#password').val().trim();

	if (userName === '' || passwd === '') {
		$('#errorBox').show();
		$('#signInLoading').hide();
		$('#signIn').removeAttr('disabled');
		$('#errorBox .errorMsg').text("The username or password you entered is incorrect.");
		return false;
	}

	var regexEmail = /^([a-zA-Z0-9_\.\-\+])+\@(([a-zA-Z0-9\-])+\.)+([a-zA-Z0-9]{2,4})+$/;
	var regexPlain = /^([A-Za-z0-9_]|[\u00C0-\u017F])([a-z0-9,._\-+/@= ]|[\u00C0-\u017F])+$/i;
	
	if(!regexPlain.test(userName)){
		if(!regexEmail.test(userName)){
                    $('#errorBox').show();
                    $('#signInLoading').hide();
                    $('#signIn').removeAttr('disabled');
			$('#errorBox .errorMsg').text("Invalid username.");
			return false;
		}
	}	
	var baseUrl = getBaseUrl();

	$.ajax({
		data : {
			username : $('#username').val(),
			password : $('#password').val()
		},
		url : baseUrl,
		type : 'POST',
		headers : {
			"cache-control" : "no-cache"
		},
		success : function() {
			if (localStorage && localStorage['backgrid-colmgr']) {
				delete localStorage['backgrid-colmgr'];
			}
			if(location.hash.length > 2)
				window.location.replace('index.html'+location.hash);
			else
				window.location.replace('index.html');
		},
		error : function(jqXHR, textStatus, err ) {
			$('#signIn').removeAttr('disabled');
			$('#signInLoading').hide();

			if(jqXHR.status && jqXHR.status == 412){
				$('#errorBox').hide();
				$('#errorBoxUnsynced').show();
			} else {
				var resp = JSON.parse(jqXHR.responseText);
				$('#errorBox .errorMsg').text(resp.msgDesc);
				$('#errorBox').show();
				$('#errorBoxUnsynced').hide();
			}
		}
		
	});

}
function getBaseUrl(){
	if(!window.location.origin){
		window.location.origin = window.location.protocol + "//" + window.location.hostname + (window.location.port ? ':' + window.location.port: '');
	}
	return window.location.origin + window.location.pathname.substring(window.location.pathname.lastIndexOf('.'),0);
}
$(function() {
  	// register handlers
	$('#signIn').on('click', function() {
		$('#signIn').attr('disabled',true);
		$('#signInLoading').show();
		doLogin();
		return false;
	});
	$('#loginForm').each(function() {
		$('input').keypress(function(e) {
			// Enter pressed?
			if (e.which == 10 || e.which == 13) {
				doLogin();
			}
		});
	});
	
	$('#loginForm  li[class^=control-group] > input').on('change',function(e){
		if(e.target.value === ''){
			$(e.target).parent().addClass('error');
		}else{
			$(e.target).parent().removeClass('error');
		}
	});
});
