
from flask import Flask, request, jsonify
from flask_cors import CORS  # CORS 지원을 위해
import sys

# stdout 버퍼링 비활성화
sys.stdout.reconfigure(line_buffering=True)

def safe_print(*args, **kwargs):
    """안전한 print 함수 - cp949 인코딩 에러 방지"""
    try:
        print(*args, **kwargs)
    except UnicodeEncodeError:
        try:
            # 한글이 포함된 문자열을 안전하게 처리
            safe_args = []
            for arg in args:
                if isinstance(arg, str):
                    safe_args.append(arg.encode('utf-8', 'ignore').decode('utf-8'))
                else:
                    safe_args.append(str(arg))
            print(*safe_args, **kwargs)
        except:
            print("[Print encoding error]")
    except Exception:
        pass

# from recommendation_hybrid import recommend_properties_parallel  # 하이브리드 모델 제거
import joblib
import pandas as pd
import numpy as np
from haversine import haversine
from pymongo import MongoClient
from concurrent.futures import ThreadPoolExecutor, as_completed
from geopy.geocoders import Nominatim
from geopy.exc import GeocoderTimedOut
import time
import json

class NormalizedMetaModelPredictor:
    def __init__(self):
        # MongoDB 연결
        self.client = MongoClient("mongodb://localhost:27017/")
        self.db = self.client["houch_db"]

        # 모델들과 스케일러 로드
        self.load_models()

        # 캐시 시스템
        self.facility_cache = {}
        self.coordinates_cache = {}

        # LH 매물 데이터 로드
        self.load_lh_properties()

    def load_models(self):
        """모든 필요한 모델들 로드"""
        try:
            # 개별 집계 모델들 로드
            model_dir = "0921_train_model/aggregate_review_models"
            self.transport_model = joblib.load(f"{model_dir}/transport_model.pkl")
            self.environment_model = joblib.load(f"{model_dir}/environment_model.pkl")
            self.complex_model = joblib.load(f"{model_dir}/complex_model.pkl")
            self.living_model = joblib.load(f"{model_dir}/living_model.pkl")

            # 개별 모델 스케일러들
            self.individual_scalers = joblib.load(f"{model_dir}/scalers.pkl")

            # gb_coef_0.10 메타모델 로드 (R²=65.20%, 룰 중요도 49.36%, ML 중요도 46.19%)
            meta_model_dir = "0921_train_model/extensive_low_rule_models_20251009_1543"
            self.meta_model = joblib.load(f"{meta_model_dir}/rank1_gb_coef_0.10.pkl")
            self.meta_scaler = joblib.load(f"{meta_model_dir}/rank1_gb_coef_0.10_scaler.pkl")
            self.meta_rule_coef = 0.1  # 정규화 계수

            safe_print("모든 모델들이 성공적으로 로드되었습니다.")

        except Exception as e:
            safe_print(f"모델 로드 중 오류: {e}")
            raise

    def calculate_facility_density_features(self, lat: float, lng: float) -> dict:
        """시설 밀도 기반 피처 계산 (집계 모델 형식에 맞춤)"""
        cache_key = f"{round(lat, 4)}_{round(lng, 4)}_density"

        if cache_key in self.facility_cache:
            return self.facility_cache[cache_key]

        # 컬렉션 매핑
        facilities = {
            'bus_stations': {'lat': 'WGS84위도', 'lng': 'WGS84경도'},
            'subway_stations': {'lat': 'WGS84위도', 'lng': 'WGS84경도'},
            'hospitals': {'lat': '위도', 'lng': '경도'},
            'schools': {'lat': 'WGS84위도', 'lng': 'WGS84경도'},
            'academies': {'lat': 'WGS84위도', 'lng': 'WGS84경도'},
            'kindergartens': {'lat': 'WGS84위도', 'lng': 'WGS84경도'},
            'convience_stores': {'lat': 'WGS84위도', 'lng': 'WGS84경도'},
            'shopping_malls': {'lat': 'WGS84위도', 'lng': 'WGS84경도'},
            'parks': {'lat': '위도', 'lng': '경도'},
            'cctvs': {'lat': 'WGS84위도', 'lng': 'WGS84경도'},
            'polices': {'lat': '위도', 'lng': '경도'},
            'fire_stations': {'lat': '위도', 'lng': '경도'}
        }

        # 다양한 반경에서 밀도 계산 (집계 모델과 동일)
        radii = [0.5, 1.0, 2.0, 3.0, 5.0]  # km
        density_features = {}

        for facility_name, coords in facilities.items():
            try:
                collection = self.db[facility_name]

                for radius in radii:
                    query = {
                        coords['lat']: {"$exists": True, "$ne": None, "$type": "number"},
                        coords['lng']: {"$exists": True, "$ne": None, "$type": "number"}
                    }

                    count = 0
                    cursor = collection.find(query, {coords['lat']: 1, coords['lng']: 1})

                    for facility in cursor:
                        try:
                            facility_lat = float(facility[coords['lat']])
                            facility_lng = float(facility[coords['lng']])

                            distance = haversine((lat, lng), (facility_lat, facility_lng))
                            if distance <= radius:
                                count += 1
                        except:
                            continue

                    # 밀도 = 개수 / 면적
                    area = np.pi * (radius ** 2)
                    density = count / area if area > 0 else 0

                    density_features[f"{facility_name}_density_{radius}km"] = density

            except Exception as e:
                safe_print(f"시설 밀도 계산 오류 ({facility_name}): {e}")
                for radius in radii:
                    density_features[f"{facility_name}_density_{radius}km"] = 0

        self.facility_cache[cache_key] = density_features
        return density_features

    def get_area_demographics(self, lat: float, lng: float) -> dict:
        """지역 인구통계학적 정보 (기본값 반환)"""
        # 실제 서비스에서는 좌표 기반으로 지역 정보를 추출해야 하지만,
        # 테스트를 위해 기본값 반환
        return {
            'population_density': 1000,
            'crime_rate': 50,
            'urbanization_level': 3
        }

    def load_lh_properties(self):
        """LH 매물 데이터 로드"""
        try:
            self.lh_properties = pd.read_csv("AI_DATA/lh_happyhouse_list.csv", encoding='cp949')
            safe_print(f"LH 매물 데이터 로드 완료: {len(self.lh_properties)}개")

            # 좌표 캐시 로드
            self.load_coordinates_cache()
        except Exception as e:
            safe_print(f"LH 매물 데이터 로드 실패: {e}")
            self.lh_properties = pd.DataFrame()

    def load_coordinates_cache(self):
        """좌표 캐시 로드"""
        try:
            with open('AI_DATA/coordinates.json', 'r', encoding='utf-8') as f:
                self.coordinates_cache = json.load(f)
            safe_print(f"좌표 캐시 로드 완료: {len(self.coordinates_cache)}개")
        except FileNotFoundError:
            safe_print("좌표 캐시 파일이 없습니다. 새로 생성합니다.")
            self.coordinates_cache = {}

    def save_coordinates_cache(self):
        """좌표 캐시 저장"""
        try:
            with open('AI_DATA/coordinates.json', 'w', encoding='utf-8') as f:
                json.dump(self.coordinates_cache, f, ensure_ascii=False)
        except Exception as e:
            safe_print(f"좌표 캐시 저장 실패: {e}")

    def get_coordinates(self, address):
        """주소를 위도/경도로 변환"""
        if address in self.coordinates_cache:
            return self.coordinates_cache[address]

        geolocator = Nominatim(user_agent="lh_recommendation_agent")
        try:
            location = geolocator.geocode(address)
            if location:
                coords = (location.latitude, location.longitude)
                self.coordinates_cache[address] = coords
                return coords
        except GeocoderTimedOut:
            safe_print(f"Geocoding timeout for address: {address}")
        except Exception as e:
            safe_print(f"Geocoding error for address {address}: {e}")

        return None

    def predict_individual_scores(self, lat: float, lng: float) -> dict:
        """개별 모델들로 각 영역 점수 예측"""
        density_features = self.calculate_facility_density_features(lat, lng)
        area_info = self.get_area_demographics(lat, lng)

        # 각 모델별 피처 구성 (집계 모델과 동일한 구조)
        model_features = {
            'transport': {
                # 밀도 피처 (버스, 지하철)
                **{k: v for k, v in density_features.items() if 'bus_stations' in k or 'subway_stations' in k},
                # 지역 정보
                'population_density': area_info['population_density'],
                'urbanization_level': area_info['urbanization_level'],
                # 리뷰 일관성 (예측 시에는 기본값)
                'review_count': 10,  # 가상값
                'score_consistency': 0.8  # 가상값
            },

            'environment': {
                # 밀도 피처 (병원, 학교, 학원, 유치원, 편의점, 쇼핑몰)
                **{k: v for k, v in density_features.items() if any(facility in k for facility in ['hospitals', 'schools', 'academies', 'kindergartens', 'convience_stores', 'shopping_malls'])},
                # 지역 정보
                'population_density': area_info['population_density'],
                'urbanization_level': area_info['urbanization_level'],
                # 리뷰 일관성
                'review_count': 10,
                'score_consistency': 0.8
            },

            'complex': {
                # 밀도 피처 (공원, CCTV)
                **{k: v for k, v in density_features.items() if 'parks' in k or 'cctvs' in k},
                # 지역 정보
                'population_density': area_info['population_density'],
                'urbanization_level': area_info['urbanization_level'],
                # 리뷰 일관성
                'review_count': 10,
                'score_consistency': 0.8
            },

            'living': {
                # 밀도 피처 (경찰서, 소방서)
                **{k: v for k, v in density_features.items() if 'polices' in k or 'fire_stations' in k},
                # 범죄 정보
                'crime_rate': area_info['crime_rate'],
                'population_density': area_info['population_density'],
                'urbanization_level': area_info['urbanization_level'],
                # 리뷰 일관성
                'review_count': 10,
                'score_consistency': 0.8
            }
        }

        predictions = {}

        for model_type in ['transport', 'environment', 'complex', 'living']:
            model = getattr(self, f"{model_type}_model")
            scaler = self.individual_scalers[model_type]

            # 피처를 DataFrame으로 변환 후 target_score 제거
            features_df = pd.DataFrame([model_features[model_type]])
            if 'target_score' in features_df.columns:
                features_df = features_df.drop('target_score', axis=1)

            # 스케일링 및 예측
            X_scaled = scaler.transform(features_df)
            prediction = model.predict(X_scaled)[0]
            predictions[model_type] = prediction  # 원본 점수 그대로

        return predictions

    def calculate_rule_based_scores(self, lat: float, lng: float, user_preferences: dict = None) -> dict:
        """룰베이스 7개 영역 점수 계산"""
        density_features = self.calculate_facility_density_features(lat, lng)
        area_info = self.get_area_demographics(lat, lng)

        # 7개 영역별 점수 계산 (1-5 범위)
        scores = {}

        # 개수 기반 계산으로 변경 (밀도 대신 반경별 시설 개수 합산)

        # 1. 교통 (버스정류장, 지하철역 개수) - 0~1 범위
        bus_count = self.count_nearby_facilities(lat, lng, 'bus_stations', [0.5, 1.0, 2.0])
        subway_count = self.count_nearby_facilities(lat, lng, 'subway_stations', [0.5, 1.0, 2.0])
        transport_score = min(1.0, max(0.0, (bus_count * 0.02 + subway_count * 0.05)))
        scores['교통'] = round(transport_score, 3)

        # 2. 교육 (학교, 학원 개수) - 0~1 범위
        school_count = self.count_nearby_facilities(lat, lng, 'schools', [1.0, 2.0, 3.0])
        academy_count = self.count_nearby_facilities(lat, lng, 'academies', [1.0, 2.0, 3.0])
        education_score = min(1.0, max(0.0, (school_count * 0.03 + academy_count * 0.02)))
        scores['교육'] = round(education_score, 3)

        # 3. 의료 (병원 개수) - 0~1 범위
        hospital_count = self.count_nearby_facilities(lat, lng, 'hospitals', [1.0, 2.0, 3.0])
        medical_score = min(1.0, max(0.0, hospital_count * 0.04))
        scores['의료'] = round(medical_score, 3)

        # 4. 쇼핑 (쇼핑몰, 편의점 개수) - 0~1 범위
        mall_count = self.count_nearby_facilities(lat, lng, 'shopping_malls', [1.0, 2.0, 3.0])
        store_count = self.count_nearby_facilities(lat, lng, 'convience_stores', [0.5, 1.0, 2.0])
        shopping_score = min(1.0, max(0.0, (mall_count * 0.06 + store_count * 0.01)))
        scores['쇼핑'] = round(shopping_score, 3)

        # 5. 쾌적성 (공원 개수) - 0~1 범위
        park_count = self.count_nearby_facilities(lat, lng, 'parks', [1.0, 2.0, 3.0])
        comfort_score = min(1.0, max(0.0, park_count * 0.05))
        scores['쾌적성'] = round(comfort_score, 3)

        # 6. 안전성 (경찰서, CCTV 개수, 범죄율) - 0~1 범위
        police_count = self.count_nearby_facilities(lat, lng, 'polices', [2.0, 3.0, 5.0])
        cctv_count = self.count_nearby_facilities(lat, lng, 'cctvs', [0.5, 1.0, 2.0])
        crime_rate = area_info.get('crime_rate', 50)
        safety_score = min(1.0, max(0.0, 0.1 + police_count * 0.1 + cctv_count * 0.002 + (50 - crime_rate) * 0.004))
        scores['안전성'] = round(safety_score, 3)

        # 7. 편의성 (종합적인 편의시설 개수) - 0~1 범위
        convenience_score = min(1.0, max(0.0, (store_count * 0.004 + mall_count * 0.04 + hospital_count * 0.02)))
        scores['편의성'] = round(convenience_score, 3)

        return scores

    def count_nearby_facilities(self, lat: float, lng: float, facility_name: str, radii: list) -> int:
        """주변 시설 개수 계산"""
        try:
            facilities_info = {
                'bus_stations': {'lat': 'WGS84위도', 'lng': 'WGS84경도'},
                'subway_stations': {'lat': 'WGS84위도', 'lng': 'WGS84경도'},
                'hospitals': {'lat': '위도', 'lng': '경도'},
                'schools': {'lat': 'WGS84위도', 'lng': 'WGS84경도'},
                'academies': {'lat': 'WGS84위도', 'lng': 'WGS84경도'},
                'kindergartens': {'lat': 'WGS84위도', 'lng': 'WGS84경도'},
                'convience_stores': {'lat': 'WGS84위도', 'lng': 'WGS84경도'},
                'shopping_malls': {'lat': 'WGS84위도', 'lng': 'WGS84경도'},
                'parks': {'lat': '위도', 'lng': '경도'},
                'cctvs': {'lat': 'WGS84위도', 'lng': 'WGS84경도'},
                'polices': {'lat': '위도', 'lng': '경도'},
                'fire_stations': {'lat': '위도', 'lng': '경도'}
            }

            coords = facilities_info.get(facility_name)
            if not coords:
                return 0

            collection = self.db[facility_name]
            max_radius = max(radii)

            count = 0
            query = {
                coords['lat']: {"$exists": True, "$ne": None, "$type": "number"},
                coords['lng']: {"$exists": True, "$ne": None, "$type": "number"}
            }

            cursor = collection.find(query, {coords['lat']: 1, coords['lng']: 1})

            for facility in cursor:
                try:
                    facility_lat = float(facility[coords['lat']])
                    facility_lng = float(facility[coords['lng']])

                    distance = haversine((lat, lng), (facility_lat, facility_lng))
                    if distance <= max_radius:
                        count += 1
                except:
                    continue

            return count

        except Exception as e:
            safe_print(f"시설 개수 계산 오류 ({facility_name}): {e}")
            return 0

    def predict_final_score(self, lat: float, lng: float, user_preferences: dict = None) -> dict:
        """정규화된 메타모델로 최종 점수 예측"""
        # 개별 모델 예측
        ml_predictions = self.predict_individual_scores(lat, lng)

        # 사용자 선호도 처리 (기본값 설정)
        if user_preferences is None:
            user_preferences = {
                '교통': 0.25,
                '교육': 0.25,
                '의료': 0.10,
                '쾌적성': 0.15,
                '안전성': 0.15,
                '편의성': 0.10
            }

        # 영어-한국어 매핑 지원
        def get_preference_value(korean_key, english_key, default_value):
            return user_preferences.get(korean_key,
                   user_preferences.get(english_key, default_value))

        # 개별 점수별 가중치 계산
        transport_weight = get_preference_value('교통', 'transport', 0.25)
        education_weight = get_preference_value('교육', 'education', 0.25)
        medical_weight = get_preference_value('의료', 'medical', 0.10)
        comfort_weight = get_preference_value('쾌적성', 'comfort', 0.15)
        safety_weight = get_preference_value('안전성', 'safety', 0.15)
        convenience_weight = get_preference_value('편의성', 'convenience', 0.10)

        environment_weight = education_weight + medical_weight
        complex_weight = comfort_weight
        living_weight = safety_weight + convenience_weight

        # 룰베이스 정규화 점수 계산
        ml_mean = np.mean(list(ml_predictions.values()))
        ml_std = np.std(list(ml_predictions.values()))

        # 룰베이스 원본 점수 (사용자 선호도 반영)
        rule_based_original = (
            ml_predictions['transport'] * transport_weight +
            ml_predictions['environment'] * environment_weight +
            ml_predictions['complex'] * complex_weight +
            ml_predictions['living'] * living_weight
        )

        # 정규화된 룰베이스 점수 (coef_0.3_more_features 모델용 계수 0.3)
        rule_normalized = ml_mean + (rule_based_original - 3.0) * (ml_std / 0.5) * self.meta_rule_coef

        # ML 점수 분산
        ml_score_variance = np.var(list(ml_predictions.values()))

        # 메타모델 입력 피처 준비
        meta_features = [
            ml_predictions['transport'],
            ml_predictions['environment'],
            ml_predictions['complex'],
            ml_predictions['living'],
            rule_normalized,
            ml_score_variance
        ]

        # 메타모델로 최종 예측
        X_meta = np.array(meta_features).reshape(1, -1)
        X_meta_scaled = self.meta_scaler.transform(X_meta)
        final_score_raw = self.meta_model.predict(X_meta_scaled)[0]

        # 디버그: 원본 점수 출력
        safe_print(f"[모델 출력] 원본 최종점수: {final_score_raw:.3f}, ML점수: T={ml_predictions['transport']:.2f} E={ml_predictions['environment']:.2f} C={ml_predictions['complex']:.2f} L={ml_predictions['living']:.2f}")

        # 룰베이스 7개 영역 점수 계산
        rule_based_scores = self.calculate_rule_based_scores(lat, lng, user_preferences)

        # 클리핑 제거 - 원본 점수 그대로 사용
        return {
            'final_score': round(final_score_raw, 3),
            'individual_scores': {
                'transport': round(ml_predictions['transport'], 3),
                'environment': round(ml_predictions['environment'], 3),
                'complex': round(ml_predictions['complex'], 3),
                'living': round(ml_predictions['living'], 3)
            },
            'rule_based_scores': rule_based_scores,
            'rule_normalized': rule_normalized,
            'ml_score_variance': ml_score_variance
        }

