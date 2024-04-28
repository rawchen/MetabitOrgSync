package com.lundong.metabitorgsync.execution;

import cn.hutool.core.util.StrUtil;
import com.lundong.metabitorgsync.config.Constants;
import com.lundong.metabitorgsync.util.SignUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Spring Boot定时任务
 *
 * @author RawChen
 * @date 2023-12-03 17:21
 */
@Slf4j
@Component
@EnableScheduling
public class ScheduleTask {

    /**
     * 每隔10分钟刷新一个token
     */
    @Scheduled(initialDelay = 10 * 60 * 1000, fixedRate = 10 * 60 * 1000)
    private void scheduleRefreshToken() {
        log.info("重新获得一个tenant_access_token");
        String accessToken = SignUtil.getAccessToken(Constants.APP_ID_FEISHU, Constants.APP_SECRET_FEISHU);
        if (!StrUtil.isEmpty(accessToken)) {
            Constants.ACCESS_TOKEN = accessToken;
        }
    }
}
