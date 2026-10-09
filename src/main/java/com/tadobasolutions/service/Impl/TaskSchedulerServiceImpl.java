package com.tadobasolutions.service.Impl;

import com.tadobasolutions.entity.Task;
import com.tadobasolutions.entity.enums.TaskStatus;
import com.tadobasolutions.repository.EmployeeRepository;
import com.tadobasolutions.repository.InternRepository;
import com.tadobasolutions.repository.TaskRepository;
import com.tadobasolutions.service.TaskSchedulerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskSchedulerServiceImpl implements TaskSchedulerService {

    private final TaskRepository     taskRepository;
    private final EmployeeRepository employeeRepository;
    private final InternRepository   internRepository;
    private final FcmNotificationService fcmService;


    @Override
    @Scheduled(fixedDelay = 600_000)
    public void markOverdueTasks() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
        List<Task> overdue = taskRepository.findOverduePendingTasks(TaskStatus.PENDING, now);
        if (!overdue.isEmpty()) {
            overdue.forEach(t -> t.setStatus(TaskStatus.OVERDUE));
            taskRepository.saveAll(overdue);
            log.info("Marked {} tasks as OVERDUE", overdue.size());
        }
    }

    /**
     * Sends an FCM reminder to whoever a task is assigned to — checks the
     * employees collection first, then falls back to interns. Both entities
     * carry their own fcmTokens list, so this covers either case.
     */
    private void sendReminderToAssignee(Task task, String title, String body, String type) {
        List<String> tokens = null;

        var empOpt = employeeRepository.findById(task.getEmployeeId());
        if (empOpt.isPresent()) {
            tokens = empOpt.get().getFcmTokens();
        } else {
            var internOpt = internRepository.findById(task.getEmployeeId());
            if (internOpt.isPresent()) {
                tokens = internOpt.get().getFcmTokens();
            }
        }

        if (tokens != null && !tokens.isEmpty()) {
            fcmService.sendToTokens(
                    tokens,
                    title,
                    body,
                    Map.of(
                            "taskId", task.getId(),
                            "type",   type
                    )
            );
            log.info("{} sent → assignee={} task={}", type, task.getEmployeeId(), task.getId());
        }
    }

    /**
     * Runs every day at 9:00 AM IST.
     * Sends a reminder to employees/interns whose PENDING task is due within the next 24 hours.
     */
    @Override
    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Kolkata")
    public void sendDueTodayReminders() {
        LocalDateTime now  = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
        LocalDateTime in24 = now.plusHours(24);

        List<Task> tasks = taskRepository.findPendingTasksDueWithin(TaskStatus.PENDING, now, in24);

        for (Task task : tasks) {
            sendReminderToAssignee(
                    task,
                    "⏰ Task Due Soon",
                    "Your task \"" + task.getTask() + "\" is due within 24 hours. Please submit it on time!",
                    "TASK_DUE_REMINDER"
            );
        }
    }

    /**
     * Runs every hour at the top of the hour.
     * Sends an urgent reminder to employees/interns whose PENDING task is due within the next 2 hours.
     */
    @Override
    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Kolkata")
    public void sendUrgentReminders() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
        LocalDateTime in2 = now.plusHours(2);

        List<Task> tasks = taskRepository.findPendingTasksDueWithin(TaskStatus.PENDING, now, in2);

        for (Task task : tasks) {
            sendReminderToAssignee(
                    task,
                    "🚨 Task Due in 2 Hours!",
                    "Urgent: \"" + task.getTask() + "\" must be submitted very soon!",
                    "TASK_DUE_URGENT"
            );
        }
    }
}