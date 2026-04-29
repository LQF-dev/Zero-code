package com.ai.zerocode.controller;

import com.ai.zerocode.annotation.AuthCheck;
import com.ai.zerocode.common.BaseResponse;
import com.ai.zerocode.common.ResultUtils;
import com.ai.zerocode.constant.UserConstant;
import com.ai.zerocode.exception.BusinessException;
import com.ai.zerocode.exception.ErrorCode;
import com.ai.zerocode.exception.ThrowUtils;
import com.ai.zerocode.model.dto.chathistory.ChatHistoryQueryRequest;
import com.ai.zerocode.model.entity.App;
import com.ai.zerocode.model.entity.ChatEventLog;
import com.ai.zerocode.model.entity.ChatHistory;
import com.ai.zerocode.model.entity.User;
import com.ai.zerocode.service.AppService;
import com.ai.zerocode.service.ChatEventLogService;
import com.ai.zerocode.service.ChatHistoryService;
import com.ai.zerocode.service.UserService;
import com.mybatisflex.core.paginate.Page;

import com.mybatisflex.core.query.QueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 对话历史 控制层。
 *
 * @author Luo QinFeng
 */
@RestController
@RequestMapping("/chatHistory")
public class ChatHistoryController {

    @Autowired
    private ChatHistoryService chatHistoryService;
    @Autowired
    private ChatEventLogService chatEventLogService;
    @Autowired
    private AppService appService;
    @Autowired
    private UserService userService;

    /**
     * 保存对话历史。
     *
     * @param chatHistory 对话历史
     * @return {@code true} 保存成功，{@code false} 保存失败
     */
    @PostMapping("save")
    public boolean save(@RequestBody ChatHistory chatHistory) {
        return chatHistoryService.save(chatHistory);
    }

    /**
     * 根据主键删除对话历史。
     *
     * @param id 主键
     * @return {@code true} 删除成功，{@code false} 删除失败
     */
    @DeleteMapping("remove/{id}")
    public boolean remove(@PathVariable Long id) {
        return chatHistoryService.removeById(id);
    }

    /**
     * 根据主键更新对话历史。
     *
     * @param chatHistory 对话历史
     * @return {@code true} 更新成功，{@code false} 更新失败
     */
    @PutMapping("update")
    public boolean update(@RequestBody ChatHistory chatHistory) {
        return chatHistoryService.updateById(chatHistory);
    }

    /**
     * 查询所有对话历史。
     *
     * @return 所有数据
     */
    @GetMapping("list")
    public List<ChatHistory> list() {
        return chatHistoryService.list();
    }

    /**
     * 根据主键获取对话历史。
     *
     * @param id 对话历史主键
     * @return 对话历史详情
     */
    @GetMapping("getInfo/{id}")
    public ChatHistory getInfo(@PathVariable Long id) {
        return chatHistoryService.getById(id);
    }

    /**
     * 分页查询对话历史。
     *
     * @param page 分页对象
     * @return 分页对象
     */
    @GetMapping("page")
    public Page<ChatHistory> page(Page<ChatHistory> page) {
        return chatHistoryService.page(page);
    }


    /**
     * 分页查询某个应用的对话历史（游标查询）
     *
     * @param appId          应用ID
     * @param pageSize       页面大小
     * @param lastCreateTime 最后一条记录的创建时间
     * @param request        请求
     * @return 对话历史分页
     */
    @GetMapping("/app/{appId}")
    public BaseResponse<Page<ChatHistory>> listAppChatHistory(@PathVariable Long appId,
                                                              @RequestParam(defaultValue = "10") int pageSize,
                                                              @RequestParam(required = false) LocalDateTime lastCreateTime,
                                                              HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Page<ChatHistory> result = chatHistoryService.listAppChatHistoryByPage(appId, pageSize, lastCreateTime, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 查询某一轮对话的结构化事件（用于前端展开查看推理/工具过程）
     */
    @GetMapping("/turn/{turnId}/events")
    public BaseResponse<List<ChatEventLog>> listTurnEvents(@PathVariable String turnId,
                                                            HttpServletRequest request) {
        ThrowUtils.throwIf(turnId == null || turnId.isBlank(), ErrorCode.PARAMS_ERROR, "turnId 不能为空");
        User loginUser = userService.getLoginUser(request);
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
     * 管理员分页查询所有对话历史
     *
     * @param chatHistoryQueryRequest 查询请求
     * @return 对话历史分页
     */
    @PostMapping("/admin/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<ChatHistory>> listAllChatHistoryByPageForAdmin(@RequestBody ChatHistoryQueryRequest chatHistoryQueryRequest) {
        ThrowUtils.throwIf(chatHistoryQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = chatHistoryQueryRequest.getPageNum();
        long pageSize = chatHistoryQueryRequest.getPageSize();
        // 查询数据
        QueryWrapper queryWrapper = chatHistoryService.getQueryWrapper(chatHistoryQueryRequest);
        Page<ChatHistory> result = chatHistoryService.page(Page.of(pageNum, pageSize), queryWrapper);
        return ResultUtils.success(result);
    }


}
