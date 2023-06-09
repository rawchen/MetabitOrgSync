package com.lundong.metabitorgsync;

import com.lundong.metabitorgsync.event.CustomServletAdapter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MetabitOrgSyncApplication {

	public static void main(String[] args) {
		SpringApplication.run(MetabitOrgSyncApplication.class, args);
	}

	// 注入扩展实例到 IOC 容器
	@Bean
	public CustomServletAdapter getServletAdapter() {
		return new CustomServletAdapter();
	}

}
