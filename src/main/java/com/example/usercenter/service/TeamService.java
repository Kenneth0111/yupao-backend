package com.example.usercenter.service;

import com.example.usercenter.model.domain.Team;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.usercenter.model.domain.User;
import com.example.usercenter.model.dto.TeamQuery;
import com.example.usercenter.model.request.TeamJoinRequest;
import com.example.usercenter.model.request.TeamQuitRequest;
import com.example.usercenter.model.request.TeamUpdateRequest;
import com.example.usercenter.model.vo.TeamUserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author 张博洋
* @description 针对表【team(队伍表)】的数据库操作Service
* @createDate 2025-09-19 13:24:41
*/
public interface TeamService extends IService<Team> {

    /**
     * 创建队伍
     *
     * @param team
     * @param loginUser
     * @return
     */
    long addTeam(Team team, User loginUser);

    /**
     * 搜索队伍
     * @param teamQuery
     * @param isAdmin
     * @return
     */
    List<TeamUserVO> listTeams(TeamQuery teamQuery, boolean isAdmin);

    /**
     * 更新队伍信息
     * @param teamUpdateRequest
     * @param loginUser
     * @return
     */
    boolean updateTeam(TeamUpdateRequest teamUpdateRequest, User loginUser);

    /**
     * 加入队伍
     * @param teamJoinRequest
     * @param loginUser
     * @return
     */
    boolean joinTeam(TeamJoinRequest teamJoinRequest, User loginUser);

    /**
     * 退出队伍
     * @param teamQuitRequest
     * @param loginUser
     * @return
     */
    boolean quitTeam(TeamQuitRequest teamQuitRequest, User loginUser);

    /**
     * 删除解散队伍
     * @param id
     * @param loginUser
     * @return
     */
    boolean dismissTeam(Long id, User loginUser);
}
