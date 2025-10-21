package com.campusmarket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campusmarket.entity.ChatMessageEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessageEntity> {
}
