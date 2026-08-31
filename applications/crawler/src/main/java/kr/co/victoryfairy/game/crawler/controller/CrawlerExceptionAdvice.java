package kr.co.victoryfairy.game.crawler.controller;

import kr.co.victoryfairy.game.crawler.service.CrawlRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class CrawlerExceptionAdvice {

    @ExceptionHandler(CrawlRequestException.class)
    ResponseEntity<CrawlResponse> handle(CrawlRequestException exception) {
        return ResponseEntity.badRequest()
            .body(CrawlResponse.failed(HttpStatus.BAD_REQUEST.value(), exception.getMessage(), null));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<CrawlResponse> handle(Exception exception) {
        return ResponseEntity.internalServerError()
            .body(CrawlResponse.failed(HttpStatus.INTERNAL_SERVER_ERROR.value(), "요청이 실패 하였습니다.",
                    exception.getMessage()));
    }
}
