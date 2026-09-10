package io.github.roledock.joboffer.extraction.openai;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import io.github.roledock.joboffer.extraction.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.net.http.HttpClient;
import static io.github.roledock.joboffer.extraction.ExtractionException.Reason.*;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OpenAiJobOfferExtractor implements JobOfferExtractor {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OpenAiJobOfferExtractor.class);
    private final String promptHash;
    private final String schemaHash;
    private final RestClient client;
    private final String apiKey;
    private final String model;
    private final ExtractionValidator validator;
    private final ObjectMapper mapper;
    private final JsonNode schema;
    private final String instructions;

    @org.springframework.beans.factory.annotation.Autowired
    public OpenAiJobOfferExtractor(
            @Value("${roledock.extraction.api-key:}") String apiKey,
            @Value("${roledock.extraction.model:}") String model,
            ExtractionValidator validator) {
        this(RestClient.builder().baseUrl("https://api.openai.com/v1")
                .requestFactory(requestFactory()), apiKey, model, validator);
    }

    // Package visibility permits a network-free HTTP contract test.
    OpenAiJobOfferExtractor(RestClient.Builder builder, String apiKey, String model,
                           ExtractionValidator validator) {
        this.client = builder.build();
        this.apiKey = apiKey;
        this.model = model;
        this.validator = validator;
        this.mapper = com.fasterxml.jackson.databind.json.JsonMapper.builder()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
                .enable(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
                .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
                .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
                .build();
        mapper.coercionConfigFor(com.fasterxml.jackson.databind.type.LogicalType.Textual)
                .setCoercion(com.fasterxml.jackson.databind.cfg.CoercionInputShape.Integer,
                        com.fasterxml.jackson.databind.cfg.CoercionAction.Fail)
                .setCoercion(com.fasterxml.jackson.databind.cfg.CoercionInputShape.Float,
                        com.fasterxml.jackson.databind.cfg.CoercionAction.Fail)
                .setCoercion(com.fasterxml.jackson.databind.cfg.CoercionInputShape.Boolean,
                        com.fasterxml.jackson.databind.cfg.CoercionAction.Fail);
        try {
            schema = mapper.readTree(resource("schema.json"));
            instructions = resource("instructions.txt");
            promptHash = fingerprint(instructions);
            schemaHash = fingerprint(resource("schema.json"));
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot load extraction contract resources.");
        }
    }

    private static JdkClientHttpRequestFactory requestFactory() {
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10)).build());
        factory.setReadTimeout(Duration.ofSeconds(60));
        return factory;
    }

    private static String resource(String name) throws IOException {
        return new ClassPathResource("job-offer-extraction/" + name)
                .getContentAsString(StandardCharsets.UTF_8);
    }

    private static String fingerprint(String value) {
        try {
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                    .digest(value.replace("\r\n", "\n").getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable");
        }
    }

    @Override
    public JobOfferExtraction extract(String rawJobOffer) {
        if (rawJobOffer == null || rawJobOffer.isBlank() || rawJobOffer.length() > 50000) {
            throw new IllegalArgumentException("Job advertisement must contain 1 to 50000 characters.");
        }
        if (apiKey.isBlank() || model.isBlank()) throw ExtractionException.unavailable();
        String attemptId = java.util.UUID.randomUUID().toString();
        // Only configured model and resource fingerprints, never advertisement/provider content.
        String diagnosticModel = model.matches("[A-Za-z0-9._:/-]{1,160}") ? model : "unrecognized";
        log.info("Extraction attempt={} model={} promptSha256Lf={} schemaSha256Lf={}",
                attemptId, diagnosticModel, promptHash, schemaHash);
        try {
            String body = client.post().uri("/responses")
                    .header("Authorization", "Bearer " + apiKey)
                    .body(Map.of(
                            "model", model, "store", false,
                            "instructions", instructions,
                            "input", List.of(Map.of("role", "user", "content", rawJobOffer)),
                            "max_output_tokens", 12000,
                            "text", Map.of("format", Map.of(
                                    "type", "json_schema", "name", "job_offer_extraction",
                                    "strict", true, "schema", schema))))
                    .retrieve().body(String.class);
            JsonNode response = mapper.readTree(body);
            if (response == null) throw ExtractionException.failed(PROVIDER_FORMAT);
            if (!"completed".equals(response.path("status").asText())) throw ExtractionException.failed(PROVIDER_INCOMPLETE);
            String output = null;
            for (JsonNode item : response.path("output")) {
                if (!"message".equals(item.path("type").asText())) continue;
                for (JsonNode content : item.path("content")) {
                    if ("refusal".equals(content.path("type").asText())) throw ExtractionException.failed(PROVIDER_REFUSAL);
                    if ("output_text".equals(content.path("type").asText())) {
                        if (output != null || !content.path("text").isTextual()) throw ExtractionException.failed(PROVIDER_FORMAT);
                        output = content.path("text").textValue();
                    }
                }
            }
            if (output == null) throw ExtractionException.failed(PROVIDER_FORMAT);
            return validator.validate(mapper.readValue(output, JobOfferExtraction.class), rawJobOffer);
        } catch (ExtractionException exception) {
            throw exception;
        } catch (org.springframework.web.client.RestClientResponseException exception) {
            throw ExtractionException.providerHttp(exception.getStatusCode().value());
        } catch (org.springframework.web.client.ResourceAccessException exception) {
            boolean timeout = false;
            for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
                if (cause instanceof java.net.http.HttpTimeoutException
                        || cause instanceof java.net.SocketTimeoutException) timeout = true;
            }
            throw ExtractionException.failed(timeout ? TIMEOUT : TRANSPORT);
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            throw ExtractionException.failed(INVALID_JSON);
        } catch (Exception exception) {
            // Never retain provider bodies, advertisement text, or credentials in exceptions.
            throw ExtractionException.failed(UNEXPECTED);
        }
    }
}
