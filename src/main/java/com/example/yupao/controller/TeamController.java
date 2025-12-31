package com.example.yupao.controller;

import com.example.yupao.common.BaseResponse;
import com.example.yupao.common.ErrorCode;
import com.example.yupao.common.ResultUtils;
import com.example.yupao.exception.BusinessException;
import com.example.yupao.model.domain.Team;
import com.example.yupao.model.domain.User;
import com.example.yupao.model.dto.TeamQuery;
import com.example.yupao.model.request.TeamCreateRequest;
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
}
