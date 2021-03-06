package com.mmall.task;

import com.mmall.common.Const;
import com.mmall.common.RedissonManager;
import com.mmall.service.IOrderService;
import com.mmall.util.PropertiesUtil;
import com.mmall.util.RedisShardedPoolUtill;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.concurrent.TimeUnit;

/**
 * Created by lenovo on 2019/1/14.
 */
@Component
@Slf4j
public class CloseOrderTask {

    @Autowired
    private IOrderService iOrderService;


    @Autowired
    private RedissonManager redissonManager;

    // 使用tomcat的shutdown关闭的时候，它会先调用 PreDestroy方法，也可以防止死锁问题
    //　但是这种方式防止死锁还是存在死锁问题，因为如果直接ｋｉｌｌ掉tomcat进程的时候，并不会执行这个方法
    @PreDestroy
    public void delLock() {
        RedisShardedPoolUtill.del(Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK);
    }

   /* @Scheduled(cron = "0 *//*1 * * * ?") //每个1分钟的整数倍来执行
    public void closeOrderTaskV1() {
        log.info("关闭订单定时任务启动");
        int hour = Integer.parseInt(PropertiesUtil.getProperty("close.order.task.time","2"));
        iOrderService.closeOrder(hour);
        log.info("关闭订单定时任务结束");
    }*/

   /* @Scheduled(cron = "0 *//*1 * * * ?") //每个1分钟的整数倍来执行
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
    }*/

   /* @Scheduled(cron = "0 *//*1 * * * ?") //每个1分钟的整数倍来执行
   // 原生实现的分布式锁。很好的方法要理解。
    public void closeOrderTaskV3() {
        log.info("关闭订单定时任务启动");
        long  lockTimeout =Long.parseLong(PropertiesUtil.getProperty("lock.timeout","5000"));

        Long setnxResult = RedisShardedPoolUtill.setnx(Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK,String.valueOf(System.currentTimeMillis() + lockTimeout));

        if(setnxResult != null && setnxResult.intValue() == 1) {
            // 如果返回值是1，代表设置成功，获取锁
            closeOrder(Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK);
        } else {
            // 未获取到锁，继续判断，判断时间戳，看是否可以重置并获取到锁
            String lockValueStr = RedisShardedPoolUtill.get(Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK);
            if(lockValueStr != null && System.currentTimeMillis() > Long.parseLong(lockValueStr)) {
                String getSetResult = RedisShardedPoolUtill.getset(Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK,String.valueOf(System.currentTimeMillis()));
                // 再次用当前时间戳 getset
                // 返回给定的key的旧值， --> 旧值判断，是否可以获取锁
                // 当key没有旧值的时候，即key不存在的时候，返回nil  -> 获取锁
                // 这里我们set了一个新的value值，获取旧的值
                if(getSetResult == null || (getSetResult != null && StringUtils.equals(lockValueStr,getSetResult))) {
                    // 真正获取到了锁
                    closeOrder(Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK);
                } else  {
                    log.info("没有获取到分布式锁：{}",Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK);
                }
            } else  {
                log.info("没有获取到分布式锁：{}",Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK);
            }
        }
        log.info("关闭订单定时任务结束");
    }*/


    @Scheduled(cron = "0 */1 * * * ?") //每个1分钟的整数倍来执行
    public void closeOrderTaskV4() {
        // 先获取Redisson的锁
        RLock rLock = redissonManager.getRedisson().getLock(Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK);
        // 是否获取锁成功
        boolean getLock = false;

        try {
            // 尝试获取锁（等待锁的时间，锁的自动释放锁的时间，时间的单位）
            // 这里需要将等待锁的时间(wait-time)设置为：0，否则会有一个小 bug：这个bug就是两个tomcat同时会获取到分布式锁
            // 同时设置为 0 就不需要预估处理关闭订单的时候所花费的时间。
            if(getLock = rLock.tryLock(0,50, TimeUnit.SECONDS)) {
                log.info("Redisson 获取到分布式锁：{},ThreadName:{}",Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK,Thread.currentThread().getName());
                // 获取小时
                int hour = Integer.parseInt(PropertiesUtil.getProperty("close.order.task.tim","2"));
                iOrderService.closeOrder(hour);
            } else {
                log.info("Redisson 没有获取到分布式锁：{},ThreadName:{}",Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK,Thread.currentThread().getName());
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            log.error("Redisson 分布式锁获取异常",e);
        } finally {
            if(!getLock) {
                return;
            }
            rLock.unlock();
            log.info("Redisson 分布式锁释放锁");
        }

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
