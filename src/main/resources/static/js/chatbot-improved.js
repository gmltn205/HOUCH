document.addEventListener('DOMContentLoaded', function() {
    checkLoginStatus();
    const chatArea = document.getElementById('chatArea');
    const userInput = document.getElementById('userInput');
    const sendButton = document.getElementById('sendButton');
    const apiSelector = document.getElementById('apiSelector');

    // marked 설정
    marked.setOptions({
        breaks: true, // 줄바꿈 활성화
        gfm: true,    // GitHub Flavored Markdown 활성화
    });

    // 로딩 상태를 추적하는 변수
    let isLoading = false;
    let loadingMessageId = null;
    let retryCount = 0;
    const MAX_RETRIES = 3;
    const RETRY_DELAY_MS = 2000; // 2초

    // 로딩 메시지 추가
    function addLoadingMessage() {
        const messageDiv = document.createElement('div');
        messageDiv.className = 'message bot-message';
        messageDiv.id = 'loadingMessage';

        const contentDiv = document.createElement('div');
        contentDiv.className = 'message-content loading-message';

        contentDiv.innerHTML = `
            HOUCH! 봇이 답변을 생성 중입니다
            <div class="loading-dots">
                <div class="dot"></div>
                <div class="dot"></div>
                <div class="dot"></div>
            </div>
        `;

        messageDiv.appendChild(contentDiv);
        chatArea.appendChild(messageDiv);

        // 스크롤 최하단으로
        chatArea.scrollTop = chatArea.scrollHeight;

        return messageDiv.id;
    }

    // 로딩 메시지를 재시도 메시지로 업데이트
    function updateToRetryMessage(id, attempt) {
        const loadingMessage = document.getElementById(id);
        if (loadingMessage) {
            const contentDiv = loadingMessage.querySelector('.message-content');
            contentDiv.innerHTML = `
                서버 응답을 기다리는 중입니다... 재시도 ${attempt}/${MAX_RETRIES}
                <div class="loading-dots">
                    <div class="dot"></div>
                    <div class="dot"></div>
                    <div class="dot"></div>
                </div>
            `;
        }
    }

    // 로딩 메시지 제거
    function removeLoadingMessage(id) {
        const loadingMessage = document.getElementById(id);
        if (loadingMessage) {
            loadingMessage.remove();
        }
    }

    // 메시지 추가 함수 개선
    function addMessage(message, sender) {
        const messageDiv = document.createElement('div');
        messageDiv.className = `message ${sender}-message`;

        const contentDiv = document.createElement('div');
        contentDiv.className = 'message-content';

        if (sender === 'bot') {
            // 봇 메시지는 마크다운 파싱
            contentDiv.innerHTML = marked.parse(message);
        } else {
            // 사용자 메시지는 일반 텍스트
            contentDiv.textContent = message;
        }

        messageDiv.appendChild(contentDiv);
        chatArea.appendChild(messageDiv);

        // 스크롤을 최하단으로 이동
        chatArea.scrollTop = chatArea.scrollHeight;
    }

    // 챗봇 API 호출 함수 (재시도 로직 포함)
    async function fetchBotResponse(message, attempt = 0) {
        if (attempt > MAX_RETRIES) {
            // 최대 재시도 횟수 초과
            removeLoadingMessage(loadingMessageId);
            addMessage("서버 연결에 문제가 있습니다. 나중에 다시 시도해주세요.", 'bot');
            isLoading = false;
            userInput.disabled = false;
            sendButton.disabled = false;
            userInput.focus();
            return;
        }

        try {
            if (attempt === 0) {
                // 첫 시도인 경우에만 초기화
                isLoading = true;
                userInput.disabled = true;
                sendButton.disabled = true;
                loadingMessageId = addLoadingMessage();
            } else {
                // 재시도인 경우 메시지 업데이트
                updateToRetryMessage(loadingMessageId, attempt);
            }

            // API 엔드포인트 선택
            let apiEndpoint = '/api/chatbot';
            if (apiSelector) {
                const apiMode = apiSelector.value;
                apiEndpoint = '/api/chatbot/recommend';
                console.log(`선택된 API 모드: ${apiMode}, 엔드포인트: ${apiEndpoint}`);
            }

            const response = await fetch(apiEndpoint, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ message: message })
            });

            if (!response.ok) {
                if (response.status === 429) {
                    // HTTP 429 (Too Many Requests) 처리
                    console.warn(`속도 제한 발생. ${RETRY_DELAY_MS/1000}초 후 재시도...`);

                    // 재시도 지연
                    setTimeout(() => {
                        fetchBotResponse(message, attempt + 1);
                    }, RETRY_DELAY_MS * (attempt + 1)); // 지수 백오프 적용

                    return;
                } else {
                    throw new Error(`API 요청 실패: ${response.status}`);
                }
            }

            const data = await response.json();
            removeLoadingMessage(loadingMessageId);

            // 응답 텍스트에서 이모지와 줄바꿈 보존
            let formattedResponse = data.response
                .replace(/\n/g, '\n') // 줄바꿈 보존
                .replace(/(🚇|💰|🏢|✨|⚠️)/g, '\n$1'); // 이모지 앞에 줄바꿈 추가

            addMessage(formattedResponse, 'bot');

            // 상태 초기화
            isLoading = false;
            userInput.disabled = false;
            sendButton.disabled = false;
            userInput.focus();
            retryCount = 0; // 성공 시 재시도 횟수 초기화

        } catch (error) {
            console.error('Error:', error);

            // 일시적인 오류로 간주하고 재시도
            if (attempt < MAX_RETRIES) {
                console.log(`재시도 ${attempt + 1}/${MAX_RETRIES} - ${RETRY_DELAY_MS}ms 후...`);
                setTimeout(() => {
                    fetchBotResponse(message, attempt + 1);
                }, RETRY_DELAY_MS * (attempt + 1)); // 지수 백오프 적용
            } else {
                // 최대 재시도 초과
                removeLoadingMessage(loadingMessageId);
                addMessage('죄송합니다. 일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.', 'bot');
                isLoading = false;
                userInput.disabled = false;
                sendButton.disabled = false;
                userInput.focus();
            }
        }
    }

    // 이벤트 리스너 등록
    sendButton.addEventListener('click', sendMessage);

    userInput.addEventListener('keypress', function(e) {
        if (e.key === 'Enter') {
            sendMessage();
        }
    });

    // 메시지 전송 함수
    function sendMessage() {
        const message = userInput.value.trim();
        if (message === '' || isLoading) return;

        addMessage(message, 'user');
        userInput.value = '';
        fetchBotResponse(message);
    }

    // 추천 모드 설정에 따라 안내 메시지 표시
    function updateRecommendGuide() {
        const recommendGuide = document.getElementById('recommendGuide');
        if (apiSelector.value === 'recommend') {
            recommendGuide.style.display = 'block';
        } else {
            recommendGuide.style.display = 'none';
        }
    }

    // API 선택기 변경 이벤트
    if (apiSelector) {
        apiSelector.addEventListener('change', updateRecommendGuide);

        // 초기 상태 설정
        updateRecommendGuide();
    }

    // 입력창 자동 포커스
    userInput.focus();
});

