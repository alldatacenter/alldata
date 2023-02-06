function loadServices() {
    $.getJSON("../services", function(resp) {
        if (!resp.success || !resp.data) {
            return
        }
        var services = resp.data
        for (i=0; i< services.length; i++) {
            (function (service) {
                var elem = $('<li><a href="#">' + service.service + '</a></li>').click(function() {
                    $("input[name=service_url]").val(service.url)
                })
                $("#service-list").append(elem)
            })(services[i])
        }
    });
}

function loadProxies() {
    $.getJSON("../apis", function(resp) {
        if (!resp.success || !resp.data) {
            return
        }
        $("#proxy-list tbody").html("")
        var list = resp.data;
        console.log(list, list.length)
        for (i=0; i < list.length; i++) {
            (function(proxy) {
                var auth = "";
                if (proxy.auth && !proxy.auth.noCheck) {
                    var auths = []
                    if (proxy.auth.checkLogin) {
                        auths.push("CheckLogin")
                    }
                    if (proxy.auth.tryCheckLogin) {
                        auths.push("TryCheckLogin")
                    }
                    if (proxy.auth.checkToken) {
                        auths.push("CheckToken")
                    }
                    if (proxy.auth.checkBasicAuth) {
                        auths.push("CheckBasicAuth")
                    }
                    auth = auths.join(" | ")
                }
                var tr = $("<tr>"+
                    "<td>" +  $("<div />").html(proxy.method).text()+ "</td>" +
                    "<td>" +  $("<div />").html(proxy.path).text()+ "</td>" +
                    "<td>" +  $("<div />").html(proxy.service_url+proxy.backend_path).text() + "</td>"+
                    "<td>" +  $("<div />").html(auth).text()+ "</td>"+
                "</tr>")
                var elem = $("<button type=\"button\" class=\"btn btn-danger\">Delete</button>").click(function(e) {
                    if (confirm("Delete ["+ proxy.method + " "+proxy.path+"] ?")) {
                        deleteProxy(proxy.method, proxy.path)
                    }
                })
                tr.append($("<td></td>").append(elem));
                $("#proxy-list tbody").append(tr);
            })(list[i])
        }
    });
}

function deleteProxy(method, path) {
    $.ajax({
        url: "../apis",
        type: "DELETE",
        data: JSON.stringify({
            method: method,
            path: path,
        }),
        headers: {
            "Content-Type": "application/json"
        },
        success: function(data) {
            $("#msg-box").html("OK").show();
            $("#error-msg-box").html("").hide();
            loadProxies();
        },
        error: function(err) {
            $("#msg-box").html("").hide();
            $("#error-msg-box").text(err.responseText).show();
        },
    });
}

$(function() {
    loadServices();
    loadProxies();
    $("#method-list li").click(function(e) {
        $("input[name=method]").val($(e.target).html())
    });
    $("#submit").click(function() {
        var data = {
            method: $("input[name=method]").val(),
            path: $("input[name=path]").val(),
            service_url: $("input[name=service_url]").val(),
            backend_path: $("input[name=backend_path]").val(),
        }
        var check_login = $("input[name=check_login]")[0].checked;
        var try_check_login = $("input[name=try_check_login]")[0].checked;
        var check_token = $("input[name=check_token]")[0].checked;
        var check_basic_auth = $("input[name=check_basic_auth]")[0].checked;
        if (check_login || try_check_login || check_token || check_basic_auth) {
            data.auth = {
                check_login: check_login,
                try_check_login: try_check_login,
                check_token: check_token,
                check_basic_auth: check_basic_auth
            }
        }
        $.ajax({
            url: "../apis",
            type: "PUT",
            data: JSON.stringify(data),
            headers: {
                "Content-Type": "application/json"
            },
            success: function(data) {
                $("#msg-box").html("OK").show();
                $("#error-msg-box").html("").hide();
                loadProxies();
            },
            error: function(err) {
                $("#msg-box").html("").hide();
                $("#error-msg-box").text(err.responseText).show();
            },
        })
    })
})