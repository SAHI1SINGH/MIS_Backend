package com.tadobasolutions.scheduler;

import com.tadobasolutions.service.StandupLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.ZoneId;

@Slf4j
@Component
@RequiredArgsConstructor
public class StandupAutoSubmitJob {

    private static final String IST = "Asia/Kolkata";
    private static final LocalTime CUTOFF = LocalTime.of(19, 0);

    private final StandupLogService standupService;

    @Scheduled(cron = "0 0 19 * * *", zone = IST)
    public void atSevenPm() {
        run(true, "7 PM");
    }

    @Scheduled(cron = "0 55 23 * * *", zone = IST)
    public void endOfDay() {
        run(true, "11:55 PM");
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        boolean afterCutoff = !LocalTime.now(ZoneId.of(IST)).isBefore(CUTOFF);
        run(afterCutoff, "startup");
    }

    private void run(boolean includeToday, String label) {
        try {
            int n = standupService.autoSubmitDrafts(includeToday);
            log.info("Standup auto-submit ({}): {} report(s) submitted", label, n);
        } catch (Exception e) {
            log.error("Standup auto-submit ({}) failed", label, e);
        }
    }
}