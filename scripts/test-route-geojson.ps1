# =============================================================================
#  RouteWise - Test & GeoJSON Exporter
#
#  Fluxo completo:
#    1. POST  /api/routes/compute  -> envia os waypoints, recebe requestId
#    2. GET   /api/routes/events/{requestId}  -> aguarda o SSE COMPLETED
#    3. Converte o payload para GeoJSON FeatureCollection
#       - Se o payload tiver "segments[]": um LineString colorido por trecho
#       - Caso contrario: fallback para geometria completa em cor unica
#    4. Salva em arquivo, copia para clipboard e abre geojson.io
#
#  Uso:
#    .\test-route-geojson.ps1                      # defaults (localhost, ROUND_TRIP)
#    .\test-route-geojson.ps1 -RouteMode ONE_WAY
#    .\test-route-geojson.ps1 -BaseUrl "http://prod.api.com"
# =============================================================================

param(
  [string]$BaseUrl   = "http://localhost:8080",
  [string]$RouteMode = "ROUND_TRIP",
  [string]$OutFile   = "$PSScriptRoot\route-output.geojson",
  [int]   $TimeoutSec = 60
)

# =============================================================================
#  WAYPOINTS DE TESTE — altere os pontos aqui conforme necessario
# =============================================================================
$testWaypoints = @(
  @{ lat = -23.5505; lng = -46.6333 }   # Praca da Se
  @{ lat = -23.5629; lng = -46.6544 }   # Pinheiros
  @{ lat = -23.5489; lng = -46.6388 }   # Consolacao
)

# =============================================================================
#  HELPERS
# =============================================================================

# Formata numero com ponto como separador decimal (invariant culture)
# Necessario porque pt_BR usa virgula, o que quebra o JSON
function FmtNum([double]$n, [int]$decimals = 6) {
  return $n.ToString("F$decimals", [System.Globalization.CultureInfo]::InvariantCulture)
}

# Converte o payload retornado pela API em uma string GeoJSON valida.
# Se o payload conter "segments[]", emite uma LineString por segmento
# (cada uma com a cor dinamica atribuida pelo backend).
# Caso contrario, faz fallback para a geometria completa em cor unica.
function ConvertTo-GeoJson($payload) {
  $sb = New-Object System.Text.StringBuilder

  $dist = FmtNum $payload.totalDistanceKm 3
  $dur  = FmtNum $payload.totalDurationMin 2
  $mode = $payload.routeMode
  $stat = $payload.osrmValidation.status

  # Remove o waypoint de retorno duplicado no ROUND_TRIP
  # (o A* repete a origem no final para fechar o loop — nao precisa exibir 2x)
  $wps = @($payload.orderedWaypoints)
  if ($mode -eq "ROUND_TRIP" -and $wps.Count -gt 1) {
    $first = $wps[0]; $last = $wps[$wps.Count - 1]
    if ($first.lat -eq $last.lat -and $first.lng -eq $last.lng) {
      $wps = $wps[0..($wps.Count - 2)]
    }
  }

  [void]$sb.Append('{"type":"FeatureCollection","features":[')
  $firstFeature = $true

  $segments = $payload.segments
  $hasSegments = ($null -ne $segments) -and ($segments.Count -gt 0)

  if ($hasSegments) {
    # ── Um LineString colorido por segmento ───────────────────────────────────
    Write-Host "      Segmentos:" -ForegroundColor Cyan
    foreach ($seg in $segments) {
      $color     = $seg.color
      $segIdx    = $seg.segmentIndex
      $segDistKm = [math]::Round($seg.distanceKm, 2)
      $segDurMin = [math]::Round($seg.durationMin, 1)

      # Monta coordenadas do segmento
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

      Write-Host ("        * Trecho {0}: {1} km / {2} min  [{3}]" -f ($segIdx + 1), $segDistKm, $segDurMin, $color) -ForegroundColor White
    }

  } else {
    # ── Fallback: geometria completa em cor unica ──────────────────────────────
    Write-Host "      (sem segmentos no payload - usando geometria completa)" -ForegroundColor Yellow
    $coordParts = @()
    foreach ($pt in $payload.geometry.coordinates) {
      $lng = [double]$pt[0]
      $lat = [double]$pt[1]
      $coordParts += ("[" + (FmtNum $lng) + "," + (FmtNum $lat) + "]")
    }
    $coordsJson = "[" + ($coordParts -join ",") + "]"

    [void]$sb.Append('{"type":"Feature",')
    [void]$sb.Append('"geometry":{"type":"LineString","coordinates":')
    [void]$sb.Append($coordsJson)
    [void]$sb.Append('},')
    [void]$sb.Append('"properties":{')
    [void]$sb.Append('"name":"Rota Otimizada",')
    [void]$sb.Append('"distance_km":' + $dist + ',')
    [void]$sb.Append('"duration_min":' + $dur + ',')
    [void]$sb.Append('"route_mode":"' + $mode + '",')
    [void]$sb.Append('"osrm_status":"' + $stat + '",')
    [void]$sb.Append('"stroke":"#e63946","stroke-width":4')
    [void]$sb.Append('}}')
    $firstFeature = $false
  }

  # ── Features: Points (pins dos waypoints otimizados pelo A*) ──────────────
  $pinColors = @("#e63946","#457b9d","#2a9d8f","#e9c46a","#f4a261","#264653","#b07aa1","#ff9da7","#9c755f","#bab0ac")

  for ($i = 0; $i -lt $wps.Count; $i++) {
    $wp    = $wps[$i]
    $color = $pinColors[$i % $pinColors.Count]
    $lng   = FmtNum ([double]$wp.lng)
    $lat   = FmtNum ([double]$wp.lat)

    [void]$sb.Append(',{"type":"Feature",')
    [void]$sb.Append('"geometry":{"type":"Point","coordinates":[')
    [void]$sb.Append($lng + "," + $lat)
    [void]$sb.Append(']},')
    [void]$sb.Append('"properties":{')
    [void]$sb.Append('"name":"Parada #' + $i + '",')
    [void]$sb.Append('"sequence_index":' + $wp.sequenceIndex + ',')
    [void]$sb.Append('"marker-color":"' + $color + '",')
    [void]$sb.Append('"marker-size":"large",')
    [void]$sb.Append('"marker-symbol":"' + $i + '"')
    [void]$sb.Append('}}')
  }

  [void]$sb.Append(']}')
  return $sb.ToString()
}

