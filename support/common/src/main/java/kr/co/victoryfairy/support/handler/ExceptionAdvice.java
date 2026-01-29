package kr.co.victoryfairy.support.handler;

import kr.co.victoryfairy.logging.sql.SqlLoggingHolder;
import kr.co.victoryfairy.support.constant.MessageEnum;
import kr.co.victoryfairy.support.exception.CustomException;
import kr.co.victoryfairy.support.model.CustomResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.text.MessageFormat;

@Slf4j
@RestControllerAdvice
public class ExceptionAdvice {

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<CustomResponse<String>> exception(NoHandlerFoundException e) {
        return CustomResponse.failed(HttpStatus.NOT_FOUND, e, MessageEnum.Valid.FAIL_NOT_FOUND_ENDPOINT.getDescKr());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<CustomResponse<String>> exception(IllegalArgumentException e) {
        log.error(e.getMessage(), e);
        return CustomResponse.failed(HttpStatus.INTERNAL_SERVER_ERROR, e, e.getMessage());
    }

    @ExceptionHandler(BindException.class)
    public CustomResponse<String> exception(BindException e) {
        log.error(e.getMessage(), e);
        return CustomResponse.failed(e, MessageEnum.Data.FAIL_NOT_NULL.getDescKr(), getCustomErrorMessage(e));
    }

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<CustomResponse<String>> exception(CustomException e) {
        log.error(e.getMessage(), e);

        if (!ObjectUtils.isEmpty(e.getStatusEnum())) {
            return CustomResponse.failed(e.getHttpStatus(), e.getStatusEnum());
        }
        else {
            // TODO ??
            return CustomResponse.failed(e.getHttpStatus(), e);
        }
    }

    /**
     * 파라미터 누락
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CustomResponse<String>> exception(MethodArgumentNotValidException e) {
        log.error(e.getMessage(), e);

        String resultMessage = "";

        if (e.getBindingResult().hasErrors()) {

            var filedErrors = e.getBindingResult().getFieldErrors();
            Object[] arguments = filedErrors.get(0).getArguments();
            arguments[0] = filedErrors.get(0).getField();

            resultMessage = this.getMessageSource(filedErrors.get(0).getDefaultMessage(), arguments);
        }
        return CustomResponse.failed(resultMessage, HttpStatus.BAD_REQUEST, MessageEnum.Common.REQUEST_PARAMETER);
    }

    /**
     * 데이터베이스 관련 예외 - SQL 쿼리 로깅
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<CustomResponse<String>> exception(DataAccessException e) {
        logSqlOnError(e);
        return CustomResponse.failed(HttpStatus.INTERNAL_SERVER_ERROR, e, MessageEnum.Common.REQUEST_FAIL.getDescKr());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<CustomResponse<String>> exception(Exception e) {
        // DB 관련 예외인 경우 SQL 로깅
        if (isDataAccessException(e)) {
            logSqlOnError(e);
        } else {
            log.error(e.getMessage(), e);
        }
        return CustomResponse.failed(e, MessageEnum.Common.REQUEST_FAIL);
    }

    ///////////////////////////////////////////

    /**
     * SQL 에러 발생 시 실행된 쿼리들을 포맷팅해서 로깅
     */
    private void logSqlOnError(Exception e) {
        var sqlList = SqlLoggingHolder.getSqlList();
        if (sqlList == null || sqlList.isEmpty()) {
            log.error("[SQL ERROR] {}", e.getMessage(), e);
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n╔══════════════════════════════════════════════════════════════╗");
        sb.append("\n║                    SQL ERROR OCCURRED                        ║");
        sb.append("\n╠══════════════════════════════════════════════════════════════╣");
        sb.append("\n║ Error: ").append(truncate(e.getMessage(), 55));
        sb.append("\n╠══════════════════════════════════════════════════════════════╣");
        sb.append("\n║ Executed Queries (").append(sqlList.size()).append("):").append(" ".repeat(40)).append("║");
        sb.append("\n╟──────────────────────────────────────────────────────────────╢");

        int idx = 1;
        for (var sqlInfo : sqlList) {
            sb.append("\n║ [").append(idx++).append("] ").append(sqlInfo.toFormattedString());
        }

        sb.append("\n╚══════════════════════════════════════════════════════════════╝");

        log.error(sb.toString(), e);
    }

    private boolean isDataAccessException(Exception e) {
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof DataAccessException ||
                    cause.getClass().getName().contains("SQLException") ||
                    cause.getClass().getName().contains("JpaException") ||
                    cause.getClass().getName().contains("HibernateException")) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private String truncate(String str, int maxLen) {
        if (str == null) return "";
        return str.length() > maxLen ? str.substring(0, maxLen) + "..." : str;
    }

    private String getCustomErrorMessage(BindException ex) {
        StringBuilder errorMessage = new StringBuilder();

        for (FieldError fieldError : ex.getFieldErrors()) {
            // 필드 이름과 메시지 부분만 추출
            errorMessage.append("Field: ")
                .append(fieldError.getField())
                .append(", Error: ")
                .append(fieldError.getDefaultMessage())
                .append("\n");
        }
        return errorMessage.toString();
    }

    private String getMessageSource(String messageKey, Object[] args) {
        // 메세지중 {0...1}부분 파라미터로 전환
        return MessageFormat.format("{0} {1}", args[0], messageKey);
    }

}