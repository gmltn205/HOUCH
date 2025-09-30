/**
 * file: ChatbotTestService.java
 * location: C:\Users\Admin\Desktop\new_real\src\main\java\org\example\recommendhouse\service
 */

package org.example.recommendhouse.service;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.recommendhouse.entity.HouseInfo;
import org.example.recommendhouse.repository.HouseInfoRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotTestService {

    private final OpenAiService openAiService;          // OpenAI 연동
    private final HouseInfoRepository houseInfoRepository; // PostgreSQL 매물 조회

    /*
     * 사용자 메시지를 입력받아,
     * 1) 지역 키워드 파싱
     * 2) DB에서 해당 지역 매물 조회
     * 3) 조회 결과 + 사용자 메시지를 GPT에 전달하여 최종 답변 생성
     */
    public String testGenerateResponse(String userMessage) {
        // 1) 사용자 메시지에서 지역 키워드 추출
        String city = extractCityFromMessage(userMessage);

        // 2) DB 조회
        List<HouseInfo> houseList = new ArrayList<>();
        if (city != null && !city.isEmpty()) {
            houseList = houseInfoRepository.findByCity(city);
            log.info("DB 조회 결과: {} 건", houseList.size());
        } else {
            log.info("도시명을 찾을 수 없어 DB 조회 건너뜀");
        }

        // 3) 조회 결과 요약
        String dbSummary = summarizeHouseInfo(houseList, city);

        // 4) 메시지 구성
        List<ChatMessage> messages = new ArrayList<>();

        // (A) 시스템 메시지
        messages.add(new ChatMessage("system",
                "당신은 부동산 및 입지분석 전문가입니다. " +
                        "아래 매물 정보(DB에서 조회된 결과)를 참고해, " +
                        "사용자의 질문에 대해 교통, 학군, 생활 편의시설, 안전성 등 입지 정보를 종합하여 답변해주세요."
        ));

        // (B) assistant 메시지 - DB 조회 정보
        messages.add(new ChatMessage("assistant", dbSummary));

        // (C) user 메시지 - 실제 사용자 입력
        messages.add(new ChatMessage("user", userMessage));

        // 5) ChatCompletionRequest 생성
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model("gpt-4")
                .messages(messages)
                .temperature(0.7)
                .maxTokens(10000)
                .build();

        // 6) OpenAI API 호출
        try {
            return openAiService.createChatCompletion(request)
                    .getChoices().get(0).getMessage().getContent();
        } catch (Exception e) {
            log.error("OpenAI API 호출 중 오류 발생", e);
            return "죄송합니다. 일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.";
        }
    }

    /**
     * 간단한 지역 키워드 추출 예시
     * - 실제로는 NLP, 정규식 등을 통해 "성남", "서울", "부산" 등등 여러 도시명을 탐색 가능
     */
    private String extractCityFromMessage(String userMessage) {
        if (userMessage.contains("성남")) {
            return "성남";
        }
        if (userMessage.contains("서울")) {
            return "서울";
        }
        if (userMessage.contains("부산")) {
            return "부산";
        }
        // TODO: 필요한 도시명들 추가
        return null;
    }

    /**
     * 조회된 매물(HouseInfo) 리스트를 간단히 요약
     * GPT가 분석에 참고할 수 있도록 정리
     */
    private String summarizeHouseInfo(List<HouseInfo> houseList, String city) {
        if (houseList.isEmpty()) {
            return (city != null ? city : "해당") + " 지역에 해당하는 매물이 없습니다.\n";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("아래는 '").append(city).append("' 지역의 매물 데이터입니다:\n\n");
        for (HouseInfo house : houseList) {
            sb.append(" - 매물 ID: ").append(house.getId())
                    .append(", 단지명: ").append(house.getComplexName())
                    .append(", 주소: ").append(house.getRoadAddress())
                    .append(", 보증금: ").append(house.getDeposit())
                    .append(", 월세: ").append(house.getMonthlyRent())
                    .append(", 전용면적: ").append(house.getExclusiveArea())
                    .append("\n");
        }
        return sb.toString();
    }
}
