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
    public String upload(MultipartFile file,String path) {
        String fileName = file.getOriginalFilename();
        //获取扩展名，从后面开始获取
        // abc.jpg   substring从后面获取第一个“.”
        String fileExtendsionName = fileName.substring(fileName.lastIndexOf(".") + 1);
        // 防止多个人传输的文件的名字一样，因此在原来的名字前面加上UUID，保证不重复
        String uploadFileName = UUID.randomUUID().toString() + "." + fileExtendsionName;
        logger.info("开始上传文件，上传文件的文件名字：{}，上传的路径：{}，新文件名：{}",fileName,path,uploadFileName);

        // 声明目录的file
        File fileDir = new File(path);
        // 判断文件夹是否存在
        if(!fileDir.exists()) {
            fileDir.setWritable(true);
            fileDir.mkdirs();
        }
        File targetFile = new File(path,uploadFileName);
        try {
            file.transferTo(targetFile);
            // 文件已经上传成功
            // todo  将targetFile上传到我们的FTP服务器上
            FTPUtil.uploadFile(Lists.<File>newArrayList(targetFile));

            // 已经长传到ftp服务器上
            // todo 上传完成之后，删除upload下面的文件
            targetFile.delete();
        } catch (IOException e) {
            logger.error("上传文件异常",e);
            return null;
        }
        // 目标文件的文件名字
        return targetFile.getName();
    }
}
