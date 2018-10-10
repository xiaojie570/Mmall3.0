package com.mmall.service;

import com.mmall.common.ServerResponse;

/**
 * Created by lenovo on 2018/10/10.
 */
public interface ICartService {

    ServerResponse add(Integer userId, Integer productId, Integer count);
}
