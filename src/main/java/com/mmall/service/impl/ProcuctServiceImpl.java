package com.mmall.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.mmall.common.Const;
import com.mmall.common.ResponseCode;
import com.mmall.common.ServerResponse;
import com.mmall.dao.CategoryMapper;
import com.mmall.dao.ProductMapper;
import com.mmall.pojo.Category;
import com.mmall.pojo.Product;
import com.mmall.service.ICategoryService;
import com.mmall.service.IProcuctService;
import com.mmall.util.DateTimeUtil;
import com.mmall.util.PropertiesUtil;
import com.mmall.vo.ProductDetailVo;
import com.mmall.vo.ProductListVo;
import org.apache.commons.lang3.StringUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lenovo on 2018/10/9.
 */
@Service("iProcuctService")
public class ProcuctServiceImpl implements IProcuctService {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ICategoryService iCategoryService;

    @Autowired
    private CategoryMapper categoryMapper;

    /**
     * 保存或者更新产品
     * 1. 如果产品不为空再进行下面的操作，否则返回
     * 2. 需要判断产品的子图是否为空
     * @param product 保存或者更新的产品
     * @return
     */
    public ServerResponse saveOrUpdateProduct(Product product) {
        if(product != null) {
            // 判断子图是否为空，如果不是空，则将子图的第一个图赋给主图
            if(StringUtils.isNotBlank(product.getSubImages())) {
                // 将子图进行分割，将第一个子图分割出来
                String[] subImageArray = product.getSubImages().split(",");
                if(subImageArray.length > 0) {
                    // 将子图的第一个图赋给主图
                    product.setMainImage(subImageArray[0]);
                }
            }
            // 如果id不是空，就说明是更新产品。如果为空，就说明是新增产品
            if(product.getId() != null) {
                int rowCount = productMapper.updateByPrimaryKey(product);
                if(rowCount > 0)
                    return ServerResponse.createBySuccess("更新产品成功");
                else
                    return ServerResponse.createByErrorMessage("更新产品失败");
            } else {
                int rowCount = productMapper.insert(product);
                if(rowCount > 0)
                   return ServerResponse.createBySuccess("新增产品成功");
                else
                    return ServerResponse.createByErrorMessage("新增产品失败");
            }
        }

        return ServerResponse.createByErrorMessage("新增或者更新产品参数不正确");
    }

    /**
     * 产品上下架
     * @param productId 需要更新的产品的id
     * @param status 产品的状态
     * @return
     */
    public ServerResponse<String> setSaleStatus(Integer productId, Integer status) {
        if(productId == null || status == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(),ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }
        // 创建一个产品Product的对象
        Product product = new Product();
        product.setId(productId);
        product.setStatus(status);
        //　按照product的主键进行有选择的更新
        int rowCount = productMapper.updateByPrimaryKeySelective(product);
        if(rowCount > 0)
            return ServerResponse.createBySuccessMsg("修改产品销售状态成功");
        return ServerResponse.createByErrorMessage("修改产品销售状态失败");
    }

    /**
     * 商品详情功能开发
     * 1. 首先判断商品id是否为空
     * 2. 从数据库中按照商品id来查询数据库中的商品
     * 3. 这里使用到了【POJO->VO】
     * @param productId
     * @return
     */
    public ServerResponse<ProductDetailVo> manageProductDetail(Integer productId) {
        if(productId == null)
            return ServerResponse.createByErrorMessage("参数错误");
        Product product = productMapper.selectByPrimaryKey(productId);

        if(product == null)
            return ServerResponse.createByErrorMessage("产品已经下架或者删除");

        // POJO->BO（business object）->VO(View Object)
        ProductDetailVo productDetailVo =aaembleProductDetailVo(product);

        return ServerResponse.createBySuccess(productDetailVo);
    }

    /**
     * 构建produceDetailVo
     * @param product
     * @return
     */
    private ProductDetailVo aaembleProductDetailVo(Product product) {
        ProductDetailVo productDetailVo = new ProductDetailVo();
        productDetailVo.setId(product.getId());
        productDetailVo.setSubtitle(product.getSubtitle());
        productDetailVo.setPrice(product.getPrice());
        productDetailVo.setMainImage(product.getMainImage());
        productDetailVo.setSubImages(product.getSubImages());
        productDetailVo.setCategoryId(product.getCategoryId());
        productDetailVo.setDetail(product.getDetail());
        productDetailVo.setName(product.getName());
        productDetailVo.setStatus(product.getStatus());
        productDetailVo.setStock(product.getStock());

        // imageHost 需要从配置文件中获取，因为这里使用的是图片服务器
        productDetailVo.setImageHost(PropertiesUtil.getProperty("ftp.server.http.prefix","http://img.happymmall.com/"));
        // parentCategoryId 获取商品的
        Category category = categoryMapper.selectByPrimaryKey(product.getCategoryId());
        if(category == null)
            productDetailVo.setParentCategoryId(0); // 默认根节点
        else
            productDetailVo.setParentCategoryId(category.getParentId());

        // createTime
        // updateTime
        // 设置商品详情的创建时间和更新时间，使用了joda-time来实现时间的转换
        productDetailVo.setCurrentTime(DateTimeUtil.dateToStr(product.getCreateTime()));
        productDetailVo.setUpdateTime(DateTimeUtil.dateToStr(product.getUpdateTime()));

        // 返回VO
        return productDetailVo;
    }

