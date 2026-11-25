document.addEventListener('DOMContentLoaded', function() {
    const regions = [
        "가평군", "고양시", "과천시", "광명시", "광주시", "구리시", "군포시",
        "김포시", "남양주시", "동두천시", "부천시", "성남시", "수원시", "시흥시",
        "안산시", "안성시", "안양시", "양주시", "양평군", "여주시", "연천군",
        "오산시", "용인시", "의왕시", "의정부시", "이천시", "파주시", "평택시",
        "포천시", "하남시", "화성시"
    ];

    const regionSelection = document.getElementById('regionSelection');

    // 지역 선택지 생성
    regions.forEach(region => {
        const div = document.createElement('div');
        div.className = 'region-checkbox';
        div.innerHTML = `
            <input type="checkbox" id="${region}" name="regions" value="${region}">
            <label for="${region}">${region}</label>
        `;
        regionSelection.appendChild(div);
    });

    // 지역 최대 3개 선택 제한
    const checkboxes = document.querySelectorAll('input[name="regions"]');
    checkboxes.forEach(checkbox => {
        checkbox.addEventListener('change', function() {
            const checked = document.querySelectorAll('input[name="regions"]:checked');
            if (checked.length > 3) {
                this.checked = false;
                alert('최대 3개까지만 선택 가능합니다.');
            }
        });
    });

    // 폼 제출 처리
    document.getElementById('userProfileForm').addEventListener('submit', async function(e) {
        e.preventDefault();

        const checkedRegions = Array.from(document.querySelectorAll('input[name="regions"]:checked'))
            .map(input => input.value);

        const formData = {
            preferredRegions: checkedRegions,
            gender: this.gender.value,
            ageGroup: this.ageGroup.value,
            annualIncome: parseInt(this.annualIncome.value),
            personalCharacteristic: this.personalCharacteristic.value,
            householdCharacteristic: this.householdCharacteristic.value
        };
        console.log('Sending data:', formData); // 데이터 확인용 로그
        try {
            const response = await fetch('/api/user/profile', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(formData)
            });

            if (response.ok) {
                alert('프로필이 저장되었습니다.');
                window.location.href = '/';
            } else {
                const error = await response.json();
                console.error('Server error:', errorData); // 서버 에러 확인용 로그
                alert(error.message || '저장 중 오류가 발생했습니다.');
            }
        } catch (error) {

            alert('서버 오류가 발생했습니다.');
            console.error('Error:', error);
        }
    });
});