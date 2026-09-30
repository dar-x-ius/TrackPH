package ph.trackph.template.util;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(String status, String message, T data) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("success", "OK", data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>("success", message, data);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>("error", message, null);
    }
}
