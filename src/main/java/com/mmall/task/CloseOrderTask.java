package com.mmall.task;

import com.mmall.common.Const;
import com.mmall.service.IOrderService;
import com.mmall.util.PropertiesUtil;
import com.mmall.util.RedisShardedPoolUtill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;

/**
 * Created by lenovo on 2019/1/14.
 */
@Component
@Slf4j
public class CloseOrderTask {

    @Autowired
    private IOrderService iOrderService;

    // 使用tomcat的shutdown关闭的时候，它会先调用 PreDestroy方法，也可以防止死锁问题
    //　但是这种方式防止死锁还是存在死锁问题，因为如果直接ｋｉｌｌ掉tomcat进程的时候，并不会执行这个方法
    @PreDestroy
    public void delLock() {
        RedisShardedPoolUtill.del(Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK);
    }

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
        // 在这一版本中，会出现死锁问题
        RedisShardedPoolUtill.expire(lockName,50); // 有效期50秒，防止死锁
        log.info("获取{}, ThreadName:{}", Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK);
        int hour = Integer.parseInt(PropertiesUtil.getProperty("close.order.task.tim","2"));
        iOrderService.closeOrder(hour);
        RedisShardedPoolUtill.del(Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK);
        log.info("释放：{}，ThreadName:{}",Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK);
        log.info("==============================");
    }




}
