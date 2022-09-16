<html>

<head>
    <meta charset="GB2312">
    <title>test1</title>
    <%--<script type="text/javascript" src="./js/analytics.js"></script>--%>
    <script type="text/javascript">
      (function () {
        var _aelog_ = _aelog_ || window._aelog_ || [];
        //设置_aelog_的属性
        _aelog_.push(["memberId", "gerryliu"]);
        window._aelog_ = _aelog_;
        (function () {
          var aejs  = document.createElement('script');
          aejs.type = 'text/javascript';
          aejs.async = true;
          aejs.src = './js/analytics.js';
          var script = document.getElementsByTagName('script')[0];
          script.parentNode.insertBefore(aejs, script);
        })();
      })();
    </script>
</head>
<body>
    test page4<br/>
    set memberid = gerryliu<br/>
    refer to:
    <a href="demo.jsp">demo</a>
    <a href="demo2.jsp">demo2</a>
    <a href="demo3.jsp">demo3</a>
    <a href="demo4.jsp">demo4</a>

</body>
</html>
