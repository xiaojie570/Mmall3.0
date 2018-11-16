package com.mmall.service.impl;

import com.google.common.collect.Lists;
import com.mmall.service.IFileService;
import com.mmall.util.FTPUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * Created by lenovo on 2018/10/9.
 */

@Service("iFileService")
public class FileServiceImpl implements IFileService {

    private Logger logger = LoggerFactory.getLogger(FileServiceImpl.class);

    // 上传文件

    /**
     * 1. 首先要获取上传文件的原始名字
     * 2. 获取图片的扩展名字：从图片名字的后面开始获取，获取第一个.之前的
     * 3. 创建存放图片的位置文件
     * 4.
     * 5.
     * @param file 图像的原始信息
     * @param path 图像的地址
     * @return
     */
    public String upload(MultipartFile file,String path) {
        // 获取上传文件的原始名字
        String fileName = file.getOriginalFilename();
        //扩展名
        //abc.jpg
        String fileExtensionName = fileName.substring(fileName.lastIndexOf(".")+1);
        String uploadFileName = UUID.randomUUID().toString()+"."+fileExtensionName;
        logger.info("开始上传文件,上传文件的文件名:{},上传的路径:{},新文件名:{}",fileName,path,uploadFileName);

        // 根据这个路径创建一个文件
        File fileDir = new File(path);
        if(!fileDir.exists()){
            fileDir.setWritable(true);
            fileDir.mkdirs();
        }
        File targetFile = new File(path,uploadFileName);


        try {
            //文件已经上传成功了
            file.transferTo(targetFile);

            //已经上传到ftp服务器上
            FTPUtil.uploadFile(Lists.newArrayList(targetFile));

            // 上传完成后，删除upload文件夹下的内容
            targetFile.delete();
        } catch (IOException e) {
            logger.error("上传文件异常",e);
            return null;
        }
        //A:abc.jpg
        //B:abc.jpg
        return targetFile.getName();
    }
}
