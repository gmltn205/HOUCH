document.addEventListener('DOMContentLoaded', function() {
    // 찜하기 기능
    const favoriteIcons = document.querySelectorAll('.favorite-icon');
    favoriteIcons.forEach(icon => {
        icon.addEventListener('click', function() {
            const houseId = this.getAttribute('data-house-id');
            const heartIcon = this.querySelector('i');

            // 찜하기 상태 토글
            fetch(`/api/favorites/${houseId}`, {
                method: 'POST',
                credentials: 'include'
            })
                .then(response => {
                    if (response.status === 401) {
                        alert('로그인이 필요합니다.');
                        throw new Error('Unauthorized');
                    }
                    return response.json();
                })
                .then(isFavorited => {
                    if (isFavorited) {
                        heartIcon.classList.remove('fa-regular');
                        heartIcon.classList.add('fa-solid');
                    } else {
                        heartIcon.classList.remove('fa-solid');
                        heartIcon.classList.add('fa-regular');
                    }
                })
                .catch(error => {
                    console.error('찜하기 오류:', error);
                });
        });
    });

    // 페이지네이션 이벤트 리스너
    const prevPageBtn = document.getElementById('prevPage');
    const nextPageBtn = document.getElementById('nextPage');

    prevPageBtn.addEventListener('click', function() {
        const currentPage = parseInt(document.querySelector('.page-numbers span').textContent);
        if (currentPage > 1) {
            loadPage(currentPage - 1);
        }
    });

    nextPageBtn.addEventListener('click', function() {
        const currentPage = parseInt(document.querySelector('.page-numbers span').textContent);
        loadPage(currentPage + 1);
    });

    function loadPage(page) {
        fetch(`/applyhome/gyeonggi-rentals?page=${page}`)
            .then(response => response.text())
            .then(html => {
                document.querySelector('.main-content').innerHTML = html;
            })
            .catch(error => console.error('페이지 로드 중 오류:', error));
    }
});