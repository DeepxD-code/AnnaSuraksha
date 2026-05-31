package com.annasuraksha.model.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final T       data;
    private final String  error;
    private final Meta    meta;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Meta {
        private final Instant timestamp;
        private final String  requestId;
        private final String  code;

        public Meta(Instant timestamp, String requestId, String code) {
            this.timestamp = timestamp;
            this.requestId = requestId;
            this.code = code;
        }

        public Instant getTimestamp() { return this.timestamp; }
        public String getRequestId() { return this.requestId; }
        public String getCode() { return this.code; }

        public static MetaBuilder builder() { return new MetaBuilder(); }

        public static class MetaBuilder {
            private Instant timestamp;
            private String requestId;
            private String code;
            public MetaBuilder timestamp(Instant timestamp) { this.timestamp = timestamp; return this; }
            public MetaBuilder requestId(String requestId) { this.requestId = requestId; return this; }
            public MetaBuilder code(String code) { this.code = code; return this; }
            public Meta build() { return new Meta(this.timestamp, this.requestId, this.code); }
        }
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .meta(Meta.builder()
                        .timestamp(Instant.now())
                        .requestId(UUID.randomUUID().toString())
                        .build())
                .build();
    }

    public static <T> ApiResponse<T> success(T data, String message, String traceId) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .meta(Meta.builder()
                        .timestamp(Instant.now())
                        .requestId(traceId != null ? traceId : UUID.randomUUID().toString())
                        .build())
                .build();
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(message)
                .meta(Meta.builder()
                        .timestamp(Instant.now())
                        .requestId(UUID.randomUUID().toString())
                        .code(code)
                        .build())
                .build();
    }

    public static <T> ApiResponse<T> validationError(String field, String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .error("Validation Error: " + field + " - " + message)
                .meta(Meta.builder()
                        .timestamp(Instant.now())
                        .requestId(UUID.randomUUID().toString())
                        .code("VALIDATION_ERROR")
                        .build())
                .build();
    }

    public ApiResponse(boolean success, T data, String error, Meta meta) {
        this.success = success;
        this.data = data;
        this.error = error;
        this.meta = meta;
    }

    public boolean isSuccess() { return this.success; }
    public T getData() { return this.data; }
    public String getError() { return this.error; }
    public Meta getMeta() { return this.meta; }

    public static <T> ApiResponseBuilder<T> builder() { return new ApiResponseBuilder<>(); }

    public static class ApiResponseBuilder<T> {
        private boolean success;
        private T data;
        private String error;
        private Meta meta;

        public ApiResponseBuilder<T> success(boolean success) { this.success = success; return this; }
        public ApiResponseBuilder<T> data(T data) { this.data = data; return this; }
        public ApiResponseBuilder<T> error(String error) { this.error = error; return this; }
        public ApiResponseBuilder<T> meta(Meta meta) { this.meta = meta; return this; }

        public ApiResponse<T> build() { return new ApiResponse<>(this.success, this.data, this.error, this.meta); }
    }
}
