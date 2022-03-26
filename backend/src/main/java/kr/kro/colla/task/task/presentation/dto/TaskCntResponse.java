package kr.kro.colla.task.task.presentation.dto;

import kr.kro.colla.task.task.domain.TaskCntByStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class TaskCntResponse {
    private String taskStatusName;
    private Long taskCnt;

    public TaskCntResponse(TaskCntByStatus taskCnt) {
        this.taskStatusName = taskCnt.getTaskStatusName();
        this.taskCnt = taskCnt.getTaskCnt();
    }
}
