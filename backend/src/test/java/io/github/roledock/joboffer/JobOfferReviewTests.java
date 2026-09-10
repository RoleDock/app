package io.github.roledock.joboffer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.roledock.joboffer.extraction.JobOfferExtractor;
import java.util.UUID;
import java.util.List;
import static io.github.roledock.joboffer.extraction.JobOfferExtraction.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"roledock.extraction.api-key=", "roledock.extraction.model="})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JobOfferReviewTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired JobOfferService service;
    @MockitoBean JobOfferExtractor extractor;
    UUID id;
    String snapshot;

    @BeforeEach void setup() throws Exception {
        var extraction = mapper.readValue(getClass().getResourceAsStream("/job-offers/unknown.expected.json"),
                io.github.roledock.joboffer.extraction.JobOfferExtraction.class);
        extraction = new io.github.roledock.joboffer.extraction.JobOfferExtraction(extraction.company(), extraction.position(),
                extraction.location(), extraction.workArrangement(), extraction.contractType(), extraction.sourceLanguage(),
                extraction.summary(), List.of("First mission", "Second mission"), List.of(
                new Requirement("Synthetic", "First requirement", RequirementCategory.OTHER, RequirementKind.UNKNOWN,
                        Centrality.UNKNOWN, Explicitness.EXPLICIT, false, null, null, ExtractionConfidence.HIGH),
                new Requirement("advertisement", "Second requirement", RequirementCategory.OTHER, RequirementKind.UNKNOWN,
                        Centrality.UNKNOWN, Explicitness.EXPLICIT, false, null, null, ExtractionConfidence.LOW)));
        id = UUID.randomUUID();
        service.save(new JobOfferService.Pending(id, "Synthetic advertisement", null, java.time.Instant.now(), extraction));
        snapshot = jdbc.queryForObject("SELECT initial_extraction FROM job_offer WHERE id = ?", String.class, id);
    }
    @AfterEach void noProviderCallsAndImmutableSources() {
        verifyNoInteractions(extractor);
        assertThat(jdbc.queryForObject("SELECT initial_extraction FROM job_offer WHERE id = ?", String.class, id)).isEqualTo(snapshot);
        assertThat(jdbc.queryForObject("SELECT original_text FROM job_offer WHERE id = ?", String.class, id)).isEqualTo("Synthetic advertisement");
    }
    ObjectNode current() throws Exception {
        return (ObjectNode) mapper.readTree(mvc.perform(get("/api/job-offers/" + id)).andReturn().getResponse().getContentAsString()).get("extraction");
    }
    org.springframework.test.web.servlet.ResultActions review(String action, ObjectNode data) throws Exception {
        var body = mapper.createObjectNode().put("action", action);
        if (data != null) body.set("extraction", data);
        return mvc.perform(put("/api/job-offers/" + id + "/review").contentType("application/json").content(body.toString()));
    }
    @Test void unchangedConfirmationPreservesSnapshot() throws Exception {
        review("SAVE", current()).andExpect(status().isOk()).andExpect(jsonPath("$.reviewStatus").value("CONFIRMED"));
    }
    @Test void correctionSurvivesReloadWithoutChangingSnapshot() throws Exception {
        var data = current().put("company", "Corrected synthetic company");
        review("SAVE", data).andExpect(status().isOk()).andExpect(jsonPath("$.reviewStatus").value("CORRECTED"));
        assertThat(current().get("company").asText()).isEqualTo("Corrected synthetic company");
        review("SAVE", current()).andExpect(jsonPath("$.reviewStatus").value("CORRECTED"));
    }
    @Test void bypassPersistsDecisionAndAllowsLaterConfirmation() throws Exception {
        review("BYPASS", null).andExpect(status().isOk()).andExpect(jsonPath("$.reviewStatus").value("UNREVIEWED"))
                .andExpect(jsonPath("$.reviewBypassedAt").isNotEmpty());
        assertThat(jdbc.queryForObject("SELECT review_bypassed_at FROM job_offer WHERE id = ?", Object.class, id)).isNotNull();
        review("SAVE", current()).andExpect(jsonPath("$.reviewStatus").value("CONFIRMED"));
        review("BYPASS", null).andExpect(status().isConflict());
    }
    @Test void bypassAllowsLaterCorrection() throws Exception {
        review("BYPASS", null).andExpect(status().isOk());
        review("SAVE", current().put("position", "Corrected role")).andExpect(jsonPath("$.reviewStatus").value("CORRECTED"));
    }
    ObjectNode manualRequirement() {
        return mapper.createObjectNode().put("canonicalLabel", "User observation").put("category", "OTHER")
                .put("requirementKind", "REQUIRED").put("centrality", "CORE").put("explicitness", "EXPLICIT")
                .put("hardBlockerCandidate", false);
    }
    @Test void addingAndRemovingRequirementsPreservesOrderAndUserProvenance() throws Exception {
        var data = current();
        var requirements = data.putArray("requirements");
        requirements.add(manualRequirement());
        requirements.add(manualRequirement().put("canonicalLabel", "Second observation"));
        review("SAVE", data).andExpect(status().isOk())
                .andExpect(jsonPath("$.extraction.requirements[0].source").value("USER_ADDED"))
                .andExpect(jsonPath("$.extraction.requirements[0].rawText").isEmpty());
        var reloaded = current();
        var removedId = reloaded.get("requirements").get(0).get("id").asText();
        ((com.fasterxml.jackson.databind.node.ArrayNode) reloaded.get("requirements")).remove(0);
        review("SAVE", reloaded).andExpect(status().isOk());
        assertThat(current().get("requirements").get(0).get("canonicalLabel").asText()).isEqualTo("Second observation");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM job_requirement WHERE id = ?", Integer.class, UUID.fromString(removedId))).isZero();
    }
    @Test void extractedCitationsCannotBeRewrittenOrForged() throws Exception {
        var data = current();
        ((ObjectNode) data.get("requirements").get(0)).put("rawText", "Forged quotation");
        review("SAVE", data).andExpect(status().isBadRequest());
        assertThat(current().get("requirements").get(0).get("rawText").asText()).isEqualTo("Synthetic");
        data = current();
        data.putArray("requirements").add(manualRequirement().put("rawText", "Synthetic"));
        review("SAVE", data).andExpect(status().isBadRequest());
    }
    @Test void requirementIdsAndOrderSurviveReorderingAndEdits() throws Exception {
        var data = current();
        var first = data.get("requirements").get(0).deepCopy();
        var second = data.get("requirements").get(1).deepCopy();
        ((ObjectNode) first).put("canonicalLabel", "Corrected label");
        data.putArray("requirements").add(second).add(first);
        data.putArray("missions").add("Second mission").add("New mission");
        review("SAVE", data).andExpect(status().isOk());
        assertThat(current()).isEqualTo(data);
    }
    @Test void rejectsInvalidEnumsDuplicateIdsAndInvalidConstraints() throws Exception {
        var data = current().put("contractType", "INVALID");
        review("SAVE", data).andExpect(status().isBadRequest());
        data = current();
        ((com.fasterxml.jackson.databind.node.ArrayNode) data.get("requirements")).add(data.get("requirements").get(0).deepCopy());
        review("SAVE", data).andExpect(status().isBadRequest());
        data = current();
        ((ObjectNode) data.get("requirements").get(0)).putObject("constraint").put("operator", "AT_LEAST").put("value", " ");
        review("SAVE", data).andExpect(status().isBadRequest());
        review("SAVE", null).andExpect(status().isBadRequest());
        review("BYPASS", current()).andExpect(status().isBadRequest());
    }
    @Test void invalidAggregateChangesNothing() throws Exception {
        var before = current();
        var invalid = before.deepCopy().put("company", "Must roll back");
        invalid.putArray("requirements").add(manualRequirement().put("hardBlockerCandidate", true));
        review("SAVE", invalid).andExpect(status().isBadRequest());
        assertThat(current()).isEqualTo(before);
    }
    @Test void databaseFailureRollsBackParentAndChildren() throws Exception {
        var before = current();
        var data = before.deepCopy().put("company", "Must roll back");
        data.putArray("requirements").add(manualRequirement());
        jdbc.execute("ALTER TABLE job_requirement ADD CONSTRAINT review_write_failure CHECK (canonical_label <> 'User observation')");
        try { review("SAVE", data).andExpect(status().isInternalServerError()); }
        finally { jdbc.execute("ALTER TABLE job_requirement DROP CONSTRAINT review_write_failure"); }
        assertThat(current()).isEqualTo(before);
    }
}
