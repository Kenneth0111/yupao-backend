package com.example.yupao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.yupao.common.ErrorCode;
import com.example.yupao.exception.BusinessException;
import com.example.yupao.model.domain.UserTeam;
import com.example.yupao.model.vo.TeamVO;
import com.example.yupao.service.UserTeamService;
import com.example.yupao.mapper.UserTeamMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
* @author 张博洋
* @description 针对表【user_team(用户队伍关系表)】的数据库操作Service实现
* @createDate 2025-12-27 17:01:20
*/
@Service
public class UserTeamServiceImpl extends ServiceImpl<UserTeamMapper, UserTeam>
    implements UserTeamService{

    /**
     * 当前用户是否已加入队伍
     * @param teamVOList 队伍列表
     * @param userId 当前用户id
     */
    public void fillHasJoined(List<TeamVO> teamVOList, Long userId) {
        // 1.判断teamVOList是否为空
        if (CollectionUtils.isEmpty(teamVOList)) {
            return;
        }

        // 2.通过teamVOList获取teamIdList
        List<Long> teamIdList = teamVOList.stream()
                .map(TeamVO::getId)
                .filter(Objects::nonNull)
                .toList();

        // 3.判断userId或teamIdList是否为空：为空则「是否已加入」= false
        if (userId == null || teamIdList.isEmpty()) {
            teamVOList.forEach(teamVO -> teamVO.setHasJoined(false));
            return;
        }

        // 4.查询当前userId在当前teamIdList中加入了哪些队伍
        QueryWrapper<UserTeam> qw = new QueryWrapper<>();
        qw.select("teamId").eq("userId", userId).in("teamId", teamIdList);
        List<UserTeam> joinRecords = this.list(qw);

        // 5.通过joinRecords得到对应的joinedTeamIds集合
        Set<Long> joinedTeamIds = joinRecords.stream()
                .map(UserTeam::getTeamId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 6. 为teamVOList逐一填充hasJoined字段
        teamVOList.forEach(
                teamVO -> teamVO.setHasJoined(joinedTeamIds.contains(teamVO.getId()))
        );
    }

    /**
     * 已加入队伍的人数
     * @param teamVOList 队伍列表
     */
    public void fillMemberCount(List<TeamVO> teamVOList) {
        // 1.判断teamVOList是否为空
        if (CollectionUtils.isEmpty(teamVOList)) {
            return;
        }

        // 2.通过teamVOList获取teamIdList
        List<Long> teamIdList = teamVOList.stream()
                .map(TeamVO::getId)
                .filter(Objects::nonNull)
                .toList();

        // 3.判断teamIdList是否为空：为空则「已加入人数」= 0
        if (teamIdList.isEmpty()) {
            teamVOList.forEach(teamVO -> teamVO.setMemberCount(0));
            return;
        }

        // 4. 批量查询所有队伍的成员关系
        QueryWrapper<UserTeam> qw = new QueryWrapper<>();
        qw.select("teamId").in("teamId", teamIdList);
        List<UserTeam> userTeams = this.list(qw);

        // 5.按 teamId 分组统计人数汇总成Map
        Map<Long, Long> teamIdToJoinCount = userTeams.stream()
                .collect(Collectors.groupingBy(
                        UserTeam::getTeamId,
                        Collectors.counting()
                ));

        // 6.填充 memberCount（未查到则为 0）
        teamVOList.forEach(teamVO ->
                teamVO.setMemberCount(teamIdToJoinCount.getOrDefault(teamVO.getId(), 0L).intValue()));
    }

    @Override
    public int getJoinedTeamNum(Long userId) {
        QueryWrapper<UserTeam> q = new QueryWrapper<UserTeam>()
                .eq("userId", userId)
                .eq("isDelete", 0);
        return (int) this.count(q);
    }

    @Override
    public int getTeamMemberCount(Long teamId) {
        QueryWrapper<UserTeam> w = new QueryWrapper<UserTeam>()
                .eq("teamId", teamId)
                .eq("isDelete", 0);
        return (int) this.count(w);
    }

    @Override
    public long findEarliestMemberExcludingLeader(Long teamId, Long excludeUserId) {
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("userId")
                .eq("teamId", teamId)
                .ne("userId", excludeUserId)
                .orderByAsc("joinTime")
                .last("LIMIT 1");
        UserTeam userTeam = this.getOne(queryWrapper);
        if (userTeam == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "无候选队长");
        }
        return userTeam.getUserId();
    }
}