package com.mmall.service.impl;

import com.alipay.api.AlipayResponse;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.demo.trade.config.Configs;
import com.alipay.demo.trade.model.ExtendParams;
import com.alipay.demo.trade.model.GoodsDetail;
import com.alipay.demo.trade.model.builder.AlipayTradePrecreateRequestBuilder;
import com.alipay.demo.trade.model.result.AlipayF2FPrecreateResult;
import com.alipay.demo.trade.service.AlipayTradeService;
import com.alipay.demo.trade.service.impl.AlipayTradeServiceImpl;
import com.alipay.demo.trade.utils.ZxingUtils;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mmall.common.Const;
import com.mmall.common.ServerResponse;
import com.mmall.dao.CartMapper;
import com.mmall.dao.OrderItemMapper;
import com.mmall.dao.OrderMapper;
import com.mmall.dao.PayInfoMapper;
import com.mmall.dao.ProductMapper;
import com.mmall.dao.ShippingMapper;
import com.mmall.pojo.Cart;
import com.mmall.pojo.Order;
import com.mmall.pojo.OrderItem;
import com.mmall.pojo.PayInfo;
import com.mmall.pojo.Product;
import com.mmall.pojo.Shipping;
import com.mmall.service.IOrderService;
import com.mmall.util.BigDecimalUtil;
import com.mmall.util.DateTimeUtil;
import com.mmall.util.FTPUtil;
import com.mmall.util.PropertiesUtil;
import com.mmall.vo.OrderItemVo;
import com.mmall.vo.OrderProductVo;
import com.mmall.vo.OrderVo;
import com.mmall.vo.ShippingVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.annotations.Mapper;
import org.aspectj.weaver.ast.Or;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Slf4j
@Service("iOrderService")
public class OrderServiceImpl implements IOrderService {


    private static  AlipayTradeService tradeService;

    static {

       /* 一定要在创建AlipayTradeService之前调用Configs.init()设置默认参数
        *  Configs会读取classpath下的zfbinfo.properties文件配置信息，如果找不到该文件则确认该文件是否在classpath目录
                */
        Configs.init("zfbinfo.properties");

        /** 使用Configs提供的默认参数
        *  AlipayTradeService可以使用单例或者为静态成员对象，不需要反复new*/
                tradeService = new AlipayTradeServiceImpl.ClientBuilder().build();
    }

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private PayInfoMapper payInfoMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private CartMapper cartMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ShippingMapper shippingMapper;

    /**
     * 1. 首先从购物车中获取已经被勾选的数据
     * 2. 计算这个订单的总价（写一个私有方法来实现这个功能）
     * 3. 生成订单，组装一下订单
     * @param userId 用户的id
     * @param shippingId
     * @return
     */
    // 用户创建订单
    public  ServerResponse createOrder(Integer userId,Integer shippingId){

        //从购物车中获取数据
        List<Cart> cartList = cartMapper.selectCheckedCartByUserId(userId);
        //计算这个订单的总价
        ServerResponse serverResponse = this.getCartOrderItem(userId,cartList);
        if(!serverResponse.isSuccess()){
            return serverResponse;
        }

        // 获取购物车中的商品信息
        List<OrderItem> orderItemList = (List<OrderItem>)serverResponse.getData();

        // 定义一个私有方法来计算商品的总价
        BigDecimal payment = this.getOrderTotalPrice(orderItemList);


        //生成订单，组装一下订单
        Order order = this.assembleOrder(userId,shippingId,payment);
        if(order == null){
            return ServerResponse.createByErrorMessage("生成订单错误");
        }
        if(CollectionUtils.isEmpty(orderItemList)){
            return ServerResponse.createByErrorMessage("购物车为空");
        }

        // 购物车中同时下单的商品 订单号 是相同的，下面的for循环就是设置订单号
        for(OrderItem orderItem : orderItemList){
            orderItem.setOrderNo(order.getOrderNo());
        }
        //mybatis 批量插入
        orderItemMapper.batchInsert(orderItemList);

        //生成成功,我们要减少我们产品的库存(私有的方法来实现这个功能)
        this.reduceProductStock(orderItemList);
        //清空一下购物车 （私有的方法实现这个功能）
        this.cleanCart(cartList);

        //返回给前端数据，即返回订单的明细信息
        OrderVo orderVo = assembleOrderVo(order,orderItemList);
        return ServerResponse.createBySuccess(orderVo);
    }


