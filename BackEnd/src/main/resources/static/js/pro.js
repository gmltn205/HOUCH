import React, { useState } from 'react';
import axios from 'axios';

function PropertyRecommendation() {
    const [preferences, setPreferences] = useState({
        교육: 0.2,
        의료: 0.1,
        교통: 0.15,
        쇼핑: 0.1,
        편의성: 0.15,
        안전성: 0.2,
        쾌적성: 0.1
    });
    const [maxDeposit, setMaxDeposit] = useState(50000000);
    const [maxMonthly, setMaxMonthly] = useState(500000);
    const [recommendations, setRecommendations] = useState(null);

    const handleSubmit = async (e) => {
        e.preventDefault();
        try {
            const response = await axios.post('/api/properties/recommend', {
                preferences,
                maxDeposit,
                maxMonthly
            });

            console.log("📌 서버 응답 데이터:", response.data); // 추가된 로그
            setRecommendations(response.data);
        } catch (error) {
            console.error("❌ 오류 발생:", error);
        }
    };


    return (
        <div>
            <h1>매물 추천 시스템</h1>
            <form onSubmit={handleSubmit}>
                {/* 선호도 및 예산 입력 폼 */}
                <button type="submit">추천 받기</button>
            </form>

            {recommendations && (
                <div>
                    <h2>추천 매물</h2>
                    {recommendations.map((property, index) => (
                        <div key={index}>
                            <h3>{property.propertyName}</h3>
                            <p>주소: {property.address}</p>
                            <p>점수: {property.score}</p>
                            {/* 나머지 상세 정보 표시 */}
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}

export default PropertyRecommendation;