# Script para redimensionar el logo a todos los tamaños necesarios para Android
Add-Type -AssemblyName System.Drawing

$sourcePath = "movier-logo.png"
$sourceImage = [System.Drawing.Image]::FromFile((Resolve-Path $sourcePath))

# Función para redimensionar imagen con zoom óptimo
function Resize-Image {
    param($image, $width, $height, $outputPath)
    
    # Zoom moderado (160%) para mostrar bien el dado
    $targetSize = [int]($width * 1.6)
    $offset = [int](($targetSize - $width) / 2)
    
    $newImage = New-Object System.Drawing.Bitmap $width, $height
    $graphics = [System.Drawing.Graphics]::FromImage($newImage)
    $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $graphics.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
    
    # Fondo negro sólido
    $backgroundColor = [System.Drawing.Color]::FromArgb(28, 28, 30)
    $graphics.Clear($backgroundColor)
    
    # Dibujar la imagen centrada con zoom moderado
    $graphics.DrawImage($image, -$offset, -$offset, $targetSize, $targetSize)
    
    $newImage.Save($outputPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $graphics.Dispose()
    $newImage.Dispose()
    
    Write-Host "Creado: $outputPath"
}

# Crear carpetas si no existen
$folders = @(
    "app\src\main\res\mipmap-mdpi",
    "app\src\main\res\mipmap-hdpi",
    "app\src\main\res\mipmap-xhdpi",
    "app\src\main\res\mipmap-xxhdpi",
    "app\src\main\res\mipmap-xxxhdpi"
)

foreach ($folder in $folders) {
    if (-not (Test-Path $folder)) {
        New-Item -ItemType Directory -Path $folder -Force | Out-Null
    }
}

# Generar todos los tamaños
Resize-Image $sourceImage 48 48 "app\src\main\res\mipmap-mdpi\ic_launcher.png"
Resize-Image $sourceImage 72 72 "app\src\main\res\mipmap-hdpi\ic_launcher.png"
Resize-Image $sourceImage 96 96 "app\src\main\res\mipmap-xhdpi\ic_launcher.png"
Resize-Image $sourceImage 144 144 "app\src\main\res\mipmap-xxhdpi\ic_launcher.png"
Resize-Image $sourceImage 192 192 "app\src\main\res\mipmap-xxxhdpi\ic_launcher.png"

# Para iconos round (redondos)
Resize-Image $sourceImage 48 48 "app\src\main\res\mipmap-mdpi\ic_launcher_round.png"
Resize-Image $sourceImage 72 72 "app\src\main\res\mipmap-hdpi\ic_launcher_round.png"
Resize-Image $sourceImage 96 96 "app\src\main\res\mipmap-xhdpi\ic_launcher_round.png"
Resize-Image $sourceImage 144 144 "app\src\main\res\mipmap-xxhdpi\ic_launcher_round.png"
Resize-Image $sourceImage 192 192 "app\src\main\res\mipmap-xxxhdpi\ic_launcher_round.png"

$sourceImage.Dispose()

Write-Host ""
Write-Host "¡Iconos creados exitosamente!" -ForegroundColor Green
