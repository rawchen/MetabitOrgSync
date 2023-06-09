package com.lundong.metabitorgsync.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

/**
 * 用户映射关系
 *
 * @author RawChen
 * @date 2023-03-06 9:56
 */
@Data
@Builder
@TableName("user")
public class User {

	/**
	 * ID
	 */
	@TableId(type = IdType.AUTO)
	private long id;

	/**
	 * 飞书用户ID
	 */
	private String userId;

	/**
	 * 金蝶用户ID
	 */
	private String staffId;

	/**
	 * 姓名
	 */
	private String name;

	/**
	 * 飞书部门ID
	 */
	private String deptId;

	/**
	 * 金蝶实体主键
	 */
	private String fid;
}
