package com.familyrecipes.mapper;

import com.familyrecipes.entity.ReminderSetting;
import org.apache.ibatis.annotations.*;

/**
 * 提醒设置Mapper
 */
@Mapper
public interface ReminderSettingMapper {

    @Select("SELECT * FROM reminder_setting WHERE user_id = #{userId}")
    ReminderSetting findByUserId(Long userId);

    @Insert("INSERT INTO reminder_setting(user_id, days_before_expiry, reminder_time, enabled) " +
            "VALUES(#{userId}, #{daysBeforeExpiry}, #{reminderTime}, #{enabled})")
    int insert(ReminderSetting setting);

    @Insert("INSERT INTO reminder_setting(user_id,days_before_expiry,reminder_time,enabled) VALUES(#{userId},#{daysBeforeExpiry},#{reminderTime},#{enabled}) ON DUPLICATE KEY UPDATE days_before_expiry=VALUES(days_before_expiry),reminder_time=VALUES(reminder_time),enabled=VALUES(enabled)")
    int update(ReminderSetting setting);
}
