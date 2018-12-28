package com.mmall.controller.portal;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.demo.trade.config.Configs;
import com.google.common.collect.Maps;
import com.mmall.common.Const;
import com.mmall.common.ResponseCode;
import com.mmall.common.ServerResponse;
import com.mmall.pojo.User;
import com.mmall.service.IOrderService;
import com.mmall.util.CookieUtil;
import com.mmall.util.JsonUtil;
import com.mmall.util.RedisPoolUtill;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Iterator;
import java.util.Map;

/**
 * 沙箱当面付文档：https://docs.open.alipay.com/194/105170/
 */
@Controller
@RequestMapping("/order/")
public class OrderController {

    private static  final Logger logger = LoggerFactory.getLogger(OrderController.class);

    @Autowired
    private IOrderService iOrderService;

    /**
     * 创建订单
     *
     * @param shippingId 创建订单时候的地址，即发货地址的 id，需要将这个地址存在订单中
     * @return
     */
    @RequestMapping(value = "create.do",method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<Boolean> create(HttpServletRequest httpServletRequest, Integer shippingId){
        String loginToken = CookieUtil.readLoginToken(httpServletRequest);
        if(StringUtils.isEmpty(loginToken)) {
            return ServerResponse.createByErrorMessage("用户未登录，无法获取当前用户信息");
        }
        String userJson = RedisPoolUtill.get(loginToken);
        User user = JsonUtil.String2Obj(userJson,User.class);


        if(user == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
        }


        return iOrderService.createOrder(user.getId(),shippingId);
    }


    /**
     *  在未付款的时候取消订单
     *
     * @param orderNo
     * @return
     */
    @RequestMapping("cancle.do")
    @ResponseBody
    public ServerResponse<String> cancle(HttpServletRequest httpServletRequest, Long orderNo){
        String loginToken = CookieUtil.readLoginToken(httpServletRequest);
        if(StringUtils.isEmpty(loginToken)) {
            return ServerResponse.createByErrorMessage("用户未登录，无法获取当前用户信息");
        }
        String userJson = RedisPoolUtill.get(loginToken);
        User user = JsonUtil.String2Obj(userJson,User.class);

        if(user == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
        }

        return iOrderService.cancel(user.getId(),orderNo);
    }

    /**
     * 获取购物车中已经选中的商品的商品详情
     *
     * @return
     */
    @RequestMapping("get_order_cart_product.do")
    @ResponseBody
    public ServerResponse<String> getOrderCartProduct(HttpServletRequest httpServletRequest){
        String loginToken = CookieUtil.readLoginToken(httpServletRequest);
        if(StringUtils.isEmpty(loginToken)) {
            return ServerResponse.createByErrorMessage("用户未登录，无法获取当前用户信息");
        }
        String userJson = RedisPoolUtill.get(loginToken);
        User user = JsonUtil.String2Obj(userJson,User.class);


        if(user == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
        }

        return iOrderService.getOrderCartProduct(user.getId());
    }

    /**
     * 前台个人中心中
     * 获取订单列表和订单详情
     * @param orderNo 查看哪一个订单的详情
     * @return
     */
    @RequestMapping("detail.do")
    @ResponseBody
    public ServerResponse detail(HttpServletRequest httpServletRequest,Long orderNo){
        String loginToken = CookieUtil.readLoginToken(httpServletRequest);
        if(StringUtils.isEmpty(loginToken)) {
            return ServerResponse.createByErrorMessage("用户未登录，无法获取当前用户信息");
        }
        String userJson = RedisPoolUtill.get(loginToken);
        User user = JsonUtil.String2Obj(userJson,User.class);

        if(user == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
        }


        return iOrderService.getOrderDetail(user.getId(),orderNo);
    }

    /**
     * 获取订单信息，这个方法需要分页
     * @param pageNum
     * @param pageSize
     * @return
     */
    @RequestMapping("list.do")
    @ResponseBody
    public ServerResponse list(HttpServletRequest httpServletRequest,
                               @RequestParam(value = "pageNum",defaultValue = "1") int pageNum,
                               @RequestParam(value = "pageSize",defaultValue = "10") int pageSize){
        String loginToken = CookieUtil.readLoginToken(httpServletRequest);
        if(StringUtils.isEmpty(loginToken)) {
            return ServerResponse.createByErrorMessage("用户未登录，无法获取当前用户信息");
        }
        String userJson = RedisPoolUtill.get(loginToken);
        User user = JsonUtil.String2Obj(userJson,User.class);


        if(user == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
        }
        return iOrderService.getOrderList(user.getId(),pageNum,pageSize);
    }












    /**
     * 支付订单
     * @param orderNo　订单号
     * @param request 获取上下文，将生成的二维码上传到图片服务器上，然后再将图片地址传递给前端，前端根据地址显示二维码
     * @return
     */
    @RequestMapping(value = "pay.do",method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse pay(HttpServletRequest httpServletRequest, Long orderNo, HttpServletRequest request){
        // 验证用户登录
        String loginToken = CookieUtil.readLoginToken(httpServletRequest);
        if(StringUtils.isEmpty(loginToken)) {
            return ServerResponse.createByErrorMessage("用户未登录，无法获取当前用户信息");
        }
        String userJson = RedisPoolUtill.get(loginToken);
        User user = JsonUtil.String2Obj(userJson,User.class);


        if(user ==null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
        }

        //　将生成的二维码传到【ftp】服务器上，然后返回给前端这个二维码的图片地址
        String path = request.getSession().getServletContext().getRealPath("upload");

        return iOrderService.pay(orderNo,user.getId(),path);
    }

    /**
     * 回调方法，支付宝的回掉会放在 request 中，我们需要从 request 中自己取出来
     * @param request
     * @return 返回Object 会根据阿里pay要求的进行返回
     */
    @RequestMapping("alipay_callback.do")
    @ResponseBody
    public Object alipayCallback(HttpServletRequest request){
        Map<String,String> params = Maps.newHashMap();
        // 使用迭代器去取出来需要的内容
        Map requestParams = request.getParameterMap();
        for(Iterator iter = requestParams.keySet().iterator();iter.hasNext();){
            // 首先取出 name
            String name = (String)iter.next();
            // 然后取出 name 对应的value
            String[] values = (String[]) requestParams.get(name);

            String valueStr = "";
            // 遍历整个 values 数组
            for(int i = 0 ; i <values.length;i++){
                // 将数组 values 拼接到一个字符串中
                valueStr = (i == values.length -1)?(valueStr + values[i]) : (valueStr + values[i] + ",");
            }
            // 从request中拿到了信息
            params.put(name,valueStr);
        }
        logger.info("支付宝回调,sign:{},trade_status:{},参数:{}",params.get("sign"),params.get("trade_status"),params.toString());

        //非常重要,验证回调的正确性,是不是支付宝发的.并且呢还要避免重复通知.

        params.remove("sign_type");
        try {
            // Configs：支付宝源码提供了该工具类
            boolean alipayRSACheckedV2 = AlipaySignature.rsaCheckV2(params, Configs.getAlipayPublicKey(),"utf-8",Configs.getSignType());

            if(!alipayRSACheckedV2){
                return ServerResponse.createByErrorMessage("非法请求,验证不通过,再恶意请求我就报警找网警了");
            }
        } catch (AlipayApiException e) {
            logger.error("支付宝验证回调异常",e);
        }



        //todo 验证各种数据
        // 从各个方面来验证支付宝传来的数据的正确性
        ServerResponse serverResponse = iOrderService.aliCallback(params);
        if(serverResponse.isSuccess()){
            return Const.AlipayCallback.RESPONSE_SUCCESS;
        }
        return Const.AlipayCallback.RESPONSE_FAILED;
    }


    /**
     * 查询订单支付状态功能
     * @param orderNo
     * @return
     */
    @RequestMapping("query_order_pay_status.do")
    @ResponseBody
    public ServerResponse<Boolean> queryOrderPayStatus(HttpServletRequest httpServletRequest, Long orderNo){
        String loginToken = CookieUtil.readLoginToken(httpServletRequest);
        if(StringUtils.isEmpty(loginToken)) {
            return ServerResponse.createByErrorMessage("用户未登录，无法获取当前用户信息");
        }
        String userJson = RedisPoolUtill.get(loginToken);
        User user = JsonUtil.String2Obj(userJson,User.class);


        if(user ==null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
        }
        // 查询订单状态
        ServerResponse serverResponse = iOrderService.queryOrderPayStatus(user.getId(),orderNo);
        // 如果查询订单状态成功，则返回 true，否则返回 false
        if(serverResponse.isSuccess()){
            return ServerResponse.createBySuccess(true);
        }
        return ServerResponse.createBySuccess(false);
    }









}
