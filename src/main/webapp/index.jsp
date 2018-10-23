<%@ page language="java"  contentType="text/html; charset=UTF-8" %>
<html>
<body>
<h2>Hello World!</h2>

springmvc上传文件
<form name="form1" action="/Mmall/manage/product/upload.do" method="post" enctype="multipart/form-data">
    <input type = "file" name="upload_file">
    <input type="submit" value="springmvc上传文件">
</form>

<%--<form name="form1" action="/Mmall/test.do" method="post" enctype="multipart/form-data">

</form>--%>

富文本图片上传文件
<form name="form2" action="/Mmall/manage/product/richtext_img_upload.do" method="post" enctype="multipart/form-data">
    <input type="file" name="upload_file" />
    <input type="submit" value="富文本图片上传文件" />
</form>
</body>
</html>