    /**
     *
     * @param order
     * @param orderItemList
     * @return 返回一个OrderVO，包含订单信息，订单明细的信息，和收获地址的信息
     */
    private OrderVo assembleOrderVo(Order order,List<OrderItem> orderItemList){

        OrderVo orderVo = new OrderVo();
        // 订单号
        orderVo.setOrderNo(order.getOrderNo());
        // 订单价钱
        orderVo.setPayment(order.getPayment());
        // 订单支付的方法（在线支付）
        orderVo.setPaymentType(order.getPaymentType());
        // 订单支付的描述
        orderVo.setPaymentTypeDesc(Const.PaymentTypeEnum.codeOf(order.getPaymentType()).getValue());

        // 订单的运费
        orderVo.setPostage(order.getPostage());
        // 订单的状态
        orderVo.setStatus(order.getStatus());
        // 订单状态的描述
        orderVo.setStatusDesc(Const.OrderStatusEnum.codeOf(order.getStatus()).getValue());

        // 设置收货地址的 id
        orderVo.setShippingId(order.getShippingId());
        // 从数据库中按照收货地址 id 查询收获地址信息
        Shipping shipping = shippingMapper.selectByPrimaryKey(order.getShippingId());

        // 在这里面徐亚组装 shipping 的信息
        if(shipping != null){
            orderVo.setReceiverName(shipping.getReceiverName());
            // 将 shipping 信息组装起来
            orderVo.setShippingVo(assembleShippingVo(shipping));
        }

        // 设置支付时间
        orderVo.setPaymentTime(DateTimeUtil.dateToStr(order.getPaymentTime()));
        // 设置发货时间
        orderVo.setSendTime(DateTimeUtil.dateToStr(order.getSendTime()));
        orderVo.setEndTime(DateTimeUtil.dateToStr(order.getEndTime()));
        orderVo.setCreateTime(DateTimeUtil.dateToStr(order.getCreateTime()));
        orderVo.setCloseTime(DateTimeUtil.dateToStr(order.getCloseTime()));

        // 设置图片地址
        orderVo.setImageHost(PropertiesUtil.getProperty("ftp.server.http.prefix"));


        List<OrderItemVo> orderItemVoList = Lists.newArrayList();

        for(OrderItem orderItem : orderItemList){
            // 组装订单信息
            OrderItemVo orderItemVo = assembleOrderItemVo(orderItem);
            // 将单个订单信息放到订单中
            orderItemVoList.add(orderItemVo);
        }
        //
        orderVo.setOrderItemVoList(orderItemVoList);
        return orderVo;
    }

    /**
     * 组装 orderItemVo
     * @param orderItem
     * @return
     */
    private OrderItemVo assembleOrderItemVo(OrderItem orderItem){
        OrderItemVo orderItemVo = new OrderItemVo();
        orderItemVo.setOrderNo(orderItem.getOrderNo());
        orderItemVo.setProductId(orderItem.getProductId());
        orderItemVo.setProductName(orderItem.getProductName());
        orderItemVo.setProductImage(orderItem.getProductImage());
        orderItemVo.setCurrentUnitPrice(orderItem.getCurrentUnitPrice());
        // 数量
        orderItemVo.setQuantity(orderItem.getQuantity());
        // 总价
        orderItemVo.setTotalPrice(orderItem.getTotalPrice());

        orderItemVo.setCreateTime(DateTimeUtil.dateToStr(orderItem.getCreateTime()));
        return orderItemVo;
    }



    private ShippingVo assembleShippingVo(Shipping shipping){
        ShippingVo shippingVo = new ShippingVo();
        shippingVo.setReceiverName(shipping.getReceiverName());
        shippingVo.setReceiverAddress(shipping.getReceiverAddress());
        shippingVo.setReceiverProvince(shipping.getReceiverProvince());
        shippingVo.setReceiverCity(shipping.getReceiverCity());
        shippingVo.setReceiverDistrict(shipping.getReceiverDistrict());
        shippingVo.setReceiverMobile(shipping.getReceiverMobile());
        shippingVo.setReceiverZip(shipping.getReceiverZip());
        shippingVo.setReceiverPhone(shippingVo.getReceiverPhone());
        return shippingVo;
    }


