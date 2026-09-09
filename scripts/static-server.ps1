param(
    [string]$Root = (Split-Path -Parent $PSScriptRoot),
    [int]$Port = 4173
)

$rootPath = [IO.Path]::GetFullPath($Root).TrimEnd([IO.Path]::DirectorySeparatorChar) + [IO.Path]::DirectorySeparatorChar
$tcp = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, $Port)
$tcp.Start()

$contentTypes = @{
    '.html' = 'text/html; charset=utf-8'
    '.css'  = 'text/css; charset=utf-8'
    '.js'   = 'application/javascript; charset=utf-8'
    '.json' = 'application/json; charset=utf-8'
    '.svg'  = 'image/svg+xml'
    '.png'  = 'image/png'
    '.jpg'  = 'image/jpeg'
    '.jpeg' = 'image/jpeg'
    '.webp' = 'image/webp'
    '.ico'  = 'image/x-icon'
}

function Send-Response($stream, [int]$status, [string]$contentType, [byte[]]$body) {
    $reason = if ($status -eq 200) { 'OK' } elseif ($status -eq 404) { 'Not Found' } else { 'Internal Server Error' }
    $header = "HTTP/1.1 $status $reason`r`nContent-Type: $contentType`r`nContent-Length: $($body.Length)`r`nConnection: close`r`n`r`n"
    $headerBytes = [Text.Encoding]::ASCII.GetBytes($header)
    $stream.Write($headerBytes, 0, $headerBytes.Length)
    if ($body.Length -gt 0) { $stream.Write($body, 0, $body.Length) }
    $stream.Flush()
}

try {
    while ($true) {
        $client = $tcp.AcceptTcpClient()
        $stream = $null
        $reader = $null
        try {
            $stream = $client.GetStream()
            $reader = [IO.StreamReader]::new($stream, [Text.Encoding]::ASCII, $false, 4096, $true)
            $requestLine = $reader.ReadLine()
            while (($line = $reader.ReadLine()) -ne $null -and $line.Length -gt 0) { }
            if (-not $requestLine) { continue }
            $parts = $requestLine.Split(' ')
            if ($parts.Length -lt 2 -or $parts[0] -ne 'GET') {
                Send-Response $stream 404 'text/plain; charset=utf-8' ([Text.Encoding]::UTF8.GetBytes('Not Found'))
                continue
            }
            $relative = [Uri]::UnescapeDataString(($parts[1] -split '\?')[0].TrimStart('/'))
            if ([string]::IsNullOrWhiteSpace($relative)) { $relative = 'index.html' }
            $candidate = [IO.Path]::GetFullPath((Join-Path $rootPath $relative))
            if (-not $candidate.StartsWith($rootPath, [StringComparison]::OrdinalIgnoreCase) -or -not [IO.File]::Exists($candidate)) {
                Send-Response $stream 404 'text/plain; charset=utf-8' ([Text.Encoding]::UTF8.GetBytes('Not Found'))
                continue
            }
            $extension = [IO.Path]::GetExtension($candidate).ToLowerInvariant()
            $contentType = if ($contentTypes.ContainsKey($extension)) { $contentTypes[$extension] } else { 'application/octet-stream' }
            Send-Response $stream 200 $contentType ([IO.File]::ReadAllBytes($candidate))
        } catch {
            try { Send-Response $stream 500 'text/plain; charset=utf-8' ([Text.Encoding]::UTF8.GetBytes('Internal Server Error')) } catch { }
        } finally {
            if ($reader) { $reader.Dispose() }
            if ($stream) { $stream.Dispose() }
            $client.Dispose()
        }
    }
} finally {
    $tcp.Stop()
}
