package com.example.yupao.service;

import com.example.yupao.model.domain.UserTeam;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.yupao.model.vo.TeamVO;

import java.util.List;

/**
* @author 张博洋
* @description 针对表【user_team(用户队伍关系表)】的数据库操作Service
* @createDate 2025-12-27 17:01:20
*/
public interface UserTeamService extends IService<UserTeam> {

    /**
     * 当前用户是否已加入队伍
     * @param teamVOList 队伍列表
     * @param userId 当前用户id
     */
    void fillHasJoined(List<TeamVO> teamVOList, Long userId);

    /**
     * 已加入队伍的人数
     *
     * @param teamVOList 队伍列表
     */
    void fillMemberCount(List<TeamVO> teamVOList);

    /**
     * 获取用户已加入的有效队伍数量（不含已退出/逻辑删除的）
     */
    int getJoinedTeamNum(Long userId);

    /**
     * 获取队伍当前有效成员数量（不含已退出/逻辑删除的）
     */
    int getTeamMemberCount(Long teamId);

    /**
     * 获取除队长外第2早加入队伍的用户id
     */
    long findEarliestMemberExcludingLeader(Long teamId, Long excludeUserId);

}