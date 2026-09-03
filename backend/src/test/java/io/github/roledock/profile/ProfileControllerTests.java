package io.github.roledock.profile;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class ProfileControllerTests {
    private static final String SKILL_JAVA = "11111111-1111-1111-1111-111111111111";
    private static final String SKILL_SQL = "22222222-2222-2222-2222-222222222222";
    private static final String EXPERIENCE_FIRST = "33333333-3333-3333-3333-333333333333";
    private static final String EXPERIENCE_SECOND = "44444444-4444-4444-4444-444444444444";
    private static final String EDUCATION = "55555555-5555-5555-5555-555555555555";
    private static final String LANGUAGE = "66666666-6666-6666-6666-666666666666";
    private static final String CERTIFICATION = "77777777-7777-7777-7777-777777777777";
    private static final String PROJECT = "88888888-8888-8888-8888-888888888888";

    @Autowired private MockMvc mockMvc;
    @Autowired private CandidateProfileRepository repository;

    @BeforeEach
    void deleteCurrentProfile() {
        repository.deleteAll();
    }

    @Test
    void returnsNoContentWhenNoProfileExists() throws Exception {
        mockMvc.perform(get("/api/profile")).andExpect(status().isNoContent());
    }

    @Test
    void createsThenReadsTheCompleteProfileFaithfullyAndKeepsOrder() throws Exception {
        mockMvc.perform(put("/api/profile").contentType(MediaType.APPLICATION_JSON).content(completeProfile()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mainTitle").value("Senior backend engineer"))
                .andExpect(jsonPath("$.targetRoles[0]").value("Lead developer"))
                .andExpect(jsonPath("$.targetRoles[1]").value("Staff engineer"))
                .andExpect(jsonPath("$.experiences[0].achievements[0]").value("Reduced latency by 30%"))
                .andExpect(jsonPath("$.experiences[0].achievements[1]").value("Led a team of four"))
                .andExpect(jsonPath("$.experiences[0].skillIds[0]").value(SKILL_SQL))
                .andExpect(jsonPath("$.experiences[0].skillIds[1]").value(SKILL_JAVA));

        mockMvc.perform(get("/api/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentLocation").value("Paris"))
                .andExpect(jsonPath("$.workModes[0]").value("HYBRID"))
                .andExpect(jsonPath("$.workModes[1]").value("REMOTE"))
                .andExpect(jsonPath("$.skills[0].name").value("Java"))
                .andExpect(jsonPath("$.skills[1].name").value("SQL"))
                .andExpect(jsonPath("$.educations[0].degree").value("Engineering degree"))
                .andExpect(jsonPath("$.languages[0].level").value("C1"))
                .andExpect(jsonPath("$.certifications[0].credentialId").value("CERT-42"))
                .andExpect(jsonPath("$.projects[0].role").value("Maintainer"));
    }

    @Test
    void updatesAndDeletesNestedItemsDeterministically() throws Exception {
        mockMvc.perform(put("/api/profile").contentType(MediaType.APPLICATION_JSON).content(completeProfile()))
                .andExpect(status().isOk());

        String update = """
                {
                  "mainTitle": "Platform engineer",
                  "targetRoles": ["Platform engineer"],
                  "currentLocation": null,
                  "professionalSummary": null,
                  "mobility": null,
                  "desiredLocations": [],
                  "workModes": ["REMOTE"],
                  "contractTypes": [],
                  "skills": [{"id":"%s","name":"Java 21","category":"Backend"}],
                  "experiences": [{
                    "id":"%s","company":"Second company updated","position":null,"location":null,
                    "startDate":null,"endDate":null,"current":false,"description":null,
                    "achievements":[],"skillIds":["%s"]
                  }],
                  "educations":[],"languages":[],"certifications":[],"projects":[],
                  "additionalInformation":null
                }
                """.formatted(SKILL_JAVA, EXPERIENCE_SECOND, SKILL_JAVA);

        mockMvc.perform(put("/api/profile").contentType(MediaType.APPLICATION_JSON).content(update))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.experiences", hasSize(1)))
                .andExpect(jsonPath("$.experiences[0].id").value(EXPERIENCE_SECOND))
                .andExpect(jsonPath("$.experiences[0].company").value("Second company updated"))
                .andExpect(jsonPath("$.skills", hasSize(1)))
                .andExpect(jsonPath("$.skills[0].name").value("Java 21"))
                .andExpect(jsonPath("$.educations", hasSize(0)))
                .andExpect(jsonPath("$.languages", hasSize(0)))
                .andExpect(jsonPath("$.certifications", hasSize(0)))
                .andExpect(jsonPath("$.projects", hasSize(0)));

        mockMvc.perform(get("/api/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetRoles", hasSize(1)))
                .andExpect(jsonPath("$.experiences", hasSize(1)))
                .andExpect(jsonPath("$.skills", hasSize(1)));
    }

    @Test
    void rejectsUnknownAndDuplicateSkillReferences() throws Exception {
        String unknownReference = emptyProfileWith("""
                "skills":[{"id":"%s","name":"Java","category":null}],
                "experiences":[{"id":"%s","company":null,"position":null,"location":null,"startDate":null,
                "endDate":null,"current":false,"description":null,"achievements":[],
                "skillIds":["99999999-9999-9999-9999-999999999999"]}]
                """.formatted(SKILL_JAVA, EXPERIENCE_FIRST));
        mockMvc.perform(put("/api/profile").contentType(MediaType.APPLICATION_JSON).content(unknownReference))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors['experiences[0].skillIds']").exists());

        String duplicateReference = emptyProfileWith("""
                "skills":[{"id":"%s","name":"Java","category":null}],
                "experiences":[{"id":"%s","company":null,"position":null,"location":null,"startDate":null,
                "endDate":null,"current":false,"description":null,"achievements":[],"skillIds":["%s","%s"]}]
                """.formatted(SKILL_JAVA, EXPERIENCE_FIRST, SKILL_JAVA, SKILL_JAVA));
        mockMvc.perform(put("/api/profile").contentType(MediaType.APPLICATION_JSON).content(duplicateReference))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors['experiences[0].skillIds']").exists());
    }

    @Test
    void rejectsInvalidFieldsAndIncoherentDates() throws Exception {
        String blankSkill = emptyProfileWith("""
                "skills":[{"id":"%s","name":"   ","category":null}],"experiences":[]
                """.formatted(SKILL_JAVA));
        mockMvc.perform(put("/api/profile").contentType(MediaType.APPLICATION_JSON).content(blankSkill))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors['skills[0].name']").exists());

        String invalidDates = emptyProfileWith("""
                "skills":[],
                "experiences":[{"id":"%s","company":null,"position":null,"location":null,
                "startDate":"2025-06-01","endDate":"2025-05-01","current":true,"description":null,
                "achievements":[],"skillIds":[]}]
                """.formatted(EXPERIENCE_FIRST));
        mockMvc.perform(put("/api/profile").contentType(MediaType.APPLICATION_JSON).content(invalidDates))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors['experiences[0].endDate']").exists());

        String invalidUrl = emptyProfileWith("""
                "skills":[],"experiences":[],
                "projects":[{"id":"%s","name":null,"role":null,"description":null,"startDate":null,
                "endDate":null,"url":"not a URL"}]
                """.formatted(PROJECT)).replace("\"projects\":[],", "");
        mockMvc.perform(put("/api/profile").contentType(MediaType.APPLICATION_JSON).content(invalidUrl))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors['projects[0].url']").exists());
    }

    @Test
    void returnsTheSharedApiErrorContractForUnreadableJson() throws Exception {
        mockMvc.perform(put("/api/profile").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("La requête contient des données invalides."))
                .andExpect(jsonPath("$.fieldErrors.request").value("Le format de la requête est invalide."));
    }

    @Test
    void acceptsAnEmptyPartialProfileWithoutInferringCandidateFacts() throws Exception {
        mockMvc.perform(put("/api/profile").contentType(MediaType.APPLICATION_JSON).content(emptyProfileWith("\"skills\":[],\"experiences\":[]")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mainTitle").doesNotExist())
                .andExpect(jsonPath("$.skills", hasSize(0)))
                .andExpect(jsonPath("$.experiences", hasSize(0)))
                .andExpect(jsonPath("$.workModes", hasSize(0)))
                .andExpect(jsonPath("$.contractTypes", hasSize(0)));
    }

    private String completeProfile() {
        return """
                {
                  "mainTitle":"  Senior backend engineer  ",
                  "targetRoles":["Lead developer","Staff engineer"],
                  "currentLocation":"Paris",
                  "professionalSummary":"Backend systems specialist",
                  "mobility":"France, occasional travel",
                  "desiredLocations":["Paris","Remote in France"],
                  "workModes":["HYBRID","REMOTE"],
                  "contractTypes":["CDI","Freelance"],
                  "skills":[
                    {"id":"%s","name":"Java","category":"Backend"},
                    {"id":"%s","name":"SQL","category":"Data"}
                  ],
                  "experiences":[
                    {"id":"%s","company":"First company","position":"Senior engineer","location":"Paris","startDate":"2022-01-01","endDate":null,"current":true,"description":"Built APIs","achievements":["Reduced latency by 30%%","Led a team of four"],"skillIds":["%s","%s"]},
                    {"id":"%s","company":"Second company","position":"Engineer","location":null,"startDate":"2019-01-01","endDate":"2021-12-31","current":false,"description":null,"achievements":[],"skillIds":[]}
                  ],
                  "educations":[{"id":"%s","institution":"Engineering school","degree":"Engineering degree","field":"Computer science","startDate":"2014-09-01","endDate":"2019-06-30","description":null}],
                  "languages":[{"id":"%s","name":"English","level":"C1"}],
                  "certifications":[{"id":"%s","name":"Cloud certificate","issuer":"Issuer","issueDate":"2024-01-01","expirationDate":"2027-01-01","credentialId":"CERT-42","credentialUrl":"https://example.test/cert"}],
                  "projects":[{"id":"%s","name":"Open source library","role":"Maintainer","description":"Maintained releases","startDate":"2020-01-01","endDate":null,"url":"https://example.test/project"}],
                  "additionalInformation":"Available in one month"
                }
                """.formatted(SKILL_JAVA, SKILL_SQL, EXPERIENCE_FIRST, SKILL_SQL, SKILL_JAVA, EXPERIENCE_SECOND,
                EDUCATION, LANGUAGE, CERTIFICATION, PROJECT);
    }

    private String emptyProfileWith(String collections) {
        return """
                {"mainTitle":null,"targetRoles":[],"currentLocation":null,"professionalSummary":null,
                "mobility":null,"desiredLocations":[],"workModes":[],"contractTypes":[],%s,
                "educations":[],"languages":[],"certifications":[],"projects":[],"additionalInformation":null}
                """.formatted(collections);
    }
}
