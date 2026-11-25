package org.example.recommendhouse.service;
import org.example.recommendhouse.dto.PolicyNews;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import java.io.StringReader;
import java.net.URI;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Arrays;
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

    @Value("NbgvcGOvtRgvFU0Gt7r6aAg1tx7Fmk9xFUL2hQpENQ8Ln1k26rgPXNIdK69iK2kKvqVHg1S1kL2yTmmPOWDerw%3D%3D")
    private String apiKey;

    private final RestTemplate restTemplate;
    private static final String API_URL = "https://apis.data.go.kr/1371000/policyNewsService2/policyNewsList2";

    public PolicyNewsService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // 클래스 상단(필드)에 선언해 두면 매번 리스트를 새로 생성하지 않아 훨씬 빠릅니다.
    private static final List<String> KEYWORDS = List.of(
            // 기본 및 주거유형
            "부동산", "주택", "아파트", "오피스텔", "빌라", "다세대", "다가구", "연립",
            "주상복합", "도시형생활주택", "상가", "지식산업센터", "단독주택", "타운하우스",

            // 청약 및 분양
            "청약", "분양", "모집공고", "입주자모집", "사전청약", "무순위", "줍줍",
            "견본주택", "모델하우스", "특별공급", "생애최초", "청약홈", "분상제",
            "분양가", "미분양", "전매제한", "입주권", "분양권", "입주장",

            // 신도시 및 공공임대
            "신도시", "공공임대", "행복주택", "국민임대", "영구임대", "매입임대",
            "전세임대", "장기전세", "청년주택", "안심주택", "뉴홈", "보금자리",
            "선도지구", "공공택지", "택지지구",

            // 정비사업 및 개발
            "재건축", "재개발", "정비사업", "리모델링", "모아타운", "신통기획",
            "가로주택", "안전진단", "관리처분", "토허제", "토지거래허가", "규제지역",

            // 거래, 금융 및 세금
            "전세", "월세", "임대차", "전월세", "매매", "실거래가", "집값", "호가",
            "신고가", "급매", "갭투자", "역전세", "깡통전세", "전세사기", "보증보험",
            "주담대", "주택담보대출", "디딤돌", "버팀목", "특례보금자리", "신생아특례",
            "종부세", "양도세", "취득세", "공시가격", "공시지가", "경매", "공매",

            // 기관 및 주요 브랜드
            "국토부", "LH", "SH", "GH", "HUG", "한국부동산원",
            "래미안", "자이", "힐스테이트", "푸르지오", "e편한세상", "더샵",
            "롯데캐슬", "아이파크", "디에이치", "아크로", "호반써밋", "우미린", "포레나"
    );

    public List<PolicyNews> getPolicyNews() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        LocalDate limitDate = today.minusYears(1); // 최대 1년 전까지만 탐색
        LocalDate currentEnd = today;

        List<PolicyNews> newsList = new ArrayList<>();

        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

            // currentEnd가 1년 전 날짜보다 뒤에 있는 동안 3일 단위로 반복 탐색
            while (currentEnd.isAfter(limitDate)) {
                LocalDate currentStart = currentEnd.minusDays(2); // start~end 포함 총 3일치 (만약 기존처럼 4일치도 가능하면 3으로 변경)
                if (currentStart.isBefore(limitDate)) {
                    currentStart = limitDate;
                }

                String url = API_URL +
                        "?serviceKey=" + apiKey +
                        "&startDate=" + currentStart.format(DateTimeFormatter.BASIC_ISO_DATE) +
                        "&endDate=" + currentEnd.format(DateTimeFormatter.BASIC_ISO_DATE) +
                        "&type=xml";

                // 이미 인코딩된 URL을 URI로 변환
                URI uri = URI.create(url);

                // URI 객체를 전달하여 중복 인코딩 방지
                ResponseEntity<String> response =
                        restTemplate.getForEntity(uri, String.class);

                System.out.println("조회 구간: " + currentStart + " ~ " + currentEnd + " | 응답 상태: " + response.getStatusCode());

                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    Document document = builder.parse(new InputSource(new StringReader(response.getBody())));
                    NodeList newsItems = document.getElementsByTagName("NewsItem");

                    System.out.println("start of policy news total : " + newsItems.getLength());
                    for (int i = 0; i < newsItems.getLength(); i++) {
                        Element item = (Element) newsItems.item(i);

                        String title = getElementText(item, "Title");
                        if (title == null) continue;

                        // 대소문자 구분 없이 영문 약어(LH, SH 등)를 잡기 위해 대문자 변환 후 비교
                        String upperTitle = title.toUpperCase();
                        boolean isRelated = KEYWORDS.stream().anyMatch(k -> upperTitle.contains(k.toUpperCase()));
                        if (!isRelated) {
                            continue;
                        }

                        PolicyNews news = new PolicyNews();
                        news.setTitle(title);

                        // ThumbnailUrl 처리 추가
                        String thumbnailUrl = getElementText(item, "ThumbnailUrl");
                        if (thumbnailUrl != null) {
                            thumbnailUrl = thumbnailUrl.replaceAll("\\<!\\[CDATA\\[|\\]\\]\\>", "").trim();
                        }
                        news.setThumbnailUrl(thumbnailUrl);

                        //--- 뉴스 부제목 -------------------------------------------------
                        String dataContents = getElementText(item, "DataContents");
                        String subtitle1 = extractFirstSentence(dataContents);
                        news.setSubTitle1(subtitle1);

                        String newsURL = getElementText(item, "OriginalUrl");
                        if (newsURL != null) {
                            newsURL = newsURL.replaceAll("\\<!\\[CDATA\\[|\\]\\]\\>", "").trim();
                        }
                        news.setOriginalUrl(newsURL);
                        //---------------------------뉴스 부제목 끝

                        // 날짜 변환 후 설정
                        String approveDate = getFormattedDate(item, "ApproveDate");
                        news.setDate(approveDate);

                        newsList.add(news);
                        System.out.println("size : " + newsList.size());

                        // 3개가 모두 차면 내부 for문 즉시 종료
                        if (newsList.size() >= 3) {
                            break;
                        }
                    }
                }

                // [핵심] 해당 3일 구간에서 조건에 맞는 뉴스가 1개라도 발견되었다면 탐색 종료(스탑) 및 반환!
                if (!newsList.isEmpty()) {
                    return newsList;
                }

                // 뉴스가 없으면 다음 과거 구간(3일 전)으로 이동
                currentEnd = currentStart.minusDays(1);

                // 연속 호출로 인한 API 차단(429 Too Many Requests) 방지용 짧은 대기
                Thread.sleep(100);
            }

            return newsList; // 1년 치를 다 뒤졌는데도 없으면 빈 리스트 반환

        } catch (Exception e) {
            logger.error("정책 뉴스 API 호출 실패");
            logger.error("예외 종류: {}", e.getClass().getName());
            logger.error("예외 메시지: {}", e.getMessage(), e);

            if (e instanceof HttpStatusCodeException) {
                HttpStatusCodeException httpError = (HttpStatusCodeException) e;

                logger.error("HTTP 상태 코드: {}", httpError.getStatusCode());
                logger.error("API 응답 내용: {}", httpError.getResponseBodyAsString());
            }

            // 만약 탐색 도중 에러가 났더라도, 그전까지 모아둔 뉴스가 있다면 반환하고 없으면 빈 리스트 반환
            return newsList.isEmpty() ? Collections.emptyList() : newsList;
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



