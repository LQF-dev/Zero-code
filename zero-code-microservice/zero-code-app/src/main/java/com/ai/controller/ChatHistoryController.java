package com.ai.controller;

import com.ai.annotation.AuthCheck;
import com.ai.common.BaseResponse;
import com.ai.common.ResultUtils;
import com.ai.constant.UserConstant;
import com.ai.exception.BusinessException;
import com.ai.exception.ErrorCode;
import com.ai.exception.ThrowUtils;
import com.ai.innerservice.InnerUserService;
import com.ai.model.dto.chathistory.ChatHistoryQueryRequest;
import com.ai.model.entity.App;
import com.ai.model.entity.ChatEventLog;
import com.ai.model.entity.ChatHistory;
import com.ai.model.entity.User;
import com.ai.service.AppService;
import com.ai.service.ChatEventLogService;
import com.ai.service.ChatHistoryService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 对话历史 控制层。
 */
@RestController
@RequestMapping("/chatHistory")
public class ChatHistoryController {

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private ChatEventLogService chatEventLogService;

    @Resource
    private AppService appService;

    /**
     * 分页查询某个应用的对话历史（游标查询）。
     */
    @GetMapping("/app/{appId}")
    public BaseResponse<Page<ChatHistory>> listAppChatHistory(@PathVariable Long appId,
                                                              @RequestParam(defaultValue = "10") int pageSize,
                                                              @RequestParam(required = false) LocalDateTime lastCreateTime,
                                                              HttpServletRequest request) {
        User loginUser = InnerUserService.getLoginUser(request);
        Page<ChatHistory> result =
                chatHistoryService.listAppChatHistoryByPage(appId, pageSize, lastCreateTime, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 查询某一轮对话的结构化事件（用于前端展开查看推理 / 工具过程）。
     */
    @GetMapping("/turn/{turnId}/events")
    public BaseResponse<List<ChatEventLog>> listTurnEvents(@PathVariable String turnId,
                                                           HttpServletRequest request) {
        ThrowUtils.throwIf(turnId == null || turnId.isBlank(), ErrorCode.PARAMS_ERROR, "turnId 不能为空");
        User loginUser = InnerUserService.getLoginUser(request);
        List<ChatEventLog> eventLogs = chatEventLogService.listEventsByTurnId(turnId);
        if (eventLogs.isEmpty()) {
            return ResultUtils.success(List.of());
        }
        Long appId = eventLogs.get(0).getAppId();
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        boolean isAdmin = UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole());
        boolean isCreator = app.getUserId().equals(loginUser.getId());
        if (!isAdmin && !isCreator) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权查看该轮对话事件");
        }
        return ResultUtils.success(eventLogs);
    }

    /**
     * 管理员分页查询所有对话历史。
     */
    @PostMapping("/admin/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<ChatHistory>> listAllChatHistoryByPageForAdmin(
            @RequestBody ChatHistoryQueryRequest chatHistoryQueryRequest) {
        ThrowUtils.throwIf(chatHistoryQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = chatHistoryQueryRequest.getPageNum();
        long pageSize = chatHistoryQueryRequest.getPageSize();
        QueryWrapper queryWrapper = chatHistoryService.getQueryWrapper(chatHistoryQueryRequest);
        Page<ChatHistory> result = chatHistoryService.page(Page.of(pageNum, pageSize), queryWrapper);
        return ResultUtils.success(result);
    }
}
