package kr.co.victoryfairy.game.crawler.controller;

import kr.co.victoryfairy.game.crawler.service.KboGameCrawler;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/craw")
public class CrawController {

    private final KboGameCrawler crawler;

    public CrawController(KboGameCrawler crawler) {
        this.crawler = crawler;
    }

    @GetMapping("/match-list")
    public CrawlResponse getMatchList(@RequestParam(name = "sYear") String sYear,
            @RequestParam(name = "sMonth", required = false) String sMonth) {
        crawler.crawMatchList(sYear, sMonth);
        return CrawlResponse.completed();
    }

    @GetMapping("/match-month-list")
    public CrawlResponse getMatchMonthList(@RequestParam(name = "sYear") String sYear,
            @RequestParam(name = "sMonth") String sMonth) {
        crawler.crawMatchListByMonth(sYear, sMonth);
        return CrawlResponse.completed();
    }

    @GetMapping("/match-detail")
    public CrawlResponse getMatchDetail(@RequestParam(name = "sYear") String sYear) {
        crawler.crawMatchDetail(sYear);
        return CrawlResponse.completed();
    }

    @GetMapping("/match-detail/{id}")
    public CrawlResponse getMatchDetailById(@PathVariable String id) {
        crawler.crawMatchDetailById(id);
        return CrawlResponse.completed();
    }

}
