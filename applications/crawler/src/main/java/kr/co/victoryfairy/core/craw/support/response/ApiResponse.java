package kr.co.victoryfairy.core.craw.support.response;

import kr.co.victoryfairy.core.craw.support.response.error.ErrorMessage;
import kr.co.victoryfairy.core.craw.support.response.error.ErrorType;

public class ApiResponse<S> {

    private final ResultType result;

    private final S data;

    private final Long totalCnt;

    private final ErrorMessage error;

    private ApiResponse(ResultType result, S data, Long totalCnt, ErrorMessage error) {
        this.result = result;
        this.data = data;
        this.totalCnt = totalCnt;
        this.error = error;
    }

    public static ApiResponse<?> success() {
        return new ApiResponse<>(ResultType.SUCCESS, null, null, null);
    }

    public static <S> ApiResponse<S> success(S data) {
        return new ApiResponse<>(ResultType.SUCCESS, data, null, null);
    }

    public static <S> ApiResponse<S> success(S data, Long totalCnt) {
        return new ApiResponse<>(ResultType.SUCCESS, data, totalCnt, null);
    }

    public static ApiResponse<?> error(ErrorType error) {
        return new ApiResponse<>(ResultType.ERROR, null, null, new ErrorMessage(error));
    }

    public static ApiResponse<?> error(ErrorType error, Object errorData) {
        return new ApiResponse<>(ResultType.ERROR, null, null, new ErrorMessage(error, errorData));
    }

    public ResultType getResult() {
        return result;
    }

    public Object getData() {
        return data;
    }

    public Long getTotalCnt() {
        return totalCnt;
    }

    public ErrorMessage getError() {
        return error;
    }

}