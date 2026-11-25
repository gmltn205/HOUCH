document.addEventListener('DOMContentLoaded', function() {
    checkLoginStatus();

    // 탭 전환 기능
    const tabs = document.querySelectorAll('.tab-item');
    tabs.forEach(tab => {
        tab.addEventListener('click', function() {
            // 현재 활성화된 탭에서 active 클래스 제거
            document.querySelector('.tab-item.active').classList.remove('active');
            // 클릭된 탭에 active 클래스 추가
            this.classList.add('active');
        });
    });

    // 글쓰기 버튼 클릭 이벤트
    const writeBtn = document.querySelector('.write-btn');
    writeBtn.addEventListener('click', function() {
        alert('글쓰기 기능 준비중입니다.');
    });

    // 검색 기능
    const searchInput = document.querySelector('.search-input');
    searchInput.addEventListener('keyup', function(e) {
        if (e.key === 'Enter') {
            alert(`검색어: ${this.value}`);
        }
    });

    // 정렬 방식 변경
    const sortSelect = document.querySelector('.sort-select');
    sortSelect.addEventListener('change', function() {
        alert(`정렬 방식이 ${this.value}로 변경되었습니다.`);
    });

    // 페이지네이션 버튼 클릭
    const pageButtons = document.querySelectorAll('.page-btn');
    pageButtons.forEach(button => {
        button.addEventListener('click', function() {
            if (!this.classList.contains('active')) {
                document.querySelector('.page-btn.active')?.classList.remove('active');
                this.classList.add('active');
            }
        });
    });

    // 게시글 클릭 이벤트
    const postItems = document.querySelectorAll('.post-item');
    postItems.forEach(post => {
        post.addEventListener('click', function() {
            alert('게시글 상세 페이지 준비중입니다.');
        });
    });
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