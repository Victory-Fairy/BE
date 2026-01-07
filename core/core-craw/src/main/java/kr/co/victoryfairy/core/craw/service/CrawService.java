package kr.co.victoryfairy.core.craw.service;

public interface CrawService {

    void crawMatchList(String sYear, String sMonth);

    void crawMatchDetail(String sYear);

    void crawMatchDetailById(String id);

    void crawMatchListByMonth(String sYear, String sMonth);

}
