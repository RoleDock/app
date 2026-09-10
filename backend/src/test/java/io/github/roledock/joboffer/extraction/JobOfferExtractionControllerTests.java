package io.github.roledock.joboffer.extraction;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(JobOfferExtractionController.class)
@Import(ExtractionValidator.class)
class JobOfferExtractionControllerTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @MockitoBean JobOfferExtractor extractor;

    @Test
    void returnsValidatedFakeResultWithUnknownsAndNulls() throws Exception {
        var result = mapper.readValue(new ClassPathResource("job-offers/unknown.expected.json")
                .getContentAsString(StandardCharsets.UTF_8), JobOfferExtraction.class);
        when(extractor.extract("Advertisement")).thenReturn(result);
        mvc.perform(post("/api/job-offers/extract").contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\"Advertisement\"}"))
                .andExpect(status().isOk())
                .andExpect(content().json(mapper.writeValueAsString(result)))
                .andExpect(jsonPath("$.contractType").value("UNKNOWN"));
        verify(extractor).extract("Advertisement");
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"text\":null}", "{\"text\":\"\"}", "{\"text\":\"  \"}", "{", "null",
            "{\"text\":123}", "{\"text\":true}", "{\"text\":[]}", "[]"})
    void rejectsInvalidRequestsWithoutInvokingProvider(String payload) throws Exception {
        mvc.perform(post("/api/job-offers/extract").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(extractor);
    }

    @Test
    void rejectsOversizedInput() throws Exception {
        mvc.perform(post("/api/job-offers/extract").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new JobOfferExtractionController.Request("a".repeat(50001)))))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(extractor);
    }

    @Test
    void reportsRejectedOutput() throws Exception {
        when(extractor.extract("Advertisement")).thenThrow(
                ExtractionException.failed(ExtractionException.Reason.INVALID_FIELDS));
        mvc.perform(post("/api/job-offers/extract").contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\"Advertisement\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("Job-offer extraction failed."));
    }

    @Test
    void reportsMissingConfigurationWithoutLeakingDetails() throws Exception {
        when(extractor.extract("Advertisement")).thenThrow(ExtractionException.unavailable());
        mvc.perform(post("/api/job-offers/extract").contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\"Advertisement\"}"))
                .andExpect(status().isServiceUnavailable());
    }
}
