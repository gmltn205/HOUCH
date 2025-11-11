
import joblib
import pandas as pd
import numpy as np
from xgboost import XGBRegressor
from sklearn.model_selection import train_test_split
from sklearn.metrics import r2_score, mean_squared_error, mean_absolute_error
from sklearn.preprocessing import StandardScaler
import os
import logging
from datetime import datetime

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

def train_meta_extensive(meta_data, config):
    train_df, test_df = train_test_split(meta_data, test_size=0.2, random_state=42)

    # 훈련 데이터 준비
    X_train_list = []
    for _, row in train_df.iterrows():
        ml_scores = [row['ml_transport'], row['ml_environment'], row['ml_complex'], row['ml_living']]
        ml_mean = np.mean(ml_scores)
        ml_std = np.std(ml_scores)
        ml_variance = np.var(ml_scores)

        rule_original = row['rule_based_total']

        # 정규화 계수
        rule_normalized = ml_mean + (rule_original - 3.0) * (ml_std / 0.5) * config['rule_coef']

        # 룰베이스 변환 옵션
        if config.get('transform_rule', 'none') == 'log':
            # 로그 변환으로 영향력 감소
            rule_normalized = np.sign(rule_normalized - 3.0) * np.log1p(abs(rule_normalized - 3.0)) + 3.0
        elif config.get('transform_rule', 'none') == 'sqrt':
            # 제곱근 변환
            rule_normalized = np.sign(rule_normalized - 3.0) * np.sqrt(abs(rule_normalized - 3.0)) + 3.0
        elif config.get('transform_rule', 'none') == 'clip':
            # 극단값 제한
            rule_normalized = np.clip(rule_normalized, ml_mean - 1.0, ml_mean + 1.0)

        # ML 점수 강화 옵션
        if config.get('boost_ml', False):
            boost_factor = config.get('boost_factor', 1.5)
            # ML 점수들을 평균에서 멀어지게 강화
            ml_scores_boosted = [ml_mean + (score - ml_mean) * boost_factor for score in ml_scores]
        else:
            ml_scores_boosted = ml_scores

        # 추가 피처 생성 옵션
        features = ml_scores_boosted + [rule_normalized, ml_variance]

        if config.get('add_interactions', False):
            # ML 점수 간 상호작용 추가
            features.append(ml_scores[0] * ml_scores[1])  # transport * environment
            features.append(ml_scores[2] * ml_scores[3])  # complex * living

        if config.get('add_ml_features', False):
            # ML 통계 피처 추가
            features.append(max(ml_scores) - min(ml_scores))  # range
            features.append(np.median(ml_scores))  # median

        X_train_list.append(features)

    # 테스트 데이터 준비 (동일한 변환)
    X_test_list = []
    for _, row in test_df.iterrows():
        ml_scores = [row['ml_transport'], row['ml_environment'], row['ml_complex'], row['ml_living']]
        ml_mean = np.mean(ml_scores)
        ml_std = np.std(ml_scores)
        ml_variance = np.var(ml_scores)

        rule_original = row['rule_based_total']
        rule_normalized = ml_mean + (rule_original - 3.0) * (ml_std / 0.5) * config['rule_coef']

        if config.get('transform_rule', 'none') == 'log':
            rule_normalized = np.sign(rule_normalized - 3.0) * np.log1p(abs(rule_normalized - 3.0)) + 3.0
        elif config.get('transform_rule', 'none') == 'sqrt':
            rule_normalized = np.sign(rule_normalized - 3.0) * np.sqrt(abs(rule_normalized - 3.0)) + 3.0
        elif config.get('transform_rule', 'none') == 'clip':
            rule_normalized = np.clip(rule_normalized, ml_mean - 1.0, ml_mean + 1.0)

        if config.get('boost_ml', False):
            boost_factor = config.get('boost_factor', 1.5)
            ml_scores_boosted = [ml_mean + (score - ml_mean) * boost_factor for score in ml_scores]
        else:
            ml_scores_boosted = ml_scores

        features = ml_scores_boosted + [rule_normalized, ml_variance]

        if config.get('add_interactions', False):
            features.append(ml_scores[0] * ml_scores[1])
            features.append(ml_scores[2] * ml_scores[3])

        if config.get('add_ml_features', False):
            features.append(max(ml_scores) - min(ml_scores))
            features.append(np.median(ml_scores))

        X_test_list.append(features)

    X_train = np.array(X_train_list)
    y_train = train_df['target_final_score'].values
    X_test = np.array(X_test_list)
    y_test = test_df['target_final_score'].values

    # 스케일러 선택
    scaler_type = config.get('scaler', 'standard')
    if scaler_type == 'robust':
        from sklearn.preprocessing import RobustScaler
        scaler = RobustScaler()
    elif scaler_type == 'minmax':
        from sklearn.preprocessing import MinMaxScaler
        scaler = MinMaxScaler()
    else:
        scaler = StandardScaler()

    X_train_scaled = scaler.fit_transform(X_train)
    X_test_scaled = scaler.transform(X_test)


    model = XGBRegressor(
        n_estimators=config.get('n_estimators', 200),
        max_depth=config.get('max_depth', 10),
        learning_rate=config.get('learning_rate', 0.1),
        random_state=42,
        n_jobs=-1
    )

    model.fit(X_train_scaled, y_train)

    # 평가
    y_pred = model.predict(X_test_scaled)
    r2 = r2_score(y_test, y_pred)
    rmse = np.sqrt(mean_squared_error(y_test, y_pred))
    mae = mean_absolute_error(y_test, y_pred)

    # 피처 중요도
    importances = model.feature_importances_

    # 기본 6개 피처
    ml_total = importances[0] + importances[1] + importances[2] + importances[3]
    rule_imp = importances[4]
    var_imp = importances[5]

    return {
        'config': config,
        'model': model,
        'scaler': scaler,
        'r2': r2,
        'rmse': rmse,
        'mae': mae,
        'ml_total': ml_total,
        'rule_imp': rule_imp,
        'var_imp': var_imp,
        'train_size': len(X_train),
        'test_size': len(X_test),
        'n_features': X_train.shape[1]
    }

