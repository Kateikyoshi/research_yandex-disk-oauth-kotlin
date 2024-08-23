package jp.warau.bakari.db.model;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface FunStuffMapper {
    @Select("SELECT * FROM FUN_STUFF WHERE id = #{id}")
    FunStuff getFunStuff(@Param("id") Long id);
}
