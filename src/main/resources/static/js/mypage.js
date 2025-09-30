document.addEventListener('DOMContentLoaded', function() {
    // 메뉴 아이템 클릭 이벤트
    const menuItems = document.querySelectorAll('.menu-item');
    menuItems.forEach(item => {
        item.addEventListener('click', function() {
            // 현재 활성화된 메뉴 아이템에서 active 클래스 제거
            const activeItem = document.querySelector('.menu-item.active');
            if (activeItem) {
                activeItem.classList.remove('active');
            }
            // 클릭된 메뉴 아이템에 active 클래스 추가
            this.classList.add('active');
        });
    });

    // 프로필 수정 버튼 클릭 이벤트
    const editProfileBtn = document.querySelector('.edit-profile-btn');
    if (editProfileBtn) {
        editProfileBtn.addEventListener('click', function() {
            alert('프로필 수정 기능 준비중입니다.');
        });
    }
});

// 찜하기 기능 스크립트
function toggleFavorite(houseId) {
    fetch(`/api/favorites/house/${houseId}`, {
        method: 'POST',
        credentials: 'include'
    })
        .then(response => {
            if (response.status === 401) {
                throw new Error('로그인이 필요합니다.');
            }
            return response.json();
        })
        .then(isFavorited => {
            // 해당 매물의 찜 버튼 찾기
            const propertyItem = document.querySelector(`[data-house-id="${houseId}"]`);
            if (propertyItem) {
                const favoriteBtn = propertyItem.querySelector('.favorite-btn');
                const icon = favoriteBtn.querySelector('i');

                if (isFavorited) {
                    icon.classList.remove('fa-regular');
                    icon.classList.add('fa-solid');
                    favoriteBtn.classList.add('active');
                } else {
                    icon.classList.remove('fa-solid');
                    icon.classList.add('fa-regular');
                    favoriteBtn.classList.remove('active');
                }
            }
        })
        .catch(error => {
            alert(error.message);
        });
}