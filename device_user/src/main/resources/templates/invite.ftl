<!DOCTYPE html>
<html lang="en">

<head>

    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">
    <meta name="renderer" content="webkit">
    <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no">
    <title>
        <%= webpackConfig.name %>
    </title>
    <style>

        h1{
            text-align: center;
            font-size: 36px;
            margin: 0 auto;

        }
        button {

            width: 200px;
            height: 50px;
            display:block;
            font-size: 20px;
            text-decoration:none;
            margin:0 auto;
            text-align: center;
            text-decoration: none;
            color: initial;
            box-shadow: 0 1px 0 #3757b7 inset,-1px 0 0 #2479be inset,-2px 0 0 #9715ce inset;
            background:-webkit-linear-gradient(top left,#948dee,#1b86e4);
            background:-moz-linear-gradient(top left,#656565,#cb873a);
            background:linear-gradient(top left,#656565,#4C4C4C);
        }
        .images{
            margin: 0 auto;
            display: flex;
            justify-content: center;
            align-items: center;
        }

    </style>
</head>

<body>
<div style="background-color: white;">
    <div class="images">
        <img src="https://mail001-1317295795.cos.ap-nanjing.myqcloud.com/imgs/20230315235428.png">
    </div>
    <div>
        <h1>你好,${userName},欢迎使用设备运维系统</h1> <br>
        <h1>你有如下待办事项</h1> <br>
        <a style="text-decoration:none;  bottom: 200px;" href=${url}>
            <button >处理待办事项</button>
        </a>
        <br><br><br>
        <!-- 打印空格 -->
        &nbsp;&nbsp;
    </div>
</div>


</body>

</html>