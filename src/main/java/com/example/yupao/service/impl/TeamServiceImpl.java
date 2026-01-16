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
import com.example.yupao.model.request.TeamJoinRequest;
import com.example.yupao.model.request.TeamQuitRequest;
import com.example.yupao.model.request.TeamUpdateRequest;
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
* @createDate  2025-12-27 16:35:50
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
        long userId = loginUser.getId();
        team.setUserId(userId);
        team.setId(null); // 确保自增 save() 方法内部会检查实体的 主键是否为“空值”（null 或 0），空值 → 插入；非空 → 更新
        String lockKey = String.format("yupao:team:create_team:%s", userId);
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean locked = lock.tryLock(1, 30, TimeUnit.SECONDS);
            if (!locked) {
                throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, "请稍后重试");
            }
            // 3.7 当前用户最多创建 5 个队伍
            long currentTeamCount = this.count(new QueryWrapper<Team>().eq("userId", userId));
            if (currentTeamCount >= 5) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "您最多只能创建 5 个队伍");
            }
            // 4.插入队伍信息
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean dismissTeam(Long id, User loginUser) {
        if (id == null || id <= 0 || loginUser == null || loginUser.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        Team team = getTeamById(id);
        Long teamLeaderId = team.getUserId();
        if (!Objects.equals(teamLeaderId, loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH, "您不是该队伍的队长");
        }

        // 条件逻辑删除队伍
        boolean teamUpdated = this.lambdaUpdate()
                .eq(Team::getId, id)
                .eq(Team::getIsDelete, 0)
                .set(Team::getIsDelete, 1)
                .set(Team::getUpdateTime, new Date())
                .update();

        if (!teamUpdated) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "解散队伍操作失败");
        }

        // 使用条件更新：只删有效的关联，避免重复操作影响行数
        boolean relationsUpdated = userTeamService.lambdaUpdate()
                .eq(UserTeam::getTeamId, id)
                .eq(UserTeam::getIsDelete, 0)
                .set(UserTeam::getIsDelete, 1)
                .set(UserTeam::getUpdateTime, new Date())
                .update();

        if (!relationsUpdated) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新用户队伍关联关系失败");
        }

        // 处理未更新成功的情况 MP 自动排除 isDelete=1，所以 null == 已解散
        Team current = this.getById(id);
        if (current == null) {
            return true;
        } else {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "队伍状态异常，请重试");
        }
    }

    private Team getTeamById(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = this.getById(id);
        if (team == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "队伍不存在");
        }
        return team;
    }

    @Override
    public boolean updateTeam(TeamUpdateRequest teamUpdateRequest, User loginUser) {
        if (teamUpdateRequest == null || loginUser == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 校验是否有权限（队长或管理员）
        Long teamId = teamUpdateRequest.getId();
        Team team = getTeamById(teamId);
        Long teamLeaderId = team.getUserId();
        if (!Objects.equals(teamLeaderId, loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "无权限更新队伍");
        }
        // 校验是否为加密队伍且填写密码
        Integer status = teamUpdateRequest.getStatus();
        TeamStatusEnum teamStatusEnum = TeamStatusEnum.getEnumByValue(status);
        String password = teamUpdateRequest.getPassword();
        if (TeamStatusEnum.SECRET.equals(teamStatusEnum)  && StringUtils.isBlank(password)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "加密队伍的密码不能为空");
        }
        Team updateTeam = new Team();
        BeanUtils.copyProperties(teamUpdateRequest, updateTeam);
        return this.updateById(updateTeam);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean joinTeam(TeamJoinRequest teamJoinRequest, User loginUser) {
        if (teamJoinRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long teamId = teamJoinRequest.getId();
        Long userId = loginUser.getId();
        Team team = getTeamById(teamId);
        if (team.getExpireTime() != null && team.getExpireTime().before(new Date())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍已过期");
        }
        Integer status = team.getStatus();
        TeamStatusEnum teamStatusEnum = TeamStatusEnum.getEnumByValue(status);
        if (Objects.equals(TeamStatusEnum.SECRET, teamStatusEnum) && !Objects.equals(team.getPassword(), teamJoinRequest.getPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId).eq("teamId", teamId);
        if (userTeamService.count(queryWrapper) > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "已加入该队伍");
        }
        String lockKey = String.format("yupao:team:join_team:%s", teamId);
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean locked = lock.tryLock(1, TimeUnit.SECONDS);
            if (locked) {
                try {
                    if (userTeamService.count(queryWrapper) > 0) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "已加入该队伍");
                    }
                    long joinedTeamNum = userTeamService.getJoinedTeamNum(userId);
                    if (joinedTeamNum >= 10) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "最多加入10个队伍");
                    }
                    long teamMemberCount = userTeamService.getTeamMemberCount(teamId);
                    if (teamMemberCount >= team.getMaxNum()) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍人数已满");
                    }
                    // 插入关联关系
                    UserTeam userTeam = new UserTeam();
                    userTeam.setUserId(userId);
                    userTeam.setTeamId(teamId);
                    userTeam.setJoinTime(new Date());
                    return userTeamService.save(userTeam);
                } finally {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
            } else {
                throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, "请稍后重试");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "加入队伍被中断");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean quitTeam(TeamQuitRequest teamQuitRequest, User loginUser) {
        if (teamQuitRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long teamId = teamQuitRequest.getId();
        Long userId = loginUser.getId();
        Team team = getTeamById(teamId);
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId).eq("teamId", teamId);
        if (userTeamService.count(queryWrapper) == 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "未加入该队伍");
        }
        String lockKey = String.format("yupao:team:quit_team:%s", teamId);
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean locked = lock.tryLock(1, TimeUnit.SECONDS);
            if (locked) {
                try {
                    if (userTeamService.count(queryWrapper) == 0) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "未加入该队伍");
                    }
                    long teamMemberCount = userTeamService.getTeamMemberCount(teamId);
                    if (teamMemberCount == 1) {
                        this.removeById(teamId);
                    }
                    if (teamMemberCount > 1 && Objects.equals(team.getUserId(), userId)) {
                        Long newTeamLeaderId = userTeamService.findEarliestMemberExcludingLeader(teamId, userId);
                        team.setUserId(newTeamLeaderId);
                        boolean updateSuccess = this.updateById(team);
                        if (!updateSuccess) {
                            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "队长移交失败");
                        }
                    }
                    return userTeamService.remove(queryWrapper);
                } finally {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
            } else {
                throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, "请稍后重试");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "退出队伍被中断");
        }
    }

    @Override
    public List<TeamVO> listTeamsMyCreated(TeamQuery teamQuery, User loginUser) {
        Long userId = loginUser.getId();
        QueryWrapper<Team> queryWrapper = bulidQueryWrapper(teamQuery);
        queryWrapper.eq("userId", userId);
        List<Team> teamList = this.list(queryWrapper);
        return toTeamVoList(teamList);
    }

    @Override
    public List<TeamVO> listTeamsMyJoined(TeamQuery teamQuery, User loginUser) {
        Long userId = loginUser.getId();
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        List<UserTeam> userTeamList = userTeamService.list(queryWrapper);
        if (CollectionUtils.isEmpty(userTeamList)) {
            return Collections.emptyList();
        }
        Set<Long> teamIds = userTeamList.stream()
                .map(UserTeam::getTeamId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        QueryWrapper<Team> teamQw = bulidQueryWrapper(teamQuery);
        teamQw.in("id", teamIds);
        List<Team> teamList = this.list(teamQw);
        return toTeamVoList(teamList);
    }
}