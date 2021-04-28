<%@ page import="java.util.ArrayList" %>
<%@ page import="com.google.gson.JsonObject" %>
<%@ page import="com.google.gson.JsonArray" %>
<%@ page import="java.util.List" %>
<%@ page import="java.awt.*" %><%--
  Created by IntelliJ IDEA.
  User: atiyakailany
  Date: 11/25/20
  Time: 9:27 PM
  To change this template use File | Settings | File Templates.
--%>
<%--<%@ page contentType="text/html;charset=UTF-8" language="java" %>--%>
<html>
<head>
    <!-- Global site tag (gtag.js) - Google Analytics -->
    <script async src="https://www.googletagmanager.com/gtag/js?id=G-RJB1R5D39E"></script>
    <script>
        window.dataLayer = window.dataLayer || [];
        function gtag(){dataLayer.push(arguments);}
        gtag('js', new Date());
        gtag('config', 'G-322HJRMC6C');
    </script>

    <link rel="stylesheet" href="./css/index.css" type="text/css"/>
    <title>Title</title>
</head>
<body>
<script>

    function mouseOver(x) {

        let color = x.style.backgroundColor
        let weight = x.style.width

        let colorText = document.getElementById("colorText")
        let colorPercent = document.getElementById("colorPercent")
        let colorBlock = document.getElementById("colorBlock")

        var a = color.split("(")[1].split(")")[0];
        a = a.split(",");
        var b = a.map(function(x){             //For each array element
            x = parseInt(x).toString(16);      //Convert to a base16 string
            return (x.length==1) ? "0"+x : x;  //Add zero if we get only one character
        })
        b = "#"+b.join("");

        colorText.innerHTML = b.toUpperCase() + ", " +color.toUpperCase()
        colorPercent.innerHTML = parseFloat(weight).toFixed(2)+"%"

        colorBlock.style.backgroundColor = color
    }

</script>
<%

    String userID = (String) request.getAttribute("userID");
    String image_url = (String) request.getAttribute("image_url");
    String api_call = (String) request.getAttribute("api_call");
    String photoID = (String) request.getAttribute("photoID");
    String currentIndex = (String) request.getAttribute("currentIndex");
    String nextIndex = (String) request.getAttribute("nextIndex");
    String searchWord = (String) request.getAttribute("searchWord");

%>

<section class="page">
    <div class="color-section">
        <h1 class="title">Dominant Colors</h1>
        <div class="color-wrapper">
            <div id="test" class="color-container">
                <%
                    String colors[] = (String[]) request.getAttribute("colors");
                    double weights[] = (double[]) request.getAttribute("weights");

                    for (int i = 0; i < colors.length; i++) {

                        out.println("<div onmouseover=\"mouseOver(this)\" class=\"color\" style=\"width:" + weights[i] + "%;background-color:#" + colors[i] + ";\"></div>");
                    }
                %>
            </div>
            <div id="row" class="color-row">
                <div id="colorText" class="color-text">
                #${colors[0].toUpperCase()}, RGB(${Color.decode("0x".concat(colors[0])).getRed()}, ${Color.decode("0x".concat(colors[0])).getGreen()}, ${Color.decode("0x".concat(colors[0])).getBlue()})
                </div>
                <div id="colorPercent" class="color-percent">${String.format("%.2f",weights[0])}</div>
                <div id="colorBlock" class="color-block" style="width:auto;background-color: ${colors[0]}"></div>
            </div>
        </div>
    </div>
    <div class="current-picture">
        <h1 class="title">Current Image</h1>
        <img src="${image_url}" alt="${photoID}"/>
    </div>
</section>


<section class="page">
    <div class="flex-center">
        <div>Current filter: <b>${searchWord}</b></div>
        <form id="form_home" action="${pageContext.request.contextPath}/app" method="get">
            <input type="hidden" name="userID" id="userID" value="${userID}">
            <input type="hidden" name="index" id="index" value="${currentIndex}">
            <input type="text" name="searchWord" id="searchWord">
            <input id="filter" type="submit" class="btn btn-default btn-block" value="Filter">

        </form>
    </div>
    <table align="center">

        <%
            JsonObject S = (JsonObject) request.getAttribute("apiData");

            List<String> images = (List<String>) request.getAttribute("images");

            int numCol = 10;
            for (int i = 0, j = 0; i < images.toArray().length; i++, j++) {
                if (j % numCol == 0) {
                    out.println("<tr>");
                }
                String imageLink = images.get(i);
                out.println("<td><img src=" + imageLink + "></td>");
                if (j % numCol == numCol - 1) {
                    out.println("</tr>");
                }
            }
        %>
    </table>
    <div style="padding: 15px">
        <a style="text-decoration: none" href="app?userID=${userID}&${nextIndex}&searchWord=${searchWord}">
            <div class="next-button">
                Next
            </div>
        </a>
    </div>
</section>



</body>
</html>
