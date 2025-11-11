import pandas as pd
import numpy as np
from pymongo import MongoClient
from sklearn.model_selection import train_test_split
from sklearn.ensemble import RandomForestRegressor, GradientBoostingRegressor
from sklearn.metrics import r2_score, mean_squared_error
import os
from haversine import haversine
import warnings
warnings.filterwarnings('ignore')

class FinalAllModels:
    def __init__(self):
        self.models = {}
        self.facility_grids = {}
        self.grid_size = 0.01

    def load_all_facility_data(self):
        """모든 실제 시설 데이터 로드"""
        print("모든 실제 시설 데이터 로드 중...")

        base_path = "AI_DATA"
        file_paths = {
            'hospital': os.path.join(base_path, "hospitals.csv"),
            'shopping': os.path.join(base_path, "shopping_malls.csv"),
            'academy': os.path.join(base_path, "academy.csv"),
            'bus': os.path.join(base_path, "bus_station.csv"),
            'subway': os.path.join(base_path, "subway_coordinates.csv"),
            'school': os.path.join(base_path, "school.csv"),
            'kindergarten': os.path.join(base_path, "kindergarten.csv"),
            'cctv': os.path.join(base_path, "cctv_information.csv"),
            'convenience': os.path.join(base_path, "Convience_info.csv"),
            'fire': os.path.join(base_path, "fire_station_info.csv"),
            'park': os.path.join(base_path, "park_info.csv"),
            'police': os.path.join(base_path, "police_info.csv"),
            'crime': os.path.join(base_path, "Criminal_rate.csv")
        }

        dfs = {}
        encodings = {
            'convenience': 'utf-8', 'crime': 'utf-8', 'subway': 'utf-8', 'cctv': 'utf-8',
            'fire': 'euc-kr', 'park': 'euc-kr', 'police': 'euc-kr'
        }

        for key, path in file_paths.items():
            try:
                if key in encodings:
                    dfs[key] = pd.read_csv(path, encoding=encodings[key])
                else:
                    dfs[key] = pd.read_csv(path, encoding='cp949')
            except Exception as e:
                print(f"  {key} 로드 실패: {str(e)}")

        return dfs

    def preprocess_facilities_grid(self, dfs):
        """시설 데이터를 격자화하여 전처리"""
        facility_grids = {}
        column_mapping = {
            'school': {'lat': 'WGS84위도', 'lng': 'WGS84경도'},
            'hospital': {'lat': '위도', 'lng': '경도'},
            'bus': {'lat': 'WGS84위도', 'lng': 'WGS84경도'},
            'subway': {'lat': 'WGS84위도', 'lng': 'WGS84경도'},
            'shopping': {'lat': 'WGS84위도', 'lng': 'WGS84경도'},
            'academy': {'lat': 'WGS84위도', 'lng': 'WGS84경도'},
            'kindergarten': {'lat': 'WGS84위도', 'lng': 'WGS84경도'},
            'cctv': {'lat': 'WGS84위도', 'lng': 'WGS84경도'},
            'convenience': {'lat': 'WGS84위도', 'lng': 'WGS84경도'},
            'fire': {'lat': '위도', 'lng': '경도'},
            'police': {'lat': '위도', 'lng': '경도'},
            'park': {'lat': '위도', 'lng': '경도'}
        }

        for facility_type in column_mapping.keys():
            if facility_type in dfs:
                df = dfs[facility_type]
                lat_col = column_mapping[facility_type]['lat']
                lng_col = column_mapping[facility_type]['lng']

                df = df.dropna(subset=[lng_col, lat_col])
                mask = (df[lng_col].notna() & df[lat_col].notna() &
                       df[lng_col].ne(float('inf')) & df[lat_col].ne(float('inf')))
                df = df[mask]

                df['grid_x'] = (df[lng_col] // self.grid_size).astype(int)
                df['grid_y'] = (df[lat_col] // self.grid_size).astype(int)

                facility_grids[facility_type] = df.groupby(['grid_x', 'grid_y']).size().to_dict()

        self.facility_grids = facility_grids
        return facility_grids

    def calculate_distance_weight(self, distance_km, max_distance=3):
        """거리에 따른 가중치 계산"""
        if distance_km > max_distance:
            return 0
        return 1 / (1 + distance_km)

    def calculate_facility_count_grid(self, property_lat, property_lng, facility_type, radius_km=3):
        """격자화 방식으로 반경 내 시설 개수 계산"""
        if facility_type not in self.facility_grids:
            return 0

        property_grid_x = int(property_lng // self.grid_size)
        property_grid_y = int(property_lat // self.grid_size)

        grid_radius = int(radius_km / (self.grid_size * 111)) + 1

        total_count = 0

        for dx in range(-grid_radius, grid_radius + 1):
            for dy in range(-grid_radius, grid_radius + 1):
                grid_x = property_grid_x + dx
                grid_y = property_grid_y + dy

                grid_center_lat = (grid_y * self.grid_size) + (self.grid_size / 2)
                grid_center_lng = (grid_x * self.grid_size) + (self.grid_size / 2)

                distance = haversine((property_lat, property_lng), (grid_center_lat, grid_center_lng))

                if distance <= radius_km:
                    facility_count = self.facility_grids[facility_type].get((grid_x, grid_y), 0)
                    weight = self.calculate_distance_weight(distance, radius_km)
                    total_count += facility_count * weight

        return total_count

    def calculate_simple_rule_based_score(self, property_lat, property_lng):
        """간단한 룰베이스 점수 계산"""
        grid_x = int(property_lng // 0.01)
        grid_y = int(property_lat // 0.01)
        radius_grids = 2

        facility_counts = {
            'schools': 0, 'academies': 0, 'kindergartens': 0,
            'hospitals': 0, 'bus_stations': 0, 'subway_stations': 0,
            'shopping_malls': 0, 'convenience_stores': 0,
            'police_stations': 0, 'fire_stations': 0, 'cctvs': 0, 'parks': 0
        }

        for dx in range(-radius_grids, radius_grids + 1):
            for dy in range(-radius_grids, radius_grids + 1):
                x, y = grid_x + dx, grid_y + dy

                grid_center_lat = (y * 0.01) + 0.005
                grid_center_lng = (x * 0.01) + 0.005
                distance = haversine((property_lat, property_lng), (grid_center_lat, grid_center_lng))

                if distance <= 2:
                    distance_weight = 1 / (1 + distance * 2)

                    facility_counts['schools'] += self.facility_grids.get('school', {}).get((x, y), 0) * distance_weight
                    facility_counts['academies'] += self.facility_grids.get('academy', {}).get((x, y), 0) * distance_weight
                    facility_counts['kindergartens'] += self.facility_grids.get('kindergarten', {}).get((x, y), 0) * distance_weight
                    facility_counts['hospitals'] += self.facility_grids.get('hospital', {}).get((x, y), 0) * distance_weight
                    facility_counts['bus_stations'] += self.facility_grids.get('bus', {}).get((x, y), 0) * distance_weight
                    facility_counts['subway_stations'] += self.facility_grids.get('subway', {}).get((x, y), 0) * distance_weight
                    facility_counts['shopping_malls'] += self.facility_grids.get('shopping', {}).get((x, y), 0) * distance_weight
                    facility_counts['convenience_stores'] += self.facility_grids.get('convenience', {}).get((x, y), 0) * distance_weight
                    facility_counts['police_stations'] += self.facility_grids.get('police', {}).get((x, y), 0) * distance_weight
                    facility_counts['fire_stations'] += self.facility_grids.get('fire', {}).get((x, y), 0) * distance_weight
                    facility_counts['cctvs'] += self.facility_grids.get('cctv', {}).get((x, y), 0) * distance_weight
                    facility_counts['parks'] += self.facility_grids.get('park', {}).get((x, y), 0) * distance_weight

        # 점수 계산
        education_score = (
            facility_counts['schools'] * 0.3 +
            facility_counts['academies'] * 0.1 +
            facility_counts['kindergartens'] * 0.2
        )

        medical_score = facility_counts['hospitals'] * 0.4
        transport_score = (
            facility_counts['bus_stations'] * 0.05 +
            facility_counts['subway_stations'] * 0.8
        )
        shopping_score = facility_counts['shopping_malls'] * 0.3
        convenience_score = facility_counts['convenience_stores'] * 0.1
        safety_score = (
            facility_counts['police_stations'] * 1.0 +
            facility_counts['fire_stations'] * 0.8 +
            facility_counts['cctvs'] * 0.01
        )
        living_score = facility_counts['parks'] * 0.2

        total_score = (
            education_score * 0.2 +
            medical_score * 0.1 +
            transport_score * 0.25 +
            shopping_score * 0.1 +
            convenience_score * 0.1 +
            safety_score * 0.15 +
            living_score * 0.1
        )

        return min(total_score / 2, 5), facility_counts

    def create_complete_final_dataset(self):
        """최종 완전한 데이터셋 생성"""
        print("최종 완전한 데이터셋 생성 중...")

        # MongoDB 데이터 로드
        client = MongoClient('mongodb://localhost:27017/')
        db = client['houch_db']
        collection = db['matched_apartments_with_reviews']
        data = list(collection.find())

        # 시설 데이터 로드 및 격자화
        facility_dfs = self.load_all_facility_data()
        self.preprocess_facilities_grid(facility_dfs)

        df_list = []
        total_apartments = len(data)

        for i, doc in enumerate(data):
            if i % 100 == 0:
                print(f"  진행률: {i}/{total_apartments} ({i/total_apartments*100:.1f}%)")

            if 'individual_reviews' not in doc or not doc['individual_reviews']:
                continue

            apartment_lat = doc.get('위도', 0)
            apartment_lng = doc.get('경도', 0)

            if apartment_lat == 0 or apartment_lng == 0:
                continue

            # 룰베이스 점수 및 시설 개수 계산
            rule_based_score, facility_counts = self.calculate_simple_rule_based_score(apartment_lat, apartment_lng)

            for review in doc['individual_reviews']:
                row = {
                    'apartment_id': str(doc['_id']),
                    'latitude': apartment_lat,
                    'longitude': apartment_lng,
                    'traffic_score': review.get('trafficScore', 0),
                    'around_score': review.get('aroundScore', 0),
                    'care_score': review.get('careScore', 0),
                    'resident_score': review.get('residentScore', 0),
                    'total_score': review.get('score', 0),
                    'rule_based_score': rule_based_score,
                }

                # 격자화 방식으로 시설 개수 계산
                row['bus_stations'] = self.calculate_facility_count_grid(apartment_lat, apartment_lng, 'bus')
                row['subway_stations'] = self.calculate_facility_count_grid(apartment_lat, apartment_lng, 'subway')
                row['hospitals'] = self.calculate_facility_count_grid(apartment_lat, apartment_lng, 'hospital')
                row['schools'] = self.calculate_facility_count_grid(apartment_lat, apartment_lng, 'school')
                row['academies'] = self.calculate_facility_count_grid(apartment_lat, apartment_lng, 'academy')
                row['kindergartens'] = self.calculate_facility_count_grid(apartment_lat, apartment_lng, 'kindergarten')
                row['convenience_stores'] = self.calculate_facility_count_grid(apartment_lat, apartment_lng, 'convenience')
                row['shopping_malls'] = self.calculate_facility_count_grid(apartment_lat, apartment_lng, 'shopping')
                row['parks'] = self.calculate_facility_count_grid(apartment_lat, apartment_lng, 'park')
                row['cctvs'] = self.calculate_facility_count_grid(apartment_lat, apartment_lng, 'cctv')
                row['police_stations'] = self.calculate_facility_count_grid(apartment_lat, apartment_lng, 'police')
                row['fire_stations'] = self.calculate_facility_count_grid(apartment_lat, apartment_lng, 'fire')

                # 범죄율
                row['crime_rate_per_10k'] = 35.0  # 기본값

                df_list.append(row)

        df = pd.DataFrame(df_list)
        print(f"\n총 {len(df)}개의 최종 데이터 포인트 생성됨")

        return df

    def train_all_final_models(self, df):
        """모든 최종 모델 훈련"""
        print("\n" + "="*80)
        print("모든 최종 모델 훈련")
        print("="*80)

        results = {}

        # 1. 교통 여건 점수 예측 모델
        print("\n=== 1. 교통 여건 점수 예측 모델 ===")
        X = df[['bus_stations', 'subway_stations']].copy()
        X['total_transport'] = X['bus_stations'] + X['subway_stations'] * 3
        X['transport_density'] = X['total_transport'] / (X['total_transport'].max() + 1)

        y = df['traffic_score']
        X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

        model = RandomForestRegressor(n_estimators=100, max_depth=10, random_state=42)
        model.fit(X_train, y_train)

        y_pred = model.predict(X_test)
        r2 = r2_score(y_test, y_pred)
        mse = mean_squared_error(y_test, y_pred)

        print(f"R² Score: {r2:.4f}")
        print(f"MSE: {mse:.4f}")
        print(f"RMSE: {np.sqrt(mse):.4f}")

        results['traffic'] = {'r2': r2, 'mse': mse}
        self.models['traffic'] = model

        # 2. 주변 환경 점수 예측 모델
        print("\n=== 2. 주변 환경 점수 예측 모델 ===")
        X = df[['hospitals', 'schools', 'academies', 'kindergartens', 'convenience_stores', 'shopping_malls']].copy()
        X['education_total'] = X['schools'] + X['academies'] + X['kindergartens']
        X['convenience_total'] = X['convenience_stores'] + X['shopping_malls']
        X['total_amenities'] = X.iloc[:, :6].sum(axis=1)

        y = df['around_score']
        X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

        model = RandomForestRegressor(n_estimators=100, max_depth=10, random_state=42)
        model.fit(X_train, y_train)

        y_pred = model.predict(X_test)
        r2 = r2_score(y_test, y_pred)
        mse = mean_squared_error(y_test, y_pred)

        print(f"R² Score: {r2:.4f}")
        print(f"MSE: {mse:.4f}")
        print(f"RMSE: {np.sqrt(mse):.4f}")

        results['environment'] = {'r2': r2, 'mse': mse}
        self.models['environment'] = model

        # 3. 단지 관리 점수 예측 모델
        print("\n=== 3. 단지 관리 점수 예측 모델 ===")
        X = df[['parks', 'cctvs']].copy()
        X['safety_score'] = X['cctvs'] * 0.1
        X['leisure_score'] = X['parks'] * 2
        X['management_total'] = X['safety_score'] + X['leisure_score']

        y = df['care_score']
        X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

        model = RandomForestRegressor(n_estimators=100, max_depth=10, random_state=42)
        model.fit(X_train, y_train)

        y_pred = model.predict(X_test)
        r2 = r2_score(y_test, y_pred)
        mse = mean_squared_error(y_test, y_pred)

        print(f"R² Score: {r2:.4f}")
        print(f"MSE: {mse:.4f}")
        print(f"RMSE: {np.sqrt(mse):.4f}")

        results['management'] = {'r2': r2, 'mse': mse}
        self.models['management'] = model

        # 4. 거주 환경 점수 예측 모델
        print("\n=== 4. 거주 환경 점수 예측 모델 ===")
        X = df[['police_stations', 'fire_stations', 'crime_rate_per_10k']].copy()
        X['emergency_services'] = X['police_stations'] + X['fire_stations']
        X['safety_index'] = X['emergency_services'] * 5 - X['crime_rate_per_10k'] * 0.1
        X['crime_inverse'] = 100 - X['crime_rate_per_10k']

        y = df['resident_score']
        X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

        model = RandomForestRegressor(n_estimators=100, max_depth=10, random_state=42)
        model.fit(X_train, y_train)

        y_pred = model.predict(X_test)
        r2 = r2_score(y_test, y_pred)
        mse = mean_squared_error(y_test, y_pred)

        print(f"R² Score: {r2:.4f}")
        print(f"MSE: {mse:.4f}")
        print(f"RMSE: {np.sqrt(mse):.4f}")

        results['residence'] = {'r2': r2, 'mse': mse}
        self.models['residence'] = model

        # 5. 총 점수 예측 모델 (앞선 4개 모델 예측값 + 룰베이스)
        print("\n=== 5. 총 점수 예측 모델 (4개 모델 + 룰베이스) ===")

        # 앞선 모델들의 예측값 생성
        traffic_X = df[['bus_stations', 'subway_stations']].copy()
        traffic_X['total_transport'] = traffic_X['bus_stations'] + traffic_X['subway_stations'] * 3
        traffic_X['transport_density'] = traffic_X['total_transport'] / (traffic_X['total_transport'].max() + 1)
        traffic_pred = self.models['traffic'].predict(traffic_X)

        env_X = df[['hospitals', 'schools', 'academies', 'kindergartens', 'convenience_stores', 'shopping_malls']].copy()
        env_X['education_total'] = env_X['schools'] + env_X['academies'] + env_X['kindergartens']
        env_X['convenience_total'] = env_X['convenience_stores'] + env_X['shopping_malls']
        env_X['total_amenities'] = env_X.iloc[:, :6].sum(axis=1)
        env_pred = self.models['environment'].predict(env_X)

        mgmt_X = df[['parks', 'cctvs']].copy()
        mgmt_X['safety_score'] = mgmt_X['cctvs'] * 0.1
        mgmt_X['leisure_score'] = mgmt_X['parks'] * 2
        mgmt_X['management_total'] = mgmt_X['safety_score'] + mgmt_X['leisure_score']
        mgmt_pred = self.models['management'].predict(mgmt_X)

        res_X = df[['police_stations', 'fire_stations', 'crime_rate_per_10k']].copy()
        res_X['emergency_services'] = res_X['police_stations'] + res_X['fire_stations']
        res_X['safety_index'] = res_X['emergency_services'] * 5 - res_X['crime_rate_per_10k'] * 0.1
        res_X['crime_inverse'] = 100 - res_X['crime_rate_per_10k']
        res_pred = self.models['residence'].predict(res_X)

        # 종합 피처
        X = pd.DataFrame({
            'traffic_pred': traffic_pred,
            'environment_pred': env_pred,
            'management_pred': mgmt_pred,
            'residence_pred': res_pred,
            'rule_based_score': df['rule_based_score'],
            'avg_model_score': (traffic_pred + env_pred + mgmt_pred + res_pred) / 4,
            'score_variance': np.var([traffic_pred, env_pred, mgmt_pred, res_pred], axis=0),
            'min_score': np.min([traffic_pred, env_pred, mgmt_pred, res_pred], axis=0),
            'max_score': np.max([traffic_pred, env_pred, mgmt_pred, res_pred], axis=0)
        })

        y = df['total_score']
        X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

        model = GradientBoostingRegressor(n_estimators=100, learning_rate=0.1, random_state=42)
        model.fit(X_train, y_train)

        y_pred = model.predict(X_test)
        r2 = r2_score(y_test, y_pred)
        mse = mean_squared_error(y_test, y_pred)

        print(f"R² Score: {r2:.4f}")
        print(f"MSE: {mse:.4f}")
        print(f"RMSE: {np.sqrt(mse):.4f}")

        # 피처 중요도
        print("피처 중요도:")
        for name, imp in zip(X.columns, model.feature_importances_):
            print(f"  {name}: {imp:.4f}")

        results['total'] = {'r2': r2, 'mse': mse}
        self.models['total'] = model

        return results

    def run_final_all_models(self):
        """최종 모든 모델 실행"""
        print("="*80)
        print("최종 모든 모델 훈련 및 평가")
        print("="*80)

        # 완전한 데이터셋 생성
        df = self.create_complete_final_dataset()

        # 모든 모델 훈련
        results = self.train_all_final_models(df)

        # 최종 결과 요약
        print("\n" + "="*80)
        print("🎯 최종 모든 모델 성능 요약")
        print("="*80)

        model_names = {
            'traffic': '교통 여건 점수',
            'environment': '주변 환경 점수',
            'management': '단지 관리 점수',
            'residence': '거주 환경 점수',
            'total': '총 점수 (통합)'
        }

        avg_r2 = 0
        avg_mse = 0

        for model_key, metrics in results.items():
            model_name = model_names[model_key]
            print(f"{model_name}:")
            print(f"  R² Score: {metrics['r2']:.4f}")
            print(f"  MSE: {metrics['mse']:.4f}")
            print(f"  RMSE: {np.sqrt(metrics['mse']):.4f}")
            print("-" * 60)
            avg_r2 += metrics['r2']
            avg_mse += metrics['mse']

        print(f"🏆 전체 평균 R² Score: {avg_r2/5:.4f}")
        print(f"📊 전체 평균 MSE: {avg_mse/5:.4f}")
        print(f"📈 전체 평균 RMSE: {np.sqrt(avg_mse/5):.4f}")
        print("="*80)

        # 데이터셋 저장
        df.to_csv('final_all_models_dataset.csv', index=False, encoding='utf-8-sig')
        print(f"\n📁 최종 데이터셋이 'final_all_models_dataset.csv'로 저장되었습니다.")

        return results, df

if __name__ == "__main__":
    model_system = FinalAllModels()
    results, dataset = model_system.run_final_all_models()
