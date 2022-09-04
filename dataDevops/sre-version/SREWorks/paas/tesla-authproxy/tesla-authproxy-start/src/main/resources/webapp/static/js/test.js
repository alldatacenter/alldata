$(function() {
    //addPermission
	$("#addPermission").click(function(){
		var addPermission = {"appId":"tesla_odps"};
		$.ajax({
			url : "permission/add?t="+(new Date()).valueOf(),
			method : "POST",
			contentType:"application/json",
			data: JSON.stringify(addPermission),
			dataType : "json",
			async : false,
			success : function(data) {
				if(null != data.errorCode){
					if(data.errorCode == "401"){
						alert("401，没有登录，跳转到登录页面");
						window.location.href=data.loginUrl;
					}else{
						$("#result").html("");
						$("#result").html("errorCode:"+data.errorCode+",errorMsg:"+data.errorMsg);
					}
				}else{
					var jsonData = JSON.stringify(data.data);
					$("#result").html("");
					$("#result").html(jsonData);
				}
			},
			error : function(XMLHttpRequest, textStatus, errorThrown) {
			}
		});
	});

});