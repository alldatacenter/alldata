/**
 * Created by yli on 2017/4/21.
 */

var searchArr;
//定义一个search的，判断浏览器有无数据存储（搜索历史）
if(localStorage.search){
//如果有，转换成 数组的形式存放到searchArr的数组里（localStorage以字符串的形式存储，所以要把它转换成数组的形式）
    searchArr= localStorage.search.split(",")
}else{
//如果没有，则定义searchArr为一个空的数组
    searchArr = [];
}
//把存储的数据显示出来作为搜索历史
MapSearchArr();


$("#btn").on("click", function(){
    var val = $("#inp").val();
//点击搜索按钮时，去重
    KillRepeat(val);
//去重后把数组存储到浏览器localStorage
    localStorage.search = searchArr;
//然后再把搜索内容显示出来
    MapSearchArr();
});


function MapSearchArr(){
    var tmpHtml = "";
    for (var i=0;i<searchArr.length;i++){
        tmpHtml += "<span>" + searchArr[i] + "</span> "
    }
    $("#keyname").html(tmpHtml);
}
//去重
function KillRepeat(val){
    var kill = 0;
    for (var i=0;i<searchArr.length;i++){
        if(val===searchArr[i]){
            kill ++;
        }
    }
    if(kill<1){
        searchArr.push(val);
    }
}

