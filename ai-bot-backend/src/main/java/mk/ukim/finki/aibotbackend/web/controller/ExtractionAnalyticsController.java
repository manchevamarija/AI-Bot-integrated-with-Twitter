package mk.ukim.finki.aibotbackend.web.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import mk.ukim.finki.aibotbackend.model.dto.SessionStatisticsDto;
import mk.ukim.finki.aibotbackend.service.application.ExtractedPostApplicationService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Additional read-only reporting endpoints. Kept separate so the
 * template-provided extracted-post controller remains unchanged.
 */
@RestController
@RequestMapping("/api/posts")
public class ExtractionAnalyticsController {
    private final ExtractedPostApplicationService extractedPostApplicationService;
    private final ObjectMapper objectMapper;

    public ExtractionAnalyticsController(
        ExtractedPostApplicationService extractedPostApplicationService,
        ObjectMapper objectMapper
    ) {
        this.extractedPostApplicationService = extractedPostApplicationService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/session/{sessionId}/statistics")
    public ResponseEntity<SessionStatisticsDto> statistics(@PathVariable Long sessionId) {
        return ResponseEntity.ok(extractedPostApplicationService.statistics(sessionId));
    }

    @GetMapping(value = "/export/json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> exportJson(@RequestParam Long sessionId)
        throws JsonProcessingException {
        byte[] json = objectMapper.writerWithDefaultPrettyPrinter()
            .writeValueAsBytes(extractedPostApplicationService.findAllBySessionId(sessionId));
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, attachmentName(sessionId, "json"))
            .contentType(new MediaType("application", "json", StandardCharsets.UTF_8))
            .body(withUtf8Bom(json));
    }

    @GetMapping(value = "/export/csv", produces = "text/csv")
    public ResponseEntity<byte[]> exportCsv(@RequestParam Long sessionId) {
        StringBuilder csv = new StringBuilder(
            "id,sessionId,network,externalId,author,content,sourceUrl,postedAt,"
                + "macedonianConfidence,mediaCount,donated\n"
        );
        extractedPostApplicationService.findAllBySessionId(sessionId).forEach(post -> csv
            .append(post.id()).append(',')
            .append(post.sessionId()).append(',')
            .append(csv(post.socialNetwork())).append(',')
            .append(csv(post.externalId())).append(',')
            .append(csv(post.authorHandle())).append(',')
            .append(csv(post.content())).append(',')
            .append(csv(post.sourceUrl())).append(',')
            .append(csv(post.postedAt())).append(',')
            .append(post.macedonianConfidence() == null ? "" : post.macedonianConfidence()).append(',')
            .append(post.mediaItems().size()).append(',')
            .append(post.donationBatchId() != null)
            .append('\n'));
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, attachmentName(sessionId, "csv"))
            .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
            .body(withUtf8Bom(csv.toString().getBytes(StandardCharsets.UTF_8)));
    }

    private String attachmentName(Long sessionId, String extension) {
        return "attachment; filename=\"x-session-%d.%s\"".formatted(sessionId, extension);
    }

    private String csv(Object value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.toString().replace("\"", "\"\"") + "\"";
    }

    private byte[] withUtf8Bom(byte[] content) {
        byte[] result = new byte[content.length + 3];
        result[0] = (byte) 0xEF;
        result[1] = (byte) 0xBB;
        result[2] = (byte) 0xBF;
        System.arraycopy(content, 0, result, 3, content.length);
        return result;
    }
}
