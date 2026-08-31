package kr.co.victoryfairy.core.craw.controller;

import kr.co.victoryfairy.core.craw.service.KboGameCrawler;
import kr.co.victoryfairy.support.constant.MessageEnum;
import kr.co.victoryfairy.support.model.CustomResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/craw")
public class CrawController {

    private final KboGameCrawler crawler;

    public CrawController(KboGameCrawler crawler) {
        this.crawler = crawler;
    }

    @GetMapping("/match-list")
    public CustomResponse<MessageEnum> getMatchList(@RequestParam(name = "sYear") String sYear,
            @RequestParam(name = "sMonth", required = false) String sMonth) {
        crawler.crawMatchList(sYear, sMonth);
        return CustomResponse.ok(MessageEnum.Common.REQUEST);
    }

    @GetMapping("/match-month-list")
    public CustomResponse<MessageEnum> getMatchMonthList(@RequestParam(name = "sYear") String sYear,
            @RequestParam(name = "sMonth") String sMonth) {
        crawler.crawMatchListByMonth(sYear, sMonth);
        return CustomResponse.ok(MessageEnum.Common.REQUEST);
    }

    @GetMapping("/match-detail")
    public CustomResponse<MessageEnum> getMatchDetail(@RequestParam(name = "sYear") String sYear) {
        crawler.crawMatchDetail(sYear);
        return CustomResponse.ok(MessageEnum.Common.REQUEST);
    }

    @GetMapping("/match-detail/{id}")
    public CustomResponse<MessageEnum> getMatchDetailById(@PathVariable String id) {
        crawler.crawMatchDetailById(id);
        return CustomResponse.ok(MessageEnum.Common.REQUEST);
    }

}
