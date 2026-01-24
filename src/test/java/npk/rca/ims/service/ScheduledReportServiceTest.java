package npk.rca.ims.service;

import npk.rca.ims.model.ScheduledReportConfig;
import npk.rca.ims.repository.ScheduledReportConfigRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduledReportServiceTest {

    @Mock
    private ScheduledReportConfigRepository configRepository;

    @Mock
    private ReportService reportService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private ScheduledReportService scheduledReportService;

    private ScheduledReportConfig dailyConfig;

    @BeforeEach
    void setUp() {
        dailyConfig = ScheduledReportConfig.builder()
                .id(1L)
                .email("test@example.com")
                .frequency(ScheduledReportConfig.ReportFrequency.DAILY)
                .reportType(ScheduledReportConfig.ScheduledReportType.TRANSACTION_HISTORY)
                .active(true)
                .scheduledTime(java.time.LocalTime.now()) // Set to current time to ensure it runs
                .lastSent(LocalDateTime.now().minusDays(2)) // Due
                .build();
    }

    @Test
    void processScheduledReports_ShouldSendEmail_WhenReportIsDue() throws IOException {
        when(configRepository.findByActiveTrue()).thenReturn(List.of(dailyConfig));
        when(reportService.generateTransactionReportPdf(any(), any(), any(), any())).thenReturn(new byte[]{1, 2, 3});

        scheduledReportService.processScheduledReports();

        verify(emailService, times(1)).sendReportEmailWithAttachment(eq("test@example.com"), anyString(), anyString(), anyMap());
        verify(configRepository, times(1)).save(dailyConfig);
    }

    @Test
    void processScheduledReports_ShouldNotSendEmail_WhenReportIsNotDue() {
        dailyConfig.setLastSent(LocalDateTime.now()); // Not due
        when(configRepository.findByActiveTrue()).thenReturn(List.of(dailyConfig));

        scheduledReportService.processScheduledReports();

        verify(emailService, never()).sendReportEmailWithAttachment(anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    @DisplayName("Should respect scheduled time preference")
    void processScheduledReports_ShouldRespectScheduledTime() throws IOException {
        // Case 1: Time Matches
        LocalTime now = LocalTime.now();
        dailyConfig.setScheduledTime(now); // Set to current hour (minute/second ignored by logic logic?)
        // Logic uses: now.getHour() != scheduledHour
        
        // Let's ensure we are safe if test runs near hour boundary?
        // Logic is simple: now.getHour() == config.getHour().
        // So setting it to LocalTime.now() is correct.
        
        when(configRepository.findByActiveTrue()).thenReturn(List.of(dailyConfig));
        when(reportService.generateTransactionReportPdf(any(), any(), any(), any())).thenReturn(new byte[]{1});

        scheduledReportService.processScheduledReports();

        verify(emailService, times(1)).sendReportEmailWithAttachment(eq("test@example.com"), anyString(), anyString(), anyMap());
        
        // Case 2: Time Does Not Match
        reset(emailService); // Reset mock
        dailyConfig.setLastSent(null); // Reset last sent so frequency doesn't block it
        dailyConfig.setScheduledTime(now.plusHours(1)); // Different hour
        
        scheduledReportService.processScheduledReports();
        
        verify(emailService, never()).sendReportEmailWithAttachment(anyString(), anyString(), anyString(), anyMap());
    }
}
