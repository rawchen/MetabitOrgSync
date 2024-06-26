package com.lundong.metabitorgsync.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kingdee.bos.webapi.sdk.K3CloudApi;
import com.lark.oapi.core.utils.Jsons;
import com.lark.oapi.service.contact.v3.ContactService;
import com.lark.oapi.service.contact.v3.model.*;
import com.lundong.metabitorgsync.config.Constants;
import com.lundong.metabitorgsync.entity.Department;
import com.lundong.metabitorgsync.entity.User;
import com.lundong.metabitorgsync.entity.*;
import com.lundong.metabitorgsync.event.CustomEventDispatcher;
import com.lundong.metabitorgsync.event.CustomServletAdapter;
import com.lundong.metabitorgsync.mapper.DepartmentMapper;
import com.lundong.metabitorgsync.mapper.UserMapper;
import com.lundong.metabitorgsync.service.DeptService;
import com.lundong.metabitorgsync.service.OrgPostService;
import com.lundong.metabitorgsync.service.UserService;
import com.lundong.metabitorgsync.util.SignUtil;
import com.lundong.metabitorgsync.util.StringUtil;
import com.lundong.metabitorgsync.util.TimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * @author RawChen
 * @date 2023-03-02 11:19
 */
@Slf4j
@RestController
@RequestMapping
public class EventController {

    @Autowired
    private CustomServletAdapter servletAdapter;

    @Autowired
    DepartmentMapper departmentMapper;

    @Autowired
    UserMapper userMapper;

    @Autowired
    private DeptService deptService;

	@Autowired
	private UserService userService;

    @Autowired
    private OrgPostService orgPostService;

	private final K3CloudApi api = new K3CloudApi();

