package com.familyrecipes.mapper;

import com.familyrecipes.entity.User;
import org.apache.ibatis.annotations.*;

/**
 * 用户Mapper
 */
@Mapper
public interface UserMapper {

    @Select("SELECT * FROM user WHERE id = #{id}")
    User findById(Long id);

    @Select("SELECT * FROM user WHERE email = #{email}")
    User findByEmail(String email);

    @Insert("INSERT INTO user(email, password, username, avatar) " +
            "VALUES(#{email}, #{password}, #{username}, #{avatar})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Update("UPDATE user SET username=#{username}, avatar=#{avatar} WHERE id=#{id}")
    int update(User user);

    @Update("UPDATE user SET password=#{password} WHERE id=#{id}")
    int updatePassword(@Param("id") Long id, @Param("password") String password);
}

