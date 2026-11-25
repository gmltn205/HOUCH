/**
 * file: ChatbotService.java
 * location: org.example.recommendhouse.service
 */
package org.example.recommendhouse.service;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ChatbotService {

    private final OpenAiService openAiService;
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 1000; // 1초

    // 시스템 프롬프트를 Markdown 활용 가이드로 수정
    private static final String SYSTEM_PROMPT = """
        당신은 수도권 부동산 및 공공임대주택 전문 컨설턴트입니다.
        
        **답변 시 다음 형식을 활용하여 가독성을 높여 주세요 (마크다운 사용 권장):**

        ---
        ### 1) 사용자 상황 요약
        - 사용자 이름, 근무지, 관심 지역 등을 짧게 요약하고, 공감을 표현해 주세요.

        ### 2) 각 섹션별 설명 (이모지와 함께)
        #### 🚇 교통 분석
        - **대중교통 소요시간**  
        - **자가용 소요시간**  
        - **주요 교통수단 및 환승**  

        #### 💰 주거 비용
        - **전세/월세 시세**  
        - **보증금 수준**  
        - **관리비 수준**  

        #### 🏢 생활 인프라
        - **상업시설** (예: 쇼핑몰, 마트 등)  
        - **교육시설** (예: 학교, 학원 등)  
        - **문화시설** (예: 도서관, 공연장 등)  
        - **공원/녹지** (예: 산책로, 공원 등)

        #### ✨ 장점
        - 지역의 주요 장점들 bullet point로 정리

        #### ⚠️ 고려사항
        - 주의해야 할 점 bullet point로 정리
        
        ### 3) 결론
        - 한두 문장으로 요약/권고안 제시

        **추가 안내:**
        - 마크다운 문법(제목, 굵게, 목록, 줄바꿈 등)을 적극 활용하여 가독성을 높이세요.
        - 답변은 친근하면서도 전문적인 어조를 유지해 주세요.
        - 가능하다면 간단한 이모지(🚇, 💰 등)를 활용하여 섹션을 구분하세요.
        ---
        """;

    public ChatbotService(OpenAiService openAiService) {
        this.openAiService = openAiService;
    }

    public String generateResponse(String userMessage) {
        List<ChatMessage> messages = new ArrayList<>();

        // 시스템 메시지
        messages.add(new ChatMessage("system", SYSTEM_PROMPT));

        // 사용자 메시지
        messages.add(new ChatMessage("user", userMessage));

        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model("gpt-4")
                .messages(messages)
                .temperature(0.7)
                .maxTokens(10000)
                .build();

        // 지수 백오프를 사용한 재시도 로직
        long backoffMs = INITIAL_BACKOFF_MS;
        Exception lastException = null;

        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                log.info("API 호출 시도 #{}", attempt + 1);
                return openAiService.createChatCompletion(request)
                        .getChoices().get(0).getMessage().getContent();
            } catch (Exception e) {
                lastException = e;
                log.error("API 호출 오류 (시도 #{}/{}): {}", attempt + 1, MAX_RETRIES, e.getMessage());

                // HTTP 429 에러 감지 (Too Many Requests)
                boolean isRateLimit = isRateLimitError(e);
                if (isRateLimit) {
                    log.warn("속도 제한 감지됨 (HTTP 429). {}ms 후 재시도...", backoffMs);
                }

                // 재시도 대기
                try {
                    Thread.sleep(backoffMs);
                    // 다음 재시도의 대기 시간을 2배로 증가 (지수 백오프)
                    backoffMs *= 2;
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        // 모든 재시도 실패 시 사용자 친화적인 오류 메시지 반환
        String region = extractRegionFromMessage(userMessage);
        if (region != null && !region.isEmpty()) {
            return String.format("""
                ### 죄송합니다. 일시적인 서비스 과부하가 발생했습니다.
                
                %s 지역에 관심이 있으신 것으로 보입니다. 현재 시스템이 많은 요청을 처리하고 있어 잠시 후 다시 시도해주시기 바랍니다.
                
                #### 💡 도움이 될 만한 다른 방법
                
                - **직접 검색**: 국토교통부 마이홈포털(https://www.myhome.go.kr/)에서 %s 지역의 공공임대주택 정보를 확인해보세요.
                - **LH 공식 홈페이지**: 한국토지주택공사(LH) 홈페이지에서도 다양한 정보를 제공합니다.
                - **잠시 후 재시도**: 약 1~2분 후에 다시 질문해주시면 자세한 답변을 드리겠습니다.
                
                오류 정보: %s
                """, region, region, lastException != null ? lastException.getMessage() : "알 수 없는 오류");
        } else {
            return "죄송합니다. 일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.\n" +
                    "Error: " + (lastException != null ? lastException.getMessage() : "Unknown error");
        }
    }

    /**
     * 오류가 속도 제한(HTTP 429)인지 확인
     */
    private boolean isRateLimitError(Exception e) {
        if (e instanceof retrofit2.HttpException) {
            return ((retrofit2.HttpException) e).code() == 429;
        }

        // 예외의 원인도 확인
        if (e.getCause() instanceof retrofit2.HttpException) {
            return ((retrofit2.HttpException) e.getCause()).code() == 429;
        }

        // 메시지로 확인 (더 일반적인 방법)
        String errorMsg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        return errorMsg.contains("429") ||
                errorMsg.contains("too many requests") ||
                errorMsg.contains("rate limit");
    }

    /**
     * 사용자 메시지에서 지역 정보 추출
     */
    private String extractRegionFromMessage(String message) {
        // 지역명 배열
        String[] regions = {"서울", "경기", "인천", "부산", "대구", "광주", "대전", "울산", "세종",
                "강원", "충북", "충남", "전북", "전남", "경북", "경남", "제주",
                "용인", "성남", "수원", "안양", "분당", "일산", "광명", "과천", "하남"};

        for (String region : regions) {
            if (message.contains(region)) {
                return region;
            }
        }

        return null;
    }
}