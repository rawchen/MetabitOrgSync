package com.lundong.metabitorgsync.execution;

import com.lundong.metabitorgsync.config.Constants;
import com.lundong.metabitorgsync.service.SystemService;
import com.lundong.metabitorgsync.util.SignUtil;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@Component
@Order(1)
public class InitialOperation implements CommandLineRunner {

	@Autowired
	private SystemService systemService;

	@Override
	public void run(String... args) throws Exception {
		System.out.println("初始化部门：" + "Begin...");
		String resultDept = systemService.initDepartment();
		System.out.println("初始化部门：" + resultDept);
		System.out.println("初始化用户：" + "Begin...");
		String resultUser = systemService.initUser();
		System.out.println("初始化用户：" + resultUser);
		// 初始化ACCESS_TOKEN
		Constants.ACCESS_TOKEN = SignUtil.getAccessToken(Constants.APP_ID_FEISHU, Constants.APP_SECRET_FEISHU);
		log.info("初始化ACCESS_TOKEN: {}", Constants.ACCESS_TOKEN);

		Constants.queue.startProcessingTasks();
		log.info("启动初始化任务队列");
	}
}
