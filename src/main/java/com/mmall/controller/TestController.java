package com.mmall.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Created by lenovo on 2018/10/22.
 */

@Controller
public class TestController {

    @RequestMapping("test.do")
    @ResponseBody
    public String test(String str) {
        Logger logger = LoggerFactory.getLogger(TestController.class);
        logger.info("testinfo");
        logger.warn("testwarn");
        logger.error("testerror");
        return "testValue:" + str;
    }
}
