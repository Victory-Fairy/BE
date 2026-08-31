package kr.co.victoryfairy.support.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class HttpClientUtils {

    public static String doGet(String url, Map<String, String> param, Map<String, String> headers) {
        var httpclient = HttpClients.createDefault();

        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectTimeout(10000)
            .setConnectionRequestTimeout(10000)
            .setSocketTimeout(10000)
            .build();

        String resultString = "";
        CloseableHttpResponse response = null;
        try {
            URIBuilder builder = new URIBuilder(url);
            if (param != null) {
                for (String key : param.keySet()) {
                    builder.addParameter(key, param.get(key));
                }
            }
            URI uri = builder.build();

            HttpGet httpGet = new HttpGet(uri);

            httpGet.setConfig(requestConfig);
            httpGet.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; rv:6.0.2) Gecko/20100101 Firefox/6.0.2");
            if (headers != null) {
                for (String headerKey : headers.keySet()) {
                    httpGet.addHeader(headerKey, headers.get(headerKey));
                }

            }

            response = httpclient.execute(httpGet);
            log.warn("doGet: ");
            log.warn("response: " + response.toString());
            log.warn("response.getStatusLine().getStatusCode(): " + response.getStatusLine().getStatusCode());
            log.warn("response.getEntity: " + response.getEntity().toString());
            if (response.getStatusLine().getStatusCode() == 200) {
                resultString = EntityUtils.toString(response.getEntity(), "UTF-8");
            }
        }
        catch (Exception e) {
            log.error(e.getMessage());
        }
        finally {
            try {
                if (response != null) {
                    response.close();
                }
                httpclient.close();
            }
            catch (IOException e) {
                log.error(e.getMessage());
            }
        }
        return resultString;
    }

    public static String doPost(String url, Map<String, String> param) {
        var httpClient = HttpClients.createDefault();

        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectTimeout(10000)
            .setConnectionRequestTimeout(10000)
            .setSocketTimeout(10000)
            .build();
        CloseableHttpResponse response = null;
        String resultString = "";
        try {
            HttpPost httpPost = new HttpPost(url);
            httpPost.setConfig(requestConfig);

            if (param != null) {
                List<NameValuePair> paramList = new ArrayList<NameValuePair>();
                for (String key : param.keySet()) {
                    paramList.add(new BasicNameValuePair(key, param.get(key)));
                }

                UrlEncodedFormEntity entity = new UrlEncodedFormEntity(paramList);
                httpPost.setEntity(entity);
            }

            response = httpClient.execute(httpPost);
            log.warn("response: " + response.toString());
            log.warn("response.getStatusLine().getStatusCode(): " + response.getStatusLine().getStatusCode());
            log.warn("response.getEntity: " + response.getEntity().toString());

            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                resultString = EntityUtils.toString(response.getEntity(), "utf-8");
            }
        }
        catch (Exception e) {
            log.error(e.getMessage());
        }
        finally {
            try {
                response.close();
            }
            catch (IOException e) {
                log.error(e.getMessage());
            }
        }
        return resultString;
    }

    public static String doPostJson(String url, String json) {
        CloseableHttpClient httpClient = HttpClients.createDefault();

        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectTimeout(10000)
            .setConnectionRequestTimeout(10000)
            .setSocketTimeout(10000)
            .build();
        CloseableHttpResponse response = null;
        String resultString = "";
        try {
            HttpPost httpPost = new HttpPost(url);
            httpPost.setConfig(requestConfig);
            StringEntity entity = new StringEntity(json, ContentType.APPLICATION_JSON);

            httpPost.setEntity(entity);

            response = httpClient.execute(httpPost);

            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                resultString = EntityUtils.toString(response.getEntity(), "utf-8");
            }
        }
        catch (Exception e) {
            log.error(e.getMessage());
        }
        finally {
            try {
                response.close();
            }
            catch (IOException e) {
                log.error(e.getMessage());
            }
        }
        return resultString;
    }

}