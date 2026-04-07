// JavaScript示例代码 - 模块7
class APIClient {
    constructor(baseURL) {
        this.baseURL = baseURL;
        this.headers = {
            'Content-Type': 'application/json'
        };
    }
    
    async fetchData(endpoint) {
        try {
            const response = await fetch(`${this.baseURL}/${endpoint}`, {
                headers: this.headers
            });
            return await response.json();
        } catch (error) {
            console.error('API请求失败:', error);
            throw error;
        }
    }
    
    async postData(endpoint, data) {
        const response = await fetch(`${this.baseURL}/${endpoint}`, {
            method: 'POST',
            headers: this.headers,
            body: JSON.stringify(data)
        });
        return await response.json();
    }
}

export default APIClient;