async function checkLoginStatus() {
    try {
        const response = await fetch('/api/auth/status', {
            method: 'GET',
            credentials: 'include'
        });

        const data = await response.json();
        const loggedOutMenu = document.getElementById('loggedOutMenu');
        const loggedInMenu = document.getElementById('loggedInMenu');

        if (data.isLoggedIn) {
            loggedOutMenu.style.display = 'none';
            loggedInMenu.style.display = 'flex';
        } else {
            loggedOutMenu.style.display = 'flex';
            loggedInMenu.style.display = 'none';
        }
    } catch (error) {
        console.error('로그인 상태 확인 중 오류 발생:', error);
    }
}

function logout() {
    fetch('/api/auth/logout', {
        method: 'POST',
        credentials: 'include'
    }).then(() => {
        window.location.href = '/';
    }).catch(error => {
        console.error('로그아웃 중 오류 발생:', error);
    });
}

// 추천 예시 사용 함수
function useRecommendExample(exampleNum) {
    const userInput = document.getElementById('userInput');

    if (exampleNum === 1) {
        userInput.value = "전세 5000만원과 월세 50만원 이하로 경기도 성남시에 살 수 있는 공공임대주택을 추천해 주세요.";
    } else if (exampleNum === 2) {
        userInput.value = "서울시 동작구에 1억 이하의 전세로 입주 가능한 40평대 아파트 추천해주세요.";
    }

    userInput.focus();
}