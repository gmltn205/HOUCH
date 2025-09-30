// policy-slider 관련 코드만 업데이트
// for heesooo
document.addEventListener('DOMContentLoaded', function() {
    checkLoginStatus();
    const policySlider = {
        currentSlide: 0,
        slides: [],
        autoPlayInterval: null,

        async init() {
            this.slidesContainer = document.querySelector('.policy-slides');
            await this.loadPolicyNews();
            this.bindEvents();
        },

        async loadPolicyNews() {
            try {
                this.slidesContainer.innerHTML = `
                    <div class="policy-loading">정책 뉴스를 불러오는 중...</div>
                `;

                const response = await fetch('/api/policy-news');
                if (!response.ok) throw new Error('Failed to fetch');

                const data = await response.json();
                this.slides = data;

                if (this.slides.length === 0) {
                    this.slidesContainer.innerHTML = `
                        <div class="policy-error">표시할 정책 뉴스가 없습니다.</div>
                    `;
                    return;
                }

                this.renderSlides();
                this.setupAutoPlay();
                this.updateActiveSlide();
            } catch (error) {
                console.error('Error:', error);
                this.slidesContainer.innerHTML = `
                    <div class="policy-error">정책 뉴스를 불러오지 못했습니다.</div>
                `;
            }
        },

        renderSlides() {
            this.slidesContainer.innerHTML = this.slides.map((slide, index) => `
                <div class="policy-slide ${index === this.currentSlide ? 'active' : ''}">
                    <div class="policy-content">
                        <div class="policy-header">
                            <span class="policy-category">${this.getCategory(slide.title)}</span>
                            <h2 class="policy-title">${slide.title}</h2>
                        </div>
                        
                        <div class="policy-horizontal">
                            <div class="policy-thumbnail">
                                ${slide.thumbnailUrl
                ? `<img src="${slide.thumbnailUrl}" alt="${slide.title}">`
                : '<div class="no-image">이미지 없음</div>'
            }
                            </div>
                            
                            <div class="policy-details">
                                <p class="policy-summary">${slide.subTitle1}</p>
                                <div class="policy-meta">
                                    <div class="policy-date">
                                        <i class="fa-regular fa-calendar"></i>
                                        ${slide.date ? slide.date : '날짜 없음'}
                                    </div>
                                    <a href="${slide.originalUrl}" target="_blank" class="policy-link">
                                        자세히 보기
                                        <i class="fa-solid fa-arrow-right"></i>
                                    </a>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            `).join('');

            this.updateSlidePosition();
        },

        getCategory(title) {
            const categories = {
                '주거': '주거지원',
                '청년': '청년지원',
                '신혼': '신혼부부',
                '전세': '전세지원',
                '임대': '임대주택'
            };

            for (const [keyword, category] of Object.entries(categories)) {
                if (title.includes(keyword)) return category;
            }
            return '정책뉴스';
        },

        updateSlidePosition() {
            if (!this.slidesContainer) return;
            const offset = this.currentSlide * 100;
            this.slidesContainer.style.transform = `translateX(-${offset}%)`;
            this.updateActiveSlide();
        },

        updateActiveSlide() {
            document.querySelectorAll('.policy-slide').forEach((slide, index) => {
                if (index === this.currentSlide) {
                    slide.classList.add('active');
                } else {
                    slide.classList.remove('active');
                }
            });
        },

        setupAutoPlay() {
            if (this.slides.length <= 1) return;

            if (this.autoPlayInterval) {
                clearInterval(this.autoPlayInterval);
            }

            this.autoPlayInterval = setInterval(() => {
                this.nextSlide();
            }, 5000);
        },

        nextSlide() {
            this.currentSlide = (this.currentSlide + 1) % this.slides.length;
            this.updateSlidePosition();
        },

        prevSlide() {
            this.currentSlide = (this.currentSlide - 1 + this.slides.length) % this.slides.length;
            this.updateSlidePosition();
        },

        bindEvents() {
            const nextBtn = document.querySelector('.nav-button.next');
            const prevBtn = document.querySelector('.nav-button.prev');

            if (nextBtn) {
                nextBtn.addEventListener('click', () => this.nextSlide());
            }

            if (prevBtn) {
                prevBtn.addEventListener('click', () => this.prevSlide());
            }

            const slider = document.querySelector('.policy-slider');
            if (slider) {
                slider.addEventListener('mouseenter', () => {
                    if (this.autoPlayInterval) {
                        clearInterval(this.autoPlayInterval);
                    }
                });

                slider.addEventListener('mouseleave', () => {
                    this.setupAutoPlay();
                });
            }
        }
    };

    policySlider.init();

});
// 로그인 상태 체크 함수
async function checkLoginStatus() {
    try {
        const response = await fetch('/api/auth/status', {
            method: 'GET',
            credentials: 'include'
        });

        const data = await response.json();
        console.log("Login status:", data);  // 디버깅용

        const loggedOutMenu = document.getElementById('loggedOutMenu');
        const loggedInMenu = document.getElementById('loggedInMenu');

        if (data.isLoggedIn) {
            loggedOutMenu.style.display = 'none';
            loggedInMenu.style.display = 'block';
        } else {
            loggedOutMenu.style.display = 'block';
            loggedInMenu.style.display = 'none';
        }
    } catch (error) {
        console.error('Error checking login status:', error);
    }
}
// 로그아웃 함수
async function logout() {
    try {
        const response = await fetch('/api/auth/logout', {
            method: 'POST',
            credentials: 'include'
        });

        if (response.ok) {
            location.reload();  // 페이지 새로고침
        }
    } catch (error) {
        console.error('Error during logout:', error);
    }
}
