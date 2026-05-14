package com.volunteer.mapper;

import com.volunteer.entity.SignRecord;
import org.apache.ibatis.annotations.*;

@Mapper
public interface SignRecordMapper {

    @Insert("INSERT INTO sign_record(user_id, activity_id, status, qr_token) VALUES(#{userId}, #{activityId}, 0, #{qrToken})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SignRecord record);

    @Select("SELECT * FROM sign_record WHERE user_id = #{userId} AND activity_id = #{activityId}")
    SignRecord findByUserAndActivity(@Param("userId") Integer userId, @Param("activityId") Integer activityId);

    @Update("UPDATE sign_record SET checkin_time = #{checkinTime}, checkin_location = #{checkinLocation}, " +
            "checkin_photo = #{checkinPhoto}, status = 1 WHERE id = #{id}")
    int updateCheckin(SignRecord record);

    @Update("UPDATE sign_record SET checkout_time = #{checkoutTime}, checkout_location = #{checkoutLocation}, status = 2 WHERE id = #{id}")
    int updateCheckout(SignRecord record);

    @Update("UPDATE sign_record SET status = 2, checkout_time = NOW(), checkout_location = #{location} WHERE id = #{id}")
    int checkout(@Param("id") Integer id, @Param("location") String location);
}