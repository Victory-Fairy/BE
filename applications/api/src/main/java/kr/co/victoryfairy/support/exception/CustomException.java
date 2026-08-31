package kr.co.victoryfairy.support.exception;

import kr.co.victoryfairy.support.constant.EnumDescriptor;
import kr.co.victoryfairy.support.constant.StatusEnum;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 일반적인 Exception 이외는 Custom Exception 으로 던진다. <pre>
 *     example : newsCrawlingCustomRepository.findNewsCrawlingById(id.getId())
 *                 .orElseThrow(() -> CustomException.of(MessageEnum.Data.FAIL_NO_RESULT));
 * </pre>
 */
@Getter
public class CustomException extends RuntimeException {

    private HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR; // HttpStatus 필드 추가

    private StatusEnum statusEnum;

    public CustomException() {
        super();
        this.statusEnum = StatusEnum.STATUS_901;
    }

    public CustomException(String message) {
        super(message);
    }

    public CustomException(String message, Throwable cause) {
        super(message, cause);
    }

    public CustomException(EnumDescriptor descriptor, Throwable cause) {
        super(descriptor.getDescKr(), cause);
    }

    public CustomException(EnumDescriptor descriptor) {
        super(descriptor.getDescKr());
        this.httpStatus = HttpStatus.BAD_REQUEST;
    }

    public CustomException(HttpStatus status, EnumDescriptor descriptor) {
        super(descriptor.getDescKr());
        this.httpStatus = status;
    }

    public CustomException(HttpStatus status, StatusEnum statusEnum) {
        super(statusEnum.getDescKr());
        this.httpStatus = status;
        this.statusEnum = statusEnum;
    }

    // HttpStatus를 가져오는 Getter
    // public HttpStatus getHttpStatus() {
    // return httpStatus;
    // }
    //
    // public StatusEnum getGet
    // return httpStatus;
    // }

    public static CustomException of() {
        return new CustomException();
    }

    public static CustomException of(String message, Throwable cause) {
        return new CustomException(message, cause);
    }

    public static CustomException of(EnumDescriptor descriptor, Throwable cause) {
        return new CustomException(descriptor, cause);
    }

    public static CustomException of(EnumDescriptor descriptor) {
        return new CustomException(descriptor);
    }

}