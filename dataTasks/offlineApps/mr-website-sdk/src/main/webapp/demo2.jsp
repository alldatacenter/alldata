<html>

<head>
    <meta charset="GB2312">
    <title>test1</title>
    <script type="text/javascript" src="./js/analytics.js"></script>
</head>
<body>
    test page2<br/>
    <label>orderid:123456</label><br/>
    <label>orderName:test_order_123456</label><br/>

    <label>currencyAmount:524.01</label><br/>

    <label>paymentType:alipay</label><br/>
    <button onclick="__AE__.onChargeRequest('123456', 'test_order_123456'
    , 524.01, 'RMB', 'alipay')">buy</button>

    refer to：
    <a href="demo.jsp">demo</a>
    <a href="demo2.jsp">demo2</a>
    <a href="demo3.jsp">demo3</a>
    <a href="demo4.jsp">demo4</a>

</body>
</html>
