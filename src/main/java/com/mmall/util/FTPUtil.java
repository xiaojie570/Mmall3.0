package com.mmall.util;

import org.apache.commons.net.ftp.FTPClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

/**
 * Created by lenovo on 2018/10/9.
 */
public class FTPUtil {

    private static  final Logger logger = LoggerFactory.getLogger(FTPUtil.class);

    private static String ftpIp = PropertiesUtil.getProperty("ftp.server.ip");
    private static String ftpUser = PropertiesUtil.getProperty("ftp.user");
    private static String ftpPass = PropertiesUtil.getProperty("ftp.pass");

    public FTPUtil(String ip,int port,String user,String pwd){
        this.ip = ip;
        this.port = port;
        this.user = user;
        this.pwd = pwd;
    }

    /**
     * 往图片服务器上上传图片文件
     * 1.
     * @param fileList
     * @return
     * @throws IOException
     */
    public static boolean uploadFile(List<File> fileList) throws IOException {
        FTPUtil ftpUtil = new FTPUtil(ftpIp,21,ftpUser,ftpPass);
        logger.info("开始连接ftp服务器");
        // 传到FTP服务器的image文件夹下
        boolean result = ftpUtil.uploadFile("image",fileList);
        logger.info("开始连接ftp服务器,结束上传,上传结果:{}");
        return result;
    }


    /**
     *
     * @param remotePath 远程路径，在linux上是一个文件夹
     * @param fileList 上传的多个文件
     * @return
     * @throws IOException
     */
    private boolean uploadFile(String remotePath,List<File> fileList) throws IOException {
        // 是否上传
        boolean uploaded = true;
        FileInputStream fis = null;
        //连接FTP服务器
        System.out.println(connectServer(this.ip,this.port,this.user,this.pwd) + "===================================");
        if(connectServer(this.ip,this.port,this.user,this.pwd)){
            try {
                //　更改工作目录，需不需要切换文件夹，如果传递的是空就不用切换
                ftpClient.changeWorkingDirectory(remotePath);
                // 设置缓冲区大小，1MB
                ftpClient.setBufferSize(1024);
                // 设置编码模式
                ftpClient.setControlEncoding("UTF-8");
                // 设置fileType，文件类型为二进制类型
                ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
                //
                ftpClient.enterLocalPassiveMode();
                for(File fileItem : fileList){
                    fis = new FileInputStream(fileItem);
                    // 存储文件
                    ftpClient.storeFile(fileItem.getName(),fis);
                }

            } catch (IOException e) {
                logger.error("上传文件异常",e);
                uploaded = false;
                e.printStackTrace();
            } finally {
                fis.close();
                ftpClient.disconnect();
            }
        }
        return uploaded;
    }


    /**
     * 连接ftp服务器
     *
     * @param ip  ip值
     * @param port 端口
     * @param user 用户
     * @param pwd 密码
     * @return 如果连接成功，返回true
     */
    private boolean connectServer(String ip,int port,String user,String pwd){

        boolean isSuccess = false;
        // 创建一个FTPClient（）；
        ftpClient = new FTPClient();
        try {
            // 使用该类来连接这个ip
            ftpClient.connect(ip,port);
            // 把用户和密码传递过来
            isSuccess = ftpClient.login(user,pwd);

        } catch (IOException e) {
            logger.error("连接FTP服务器异常",e);
        }
        return isSuccess;
    }











    private String ip;
    private int port;
    private String user;
    private String pwd;
    private FTPClient ftpClient;

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPwd() {
        return pwd;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd;
    }

    public FTPClient getFtpClient() {
        return ftpClient;
    }

    public void setFtpClient(FTPClient ftpClient) {
        this.ftpClient = ftpClient;
    }
}