def recommend_properties_with_metamodel(preferences, preferred_regions=None, max_deposit=None, max_monthly=None, top_n=5):
    """메타모델을 사용한 매물 추천"""
    predictor_instance = get_predictor()

    def process_property(row):
        try:
            # 지역 필터링
            if preferred_regions:
                property_region = row['시군구'].strip()
                if property_region not in preferred_regions:
                    return None

            # 예산 필터링
            deposit = float(str(row['임대보증금']).replace(',', ''))
            monthly = float(str(row['월임대료']).replace(',', ''))

            within_budget = True
            if (max_deposit and deposit > max_deposit) or (max_monthly and monthly > max_monthly):
                within_budget = False

            # 주소로부터 좌표 가져오기
            address = row['도로명주소']
            coords = predictor_instance.get_coordinates(address)

            if not coords:
                return None

            lat, lng = coords

            # 메타모델로 점수 예측
            result = predictor_instance.predict_final_score(lat, lng, preferences)
            final_score = result['final_score']

            # 예산 초과 매물은 점수가 매우 높은 경우에만 포함
            if not within_budget and final_score < 4.0:
                return None

            return {
                'score': round(final_score, 3),
                'detail_scores': {
                    # 룰베이스 7개 영역만 (육각형 차트용)
                    **result['rule_based_scores']
                },
                'address': address,
                'coordinates': {'latitude': lat, 'longitude': lng},
                'within_budget': within_budget,
                'region': row['시군구'],
                'details': {
                    '단지명': row['단지명'],
                    '세대수': row['세대수'],
                    '주택유형': row['주택유형'],
                    '공급면적(전용)': row['공급면적(전용)'],
                    '임대보증금': deposit,
                    '월임대료': monthly
                }
            }

        except Exception as e:
            safe_print(f"매물 처리 오류: {e}")
            return None

    # 지역 및 예산 필터링을 먼저 적용하여 처리할 매물 수 줄이기
    filtered_properties = predictor_instance.lh_properties.copy()

    # 지역 필터링 먼저 적용
    if preferred_regions:
        safe_print(f"지역 필터링: {preferred_regions}")
        filtered_properties = filtered_properties[filtered_properties['시군구'].isin(preferred_regions)]
        safe_print(f"지역 필터링 후 매물 수: {len(filtered_properties)}개")

    # 예산 필터링도 미리 적용 (일부)
    if max_deposit or max_monthly:
        initial_count = len(filtered_properties)
        if max_deposit:
            # 임대보증금이 숫자가 아닌 경우 처리
            filtered_properties = filtered_properties[
                pd.to_numeric(filtered_properties['임대보증금'].astype(str).str.replace(',', ''), errors='coerce') <= max_deposit * 1.5  # 1.5배까지 허용 (프리미엄 매물)
            ]
        if max_monthly:
            # 월임대료가 숫자가 아닌 경우 처리
            filtered_properties = filtered_properties[
                pd.to_numeric(filtered_properties['월임대료'].astype(str).str.replace(',', ''), errors='coerce') <= max_monthly * 1.5  # 1.5배까지 허용 (프리미엄 매물)
            ]
        safe_print(f"예산 필터링 후 매물 수: {len(filtered_properties)}개 (기존: {initial_count}개)")

    if len(filtered_properties) == 0:
        safe_print("필터링 조건에 맞는 매물이 없습니다.")
        return {'recommended': [], 'premium': []}

    safe_print("매물 평가 시작...")

    # 필터링된 매물만 병렬 처리
    with ThreadPoolExecutor(max_workers=4) as executor:
        property_scores = []
        futures = [executor.submit(process_property, row) for _, row in filtered_properties.iterrows()]

        processed_count = 0
        for future in as_completed(futures):
            result = future.result()
            if result:
                property_scores.append(result)

            processed_count += 1
            if processed_count % 50 == 0:  # 더 자주 업데이트
                safe_print(f"처리 진행률: {processed_count}/{len(futures)}")

    safe_print(f"총 {len(property_scores)}개 매물 평가 완료")

    # 중복 매물 제거 (주소 기준)
    unique_properties = {}
    for prop in property_scores:
        address = prop.get('address', '')
        # 동일 주소가 이미 있으면, 점수가 더 높은 것만 유지
        if address not in unique_properties or prop['score'] > unique_properties[address]['score']:
            unique_properties[address] = prop

    property_scores = list(unique_properties.values())
    safe_print(f"중복 제거 후: {len(property_scores)}개 매물")

    # 예산 내 매물과 초과 매물 분리
    within_budget = [prop for prop in property_scores if prop['within_budget']]
    premium = [prop for prop in property_scores if not prop['within_budget']]

    # 점수순 정렬
    within_budget = sorted(within_budget, key=lambda x: x['score'], reverse=True)
    premium = sorted(premium, key=lambda x: x['score'], reverse=True)

    # 예산 내 최고 점수보다 10% 이상 높은 프리미엄 매물만 선택
    if within_budget:
        best_normal_score = within_budget[0]['score']
        premium = [p for p in premium if p['score'] > best_normal_score * 1.1]

    # 좌표 캐시 저장
    predictor_instance.save_coordinates_cache()

    return {
        'recommended': within_budget[:top_n],
        'premium': premium[:3] if premium else []
    }

