package org.example.lab1.service;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Виконує ідемпотентні операції планувальника в межах транзакції. */
@Service
@RequiredArgsConstructor
public class ScheduledMaintenanceService {
    private final JdbcTemplate jdbcTemplate;
    private final Clock clock = Clock.systemUTC();

    /** Реєструє нагадування про списання на заданий горизонт; унікальний ключ не допускає дублів. */
    @Transactional
    public int scheduleBillingReminders(int daysAhead) {
        if (daysAhead < 0) {
            throw new IllegalArgumentException("daysAhead не може бути від’ємним");
        }
        LocalDate today = LocalDate.now(clock);
        return jdbcTemplate.update("""
                INSERT IGNORE INTO scheduled_reminders (subscription_id, billing_date)
                SELECT id, billing_date FROM subscriptions
                WHERE billing_date BETWEEN ? AND DATE_ADD(?, INTERVAL ? DAY)
                """, Date.valueOf(today), Date.valueOf(today), daysAhead);
    }

    /** Зберігає один щоденний знімок статистики; повторний запуск того самого дня нічого не змінює. */
    @Transactional
    public int createDailySubscriptionReport() {
        LocalDate today = LocalDate.now(clock);
        return jdbcTemplate.update("""
                INSERT IGNORE INTO daily_subscription_reports
                    (report_date, subscription_count, total_monthly_price)
                SELECT ?, COUNT(*), COALESCE(SUM(price), 0) FROM subscriptions
                """, Date.valueOf(today));
    }
}
