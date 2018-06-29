package com.nsn.benchmark.mapper;

import com.nsn.benchmark.entity.Firewall;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface FirewallMapper {
    @Select("<script>" +
            "SELECT CONDITION, SOURCE_HOST, SOURCE_PORT, START_TIME FROM PAIR \n" +
            "WHERE CONDITION IN (" +
            "<foreach collection=\"conditions\" item=\"c\" separator=\",\">#{c}</foreach>" +
            ")" +
            "</script>")
    List<Map<String, Object>> select(@Param("conditions") List<byte[]> conditions);

    @Insert("<script>" +
            "INSERT INTO FIREWALL " +
            " (PRIVATE_HOST, PRIVATE_PORT, DESTINATION_HOST, DESTINATION_PORT, SOURCE_HOST, SOURCE_PORT, START_TIME) " +
            "VALUES " +
            "<foreach collection=\"list\" item=\"e\" separator=\",\"> " +
            " (#{e.privateHost}, #{e.privatePort}, " +
            "#{e.destinationHost}, #{e.destinationPort}, " +
            "#{e.sourceHost}, #{e.sourcePort}, " +
            "#{e.startTime}) " +
            "</foreach>" +
            "ON DUPLICATE KEY UPDATE " +
            "SOURCE_HOST = VALUES(SOURCE_HOST), " +
            "SOURCE_PORT = VALUES(SOURCE_PORT)" +
            "</script>")
    int insert(@Param("list") List<Firewall> list);

    @Delete("DELETE FROM FIREWALL")
    int deleteAll();

    @Select("SELECT T.INDEX, P.SOURCE_HOST, P.SOURCE_PORT, P.START_TIME " +
            "FROM TEMPORARY T RIGHT OUTER JOIN PAIR P ON (T.CONDITION = P.CONDITION) " +
            "WHERE " +
            "    T.HOST = #{host}" +
            "    AND THREAD_NAME = #{threadName}" +
            "    AND TIME = TO_TIMESTAMP(MILLIS, #{millis}) " +
            "ORDER BY T.INDEX")
    List<Map<String, Object>> joinSelect(@Param("host") String host,
                                         @Param("threadName") String threadName,
                                         @Param("millis") long millis);

    @Delete("DELETE FROM TEMPORARY " +
            "WHERE" +
            "   HOST = #{host}" +
            "   AND THREAD_NAME = #{threadName}" +
            "   AND (TIME = TO_TIMESTAMP(MILLIS, #{millis}) " +
            "       OR TIME < DATEADD(HOUR, -1, TO_TIMESTAMP(MILLIS, #{millis})))")
    int delete(@Param("host") String host,
               @Param("threadName") String threadName,
               @Param("millis") long millis);
}
