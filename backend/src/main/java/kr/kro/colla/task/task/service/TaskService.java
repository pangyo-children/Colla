package kr.kro.colla.task.task.service;

import kr.kro.colla.exception.exception.task.TaskNotFoundException;
import kr.kro.colla.project.project.domain.Project;
import kr.kro.colla.project.project.service.ProjectService;
import kr.kro.colla.project.task_status.domain.TaskStatus;
import kr.kro.colla.project.task_status.service.TaskStatusService;
import kr.kro.colla.story.domain.Story;
import kr.kro.colla.story.service.StoryService;
import kr.kro.colla.task.task.domain.Task;
import kr.kro.colla.task.task.domain.repository.TaskRepository;
import kr.kro.colla.task.task.domain.repository.dto.TaskCountByStatus;
import kr.kro.colla.task.task.presentation.dto.*;
import kr.kro.colla.task.task.service.converter.TaskResponseConverter;
import kr.kro.colla.task.task_status_log.service.TaskStatusLogService;
import kr.kro.colla.task.task_tag.domain.TaskTag;
import kr.kro.colla.task.task_tag.service.TaskTagService;
import kr.kro.colla.user.user.domain.User;
import kr.kro.colla.user.user.service.UserService;
import kr.kro.colla.utils.ProxyInitialized;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Transactional
@Service
public class TaskService {

    private final UserService userService;
    private final StoryService storyService;
    private final ProjectService projectService;
    private final TaskTagService taskTagService;
    private final TaskStatusService taskStatusService;
    private final TaskStatusLogService taskStatusLogService;
    private final TaskRepository taskRepository;

    @CacheEvict(value = "Project", key = "#createTaskRequest.projectId")
    public Long createTask(CreateTaskRequest createTaskRequest) {
        Project project = projectService.findProjectById(createTaskRequest.getProjectId());
        Story story = !createTaskRequest.getStory().isBlank()
                ? storyService.findStoryByTitle(createTaskRequest.getStory())
                : null;
        TaskStatus taskStatus = taskStatusService.findTaskStatusByName(createTaskRequest.getStatus());

        Task task = Task.builder()
                .title(createTaskRequest.getTitle())
                .managerId(createTaskRequest.getManagerId())
                .description(createTaskRequest.getDescription())
                .priority(createTaskRequest.getPriority())
                .project(project)
                .taskStatus(taskStatus)
                .story(story)
                .preTasks(createTaskRequest.getPreTasks())
                .build();

        List<TaskTag> tags = taskTagService.translateTaskTags(task, createTaskRequest.getTags());
        task.addTags(tags);

        task = taskRepository.save(task);
        taskStatusLogService.writeTaskStatusLog(project, taskStatus);

        return task.getId();
    }

    public ProjectTaskResponse getTask(Long taskId) {
        Task task = findTaskById(taskId);
        User manager = task.getManagerId() != null
                ? userService.findUserById(task.getManagerId())
                : null;

        return TaskResponseConverter.convertToProjectTaskResponse(task, manager);
    }

    public void updateTask(Long taskId, UpdateTaskRequest updateTaskRequest) {
        Task task = findTaskById(taskId);
        String title = task.getStory() != null ? task.getStory().getTitle() : null;
        List<TaskTag> taskTags = taskTagService.translateTaskTags(task, updateTaskRequest.getTags());

        task.updateContents(updateTaskRequest);
        task.updateTags(taskTags);

        if (title == null && !updateTaskRequest.getStory().isBlank()
                || title != null && !title.equals(updateTaskRequest.getStory())) {
            Story story = storyService.findStoryByTitle(updateTaskRequest.getStory());
            task.updateStory(story);
        }
    }

    public void deleteTaskStatus(Long projectId, String from, String to) {
        TaskStatus fromTaskStatus = taskStatusService.findTaskStatusByName(from);
        TaskStatus toTaskStatus = taskStatusService.findTaskStatusByName(to);
        int count = taskRepository.bulkUpdateTaskStatusToAnother(fromTaskStatus, toTaskStatus);

        Project project = projectService.findProjectById(projectId);
        project.removeStatus(fromTaskStatus);
        taskStatusLogService.updateTaskStatusLogForTaskStatusDeletion(project, toTaskStatus, count);
    }

