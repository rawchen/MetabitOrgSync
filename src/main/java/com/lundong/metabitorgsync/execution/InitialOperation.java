package com.lundong.metabitorgsync.execution;

import com.lundong.metabitorgsync.service.SystemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Spring Boot启动后自动执行
 *
 * @author RawChen
 * @since 2023-03-07 15:50
 */
@Component
@Order(1)
public class InitialOperation implements CommandLineRunner {

	@Autowired
	private SystemService systemService;

	@Override
	public void run(String... args) throws Exception {
		String resultDept = systemService.initDepartment();
		System.out.println("初始化部门：" + resultDept);
		String resultUser = systemService.initUser();
		System.out.println("初始化用户：" + resultUser);
	}
}
