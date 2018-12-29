package com.mmall.controller.backend;

import com.google.common.collect.Maps;
import com.mmall.common.ResponseCode;
import com.mmall.common.ServerResponse;
import com.mmall.pojo.Product;
import com.mmall.pojo.User;
import com.mmall.service.IFileService;
import com.mmall.service.IProcuctService;
import com.mmall.service.IUserService;
import com.mmall.util.CookieUtil;
import com.mmall.util.JsonUtil;
import com.mmall.util.PropertiesUtil;
import com.mmall.util.RedisShardedPoolUtill;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * Created by lenovo on 2018/10/9.
 */
@Controller
@RequestMapping("/manage/product")
public class ProductManageController {

    @Autowired
    private IUserService iUserService;

    @Autowired
    private IProcuctService iProcuctService;

    @Autowired
    private IFileService iFileService;

    /**
     * 新增商品或者更新商品
     * 1. 首先判断用户是否登录
     * 2. 其次判断用户的权限
     * 3. 调service层
     * @param product 新增或者更新的产品信息
     * @return
     */
    @RequestMapping("save.do")
    @ResponseBody
    public ServerResponse productSave(HttpServletRequest httpServletRequest, Product product) {
        String loginToken = CookieUtil.readLoginToken(httpServletRequest);
        if(StringUtils.isEmpty(loginToken)) {
            return ServerResponse.createByErrorMessage("用户未登录，无法获取当前用户信息");
        }
        String userJson = RedisShardedPoolUtill.get(loginToken);
        User user = JsonUtil.String2Obj(userJson,User.class);


        if (user == null)
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), "用户未登录，请登录管理员");
        if (iUserService.checkAdminRole(user).isSuccess()) {
            //填充我们增加商品的业务逻辑
            return iProcuctService.saveOrUpdateProduct(product);
        } else {
            return ServerResponse.createByErrorMessage("无权限操作");
        }
    }

    /**
     * 更新商品的状态，即产品上下架
     * @param productId
     * @param status
     * @return
     */
    @RequestMapping("set_sale_status.do")
    @ResponseBody
    public  ServerResponse setSaleStatus(HttpServletRequest httpServletRequest,Integer productId, Integer status) {
        String loginToken = CookieUtil.readLoginToken(httpServletRequest);
        if(StringUtils.isEmpty(loginToken)) {
            return ServerResponse.createByErrorMessage("用户未登录，无法获取当前用户信息");
        }
        String userJson = RedisShardedPoolUtill.get(loginToken);
        User user = JsonUtil.String2Obj(userJson,User.class);

        if(user == null)
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"用户未登录，请登录管理员");

        if(iUserService.checkAdminRole(user).isSuccess()) {
            //
            return iProcuctService.setSaleStatus(productId,status);
        } else {
            return ServerResponse.createByErrorMessage("无权限操作");
        }
    }


    /**
     * 7-3 获取商品详情功能的开发
     *
     * @param productId 获得需要的商品的id
     * @return
     */
    @RequestMapping("detail.do")
    @ResponseBody
    public  ServerResponse getDetail(HttpServletRequest httpServletRequest,Integer productId) {
        String loginToken = CookieUtil.readLoginToken(httpServletRequest);
        if(StringUtils.isEmpty(loginToken)) {
            return ServerResponse.createByErrorMessage("用户未登录，无法获取当前用户信息");
        }
        String userJson = RedisShardedPoolUtill.get(loginToken);
        User user = JsonUtil.String2Obj(userJson,User.class);


        if(user == null)
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"用户未登录，请登录管理员");

        if(iUserService.checkAdminRole(user).isSuccess()) {
            // 填充业务
            return iProcuctService.manageProductDetail(productId);
        } else {
            return ServerResponse.createByErrorMessage("无权限操作");
        }
    }

    // 得到所有的商品，用于分页操作

    /**
     * 后台商品列表动态分页功能的开发
     *
     * @param pageNum 页面的页码，即第几页。默认第一页
     * @param pageSize 页面中能存放的记录数，默认一个页面有10条记录
     * @return
     */
    @RequestMapping("list.do")
    @ResponseBody
    public  ServerResponse getList(HttpServletRequest httpServletRequest,
                                   @RequestParam(value = "pageNum",defaultValue = "1") int pageNum,
                                   @RequestParam(value = "pageSize",defaultValue = "10") int pageSize) {
        String loginToken = CookieUtil.readLoginToken(httpServletRequest);
        if(StringUtils.isEmpty(loginToken)) {
            return ServerResponse.createByErrorMessage("用户未登录，无法获取当前用户信息");
        }
        String userJson = RedisShardedPoolUtill.get(loginToken);
        User user = JsonUtil.String2Obj(userJson,User.class);


        if(user == null)
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"用户未登录，请登录管理员");

        if(iUserService.checkAdminRole(user).isSuccess()) {
            // 填充业务
            return iProcuctService.getProductList(pageNum,pageSize);
        } else {
            return ServerResponse.createByErrorMessage("无权限操作");
        }
    }



    /**
     * 按照商品的名字或者id来查找商品
     * @param productName
     * @param productId
     * @param pageNum
     * @param pageSize
     * @return
     */
    @RequestMapping("search.do")
    @ResponseBody
    public  ServerResponse productSearch(HttpServletRequest httpServletRequest,
                                         String productName,
                                         Integer productId,
                                         @RequestParam(value = "pageNum",defaultValue = "1") int pageNum,
                                         @RequestParam(value = "pageSize",defaultValue = "10") int pageSize) {
        String loginToken = CookieUtil.readLoginToken(httpServletRequest);
        if(StringUtils.isEmpty(loginToken)) {
            return ServerResponse.createByErrorMessage("用户未登录，无法获取当前用户信息");
        }
        String userJson = RedisShardedPoolUtill.get(loginToken);
        User user = JsonUtil.String2Obj(userJson,User.class);

        if(user == null)
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"用户未登录，请登录管理员");

        if(iUserService.checkAdminRole(user).isSuccess()) {
            // 填充业务
            return iProcuctService.searchProduct(productName,productId,pageNum,pageSize);
        } else {
            return ServerResponse.createByErrorMessage("无权限操作");
        }
    }

    /**
     * 上传图片,文件
     *
     * @param session
     * @param file
     * @param request
     * @return
     */
    @RequestMapping("upload.do")
    @ResponseBody
    public ServerResponse upload(HttpServletRequest httpServletRequest,
                                 @RequestParam(value = "upload_file",required = false) MultipartFile file,
                                 HttpServletRequest request) {
        // 权限判断
        String loginToken = CookieUtil.readLoginToken(httpServletRequest);
        if(StringUtils.isEmpty(loginToken)) {
            return ServerResponse.createByErrorMessage("用户未登录，无法获取当前用户信息");
        }
        String userJson = RedisShardedPoolUtill.get(loginToken);
        User user = JsonUtil.String2Obj(userJson,User.class);
        //if(user == null)
        //    return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"用户未登录，请登录管理员");

        if(!iUserService.checkAdminRole(user).isSuccess()) {
            String path = request.getSession().getServletContext().getRealPath("upload");
            System.out.println(path + "=================================================");
            String targetFileName = iFileService.upload(file,path);
            System.out.println(targetFileName + "111111111111111111111111111111111111111111111111111111111111111111111");
            String url = PropertiesUtil.getProperty("ftp.server.http.prefix") + targetFileName;
            Map fileMap = Maps.newHashMap();
            // uri放targetFileName
            fileMap.put("uri",targetFileName);
            fileMap.put("url",url);

            return ServerResponse.createBySuccess(fileMap);
        } else {
            return ServerResponse.createByErrorMessage("无权限操作");
        }
    }


    /**
     * 上传富文本
     *
     * @param file 文件的名字
     * @param request
     * @param response
     * @return
     */
    @RequestMapping("richtext_img_upload.do")
    @ResponseBody
    public Map richtextImgUpload(@RequestParam(value = "upload_file",required = false) MultipartFile file,
                                 HttpServletRequest request,
                                 HttpServletResponse response){
        // 声明一个用于返回值的承载对象Map
        Map resultMap = Maps.newHashMap();
        String loginToken = CookieUtil.readLoginToken(request);
        if(StringUtils.isEmpty(loginToken)) {
            resultMap.put("success",false);
            resultMap.put("msg","请登录管理员");
            return resultMap;
        }
        String userJson = RedisShardedPoolUtill.get(loginToken);
        User user = JsonUtil.String2Obj(userJson,User.class);

        if(user == null){
            resultMap.put("success",false);
            resultMap.put("msg","请登录管理员");
            return resultMap;
        }
        //富文本中对于返回值有自己的要求,我们使用是simditor所以按照simditor的要求进行返回
//        {
//            "success": true/false,
//                "msg": "error message", # optional
//            "file_path": "[real file path]"
//        }
        if(!iUserService.checkAdminRole(user).isSuccess()){
            String path = request.getSession().getServletContext().getRealPath("upload");
            String targetFileName = iFileService.upload(file,path);
            if(StringUtils.isBlank(targetFileName)){
                resultMap.put("success",false);
                resultMap.put("msg","上传失败");
                return resultMap;
            }
            String url = PropertiesUtil.getProperty("ftp.server.http.prefix") + targetFileName;
            resultMap.put("success",true);
            resultMap.put("msg","上传成功");
            resultMap.put("file_path",url);
            // 用富文本上传的时候需要修改HTTP的header
            response.addHeader("Access-Control-Allow-Headers","X-File-Name");
            return resultMap;
        }else{
            resultMap.put("success",false);
            resultMap.put("msg","无权限操作");
            return resultMap;
        }
    }


}
