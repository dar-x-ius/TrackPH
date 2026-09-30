package ph.trackph.template.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ph.trackph.template.dto.request.SampleRequest;
import ph.trackph.template.dto.response.SampleResponse;
import ph.trackph.template.service.SampleService;
import ph.trackph.template.util.ApiResponse;
import java.util.List;

/**
 * Example controller — delete and replace with your own.
 * Pattern: return ApiResponse.success(data) / ApiResponse.error(message).
 */
@RestController
@RequestMapping("/api/v1/samples")
@Tag(name = "Samples", description = "Sample CRUD — replace with your resource")
public class SampleController {
    private final SampleService service;

    @GetMapping
    @Operation(summary = "List all samples")
    public ResponseEntity<ApiResponse<List<SampleResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.success(service.findAll()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get sample by ID")
    public ResponseEntity<ApiResponse<SampleResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(service.findById(id)));
    }

    @PostMapping
    @Operation(summary = "Create a sample")
    public ResponseEntity<ApiResponse<SampleResponse>> create(@Valid @RequestBody SampleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Created successfully", service.create(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a sample")
    public ResponseEntity<ApiResponse<SampleResponse>> update(@PathVariable Long id, @Valid @RequestBody SampleRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Updated successfully", service.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a sample")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Deleted successfully", null));
    }

    public SampleController(final SampleService service) {
        this.service = service;
    }
}
