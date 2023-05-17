package com.lundong.metabitorgsync;

import com.lundong.metabitorgsync.config.Constants;
import com.lundong.metabitorgsync.entity.KingdeeUser;
import com.lundong.metabitorgsync.service.UserService;
import com.lundong.metabitorgsync.service.impl.UserServiceImpl;
import com.lundong.metabitorgsync.util.SignUtil;
import com.lundong.metabitorgsync.util.TimeUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class MetabitOrgSyncApplicationTests {

	@Test
	void contextLoads() {
		String s = TimeUtil.timestampToUTC("1677813905");
		System.out.println(s);
	}

	@Test
	void getParentId() {
		String accessToken = SignUtil.getAccessToken(Constants.APP_ID_FEISHU, Constants.APP_SECRET_FEISHU);

		String result = SignUtil.getDepartmentIdAndName(accessToken, "od-b66de0fbb2edb71b7f5b020d675a3e04");
		System.out.println(result);

		UserService userService = new UserServiceImpl();
		List<KingdeeUser> kingdeeUsers = userService.queryUserList();
		for (KingdeeUser kingdeeUser : kingdeeUsers) {
			System.out.println(kingdeeUser);
		}
	}

}
