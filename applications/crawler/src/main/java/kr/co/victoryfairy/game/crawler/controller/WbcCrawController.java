package kr.co.victoryfairy.game.crawler.controller;

import kr.co.victoryfairy.game.crawler.service.WbcGameCrawler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/craw/wbc")
public class WbcCrawController {

    private final WbcGameCrawler crawler;

    public WbcCrawController(WbcGameCrawler crawler) {
        this.crawler = crawler;
    }

    @GetMapping("/match-list")
    public CrawlResponse getMatchList(@RequestParam(name = "sYear") String sYear,
            @RequestParam(name = "sMonth", required = false) String sMonth) {
        crawler.crawMatchList(sYear, sMonth);
        return CrawlResponse.completed();
    }

}