    /**
     * 清空购物车
     *
     * @param cartList
     */
    private void cleanCart(List<Cart> cartList){
       // 将购物车中已经付款的订单中的商品清空
        for(Cart cart : cartList){
            cartMapper.deleteByPrimaryKey(cart.getId());
        }
    }


    /**
     * 减少产品的库存
     *
     * @param orderItemList
     */
    private void reduceProductStock(List<OrderItem> orderItemList){
        for(OrderItem orderItem : orderItemList){
            // 按照产品的id，拿到单个产品
            Product product = productMapper.selectByPrimaryKey(orderItem.getProductId());
            // 重新set一下商品的库存
            product.setStock(product.getStock()-orderItem.getQuantity());
            // 按照主键有选择的更新产品信息
            productMapper.updateByPrimaryKeySelective(product);
        }
    }


    /**
     * 组装订单信息
     * 1. 要生成订单号信息
     * 2.
     * @param userId
     * @param shippingId
     * @param payment
     * @return
     */
    private Order assembleOrder(Integer userId,Integer shippingId,BigDecimal payment){
        Order order = new Order();
        long orderNo = this.generateOrderNo();
        order.setOrderNo(orderNo);
        // 组装订单的状态
        order.setStatus(Const.OrderStatusEnum.NO_PAY.getCode());
        // 运费：全场包邮，设置为0
        order.setPostage(0);
        // 设置支付方式，这里使用枚举的方式
        order.setPaymentType(Const.PaymentTypeEnum.ONLINE_PAY.getCode());
        // 设置需要支付的金额
        order.setPayment(payment);
        // 设置用户id
        order.setUserId(userId);
        order.setShippingId(shippingId);
        //发货时间等等
        //付款时间等等

        // 将 order 订单信息插入到数据库中
        int rowCount = orderMapper.insert(order);
        if(rowCount > 0){
            return order;
        }
        return null;
    }


    /**
     * 生成订单号方法，
     * 不能使用主键也不能使用时间，订单号的生成规则比较重要。
     * 这里如果高并发的时候如果返回的是：currentTime + currentTime % 10 就会出现问题
     * @return
     */
    private long generateOrderNo(){
        long currentTime =System.currentTimeMillis();
        return currentTime + new Random().nextInt(100);
    }


    /**
     * 获取订单明细中各个商品的总价，将订单中的商品总价格返回来
     *
     * @param orderItemList
     * @return
     */
    private BigDecimal getOrderTotalPrice(List<OrderItem> orderItemList){
        BigDecimal payment = new BigDecimal("0");
        for(OrderItem orderItem : orderItemList){
            payment = BigDecimalUtil.add(payment.doubleValue(),orderItem.getTotalPrice().doubleValue());
        }
        return payment;
    }

