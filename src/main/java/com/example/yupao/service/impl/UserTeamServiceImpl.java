package com.example.yupao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
        if (CollectionUtils.isEmpty(teamVOList)) {
            return;
        }

        List<Long> teamIdList = teamVOList.stream()
                .map(TeamVO::getId)
                .filter(Objects::nonNull)
                .toList();

        if (userId == null || teamIdList.isEmpty()) {
            teamVOList.forEach(team -> team.setHasJoined(false));
            return;
        }

        QueryWrapper<UserTeam> qw = new QueryWrapper<>();
        qw.select("teamId")
                .eq("userId", userId)
                .in("teamId", teamIdList);

        List<UserTeam> joinRecords = this.list(qw);
        Set<Long> joinedTeamIds = joinRecords.stream()
                .map(UserTeam::getTeamId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        teamVOList.forEach(team ->
                team.setHasJoined(joinedTeamIds.contains(team.getId()))
        );
    }

    /**
     * 已加入队伍的人数
     *
     * @param teamVOList 队伍列表
     */
    public void fillMemberCount(List<TeamVO> teamVOList) {
        if (CollectionUtils.isEmpty(teamVOList)) {
            return;
        }

        List<Long> teamIdList = teamVOList.stream()
                .map(TeamVO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (teamIdList.isEmpty()) {
            teamVOList.forEach(team -> team.setMemberCount(0));
            return;
        }

        // 批量查询所有队伍的成员关系
        QueryWrapper<UserTeam> qw = new QueryWrapper<>();
        qw.select("teamId")
                .in("teamId", teamIdList);

        List<UserTeam> userTeams = this.list(qw);

        // 按 teamId 分组统计人数
        Map<Long, Long> teamIdToJoinCount = userTeams.stream()
                .collect(Collectors.groupingBy(
                        UserTeam::getTeamId,
                        Collectors.counting()
                ));

        // 填充 memberCount（未查到则为 0）
        teamVOList.forEach(teamVO ->
                teamVO.setMemberCount(teamIdToJoinCount.getOrDefault(teamVO.getId(), 0L).intValue())
        );
    }

}




