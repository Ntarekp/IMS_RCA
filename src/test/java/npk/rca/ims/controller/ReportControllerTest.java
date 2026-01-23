package npk.rca.ims.controller;

import npk.rca.ims.model.ScheduledReportConfig;
import npk.rca.ims.repository.ScheduledReportConfigRepository;
import npk.rca.ims.service.JwtService;
import npk.rca.ims.service.ReportService;
import npk.rca.ims.service.StockTransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportController.class)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StockTransactionService transactionService;

    @MockBean
    private ReportService reportService;

    @MockBean
    private ScheduledReportConfigRepository scheduledReportConfigRepository;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser
    void getReportHistory_ShouldReturnOk() throws Exception {
        given(reportService.getReportHistory()).willReturn(Arrays.asList());

        mockMvc.perform(get("/api/reports/history"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void scheduleReport_ShouldReturnOk() throws Exception {
        ScheduledReportConfig config = ScheduledReportConfig.builder()
                .email("test@example.com")
                .frequency(ScheduledReportConfig.ReportFrequency.DAILY)
                .reportType(ScheduledReportConfig.ScheduledReportType.TRANSACTION_HISTORY)
                .build();

        given(scheduledReportConfigRepository.save(any(ScheduledReportConfig.class))).willReturn(config);

        mockMvc.perform(post("/api/reports/schedule")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@example.com\",\"frequency\":\"DAILY\",\"reportType\":\"TRANSACTION_HISTORY\"}"))
                .andExpect(status().isOk());
    }
}
