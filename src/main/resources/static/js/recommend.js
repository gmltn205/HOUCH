document.addEventListener("DOMContentLoaded", () => {
    checkLoginStatus();
    // 모든 슬라이더 값이 변경될 때 UI 업데이트
    document.querySelectorAll("input[type=range]").forEach(slider => {
        slider.addEventListener("input", function () {
            document.getElementById(this.id + "Val").innerText = this.value;
        });
    });
});

function getRecommendations() {
    // 로딩 상태 표시 (선택사항)
    const resultsContainer = document.getElementById('results-container');
    const resultsDiv = document.getElementById('results');
    resultsDiv.innerHTML = '<div class="loading">추천 매물을 검색중입니다...</div>';
    resultsContainer.style.display = 'block';

    const requestData = {
        preferences: {
            "교육": parseFloat(document.getElementById("edu").value),
            "의료": parseFloat(document.getElementById("med").value),
            "교통": parseFloat(document.getElementById("trans").value),
            "쇼핑": parseFloat(document.getElementById("shop").value),
            "편의성": parseFloat(document.getElementById("conv").value),
            "안전성": parseFloat(document.getElementById("safety").value),
            "쾌적성": parseFloat(document.getElementById("comfort").value)
        },
        maxDeposit: parseInt(document.getElementById("maxDeposit").value),
        maxMonthly: parseInt(document.getElementById("maxMonthly").value)
    };

    // Spring Boot 서버로 요청
    fetch("http://localhost:8080/api/recommendations", {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify(requestData)

    })
        .then(response => {
            console.log("응답 상태:", response.status);
            console.log("응답 헤더:", response.headers);

            if (!response.ok) {
                return response.text().then(errorText => {
                    console.log("오류 응답 본문:", errorText);
                    throw new Error(`HTTP ${response.status}: ${errorText}`);
                });
            }
            return response.json();
        })
        .then(data => {
            console.log("서버 응답 데이터:", data);

            displayResults(data.properties);
            // 결과가 있을 때 컨테이너를 보이게 함
            resultsContainer.style.display = 'block';
        })
        .catch(error => {
            console.error("API 호출 실패:", error);
            document.getElementById("results").innerHTML =
                `<p style="color: red;">추천 시스템 오류가 발생했습니다: ${error.message}</p>`;
        });
}
function displayResults(properties) {
    const resultsDiv = document.getElementById("results");
    resultsDiv.innerHTML = "";

    if (!properties || properties.length === 0) {
        resultsDiv.innerHTML = "<p class='text-center text-red-500'>❌ 추천 결과가 없습니다.</p>";
        return;
    }

    const resultsGrid = document.createElement("div");
    resultsGrid.className = "results-grid";

    properties.forEach((property, index) => {
        const propertyCard = document.createElement("div");
        propertyCard.className = `property-card ${property.isPremium ? 'premium' : ''}`;

        // 프리미엄 배지 추가 (조건부)
        const premiumBadge = property.isPremium ?
            `<span class="premium-badge">프리미엄 매물</span>` : '';

        // 카드 헤더 (단지명 + 프리미엄 배지)
        const headerHtml = `
            <div class="property-header">
                <h3>${property.details.propertyName}</h3>
                ${premiumBadge}
            </div>
        `;

        // 매물 세부 정보
        const detailsHtml = `
            <div class="property-body">
                <p class="property-type">${property.details.propertyType}</p>
                <p>📍 ${property.address}</p>
                <p>🏘️ 세대수: ${property.details.unitCount}세대</p>
                <p>📏 면적: ${property.details.area}㎡</p>
                <p>💰 보증금: ${property.details.deposit.toLocaleString()}원</p>
                <p>💵 월세: ${property.details.monthlyRent.toLocaleString()}원</p>
                <p class="property-score">종합 점수: ${(property.score * 100).toFixed(1)}점</p>
            </div>
        `;

        // 차트를 위한 컨테이너
        const chartsHtml = `
            <div class="charts-container">
                <div class="chart-wrapper">
                    <canvas id="donutChart-${index}"></canvas>
                </div>
                <div class="chart-wrapper">
                    <canvas id="radarChart-${index}"></canvas>
                </div>
            </div>
        `;

        // 전체 카드 내용 조합
        propertyCard.innerHTML = headerHtml + detailsHtml + chartsHtml;
        resultsGrid.appendChild(propertyCard);

        // 차트 생성 (DOM에 추가된 후)
        setTimeout(() => {
            createDonutChart(`donutChart-${index}`, property.score);
            createRadarChart(`radarChart-${index}`, property.detailScores);
        }, 0);
    });

    resultsDiv.appendChild(resultsGrid);
}

function createDonutChart(canvasId, score) {
    const ctx = document.getElementById(canvasId).getContext('2d');
    new Chart(ctx, {
        type: 'doughnut',
        data: {
            datasets: [{
                data: [score * 100, 100 - (score * 100)],
                backgroundColor: [
                    'rgba(54, 162, 235, 0.8)',
                    'rgba(211, 211, 211, 0.3)'
                ],
                borderWidth: 0
            }]
        },
        options: {
            cutout: '80%',
            plugins: {
                legend: { display: false },
                tooltip: { enabled: false }
            },
            responsive: true,
            maintainAspectRatio: false
        }
    });
}

function createRadarChart(canvasId, detailScores) {
    const ctx = document.getElementById(canvasId).getContext('2d');
    new Chart(ctx, {
        type: 'radar',
        data: {
            labels: Object.keys(detailScores),
            datasets: [{
                label: '항목별 점수',
                data: Object.values(detailScores),
                fill: true,
                backgroundColor: 'rgba(54, 162, 235, 0.2)',
                borderColor: 'rgba(54, 162, 235, 0.8)',
                pointBackgroundColor: 'rgba(54, 162, 235, 1)',
                pointBorderColor: '#fff',
                pointHoverBackgroundColor: '#fff',
                pointHoverBorderColor: 'rgba(54, 162, 235, 1)'
            }]
        },
        options: {
            scales: {
                r: {
                    min: 0,
                    max: 1,
                    ticks: { stepSize: 0.2 }
                }
            },
            plugins: { legend: { display: false } },
            responsive: true,
            maintainAspectRatio: false
        }
    });
}

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