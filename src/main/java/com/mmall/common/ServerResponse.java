package com.mmall.common;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import java.io.Serializable;

/**
 * 服务端响应对象
 * Created by lenovo on 2018/10/8.
 */
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
// 保证序列化json的时候，如果是null的对象，可以就会消失
public class ServerResponse<T> implements Serializable {
    private int status;
    private String msg;
    private T data;

    // 写私有的构造方法
    private ServerResponse(int status) {
        this.status = status;
    }
    private ServerResponse(int status,T data) {
        this.status = status;
        this.data = data;
    }
    private ServerResponse(int status,String msg) {
        this.status = status;
        this.msg = msg;
    }
    private ServerResponse(int status,String msg,T data) {
        this.status = status;
        this.msg = msg;
        this.data = data;
    }

    @JsonIgnore
    // 使之不再json序列化结果当中
    public boolean isSuccess() {
        return this.status == ResponseCode.SUCCESS.getCode();
    }

    public int getStatus() {
        return status;
    }
    public T getData() {
        return data;
    }
    public String getMsg() {
        return msg;
    }

    // 只返回成功的code值
    public static<T> ServerResponse<T> createBySuccess(){
        return new ServerResponse<T>(ResponseCode.SUCCESS.getCode());
    }

    //
    public static<T> ServerResponse<T> createBySuccessMsg(String msg){
        return new ServerResponse<T>(ResponseCode.SUCCESS.getCode(),msg);
    }

    // 创建一个成功的返回，将成功的数据返回
    public static<T> ServerResponse<T> createBySuccess(T data){
        return new ServerResponse<T>(ResponseCode.SUCCESS.getCode(),data);
    }

    // 传递消息和数据
    public static<T> ServerResponse<T> createBySuccessMsg(String msg,T data){
        return new ServerResponse<T>(ResponseCode.SUCCESS.getCode(),msg,data);
    }

    public static<T> ServerResponse<T> createByError(){
        return new ServerResponse<T>(ResponseCode.ERROR.getCode(),ResponseCode.ERROR.getDesc());
    }

    public static<T> ServerResponse<T> createByErrorMessage(String errprMessage){
        return new ServerResponse<T>(ResponseCode.ERROR.getCode(),errprMessage);
    }

    public static<T> ServerResponse<T> createByErrorCodeMessage(int errorCode,String errorMessage){
        return new ServerResponse<T>(errorCode,errorMessage);
    }
}
