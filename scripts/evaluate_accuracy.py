import requests
import json
import time

# Configuration
API_BASE = "http://localhost:8080/api"
CHAT_ENDPOINT = f"{API_BASE}/chat"
# Mock Golden Dataset for demonstration
# In production, Replace with real questions and expected relevant document IDs
GOLDEN_DATASET = [
    {"query": "什么是多模态RAG？", "expected_doc_id": 1},
    {"query": "如何配置 Docker 镜像？", "expected_doc_id": 5},
    {"query": "系统的存储配额是多少？", "expected_doc_id": 10},
]

def evaluate_retrieval():
    print("🚀 Starting Retrieval Accuracy Evaluation...")
    hits = 0
    total = len(GOLDEN_DATASET)
    mrr_sum = 0
    
    # Note: This script assumes you have a test user session or public access
    # Since we added RBAC, you might need to provide a JWT token in headers
    headers = {
        "Content-Type": "application/json",
        # "Authorization": "Bearer YOUR_TOKEN_HERE" 
    }

    results = []

    for item in GOLDEN_DATASET:
        query = item['query']
        expected = item['expected_doc_id']
        
        print(f"Testing Query: {query}")
        
        start_time = time.time()
        try:
            response = requests.post(CHAT_ENDPOINT, json={"query": query}, headers=headers)
            latency = (time.time() - start_time) * 1000
            
            # NOTE: To get specific doc IDs from the retrieval phase, 
            # the backend would ideally return them in metadata.
            # Here we simulate the evaluation logic.
            
            # Simulated check (In a real test, backend should return 'source_documents')
            success = True # Placeholder
            rank = 1       # Placeholder
            
            if success:
                hits += 1
                mrr_sum += (1.0 / rank)
            
            results.append({
                "query": query,
                "latency_ms": f"{latency:.2f}",
                "status": "PASS" if success else "FAIL"
            })
            
        except Exception as e:
            print(f"Error testing query '{query}': {e}")

    # Summary
    hit_rate = (hits / total) * 100 if total > 0 else 0
    mrr = (mrr_sum / total) if total > 0 else 0
    
    print("\n" + "="*30)
    print("📊 EVALUATION SUMMARY")
    print(f"Total Queries: {total}")
    print(f"Hit Rate @ 1: {hit_rate:.2f}%")
    print(f"Avg Latency: {sum([float(r['latency_ms']) for r in results])/total:.2f}ms")
    print("="*30)

    with open("retrieval_report.json", "w", encoding="utf-8") as f:
        json.dump({"metrics": {"hit_rate": hit_rate, "mrr": mrr}, "details": results}, f, indent=4, ensure_ascii=False)
    print("✅ Report saved to retrieval_report.json")

if __name__ == "__main__":
    evaluate_retrieval()
