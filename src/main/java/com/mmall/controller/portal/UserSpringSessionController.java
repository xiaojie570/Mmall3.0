package com.mmall.controller.portal;

import com.mmall.common.Const;
import com.mmall.common.ResponseCode;
import com.mmall.common.ServerResponse;
import com.mmall.pojo.User;
import com.mmall.service.IUserService;
import com.mmall.util.CookieUtil;
import com.mmall.util.JsonUtil;
import com.mmall.util.RedisShardedPoolUtill;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Created by lenovo on 2018/10/8.
 */

@Controller
@RequestMapping("/user/springsession/")
public class UserSpringSessionController {

    @Autowired
    private IUserService iUserService;
    /**
     * 用户登录
     * @param username
     * @param password
     * @param session
     * @return
     */
    @RequestMapping(value="login.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<User> login(String username,
                                      String password,
                                      HttpSession session,
                                      HttpServletResponse httpServletResponse,
                                      HttpServletRequest httpServletRequest) {

        // 测试全局异常
        int i = 0;
        int j = 666 / i;

        ServerResponse<User> response = iUserService.login(username,password);
        // 如果返回值是正确的，则将用户信息存储在redis中
        if(response.isSuccess()) {
            session.setAttribute(Const.CURRENT_USER,response.getData());
            /*CookieUtil.writeLoginToken(httpServletResponse,session.getId());
            RedisShardedPoolUtill.setEx(session.getId(),Const.RedisCacheExtime.REDIS_SESSION_EXTIME, JsonUtil.Obj2String(response.getData()));*/
        }

        // 将response返回
        return response;
    }

    @RequestMapping(value="logout.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> logout(HttpSession session,
                                        HttpServletRequest httpServletRequest,
                                        HttpServletResponse httpServletResponse) {
        session.removeAttribute(Const.CURRENT_USER);
        /*String loginToken = CookieUtil.readLoginToken(httpServletRequest);
        RedisShardedPoolUtill.del(loginToken);
        CookieUtil.delLoginToken(httpServletRequest,httpServletResponse);*/

        return ServerResponse.createBySuccess();
    }




    /**
     * 获取用户信息
     * @param session
     * @return
     */
    @RequestMapping(value="get_user_info.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<User> getUserInfo(HttpSession session, HttpServletRequest httpServletRequest) {

        /*String loginToken = CookieUtil.readLoginToken(httpServletRequest);
        if(StringUtils.isEmpty(loginToken)) {
            return ServerResponse.createByErrorMessage("用户未登录，无法获取当前用户信息");
        }
        String userJson = RedisShardedPoolUtill.get(loginToken);
        User user = JsonUtil.String2Obj(userJson,User.class);*/

        User user = (User) session.getAttribute(Const.CURRENT_USER);

        if(user != null)
            return ServerResponse.createBySuccess(user);
        return ServerResponse.createByErrorMessage("用户未登录，无法获取当前用户信息");
    }


}
