/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more contributor
 *  license agreements. See the NOTICE file distributed with this work for additional
 *  information regarding copyright ownership. The ASF licenses this file to
 *  You under the Apache License, Version 2.0 (the "License"); you may not use
 *  this file except in compliance with the License. You may obtain a copy of
 *  the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required
 *  by applicable law or agreed to in writing, software distributed under the
 *  License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 *  OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

// Shows Json message from the server
function serverMessage(data) {
    const messageEl = $("#message");
    if (data.result === "Success") {
        messageEl.removeClass("d-none")
            .removeClass("alert-danger")
            .addClass("alert-info")
            .text(data.result).alert();
        setTimeout(function() { window.location.href = "/storage"; }, 800);
        return true;
    } else {
	    const errorMessage = data.errorMessage || data.responseJSON["result"];

        messageEl.addClass("d-none");
        // Wait a fraction of a second before showing the message again. This
        // makes it clear if a second attempt gives the same error as
        // the first that a "new" message came back from the server
        setTimeout(function() {
            messageEl.removeClass("d-none")
                .removeClass("alert-info")
                .addClass("alert-danger")
                .text("Please retry: " + errorMessage).alert();
        }, 200);
        return false;
    }
}
