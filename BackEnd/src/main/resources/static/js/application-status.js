document.addEventListener('DOMContentLoaded', function() {
    let currentDate = new Date();
    console.log('받은 데이터:', applicationsData); // 데이터 확인용

    // yyyymmdd 형식의 문자열을 Date 객체로 변환하는 함수
    function parseYYYYMMDD(dateStr) {
        if (!dateStr) return null;
        const [year, month, day] = dateStr.split('-');
        return new Date(year, parseInt(month)-1, day);
    }

    function hasApplicationEvent(date) {
        return applicationsData.some(app => {
            const startDate = parseYYYYMMDD(app.SUBSCRPT_RCEPT_BGNDE);
            const endDate = parseYYYYMMDD(app.SUBSCRPT_RCEPT_ENDDE);
            if (!startDate || !endDate) return false;

            return date >= startDate && date <= endDate;
        });
    }

    function formatDate(dateStr) {
        return dateStr.replace(/(\d{4})(\d{2})(\d{2})/, '$1-$2-$3');
    }

    function updateCalendar() {
        const firstDay = new Date(currentDate.getFullYear(), currentDate.getMonth(), 1);
        const lastDay = new Date(currentDate.getFullYear(), currentDate.getMonth() + 1, 0);

        document.getElementById('currentMonth').textContent =
            `${currentDate.getFullYear()}년 ${currentDate.getMonth() + 1}월`;

        const calendarGrid = document.querySelector('.calendar-grid');

        // 기존 날짜들 삭제 (요일 헤더는 유지)
        const days = calendarGrid.querySelectorAll('.day');
        days.forEach(day => day.remove());

        // 이전 달의 날짜들 추가
        const firstDayOfWeek = firstDay.getDay();
        const prevMonthLastDay = new Date(currentDate.getFullYear(), currentDate.getMonth(), 0).getDate();

        for (let i = 0; i < firstDayOfWeek; i++) {
            const dayElement = createDayElement(prevMonthLastDay - firstDayOfWeek + i + 1);
            dayElement.classList.add('prev-month');
            calendarGrid.appendChild(dayElement);
        }

        // 현재 달의 날짜들 추가
        for (let i = 1; i <= lastDay.getDate(); i++) {
            const currentDateToCheck = new Date(currentDate.getFullYear(), currentDate.getMonth(), i);
            const dayElement = createDayElement(i);

            // 오늘 날짜 확인
            if (isToday(currentDateToCheck)) {
                dayElement.classList.add('today');
            }

            // 청약 일정 체크
            if (hasApplicationEvent(currentDateToCheck)) {
                dayElement.classList.add('has-event');

                // 해당 날짜의 청약 정보 툴팁 추가
                const events = applicationsData.filter(app => {
                    const startDate = parseYYYYMMDD(app.SUBSCRPT_RCEPT_BGNDE);
                    const endDate = parseYYYYMMDD(app.SUBSCRPT_RCEPT_ENDDE);
                    return currentDateToCheck >= startDate && currentDateToCheck <= endDate;
                });

                if (events.length > 0) {
                    const tooltip = document.createElement('div');
                    tooltip.className = 'event-tooltip';
                    tooltip.innerHTML = events.map(event => `
                        <div class="event-item">
                            <strong>${event.HOUSE_NM}</strong><br>
                            청약기간: ${formatDate(event.SUBSCRPT_RCEPT_BGNDE)} 
                            ~ ${formatDate(event.SUBSCRPT_RCEPT_ENDDE)}
                        </div>
                    `).join('');
                    dayElement.appendChild(tooltip);
                }
            }

            calendarGrid.appendChild(dayElement);
        }
    }

    function createDayElement(dayNumber) {
        const dayElement = document.createElement('div');
        dayElement.className = 'day';
        dayElement.innerHTML = `
            <span class="date-number">${dayNumber}</span>
            <div class="event-marker"></div>
        `;
        return dayElement;
    }

    function isToday(date) {
        const today = new Date();
        return date.getDate() === today.getDate() &&
            date.getMonth() === today.getMonth() &&
            date.getFullYear() === today.getFullYear();
    }

    // 초기 달력 표시
    updateCalendar();

    // 이전/다음 달 버튼 이벤트
    document.getElementById('prevMonth').addEventListener('click', () => {
        currentDate.setMonth(currentDate.getMonth() - 1);
        updateCalendar();
    });

    document.getElementById('nextMonth').addEventListener('click', () => {
        currentDate.setMonth(currentDate.getMonth() + 1);
        updateCalendar();
    });
});