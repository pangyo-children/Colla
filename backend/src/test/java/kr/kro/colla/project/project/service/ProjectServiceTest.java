package kr.kro.colla.project.project.service;

import kr.kro.colla.common.fixture.FileProvider;
import kr.kro.colla.project.project.domain.Project;
import kr.kro.colla.project.project.domain.profile.ProjectProfileStorage;
import kr.kro.colla.project.project.domain.repository.ProjectRepository;
import kr.kro.colla.project.project.presentation.dto.ProjectResponse;
import kr.kro.colla.project.project.presentation.dto.ProjectStoryResponse;
import kr.kro.colla.project.task_status.domain.TaskStatus;
import kr.kro.colla.story.domain.Story;
import kr.kro.colla.user.user.domain.User;
import kr.kro.colla.user.user.presentation.dto.CreateProjectRequest;
import kr.kro.colla.user_project.domain.UserProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectProfileStorage projectProfileStorage;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private ProjectService projectService;

    private Long id = 1L, managerId = 1L;
    private String name = "프로젝트 이름", desc = "프로젝트 설명";

    @Test
    void 프로젝트_생성을_성공한다() {
        // given
        String fileName = "thumbnail.png";
        MultipartFile thumbnail1 = FileProvider.getTestMultipartFile(fileName);
        CreateProjectRequest request = CreateProjectRequest.builder()
                .name(name)
                .description(desc)
                .thumbnail(thumbnail1)
                .build();
        Project project = Project.builder()
                .managerId(managerId)
                .name(name)
                .description(desc)
                .thumbnail(FileProvider.extractImageUrl(thumbnail1))
                .build();
        ReflectionTestUtils.setField(project, "id", id);
        ReflectionTestUtils.setField(project, "taskStatuses", List.of("To do", "In progress", "Done"));

        given(projectProfileStorage.upload(any(MultipartFile.class)))
                .willReturn(FileProvider.extractImageUrl(thumbnail1));
        given(projectRepository.save(any(Project.class)))
                .willReturn(project);

        // when
        Project result = projectService.createProject(managerId, request);

        // then
        assertThat(result.getId()).isNotNull();
        assertThat(result.getManagerId()).isEqualTo(managerId);
        assertThat(result.getName()).isEqualTo(name);
        assertThat(result.getDescription()).isEqualTo(desc);
        assertThat(result.getThumbnail()).isEqualTo(FileProvider.extractImageUrl(thumbnail1));
        assertThat(result.getTaskStatuses().size()).isEqualTo(3);

    }

    @Test
    void projectId에_해당하는_프로젝트를_조회한다() {
        // given
        String fileName = "thumbnail.png";
        MultipartFile thumbnail1 = FileProvider.getTestMultipartFile(fileName);
        User user = User.builder()
                .name("kykapple")
                .githubId("kykapple")
                .avatar("github_content")
                .build();
        Project project = Project.builder()
                .managerId(managerId)
                .name(name)
                .description(desc)
                .thumbnail(FileProvider.extractImageUrl(thumbnail1))
                .build();
        ReflectionTestUtils.setField(project, "taskStatuses", List.of(
                new TaskStatus("To Do"),
                new TaskStatus("In Progress"),
                new TaskStatus("Done")
        ));
        ReflectionTestUtils.setField(project, "members", List.of(new UserProject(user, project)));

        given(projectRepository.findById(eq(id)))
                .willReturn(Optional.of(project));

        // when
        ProjectResponse result = projectService.getProject(id);

        // then
        assertThat(result.getName()).isEqualTo(project.getName());
        assertThat(result.getDescription()).isEqualTo(project.getDescription());
        assertThat(result.getMembers().size()).isOne();
        assertThat(result.getMembers().get(0).getGithubId()).isEqualTo(user.getGithubId());
        assertThat(result.getTasks().size()).isEqualTo(3);
        assertThat(result.getTasks().get("Done")).isEmpty();
    }

    @Test
    void 프로젝트의_스토리를_조회한다() {
        // given
        String fileName = "thumbnail.png";
        MultipartFile thumbnail1 = FileProvider.getTestMultipartFile(fileName);
        Project project = Project.builder()
                .managerId(managerId)
                .name(name)
                .description(desc)
                .thumbnail(FileProvider.extractImageUrl(thumbnail1))
                .build();
        Story story = Story.builder()
                .title("story title")
                .preStories("[]")
                .build();
        ReflectionTestUtils.setField(project, "stories", List.of(story));

        given(projectRepository.findById(eq(id)))
                .willReturn(Optional.of(project));

        // when
        List<ProjectStoryResponse> result = projectService.getProjectStories(id);

        // then
        ProjectStoryResponse response = result.get(0);
        assertThat(result.size()).isEqualTo(1);
        assertThat(response.getTitle()).isEqualTo(story.getTitle());
        verify(projectRepository, times(1)).findById(1L);
    }

}
