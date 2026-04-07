# JMeter Load Test Script
Write-Host "Starting JMeter Load Test..." -ForegroundColor Green

# Configuration
$jmeterZip = "D:\gemini\apache-jmeter-5.6.3.zip"
$jmeterDir = "D:\gemini\apache-jmeter-5.6.3"
$resultsDir = "D:\gemini\multimodal-rag-system\results"
$testPlan = "D:\gemini\multimodal-rag-system\scripts\load_test.jmx"

# Check JMeter download
if (!(Test-Path $jmeterZip)) {
    Write-Host "Downloading JMeter..." -ForegroundColor Yellow
    Invoke-WebRequest -Uri "https://dlcdn.apache.org//jmeter/binaries/apache-jmeter-5.6.3.zip" -OutFile $jmeterZip
    Write-Host "Download complete" -ForegroundColor Green
}

# Extract JMeter
if (!(Test-Path $jmeterDir)) {
    Write-Host "Extracting JMeter..." -ForegroundColor Yellow
    Expand-Archive -Path $jmeterZip -DestinationPath "D:\gemini\" -Force
    Write-Host "Extraction complete" -ForegroundColor Green
}

# Create results directory
if (!(Test-Path $resultsDir)) {
    New-Item -ItemType Directory -Path $resultsDir -Force | Out-Null
}

# Run load test
Write-Host ""
Write-Host "Starting load test: 300 concurrent users, 5 minutes..." -ForegroundColor Cyan
Write-Host "Test scenarios: Document list + RAG Q&A" -ForegroundColor Cyan

$jmeterBin = "$jmeterDir\bin\jmeter.bat"
$resultFile = "$resultsDir\test_results.jtl"
$htmlReport = "$resultsDir\html_report"

# Remove old results
if (Test-Path $htmlReport) {
    Remove-Item -Path $htmlReport -Recurse -Force
}

# Execute test
& $jmeterBin -n -t $testPlan -l $resultFile -e -o $htmlReport

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "Load test completed successfully!" -ForegroundColor Green
    Write-Host "HTML report generated: $htmlReport\index.html" -ForegroundColor Green
    Write-Host ""
    Write-Host "Opening test report..." -ForegroundColor Cyan
    Start-Process "$htmlReport\index.html"
} else {
    Write-Host ""
    Write-Host "Load test failed!" -ForegroundColor Red
    Write-Host "Please check if backend is running at http://localhost:8080" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "======================================" -ForegroundColor Cyan
Write-Host "Test Summary" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan
Write-Host "Test data: 190+ files"
Write-Host "Concurrent users: 300"
Write-Host "Test duration: 5 minutes"
Write-Host "Report: $htmlReport\index.html"
Write-Host "======================================" -ForegroundColor Cyan
