package com.mmall.service.impl;

import com.mmall.common.Const;
import com.mmall.common.ServerResponse;
import com.mmall.common.TokenCache;
import com.mmall.dao.UserMapper;
import com.mmall.pojo.User;
import com.mmall.service.IUserService;
import com.mmall.util.MD5Util;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.util.UUID;

/**
 * Created by lenovo on 2018/10/8.
 */
@Service("iUserService")
public class UserServiceImpl implements IUserService {
    @Autowired
    private UserMapper userMapper;

    // 登录
    @Override
    public ServerResponse<User> login(String username, String password) {
        // 检查用户名是否存在
        int resultCount = userMapper.checkUsername(username);
        if(resultCount == 0) {
            return ServerResponse.createByErrorMessage("用户名不存在");
        }
        // 密码登录MD5
        String md5password = MD5Util.MD5EncodeUtf8(password);

        // 返回查到的user
        User user = userMapper.selectLogin(username,md5password);

        if(user == null) {
            return ServerResponse.createByErrorMessage("密码错误");
        }

        user.setPassword(StringUtils.EMPTY);
        return ServerResponse.createBySuccessMsg("登录成功",user);
    }

    // 注册
    public ServerResponse<String> register(User user) {
        // 判断该用户名是否已经存在
        ServerResponse validResponse = this.checkValid(user.getUsername(),Const.USERNAME);
        if(!validResponse.isSuccess()) {
            System.out.println("有用户是这个名字");
            return validResponse;
        }

        System.out.println("没有用户是这个名字");
        // 直接用于校验用户名一样的校验方法，多传入一个Const常量值，来告诉Dao传入的是用户名还是邮箱
        // 校验该email是否已经存在
        validResponse = this.checkValid(user.getEmail(),Const.EMAIL);

        if(!validResponse.isSuccess()) {
            return validResponse;
        }

        System.out.println("没有用户是这个邮箱");
        // 设置用户是普通用户
        user.setRole(Const.Role.ROLE_CUSTOMER);
        // MD5加密
        user.setPassword(MD5Util.MD5EncodeUtf8(user.getPassword()));
        // 将用户插入到数据库
        int resultCount = userMapper.insert(user);
        // 如果生效的行数为0， 则返回一个“注册失败”的错误信息
        if(resultCount == 0) {
            return ServerResponse.createByErrorMessage("注册失败");
        }
        return ServerResponse.createBySuccessMsg("注册成功");
    }

    // 检查是否有效
    public ServerResponse<String> checkValid(String str,String type){
        if(StringUtils.isNoneBlank(type)) {
            if(Const.USERNAME.equals(type)) {
                int resultCount = userMapper.checkUsername(str);
                if(resultCount > 0) {
                    return ServerResponse.createByErrorMessage("用户名已经存在了");
                }
            }

            if(Const.EMAIL.equals(type)) {
                int resultCount = userMapper.checkEmail(str);

                if(resultCount > 0) {
                    return ServerResponse.createByErrorMessage("email已经存在！");
                }
            }
        }else {
            return ServerResponse.createByErrorMessage("参数错误");
        }
        return ServerResponse.createBySuccessMsg("校验成功");
    }

    public ServerResponse selectQuestion(String username) {
        ServerResponse validResponse = this.checkValid(username,Const.USERNAME);
        if(validResponse.isSuccess()) {
            // 用户不存在
            return ServerResponse.createByErrorMessage("用户不存在");
        }
        // 根据用户名查找用户忘记密码的问题
        String question = userMapper.selectQuestionByUserName(username);
        if(StringUtils.isNoneBlank(question)) {
            return ServerResponse.createBySuccess(question);
        }

        return ServerResponse.createByErrorMessage("找回密码的问题是空的");
    }

    /**
     * 1. 首先检查用户名，忘记密码的问题，忘记密码的答案
     * 2. 通过UUID来生成一个forgetToken
     * 3. 调用Guava的TokenCache，将token写入guava中
     *
     * @param username
     * @param question
     * @param answer
     * @return
     */
    // 使用本地缓存来检查问题答案 使用guava本地缓存  mapper中对于多个参数返回值设置为map，
    public ServerResponse<String> checkAnswer(String username,String question,String answer) {
        // 这个方法直接查返回值数量就可以了
        int resultCount = userMapper.checkAnswer(username,question,answer);
        if(resultCount > 0) {
            // 说明问题及问题答案是这个用户的,并且是正确的
            // 声明一个token，这个token使用的是UUID来实现的
            String forgetToken = UUID.randomUUID().toString();
            // 调用刚刚写的TokenCache
            TokenCache.setKey(TokenCache.TOKEN_PREFIX + username,forgetToken);
            return ServerResponse.createBySuccess(forgetToken);
        }
        return ServerResponse.createByErrorMessage("问题的答案错误");
    }


