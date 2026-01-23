package kr.co.victoryfairy.core.craw.controller;

import kr.co.victoryfairy.core.craw.service.CrawService;
import kr.co.victoryfairy.support.constant.MessageEnum;
import kr.co.victoryfairy.support.model.CustomResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/craw/wbc")
public class WbcCrawController {

    private final CrawService crawService;

    public WbcCrawController(@Qualifier("wbcCrawService") CrawService crawService) {
        this.crawService = crawService;
    }

    @GetMapping("/match-list")
    public CustomResponse<MessageEnum> getMatchList(@RequestParam(name = "sYear") String sYear,
            @RequestParam(name = "sMonth", required = false) String sMonth) {
        crawService.crawMatchList(sYear, sMonth);
        return CustomResponse.ok(MessageEnum.Common.REQUEST);
    }

}
