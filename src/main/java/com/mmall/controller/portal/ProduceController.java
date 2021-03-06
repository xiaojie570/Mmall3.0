package com.mmall.controller.portal;

import com.github.pagehelper.PageInfo;
import com.mmall.common.Const;
import com.mmall.common.ResponseCode;
import com.mmall.common.ServerResponse;
import com.mmall.pojo.User;
import com.mmall.service.IOrderService;
import com.mmall.service.IProcuctService;
import com.mmall.vo.ProductDetailVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;

/**
 * Created by lenovo on 2018/10/10.
 */

@Controller
@RequestMapping("/product/")
public class ProduceController {
    @Autowired
    private IProcuctService iProcuctService;


    /**
     * 获取商品详情
     * @param productId 传入商品的id
     * @return
     */
    @RequestMapping("detail.do")
    @ResponseBody
    public ServerResponse<ProductDetailVo> detail(Integer productId) {
        return iProcuctService.getProductDetail(productId);
    }

    @RequestMapping(value = "/{productId}",method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<ProductDetailVo> detailRESTful(@PathVariable  Integer productId) {
        return iProcuctService.getProductDetail(productId);
    }

    /**
     * 产品搜索及动态排序List
     * @param keyword 关键字，关键字非必须的，因为用户可以不按照关键字搜索，所以设置它的require是false
     * @param categoryId 分类的id，这个也不是必须传入的
     * @param pageNum 页的数量  默认是1
     * @param pageSize 页的大小  默认是10
     * @param orderBy 默认是空字符串
     * @return
     */
    @RequestMapping("list.do")
    @ResponseBody
    public ServerResponse<PageInfo> list(@RequestParam(value="keyword",required = false)String keyword,
                                         @RequestParam(value = "categoryId",required = false) Integer categoryId,
                                         @RequestParam(value = "pageNum",defaultValue = "1") int pageNum,
                                         @RequestParam(value = "pageSize",defaultValue = "10") int pageSize,
                                         @RequestParam(value = "orderBy",defaultValue = "") String orderBy) {
        return iProcuctService.getProductByKeyworldCategory(keyword,categoryId,pageNum,pageSize,orderBy);
    }


    // http://new.nginx.tomcat.com/product/%E6%89%8B%E6%9C%BA/100012/1/10/price_asc
    @RequestMapping(value = "/{keyword}/{categoryId}/{pageNum}/{pageSize}/{orderBy}", method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<PageInfo> listRESTful(@PathVariable(value="keyword")String keyword,
                                         @PathVariable(value = "categoryId") Integer categoryId,
                                         @PathVariable(value = "pageNum") Integer pageNum,
                                         @PathVariable(value = "pageSize") Integer pageSize,
                                         @PathVariable(value = "orderBy") String orderBy) {
        if(pageNum == null)
            pageNum = 1;
        if(pageSize == null)
            pageSize = 20;
        if(StringUtils.isBlank(orderBy)) {
            orderBy = "price_asc";
        }
        return iProcuctService.getProductByKeyworldCategory(keyword,categoryId,pageNum,pageSize,orderBy);
    }


    @RequestMapping(value = "/{categoryId}/{pageNum}/{pageSize}/{orderBy}", method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<PageInfo> listRESTfulBadcase(@PathVariable(value = "categoryId") Integer categoryId,
                                                @PathVariable(value = "pageNum") Integer pageNum,
                                                @PathVariable(value = "pageSize") Integer pageSize,
                                                @PathVariable(value = "orderBy") String orderBy) {
        if(pageNum == null)
            pageNum = 1;
        if(pageSize == null)
            pageSize = 20;
        if(StringUtils.isBlank(orderBy)) {
            orderBy = "price_asc";
        }
        return iProcuctService.getProductByKeyworldCategory("",categoryId,pageNum,pageSize,orderBy);
    }

    @RequestMapping(value = "/{keyword}/{pageNum}/{pageSize}/{orderBy}", method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<PageInfo> listRESTfulBadcase(@PathVariable(value="keyword")String keyword,
                                                       @PathVariable(value = "pageNum") Integer pageNum,
                                                       @PathVariable(value = "pageSize") Integer pageSize,
                                                       @PathVariable(value = "orderBy") String orderBy) {
        if(pageNum == null)
            pageNum = 1;
        if(pageSize == null)
            pageSize = 20;
        if(StringUtils.isBlank(orderBy)) {
            orderBy = "price_asc";
        }
        return iProcuctService.getProductByKeyworldCategory(keyword,null,pageNum,pageSize,orderBy);
    }

    // 这样服务器就知道你请求的是哪一个controller了
    @RequestMapping(value = "/categoryId/{categoryId}/{pageNum}/{pageSize}/{orderBy}", method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<PageInfo> listRESTful(@PathVariable(value = "categoryId") Integer categoryId,
                                                       @PathVariable(value = "pageNum") Integer pageNum,
                                                       @PathVariable(value = "pageSize") Integer pageSize,
                                                       @PathVariable(value = "orderBy") String orderBy) {
        if(pageNum == null)
            pageNum = 1;
        if(pageSize == null)
            pageSize = 20;
        if(StringUtils.isBlank(orderBy)) {
            orderBy = "price_asc";
        }
        return iProcuctService.getProductByKeyworldCategory("",categoryId,pageNum,pageSize,orderBy);
    }


    @RequestMapping(value = "/keyword/{keyword}/{pageNum}/{pageSize}/{orderBy}", method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<PageInfo> listRESTful(@PathVariable(value="keyword")String keyword,
                                                       @PathVariable(value = "pageNum") Integer pageNum,
                                                       @PathVariable(value = "pageSize") Integer pageSize,
                                                       @PathVariable(value = "orderBy") String orderBy) {
        if(pageNum == null)
            pageNum = 1;
        if(pageSize == null)
            pageSize = 20;
        if(StringUtils.isBlank(orderBy)) {
            orderBy = "price_asc";
        }
        return iProcuctService.getProductByKeyworldCategory(keyword,null,pageNum,pageSize,orderBy);
    }
}
