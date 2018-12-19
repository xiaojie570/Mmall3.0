package com.mmall.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Maps;
import com.mmall.common.ServerResponse;
import com.mmall.dao.ShippingMapper;
import com.mmall.pojo.Shipping;
import com.mmall.service.IShippingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * Created by lenovo on 2018/10/11.
 */
@Service("iShippingService")
public class ShippingServiceImpl implements IShippingService {

    @Autowired
    private ShippingMapper shippingMapper;

    /**
     * 新增收货地址
     * 1. 使用springMVC来直接绑定对象，将userId防止在shipping中
     * 2. 将shipping插入数据库中，然后返回生效行数，如果插入成功则将shippingId传递给前端
     * @param userId
     * @param shipping
     * @return
     */
    public ServerResponse add(Integer userId,Shipping shipping) {
        shipping.setUserId(userId);
        int rowCount = shippingMapper.insert(shipping);
        if(rowCount > 0) {
            Map result = Maps.newHashMap();
            // 获取这个自动增长的id是通过设置mybatis层的两个关键字获得：useGeneratedKeys="true" keyProperty="id"
            result.put("shippingId",shipping.getId());
            return ServerResponse.createBySuccessMsg("新建地址成功",result);
        }
        return ServerResponse.createByErrorMessage("新建地址失败");
    }


    /**
     * 删除收货地址。 这里防止横向越权，所以要将收货地址id与用户id都传递到数据库中
     * @param userId  删除的用户id
     * @param shippingId 删除的收货地址id
     * @return
     */
    public ServerResponse<String> del(Integer userId,Integer shippingId){
        int resultCount = shippingMapper.deleteByShippingIdUserId(userId,shippingId);
        if(resultCount > 0){
            return ServerResponse.createBySuccess("删除地址成功");
        }
        return ServerResponse.createByErrorMessage("删除地址失败");
    }


    /**
     * 更新收货地址信息，为了防止横向越权，需要将用户的id传递到mybatis中
     * 注意在mybatis中更改收货地址信息sql中，userid是不能更改的，它只是作为where条件存在
     * @param userId
     * @param shipping
     * @return
     */
    public ServerResponse<String> update(Integer userId,Shipping shipping) {
        // 防止横向越权
        shipping.setUserId(userId);
        int resultCount = shippingMapper.updateByShipping(shipping);
        if(resultCount > 0) {
            return ServerResponse.createBySuccess("更新地址成功");
        }
        return ServerResponse.createByErrorMessage("更新地址失败");
    }

    /**
     * 查询收货地址详细信息
     *
     * @param userId
     * @param shippingId
     * @return
     */
    public ServerResponse<Shipping> select(Integer userId, Integer shippingId){
        Shipping shipping = shippingMapper.selectByShippingIdUserId(userId,shippingId);
        if(shipping == null){
            return ServerResponse.createByErrorMessage("无法查询到该地址");
        }
        return ServerResponse.createBySuccessMsg("查询地址成功",shipping);
    }

    /**
     * 收货地址的分页列表
     * @param userId
     * @param pageNum
     * @param pageSize
     * @return
     */
    public ServerResponse<PageInfo> list(Integer userId, int pageNum,int pageSize) {
        PageHelper.startPage(pageNum,pageSize);
        List<Shipping> shippingList = shippingMapper.selectByUserId(userId);
        PageInfo pageInfo = new PageInfo(shippingList);
        return ServerResponse.createBySuccess(pageInfo);
    }
}
