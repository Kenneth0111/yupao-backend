package com.example.yupao.service;

import com.example.yupao.model.domain.Team;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.yupao.model.domain.User;
import com.example.yupao.model.dto.TeamQuery;
import com.example.yupao.model.vo.TeamVO;

import java.util.List;

/**
* @author 张博洋
* @description 针对表【team(队伍表)】的数据库操作Service
* @createDate 2025-12-27 16:35:50
*/
public interface TeamService extends IService<Team> {

    /**
     * 创建队伍
     *
     * @param team 队伍
     * @param loginUser 登录用户
     * @return teamId
     */
    long createTeam(Team team, User loginUser);

    /**
     * 查询队伍
     * @param teamQuery 队伍查询
     * @return
     */
    List<TeamVO> listTeams(TeamQuery teamQuery);
}
