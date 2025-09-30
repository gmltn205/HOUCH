async function getRecommendations() {
    const preferences = {
        교육: parseFloat(document.getElementById('edu').value),
        의료: parseFloat(document.getElementById('med').value),
        교통: parseFloat(document.getElementById('trans').value),
        쇼핑: parseFloat(document.getElementById('shop').value),
        편의성: parseFloat(document.getElementById('conv').value),
        안전성: parseFloat(document.getElementById('safety').value),
        쾌적성: parseFloat(document.getElementById('comfort').value),
        maxDeposit: parseInt(document.getElementById('maxDeposit').value),
        maxMonthly: parseInt(document.getElementById('maxMonthly').value)
    };

    try {
        const response = await fetch('/api/recommendations', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(preferences)
        });

        const data = await response.json();
        displayResults(data);
    } catch (error) {
        console.error('Error:', error);
    }
}

function displayResults(data) {
    const resultsDiv = document.getElementById('results');
    let html = '<h2>추천 매물</h2>';

    // 일반 추천 매물 표시
    data.recommended.forEach((property, index) => {
        html += createPropertyCard(property, index + 1);
    });

    // 프리미엄 매물이 있는 경우 표시
    if (data.premium && data.premium.length > 0) {
        html += '<h2>프리미엄 추천 매물</h2>';
        data.premium.forEach((property, index) => {
            html += createPropertyCard(property, index + 1, true);
        });
    }

    resultsDiv.innerHTML = html;
}

function createPropertyCard(property, index, isPremium = false) {
    return `
        <div class="property-card ${isPremium ? 'premium' : ''}">
            <h3>${index}순위: ${property.단지명}</h3>
            <p>주소: ${property.address}</p>
            <p>종합 점수: ${property.score.toFixed(2)}</p>
            <p>공급면적: ${property.공급면적}㎡</p>
            <p>임대보증금: ${property.임대보증금.toLocaleString()}원</p>
            <p>월임대료: ${property.월임대료.toLocaleString()}원</p>
        </div>
    `;
}