    public void updateTaskStatus(Long projectId, Long taskId, String statusName) {
        Project project = projectService.findProjectById(projectId);
        Task task = findTaskById(taskId);
        TaskStatus prevStatus = task.getTaskStatus();
        TaskStatus newTaskStatus = taskStatusService.findTaskStatusByName(statusName);

        task.updateTaskStatus(newTaskStatus);
        taskStatusLogService.updateTaskStatusLog(project, task, prevStatus, newTaskStatus);
    }

    @ProxyInitialized(target ="ProjectMember")
    public List<RoadmapTaskResponse> getStoryTasks(Long projectId, Long storyId) {
        Story story = storyService.findStoryById(storyId);
        List<Task> taskList = taskRepository.findStoryTasks(story);

        return taskList.stream()
                .map(task -> {
                    User manager = task.getManagerId() != null
                            ? userService.findUserById(task.getManagerId())
                            : null;

                    return TaskResponseConverter.convertToRoadmapTaskResponse(task, manager);
                }).collect(Collectors.toList());
    }

    @ProxyInitialized(target = "ProjectMemberAndTaskStatus")
    public List<ProjectTaskSimpleResponse> getTasksOrderByCreatedDate(Long projectId, Boolean ascending) {
        Project project = projectService.findProjectById(projectId);

        List<Task> taskList = ascending
                ? taskRepository.findAllOrderByCreatedAtAsc(project)
                : taskRepository.findAllOrderByCreatedAtDesc(project);

        return taskList.stream()
                .map(task -> {
                    User manager = task.getManagerId() != null
                            ? userService.findUserById(task.getManagerId())
                            : null;

                    return TaskResponseConverter.convertToProjectTaskSimpleResponse(task, manager);
                }).collect(Collectors.toList());
    }

    @ProxyInitialized(target = "ProjectMemberAndTaskStatus")
    public List<ProjectTaskSimpleResponse> getTasksOrderByPriority(Long projectId, Boolean ascending) {
        Project project = projectService.findProjectById(projectId);

        List<Task> taskList = ascending
                ? taskRepository.findAllOrderByPriorityAsc(project)
                : taskRepository.findAllOrderByPriorityDesc(project);

        return taskList.stream()
                .map(task -> {
                    User manager = task.getManagerId() != null
                            ? userService.findUserById(task.getManagerId())
                            : null;

                    return TaskResponseConverter.convertToProjectTaskSimpleResponse(task, manager);
                }).collect(Collectors.toList());
    }

    @ProxyInitialized(target = "ProjectMemberAndTaskStatus")
    public List<ProjectTaskSimpleResponse> getTasksFilterByTags(Long projectId, List<String> tags) {
        Project project = projectService.findProjectById(projectId);
        List<Task> taskList = taskRepository.findAllOrderByCreatedAtDesc(project);

        return taskList.stream()
                .filter(task -> {
                    List<String> taskTags = task.getTaskTags()
                            .stream()
                            .map(taskTag -> taskTag.getTag().getName())
                            .sorted()
                            .collect(Collectors.toList());

                    return taskTags.containsAll(tags);
                }).map(task -> {
                    User manager = task.getManagerId() != null
                            ? userService.findUserById(task.getManagerId())
                            : null;

                    return TaskResponseConverter.convertToProjectTaskSimpleResponse(task, manager);
                }).collect(Collectors.toList());
    }

    @ProxyInitialized(target = "ProjectMemberAndTaskStatus")
    public List<ProjectTaskSimpleResponse> getTasksFilterByStatus(Long projectId, List<String> statuses) {
        Project project = projectService.findProjectById(projectId);

        List<Task> taskList = taskRepository.findAllFilterByTaskStatus(project, statuses);

        return taskList.stream()
                .map(task -> {
                    User manager = task.getManagerId() != null
                            ? userService.findUserById(task.getManagerId())
                            : null;

                    return TaskResponseConverter.convertToProjectTaskSimpleResponse(task, manager);
                }).collect(Collectors.toList());
    }

