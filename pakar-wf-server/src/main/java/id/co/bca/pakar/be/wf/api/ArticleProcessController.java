package id.co.bca.pakar.be.wf.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.co.bca.pakar.be.wf.common.Constant;
import id.co.bca.pakar.be.wf.dto.ArticleDto;
import id.co.bca.pakar.be.wf.dto.RequestTaskDto;
import id.co.bca.pakar.be.wf.dto.TaskDto;
import id.co.bca.pakar.be.wf.exception.UndefinedProcessException;
import id.co.bca.pakar.be.wf.exception.UndefinedStartedStateException;
import id.co.bca.pakar.be.wf.exception.UndefinedUserTaskException;
import id.co.bca.pakar.be.wf.service.ArticleWorkflowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
public class ArticleProcessController extends BaseController {
    private static Logger logger = LoggerFactory.getLogger(ArticleProcessController.class);

    @Autowired
    private ArticleWorkflowService articleWorkflowService;

    /**
     * start workflow request
     *
     * @param authorization
     * @param username
     * @param articleDto
     */
    @PostMapping(value = "/api/wf/start", consumes = {MediaType.APPLICATION_JSON_VALUE}, produces = {
            MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<RestResponse<TaskDto>> start(@RequestHeader(name = "Authorization") String authorization, @RequestHeader(name = "X-USERNAME") String username, @RequestBody ArticleDto articleDto) {
        try {
            logger.info("receive request to start article workflow");
            ObjectMapper oMapper = new ObjectMapper();
            TaskDto taskDto = articleWorkflowService.startProcess(username, oMapper.convertValue(articleDto, Map.class));
            return createResponse(taskDto, Constant.ApiResponseCode.OK.getAction()[0], messageSource.getMessage("success.response", null, null));
        } catch (UndefinedUserTaskException e) {
            logger.error("exception", e);
            return createResponse(new TaskDto(), Constant.ApiResponseCode.GENERAL_ERROR.getAction()[0], messageSource.getMessage("usertask.not.found", null, null));
        } catch (UndefinedProcessException e) {
            logger.error("exception", e);
            return createResponse(new TaskDto(), Constant.ApiResponseCode.GENERAL_ERROR.getAction()[0], messageSource.getMessage("general.error", null, null));
        } catch (UndefinedStartedStateException e) {
            logger.error("exception", e);
            return createResponse(new TaskDto(), Constant.ApiResponseCode.GENERAL_ERROR.getAction()[0], messageSource.getMessage("general.error", null, null));
        } catch (Exception e) {
            logger.error("exception", e);
            return createResponse(new TaskDto(), Constant.ApiResponseCode.GENERAL_ERROR.getAction()[0], messageSource.getMessage("general.error", null, null));
        }
    }

    @PostMapping(value = "/api/wf/next", consumes = {MediaType.APPLICATION_JSON_VALUE}, produces = {
            MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<RestResponse<TaskDto>> next(@RequestHeader(name = "Authorization") String authorization, @RequestHeader(name = "X-USERNAME") String username, @RequestBody ArticleDto articleDto) {
        try {
            logger.info("receive request to process workflow");
            ObjectMapper oMapper = new ObjectMapper();
            TaskDto taskDto = articleWorkflowService.next(username, oMapper.convertValue(articleDto, Map.class));
            return createResponse(taskDto, Constant.ApiResponseCode.OK.getAction()[0], messageSource.getMessage("success.response", null, null));
        } catch (UndefinedUserTaskException e) {
            logger.error("exception", e);
            return createResponse(new TaskDto(), Constant.ApiResponseCode.GENERAL_ERROR.getAction()[0], messageSource.getMessage("usertask.not.found", null, null));
        } catch (UndefinedProcessException e) {
            logger.error("exception", e);
            return createResponse(new TaskDto(), Constant.ApiResponseCode.GENERAL_ERROR.getAction()[0], messageSource.getMessage("general.error", null, null));
        } catch (UndefinedStartedStateException e) {
            logger.error("exception", e);
            return createResponse(new TaskDto(), Constant.ApiResponseCode.GENERAL_ERROR.getAction()[0], messageSource.getMessage("general.error", null, null));
        } catch (Exception e) {
            logger.error("exception", e);
            return createResponse(new TaskDto(), Constant.ApiResponseCode.GENERAL_ERROR.getAction()[0], messageSource.getMessage("general.error", null, null));
        }
    }

    /**
     * API for get all task for certain assigne
     *
     * @param authorization
     * @param username
     * @param dto
     * @return
     */
    @PostMapping("/api/wf/tasks")
    public ResponseEntity<RestResponse<List<TaskDto>>> getTasks(@RequestHeader(name = "Authorization") String authorization, @RequestHeader(name = "X-USERNAME") String username, @RequestBody RequestTaskDto dto) {
        try {
            logger.info("get All Task");
            logger.info("received token bearer --- {}", authorization);
            dto.setUsername(username);
            dto.setToken(getTokenFromHeader(authorization));
            List<TaskDto> taskDtos = articleWorkflowService.getTasks(username);
            return createResponse(taskDtos, Constant.ApiResponseCode.OK.getAction()[0], messageSource.getMessage("success.response", null, Locale.ENGLISH));
        } catch (Exception e) {
            logger.error("exception", e);
            return createResponse(new ArrayList<TaskDto>(), Constant.ApiResponseCode.GENERAL_ERROR.getAction()[0], messageSource.getMessage("general.error", null, null));
        }
    }

    /**
     * API for get all task for certain assigne
     *
     * @param authorization
     * @param username
     * @param dto
     * @return
     */
    @PostMapping("/api/wf/tasksWithState")
    public ResponseEntity<RestResponse<List<TaskDto>>> getTasksWithState(@RequestHeader(name = "Authorization") String authorization, @RequestHeader(name = "X-USERNAME") String username, @RequestBody RequestTaskDto dto) {
        try {
            logger.info("get All Task");
            logger.info("received token bearer --- {}", authorization);
            dto.setUsername(username);
            dto.setToken(getTokenFromHeader(authorization));
            List<TaskDto> taskDtos = articleWorkflowService.getTasksWithPicState(username, dto.getState());
            return createResponse(taskDtos, Constant.ApiResponseCode.OK.getAction()[0], messageSource.getMessage("success.response", null, Locale.ENGLISH));
        } catch (Exception e) {
            logger.error("exception", e);
            return createResponse(new ArrayList<TaskDto>(), Constant.ApiResponseCode.GENERAL_ERROR.getAction()[0], messageSource.getMessage("general.error", null, null));
        }
    }

    @PostMapping("/api/wf/taskRequest")
    public ResponseEntity<RestResponse<List<TaskDto>>> getTaskRequest(@RequestHeader(name = "Authorization") String authorization, @RequestHeader(name = "X-USERNAME") String username, @RequestBody RequestTaskDto dto) {
        try {
            logger.info("get All Task");
            logger.info("received token bearer --- {}", authorization);
            dto.setUsername(username);
            dto.setToken(getTokenFromHeader(authorization));
            List<TaskDto> taskDtos = articleWorkflowService.getTasks(username);
            return createResponse(taskDtos, Constant.ApiResponseCode.OK.getAction()[0], messageSource.getMessage("success.response", null, Locale.ENGLISH));
        } catch (Exception e) {
            logger.error("exception", e);
            return createResponse(new ArrayList<TaskDto>(), Constant.ApiResponseCode.GENERAL_ERROR.getAction()[0], messageSource.getMessage("general.error", null, null));
        }
    }
}
