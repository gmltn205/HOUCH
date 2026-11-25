let map;
let markers = [];
let geocoder;

// 모든 페이지의 script 부분에 추가
document.addEventListener('DOMContentLoaded', function() {
    checkLoginStatus();
});
// 지도 초기화
window.onload = function() {
    initMap();
    initSearchEvent();
    initFilterEvent();
};

function initMap() {
    var container = document.getElementById('map');
    var options = {
        center: new kakao.maps.LatLng(37.5666805, 126.9784147),
        level: 7  // 초기 줌 레벨을 조금 더 넓게 설정
    };

    map = new kakao.maps.Map(container, options);
    geocoder = new kakao.maps.services.Geocoder();

    // 지도 컨트롤 추가
    var zoomControl = new kakao.maps.ZoomControl();
    map.addControl(zoomControl, kakao.maps.ControlPosition.RIGHT);

    var mapTypeControl = new kakao.maps.MapTypeControl();
    map.addControl(mapTypeControl, kakao.maps.ControlPosition.TOPRIGHT);

    // 초기 매물 로드
    loadInitialProperties();
}

// 초기 매물 로드 함수
async function loadInitialProperties() {
    try {
        const center = map.getCenter();
        const lat = center.getLat();
        const lng = center.getLng();

        const response = await fetch(`/api/properties/nearby?lat=${lat}&lon=${lng}`);
        if (!response.ok) {
            throw new Error('API 요청 실패');
        }
        const properties = await response.json();
        displayProperties(properties);
    } catch (error) {
        console.error('초기 매물 로드 오류:', error);
    }
}


// 검색 이벤트 초기화
function initSearchEvent() {
    const searchInput = document.getElementById('addressSearch');
    const searchBtn = document.getElementById('searchBtn');

    searchBtn.addEventListener('click', () => {
        searchAddress(searchInput.value);
    });

    searchInput.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') {
            searchAddress(searchInput.value);
        }
    });
}

// 필터 이벤트 초기화
function initFilterEvent() {
    const depositFilter = document.getElementById('depositFilter');
    const monthlyFilter = document.getElementById('monthlyFilter');

    depositFilter.addEventListener('change', applyFilters);
    monthlyFilter.addEventListener('change', applyFilters);
}

// 주소 검색
function searchAddress(address) {
    geocoder.addressSearch(address, function(result, status) {
        if (status === kakao.maps.services.Status.OK) {
            // 기존 마커 제거
            removeAllMarkers();

            const coords = new kakao.maps.LatLng(result[0].y, result[0].x);

            // 마커 생성
            const marker = new kakao.maps.Marker({
                map: map,
                position: coords
            });

            markers.push(marker);

            // 지도 중심 이동
            map.setCenter(coords);

            // 검색된 주소 주변의 매물 검색
            searchNearbyProperties(coords);
        }
    });
}

// 모든 마커 제거
function removeAllMarkers() {
    markers.forEach(marker => marker.setMap(null));
    markers = [];
}

// 필터 적용
function applyFilters() {
    const depositFilter = document.getElementById('depositFilter').value;
    const monthlyFilter = document.getElementById('monthlyFilter').value;

    // 필터 조건에 맞는 매물만 표시
    // 실제 구현 시에는 서버에 필터 조건을 전송하여 데이터를 받아옴
    filterProperties(depositFilter, monthlyFilter);
}

// 주변 매물 검색
async function searchNearbyProperties(coords) {
    const lat = coords.getLat();
    const lng = coords.getLng();
    try {
        // URL 파라미터 이름을 컨트롤러와 일치시킴
        const response = await fetch(`/api/properties/nearby?lat=${lat}&lon=${lng}`);
        if (!response.ok) {
            throw new Error('API 요청 실패');
        }
        const properties = await response.json();
        console.log('받아온 매물 데이터:', properties); // 디버깅용
        displayProperties(properties);
    } catch (error) {
        console.error('주변 매물 조회 오류:', error);
    }
}

// 매물 표시
function displayProperties(properties) {
    const propertyList = document.getElementById('propertyList');
    propertyList.innerHTML = '';

    if (!properties || properties.length === 0) {
        propertyList.innerHTML = '<p>주변에 매물이 없습니다.</p>';
        return;
    }

    properties.forEach(property => {
        // 위도/경도 확인
        console.log('매물 좌표:', property.latitude, property.longitude);

        // 마커 생성
        const marker = new kakao.maps.Marker({
            map: map,
            position: new kakao.maps.LatLng(property.latitude, property.longitude)
        });

        markers.push(marker);

        // 인포윈도우 생성
        const infowindow = createInfoWindow(property);
        kakao.maps.event.addListener(marker, 'click', function() {
            infowindow.open(map, marker);
        });

        // 목록에도 표시
        propertyList.appendChild(createPropertyListItem(property));
    });
}

// 인포윈도우 생성 함수 수정
function createInfoWindow(property) {
    const content = `
        <div class="infowindow" style="padding:15px;width:300px;background:white;border-radius:8px;">
            <h3 style="margin:0 0 10px;font-size:16px;font-weight:bold;color:#2575fc;">
                ${property.complexName}
            </h3>
            <div style="margin:0;font-size:13px;line-height:1.6;">
                <p style="margin:5px 0;"><i class="fas fa-map-marker-alt" style="color:#2575fc;margin-right:5px;"></i> ${property.roadAddress}</p>
                <p style="margin:5px 0;"><i class="fas fa-home" style="color:#2575fc;margin-right:5px;"></i> ${property.houseType}</p>
                <p style="margin:5px 0;"><i class="fas fa-money-bill-wave" style="color:#2575fc;margin-right:5px;"></i> 보증금: ${property.deposit.toLocaleString()}원</p>
                <p style="margin:5px 0;"><i class="fas fa-coins" style="color:#2575fc;margin-right:5px;"></i> 월세: ${property.monthlyRent.toLocaleString()}원</p>
                <p style="margin:5px 0;"><i class="fas fa-expand" style="color:#2575fc;margin-right:5px;"></i> 전용면적: ${property.exclusiveArea}㎡</p>
            </div>
        </div>
    `;

    return new kakao.maps.InfoWindow({
        content: content,
        removable: true
    });
}

// 매물 목록 아이템 생성 함수 수정
function createPropertyListItem(property) {
    const div = document.createElement('div');
    div.className = 'property-item';
    div.innerHTML = `
        <h3><i class="fas fa-building"></i> ${property.complexName}</h3>
        <p><i class="fas fa-map-marker-alt"></i> ${property.roadAddress}</p>
        <p><i class="fas fa-home"></i> ${property.houseType}</p>
        <p class="price">
            <i class="fas fa-money-bill-wave"></i> 보증금: ${property.deposit.toLocaleString()}원<br>
            <i class="fas fa-coins"></i> 월세: ${property.monthlyRent.toLocaleString()}원
        </p>
        <p><i class="fas fa-expand"></i> 전용면적: ${property.exclusiveArea}㎡</p>
    `;

    div.addEventListener('click', () => {
        const coords = new kakao.maps.LatLng(property.latitude, property.longitude);
        map.setCenter(coords);
        map.setLevel(3); // 클릭 시 줌 레벨 조정
    });

    return div;
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