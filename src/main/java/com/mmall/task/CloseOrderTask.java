package com.mmall.task;

import com.mmall.common.Const;
import com.mmall.service.IOrderService;
import com.mmall.util.PropertiesUtil;
import com.mmall.util.RedisShardedPoolUtill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Created by lenovo on 2019/1/14.
 */
@Component
@Slf4j
public class CloseOrderTask {

    @Autowired
    private IOrderService iOrderService;

    @Scheduled(cron = "0 */1 * * * ?") //每个1分钟的整数倍来执行
    public void closeOrderTaskV1() {
        log.info("关闭订单定时任务启动");
        int hour = Integer.parseInt(PropertiesUtil.getProperty("close.order.task.time","2"));
        iOrderService.closeOrder(hour);
        log.info("关闭订单定时任务结束");
    }

    @Scheduled(cron = "0 */1 * * * ?") //每个1分钟的整数倍来执行
    public void closeOrderTaskV2() {
        log.info("关闭订单定时任务启动");
        long  lockTimeout =Long.parseLong(PropertiesUtil.getProperty("lock.timeout","5000"));

        Long setnxResult = RedisShardedPoolUtill.setnx(Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK,String.valueOf(System.currentTimeMillis() + lockTimeout));

        if(setnxResult != null && setnxResult.intValue() == 1) {
            // 如果返回值是1，代表设置成功，获取锁
            closeOrder(Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK);
        } else {
            log.info("没有获取分布式锁：{}",Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK);
        }
        log.info("关闭订单定时任务结束");
    }

    private void closeOrder(String lockName) {
        RedisShardedPoolUtill.expire(lockName,50); // 有效期50秒，防止死锁
        log.info("获取{}, ThreadName:{}", Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK);
        int hour = Integer.parseInt(PropertiesUtil.getProperty("close.order.task.tim","2"));
        iOrderService.closeOrder(hour);
        RedisShardedPoolUtill.del(Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK);
        log.info("释放：{}，ThreadName:{}",Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK);
        log.info("==============================");
    }




}
