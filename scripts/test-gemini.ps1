param(
  [string]$Model = "gemini-2.5-flash",
  [string]$Prompt = "Say hello in one short sentence."
)

$ErrorActionPreference = "Stop"

function Get-GeminiKey {
  if ($env:GEMINI_API_KEY -and $env:GEMINI_API_KEY.Trim().Length -gt 0) {
    return $env:GEMINI_API_KEY.Trim()
  }
  throw "GEMINI_API_KEY is not set. Set it in your shell environment and re-run."
}

$key = Get-GeminiKey
$uri = "https://generativelanguage.googleapis.com/v1beta/models/$Model`:generateContent?key=$key"

$body = @{
  contents = @(
    @{
      role  = "user"
      parts = @(@{ text = $Prompt })
    }
  )
} | ConvertTo-Json -Depth 8

try {
  $resp = Invoke-RestMethod -Method Post -Uri $uri -ContentType "application/json" -Body $body -TimeoutSec 45
  $text = $resp.candidates[0].content.parts[0].text
  if (-not $text) { $text = "" }
  $preview = $text.Trim()
  if ($preview.Length -gt 240) { $preview = $preview.Substring(0, 240) + "..." }
  Write-Host "OK ($Model)"
  Write-Host "Reply preview: $preview"
  exit 0
} catch {
  $ex = $_.Exception
  $status = $null
  if ($ex.Response -and $ex.Response.StatusCode) { $status = [int]$ex.Response.StatusCode }

  Write-Host "FAILED ($Model) HTTP $status"
  if ($ex.Response) {
    try {
      $stream = $ex.Response.GetResponseStream()
      $reader = New-Object System.IO.StreamReader($stream)
      $raw = $reader.ReadToEnd()
      if ($raw) {
        $rawTrim = $raw.Trim()
        if ($rawTrim.Length -gt 1000) { $rawTrim = $rawTrim.Substring(0, 1000) + "..." }
        Write-Host "Error body (truncated):"
        Write-Host $rawTrim
      }
    } catch {
      # ignore
    }
  }
  exit 1
}