    /**
     * 获取订单总价
     *
     * @param userId  用户的id
     * @param cartList 购物车中被勾选的商品
     * @return
     */
    private ServerResponse getCartOrderItem(Integer userId, List<Cart> cartList){
        List<OrderItem> orderItemList = Lists.newArrayList();
        if(CollectionUtils.isEmpty(cartList)){
            return ServerResponse.createByErrorMessage("购物车为空");
        }

        //校验购物车的数据,包括产品的状态和数量
        for(Cart cartItem : cartList){
            OrderItem orderItem = new OrderItem();
            // 购物车中单个商品的信息，获取到之后还要判断一下这个商品是否还在线售卖
            Product product = productMapper.selectByPrimaryKey(cartItem.getProductId());
            if(Const.ProductStatusEnum.ON_SALE.getCode() != product.getStatus()){
                return ServerResponse.createByErrorMessage("产品" + product.getName() + "不是在线售卖状态");
            }

            //校验库存：如果购物车中的库存 > 产品本身的库存的话，则返回库存不存
            if(cartItem.getQuantity() > product.getStock()){
                return ServerResponse.createByErrorMessage("产品" + product.getName() + "库存不足");
            }

            // 将商品的信息组装到购物车中
            orderItem.setUserId(userId);
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName());
            orderItem.setProductImage(product.getMainImage());
            orderItem.setCurrentUnitPrice(product.getPrice());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setTotalPrice(BigDecimalUtil.mul(product.getPrice().doubleValue(),cartItem.getQuantity()));
            //
            orderItemList.add(orderItem);
        }
        return ServerResponse.createBySuccess(orderItemList);
    }


    /**
     *  在未付款的时候取消订单
     * 1. 先从数据库中查看该用户是否有该订单
     * 2. 查看该订单的状态是否是未付款的，如果已经付款了，这里是不能够取消订单的
     * 3. 更新订单的状态
     *
     * @param userId
     * @param orderNo
     * @return
     */
    public ServerResponse<String> cancel(Integer userId,Long orderNo){
        // 从数据库中查看 用户 id 的 + 订单号 no 的订单
        Order order  = orderMapper.selectByUserIdAndOrderNo(userId,orderNo);
        if(order == null){
            return ServerResponse.createByErrorMessage("该用户此订单不存在");
        }

        if(order.getStatus() != Const.OrderStatusEnum.NO_PAY.getCode()){
            return ServerResponse.createByErrorMessage("已付款,无法取消订单");
        }
        Order updateOrder = new Order();
        updateOrder.setId(order.getId());
        updateOrder.setStatus(Const.OrderStatusEnum.CANCLED.getCode());

        int row = orderMapper.updateByPrimaryKeySelective(updateOrder);
        if(row > 0){
            return ServerResponse.createBySuccess();
        }
        return ServerResponse.createByError();
    }


    /**
     * 获取购物车中已经选中的商品的商品详情
     *
     * @param userId
     * @return
     */
    public ServerResponse getOrderCartProduct(Integer userId){
        OrderProductVo orderProductVo = new OrderProductVo();
        //从购物车中获取数据，获取这个用户的购物车信息
        List<Cart> cartList = cartMapper.selectCheckedCartByUserId(userId);
        // 获取购物车中的 商品信息
        ServerResponse serverResponse =  this.getCartOrderItem(userId,cartList);
        if(!serverResponse.isSuccess()){
            return serverResponse;
        }
        // 获取购物车中的 商品信息 存放到 orderItemList 中
        List<OrderItem> orderItemList =( List<OrderItem> ) serverResponse.getData();

        List<OrderItemVo> orderItemVoList = Lists.newArrayList();

        // 计算一下 购物车 中目前选中商品的总价
        // 初始化一下订单总价
        BigDecimal payment = new BigDecimal("0");
        for(OrderItem orderItem : orderItemList){
            // 本身自己的订单价钱 + 订单现有的订单价钱
            payment = BigDecimalUtil.add(payment.doubleValue(),orderItem.getTotalPrice().doubleValue());
            orderItemVoList.add(assembleOrderItemVo(orderItem));
        }

        orderProductVo.setProductTotalPrice(payment);
        orderProductVo.setOrderItemVoList(orderItemVoList);
        orderProductVo.setImageHost(PropertiesUtil.getProperty("ftp.server.http.prefix"));
        return ServerResponse.createBySuccess(orderProductVo);
    }

    /**
     * 获取订单详情
     *
     * @param userId
     * @param orderNo
     * @return
     */
    public ServerResponse<OrderVo> getOrderDetail(Integer userId,Long orderNo){
        // 从数据库中获取 用户 id 和 订单 id 的订单
        Order order = orderMapper.selectByUserIdAndOrderNo(userId,orderNo);
        if(order != null){
            // 当订单不为空的时候，我们需要获取订单的orderItem的集合
            List<OrderItem> orderItemList = orderItemMapper.getByOrderNoUserId(orderNo,userId);
            // 组装 OrderVo
            OrderVo orderVo = assembleOrderVo(order,orderItemList);
            return ServerResponse.createBySuccess(orderVo);
        }
        return  ServerResponse.createByErrorMessage("没有找到该订单");
    }


    /**
     * 获取订单信息
     * @param userId
     * @param pageNum
     * @param pageSize
     * @return
     */
    public ServerResponse<PageInfo> getOrderList(Integer userId,int pageNum,int pageSize){
        PageHelper.startPage(pageNum,pageSize);
        // 从数据库中按照用户的 id 获取用户的订单信息
        List<Order> orderList = orderMapper.selectByUserId(userId);
        // 将orderList 组装成 orderVoList
        List<OrderVo> orderVoList = assembleOrderVoList(orderList,userId);
        PageInfo pageResult = new PageInfo(orderList);
        pageResult.setList(orderVoList);
        return ServerResponse.createBySuccess(pageResult);
    }


    private List<OrderVo> assembleOrderVoList(List<Order> orderList,Integer userId){
        List<OrderVo> orderVoList = Lists.newArrayList();
        // 遍历我们的订单
        for(Order order : orderList){
            // 订单的明细，需要加载订单的明细，然后放在OrderVo中
            List<OrderItem>  orderItemList = Lists.newArrayList();
            // 这里需要分角色进行查询，管路员和用户
            // 如果是管理员的话，这个userId传递一个null。
            if(userId == null){
                //todo 管理员查询的时候 不需要传userId
                orderItemList = orderItemMapper.getByOrderNo(order.getOrderNo());
            }else{
                orderItemList = orderItemMapper.getByOrderNoUserId(order.getOrderNo(),userId);
            }
            // 组装 orderVo
            OrderVo orderVo = assembleOrderVo(order,orderItemList);
            orderVoList.add(orderVo);
        }
        return orderVoList;
    }


    //backend

    /**
     * 获取订单的信息，需要分页
     * @param pageNum
     * @param pageSize
     * @return
     */
    public ServerResponse<PageInfo> manageList(int pageNum,int pageSize){
        PageHelper.startPage(pageNum,pageSize);
        // 从数据库中获取所有人的订单信息
        List<Order> orderList = orderMapper.selectAllOrder();
        // 这里与前台的组装方法是一样的，按照角色进行区分
        List<OrderVo> orderVoList = this.assembleOrderVoList(orderList,null);
        PageInfo pageResult = new PageInfo(orderList);
        pageResult.setList(orderVoList);
        return ServerResponse.createBySuccess(pageResult);
    }


    public ServerResponse<OrderVo> manageDetail(Long orderNo){
        Order order = orderMapper.selectByOrderNo(orderNo);
        if(order != null){
            List<OrderItem> orderItemList = orderItemMapper.getByOrderNo(orderNo);
            OrderVo orderVo = assembleOrderVo(order,orderItemList);
            return ServerResponse.createBySuccess(orderVo);
        }
        return ServerResponse.createByErrorMessage("订单不存在");
    }



    public ServerResponse<PageInfo> manageSearch(Long orderNo,int pageNum,int pageSize){
        PageHelper.startPage(pageNum,pageSize);
        Order order = orderMapper.selectByOrderNo(orderNo);
        if(order != null){
            List<OrderItem> orderItemList = orderItemMapper.getByOrderNo(orderNo);
            OrderVo orderVo = assembleOrderVo(order,orderItemList);

            PageInfo pageResult = new PageInfo(Lists.newArrayList(order));
            pageResult.setList(Lists.newArrayList(orderVo));
            return ServerResponse.createBySuccess(pageResult);
        }
        return ServerResponse.createByErrorMessage("订单不存在");
    }


    public ServerResponse<String> manageSendGoods(Long orderNo){
        Order order= orderMapper.selectByOrderNo(orderNo);
        if(order != null){
            if(order.getStatus() == Const.OrderStatusEnum.PAID.getCode()){
                order.setStatus(Const.OrderStatusEnum.SHIPPED.getCode());
                order.setSendTime(new Date());
                orderMapper.updateByPrimaryKeySelective(order);
                return ServerResponse.createBySuccess("发货成功");
            }
        }
        return ServerResponse.createByErrorMessage("订单不存在");
    }






    /**
     * 支付
     * @param orderNo 订单号
     * @param userId 用户的id
     * @param path 生成二维码传到哪里的路径
     * @return
     */
    public ServerResponse pay(Long orderNo,Integer userId,String path){
        // 使用map来承载这个对象
        Map<String ,String> resultMap = Maps.newHashMap();

        // 判断这个用户是否有该订单
        Order order = orderMapper.selectByUserIdAndOrderNo(userId,orderNo);
        if(order == null){
            return ServerResponse.createByErrorMessage("该用户没有该订单");
        }
        resultMap.put("orderNo",String.valueOf(order.getOrderNo()));


        /**
         * 生成支付宝订单需要使用的各种参数
         */
        // (必填) 商户网站订单系统中唯一订单号，64个字符以内，只能包含字母、数字、下划线，
        // 需保证商户系统端不能重复，建议通过数据库sequence生成，
        // 订单号
        String outTradeNo = order.getOrderNo().toString();


        // (必填) 订单标题，粗略描述用户的支付目的。如“xxx品牌xxx门店当面付扫码消费”
        String subject = new StringBuilder().append("happymmall扫码支付,订单号:").append(outTradeNo).toString();


        // (必填) 订单总金额，单位为元，不能超过1亿元
        // 如果同时传入了【打折金额】,【不可打折金额】,【订单总金额】三者,则必须满足如下条件:【订单总金额】=【打折金额】+【不可打折金额】
        String totalAmount = order.getPayment().toString();


        // (可选) 订单不可打折金额，可以配合商家平台配置折扣活动，如果酒水不参与打折，则将对应金额填写至此字段
        // 如果该值未传入,但传入了【订单总金额】,【打折金额】,则该值默认为【订单总金额】-【打折金额】
        String undiscountableAmount = "0";



        // 卖家支付宝账号ID，用于支持一个签约账号下支持打款到不同的收款账号，(打款到sellerId对应的支付宝账号)
        // 如果该字段为空，则默认为与支付宝签约的商户的PID，也就是appid对应的PID
        // 这里就使用一个默认的就可以了
        String sellerId = "";

        // 订单描述，可以对交易或商品进行一个详细地描述，比如填写"购买商品2件共15.00元"
        String body = new StringBuilder().append("订单").append(outTradeNo).append("购买商品共").append(totalAmount).append("元").toString();


        // 商户操作员编号，添加此参数可以为商户操作员做销售统计
        // 使用默认
        String operatorId = "test_operator_id";

        // (必填) 商户门店编号，通过门店号和商家后台可以配置精准到门店的折扣信息，详询支付宝技术支持
        // 使用默认，如果连锁店就会用到这个门店编号
        String storeId = "test_store_id";

        // 业务扩展参数，目前可添加由支付宝分配的系统商编号(通过setSysServiceProviderId方法)，详情请咨询支付宝技术支持
        // 不变
        ExtendParams extendParams = new ExtendParams();
        extendParams.setSysServiceProviderId("2088100200300400500");




        // 支付超时，定义为120分钟
        // 不变
        String timeoutExpress = "120m";

        // 商品明细列表，需填写购买商品详细信息，
        List<GoodsDetail> goodsDetailList = new ArrayList<GoodsDetail>();
        // 获取订单下面的item，即订单下面的明细。要根据订单号和用户id获取订单item
        List<OrderItem> orderItemList = orderItemMapper.getByOrderNoUserId(orderNo,userId);

        for(OrderItem orderItem : orderItemList){

            GoodsDetail goods = GoodsDetail.newInstance(orderItem.getProductId().toString(), orderItem.getProductName(),
                    BigDecimalUtil.mul(orderItem.getCurrentUnitPrice().doubleValue(),new Double(100).doubleValue()).longValue(),
                    orderItem.getQuantity());
            goodsDetailList.add(goods);
        }

        // 创建扫码支付请求builder，设置请求参数
        AlipayTradePrecreateRequestBuilder builder = new AlipayTradePrecreateRequestBuilder()
                .setSubject(subject).setTotalAmount(totalAmount).setOutTradeNo(outTradeNo)
                .setUndiscountableAmount(undiscountableAmount).setSellerId(sellerId).setBody(body)
                .setOperatorId(operatorId).setStoreId(storeId).setExtendParams(extendParams)
                .setTimeoutExpress(timeoutExpress)
                .setNotifyUrl(PropertiesUtil.getProperty("alipay.callback.url"))//支付宝服务器主动通知商户服务器里指定的页面http路径,根据需要设置、这里面写的是支付宝中写的回调地址
                .setGoodsDetailList(goodsDetailList);


        AlipayF2FPrecreateResult result = tradeService.tradePrecreate(builder);
        switch (result.getTradeStatus()) {
            case SUCCESS:
                log.info("支付宝预下单成功: )");

                AlipayTradePrecreateResponse response = result.getResponse();
                dumpResponse(response);

                //　判断目录是否存在，如果不存在需要将该路径创建出来
                File folder = new File(path);
                if(!folder.exists()){
                    folder.setWritable(true);
                    folder.mkdirs();
                }

                // 需要修改为运行机器上的路径
                // 细节细节细节，生成二维码，传到服务器上
                String qrPath = String.format(path+"/qr-%s.png",response.getOutTradeNo());
                // 生成一个文件。订单号会替换%s
                String qrFileName = String.format("qr-%s.png",response.getOutTradeNo());
                // 将二维码存储在哪里，这里存储在qrPath中
                ZxingUtils.getQRCodeImge(response.getQrCode(), 256, qrPath);

                // 目标文件的path, 目标文件的文件名：qrFileName
                File targetFile = new File(path,qrFileName);

                try {
                    FTPUtil.uploadFile(Lists.newArrayList(targetFile));
                } catch (Exception e) {
                    log.error("上传二维码异常",e);
                }

                log.info("qrPath:" + qrPath);

                String qrUrl = PropertiesUtil.getProperty("ftp.server.http.prefix")+targetFile.getName();
                resultMap.put("qrUrl",qrUrl);
                return ServerResponse.createBySuccess(resultMap);
            case FAILED:
                log.error("支付宝预下单失败!!!");
                return ServerResponse.createByErrorMessage("支付宝预下单失败!!!");

            case UNKNOWN:
                log.error("系统异常，预下单状态未知!!!");
                return ServerResponse.createByErrorMessage("系统异常，预下单状态未知!!!");

            default:
                log.error("不支持的交易状态，交易返回异常!!!");
                return ServerResponse.createByErrorMessage("不支持的交易状态，交易返回异常!!!");
        }

    }


    // 简单打印应答
    private void dumpResponse(AlipayResponse response) {
        if (response != null) {

            log.info(String.format("code:%s, msg:%s", response.getCode(), response.getMsg()));
            if (StringUtils.isNotEmpty(response.getSubCode()))
                log.info(String.format("subCode:%s, subMsg:%s", response.getSubCode(), response.getSubMsg()));
            log.info("body:" + response.getBody());
        }
    }

    /**
     * 验证支付宝回调的正确性
     *
     * @param params
     * @return
     */
    public ServerResponse aliCallback(Map<String,String> params) {
       // 将 params 中的各个数据拿出来
        Long orderNo = Long.parseLong(params.get("out_trade_no"));
        String tradeNo = params.get("trade_no");
        String tradeStatus = params.get("trade_status");
        // 根据订单号来查询看数据库中该订单是否存在
        Order order = orderMapper.selectByOrderNo(orderNo);
        // 对数据库返回的order进行判断是否为空
        if(order == null)
            return ServerResponse.createByErrorMessage("非商城的订单，回调忽略");
        // 对订单状态进行判断
        if(order.getStatus() >= Const.OrderStatusEnum.PAID.getCode()) {
            return ServerResponse.createBySuccess("支付宝重复调用");
        }
        if(Const.AlipayCallback.TRADE_STATUS_TRADE_SUCCESS.equals(tradeStatus)) {
            // 订单的时间，可以从支付宝的官方文档中获取到支付订单的时间
            order.setPaymentTime(DateTimeUtil.strToDate(params.get("gmt_payment")));
            // 将订单状态置成已经付款
            order.setStatus(Const.OrderStatusEnum.PAID.getCode());
            // 更新订单状态
            orderMapper.updateByPrimaryKeySelective(order);
        }

        // 组装我们的payInfo对象
        PayInfo payInfo = new PayInfo();
        payInfo.setUserId(order.getUserId());
        payInfo.setOrderNo(order.getOrderNo());
        // 支付的平台
        payInfo.setPayPlatform(Const.PayPlatformEnum.ALIPAY.getCode());
        // 支付宝的交易号
        payInfo.setPlatformNumber(tradeNo);
        // 支付宝的交易状态
        payInfo.setPlatformStatus(tradeStatus);

        payInfoMapper.insert(payInfo);
        return ServerResponse.createBySuccess();
    }

    /**
     * 查询订单状态
     * @param userId  用户的 id
     * @param orderNo 订单号
     * @return
     */
    public ServerResponse queryOrderPayStatus(Integer userId,Long orderNo) {
        // 首先要查询这个订单是否存在
        Order order = orderMapper.selectByUserIdAndOrderNo(userId,orderNo);
        if(order == null)
            return ServerResponse.createByErrorMessage("用户没有该订单");
        if(order.getStatus() >= Const.OrderStatusEnum.PAID.getCode()) {
            return ServerResponse.createBySuccess();
        }
        return ServerResponse.createByError();
    }

}
