package com.lundong.metabitorgsync.controller;

import com.lundong.metabitorgsync.service.SystemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
public class SystemController {

    @Autowired
    private SystemService systemService;

    /**
     * 初始化部门
     *
     * @return
     */
    @GetMapping("/init/department")
    public String initDepartment() {
        return systemService.initDepartment();
    }

    /**
     * 初始化用户
     *
     * @return
     */
    @GetMapping("/init/user")
    public String initUser() {
        return systemService.initUser();
    }
}