    /**
     * 后台商品列表动态分页功能开发
     * @param pageNum
     * @param pageSize
     * @return
     */
    public  ServerResponse<PageInfo> getProductList(int pageNum,  int pageSize) {
        // startPage -- start
        // 填充自己的sql查询逻辑
        // pageHelper-收尾
        PageHelper.startPage(pageNum,pageSize);
        // 查询商品的list
        List<Product> productList = productMapper.selectList();
        // 将查询到的商品组装起来
        List<ProductListVo> productListVoList = Lists.newArrayList();
        for(Product productItem : productList) {
            ProductListVo productListVo = assembelProductListVo(productItem);
            productListVoList.add(productListVo);
        }

        PageInfo pageResult = new PageInfo(productList);
        pageResult.setList(productListVoList);
        return ServerResponse.createBySuccess(pageResult);
    }

    private ProductListVo assembelProductListVo(Product product) {
        ProductListVo productListVo = new ProductListVo();
        productListVo.setId(product.getId());
        productListVo.setName(product.getName());
        productListVo.setCategoryId(product.getCategoryId());
        productListVo.setImageHost(PropertiesUtil.getProperty(""));
        productListVo.setMainImage(product.getMainImage());
        productListVo.setPrice(product.getPrice());
        productListVo.setSubtitle(product.getSubtitle());
        productListVo.setStatus(product.getStatus());
        return productListVo;
    }

    /**
     * 后台商品搜索功能
     *
     * @param productName
     * @param productId
     * @param pageNum
     * @param pageSize
     * @return
     */
    public ServerResponse<PageInfo> searchProduct(String productName,Integer productId,int pageNum,int pageSize) {
        PageHelper.startPage(pageNum,pageSize);
        if(StringUtils.isNotBlank(productName)) {
            productName = new StringBuilder().append("%").append(productName).append("%").toString();

        }
        // 从数据库中按照商品名字或者按照商品的ID来查询商品信息
        List<Product>  productList = productMapper.selectByNameAndProductId(productName,productId);

        List<ProductListVo> productListVoList = Lists.newArrayList();
        for(Product productItem : productList) {
            ProductListVo productListVo = assembelProductListVo(productItem);
            productListVoList.add(productListVo);
        }

        PageInfo pageResult = new PageInfo(productList);
        pageResult.setList(productListVoList);
        return ServerResponse.createBySuccess(pageResult);
    }

    public ServerResponse<ProductDetailVo> getProductDetail(Integer productId) {
        if(productId == null)
            return ServerResponse.createByErrorMessage("参数错误");
        Product product = productMapper.selectByPrimaryKey(productId);

        if(product == null) {
            return ServerResponse.createByErrorMessage("产品已经下架或者删除");
        }
        if(product.getStatus() != Const.ProductStatusEnum.ON_SALE.getCode())
            return ServerResponse.createByErrorMessage("产品已经下架或者删除");

        ProductDetailVo productDetailVo =aaembleProductDetailVo(product);

        return ServerResponse.createBySuccess(productDetailVo);
    }

    /**
     * 产品搜索及动态排序List
     * @param keyword
     * @param categoryId
     * @param pageNum
     * @param pageSize
     * @param orderBy
     * @return
     */
    public ServerResponse<PageInfo> getProductByKeyworldCategory(String keyword,Integer categoryId,int pageNum,int pageSize,String orderBy) {
        if(StringUtils.isBlank(keyword) && categoryId == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(),ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }
        // 如果写了一个大的分类，就要把所有的该分类下的商品选出来，使用递归算法
        List<Integer> categoryIdList = new ArrayList<>();

        if(categoryId != null ) {
            Category category = categoryMapper.selectByPrimaryKey(categoryId);
            if (categoryId == null && StringUtils.isBlank(keyword)) {
                //没有该分类，并且还没有关键字，这个时候返回一个空的结果集，不报错
                PageHelper.startPage(pageNum, pageSize);
                List<ProductDetailVo> productDetailVoList = Lists.newArrayList();
                PageInfo pageInfo = new PageInfo(productDetailVoList);
                return ServerResponse.createBySuccess(pageInfo);
            }
            categoryIdList = iCategoryService.selectCategoryAndChildrenById(category.getId()).getData();
        }
            if(StringUtils.isNotBlank(keyword)) {
                keyword = new StringBuilder().append("%").append(keyword).append("%").toString();
            }
            PageHelper.startPage(pageNum,pageSize);
            // 排序处理
            if(StringUtils.isNotBlank(orderBy)) {
                if(Const.ProductListOrderBy.PRICE_ASC_DESC.contains(orderBy)) {
                    String[] orderByArray = orderBy.split("_");
                    PageHelper.orderBy(orderByArray[0] + " " + orderByArray[1]);
                }
            }
            List<Product> productList = productMapper.selectByNameAndCategoryIds(StringUtils.isBlank(keyword) ? null:keyword,categoryIdList.size() == 0 ? null:categoryIdList);
            List<ProductListVo> productListVoList = Lists.newArrayList();
            for(Product product : productList) {
                ProductListVo productListVo = assembelProductListVo(product);
                productListVoList.add(productListVo);
            }
            PageInfo pageInfo = new PageInfo(productList);
            pageInfo.setList(productListVoList);
            return ServerResponse.createBySuccess(pageInfo);
    }


}
