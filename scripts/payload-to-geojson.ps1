param(
  [string]$InputJson = "",
  [string]$OutFile   = "$PSScriptRoot\route-output.geojson"
)

if ([string]::IsNullOrWhiteSpace($InputJson)) {
  $InputJson = Get-Clipboard
  Write-Host "Reading from clipboard..." -ForegroundColor Cyan
}

$parsed = $InputJson | ConvertFrom-Json

if ($parsed.PSObject.Properties.Name -contains "payload") {
  $payload = $parsed.payload
} elseif ($parsed.PSObject.Properties.Name -contains "requestId") {
  $payload = $parsed
} else {
  Write-Host "ERROR: JSON not recognized." -ForegroundColor Red
  exit 1
}

# Helper: formats a double with dot as decimal separator (invariant culture)
function FmtNum([double]$n, [int]$decimals = 6) {
  return $n.ToString("F$decimals", [System.Globalization.CultureInfo]::InvariantCulture)
}

$km  = [math]::Round($payload.totalDistanceKm, 2)
$min = [math]::Round($payload.totalDurationMin, 1)
$segsCount = if ($payload.segments) { $payload.segments.Count } else { 0 }
Write-Host ("Route: {0} km / {1} min / {2} segments" -f $km, $min, $segsCount) -ForegroundColor Green

# Remove return-to-origin duplicate for ROUND_TRIP display
$wps = @($payload.orderedWaypoints)
if ($payload.routeMode -eq "ROUND_TRIP" -and $wps.Count -gt 1) {
  $first = $wps[0]
  $last  = $wps[$wps.Count - 1]
  if ($first.lat -eq $last.lat -and $first.lng -eq $last.lng) {
    $wps = $wps[0..($wps.Count - 2)]
  }
}

$sb = New-Object System.Text.StringBuilder
[void]$sb.Append('{"type":"FeatureCollection","features":[')
$firstFeature = $true

$segments    = $payload.segments
$hasSegments = ($null -ne $segments) -and ($segments.Count -gt 0)

if ($hasSegments) {
  # ── Um LineString colorido por segmento ─────────────────────────────────────
  Write-Host "Segments:" -ForegroundColor Cyan
  foreach ($seg in $segments) {
    $color     = $seg.color
    $segIdx    = $seg.segmentIndex
    $segDistKm = [math]::Round($seg.distanceKm, 2)
    $segDurMin = [math]::Round($seg.durationMin, 1)

    $segCoordParts = @()
    foreach ($pt in $seg.geometry.coordinates) {
      $lng = [double]$pt[0]
      $lat = [double]$pt[1]
      $segCoordParts += ("[" + (FmtNum $lng) + "," + (FmtNum $lat) + "]")
    }
    $segCoordsJson = "[" + ($segCoordParts -join ",") + "]"

    $prefix = if ($firstFeature) { "" } else { "," }
    $firstFeature = $false

    [void]$sb.Append($prefix + '{"type":"Feature",')
    [void]$sb.Append('"geometry":{"type":"LineString","coordinates":')
    [void]$sb.Append($segCoordsJson)
    [void]$sb.Append('},')
    [void]$sb.Append('"properties":{')
    [void]$sb.Append('"name":"Trecho ' + ($segIdx + 1) + '",')
    [void]$sb.Append('"segment_index":' + $segIdx + ',')
    [void]$sb.Append('"distance_km":' + (FmtNum $segDistKm 2) + ',')
    [void]$sb.Append('"duration_min":' + (FmtNum $segDurMin 1) + ',')
    [void]$sb.Append('"stroke":"' + $color + '",')
    [void]$sb.Append('"stroke-width":5,')
    [void]$sb.Append('"stroke-opacity":0.9')
    [void]$sb.Append('}}')

    Write-Host ("  * Trecho {0}: {1} km / {2} min  [{3}]" -f ($segIdx + 1), $segDistKm, $segDurMin, $color) -ForegroundColor White
  }

} else {
  # ── Fallback: geometria completa em cor unica ────────────────────────────────
  Write-Host "(no segments in payload - using full geometry fallback)" -ForegroundColor Yellow
  $coordParts = @()
  foreach ($pt in $payload.geometry.coordinates) {
    $lng = [double]$pt[0]
    $lat = [double]$pt[1]
    $coordParts += ("[" + (FmtNum $lng) + "," + (FmtNum $lat) + "]")
  }
  $coordsJson = "[" + ($coordParts -join ",") + "]"

  $dist = FmtNum $payload.totalDistanceKm 3
  $dur  = FmtNum $payload.totalDurationMin 2
  $mode = $payload.routeMode
  $stat = $payload.osrmValidation.status

  [void]$sb.Append('{"type":"Feature","geometry":{"type":"LineString","coordinates":')
  [void]$sb.Append($coordsJson)
  [void]$sb.Append('},"properties":{"name":"Route","distance_km":')
  [void]$sb.Append($dist)
  [void]$sb.Append(',"duration_min":')
  [void]$sb.Append($dur)
  [void]$sb.Append(',"route_mode":"')
  [void]$sb.Append($mode)
  [void]$sb.Append('","osrm_status":"')
  [void]$sb.Append($stat)
  [void]$sb.Append('","stroke":"#e63946","stroke-width":4}}')
  $firstFeature = $false
}

# ── Features: Waypoint pins ──────────────────────────────────────────────────
$pinColors = @("#e63946","#457b9d","#2a9d8f","#e9c46a","#f4a261","#264653","#b07aa1","#ff9da7","#9c755f","#bab0ac")

for ($i = 0; $i -lt $wps.Count; $i++) {
  $wp    = $wps[$i]
  $color = $pinColors[$i % $pinColors.Count]
  $lng   = FmtNum ([double]$wp.lng)
  $lat   = FmtNum ([double]$wp.lat)

  [void]$sb.Append(',{"type":"Feature","geometry":{"type":"Point","coordinates":[')
  [void]$sb.Append($lng)
  [void]$sb.Append(',')
  [void]$sb.Append($lat)
  [void]$sb.Append(']},"properties":{"name":"Stop ')
  [void]$sb.Append($i)
  [void]$sb.Append('","sequence_index":')
  [void]$sb.Append($wp.sequenceIndex)
  [void]$sb.Append(',"marker-color":"')
  [void]$sb.Append($color)
  [void]$sb.Append('","marker-size":"large"}}')
}

[void]$sb.Append(']}')

$geoJson = $sb.ToString()

$geoJson | Out-File -FilePath $OutFile -Encoding UTF8 -NoNewline
$geoJson | Set-Clipboard

Write-Host "Saved: $OutFile" -ForegroundColor Green
Write-Host "Copied to clipboard - paste it on geojson.io!" -ForegroundColor Cyan
Start-Process "https://geojson.io"
