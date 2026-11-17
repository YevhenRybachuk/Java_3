package org.example.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

class ReportingServiceTest {
    @Test
    void aggregatesSubscriptionsFromSubscriptionServiceAndForwardsJwt() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://subscriptions/api/subscriptions?userId=42"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer user-token"))
                .andRespond(withSuccess("[{\"price\":12.50},{\"price\":7.50}]", MediaType.APPLICATION_JSON));

        ReportingService service = new ReportingService(builder, "http://subscriptions");
        SubscriptionReport report = service.summary(42L, "user-token");

        assertThat(report.subscriptions()).isEqualTo(2);
        assertThat(report.totalPrice()).isEqualByComparingTo("20.00");
        server.verify();
    }
}
