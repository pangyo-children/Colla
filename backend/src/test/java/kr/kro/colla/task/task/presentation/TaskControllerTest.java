package kr.kro.colla.task.task.presentation;

import io.restassured.mapper.ObjectMapper;
import kr.kro.colla.common.ControllerTest;
import kr.kro.colla.task.task.presentation.dto.CreateTaskRequest;
import kr.kro.colla.task.task.presentation.dto.ProjectTaskResponse;
import kr.kro.colla.task.task.presentation.dto.UpdateTaskStatusRequest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import javax.servlet.http.Cookie;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
class TaskControllerTest extends ControllerTest {

    @Test
    void 프로젝트에_새로운_태스크를_등록한다() throws Exception {
        // given
        Long taskId = 1L;
        given(taskService.createTask(any(CreateTaskRequest.class)))
                .willReturn(taskId);

        // when
        ResultActions perform = mockMvc.perform(post("/projects/tasks")
                .cookie(new Cookie("accessToken", accessToken))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .param("title", "task title")
                .param("priority", "3")
                .param("status", "To Do")
                .param("projectId", "1"));

        // then
        MvcResult result = perform.andReturn();

        perform.andExpect(status().isCreated());
        assertThat(result.getResponse().getHeader(HttpHeaders.LOCATION)).isEqualTo("/api/projects/tasks/" + taskId);
    }

    @Test
    void 프로젝트에_속한_태스크를_조회한다() throws Exception {
        // given
        Long taskId = 1L;
        ProjectTaskResponse projectTaskResponse = ProjectTaskResponse.builder()
                .id(taskId)
                .title("task title")
                .description("task description")
                .story("user can login with github")
                .preTasks("[]")
                .manager("kykapple")
                .status("In Progress")
                .priority(4)
                .tags(List.of("backend", "frontend"))
                .build();

        given(taskService.getTask(eq(taskId)))
                .willReturn(projectTaskResponse);

        // when
        ResultActions perform = mockMvc.perform(get("/projects/tasks/" + taskId)
                .cookie(new Cookie("accessToken", accessToken))
                .contentType(MediaType.APPLICATION_JSON));

        // then
        perform
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(projectTaskResponse.getTitle()))
                .andExpect(jsonPath("$.description").value(projectTaskResponse.getDescription()))
                .andExpect(jsonPath("$.status").value(projectTaskResponse.getStatus()))
                .andExpect(jsonPath("$.priority").exists())
                .andExpect(jsonPath("$.tags").isArray());
    }

    @Test
    void 프로젝트에_속한_테스크의_상태값을_수정한다() throws Exception {
        // given
        Long taskId = 13494L;
        String statusNameToUpdate = "새로운~상태~값~입니다~";
        UpdateTaskStatusRequest request = new UpdateTaskStatusRequest(statusNameToUpdate);
        // when
        ResultActions perform = mockMvc.perform(patch("/projects/tasks/"+taskId)
                .cookie(new Cookie("accessToken", accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
        // then
        perform
                .andExpect(status().isOk());
        verify(taskService, times(1)).updateTaskStatus(taskId, statusNameToUpdate);
    }

    @Test
    void 상태값_이름이_부족하면_테스크_상태를_수정할_수_없다() throws Exception {
        // given
        Long taskId = 13494L;
        UpdateTaskStatusRequest request = new UpdateTaskStatusRequest();
        // when
        ResultActions perform = mockMvc.perform(patch("/projects/tasks/"+taskId)
                .cookie(new Cookie("accessToken", accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
        // then
        perform
                .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.message").value("statusName : must not be null"));
        verify(taskService, times(0)).updateTaskStatus(eq(taskId), anyString());

    }
}