def main():
    cache_file = "0921_train_model/full_reviews_meta_cache.pkl"
    logger.info(f"캐시 파일 로드 중: {cache_file}")
    meta_data = joblib.load(cache_file)

    logger.info(f"메타 훈련 데이터 로드 완료: {len(meta_data)}개")

    logger.info("\n" + "="*100)
    logger.info("XGBoost 모델 (계수 0.1) 학습")
    logger.info("="*100)

    # XGBoost 모델, 계수 0.1만 학습
    config = {
        'name': 'xgb_coef_0.10',
        'rule_coef': 0.1,
        'model_type': 'xgb',
        'n_estimators': 200,
        'max_depth': 5,
        'max_features': None,
        'learning_rate': 0.1
    }

    logger.info(f"학습 시작: {config['name']}\n")

    try:
        result = train_meta_extensive(meta_data, config)
        logger.info(f"✅ 학습 완료!")
        logger.info(f"  R²: {result['r2']:.4f}")
        logger.info(f"  RMSE: {result['rmse']:.4f}")
        logger.info(f"  MAE: {result['mae']:.4f}")
        logger.info(f"  ML 중요도: {result['ml_total']*100:.2f}%")
        logger.info(f"  룰 중요도: {result['rule_imp']*100:.2f}%")
        logger.info(f"  분산 중요도: {result['var_imp']*100:.2f}%")

        # 모델 저장
        timestamp = datetime.now().strftime("%Y%m%d_%H%M")
        save_dir = f"0921_train_model/xgb_model_{timestamp}"
        os.makedirs(save_dir, exist_ok=True)

        model_path = os.path.join(save_dir, "rank1_gb_coef_0.10.pkl")
        scaler_path = os.path.join(save_dir, "rank1_gb_coef_0.10_scaler.pkl")

        joblib.dump(result['model'], model_path)
        joblib.dump(result['scaler'], scaler_path)

        logger.info(f"\n모델 저장 완료: {save_dir}")

        # 결과 파일 저장
        result_file = os.path.join(save_dir, "training_result.txt")
        with open(result_file, 'w', encoding='utf-8') as f:
            f.write(f"XGBoost 모델 (계수 0.1) 학습 결과\n")
            f.write(f"학습 일시: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
            f.write(f"데이터: {len(meta_data)}개\n\n")
            f.write(f"성능 지표:\n")
            f.write(f"  R²: {result['r2']:.4f}\n")
            f.write(f"  RMSE: {result['rmse']:.4f}\n")
            f.write(f"  MAE: {result['mae']:.4f}\n\n")
            f.write(f"피처 중요도:\n")
            f.write(f"  ML 점수: {result['ml_total']*100:.2f}%\n")
            f.write(f"  룰베이스: {result['rule_imp']*100:.2f}%\n")
            f.write(f"  분산: {result['var_imp']*100:.2f}%\n")

        logger.info("="*100)
        logger.info("✅ 완료!")

    except Exception as e:
        logger.error(f"❌ 오류 발생: {e}")
        raise

if __name__ == "__main__":
    main()