# =============================================================================
#  STEP 1 — POST /api/routes/compute
# =============================================================================
Write-Host ""
Write-Host "[ 1/3 ] Enviando requisicao para a API..." -ForegroundColor Cyan

$body = @{
  routeMode = $RouteMode
  waypoints = $testWaypoints
} | ConvertTo-Json -Depth 5

try {
  $computeResponse = Invoke-RestMethod `
    -Uri         "$BaseUrl/api/routes/compute" `
    -Method      POST `
    -Body        $body `
    -ContentType "application/json"
} catch {
  Write-Host "ERRO no POST /compute: $_" -ForegroundColor Red
  exit 1
}

$requestId = $computeResponse.requestId
Write-Host "      requestId: $requestId" -ForegroundColor Green

# =============================================================================
#  STEP 2 — GET /api/routes/events/{requestId}  (aguarda o SSE fechar)
# =============================================================================
Write-Host "[ 2/3 ] Aguardando resultado via SSE (timeout: ${TimeoutSec}s)..." -ForegroundColor Yellow

$sseUrl  = "$BaseUrl/api/routes/events/$requestId"
$payload = $null

try {
  # Invoke-WebRequest espera o stream fechar; o backend faz isso apos COMPLETED
  $response = Invoke-WebRequest -Uri $sseUrl -Method GET -TimeoutSec $TimeoutSec

  # O corpo SSE tem o formato de texto:
  #   event:PROCESSING
  #   data:{"type":"PROCESSING",...}
  #
  #   event:COMPLETED
  #   data:{"type":"COMPLETED","payload":{...}}
  #
  # Percorremos linha por linha e extraimos o bloco "data:" do COMPLETED
  $lines = $response.Content -split "`n"
  foreach ($line in $lines) {
    if ($line -match '^data:(.+)$') {
      $evt = $Matches[1] | ConvertFrom-Json
      if ($evt.type -eq "COMPLETED") { $payload = $evt.payload; break }
      if ($evt.type -eq "ERROR") {
        Write-Host "ERRO retornado pela API: $($evt.payload)" -ForegroundColor Red
        exit 1
      }
    }
  }
} catch {
  Write-Host "ERRO ao conectar no SSE: $_" -ForegroundColor Red
  exit 1
}

if ($null -eq $payload) {
  Write-Host "Evento COMPLETED nao encontrado na resposta SSE." -ForegroundColor Red
  exit 1
}

$km  = [math]::Round($payload.totalDistanceKm, 2)
$min = [math]::Round($payload.totalDurationMin, 1)
$pts = $payload.geometry.coordinates.Count
$segsCount = if ($payload.segments) { $payload.segments.Count } else { 0 }
Write-Host "      Rota: ${km} km / ${min} min / ${pts} pontos de geometria / ${segsCount} segmentos" -ForegroundColor Green

# =============================================================================
#  STEP 3 — Converte para GeoJSON e exporta
# =============================================================================
Write-Host "[ 3/3 ] Montando GeoJSON e exportando..." -ForegroundColor Cyan

$geoJson = ConvertTo-GeoJson $payload

$geoJson | Out-File -FilePath $OutFile -Encoding UTF8 -NoNewline
$geoJson | Set-Clipboard

Write-Host ""
Write-Host "Arquivo salvo : $OutFile" -ForegroundColor Green
Write-Host "Copiado no clipboard. Abra geojson.io, clique em 'New' e cole o JSON." -ForegroundColor Cyan
Write-Host ""

Start-Process "https://geojson.io"
