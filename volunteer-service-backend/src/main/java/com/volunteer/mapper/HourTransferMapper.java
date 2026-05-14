package com.volunteer.mapper;

import com.volunteer.entity.HourTransfer;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface HourTransferMapper {

    @Insert("INSERT INTO hour_transfer(from_user_id, to_user_id, hours, reason, transfer_time, status) " +
            "VALUES(#{fromUserId}, #{toUserId}, #{hours}, #{reason}, NOW(), #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "transferId")
    int insert(HourTransfer transfer);

    @Select("SELECT * FROM hour_transfer WHERE transfer_id = #{transferId}")
    HourTransfer findById(Integer transferId);

    @Select("<script>" +
            "SELECT t.*, uf.real_name as from_user_name, ut.real_name as to_user_name " +
            "FROM hour_transfer t " +
            "JOIN user uf ON t.from_user_id = uf.user_id " +
            "JOIN user ut ON t.to_user_id = ut.user_id " +
            "WHERE 1=1 " +
            "<if test='userId != null'>" +
            "AND (t.from_user_id = #{userId} OR t.to_user_id = #{userId})" +
            "</if>" +
            "<if test='status != null'>" +
            "AND t.status = #{status}" +
            "</if>" +
            "ORDER BY t.transfer_time DESC " +
            "LIMIT #{offset}, #{limit}" +
            "</script>")
    List<HourTransfer> findPage(Map<String, Object> params);

    @Select("SELECT COUNT(*) FROM hour_transfer WHERE 1=1 " +
            "<if test='userId != null'>" +
            "AND (from_user_id = #{userId} OR to_user_id = #{userId})" +
            "</if>" +
            "<if test='status != null'>" +
            "AND status = #{status}" +
            "</if>")
    int count(Map<String, Object> params);

    @Update("UPDATE hour_transfer SET status = #{status} WHERE transfer_id = #{transferId}")
    int updateStatus(@Param("transferId") Integer transferId, @Param("status") Integer status);
}
