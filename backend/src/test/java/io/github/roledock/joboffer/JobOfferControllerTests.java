package io.github.roledock.joboffer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.roledock.joboffer.extraction.*;
import static io.github.roledock.joboffer.extraction.JobOfferExtraction.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"roledock.extraction.api-key=", "roledock.extraction.model="})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JobOfferControllerTests {
    private static final String TEXT = "  Synthetic vacancy\r\nJava required.\nThree years of practice.  \n";
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired JobOfferService service;
    @MockitoBean JobOfferExtractor extractor;
    @MockitoSpyBean JobOfferRepository repository;

    @BeforeEach
    void setup() {
        reset(repository);
        jdbc.update("DELETE FROM job_requirement");
        jdbc.update("DELETE FROM job_offer_mission");
        jdbc.update("DELETE FROM job_offer");
        when(extractor.extract(TEXT)).thenReturn(extraction());
    }

    @Test
    void assessmentsRecomputeFromCurrentProfileWithoutCallingExtractorOrChangingReview() throws Exception {
        var saved = service.save(new JobOfferService.Pending(UUID.randomUUID(), TEXT, null, java.time.Instant.now(), extraction()));
        clearInvocations(extractor);
        var skillId = UUID.randomUUID();
        String profile = "{\"skills\":[{\"id\":\"" + skillId + "\",\"name\":\"Java\"}],\"certificationsComplete\":true}";
        mvc.perform(put("/api/profile").contentType(MediaType.APPLICATION_JSON).content(profile)).andExpect(status().isOk())
                .andExpect(jsonPath("$.certificationsComplete").value(true));
        mvc.perform(get("/api/job-offers/" + saved.id() + "/requirement-assessments"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.assessedOn").isNotEmpty())
                .andExpect(jsonPath("$.assessments[0].requirementId").value(saved.extraction().requirements().getFirst().id().toString()))
                .andExpect(jsonPath("$.assessments[0].status").value("MATCH"))
                .andExpect(jsonPath("$.assessments[0].evidence[0].id").value(skillId.toString()));
        mvc.perform(put("/api/profile").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.certificationsComplete").value(false));
        mvc.perform(get("/api/job-offers/" + saved.id() + "/requirement-assessments"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.assessments[0].status").value("UNKNOWN"));
        mvc.perform(get("/api/job-offers/" + saved.id())).andExpect(jsonPath("$.reviewStatus").value("UNREVIEWED"));
        verifyNoInteractions(extractor);
    }

    @Test
    void assessmentsRejectMissingAndMalformedOfferIds() throws Exception {
        mvc.perform(get("/api/job-offers/" + UUID.randomUUID() + "/requirement-assessments")).andExpect(status().isNotFound());
        mvc.perform(get("/api/job-offers/invalid/requirement-assessments")).andExpect(status().isBadRequest());
        verifyNoInteractions(extractor);
    }

    @Test
    void analysisRecomputesFromProfileAndOfferWithoutProviderCalls() throws Exception {
        var saved = service.save(new JobOfferService.Pending(UUID.randomUUID(), TEXT, null, java.time.Instant.now(), extraction()));
        clearInvocations(extractor);
        var skillId = UUID.randomUUID();
        mvc.perform(put("/api/profile").contentType(MediaType.APPLICATION_JSON)
                .content("{\"skills\":[{\"id\":\"" + skillId + "\",\"name\":\"Java\"}]}"))
                .andExpect(status().isOk());
        String url = "/api/job-offers/" + saved.id() + "/analysis";
        mvc.perform(get(url)).andExpect(status().isOk())
                .andExpect(jsonPath("$.coverageScore").value(100))
                .andExpect(jsonPath("$.reviewStatus").value("UNREVIEWED"))
                .andExpect(jsonPath("$.recommendation").value("VERIFY_FIRST"))
                .andExpect(jsonPath("$.requirementAssessments.length()").value(2))
                .andExpect(jsonPath("$.contributions.length()").value(2));
        mvc.perform(put("/api/profile").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
        mvc.perform(get(url)).andExpect(jsonPath("$.coverageScore").isEmpty());
        jdbc.update("UPDATE job_requirement SET requirement_kind = 'CONTEXTUAL', hard_blocker_candidate = false, blocker_condition = null WHERE job_offer_id = ?", saved.id());
        mvc.perform(get(url)).andExpect(jsonPath("$.coverageScore").isEmpty())
                .andExpect(jsonPath("$.requirementAssessments[0].status").value("NOT_APPLICABLE"));
        mvc.perform(get("/api/job-offers/" + saved.id())).andExpect(jsonPath("$.reviewStatus").value("UNREVIEWED"));
        verifyNoInteractions(extractor);
    }

    @Test
    void analysisPreservesBlockerReviewSafetyWithTheRealAssessmentEngine() throws Exception {
        var extraction = new JobOfferExtraction(null, null, new Location(null, null, null),
                new WorkArrangement(WorkArrangementType.UNKNOWN, null, null), ContractType.UNKNOWN, null, null, List.of(),
                List.of(new Requirement("Credential Alpha mandatory", "Credential Alpha", RequirementCategory.CERTIFICATION,
                        RequirementKind.REQUIRED, Centrality.CORE, Explicitness.EXPLICIT, true, "Credential Alpha", null, ExtractionConfidence.HIGH)));
        var saved = service.save(new JobOfferService.Pending(UUID.randomUUID(), "Fictional credential offer", null, java.time.Instant.now(), extraction));
        clearInvocations(extractor);
        mvc.perform(put("/api/profile").contentType(MediaType.APPLICATION_JSON).content("{\"certificationsComplete\":true}"))
                .andExpect(status().isOk());
        String url = "/api/job-offers/" + saved.id() + "/analysis";
        mvc.perform(get(url)).andExpect(status().isOk()).andExpect(jsonPath("$.recommendation").value("VERIFY_FIRST"))
                .andExpect(jsonPath("$.eligibility").value("ELIGIBLE_WITH_CONSTRAINT"))
                .andExpect(jsonPath("$.criticalGaps[0].label").value("Credential Alpha"));
        jdbc.update("UPDATE job_offer SET review_status = 'CONFIRMED' WHERE id = ?", saved.id());
        mvc.perform(get(url)).andExpect(jsonPath("$.recommendation").value("SKIP_CONFIRMED_BLOCKER"))
                .andExpect(jsonPath("$.eligibility").value("NOT_ELIGIBLE"));
        verifyNoInteractions(extractor);
    }

    @Test
    void analysisHandlesEmptyOfferAndInvalidIdentifiers() throws Exception {
        var empty = new JobOfferExtraction(null, null, new Location(null, null, null),
                new WorkArrangement(WorkArrangementType.UNKNOWN, null, null), ContractType.UNKNOWN, null, null, List.of(), List.of());
        var saved = service.save(new JobOfferService.Pending(UUID.randomUUID(), "Fictional empty offer", null, java.time.Instant.now(), empty));
        mvc.perform(get("/api/job-offers/" + saved.id() + "/analysis")).andExpect(status().isOk())
                .andExpect(jsonPath("$.coverageScore").isEmpty()).andExpect(jsonPath("$.recommendation").value("VERIFY_FIRST"));
        mvc.perform(get("/api/job-offers/" + UUID.randomUUID() + "/analysis")).andExpect(status().isNotFound());
        mvc.perform(get("/api/job-offers/invalid/analysis")).andExpect(status().isBadRequest());
        verifyNoInteractions(extractor);
    }

    @Test
    void persistsAndRetrievesCompleteDraftWithoutTheAnalysisSession() throws Exception {
        var session = new MockHttpSession();
        String id = analyze(session, "https://example.org/vacancy");
        assertThat(repository.count()).isZero();
        var response = mvc.perform(post("/api/job-offers").session(session).contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("analysisId", id))))
                .andExpect(status().isCreated()).andExpect(header().string("Location", "/api/job-offers/" + id))
                .andExpect(jsonPath("$.reviewStatus").value("UNREVIEWED")).andReturn();
        var saved = mapper.readValue(response.getResponse().getContentAsString(), JobOfferDtos.Response.class);
        assertThat(saved.originalText()).isEqualTo(TEXT);
        assertThat(saved.sourceUrl()).isEqualTo("https://example.org/vacancy");
        assertThat(saved.analyzedAt()).isNotNull();
        assertThat(saved.extraction()).usingRecursiveComparison()
                .ignoringFields("requirements.id", "requirements.source").isEqualTo(extraction());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM job_requirement", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForList("SELECT mission FROM job_offer_mission ORDER BY sort_order", String.class))
                .containsExactly("Build synthetic APIs.", "Maintain tests.");
        String snapshot = jdbc.queryForObject("SELECT initial_extraction FROM job_offer WHERE id = ?", String.class, UUID.fromString(id));
        assertThat(mapper.readValue(snapshot, JobOfferExtraction.class)).isEqualTo(extraction());
        mvc.perform(get("/api/job-offers/" + id)).andExpect(status().isOk())
                .andExpect(content().json(response.getResponse().getContentAsString()));
        verify(extractor, times(1)).extract(TEXT);
    }

    @Test
    void nullableSourceAndRetriesDoNotDuplicateTheOffer() throws Exception {
        var session = new MockHttpSession();
        String id = analyze(session, null);
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/api/job-offers").session(session).contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(Map.of("analysisId", id))))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.sourceUrl").isEmpty());
        }
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void currentValuesCanChangeWithoutOverwritingOriginalSnapshot() throws Exception {
        var session = new MockHttpSession();
        String id = analyze(session, null);
        mvc.perform(post("/api/job-offers").session(session).contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("analysisId", id)))).andExpect(status().isCreated());
        jdbc.update("UPDATE job_offer SET company = 'Corrected company', review_status = 'CORRECTED' WHERE id = ?", UUID.fromString(id));
        mvc.perform(get("/api/job-offers/" + id)).andExpect(jsonPath("$.extraction.company").value("Corrected company"));
        String snapshot = jdbc.queryForObject("SELECT initial_extraction FROM job_offer", String.class);
        assertThat(mapper.readValue(snapshot, JobOfferExtraction.class).company()).isEqualTo("Synthetic Company");
        assertThat(jdbc.queryForObject("SELECT original_text FROM job_offer", String.class)).isEqualTo(TEXT);
    }

    @Test
    void extractionFailureCreatesNoOfferAndUsesSafeDiagnostics() throws Exception {
        when(extractor.extract(TEXT)).thenThrow(ExtractionException.failed(ExtractionException.Reason.PROVIDER_REFUSAL));
        mvc.perform(post("/api/job-offers/analyze").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new JobOfferDtos.AnalyzeRequest(TEXT, null))))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("Job-offer extraction failed."));
        assertThat(repository.count()).isZero();
    }

    @Test
    void invalidExtractorOutputNeverBecomesSaveable() throws Exception {
        when(extractor.extract(TEXT)).thenReturn(new JobOfferExtraction(null, null, null, null, null, null, null, null, null));
        mvc.perform(post("/api/job-offers/analyze").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new JobOfferDtos.AnalyzeRequest(TEXT, null))))
                .andExpect(status().isBadGateway());
        assertThat(repository.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{", "{\"originalText\":123}", "{\"originalText\":true}", "{\"originalText\":\"   \"}",
            "{\"originalText\":\"valid\",\"sourceUrl\":\"javascript:alert(1)\"}",
            "{\"originalText\":\"valid\",\"sourceUrl\":\"https://user:password@example.org\"}"})
    void invalidRequestsNeverCallTheExtractor(String payload) throws Exception {
        mvc.perform(post("/api/job-offers/analyze").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(extractor);
        assertThat(repository.count()).isZero();
    }

    @Test
    void rejectsOversizedTextAndInvalidSaveRequests() throws Exception {
        mvc.perform(post("/api/job-offers/analyze").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new JobOfferDtos.AnalyzeRequest("x".repeat(50001), null))))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/job-offers").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/job-offers").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("analysisId", UUID.randomUUID()))))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/job-offers/" + UUID.randomUUID())).andExpect(status().isNotFound());
        mvc.perform(get("/api/job-offers/not-a-uuid")).andExpect(status().isBadRequest());
        assertThat(repository.count()).isZero();
    }

    @Test
    void replacedAnalysisCannotSaveDifferentOrClientSuppliedValues() throws Exception {
        var session = new MockHttpSession();
        String old = analyze(session, null);
        analyze(session, null);
        mvc.perform(post("/api/job-offers").session(session).contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("analysisId", old))))
                .andExpect(status().isConflict());
        assertThat(repository.count()).isZero();
    }

    @Test
    void persistenceFailureRetainsServerAnalysisForRetry() throws Exception {
        var session = new MockHttpSession();
        String id = analyze(session, null);
        doThrow(new org.springframework.dao.DataAccessResourceFailureException("sensitive diagnostics"))
                .when(repository).findById(UUID.fromString(id));
        String payload = mapper.writeValueAsString(Map.of("analysisId", id));
        mvc.perform(post("/api/job-offers").session(session).contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.code").value("PERSISTENCE_FAILED"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("sensitive"))));
        reset(repository);
        assertThat(repository.count()).isZero();
        mvc.perform(post("/api/job-offers").session(session).contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated());
        assertThat(repository.count()).isEqualTo(1);
        verify(extractor, times(1)).extract(TEXT);
    }

    @Test
    void childPersistenceFailureRollsBackTheEntireAggregate() {
        var pending = new JobOfferService.Pending(UUID.randomUUID(), TEXT, null,
                java.time.Instant.now(), extraction());
        // Force the database to reject a child after the parent INSERT.
        jdbc.execute("ALTER TABLE job_requirement ADD CONSTRAINT simulated_write_failure CHECK (canonical_label <> 'Java')");
        try {
            assertThatThrownBy(() -> service.save(pending)).isInstanceOf(RuntimeException.class);
        } finally {
            jdbc.execute("ALTER TABLE job_requirement DROP CONSTRAINT simulated_write_failure");
        }
        assertThat(repository.count()).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM job_requirement", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM job_offer_mission", Integer.class)).isZero();
    }

    private String analyze(MockHttpSession session, String url) throws Exception {
        var result = mvc.perform(post("/api/job-offers/analyze").session(session).contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new JobOfferDtos.AnalyzeRequest(TEXT, url))))
                .andExpect(status().isOk()).andReturn();
        return mapper.readTree(result.getResponse().getContentAsString()).get("analysisId").asText();
    }

    private JobOfferExtraction extraction() {
        return new JobOfferExtraction("Synthetic Company", "Engineer",
                new Location("Lyon", null, "France"), new WorkArrangement(WorkArrangementType.HYBRID, "France", 2),
                ContractType.PERMANENT, "en", "A synthetic engineering vacancy.",
                List.of("Build synthetic APIs.", "Maintain tests."),
                List.of(new Requirement("Java required.", "Java", RequirementCategory.TECH_SKILL, RequirementKind.REQUIRED,
                        Centrality.CORE, Explicitness.EXPLICIT, false, null, null, ExtractionConfidence.HIGH),
                        new Requirement("Three years of practice.", "Experience", RequirementCategory.EXPERIENCE,
                                RequirementKind.REQUIRED, Centrality.SUPPORTING, Explicitness.EXPLICIT, true,
                                "Three years of practice.", new Constraint(ConstraintOperator.AT_LEAST, "3", "years"),
                                ExtractionConfidence.MEDIUM)));
    }
}