    /**
     * 1. 首先判断token是否传递了，使用StringUtils的isBlank方法
     * 2. 校验用户名是否是正确的
     * 3. 从guava中获取token，同时还需要对cache中的token做是否为空的判断
     * 4. 判断token是否无效或者过期
     * 5. 开始修改密码 ， 密码要先加密MD5才能进行更改
     *
     * @param username  用户名
     * @param passwordNew 新密码
     * @param forgetToken 忘记密码的token
     * @return
     */
    public ServerResponse<String> forgetRestPassword(String username,String passwordNew,String forgetToken) {
        System.out.println(forgetToken + "........");
        if(StringUtils.isBlank(forgetToken)) {
            return ServerResponse.createByErrorMessage("参数错误，token需要传递");
        }
        ServerResponse validResponse = this.checkValid(username,Const.USERNAME);
        if(validResponse.isSuccess()) {
            // 用户不存在
            return ServerResponse.createByErrorMessage("用户不存在");
        }
        // 从guava的cache中获取token。
        String token = TokenCache.getKey(TokenCache.TOKEN_PREFIX + username);
        if(StringUtils.isBlank(token)) {
            return ServerResponse.createByErrorMessage("token无效或者过期");
        }
        if(StringUtils.equals(forgetToken,token)) {
            // 更新密码之前需要使用MD5进行加密
            String md5Password = MD5Util.MD5EncodeUtf8(passwordNew);
            // 通过用户名来更新密码
            int rowCount = userMapper.updatePasswordByUsername(username,md5Password);
            if(rowCount > 0) {
                return ServerResponse.createBySuccessMsg("修改密码成功");
            }
        } else {
            return ServerResponse.createByErrorMessage("token错误，请重新获取重置密码的token");
        }
        return ServerResponse.createByErrorMessage("修改密码失败");
    }


    /**
     * 在用户登录的状态下更新用户密码
     * 1. 防止横向越权，要校验一下这个用户的旧密码。一定要指定是这个用户的旧密码。因为我们会查询的是一个count(1)，所以一定要指定用户id
     * 2. 通过用户的id来判断用户的旧密码是否正确
     * 3. 如果旧密码正确。将新密码进行加密，然后将新密码set到用户密码中，然后将用户更新到数据库中
     *
     * @param passwordOld
     * @param passwordNew
     * @param user
     * @return
     */
    public ServerResponse<String> resetPassword(String passwordOld, String passwordNew,User user) {
        // 防止横向越权，要校验一下这个用户的旧密码。一定要指定是这个用户，因为我们会查询一个count(1）
        passwordOld = MD5Util.MD5EncodeUtf8(passwordOld);
        // 通过用户id来查找用户密码
        int resultCount = userMapper.checkPassword(passwordOld,user.getId());
        if(resultCount == 0)
            return ServerResponse.createByErrorMessage("旧密码错误");
        // 对新密码进行加密，然后存入到user中
        user.setPassword(MD5Util.MD5EncodeUtf8(passwordNew));
        // 将用户更新到数据库中
        // updateByPrimaryKeySelective：是选择性的更新，哪个if中的值不为空，就更新哪个属性
        int updateCount = userMapper.updateByPrimaryKeySelective(user);
        if(updateCount > 0)
            return ServerResponse.createByErrorMessage("密码更新成功");
        return ServerResponse.createByErrorMessage("密码更新失败");
    }

    /**
     * 更新用户的个人信息
     * 1. 首先用户名字不能更新，在更新email的时候，要进行一个email的校验 ，校验新的email是不是已经存在了，如果存在的email且email不是当前用户的email，就说明这个email已经被其他用户使用了的。就不可以进行更新。
     * 2. 将个人信息存储到新的User中
     * @param user
     * @return
     */
    public ServerResponse<User> updateInformation(User user) {
        // username不能被更新
        // email也要进行一个校验，校验新的email是不是已经存在，并且存在的email如果相同的话，不能是我们当前的这个用户的。
        int resultCount = userMapper.checkEmailByUserId(user.getEmail(),user.getId());
        if(resultCount > 0 ){
            return ServerResponse.createByErrorMessage("email已经存在，请更换email再尝试更新");
        }
        // 将个人信息存储到新的User中
        User updateUser = new User();
        updateUser.setId(user.getId());
        updateUser.setEmail(user.getEmail());
        updateUser.setPhone(user.getPhone());
        updateUser.setQuestion(user.getQuestion());
        updateUser.setAnswer(user.getAnswer());

        // 将新的user更新的数据库中
        int updateCount = userMapper.updateByPrimaryKeySelective(updateUser);
        if(updateCount > 0)
            return ServerResponse.createBySuccessMsg("更新个人信息成功",updateUser);
        return ServerResponse.createByErrorMessage("更新个人信息失败");
    }

    /**
     * 获取用户个人详细信息
     * 1. 从数据库中通过主键获取用户的信息
     * 2. 如果用户等于null。则返回一个错误：找不到当前用户
     * 3. 如果用户存在的话，需要先将密码置空，然后将结果返回到前台
     * @param userId
     * @return
     */
    public ServerResponse<User> getInformation(Integer userId) {

        User user = userMapper.selectByPrimaryKey(userId);
        if(user == null)
            return ServerResponse.createByErrorMessage("找不到当前用户");
        user.setPassword(StringUtils.EMPTY);
        return ServerResponse.createBySuccess(user);
    }

    // backend
    // 校验是否是管理员
    public ServerResponse checkAdminRole(User user) {
        if(user != null && user.getRole().equals(Const.Role.ROLE_ADMIN)) {
            return ServerResponse.createBySuccess();
        }
        return ServerResponse.createByError();
    }
}
