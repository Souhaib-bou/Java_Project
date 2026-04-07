<?php

namespace App\Onboarding;

use Symfony\Component\HttpFoundation\Request;

final class PublicUrlConfiguration
{
    private const AUTO_VALUE = 'auto';

    public function __construct(private readonly ?string $publicBaseUrl)
    {
    }

    public function resolveBaseUrl(Request $request): string
    {
        $configuredBaseUrl = $this->resolveConfiguredBaseUrl();
        if (null !== $configuredBaseUrl) {
            return $configuredBaseUrl;
        }

        $requestHost = (string) $request->getHost();
        if ($this->isReachableHost($requestHost)) {
            return rtrim($request->getSchemeAndHttpHost(), '/');
        }

        $detectedHostname = $this->detectStableHostName();
        if (null !== $detectedHostname) {
            return $this->buildBaseUrl($request, $detectedHostname);
        }

        $detectedHost = $this->detectLanHost();
        if (null === $detectedHost) {
            return rtrim($request->getSchemeAndHttpHost(), '/');
        }

        return $this->buildBaseUrl($request, $detectedHost);
    }

    public function isUsingConfiguredPublicBaseUrl(): bool
    {
        return null !== $this->resolveConfiguredBaseUrl();
    }

    private function buildBaseUrl(Request $request, string $host): string
    {
        $scheme = $request->isSecure() ? 'https' : 'http';
        $port = $request->getPort();
        $portSuffix = $this->shouldAppendPort($scheme, $port) ? ':' . $port : '';

        return sprintf('%s://%s%s', $scheme, $host, $portSuffix);
    }

    private function resolveConfiguredBaseUrl(): ?string
    {
        $configuredBaseUrl = trim((string) $this->publicBaseUrl);
        if ('' === $configuredBaseUrl || self::AUTO_VALUE === strtolower($configuredBaseUrl)) {
            return null;
        }

        if (!preg_match('#^https?://#i', $configuredBaseUrl)) {
            $configuredBaseUrl = 'https://' . ltrim($configuredBaseUrl, '/');
        }

        return rtrim($configuredBaseUrl, '/');
    }

    private function detectStableHostName(): ?string
    {
        $hostname = trim((string) gethostname());
        if (!$this->isReachableHost($hostname)) {
            return null;
        }

        return $hostname;
    }

    private function detectLanHost(): ?string
    {
        $gatewayCandidates = $this->detectGatewayInterfaceAddresses();
        if ([] !== $gatewayCandidates) {
            usort($gatewayCandidates, fn (array $left, array $right): int => $right['score'] <=> $left['score']);

            return $gatewayCandidates[0]['ip'];
        }

        $hostnameCandidates = array_filter(
            gethostbynamel(gethostname()) ?: [],
            fn (string $ip): bool => $this->isUsableIpv4($ip)
        );

        if ([] === $hostnameCandidates) {
            return null;
        }

        usort($hostnameCandidates, fn (string $left, string $right): int => $this->scoreIp($right) <=> $this->scoreIp($left));

        return $hostnameCandidates[0];
    }

    /**
     * @return list<array{ip: string, score: int}>
     */
    private function detectGatewayInterfaceAddresses(): array
    {
        if ('Windows' !== PHP_OS_FAMILY) {
            return [];
        }

        $script = <<<'POWERSHELL'
Get-NetIPConfiguration | Where-Object { $_.IPv4DefaultGateway -ne $null -and $_.IPv4Address -ne $null } | ForEach-Object { [PSCustomObject]@{ InterfaceAlias=$_.InterfaceAlias; IPv4Address=$_.IPv4Address.IPAddress } } | ConvertTo-Json -Compress
POWERSHELL;

        $output = shell_exec('powershell -NoProfile -Command ' . escapeshellarg($script));
        if (!is_string($output) || '' === trim($output)) {
            return [];
        }

        $decoded = json_decode($output, true);
        if (!is_array($decoded)) {
            return [];
        }

        $records = isset($decoded['IPv4Address']) ? [$decoded] : $decoded;
        $candidates = [];

        foreach ($records as $record) {
            if (!is_array($record)) {
                continue;
            }

            $ip = trim((string) ($record['IPv4Address'] ?? ''));
            if (!$this->isUsableIpv4($ip)) {
                continue;
            }

            $interfaceAlias = trim((string) ($record['InterfaceAlias'] ?? ''));
            $candidates[] = [
                'ip' => $ip,
                'score' => $this->scoreInterfaceAlias($interfaceAlias) + $this->scoreIp($ip),
            ];
        }

        return $candidates;
    }

    private function isReachableHost(string $host): bool
    {
        $normalizedHost = strtolower(trim($host));

        if ('' === $normalizedHost) {
            return false;
        }

        if (in_array($normalizedHost, ['localhost', '127.0.0.1', '::1'], true)) {
            return false;
        }

        if (str_starts_with($normalizedHost, '127.')) {
            return false;
        }

        return true;
    }

    private function isUsableIpv4(string $ip): bool
    {
        if (!filter_var($ip, FILTER_VALIDATE_IP, FILTER_FLAG_IPV4)) {
            return false;
        }

        if (str_starts_with($ip, '127.') || str_starts_with($ip, '169.254.')) {
            return false;
        }

        return true;
    }

    private function shouldAppendPort(string $scheme, int $port): bool
    {
        return !('http' === $scheme && 80 === $port || 'https' === $scheme && 443 === $port);
    }

    private function scoreInterfaceAlias(string $interfaceAlias): int
    {
        $normalizedAlias = strtolower($interfaceAlias);

        if (preg_match('/wi-?fi|wlan|wireless/', $normalizedAlias)) {
            return 120;
        }

        if (preg_match('/ethernet|lan/', $normalizedAlias)) {
            return 100;
        }

        if (preg_match('/vmware|virtual|hyper-v|vbox|virtualbox|docker|wsl|loopback|bluetooth|vpn|tailscale/', $normalizedAlias)) {
            return -200;
        }

        return 10;
    }

    private function scoreIp(string $ip): int
    {
        if (preg_match('/^192\.168\./', $ip)) {
            return 80;
        }

        if (preg_match('/^10\./', $ip)) {
            return 70;
        }

        if (preg_match('/^172\.(1[6-9]|2\d|3[0-1])\./', $ip)) {
            return 60;
        }

        return 0;
    }
}
