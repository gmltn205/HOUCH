let currentPage = 1;
const pageSize = 10;
let totalItems = 0;
const MAX_PAGE_BUTTONS = 5;

// 메인 로딩 함수
async function loadHouses() {
    try {
        const keyword = document.getElementById('searchKeyword')?.value || '';
        const city = document.getElementById('cityFilter')?.value || '';
        const houseType = document.getElementById('houseTypeFilter')?.value || '';
        const minDeposit = document.getElementById('minDeposit')?.value || '';
        const maxDeposit = document.getElementById('maxDeposit')?.value || '';
        const minMonthly = document.getElementById('minMonthly')?.value || '';
        const maxMonthly = document.getElementById('maxMonthly')?.value || '';

        // queryParams 구성
        const params = new URLSearchParams({
            page: currentPage - 1,
            size: pageSize,
            ...(keyword && { keyword }),
            ...(city && { city }),
            ...(houseType && { houseType }),
            ...(minDeposit && { minDeposit }),
            ...(maxDeposit && { maxDeposit }),
            ...(minMonthly && { minMonthly }),
            ...(maxMonthly && { maxMonthly })
        });

        // list 엔드포인트 사용
        const response = await fetch(`/api/houses/list?${params}`, {
            method: 'GET',
            credentials: 'include',
            headers: {
                'Content-Type': 'application/json'
            }
        });

        if (!response.ok) {
            throw new Error('Network response was not ok: ' + response.statusText);
        }

        const data = await response.json();
        console.log('Received data:', data);
        console.log('Content:', data.content);
        console.log('First house:', data.content[0]);
        console.log('First house ID:', data.content[0].id);

        displayHouses(data.content);
        updatePagination(data.totalPages);
        totalItems = data.totalElements;
    } catch (error) {
        console.error('Error loading houses:', error);
        document.getElementById('houseList').innerHTML = '<tr><td colspan="7">데이터를 불러오는데 실패했습니다.</td></tr>';
    }
}

// 매물 표시 함수
function displayHouses(houses) {
    const tbody = document.getElementById('houseList');
    tbody.innerHTML = '';

    if (!houses || houses.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7">표시할 매물이 없습니다.</td></tr>';
        return;
    }

    houses.forEach(house => {
        const tr = document.createElement('tr');
        tr.style.cursor = 'pointer';
        tr.onclick = () => location.href = `/houses/${house.id}`;
        tr.innerHTML = `
            <td>${house.complexName || '-'}</td>
            <td>${house.roadAddress || '-'}</td>
            <td>${house.city || '-'}</td>
            <td>${house.houseType || '-'}</td>
            <td>${formatPrice(house.deposit)}원</td>
            <td>${formatPrice(house.monthlyRent)}원</td>
            <td><i class="fas fa-eye"></i>${house.viewCount || 0}</td>
        `;
        tbody.appendChild(tr);
    });
}

// 페이지네이션 업데이트
function updatePagination(totalPages) {
    const pagination = document.querySelector('.pagination');
    pagination.innerHTML = '';

    // 시작 페이지와 끝 페이지 계산
    let startPage = Math.max(1, currentPage - Math.floor(MAX_PAGE_BUTTONS / 2));
    let endPage = Math.min(totalPages, startPage + MAX_PAGE_BUTTONS - 1);

    if (endPage - startPage + 1 < MAX_PAGE_BUTTONS) {
        startPage = Math.max(1, endPage - MAX_PAGE_BUTTONS + 1);
    }

    // 이전 버튼
    const prevButton = createPageButton('◁', () => {
        if (currentPage > 1) {
            currentPage--;
            loadHouses();
        }
    });
    pagination.appendChild(prevButton);

    // 페이지 번호
    for (let i = startPage; i <= endPage; i++) {
        const pageButton = createPageButton(i, () => {
            currentPage = i;
            loadHouses();
        }, currentPage === i);
        pagination.appendChild(pageButton);
    }

    // 다음 버튼
    const nextButton = createPageButton('▷', () => {
        if (currentPage < totalPages) {
            currentPage++;
            loadHouses();
        }
    });
    pagination.appendChild(nextButton);
}

// 페이지 버튼 생성 함수
function createPageButton(text, onClick, isActive = false) {
    const button = document.createElement('button');
    button.textContent = text;
    button.className = `page-btn ${isActive ? 'active' : ''}`;
    button.onclick = onClick;
    return button;
}

// 가격 포맷팅
function formatPrice(price) {
    return price.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',');
}

// 이벤트 리스너 설정
document.addEventListener('DOMContentLoaded', function() {
    // 데이터 로드 전에 로딩 표시
    document.getElementById('houseList').innerHTML = '<tr><td colspan="7">데이터를 불러오는 중...</td></tr>';

    // 초기 데이터 로드
    loadHouses();
    checkLoginStatus();

    // 검색 버튼 이벤트
    const searchBtn = document.getElementById('searchBtn');
    if (searchBtn) {
        searchBtn.addEventListener('click', () => {
            currentPage = 1;
            loadHouses();
        });
    }

    // 필터 변경 이벤트
    ['cityFilter', 'houseTypeFilter', 'minDeposit', 'maxDeposit', 'minMonthly', 'maxMonthly'].forEach(id => {
        const element = document.getElementById(id);
        if (element) {
            element.addEventListener('change', () => {
                currentPage = 1;
                loadHouses();
            });
        }
    });
});

// 로그인 상태 확인
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

// 로그아웃 처리
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