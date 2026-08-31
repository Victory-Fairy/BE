package kr.co.victoryfairy.support.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import kr.co.victoryfairy.support.constant.EnumDescriptor;
import kr.co.victoryfairy.support.constant.StatusEnum;
import kr.co.victoryfairy.support.exception.CustomException;
import lombok.Builder;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(title = "공통 Response", hidden = true)
@Builder
public class CustomResponse<T> {

    @Schema(title = "응답시간", example = "20210701123030")
    private String timestamp;

    @Schema(title = "응답코드", example = "200")
    private Integer status;

    @Schema(title = "오류 메세지", example = "")
    private String errorMsg;

    @Schema(title = "표출 메시지", example = "")
    private String message;

    // List 일때만 표기
    @Schema(title = "목록 갯수", example = "1")
    private Integer rowCount;

    // List 이고, Paging 일때 표기
    @Schema(title = "데이터 총 갯수", example = "1")
    private Long totalCount;

    @Schema(title = "데이터")
    private T data;

    /**
     * API Success
     */
    public static <T> CustomResponse<T> ok(T data) {
        return (CustomResponse<T>) CustomResponse.builder()
            .rowCount((data instanceof List list ? list.size() : null))
            .status(HttpStatus.OK.value())
            .data(data)
            .build();
    }

    /**
     * API Success
     */
    public static <T> CustomResponse<T> ok(T data, Long totalCount) {
        return (CustomResponse<T>) CustomResponse.builder()
            .rowCount((data instanceof List list ? list.size() : null))
            .totalCount(totalCount)
            .status(HttpStatus.OK.value())
            .data(data)
            .build();
    }

    public static <T> CustomResponse<T> ok(EnumDescriptor enumDescriptor) {
        return (CustomResponse<T>) CustomResponse.builder()
            .status(HttpStatus.OK.value())
            .message(enumDescriptor.getDescKr())
            .build();
    }

    public static ResponseEntity<CustomResponse<String>> failed(Exception e, EnumDescriptor descriptor) {
        var httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;

        var response = CustomResponse.<String>builder()
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .message(descriptor.getDescKr())
            .errorMsg(e.getMessage())
            .build();

        return ResponseEntity.status(httpStatus).body(response);
    }

    public static ResponseEntity<CustomResponse<String>> failed(String message, HttpStatus httpStatus,
            EnumDescriptor descriptor) {
        var response = CustomResponse.<String>builder()
            .status(httpStatus.value())
            .message(descriptor.getDescKr())
            .errorMsg(message)
            .build();

        return ResponseEntity.status(httpStatus).body(response);
    }

    /**
     * API Failed
     */
    public static <T> CustomResponse<T> failed(Exception e, String message) {
        return (CustomResponse<T>) CustomResponse.builder()
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .message(message)
            .build();
    }

    /**
     * API Failed
     */
    public static <T> CustomResponse<T> failed(Exception e, String message, String errorMsg) {
        return (CustomResponse<T>) CustomResponse.builder()
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .message(message)
            .errorMsg(errorMsg)
            .build();
    }

    /**
     * API Failed
     */
    public static ResponseEntity<CustomResponse<String>> failed(HttpStatus httpStatus, Exception e, String message) {
        var response = CustomResponse.<String>builder()
            .status(httpStatus.value())
            .message(message)
            .errorMsg(e.getMessage())
            .build();

        return ResponseEntity.status(httpStatus).body(response);
    }

    /**
     * API Failed
     */
    public static ResponseEntity<CustomResponse<String>> failed(HttpStatus httpStatus, StatusEnum statusEnum) {
        var response = CustomResponse.<String>builder()
            .status(statusEnum.getStatus())
            .message(statusEnum.getDescKr())
            .build();

        return ResponseEntity.status(httpStatus).body(response);
    }

    /**
     * API Failed
     */
    public static ResponseEntity<CustomResponse<String>> failed(HttpStatus httpStatus, CustomException e) {
        var response = CustomResponse.<String>builder().status(httpStatus.value()).message(e.getMessage()).build();

        return ResponseEntity.status(httpStatus).body(response);
    }

}