# 전역 예측기 인스턴스
predictor = None

def get_predictor():
    global predictor
    if predictor is None:
        predictor = NormalizedMetaModelPredictor()
    return predictor

app = Flask(__name__)
CORS(app, resources={
    r"/api/*": {
        "origins": ["http://localhost:8080"],
        "methods": ["GET", "POST", "OPTIONS"],
        "allow_headers": ["Content-Type", "Authorization"]
    }
})

@app.route('/api/recommend', methods=['POST'])
def recommend_properties():
    # 기존 룰베이스 기반으로 작동하던 코드를 메타모델 API로 리다이렉트
    return recommend_properties_with_meta()

@app.route('/api/recommendations', methods=['POST'])
def recommend_properties_alternative():
    """프론트엔드 호환성을 위한 추가 엔드포인트"""
    safe_print("API recommendations request - redirecting to meta-model")

    try:
        data = request.get_json()

        # 프론트엔드 형식을 Flask 형식으로 변환
        flask_data = {
            'preferences': data.get('preferences', {}),
            'maxDeposit': data.get('maxDeposit'),
            'maxMonthly': data.get('maxMonthly'),
            'preferredRegions': data.get('preferredRegions', []),  # 빈 배열로 기본값 설정
            'topN': 5
        }

        # 임시로 request 데이터 교체
        original_data = request.get_json
        request.get_json = lambda: flask_data

        # 메타모델 API 호출
        response = recommend_properties_with_meta()

        # 원래 request 복원
        request.get_json = original_data

        # 응답 형식을 프론트엔드가 기대하는 형식으로 변환
        if hasattr(response, 'get_json'):
            result = response.get_json()
            return jsonify({
                'properties': result.get('recommendedProperties', []) + result.get('premiumProperties', [])
            })
        else:
            return response

    except Exception as e:
        safe_print(f"API recommendations error: {e}")
        return jsonify({'properties': []}), 500

