package com.mmall.service.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mmall.common.ServerResponse;
import com.mmall.dao.CategoryMapper;
import com.mmall.pojo.Category;
import com.mmall.service.ICategoryService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.annotations.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * Created by lenovo on 2018/10/9.
 */
@Service("iCategoryService")
public class CategoryServiceImpl implements ICategoryService {
    private Logger logger  = LoggerFactory.getLogger(CategoryServiceImpl.class);

    @Autowired
    CategoryMapper categoryMapper;

    /**
     * 1. 首先判断传递进来的parentId是否正确
     * 2. 创建Category对象，将该对象的名字和父节点添加进来。并且设置category的状态是可用的。
     * 3. 将新创建的category插入到数据库中
     * @param categoryName
     * @param parentId
     * @return
     */
    public ServerResponse addCategory(String categoryName,Integer parentId) {
        if(parentId == null || StringUtils.isBlank(categoryName)) {
            ServerResponse.createByErrorMessage("添加品类参数错误");
        }
        // 创建这个对象
        Category category = new Category();
        category.setName(categoryName);
        category.setParentId(parentId);
        category.setStatus(true); // 分类是可用的

        int rowCount = categoryMapper.insert(category);
        if(rowCount > 0) {
            return ServerResponse.createBySuccess("添加品类成功");
        }
        return ServerResponse.createByErrorMessage("添加品类失败");
    }

    /**
     * 更新产品分类的名字
     * 1. 首先判断传入的更新品类id是否正确
     * 2. 创建一个Category对象，将id和name放进去
     * 3. 然后按照品类主键进行有选择的更新
     *
     * @param categoryId
     * @param categoryName
     * @return
     */
    public ServerResponse updateCategoryName(Integer categoryId, String categoryName) {
        if(categoryId == null || StringUtils.isBlank(categoryName)) {
            return ServerResponse.createByErrorMessage("更新品类参数错误");
        }
        Category category = new Category();
        category.setId(categoryId);
        category.setName(categoryName);

        int rowCount = categoryMapper.updateByPrimaryKeySelective(category);

        if(rowCount > 0)
            return ServerResponse.createBySuccessMsg("跟新品类名字成功");
        return ServerResponse.createByErrorMessage("更新品类名字失败");
    }

    /**
     * 获取当前分类的子节点（平级），且不递归
     * 1. 按照categoryId来查找平级子节点，返回的是一个集合
     * @param categoryId
     * @return
     */
    public ServerResponse<List<Category>> getChildrenParallelCategroy(Integer categoryId) {
        List<Category> categoryList = categoryMapper.selectCategoryChildrenByParentId(categoryId);
        if(CollectionUtils.isEmpty(categoryList)) {
            logger.info("未找到当前分类的子分类");
        }
        return ServerResponse.createBySuccess(categoryList);
    }

    /**
     * 递归查询本节点的id及孩子节点的id
     *
     * @param categoryId
     * @return
     */
    public ServerResponse<List<Integer>> selectCategoryAndChildrenById(Integer categoryId) {

        Set<Category> categorySet = Sets.newHashSet();
        // 调用递归方法。通过这个方法将返回的set值放在了categorySet中
        findChildCategory(categorySet,categoryId);
        // 因为返回的是所有孩子结点的id，所以还需要从返回的categorySet中获取id值
        List<Integer> categoryIdList = Lists.newArrayList();
        if(categoryId != null) {
            for(Category categoryItem : categorySet) {
                categoryIdList.add(categoryItem.getId());
            }
        }
        return ServerResponse.createBySuccess(categoryIdList);
    }


    /**
     * 递归算法：算出子节点
     * 1. 首先要重写对应类的hashcode和equals方法，如果两个对象的hashcode相同，可能会出现equals不同。但是如果equals相同，hashcode一定相同
     * 2. 拿categoryId查一下，如果有返回Category，则要将查出来的Category放入CategorySet中
     * @param categorySet
     * @param categoryId
     * @return
     */

    private Set<Category> findChildCategory(Set<Category>categorySet,Integer categoryId) {
        Category category = categoryMapper.selectByPrimaryKey(categoryId);
        if(category != null) {
            categorySet.add(category);
        }
        // 查找子节点，递归算法一定要有一个退出条件
        // 在mybatis中返回的List如果是没有值，那么mybatis也不会返回null
        List<Category> categoryList = categoryMapper.selectCategoryChildrenByParentId(categoryId);
        for(Category categoryItem : categoryList) {
            // 继续调用自己
            findChildCategory(categorySet,categoryItem.getId());
        }
        return categorySet;
    }
}
