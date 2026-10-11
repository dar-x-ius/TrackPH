package ph.trackph.milestone.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ph.trackph.milestone.dto.response.AttachmentResponse;
import ph.trackph.milestone.service.AttachmentService;
import ph.trackph.milestone.util.ApiResponse;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/milestones/{milestoneId}/attachments")
@Tag(name = "Milestone Attachments", description = "Proof-of-progress files for milestones")
public class AttachmentController {
    private final AttachmentService attachmentService;

    @GetMapping
    @Operation(summary = "List attachments for a milestone")
    public ResponseEntity<ApiResponse<List<AttachmentResponse>>> list(@PathVariable Long milestoneId) {
        return ResponseEntity.ok(ApiResponse.success(attachmentService.findByMilestone(milestoneId)));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a proof attachment (AGENCY only)")
    public ResponseEntity<ApiResponse<AttachmentResponse>> upload(@PathVariable Long milestoneId, @RequestParam("file") MultipartFile file, Authentication auth) throws IOException {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Attachment uploaded", attachmentService.upload(milestoneId, file, userId)));
    }

    @GetMapping("/{attachmentId}/download")
    @Operation(summary = "Download an attachment's file")
    public ResponseEntity<byte[]> download(@PathVariable Long milestoneId, @PathVariable Long attachmentId) throws IOException {
        AttachmentService.FileDownload download = attachmentService.download(milestoneId, attachmentId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(download.filename()).build().toString())
                .body(download.content());
    }

    @DeleteMapping("/{attachmentId}")
    @Operation(summary = "Delete an attachment (uploader or ADMIN only)")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long milestoneId, @PathVariable Long attachmentId, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        boolean isAdmin = auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        attachmentService.delete(milestoneId, attachmentId, userId, isAdmin);
        return ResponseEntity.ok(ApiResponse.success("Deleted", null));
    }

    public AttachmentController(final AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }
}
