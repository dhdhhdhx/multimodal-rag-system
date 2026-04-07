# Python示例代码 - 模块39
import numpy as np
import pandas as pd
from sklearn.model_selection import train_test_split

class DataProcessor:
    def __init__(self, data_path):
        self.data = pd.read_csv(data_path)
    
    def preprocess(self):
        # 数据预处理
        self.data = self.data.dropna()
        return self.data
    
    def train_model(self):
        X = self.data.drop('target', axis=1)
        y = self.data['target']
        X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2)
        return X_train, X_test, y_train, y_test

if __name__ == '__main__':
    processor = DataProcessor('data.csv')
    processor.preprocess()
    print("数据处理完成")
