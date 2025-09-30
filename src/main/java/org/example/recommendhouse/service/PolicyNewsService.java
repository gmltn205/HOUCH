package org.example.recommendhouse.service;
import org.example.recommendhouse.dto.PolicyNews;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import java.io.StringReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

@Service
public class PolicyNewsService {
    private static final Logger logger = LoggerFactory.getLogger(PolicyNewsService.class);

    @Value("NbgvcGOvtRgvFU0Gt7r6aAg1tx7Fmk9xFUL2hQpENQ8Ln1k26rgPXNIdK69iK2kKvqVHg1S1kL2yTmmPOWDerw==")
    private String apiKey;

    private final RestTemplate restTemplate;
    private static final String API_URL = "http://apis.data.go.kr/1371000/policyNewsService/policyNewsList";

    public PolicyNewsService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<PolicyNews> getPolicyNews() {
        try {
            LocalDate endDate = LocalDate.of(2025, 1, 15);
            LocalDate startDate = endDate.minusDays(3);

            String url = API_URL +
                    "?serviceKey=" + apiKey +
                    "&startDate=" + startDate.format(DateTimeFormatter.BASIC_ISO_DATE) +
                    "&endDate=" + endDate.format(DateTimeFormatter.BASIC_ISO_DATE) +
                    "&type=xml";

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                Document document = builder.parse(new InputSource(new StringReader(response.getBody())));

                NodeList newsItems = document.getElementsByTagName("NewsItem");
                List<PolicyNews> newsList = new ArrayList<>();

                System.out.println("start of policy news total : " +newsItems.getLength());
                for (int i = 0; i < newsItems.getLength(); i++) {
                    Element item = (Element) newsItems.item(i);

                    PolicyNews news = new PolicyNews();
                    news.setTitle(getElementText(item, "Title"));
                    String title = getElementText(item, "Title");
                    // 🔹 "부동산"이 포함된 뉴스만 리스트에 추가
                    if (!(title.contains("부동산") || title.contains("청약") ||title.contains("주택") || title.contains("신도시") || title.contains("공공임대주택")))
                    {
                        continue; // 키워드가 없으면 건너뛰기
                    }

                    // ThumbnailUrl 처리 추가
                    String thumbnailUrl = getElementText(item, "ThumbnailUrl");
                    // CDATA 제거 및 공백 제거
                    thumbnailUrl = thumbnailUrl.replaceAll("\\<!\\[CDATA\\[|\\]\\]\\>", "").trim();
                    news.setThumbnailUrl(thumbnailUrl);

//--- 뉴스 부제목 -------------------------------------------------
                    // DataContents에서 첫 번째 문장 추출하여 summary로 설정
                    String dataContents = getElementText(item, "DataContents");
                    String subtitle1 = extractFirstSentence(dataContents);
                    news.setSubTitle1(subtitle1);

                    String newsURL = getElementText(item, "OriginalUrl");
                    // CDATA 제거 및 공백 제거
                    newsURL = newsURL.replaceAll("\\<!\\[CDATA\\[|\\]\\]\\>", "").trim();

                    news.setOriginalUrl(newsURL);
    //---------------------------뉴스 부제목 끝
                    // 날짜 변환 후 설정
                    String approveDate = getFormattedDate(item, "ApproveDate");
                    news.setDate(approveDate);  // PolicyNews클래스 dataContents


                    newsList.add(news);
                    System.out.println("size : "+newsList.size());
                    if(newsList.size() >= 3) {break;}
                }


                return newsList;
            }

            return Collections.emptyList();
        } catch (Exception e) {
            logger.error("정책 뉴스 API 호출 실패", e);
            return Collections.emptyList();
        }
    }
    // 첫 번째 문장 추출 메서드
    private String extractFirstSentence(String content) {
        content = content.replaceAll("\\<.*?\\>", ""); // HTML 태그 제거
        content = content.trim(); // 앞뒤 공백 제거

        int index = content.indexOf(".");
        if (index != -1) {
            content = content.substring(0, index + 1);
        }

        return content + "...";
    }
    private String getElementText(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            String content = nodes.item(0).getTextContent();
            return content.replaceAll("<!\\[CDATA\\[|\\]\\]>", "").trim();
        }
        return "";
    }
    private String getURl(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            String content = nodes.item(0).getTextContent();
            return content.replaceAll("<!\\[CDATA\\[|\\]\\]>", "").trim();
        }
        return "";
    }

    private String getFormattedDate(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            String content = nodes.item(0).getTextContent().trim();

            // 날짜 포맷 변환 (예: "02/16/2024 23:01:00" → "2024-02-16")
            try {
                DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss");
                DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

                LocalDate date = LocalDate.parse(content, inputFormatter);
                return date.format(outputFormatter);
            } catch (Exception e) {
                System.out.println(e);
                return "날짜 오류"; // 변환 실패 시 기본값 반환
            }
        }
        return "날짜 없음";
    }
}