    // 注册消息处理器
    private final CustomEventDispatcher EVENT_DISPATCHER = CustomEventDispatcher
            .newBuilder(Constants.VERIFICATION_TOKEN, Constants.ENCRYPT_KEY)
//			.onCorehrOffboardingUpdatedV1(new CorehrOffboardingUpdatedV1Handler() {
//				// 离职状态变更
//				@Override
//				public void handle(CorehrOffboardingUpdatedEvent event) throws Exception {
//					log.info("CorehrOffboardingUpdated: {}", Jsons.DEFAULT.toJson(event));
//					String resultJson = Jsons.DEFAULT.toJson(event);
//					JSONObject eventsObject = (JSONObject) JSONObject.parse(resultJson);
//					JSONObject eventObject = (JSONObject) eventsObject.get("event");
//					Integer status = eventObject.getInteger("status");
//
//					String userId = "";
//					if (status != null && status == 5) {
//						JSONObject targetUserId = eventObject.getJSONObject("target_user_id");
//						if (targetUserId != null) {
//							userId = targetUserId.getString("user_id");
//							log.info("userId: {} processId: {} offboardingId: {}", userId, eventObject.getString("process_id"), eventsObject.getString("offboarding_id"));
//						}
//					}
//
//					User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUserId, userId).last("limit 1"));
//					if (user == null) {
//						log.info("CorehrOffboardingUpdatedV1事件：用户映射表中根据user_id获取不到记录: {}", userId);
//						return;
//					}
//
//					// 根据离职状态变更的 process_id
//					// 调用搜索离职信息
//					// offboarding_checklist -> checklist_status == Finished 完成办理
//					// 如果该事件雇员查询到离职审批为完成办理，就执行
//					String employmentId = eventObject.getString("employment_id");
//					boolean result = SignUtil.corehrOffboardingsSearch(employmentId);
//					if (result) {
//						// 改为如果是审核了就反审核，并且禁用
//						String unAuditEmpinfoResult = api.unAudit("BD_Empinfo", "{\"Ids\":\"" + user.getFid() + "\"}");
//						JSONObject unAuditEmpinfoObject = JSONObject.parseObject(unAuditEmpinfoResult);
//						JSONObject unAuditResultObject = (JSONObject) unAuditEmpinfoObject.get("Result");
//						JSONObject unAuditResponseStatus = (JSONObject) unAuditResultObject.get("ResponseStatus");
//
//						String forbidEmpinfoResult = api.excuteOperation("BD_Empinfo", "Forbid", "{\"Ids\":\"" + user.getFid() + "\"}");
//						JSONObject forbidEmpinfoObject = JSONObject.parseObject(forbidEmpinfoResult);
//						JSONObject forbidResultObject = (JSONObject) forbidEmpinfoObject.get("Result");
//						JSONObject forbidResponseStatus = (JSONObject) forbidResultObject.get("ResponseStatus");
//
//						// 映射表删除用户
//						if (forbidResponseStatus.getBoolean("IsSuccess")) {
//							userMapper.deleteById(user.getId());
//							log.info("deleteEmpinfo success: {}", forbidResponseStatus);
//						} else {
//							log.info("deleteEmpinfo fail: {}", forbidResponseStatus.getJSONArray("Errors").toJSONString());
//						}
//					}
//				}
//			})
            .onP2UserCreatedV3(new ContactService.P2UserCreatedV3Handler() {
                // 用户创建
                @Override
                public void handle(P2UserCreatedV3 event) throws Exception {
	                Runnable worker = () -> {
						log.info("P2UserCreatedV3: {}", Jsons.DEFAULT.toJson(event));
						// 处理用户创建事件
						// 1.获取处理订阅消息体
						String resultJson = Jsons.DEFAULT.toJson(event);
						JSONObject eventsObject = (JSONObject) JSONObject.parse(resultJson);
						JSONObject eventObject = (JSONObject) eventsObject.get("event");
						JSONObject object = (JSONObject) eventObject.get("object");
						String user_id = object.getString("user_id");
						String name = object.getString("name");
						String mobile = object.getString("mobile");
						JSONArray department_ids = (JSONArray) object.get("department_ids");
						String employee_no = object.getString("employee_no");
						String join_time = object.getString("join_time");
						String job_title = object.getString("job_title");
	//                    String email = object.getString("email");
	//                    String gender = object.getString("gender");
	//                    String city = object.getString("city");
						Integer employee_type = object.getInteger("employee_type");
	//					System.out.println("employee_type: " + employee_type);
						// 排除外包
						if (employee_type != 3) {
	//                    String nickname = object.getString("nickname");
	//                    String leader_user_id = object.getString("leader_user_id");

							// 判断这个用户在映射表是否已经存在（防止事件流重复订阅）
							User userTemp = userMapper.selectOne(new LambdaQueryWrapper<User>()
									.eq(User::getUserId, user_id).last("limit 1"));

							if (userTemp != null) {
								log.info("P2UserCreatedV3: 添加失败，重复添加用户：" + name);
								return;
							}

							String kingdeeDeptId = "";
							String kingdeeDeptNumber = "";

							// 拿到飞书-Kingdee映射的部门id
							String deptIdAndName = SignUtil.getDepartmentIdAndName(department_ids.getString(0));
							String deptId = "";
							if (deptIdAndName.contains(",")) {
								String[] split = deptIdAndName.split(",");
								deptId = split[0];
							}
							Department department = departmentMapper.selectOne(new LambdaQueryWrapper<Department>().eq(Department::getFeishuDeptId, deptId).last("limit 1"));
							if (department == null) {
								log.info("P2UserCreatedV3：部门映射表中根据deptId获取不到记录: {}", deptId);
								return;
							}
							if (department.getKingdeeDeptId() != null) {
								kingdeeDeptId = department.getKingdeeDeptId();
								kingdeeDeptNumber = department.getNumber();
							} else {
								kingdeeDeptId = "0";
								kingdeeDeptNumber = "0";
							}
							String orgPostNumber = "0";
							try {
								// 根据所属部门作为过滤条件，调用金蝶查询就任岗位列表接口，
								// 列表循环匹配名称找对应岗位number，如果匹配不到就新增岗位，取岗位number
								List<KingdeeOrgPost> kingdeeOrgPosts = orgPostService.queryOrgPostList("FDEPTID='" + kingdeeDeptId + "'");
								if (kingdeeOrgPosts != null && kingdeeOrgPosts.size() > 0) {
									for (KingdeeOrgPost orgPost : kingdeeOrgPosts) {
										if (job_title.equalsIgnoreCase(orgPost.getName())) {
											orgPostNumber = orgPost.getNumber();
											break;
										}
									}
								}
								// 如果根据部门没有找到至少一个岗位信息就新增一个岗位
								if ("0".equals(orgPostNumber)) {
									String saveOrgPostData = "{\"NeedUpDateFields\":[],\"NeedReturnFields\":[]," +
											"\"IsDeleteEntry\":\"true\",\"SubSystemId\":\"\",\"IsVerifyBaseDataField\":\"false\"," +
											"\"IsEntryBatchFill\":\"true\",\"ValidateFlag\":\"true\",\"NumberSearch\":\"true\"," +
											"\"IsAutoAdjustField\":\"false\",\"InterationFlags\":\"\",\"IgnoreInterationFlag\":\"\"," +
											"\"IsControlPrecision\":\"false\",\"ValidateRepeatJson\":\"false\"," +
											"\"Model\":{\"FPOSTID\":0,\"FCreateOrgId\":{\"FNumber\":\"创建组织\"},\"FNumber\":\"\"," +
											"\"FUseOrgId\":{\"FNumber\":\"使用组织\"},\"FName\":\"名称\",\"FHelpCode\":\"\"," +
											"\"FDept\":{\"FNumber\":\"所属部门\"},\"FEffectDate\":\"1900-01-01\"," +
											"\"FLapseDate\":\"1900-01-01\",\"FDESCRIPTIONS\":\"\",\"FHRPostSubHead\":{\"FHRPOSTID\":0," +
											"\"FLEADERPOST\":\"false\"},\"FSHRMapEntity\":{\"FMAPID\":0}," +
											"\"FSubReportEntity\":[{\"FSubNumber\":\"\"}]}}";
									saveOrgPostData = saveOrgPostData.replaceAll("所属部门", kingdeeDeptNumber);
									saveOrgPostData = saveOrgPostData.replaceAll("名称", job_title);
									saveOrgPostData = saveOrgPostData.replaceAll("创建组织", Constants.ORG_NUMBER);
									saveOrgPostData = saveOrgPostData.replaceAll("使用组织", Constants.ORG_NUMBER);
									System.out.println("saveOrgPostData: " + saveOrgPostData);
									String saveOrgPostDataResult = api.save("HR_ORG_HRPOST", saveOrgPostData);
									System.out.println("saveOrgPostDataResult: " + saveOrgPostDataResult);
									JSONObject postObject = JSONObject.parseObject(saveOrgPostDataResult);
									JSONObject resultObject = (JSONObject) postObject.get("Result");
									JSONObject responseStatus = (JSONObject) resultObject.get("ResponseStatus");
									if (responseStatus.getBoolean("IsSuccess")) {
										// 请求成功
										orgPostNumber = resultObject.getString("Number");
										// 提交 & 审核
										api.submit("HR_ORG_HRPOST", "{\"Numbers\":\"" + orgPostNumber + "\"}");
										api.audit("HR_ORG_HRPOST", "{\"Numbers\":\"" + orgPostNumber + "\"}");
									}
								}

								// 获取法定姓名，通过搜索员工信息的employee_no查询
								String legalName = SignUtil.getLegalNameByEmployeeNumber(employee_no);

								String staffSaveJson = "{\"NeedUpDateFields\":[],\"NeedReturnFields\":[]," +
										"\"IsDeleteEntry\":\"true\",\"SubSystemId\":\"\",\"IsVerifyBaseDataField\":\"false\"," +
										"\"IsEntryBatchFill\":\"true\",\"ValidateFlag\":\"true\",\"NumberSearch\":\"true\"," +
										"\"IsAutoAdjustField\":\"false\",\"InterationFlags\":\"\",\"IgnoreInterationFlag\":\"\"," +
										"\"IsControlPrecision\":\"false\",\"ValidateRepeatJson\":\"false\"," +
										"\"Model\":{\"FID\":0,\"FName\":\"员工姓名\",\"FStaffNumber\":\"员工编号\",\"FMobile\":\"移动电话\",\"FTel\":\"\"," +
										"\"FEmail\":\"\",\"FDescription\":\"\",\"FAddress\":\"\",\"FCreateOrgId\":{\"FNumber\":\"创建组织\"}," +
										"\"FUseOrgId\":{\"FNumber\":\"使用组织\"},\"FBranchID\":{\"FNUMBER\":\"\"},\"FCreateSaler\":\"false\"," +
										"\"FCreateUser\":\"false\",\"FCreateCashier\":\"false\",\"FCashierGrp\":{\"FNUMBER\":\"\"}," +
										"\"FUserId\":{\"FUSERACCOUNT\":\"\"},\"FCashierId\":{\"FNUMBER\":\"\"},\"FSalerId\":{\"FNUMBER\":\"\"}," +
										"\"FPostId\":{\"FNUMBER\":\"\"},\"FJoinDate\":\"1900-01-01\",\"FUniportalNo\":\"\"," +
										"\"FSHRMapEntity\":{\"FMAPID\":0},\"FPostEntity\":[{\"FENTRYID\":0,\"FWorkOrgId\":{\"FNumber\":\"工作组织\"}," +
										"\"FPostDept\":{\"FNumber\":\"所属部门\"},\"FPost\":{\"FNumber\":\"就任岗位\"},\"FStaffStartDate\":\"任岗开始日期\"" +
										",\"FIsFirstPost\":\"true\",\"FStaffDetails\":0,\"FOperatorType\":\"\"}]," +
										"\"FBankInfo\":[{\"FBankId\":0,\"FBankCountry\":{\"FNumber\":\"\"},\"FBankCode\":\"\"," +
										"\"FBankHolder\":\"\",\"FBankTypeRec\":{\"FNUMBER\":\"\"},\"FTextBankDetail\":\"\"," +
										"\"FBankDetail\":{\"FNUMBER\":\"\"},\"FOpenBankName\":\"\",\"FOpenAddressRec\":\"\"," +
										"\"FCNAPS\":\"\",\"FBankCurrencyId\":{\"FNUMBER\":\"\"},\"FBankIsDefault\":\"false\"," +
										"\"FBankDesc\":\"\",\"FCertType\":\"\",\"FIsFromSHR\":\"false\",\"FCertNum\":\"\"}]}}";
								staffSaveJson = staffSaveJson.replaceAll("员工姓名", legalName);
								staffSaveJson = staffSaveJson.replaceAll("创建组织", Constants.ORG_NUMBER);
								staffSaveJson = staffSaveJson.replaceAll("使用组织", Constants.ORG_NUMBER);
								staffSaveJson = staffSaveJson.replaceAll("员工编号", employee_no);
								staffSaveJson = staffSaveJson.replaceAll("所属部门", kingdeeDeptNumber);
								staffSaveJson = staffSaveJson.replaceAll("就任岗位", orgPostNumber);
								staffSaveJson = staffSaveJson.replaceAll("工作组织", Constants.ORG_NUMBER);
								// 非必填
								staffSaveJson = staffSaveJson.replaceAll("移动电话", StringUtil.mobileDivAreaCode(mobile));
								staffSaveJson = staffSaveJson.replaceAll("任岗开始日期", TimeUtil.timestampToYMD(join_time));
								String saveEmpinfoResult = api.save("BD_Empinfo", staffSaveJson);
								System.out.println("staffSaveJson: " + staffSaveJson);
								JSONObject postObject = JSONObject.parseObject(saveEmpinfoResult);
								JSONObject resultObject = (JSONObject) postObject.get("Result");
								JSONObject responseStatus = (JSONObject) resultObject.get("ResponseStatus");
								if (responseStatus.getBoolean("IsSuccess")) {
									User user = User.builder()
											.fid(resultObject.getString("Id"))
											.name(legalName)
											.userId(user_id)
											.deptId(deptId)
											.build();
									userMapper.insert(user);
									log.info("saveEmpinfo success: {}", saveEmpinfoResult);
									// 提交 & 审核
									api.submit("BD_Empinfo", "{\"Numbers\":\"" + employee_no + "\"}");
									api.audit("BD_Empinfo", "{\"Numbers\":\"" + employee_no + "\"}");
								} else {
									log.info("saveEmpinfo fail: {}", saveEmpinfoResult);
								}
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					};
	                Constants.queue.submitTask(worker);
                }
            }).onP2UserUpdatedV3(new ContactService.P2UserUpdatedV3Handler() {
                // 用户修改
                @Override
                public void handle(P2UserUpdatedV3 event) throws Exception {
	                Runnable worker = () -> {
						log.info("P2UserUpdatedV3: {}", Jsons.DEFAULT.toJson(event));
						String resultJson = Jsons.DEFAULT.toJson(event);
						JSONObject eventsObject = (JSONObject) JSONObject.parse(resultJson);
						JSONObject eventObject = (JSONObject) eventsObject.get("event");
						JSONObject object = (JSONObject) eventObject.get("object");
						String user_id = object.getString("user_id");
						String name = object.getString("name");
						String en_name = object.getString("en_name");
						String mobile = object.getString("mobile");
						String email = object.getString("email");
						JSONArray department_ids = (JSONArray) object.get("department_ids");
						String employee_no = object.getString("employee_no");
						String join_time = object.getString("join_time");
						String job_title = object.getString("job_title");

						String kingdeeDeptId = "";
						String kingdeeDeptNumber = "";

						// 拿到飞书-Kingdee映射的部门id
						String deptIdAndName = SignUtil.getDepartmentIdAndName(department_ids.getString(0));
						String deptId = "";
						if (deptIdAndName.contains(",")) {
							String[] split = deptIdAndName.split(",");
							deptId = split[0];
						}
						Department department = departmentMapper.selectOne(new LambdaQueryWrapper<Department>().eq(Department::getFeishuDeptId, deptId).last("limit 1"));
						if (department == null) {
							log.info("P2UserUpdatedV3事件：部门映射表中根据deptId获取不到记录: {}", deptId);
							return;
						}
						if (department.getKingdeeDeptId() != null) {
							kingdeeDeptId = department.getKingdeeDeptId();
							kingdeeDeptNumber = department.getNumber();
						} else {
							kingdeeDeptId = "0";
							kingdeeDeptNumber = "0";
						}
						String orgPostNumber = "0";

						// 根据飞书user_id查询kingdee单据id（映射好的用户数据库查）
						User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUserId, user_id).last("limit 1"));
						if (user == null) {
							log.info("P2UserUpdatedV3事件：用户映射表中根据user_id获取不到记录: {}", user_id);
							return;
						}
						String fid = "";
						if (user.getFid() != null) {
							fid = user.getFid();
						} else {
							fid = "0";
							log.info("用户修改事件查询不到用户user_id: {}", user_id);
						}
						try {
							// 根据所属部门作为过滤条件，调用金蝶查询就任岗位列表接口，
							// 列表循环匹配名称找对应岗位number，如果匹配不到就新增岗位，取岗位number
							List<KingdeeOrgPost> kingdeeOrgPosts = orgPostService.queryOrgPostList("FDEPTID='" + kingdeeDeptId + "'");
							if (kingdeeOrgPosts != null && kingdeeOrgPosts.size() > 0) {
								for (KingdeeOrgPost orgPost : kingdeeOrgPosts) {
									if (job_title.equalsIgnoreCase(orgPost.getName())) {
										orgPostNumber = orgPost.getNumber();
										break;
									}
								}
							}
							// 如果根据部门没有找到至少一个岗位信息就新增一个岗位
							if ("0".equals(orgPostNumber)) {
								String saveOrgPostData = "{\"NeedUpDateFields\":[],\"NeedReturnFields\":[]," +
										"\"IsDeleteEntry\":\"true\",\"SubSystemId\":\"\",\"IsVerifyBaseDataField\":\"false\"," +
										"\"IsEntryBatchFill\":\"true\",\"ValidateFlag\":\"true\",\"NumberSearch\":\"true\"," +
										"\"IsAutoAdjustField\":\"false\",\"InterationFlags\":\"\",\"IgnoreInterationFlag\":\"\"," +
										"\"IsControlPrecision\":\"false\",\"ValidateRepeatJson\":\"false\"," +
										"\"Model\":{\"FPOSTID\":0,\"FCreateOrgId\":{\"FNumber\":\"创建组织\"},\"FNumber\":\"\"," +
										"\"FUseOrgId\":{\"FNumber\":\"使用组织\"},\"FName\":\"名称\",\"FHelpCode\":\"\"," +
										"\"FDept\":{\"FNumber\":\"所属部门\"},\"FEffectDate\":\"1900-01-01\"," +
										"\"FLapseDate\":\"1900-01-01\",\"FDESCRIPTIONS\":\"\",\"FHRPostSubHead\":{\"FHRPOSTID\":0," +
										"\"FLEADERPOST\":\"false\"},\"FSHRMapEntity\":{\"FMAPID\":0}," +
										"\"FSubReportEntity\":[{\"FSubNumber\":\"\"}]}}";
								saveOrgPostData = saveOrgPostData.replaceAll("所属部门", kingdeeDeptNumber);
								saveOrgPostData = saveOrgPostData.replaceAll("名称", job_title);
								saveOrgPostData = saveOrgPostData.replaceAll("创建组织", Constants.ORG_NUMBER);
								saveOrgPostData = saveOrgPostData.replaceAll("使用组织", Constants.ORG_NUMBER);
								System.out.println("saveOrgPostData: " + saveOrgPostData);
								String saveOrgPostDataResult = api.save("HR_ORG_HRPOST", saveOrgPostData);
								System.out.println("saveOrgPostDataResult: " + saveOrgPostDataResult);
								JSONObject postObject = JSONObject.parseObject(saveOrgPostDataResult);
								JSONObject resultObject = (JSONObject) postObject.get("Result");
								JSONObject responseStatus = (JSONObject) resultObject.get("ResponseStatus");
								if (responseStatus.getBoolean("IsSuccess")) {
									// 请求成功
									orgPostNumber = resultObject.getString("Number");
									// 提交 & 审核
									api.submit("HR_ORG_HRPOST", "{\"Numbers\":\"" + orgPostNumber + "\"}");
									api.audit("HR_ORG_HRPOST", "{\"Numbers\":\"" + orgPostNumber + "\"}");
								}
							}
							// 修改基本信息
							String staffSaveJson = "{\"NeedUpDateFields\":[],\"NeedReturnFields\":[]," +
									"\"IsDeleteEntry\":\"true\",\"SubSystemId\":\"\",\"IsVerifyBaseDataField\":\"false\"," +
									"\"IsEntryBatchFill\":\"true\",\"ValidateFlag\":\"true\",\"NumberSearch\":\"true\"," +
									"\"IsAutoAdjustField\":\"false\",\"InterationFlags\":\"\",\"IgnoreInterationFlag\":\"\"," +
									"\"IsControlPrecision\":\"false\",\"ValidateRepeatJson\":\"false\"," +
									"\"Model\":{\"FID\":\"实体主键\",\"FName\":\"员工姓名\",\"FStaffNumber\":\"员工编号\",\"FMobile\":\"移动电话\",\"FTel\":\"\"," +
									"\"FEmail\":\"\",\"FDescription\":\"\",\"FAddress\":\"\",\"FCreateOrgId\":{\"FNumber\":\"创建组织\"}," +
									"\"FUseOrgId\":{\"FNumber\":\"使用组织\"},\"FBranchID\":{\"FNUMBER\":\"\"},\"FCreateSaler\":\"false\"," +
									"\"FCreateUser\":\"false\",\"FCreateCashier\":\"false\",\"FCashierGrp\":{\"FNUMBER\":\"\"}," +
									"\"FUserId\":{\"FUSERACCOUNT\":\"\"},\"FCashierId\":{\"FNUMBER\":\"\"},\"FSalerId\":{\"FNUMBER\":\"\"}," +
									"\"FPostId\":{\"FNUMBER\":\"\"},\"FJoinDate\":\"1900-01-01\",\"FUniportalNo\":\"\"," +
									"\"FSHRMapEntity\":{\"FMAPID\":0}," +
									"\"FBankInfo\":[{\"FBankId\":0,\"FBankCountry\":{\"FNumber\":\"\"},\"FBankCode\":\"\"," +
									"\"FBankHolder\":\"\",\"FBankTypeRec\":{\"FNUMBER\":\"\"},\"FTextBankDetail\":\"\"," +
									"\"FBankDetail\":{\"FNUMBER\":\"\"},\"FOpenBankName\":\"\",\"FOpenAddressRec\":\"\"," +
									"\"FCNAPS\":\"\",\"FBankCurrencyId\":{\"FNUMBER\":\"\"},\"FBankIsDefault\":\"false\"," +
									"\"FBankDesc\":\"\",\"FCertType\":\"\",\"FIsFromSHR\":\"false\",\"FCertNum\":\"\"}]}}";
							staffSaveJson = staffSaveJson.replaceAll("实体主键", fid);
							staffSaveJson = staffSaveJson.replaceAll("员工姓名", user.getName());
							staffSaveJson = staffSaveJson.replaceAll("创建组织", Constants.ORG_NUMBER);
							staffSaveJson = staffSaveJson.replaceAll("使用组织", Constants.ORG_NUMBER);
							staffSaveJson = staffSaveJson.replaceAll("员工编号", employee_no);

	//						staffSaveJson = staffSaveJson.replaceAll("所属部门", kingdeeDeptNumber);
	//						staffSaveJson = staffSaveJson.replaceAll("就任岗位", orgPostNumber);
	//						staffSaveJson = staffSaveJson.replaceAll("工作组织", Constants.ORG_NUMBER);
							// 非必填
							staffSaveJson = staffSaveJson.replaceAll("移动电话", StringUtil.mobileDivAreaCode(mobile));
	//						staffSaveJson = staffSaveJson.replaceAll("任岗开始日期", TimeUtil.timestampToYMD(join_time));
							String saveEmpinfoResult = api.save("BD_Empinfo", staffSaveJson);
							JSONObject postObject = JSONObject.parseObject(saveEmpinfoResult);
							JSONObject resultObject = (JSONObject) postObject.get("Result");
							JSONObject responseStatus = (JSONObject) resultObject.get("ResponseStatus");
							// 金蝶修改成功，映射表更新用户
							if (responseStatus.getBoolean("IsSuccess")) {
								User userTemp = User.builder()
										.id(user.getId())
										.fid(fid)
										.name(user.getName())
										.deptId(deptId)
										.build();
								userMapper.updateById(userTemp);
								log.info("saveEmpinfo success: {}", saveEmpinfoResult);
							} else {
								log.info("saveEmpinfo fail: {}", saveEmpinfoResult);
							}

							// 判断岗位是否变更：如果事件体拿到的岗位和当前用户主岗位的名称相同，则说明岗位没变动，不用传岗位明细，否则调用修改接口
							// 查询员工岗位信息
							String viewStaffInfoJson = "{\"CreateOrgId\":0,\"Number\":\"\",\"Id\":\"员工ID\",\"IsSortBySeq\":\"false\"}";
							viewStaffInfoJson = viewStaffInfoJson.replaceAll("员工ID", user.getFid());
							System.out.println("viewStaffInfoJson: " + viewStaffInfoJson);
							String viewStaffInfoJsonResult = api.view("BD_Empinfo", viewStaffInfoJson);
							JSONObject postObjectNew = JSONObject.parseObject(viewStaffInfoJsonResult);
							JSONObject resultObjectNew = (JSONObject) postObjectNew.get("Result");
							JSONObject responseStatusNew = (JSONObject) resultObjectNew.get("ResponseStatus");
							if (responseStatusNew.getBoolean("IsSuccess")) {
								String postName = "";
								// 请求成功
								JSONObject result = resultObjectNew.getJSONObject("Result");
								JSONArray postEntity = result.getJSONArray("PostEntity");
								for (int i = 0; i < postEntity.size(); i++) {
									if (postEntity.getJSONObject(i).getBoolean("IsFirstPost")) {
										JSONArray jsonArrayName = postEntity.getJSONObject(i).getJSONObject("Post").getJSONArray("Name");
										for (int j = 0; j < jsonArrayName.size(); j++) {
											if ("2052".equals(jsonArrayName.getJSONObject(j).getString("Key"))) {
												postName = jsonArrayName.getJSONObject(j).getString("Value");
												break;
											}
										}

									}
								}

								// 不可能只修改个用户名称也新增岗位明细吧
								// 新增任岗明细条件： 改了岗位 || 改了部门
								if (!postName.equals(job_title) || !department.getFeishuDeptId().equals(user.getDeptId())) {
									// 岗位不同就添加岗位明细
									String newStaffSaveJson = "{\"NeedUpDateFields\":[],\"NeedReturnFields\":[],\"IsDeleteEntry\":\"true\",\"SubSystemId\":\"\"," +
											"\"IsVerifyBaseDataField\":\"false\",\"IsEntryBatchFill\":\"true\",\"ValidateFlag\":\"true\",\"NumberSearch\":\"true\"," +
											"\"IsAutoAdjustField\":\"false\",\"InterationFlags\":\"\",\"IgnoreInterationFlag\":\"\",\"IsControlPrecision\":\"false\"," +
											"\"ValidateRepeatJson\":\"false\",\"Model\":{\"FCreateOrgId\":{\"FNumber\":\"创建组织\"},\"FNumber\":\"员工编码\"," +
											"\"FUseOrgId\":{\"FNumber\":\"使用组织\"}, \"FPerson\":{\"FNumber\":\"人员详细信息编号\"},\"FStartDate\":\"任岗开始日期\"," +
											"\"FDept\":{\"FNumber\":\"所属部门\"},\"FName\":\"姓名\",\"FPosition\":{\"FNumber\":\"就任岗位\"},\"FEmpInfoId\":{\"FNumber\":\"员工编号\"}," +
											"\"FDescription\":\"\",\"FOperatorType\":\"\",\"FPOSTBILLEntity\":{\"FENTRYID\":0,\"FIsFirstPost\":\"true\"}," +
											"\"FOtherEntity\":{\"FENTRYID\":0},\"FSHRMapEntity\":{\"FMAPID\":0},\"FStaffRole\":[{\"FSTAFFROLEID\":0," +
											"\"FRoleStaffID\":0,\"FIsStaffRole\":\"\"}],\"FPostEntity\":[{\"FOperaType\":\"\"}]}}";
									newStaffSaveJson = newStaffSaveJson.replaceAll("创建组织", Constants.ORG_NUMBER);
									newStaffSaveJson = newStaffSaveJson.replaceAll("使用组织", Constants.ORG_NUMBER);


									JSONArray jsonArrayName = result.getJSONArray("Name");
									for (int j = 0; j < jsonArrayName.size(); j++) {
										if ("2052".equals(jsonArrayName.getJSONObject(j).getString("Key"))) {
											newStaffSaveJson = newStaffSaveJson.replaceAll("姓名", jsonArrayName.getJSONObject(j).getString("Value"));
											break;
										}
									}
									System.out.println("员工编号: " + result.getString("Id") + "  " + result.getString("Number"));

									// 通过表DB_Person人员详细信息(公共)表列表，根据上一步员工FID->fEmpInfo找到对应的fnumber
									String personNumber = "";
									List<KingdeePerson> personList = userService.queryPersonList("fEmpInfo='" + result.getString("Id") + "'");
									if (personList != null && personList.size() > 0) {
										personNumber = personList.get(0).getNumber();
									}

									newStaffSaveJson = newStaffSaveJson.replaceAll("员工编号", result.getString("Number"));
									newStaffSaveJson = newStaffSaveJson.replaceAll("员工编码", result.getString("Number"));
									newStaffSaveJson = newStaffSaveJson.replaceAll("人员详细信息编号", personNumber);

									newStaffSaveJson = newStaffSaveJson.replaceAll("所属部门", kingdeeDeptNumber);
									newStaffSaveJson = newStaffSaveJson.replaceAll("就任岗位", orgPostNumber);
									newStaffSaveJson = newStaffSaveJson.replaceAll("任岗开始日期", TimeUtil.timestampToYMD(join_time));
									String saveNewStaffResult = api.save("BD_NEWSTAFF", newStaffSaveJson);
									JSONObject postObjectNewStaff = JSONObject.parseObject(saveNewStaffResult);
									JSONObject resultObjectNewStaff = (JSONObject) postObjectNewStaff.get("Result");
									JSONObject responseStatusNewStaff = (JSONObject) resultObjectNewStaff.get("ResponseStatus");
									// 金蝶修改成功，映射表更新用户
									if (responseStatusNewStaff.getBoolean("IsSuccess")) {
										log.info("saveNewStaff success: {}", saveNewStaffResult);
										String newStaffNumber = resultObjectNewStaff.getString("Number");
										// 提交 & 审核
										api.submit("BD_NEWSTAFF", "{\"Numbers\":\"" + newStaffNumber + "\"}");
										api.audit("BD_NEWSTAFF", "{\"Numbers\":\"" + newStaffNumber + "\"}");
									} else {
										log.info("saveNewStaff fail param: {}", newStaffSaveJson);
										log.info("saveNewStaff fail result: {}", saveNewStaffResult);
									}

								}
							}

						} catch (Exception e) {
							e.printStackTrace();
						}
					};
	                Constants.queue.submitTask(worker);
                }
            }).onP2UserDeletedV3(new ContactService.P2UserDeletedV3Handler() {
                // 用户删除
                @Override
                public void handle(P2UserDeletedV3 event) throws Exception {
	                Runnable worker = () -> {
						log.info("P2UserDeletedV3: {}", Jsons.DEFAULT.toJson(event));
						String resultJson = Jsons.DEFAULT.toJson(event);
						JSONObject eventsObject = (JSONObject) JSONObject.parse(resultJson);
						JSONObject eventObject = (JSONObject) eventsObject.get("event");
						JSONObject object = (JSONObject) eventObject.get("object");
						String user_id = object.getString("user_id");
						User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUserId, user_id).last("limit 1"));
						if (user == null) {
							log.info("P2UserDeletedV3事件：用户映射表中根据user_id获取不到记录: {}", user_id);
							return;
						}
						try {

							// 改为如果是审核了就反审核，并且禁用
							String unAuditEmpinfoResult = api.unAudit("BD_Empinfo", "{\"Ids\":\"" + user.getFid() + "\"}");
							JSONObject unAuditEmpinfoObject = JSONObject.parseObject(unAuditEmpinfoResult);
							JSONObject unAuditResultObject = (JSONObject) unAuditEmpinfoObject.get("Result");
							JSONObject unAuditResponseStatus = (JSONObject) unAuditResultObject.get("ResponseStatus");

							String forbidEmpinfoResult = api.excuteOperation("BD_Empinfo", "Forbid", "{\"Ids\":\"" + user.getFid() + "\"}");
							JSONObject forbidEmpinfoObject = JSONObject.parseObject(forbidEmpinfoResult);
							JSONObject forbidResultObject = (JSONObject) forbidEmpinfoObject.get("Result");
							JSONObject forbidResponseStatus = (JSONObject) forbidResultObject.get("ResponseStatus");

							// 映射表删除用户
							if (forbidResponseStatus.getBoolean("IsSuccess")) {
								userMapper.deleteById(user.getId());
								log.info("deleteEmpinfo success: {}", forbidResponseStatus);
							} else {
								log.info("deleteEmpinfo fail: {}", forbidResponseStatus.getJSONArray("Errors").toJSONString());
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					};
	                Constants.queue.submitTask(worker);
                }
            }).onP2DepartmentCreatedV3(new ContactService.P2DepartmentCreatedV3Handler() {
                // 部门创建
                @Override
                public void handle(P2DepartmentCreatedV3 event) throws Exception {
	                Runnable worker = () -> {
						log.info("P2DepartmentCreatedV3: {}", Jsons.DEFAULT.toJson(event));
						String resultJson = Jsons.DEFAULT.toJson(event);
						JSONObject eventsObject = (JSONObject) JSONObject.parse(resultJson);
						JSONObject eventObject = (JSONObject) eventsObject.get("event");
						JSONObject object = (JSONObject) eventObject.get("object");
						String name = object.getString("name");
						String department_id = object.getString("department_id");
						String parent_department_id = object.getString("parent_department_id");

						// 调用自建应用根据父部门id：od-xxx获取部门名和导出的id样例
						String deptIdAndName = SignUtil.getDepartmentIdAndName(parent_department_id);
						String deptParentId = "";
						String deptParentNumber = "";
						if (deptIdAndName.startsWith("0,")) {
							deptParentId = "0";
						} else if (deptIdAndName.contains(",")
								&& deptIdAndName.split(",")[0] != null
								&& deptIdAndName.split(",")[1] != null) {
							String[] split = deptIdAndName.split(",");
							deptParentId = split[0];
						}

						// 根据飞书部门id查询部门number
						Department department = departmentMapper.selectOne(new LambdaQueryWrapper<Department>().eq(Department::getFeishuDeptId, deptParentId).last("limit 1"));
						if (department == null) {
							log.info("P2DepartmentCreatedV3事件：用户映射表中根据deptParentId获取不到父部门记录: {}", deptParentId);
							deptParentNumber = "0";
						}
						if (department != null && department.getNumber() != null) {
							deptParentNumber = department.getNumber();
						}
						try {
							String deptSaveJson = "{\"Model\":{\"FCreateOrgId\":{\"Number\":\"创建组织\"}," +
									"\"FNumber\":\"\",\"FUseOrgId\":{\"Number\":\"使用组织\"},\"FName\":\"部门名称\"," +
									"\"FHelpCode\":\"助记码\",\"FParentID\":{\"FNumber\":\"父部门\"}," +
									"\"FEffectDate\":\"1900-01-01\"," +
									"\"FLapseDate\":\"9999-01-01\",\"FDescription\":\"\"," +
									"\"FDeptProperty\":{\"FNumber\":\"\"},\"FSHRMapEntity\":{\"FMAPID\":0}}}";
							deptSaveJson = deptSaveJson.replaceAll("创建组织", Constants.ORG_NUMBER);
							deptSaveJson = deptSaveJson.replaceAll("使用组织", Constants.ORG_NUMBER);
							deptSaveJson = deptSaveJson.replaceAll("部门名称", name);
							deptSaveJson = deptSaveJson.replaceAll("父部门", deptParentNumber);

							// 修改助记码
							String code = SignUtil.corehrDepartment(department_id);
							if (StringUtil.isEmpty(code)) {
								deptSaveJson = deptSaveJson.replaceAll("助记码", "");
							} else {
								deptSaveJson = deptSaveJson.replaceAll("助记码", code);
							}

							String deptSaveResult = api.save("BD_Department", deptSaveJson);
							JSONObject postObject = JSONObject.parseObject(deptSaveResult);
							JSONObject resultObject = (JSONObject) postObject.get("Result");
							JSONObject responseStatus = (JSONObject) resultObject.get("ResponseStatus");
							// 金蝶新增部门成功，获取id，映射表创建部门
							if (responseStatus.getBoolean("IsSuccess")) {

								String departmentNewNumber = resultObject.getString("Number");
								// 提交 & 审核
								api.submit("BD_Department", "{\"Numbers\":\"" + departmentNewNumber + "\"}");
								api.audit("BD_Department", "{\"Numbers\":\"" + departmentNewNumber + "\"}");

								String kingdeeDeptId = resultObject.getString("Id");
								String number = resultObject.getString("Number");
								String kingdeeParentId = "";

								// 根据Number查询金蝶父部门ID
								List<KingdeeDept> kingdeeDepts = deptService.queryDepartmentList();
								for (KingdeeDept kingdeeDept : kingdeeDepts) {
									if (kingdeeDept.getNumber().equals(number)) {
										kingdeeParentId = kingdeeDept.getParentId();
										break;
									}
								}

								// 映射表新增部门
								Department departmentTemp = Department.builder()
										.name(name)
										.feishuDeptId(department_id)
										.feishuParentId(deptParentId)
										.kingdeeParentId(kingdeeParentId)
										.kingdeeDeptId(kingdeeDeptId)
										.number(number)
										.build();
								departmentMapper.insert(departmentTemp);
								log.info("saveDepartment success: {}", deptSaveResult);
							} else {
								log.info("saveDepartment fail: {}", deptSaveResult);
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					};
	                Constants.queue.submitTask(worker);
                }
            }).onP2DepartmentUpdatedV3(new ContactService.P2DepartmentUpdatedV3Handler() {
                // 部门修改
                @Override
                public void handle(P2DepartmentUpdatedV3 event) throws Exception {
	                Runnable worker = () -> {
						log.info("P2DepartmentUpdatedV3: {}", Jsons.DEFAULT.toJson(event));
						String resultJson = Jsons.DEFAULT.toJson(event);
						JSONObject eventsObject = (JSONObject) JSONObject.parse(resultJson);
						JSONObject eventObject = (JSONObject) eventsObject.get("event");
						JSONObject object = (JSONObject) eventObject.get("object");
						String name = object.getString("name");
						String department_id = object.getString("department_id");
						String parent_department_id = object.getString("parent_department_id");

						Department department = departmentMapper.selectOne(new LambdaQueryWrapper<Department>().eq(Department::getFeishuDeptId, department_id).last("limit 1"));
						if (department == null) {
							log.info("P2DepartmentUpdatedV3事件：部门映射表中根据deptId获取不到记录: {}", department_id);
							return;
						}
						if (department.getKingdeeDeptId() == null || department.getNumber() == null) {
							log.info("P2DepartmentUpdatedV3事件：部门映射表中根据deptId={}获取记录KingdeeDeptId为空或Number为空", department_id);
							return;
						}
						String kingdeeDeptId = department.getKingdeeDeptId();

						// 根据parent_department_id查询原来映射表的部门
						String deptIdAndName = SignUtil.getDepartmentIdAndName(parent_department_id);
						String deptId = "";
						if (deptIdAndName.startsWith("0,")) {
							deptId = "0";
						} else if (deptIdAndName.contains(",")) {
							String[] split = deptIdAndName.split(",");
							deptId = split[0];
						}
						String deptParentNumber = "";
						Department departmentParent = departmentMapper.selectOne(new LambdaQueryWrapper<Department>().eq(Department::getFeishuDeptId, deptId).last("limit 1"));
						if (departmentParent == null) {
							log.info("P2DepartmentUpdatedV3事件：该部门通过上级部门id找不到: {}", deptId);
							deptParentNumber = "0";
						} else {
							deptParentNumber = departmentParent.getNumber();
						}

						// 修改Kingdee系统部门的名称ParentName和DeptName
						String deptSaveJson = "{\"Model\":{\"FDEPTID\":\"部门ID\",\"FCreateOrgId\":{\"Number\":\"创建组织\"}," +
								"\"FNumber\":\"编号\",\"FUseOrgId\":{\"Number\":\"使用组织\"},\"FName\":\"部门名称\"," +
								"\"FHelpCode\":\"助记码\",\"FParentID\":{\"FNumber\":\"父部门\"}," +
								"\"FDescription\":\"\"," +
								"\"FDeptProperty\":{\"FNumber\":\"\"},\"FSHRMapEntity\":{\"FMAPID\":0}}}";
						deptSaveJson = deptSaveJson.replaceAll("创建组织", Constants.ORG_NUMBER);
						deptSaveJson = deptSaveJson.replaceAll("使用组织", Constants.ORG_NUMBER);
						deptSaveJson = deptSaveJson.replaceAll("编号", department.getNumber());
						deptSaveJson = deptSaveJson.replaceAll("部门ID", kingdeeDeptId);
						deptSaveJson = deptSaveJson.replaceAll("部门名称", name);
						if (deptParentNumber == null) {
							deptSaveJson = deptSaveJson.replaceAll("父部门", "0");
						} else {
							deptSaveJson = deptSaveJson.replaceAll("父部门", deptParentNumber);
						}

						// 修改助记码
						String code = SignUtil.corehrDepartment(department_id);
						if (StringUtil.isEmpty(code)) {
							deptSaveJson = deptSaveJson.replaceAll("助记码", "");
						} else {
							deptSaveJson = deptSaveJson.replaceAll("助记码", code);
						}
						try {
							String deptSaveResult = api.save("BD_Department", deptSaveJson);
							JSONObject postObject = JSONObject.parseObject(deptSaveResult);
							JSONObject resultObject = (JSONObject) postObject.get("Result");
							JSONObject responseStatus = (JSONObject) resultObject.get("ResponseStatus");
							// 金蝶修改部门成功，修改映射表金蝶parentId，部门名称
							if (responseStatus.getBoolean("IsSuccess")) {
								// 根据id查询kingdeeParentId
								Department departmentTemp = Department.builder()
										.id(department.getId())
										.name(name)
										.feishuDeptId(department_id)
										.feishuParentId(deptId)
										.kingdeeParentId(departmentParent.getKingdeeDeptId())
										.build();
								departmentMapper.updateById(departmentTemp);
								log.info("saveDepartment success: {}", deptSaveResult);
							} else {
								log.info("saveDepartment fail: {}", deptSaveResult);
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					};
	                Constants.queue.submitTask(worker);
                }
            }).onP2DepartmentDeletedV3(new ContactService.P2DepartmentDeletedV3Handler() {
                // 部门删除
                @Override
                public void handle(P2DepartmentDeletedV3 event) throws Exception {
	                Runnable worker = () -> {
						log.info("P2DepartmentDeletedV3: {}", Jsons.DEFAULT.toJson(event));
						String resultJson = Jsons.DEFAULT.toJson(event);
						JSONObject eventsObject = (JSONObject) JSONObject.parse(resultJson);
						JSONObject eventObject = (JSONObject) eventsObject.get("event");
						JSONObject object = (JSONObject) eventObject.get("object");
						String department_id = object.getString("department_id");
						Department department = departmentMapper.selectOne(new LambdaQueryWrapper<Department>().eq(Department::getFeishuDeptId, department_id).last("limit 1"));
						if (department == null) {
							log.info("P2DepartmentDeletedV3事件：用户映射表中根据department_id获取不到记录: {}", department_id);
							return;
						}
						try {
	//						String deptDeleteJson = "{\"CreateOrgId\":0,\"Numbers\":[],\"ids\":\"实体主键\",\"NetworkCtrl\":\"\"}";
	//						deptDeleteJson = deptDeleteJson.replaceAll("实体主键", feishuDeptId);
	//						String deleteDeptResult = api.delete("BD_Department", deptDeleteJson);
	//						JSONObject postObject = JSONObject.parseObject(deleteDeptResult);
	//						JSONObject resultObject = (JSONObject) postObject.get("Result");
	//						JSONObject responseStatus = (JSONObject) resultObject.get("ResponseStatus");

							// 改为如果是审核了就反审核，并且禁用
	//						String unAuditEmpinfoResult = api.unAudit("BD_Department", "{\"Ids\":\"" + department.getKingdeeDeptId() + "\"}");
	//						JSONObject unAuditEmpinfoObject = JSONObject.parseObject(unAuditEmpinfoResult);
	//						JSONObject unAuditResultObject = (JSONObject) unAuditEmpinfoObject.get("Result");
	//						JSONObject unAuditResponseStatus = (JSONObject) unAuditResultObject.get("ResponseStatus");
	//
	//						String forbidEmpinfoResult = api.excuteOperation("BD_Department", "Forbid", "{\"Ids\":\"" + department.getKingdeeDeptId() + "\"}");
	//						JSONObject forbidEmpinfoObject = JSONObject.parseObject(forbidEmpinfoResult);
	//						JSONObject forbidResultObject = (JSONObject) forbidEmpinfoObject.get("Result");
	//						JSONObject forbidResponseStatus = (JSONObject) forbidResultObject.get("ResponseStatus");
	//
	//						// 金蝶删除成功，映射表删除部门
	//						if (forbidResponseStatus.getBoolean("IsSuccess")) {
	//							departmentMapper.deleteById(department.getId());
	//							log.info("deleteDept success: {}", forbidResponseStatus);
	//						} else {
	//							log.info("deleteDept fail: {}", forbidResponseStatus.getJSONArray("Errors").toJSONString());
	//						}

	//						String deptDeleteJson = "{\"data\":\"{\"FormId\":\"HR_ORG_HRPOST\",\"FieldKeys\":\"FPOSTID\",\"FilterString\":\"FDept='" + department.getKingdeeDeptId() + "'\",\"OrderString\":\"\",\"TopRowCount\":0,\"StartRow\":0,\"Limit\":2000,\"SubSystemId\":\"\"}\"}";
							String text = "{\"FormId\":\"HR_ORG_HRPOST\",\"FieldKeys\":\"FPOSTID\",\"FilterString\":\"FDept='" + department.getKingdeeDeptId() + "'\",\"OrderString\":\"\",\"TopRowCount\":0,\"StartRow\":0,\"Limit\":2000,\"SubSystemId\":\"\"}";

							String unAuditEmpinfoResult = api.executeBillQueryJson(text);
							JSONArray jsonArray = JSONArray.parseArray(unAuditEmpinfoResult);
							//部门下的岗位
							String stationReplace = jsonArray.get(0).toString().replace("[", "").replace("]", "");
	//							String empinfo = "{\"FormId\":\"BD_Empinfo\",\"FieldKeys\":\"FID\",\"FilterString\":\"FPost='" + stationReplace + "'\",\"OrderString\":\"\",\"TopRowCount\":0,\"StartRow\":0,\"Limit\":2000,\"SubSystemId\":\"\"}";
	//							String json = api.executeBillQueryJson(empinfo);
	//							JSONArray userJson = JSONArray.parseArray(json);
							//岗位任职人员
	//							for (int n = 0; n < userJson.size(); n++) {
	//								String userReplace = userJson.get(n).toString().replace("[", "").replace("]", "");
	////								//反审核用户
	////								String bdEmpinfo = api.unAudit("BD_Empinfo", "{\"Ids\":\"" + userReplace + "\"}");
	////								//禁用用户
	////								String forbidEmpinfoResult = api.excuteOperation("BD_Empinfo", "Forbid", "{\"Ids\":\"" + userReplace + "\"}");
	//							}
							// 根据部门找到任岗明细列表禁用
							String findNewstaffListByHrPost = "{\"FormId\":\"BD_NEWSTAFF\",\"FieldKeys\":\"FSTAFFID\",\"FilterString\":\"FDept='" + department.getKingdeeDeptId() + "'\",\"OrderString\":\"\",\"TopRowCount\":0,\"StartRow\":0,\"Limit\":2000,\"SubSystemId\":\"\"}";
							String newstaffListByHrPostResult = api.executeBillQueryJson(findNewstaffListByHrPost);
							JSONArray newstaffListByHrPostArray = JSONArray.parseArray(newstaffListByHrPostResult);
							String newstaffListByHrPostStationReplace = newstaffListByHrPostArray.get(0).toString().replaceAll("\\[", "").replaceAll("\\]", "");
							System.out.println("newstaffListByHrPostStationReplace: " + newstaffListByHrPostStationReplace);
							//反审核&禁用 任岗明细
							String unAuditNewstaffResult = api.unAudit("BD_NEWSTAFF", "{\"Ids\":\"" + newstaffListByHrPostStationReplace + "\"}");
							String forbidNewstaffResult = api.excuteOperation("BD_NEWSTAFF", "Forbid", "{\"Ids\":\"" + newstaffListByHrPostStationReplace + "\"}");
							System.out.println("unAuditNewstaffResult: " + unAuditNewstaffResult);
							System.out.println("forbidNewstaffResult: " + forbidNewstaffResult);

							//反审核&禁用 岗位
							String unAuditHrPostResult = api.unAudit("HR_ORG_HRPOST", "{\"Ids\":\"" + stationReplace + "\"}");
							String forbidHrPostResult = api.excuteOperation("HR_ORG_HRPOST", "Forbid", "{\"Ids\":\"" + stationReplace + "\"}");
							System.out.println("unAuditHrPostResult: " + unAuditHrPostResult);
							System.out.println("forbidHrPostResult: " + forbidHrPostResult);

							//反审核部门
							String unDepartment = api.unAudit("BD_Department", "{\"Ids\":\"" + department.getKingdeeDeptId() + "\"}");
							JSONObject unAuditEmpinfoObject = JSONObject.parseObject(unDepartment);
							JSONObject unAuditResultObject = (JSONObject) unAuditEmpinfoObject.get("Result");
							JSONObject unAuditResponseStatus = (JSONObject) unAuditResultObject.get("ResponseStatus");
							//禁用部门
							String unAuditDepartmentResult = api.excuteOperation("BD_Department", "Forbid", "{\"Ids\":\"" + department.getKingdeeDeptId() + "\"}");
							JSONObject forbidEmpinfoObject = JSONObject.parseObject(unAuditDepartmentResult);
							JSONObject forbidResultObject = (JSONObject) forbidEmpinfoObject.get("Result");
							JSONObject forbidResponseStatus = (JSONObject) forbidResultObject.get("ResponseStatus");
							// 金蝶删除成功，映射表删除部门
							if (forbidResponseStatus.getBoolean("IsSuccess")) {
								departmentMapper.deleteById(department.getId());
								log.info("deleteDept success: {}", forbidResponseStatus);
							} else {
								log.info("deleteDept fail: {}", forbidResponseStatus.getJSONArray("Errors").toJSONString());
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					};
	                Constants.queue.submitTask(worker);
                }
            })
            .build();

    /**
     * 飞书订阅事件回调
     *
     * @param request
     * @param response
     * @throws Throwable
     */
    @RequestMapping(value = "/feishu/webhook/event")
    public void event(HttpServletRequest request, HttpServletResponse response)
            throws Throwable {
        servletAdapter.handleEvent(request, response, EVENT_DISPATCHER);
    }
}