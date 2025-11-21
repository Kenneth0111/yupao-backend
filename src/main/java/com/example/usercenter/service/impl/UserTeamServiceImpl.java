package com.example.usercenter.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.usercenter.model.domain.UserTeam;
import com.example.usercenter.service.UserTeamService;
import com.example.usercenter.mapper.UserTeamMapper;
import org.springframework.stereotype.Service;

/**
* @author 张博洋
* @description 针对表【user_team(用户队伍关系表)】的数据库操作Service实现
* @createDate 2025-09-19 13:32:01
*/
@Service
public class UserTeamServiceImpl extends ServiceImpl<UserTeamMapper, UserTeam>
    implements UserTeamService{

}




