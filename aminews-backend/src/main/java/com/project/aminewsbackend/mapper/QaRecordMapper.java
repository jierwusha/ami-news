package com.project.aminewsbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.project.aminewsbackend.entity.QaRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface QaRecordMapper extends BaseMapper<QaRecord> {
    
    /**
     * 根据用户ID和会话ID查询记录
     */
    @Select("SELECT * FROM qa_record WHERE user_id = #{userId} AND session_id = #{sessionId} ORDER BY created_time DESC")
    List<QaRecord> selectByUserIdAndSessionId(@Param("userId") Integer userId, @Param("sessionId") String sessionId);
    
    /**
     * 根据会话ID查询记录（兼容旧版本）
     */
    @Select("SELECT * FROM qa_record WHERE session_id = #{sessionId} ORDER BY created_time DESC")
    List<QaRecord> selectBySessionId(@Param("sessionId") String sessionId);
    
    /**
     * 获取用户最近的问答记录
     */
    @Select("SELECT * FROM qa_record WHERE user_id = #{userId} AND session_id = #{sessionId} ORDER BY created_time DESC LIMIT #{limit}")
    List<QaRecord> selectRecentByUserIdAndSessionId(@Param("userId") Integer userId, @Param("sessionId") String sessionId, @Param("limit") int limit);
    
    /**
     * 获取最近的问答记录（兼容旧版本）
     */
    @Select("SELECT * FROM qa_record WHERE session_id = #{sessionId} ORDER BY created_time DESC LIMIT #{limit}")
    List<QaRecord> selectRecentBySessionId(@Param("sessionId") String sessionId, @Param("limit") int limit);
    
    /**
     * 删除用户的会话记录
     */
    @Delete("DELETE FROM qa_record WHERE user_id = #{userId} AND session_id = #{sessionId}")
    int deleteByUserIdAndSessionId(@Param("userId") Integer userId, @Param("sessionId") String sessionId);
    
    /**
     * 删除会话记录（兼容旧版本）
     */
    @Delete("DELETE FROM qa_record WHERE session_id = #{sessionId}")
    int deleteBySessionId(@Param("sessionId") String sessionId);
    
    /**
     * 获取用户的所有会话记录
     */
    @Select("SELECT * FROM qa_record WHERE user_id = #{userId} ORDER BY created_time DESC LIMIT #{limit}")
    List<QaRecord> selectByUserId(@Param("userId") Integer userId, @Param("limit") int limit);
}