@app.route('/api/recommend-meta', methods=['POST'])
def recommend_properties_with_meta():
    """메타모델을 사용한 매물 추천 API"""
    try:
        safe_print("Meta model recommendation API request received")
    except:
        pass

    if not request.is_json:
        return jsonify({"error": "Content-Type must be application/json"}), 415

    try:
        data = request.get_json()

        # 요청 데이터 확인
        required_fields = ['preferences', 'maxDeposit', 'maxMonthly']
        if not all(field in data for field in required_fields):
            return jsonify({"error": "Missing required parameters: preferences, maxDeposit, maxMonthly"}), 400

        preferences = data['preferences']
        max_deposit = data['maxDeposit']
        max_monthly = data['maxMonthly']
        preferred_regions = data.get('preferredRegions', None)
        top_n = data.get('topN', 5)

        safe_print("="*50)
        safe_print("Meta model recommendation API request:")
        safe_print(f"Preferences: {preferences}")
        safe_print(f"Max deposit: {max_deposit:,}")
        safe_print(f"Max monthly: {max_monthly:,}")
        safe_print(f"Preferred regions: {preferred_regions}")
        safe_print("="*50)

        # 메타모델 기반 추천 실행
        recommendations = recommend_properties_with_metamodel(
            preferences=preferences,
            preferred_regions=preferred_regions,
            max_deposit=max_deposit,
            max_monthly=max_monthly,
            top_n=top_n
        )

        # 응답 데이터 구성 (기존 형식과 동일)
        response = {
            'recommendedProperties': recommendations['recommended'],
            'premiumProperties': recommendations['premium']
        }

        safe_print("="*50)
        safe_print("Meta model recommendation API response:")
        safe_print(f"Recommended properties: {len(response['recommendedProperties'])}")
        safe_print(f"Premium properties: {len(response['premiumProperties'])}")
        if response['recommendedProperties']:
            safe_print(f"Top property score: {response['recommendedProperties'][0].get('score', 'N/A')}")
        safe_print("="*50)

        return jsonify(response)

    except Exception as e:
        try:
            safe_print("Meta recommendation error occurred:", str(e))
        except:
            pass
        return jsonify({"error": str(e)}), 500



if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5001, debug=True)
