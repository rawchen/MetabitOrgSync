package com.lundong.metabitorgsync.config;

/**
 * @author RawChen
 * @date 2023-03-02 14:52
 */
public class Constants {

	public static TaskQueueExample queue = new TaskQueueExample(1000);

	public static String ACCESS_TOKEN = "";

	// 飞书自建应用 App ID
	public final static String APP_ID_FEISHU = "cli_a4exxxxxxxxxxxxx";

	// 飞书自建应用 App Secret
	public final static String APP_SECRET_FEISHU = "text0BBfJRSr6u8IRQv9Bxxxxxxxxxxx";

	// 飞书自建应用订阅事件 Encrypt Key
	public final static String ENCRYPT_KEY = "nZYEqeSPgTo6TxBOAAxxxxxxxxxxxxxx";

	// 飞书自建应用订阅事件 Verification Token
	public final static String VERIFICATION_TOKEN = "oiVKj9aDXnTanFXGIWnQxxxxxxxxxxxx";

	// 金蝶云星空网址
	public final static String DOMAIN_PORT = "http://192.168.121.129/k3cloud";

	// 增、改 Save
	public final static String KINGDEE_SAVE = "/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.Save.common.kdsvc";

	// 删 Delete
	public final static String KINGDEE_DELETE = "/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.Delete.common.kdsvc";

	// 单据查询 View
	public final static String KINGDEE_VIEW = "/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.View.common.kdsvc";

	// 列表查询 ExecuteBillQuery
	public final static String KINGDEE_QUERY = "/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc";

	// 固定工作组织NUMBER，北京乾象私募基金管理有限公司
	public final static String ORG_NUMBER_WORK = "QXBJ";		// 测试100 正式QXBJ

	// 固定创建组织/使用组织NUMBER，北京乾象私募基金管理有限公司
	public final static String ORG_NUMBER = "QXBJ";				// 测试100 正式QXBJ

	// 固定根部门名称
	public final static String ORG_NAME = "北京乾象私募基金管理有限公司";

}
