package org.example.lab1.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Date;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

class ScheduledMaintenanceServiceTest {

    @Test
    void duplicateReminderRunDoesNotInsertDuplicateRows() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.update(any(String.class), any(Date.class), any(Date.class), anyInt()))
                .thenReturn(2, 0);
        ScheduledMaintenanceService service = new ScheduledMaintenanceService(jdbcTemplate);

        assertEquals(2, service.scheduleBillingReminders(3));
        assertEquals(0, service.scheduleBillingReminders(3));

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, times(2)).update(sql.capture(), any(Date.class), any(Date.class), anyInt());
        assertTrue(sql.getAllValues().get(0).contains("INSERT IGNORE"));
    }

    @Test
    void duplicateDailyReportRunDoesNotChangeExistingSnapshot() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.update(any(String.class), any(Date.class))).thenReturn(1, 0);
        ScheduledMaintenanceService service = new ScheduledMaintenanceService(jdbcTemplate);

        assertEquals(1, service.createDailySubscriptionReport());
        assertEquals(0, service.createDailySubscriptionReport());

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, times(2)).update(sql.capture(), any(Date.class));
        assertTrue(sql.getAllValues().get(0).contains("INSERT IGNORE"));
    }
}
