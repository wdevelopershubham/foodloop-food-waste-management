"""Builds a SYNTHETIC history of food prepared vs. consumed and trains the surplus model.
Replace data/daily_food_records.csv with real DailyFoodRecord exports when you have them."""
import os
import joblib
import numpy as np
import pandas as pd
from sklearn.ensemble import GradientBoostingRegressor
from sklearn.metrics import mean_absolute_error
from sklearn.model_selection import train_test_split

FEATURES = ["day_of_week", "month", "event_flag", "prepared_qty"]
os.makedirs("data", exist_ok=True)
CSV = "data/daily_food_records.csv"


def synthesize(n=3000, seed=42):
    rng = np.random.default_rng(seed)
    df = pd.DataFrame({
        "day_of_week": rng.integers(0, 7, n),
        "month": rng.integers(1, 13, n),
        "event_flag": rng.choice([0, 1], n, p=[0.85, 0.15]),
        "prepared_qty": rng.uniform(20, 300, n).round(1),
    })
    rate = 0.08 + 0.05 * (df.day_of_week >= 5) + 0.07 * df.event_flag + 0.03 * df.month.isin([5, 6, 12])
    df["surplus_qty"] = (df.prepared_qty * rate + rng.normal(0, 3, n)).clip(lower=0).round(1)
    return df


def main():
    df = pd.read_csv(CSV) if os.path.exists(CSV) else synthesize()
    df.to_csv(CSV, index=False)
    X, y = df[FEATURES], df["surplus_qty"]
    Xtr, Xte, ytr, yte = train_test_split(X, y, test_size=0.2, random_state=1)
    model = GradientBoostingRegressor(random_state=1).fit(Xtr, ytr)
    print("MAE (kg):", round(mean_absolute_error(yte, model.predict(Xte)), 2))
    joblib.dump(model, "model.joblib")


if __name__ == "__main__":
    main()
