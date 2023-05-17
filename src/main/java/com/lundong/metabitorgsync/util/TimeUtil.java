package com.lundong.metabitorgsync.util;

import cn.hutool.core.date.LocalDateTimeUtil;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;

/**
 * @author RawChen
 * @date 2023-03-02 15:14
 */
public class TimeUtil {

	/**
	 * 生成时间戳
	 *
	 * @return
	 */
	public static String getTimestamp() {
		return (Calendar.getInstance().getTimeInMillis() / 1000) + "";
	}

	/**
	 * 时间戳转UTC格式
	 *
	 * @param joinTime
	 * @return
	 */
	public static String timestampToUTC(String joinTime) {
		try {
			LocalDateTime localDateTime = LocalDateTimeUtil
					.ofUTC(Long.parseLong(joinTime + "000") + 28800000);
			return localDateTime
					.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss+08:00"));
		} catch (NumberFormatException e) {
			return LocalDateTime.now().
					format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss+08:00"));
		}
	}

	public static String timestampToYMD(String timestamp) {
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		return format.format(Long.parseLong(timestamp) * 1000);
	}
}
