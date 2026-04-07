# Simple Load Test Runner
Write-Host "Starting Simple Load Test..." -ForegroundColor Green

# Configuration
$jmeterDir = "D:\gemini\apache-jmeter-5.6.3"
$resultsDir = "D:\gemini\multimodal-rag-system\results"
$testPlan = "D:\gemini\multimodal-rag-system\scripts\simple_load_test.jmx"

# Create results directory
if (!(Test-Path $resultsDir)) {
    New-Item -ItemType Directory -Path $resultsDir -Force | Out-Null
}

# Run load test
Write-Host ""
Write-Host "Running: 300 concurrent users, 10 loops each" -ForegroundColor Cyan
Write-Host "Target: Frontend homepage (no auth required)" -ForegroundColor Cyan
Write-Host ""

$jmeterBin = "$jmeterDir\bin\jmeter.bat"
$resultFile = "$resultsDir\simple_results.jtl"
$htmlReport = "$resultsDir\simple_report"

# Remove old results
if (Test-Path $htmlReport) {
    Remove-Item -Path $htmlReport -Recurse -Force
}
if (Test-Path $resultFile) {
    Remove-Item -Path $resultFile -Force
}

# Execute test
& $jmeterBin -n -t $testPlan -l $resultFile -e -o $htmlReport

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "Load test completed!" -ForegroundColor Green
    Write-Host "Report: $htmlReport\index.html" -ForegroundColor Green
    Start-Process "$htmlReport\index.html"
} else {
    Write-Host ""
    Write-Host "Test failed!" -ForegroundColor Red
}
