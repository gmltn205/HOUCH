package org.example.recommendhouse.service;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import retrofit2.HttpException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class FilteredRecommendService {

    private final OpenAiService openAiService;
    
    // CSV 파일 리소스 주입
    @Value("classpath:lh_happyhouse_list_with_coords.csv")
    private Resource csvResource;
    
    // 재시도 관련 상수
    private static final int MAX_RETRIES = 5;
    private static final long INITIAL_BACKOFF_MS = 1000; // 1초
    
    /**
     * 필터링된 데이터 기반 추천 서비스
     */
    public String generateFilteredRecommendation(String userMessage) {
        try {
            // 1. 지역 정보 추출
            String region = extractRegionFromMessage(userMessage);
            if (region == null || region.isEmpty()) {
                return "죄송합니다. 질문에서 지역을 파악하지 못했습니다. 지역명을 포함하여 다시 질문해주세요. " +
                       "예: '경기도 성남시에 살 수 있는 공공임대주택을 추천해 주세요.'";
            }
            
            log.info("사용자 메시지에서 추출한 지역: {}", region);
            
            // 2. 예산 정보 추출
            Map<String, Object> budget = extractBudgetFromMessage(userMessage);
            Long depositLimit = (Long) budget.get("deposit");
            Long rentLimit = (Long) budget.get("rent");
            
            log.info("추출된 예산 정보 - 보증금: {}, 월세: {}", 
                    depositLimit != null ? depositLimit + "만원" : "제한 없음", 
                    rentLimit != null ? rentLimit + "만원" : "제한 없음");
            
            // 3. CSV에서 필터링된 주택 데이터 가져오기
            List<Map<String, String>> filteredHouses = getFilteredHousesFromCsv(region, depositLimit, rentLimit);
            log.info("필터링 결과: {} 건의 주택 발견", filteredHouses.size());
            
            if (filteredHouses.isEmpty()) {
                return String.format(
                    "죄송합니다. '%s' 지역에서 조건에 맞는 주택을 찾을 수 없습니다. " +
                    "다른 지역이나 예산 조건으로 다시 시도해보세요.", region);
            }
            
            // 4. GPT에 전달할 데이터 준비
            String filteredData = prepareFilteredDataForGpt(filteredHouses, region, depositLimit, rentLimit);
            
            // 5. GPT에 추천 요청
            return getRecommendationFromGpt(userMessage, filteredData);
            
        } catch (Exception e) {
            log.error("추천 생성 중 오류 발생", e);
            return "죄송합니다. 추천 생성 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.";
        }
    }
    
    /**
     * CSV 파일에서 필터링된 주택 정보 가져오기
     */
    private List<Map<String, String>> getFilteredHousesFromCsv(String region, Long depositLimit, Long rentLimit) {
        List<Map<String, String>> result = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(csvResource.getInputStream(), StandardCharsets.UTF_8))) {
            
            // 헤더 읽기
            String headerLine = reader.readLine();
            if (headerLine == null) {
                log.error("CSV 파일 헤더가 없습니다.");
                return result;
            }
            
            String[] headers = headerLine.split(",");
            
            // 데이터 읽기
            String line;
            while ((line = reader.readLine()) != null) {
                String[] values = line.split(",");
                
                if (values.length < headers.length) {
                    continue; // 잘못된 데이터 건너뛰기
                }
                
                Map<String, String> houseData = new HashMap<>();
                
                // 데이터 매핑
                for (int i = 0; i < headers.length; i++) {
                    houseData.put(headers[i], i < values.length ? values[i] : "");
                }
                
                // 1. 지역 필터링
                String cityValue = houseData.get("광역시도");
                String districtValue = houseData.get("시군구");
                String addressValue = houseData.get("도로명주소");
                
                boolean regionMatch = false;
                
                if (cityValue != null && cityValue.contains(region)) {
                    regionMatch = true;
                } else if (districtValue != null && districtValue.contains(region)) {
                    regionMatch = true;
                } else if (addressValue != null && addressValue.contains(region)) {
                    regionMatch = true;
                }
                
                if (!regionMatch) {
                    continue; // 지역이 일치하지 않으면 건너뛰기
                }
                
                // 2. 예산 필터링 (보증금)
                if (depositLimit != null) {
                    try {
                        long houseDeposit = Long.parseLong(houseData.getOrDefault("임대보증금", "0"));
                        if (houseDeposit/10000 > depositLimit) {
                            continue; // 보증금이 예산을 초과하면 건너뛰기
                        }
                    } catch (NumberFormatException e) {
                        // 숫자 변환 오류 시 일단 포함 (나중에 GPT가 판단)
                    }
                }
                
                // 3. 예산 필터링 (월세)
                if (rentLimit != null) {
                    try {
                        long houseRent = Long.parseLong(houseData.getOrDefault("월임대료", "0"));
                        if (houseRent/10000 > rentLimit) {
                            continue; // 월세가 예산을 초과하면 건너뛰기
                        }
                    } catch (NumberFormatException e) {
                        // 숫자 변환 오류 시 일단 포함 (나중에 GPT가 판단)
                    }
                }
                
                // 모든 필터를 통과하면 결과에 추가
                result.add(houseData);
            }
            
        } catch (Exception e) {
            log.error("CSV 파일 처리 중 오류 발생", e);
        }
        
        return result;
    }
    
    /**
     * GPT에게 전달할 필터링된 데이터 준비
     */
    private String prepareFilteredDataForGpt(List<Map<String, String>> filteredHouses, 
                                          String region, 
                                          Long depositLimit, 
                                          Long rentLimit) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("# 필터링된 공공임대주택 정보\n\n");
        sb.append("## 검색 조건\n");
        sb.append("- 지역: ").append(region).append("\n");
        
        if (depositLimit != null) {
            sb.append("- 최대 보증금: ").append(depositLimit).append("만원\n");
        }
        
        if (rentLimit != null) {
            sb.append("- 최대 월세: ").append(rentLimit).append("만원\n");
        }
        
        sb.append("\n## 검색 결과 (총 ").append(filteredHouses.size()).append("건)\n\n");
        
        // 최대 20개만 보여주기 (너무 많은 데이터는 GPT 토큰 한도 초과 가능성)
        int limit = Math.min(filteredHouses.size(), 20);
        
        for (int i = 0; i < limit; i++) {
            Map<String, String> house = filteredHouses.get(i);
            
            sb.append("### 주택 #").append(i + 1).append("\n");
            sb.append("- 단지명: ").append(house.getOrDefault("단지명", "정보없음")).append("\n");
            sb.append("- 임대종류: ").append(house.getOrDefault("임대종류", "정보없음")).append("\n");
            sb.append("- 주소: ").append(house.getOrDefault("광역시도", ""))
              .append(" ").append(house.getOrDefault("시군구", ""))
              .append(" ").append(house.getOrDefault("도로명주소", "정보없음")).append("\n");
            sb.append("- 보증금: ").append(house.getOrDefault("임대보증금", "정보없음")).append("만원\n");
            sb.append("- 월임대료: ").append(house.getOrDefault("월임대료", "정보없음")).append("만원\n");
            sb.append("- 전용면적: ").append(house.getOrDefault("공급면적(전용)", "정보없음")).append("㎡\n");
            sb.append("- 주택유형: ").append(house.getOrDefault("주택유형", "정보없음")).append("\n");
            sb.append("- 난방방식: ").append(house.getOrDefault("난방방식", "정보없음")).append("\n");
            sb.append("- 세대수: ").append(house.getOrDefault("세대수", "정보없음")).append("\n");
            sb.append("- 주차수: ").append(house.getOrDefault("주차수", "정보없음")).append("\n");
            sb.append("- 준공일자: ").append(house.getOrDefault("준공일자", "정보없음")).append("\n");
            sb.append("- 승강기여부: ").append(house.getOrDefault("승강기설치여부", "정보없음")).append("\n");
            sb.append("\n");
        }
        
        if (filteredHouses.size() > limit) {
            sb.append("*참고: 총 ").append(filteredHouses.size()).append("건의 결과 중 ")
              .append(limit).append("건만 표시되었습니다.*\n\n");
        }
        
        return sb.toString();
    }
    
    /**
     * GPT에게 추천 요청
     */
    private String getRecommendationFromGpt(String userMessage, String filteredData) {
        List<ChatMessage> messages = new ArrayList<>();
        
        // 시스템 프롬프트
        messages.add(new ChatMessage("system", """
            당신은 공공임대주택 전문 컨설턴트입니다. 사용자에게 제공된 필터링된 공공임대주택 데이터를 기반으로 최적의 임대주택을 추천해주세요.
            
            데이터는 이미 사용자의 지역 및 예산 조건으로 필터링되어 있습니다.
            
            - 예산, 지역, 면적, 주택 유형, 교통 등 사용자의 요구사항을 종합적으로 분석하세요.
            - 최대 5개의 주택을 추천하고, 각 주택의 장단점을 설명하세요.
            - 한국어로 친절하고 전문적인 톤으로 답변하세요.
            - 임대종류, 주택유형, 면적 등 주요 특성을 중심으로 비교 분석하세요.
            - 서로 다양한 특성을 가진 주택을 추천하여 선택의 폭을 넓혀주세요.
            
            다음 형식으로 답변해주세요:
            
            ---
            ### 사용자 요구사항 분석
            - 사용자의 요구사항 요약 (지역, 예산 등)
            
            ### 추천 임대주택
            1. [단지명] (임대종류)
               - 위치: 000
               - 보증금/월세: 000원/00원
               - 면적: 00㎡
               - 특징: 000
               - 장점: 000
            
            2. [단지명] (임대종류)
               - 위치: 000
               ...
            
            ### 종합 조언
            - 해당 지역 임대시장 특징과 조언
            ---
            
            만약 추천할 만한 주택이 없거나 데이터가 충분하지 않다면, 사용자에게 다른 지역이나 예산 조건을 제안해주세요.
            """
        ));
        
        // 컨텍스트 메시지 (필터링된 주택 데이터)
        messages.add(new ChatMessage("assistant", filteredData));
        
        // 사용자 메시지
        messages.add(new ChatMessage("user", userMessage));
        
        // ChatCompletionRequest 생성
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model("gpt-4")
                .messages(messages)
                .temperature(0.7)
                .maxTokens(1500)
                .build();
        
        // API 호출 (지수 백오프 재시도 적용)
        long backoffMs = INITIAL_BACKOFF_MS;
        
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                log.info("GPT API 호출 시도 #{}", attempt + 1);
                return openAiService.createChatCompletion(request)
                        .getChoices().get(0).getMessage().getContent();
            } catch (Exception e) {
                log.error("GPT API 호출 오류 (시도 #{}/{}): {}", attempt + 1, MAX_RETRIES, e.getMessage());
                
                // HTTP 429 에러 감지 (Too Many Requests)
                boolean isRateLimit = isRateLimitError(e);
                if (isRateLimit) {
                    log.warn("속도 제한 감지됨 (HTTP 429). {}ms 후 재시도...", backoffMs);
                }
                
                if (attempt < MAX_RETRIES - 1) {
                    // 재시도 대기
                    try {
                        Thread.sleep(backoffMs);
                        // 다음 재시도의 대기 시간을 2배로 증가 (지수 백오프)
                        backoffMs *= 2;
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    // 최대 재시도 후에도 실패
                    return "죄송합니다. 서버 연결에 일시적인 문제가 있습니다. 잠시 후 다시 시도해주세요.";
                }
            }
        }
        
        return "죄송합니다. 서버 연결에 일시적인 문제가 있습니다. 잠시 후 다시 시도해주세요.";
    }
    
    /**
     * 사용자 메시지에서 지역 정보 추출
     */
    private String extractRegionFromMessage(String message) {
        // 일반적인 지역명 배열
        String[] regions = {
            "서울", "경기", "인천", "부산", "대구", "광주", "대전", "울산", "세종", 
            "강원", "충북", "충남", "전북", "전남", "경북", "경남", "제주",
            "성남", "용인", "수원", "안양", "분당", "일산", "광명", "과천", "하남"
        };
        
        // 먼저 '~시', '~구', '~동' 패턴 찾기
        Pattern cityPattern = Pattern.compile("([가-힣]+[시구군동])");
        Matcher cityMatcher = cityPattern.matcher(message);
        
        while (cityMatcher.find()) {
            return cityMatcher.group(1);
        }
        
        // 일반 지역명 찾기
        for (String region : regions) {
            if (message.contains(region)) {
                return region;
            }
        }
        
        return null;
    }
    
    /**
     * 사용자 메시지에서 예산 정보 추출
     */
    private Map<String, Object> extractBudgetFromMessage(String message) {
        Map<String, Object> budget = new HashMap<>();
        
        // 보증금/전세 패턴 ('전세' + 숫자 + '만원'/'억' 또는 숫자 + '만원'/'억' + '전세'/'보증금')
        Pattern depositPattern = Pattern.compile("(전세|보증금)?\\s*(\\d+)\\s*(만원|억)(\\s*이하)?|" +
                                                "(\\d+)\\s*(만원|억)(\\s*이하)?\\s*(전세|보증금)");
        Matcher depositMatcher = depositPattern.matcher(message);
        
        while (depositMatcher.find()) {
            String amountStr = depositMatcher.group(2) != null ? depositMatcher.group(2) : depositMatcher.group(5);
            String unit = depositMatcher.group(3) != null ? depositMatcher.group(3) : depositMatcher.group(6);
            
            if (amountStr != null && unit != null) {
                long amount = Long.parseLong(amountStr);
                if (unit.contains("억")) {
                    amount *= 10000; // 억 단위를 만원 단위로 변환
                }
                budget.put("deposit", amount);
                break;
            }
        }
        
        // 월세 패턴 ('월세' + 숫자 + '만원' 또는 숫자 + '만원' + '월세')
        Pattern rentPattern = Pattern.compile("(월세)?\\s*(\\d+)\\s*(만원)(\\s*이하)?|" +
                                             "(\\d+)\\s*(만원)(\\s*이하)?\\s*(월세)");
        Matcher rentMatcher = rentPattern.matcher(message);
        
        while (rentMatcher.find()) {
            String amountStr = rentMatcher.group(2) != null ? rentMatcher.group(2) : rentMatcher.group(5);
            if (amountStr != null) {
                budget.put("rent", Long.parseLong(amountStr));
                break;
            }
        }
        
        return budget;
    }
    
    /**
     * 오류가 속도 제한(HTTP 429)인지 확인
     */
    private boolean isRateLimitError(Exception e) {
        if (e instanceof HttpException) {
            return ((HttpException) e).code() == 429;
        }
        
        if (e.getCause() instanceof retrofit2.HttpException) {
            return ((retrofit2.HttpException) e.getCause()).code() == 429;
        }
        
        String errorMsg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        return errorMsg.contains("429") || 
               errorMsg.contains("too many requests") || 
               errorMsg.contains("rate limit");
    }
}