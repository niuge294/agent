package com.yupi.yuaiagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yupi.yuaiagent.model.entity.Conversation;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ConversationMapper extends BaseMapper<Conversation> {
}
