# Mo cong 3000 (Back-end) va 5173 (web) cho dien thoai trong cung Wi-Fi/LAN.
# - Chi ap dung cho mang "Private" va chi cho cac may trong cung mang con (LocalSubnet), khong mo ra Internet.
# - Can quyen Administrator: chuot phai vao PowerShell -> "Run as administrator", roi chay:
#       cd <thu muc du an>
#       powershell -ExecutionPolicy Bypass -File .\mo-firewall-lan.ps1
# - Go bo sau khi demo xong:
#       Remove-NetFirewallRule -DisplayName 'Thuyet minh - Backend 3000','Thuyet minh - Web Vite 5173'
#Requires -RunAsAdministrator

$rules = @(
    @{ Name = 'Thuyet minh - Backend 3000';  Port = 3000 },
    @{ Name = 'Thuyet minh - Web Vite 5173'; Port = 5173 }
)

foreach ($r in $rules) {
    if (Get-NetFirewallRule -DisplayName $r.Name -ErrorAction SilentlyContinue) {
        Write-Host "Da co quy tac: $($r.Name)"
        continue
    }
    New-NetFirewallRule -DisplayName $r.Name -Direction Inbound -Action Allow -Protocol TCP `
        -LocalPort $r.Port -Profile Private -RemoteAddress LocalSubnet | Out-Null
    Write-Host "Da tao quy tac: $($r.Name) (TCP $($r.Port), mang Private, chi mang con noi bo)"
}

Write-Host ''
Write-Host 'Xong. Neu Wi-Fi dang o che do Public, doi sang Private: Settings -> Network & internet -> Wi-Fi -> (ten mang) -> Network profile type = Private.'