    @ProxyInitialized(target = "ProjectMemberAndTaskStatus")
    public List<ProjectTaskSimpleResponse> getTasksFilterByManager(Long projectId, List<Long> managers, Boolean notSelected) {
        Project project = projectService.findProjectById(projectId);

        List<Task> taskList = taskRepository.findAllFilterByManager(project, managers, notSelected);

        return taskList.stream()
                .map(task -> {
                    User manager = task.getManagerId() != null
                            ? userService.findUserById(task.getManagerId())
                            : null;

                    return TaskResponseConverter.convertToProjectTaskSimpleResponse(task, manager);
                }).collect(Collectors.toList());
    }

    @ProxyInitialized(target = "ProjectInfo")
    public List<ProjectStoryTaskResponse> getTasksGroupByStory(Long projectId) {
        Project project = projectService.findProjectById(projectId);
        List<Task> taskList = taskRepository.findAllOrderByCreatedAtDesc(project);

        List<ProjectStoryTaskResponse> projectStoryTaskResponseList = new ArrayList<>();
        Map<String, List<ProjectTaskSimpleResponse>> taskMap = new HashMap<>();
        List<ProjectTaskSimpleResponse> emptyStoryTaskList = new ArrayList<>();

        for (Story story : project.getStories()) {
            String title = story.getTitle();
            taskMap.put(title, new ArrayList<>());
            projectStoryTaskResponseList.add(new ProjectStoryTaskResponse(title, taskMap.get(title)));
        }

        for (Task task : taskList) {
            String story = task.getStory() != null
                    ? task.getStory().getTitle()
                    : null;
            User manager = task.getManagerId() != null
                    ? userService.findUserById(task.getManagerId())
                    : null;
            ProjectTaskSimpleResponse projectTaskSimpleResponse = TaskResponseConverter.convertToProjectTaskSimpleResponse(task, manager);

            if (story == null) {
                emptyStoryTaskList.add(projectTaskSimpleResponse);
                continue;
            }

            taskMap.get(story).add(projectTaskSimpleResponse);
        }
        projectStoryTaskResponseList.add(new ProjectStoryTaskResponse(null, emptyStoryTaskList));

        return projectStoryTaskResponseList;
    }

    @ProxyInitialized(target = "ProjectMemberAndTaskStatus")
    public List<ProjectTaskSimpleResponse> searchTasksByKeyword(Long projectId, String keyword) {
        Project project = projectService.findProjectById(projectId);
        List<Task> taskList = taskRepository.findTasksSearchByKeyword(project, keyword);

        return taskList.stream()
                .map(task -> {
                    User manager = task.getManagerId() != null
                            ? userService.findUserById(task.getManagerId())
                            : null;

                    return TaskResponseConverter.convertToProjectTaskSimpleResponse(task, manager);
                }).collect(Collectors.toList());
    }

    public Task findTaskById(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(TaskNotFoundException::new);
    }

    public List<TaskCountResponse> getTaskCountsByStatus(Long projectId) {
        Project project = projectService.findProjectById(projectId);
        List<TaskCountByStatus> taskCntList = taskRepository.groupByTaskStatus(project);

        return taskCntList.stream()
                .map(TaskCountResponse::new)
                .collect(Collectors.toList());
    }

    public List<ManagerTaskCountResponse> getTaskCountsByManagerAndStatus(Long projectId) {
        Project project = projectService.findProjectById(projectId);
        List<TaskCountByStatus> taskCntList = taskRepository.groupByTaskStatusAndManager(project);

        Map<String, List<TaskCountResponse>> byManager = new HashMap<>();
        taskCntList.forEach(taskCnt -> {
                    String managerName = taskCnt.getManager()!=null ? taskCnt.getManager() : "담당자 없음";
                    byManager.putIfAbsent(managerName, new ArrayList<>());
                    byManager.get(managerName).add(new TaskCountResponse(taskCnt));
                });

        return byManager.entrySet().stream()
                .map(e -> new ManagerTaskCountResponse(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }
}
