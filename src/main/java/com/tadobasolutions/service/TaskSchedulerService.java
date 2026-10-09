package com.tadobasolutions.service;

public interface TaskSchedulerService {
    void markOverdueTasks();
    void sendDueTodayReminders();
    void sendUrgentReminders();
}