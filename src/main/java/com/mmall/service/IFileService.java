package com.mmall.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * Created by lenovo on 2018/10/9.
 */
public interface IFileService {

    String upload(MultipartFile file, String path);
}
