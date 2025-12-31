package com.example.yupao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.yupao.common.ErrorCode;
import com.example.yupao.exception.BusinessException;
import com.example.yupao.model.domain.Team;
import com.example.yupao.mapper.TeamMapper;
import com.example.yupao.model.domain.User;
import com.example.yupao.model.domain.UserTeam;
import com.example.yupao.model.dto.TeamQuery;
import com.example.yupao.model.enums.TeamStatusEnum;
import com.example.yupao.model.vo.TeamVO;
import com.example.yupao.model.vo.UserVO;
import com.example.yupao.service.TeamService;
import com.example.yupao.service.UserService;
import com.example.yupao.service.UserTeamService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
* @author 张博洋
* @description 针对表【team(队伍表)】的数据库操作Service实现
* @createDate 2025-12-27 16:35:50
*/
@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team>
    implements TeamService {

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private UserTeamService userTeamService;

    @Resource
    private UserService userService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long createTeam(Team team, User loginUser) {
        // 1.请求参数是否为空
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 2.用户是否登录
        if(loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        long userId = loginUser.getId();
        String lockKey = String.format("yupao:team:create_team:%s", userId);
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean locked = lock.tryLock(1, 30, TimeUnit.SECONDS);
            if (!locked) {
                throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, "请稍后重试");
            }
            // 3.1 队伍标题 >= 1 且 <= 20
            String name = team.getName();
            if (StringUtils.isBlank(name) || name.length() > 20) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍标题需为 1~20 位有效字符");
            }
            // 3.2 队伍描述 可空 且 <= 512
            String description = team.getDescription();
            if (StringUtils.isNotBlank(description) && description.length() > 512) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍描述不能超过 512 字符");
            }
            // 3.3 队伍最大人数 >= 1 且 <= 20
            Integer maxNum = team.getMaxNum();
            if (maxNum == null || maxNum < 1 || maxNum > 20) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍人数需为 1~20 人");
            }
            // 3.4 超时时间 > 当前时间
            Date expireTime = team.getExpireTime();
            if (expireTime == null || new Date().after(expireTime)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "超时时间需大于当前时间");
            }
            // 3.5 status 不传默认为 0（公开）
            Integer status = Optional.ofNullable(team.getStatus()).orElse(0);
            TeamStatusEnum teamStatusEnum = TeamStatusEnum.getEnumByValue(status);
            if (teamStatusEnum == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "无效的队伍状态");
            }
            // 3.6 如果 status 是加密状态，一定要有密码 >= 1 且 <= 32
            if (teamStatusEnum == TeamStatusEnum.SECRET) {
                String password = team.getPassword();
                if (StringUtils.isBlank(password) || password.length() > 32) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "加密队伍需设置 1~32 位密码");
                }
            }
            // 3.7 当前用户最多创建 5 个队伍
            long currentTeamCount = this.count(new QueryWrapper<Team>().eq("userId", userId));
            if (currentTeamCount >= 5) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "您最多只能创建 5 个队伍");
            }
            // 4.插入队伍信息
            team.setUserId(userId);
            team.setId(null); // 确保自增 save() 方法内部会检查实体的 主键是否为“空值”（null 或 0），空值 → 插入；非空 → 更新
            boolean teamSaved = this.save(team);
            Long teamId = team.getId();
            if (!teamSaved || teamId == null) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建队伍失败");
            }
            // 5.插入用户队伍关系
            UserTeam userTeam = new UserTeam();
            userTeam.setTeamId(teamId);
            userTeam.setUserId(userId);
            userTeam.setJoinTime(new Date());
            boolean relationSaved = userTeamService.save(userTeam);
            if (!relationSaved) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建队伍失败");
            }
            // 6.返回teamId
            return teamId;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // 恢复中断状态
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取锁被中断");
        } finally {
            // 释放自己的锁
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public List<TeamVO> listTeams(TeamQuery teamQuery) {
        QueryWrapper<Team> qw = bulidQueryWrapper(teamQuery);
        List<Team> teamList = this.list(qw);
        return toTeamVoList(teamList);
    }

    private QueryWrapper<Team> bulidQueryWrapper(TeamQuery teamQuery) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QueryWrapper<Team> qw = new QueryWrapper<>();
        Integer status = teamQuery.getStatus();
        if (status != null) {
            if (status == 0 || status == 1) {
                qw.eq("status", status);
            } else {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "无效的队伍状态");
            }
        }
        String searchText = teamQuery.getSearchText();
        if (StringUtils.isNotBlank(searchText)) {
            qw.and(q -> q.like("name", searchText).or().like("description", searchText));
        }
        qw.and(q -> q.gt("expireTime", new Date()).or().isNull("expireTime"));
        return qw;
    }

    private List<TeamVO> toTeamVoList(List<Team> teamList) {
        if (CollectionUtils.isEmpty(teamList)) {
            return Collections.emptyList();
        }
        // 批量查询创建人
        Set<Long> creatorIds = teamList.stream()
                .map(Team::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, UserVO> userVOMap;
        if (!creatorIds.isEmpty()) {
            List<User> users = userService.listByIds(creatorIds);
            userVOMap = users.stream()
                    .collect(Collectors.toMap(User::getId, this::toUserVO, (u1, u2) -> u1));
        } else {
            userVOMap = new HashMap<>();
        }

        return teamList.stream().map(team -> {
            TeamVO vo = new TeamVO();
            BeanUtils.copyProperties(team, vo);
            vo.setCreatorUser(userVOMap.get(team.getUserId()));
            return vo;
        }).toList();
    }

    private UserVO toUserVO(User user) {
        if (user == null) return null;
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        vo.setPhone(null);
        vo.setEmail(null);
        return vo;
    }

}




