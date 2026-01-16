package com.example.yupao.controller;

import com.example.yupao.common.BaseResponse;
import com.example.yupao.common.ErrorCode;
import com.example.yupao.common.ResultUtils;
import com.example.yupao.exception.BusinessException;
import com.example.yupao.model.domain.Team;
import com.example.yupao.model.domain.User;
import com.example.yupao.model.dto.TeamQuery;
import com.example.yupao.model.request.*;
import com.example.yupao.model.vo.TeamVO;
import com.example.yupao.service.TeamService;
import com.example.yupao.service.UserService;
import com.example.yupao.service.UserTeamService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/team")
@CrossOrigin(origins = {"http://localhost:5173"})
@Slf4j
public class TeamController {

    @Resource
    private UserService userService;

    @Resource
    private TeamService teamService;

    @Resource
    private UserTeamService userTeamService;

    @PostMapping("/create")
    public BaseResponse<Long> createTeam(@RequestBody TeamCreateRequest teamCreateRequest, HttpServletRequest request) {
        if (teamCreateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        Team team = new Team();
        BeanUtils.copyProperties(teamCreateRequest, team);
        long teamId = teamService.createTeam(team, loginUser);
        return ResultUtils.success(teamId);
    }

    @GetMapping("/list")
    public BaseResponse<List<TeamVO>> listTeams(TeamQuery teamQuery, HttpServletRequest request) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        Long userId = loginUser.getId();
        List<TeamVO> teamList = teamService.listTeams(teamQuery);
        // 填充「是否已加入」→ hasJoined: Boolean
        userTeamService.fillHasJoined(teamList, userId);
        // 填充「已加入人数」→ memberCount: Integer
        userTeamService.fillMemberCount(teamList);
        return ResultUtils.success(teamList);
    }

    @PostMapping("/dismiss")
    public BaseResponse<Boolean> dismissTeam(@RequestBody TeamDeleteRequest teamDeleteRequest, HttpServletRequest request) {

        if (teamDeleteRequest == null || teamDeleteRequest.getId() == null || teamDeleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍ID非法");
        }
        Long id = teamDeleteRequest.getId();
        User loginUser = userService.getLoginUser(request);
        boolean result = teamService.dismissTeam(id, loginUser);
        return ResultUtils.success(result);
    }

    @PostMapping("/update")
    public BaseResponse<Boolean> updateTeam(@RequestBody TeamUpdateRequest teamUpdateRequest, HttpServletRequest request) {
        if (teamUpdateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        boolean result = teamService.updateTeam(teamUpdateRequest, loginUser);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新队伍失败");
        }
        return ResultUtils.success(true);
    }

    @PostMapping("/join")
    public BaseResponse<Boolean> joinTeam(@RequestBody TeamJoinRequest teamJoinRequest, HttpServletRequest request) {
        if (teamJoinRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        boolean result = teamService.joinTeam(teamJoinRequest, loginUser);
        return ResultUtils.success(result);
    }

    @PostMapping("/quit")
    public BaseResponse<Boolean> quitTeam(@RequestBody TeamQuitRequest teamQuitRequest, HttpServletRequest request) {
        if (teamQuitRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        boolean result = teamService.quitTeam(teamQuitRequest, loginUser);
        return ResultUtils.success(result);
    }

    @GetMapping("/list/my/create")
    public BaseResponse<List<TeamVO>> listTeamsMyCreated(TeamQuery teamQuery, HttpServletRequest request) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        Long userId = loginUser.getId();
        List<TeamVO> teamList = teamService.listTeamsMyCreated(teamQuery, loginUser);
        userTeamService.fillHasJoined(teamList, userId);
        userTeamService.fillMemberCount(teamList);
        return ResultUtils.success(teamList);
    }

    @GetMapping("/list/my/join")
    public BaseResponse<List<TeamVO>> listTeamsMyJoined(TeamQuery teamQuery, HttpServletRequest request) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        Long userId = loginUser.getId();
        List<TeamVO> teamList = teamService.listTeamsMyJoined(teamQuery, loginUser);
        userTeamService.fillHasJoined(teamList, userId);
        userTeamService.fillMemberCount(teamList);
        return ResultUtils.success(teamList);
    }
}
