using System.Net.Http.Json;
using System.Text.Json;
using Microsoft.Extensions.Options;
using StackExchange.Redis;

namespace DotnetGuardian;

public sealed class HealthMonitorWorker : BackgroundService
{
    private readonly ILogger<HealthMonitorWorker> _logger;
    private readonly IHttpClientFactory _httpClientFactory;
    private readonly TelemetryState _telemetryState;
    private readonly GuardianOptions _options;
    private readonly IConnectionMultiplexer _redis;
    private bool _rollbackTriggeredForCurrentFailureWindow;

    public HealthMonitorWorker(
        ILogger<HealthMonitorWorker> logger,
        IHttpClientFactory httpClientFactory,
        TelemetryState telemetryState,
        IOptions<GuardianOptions> options,
        IConnectionMultiplexer redis)
    {
        _logger = logger;
        _httpClientFactory = httpClientFactory;
        _telemetryState = telemetryState;
        _options = options.Value;
        _redis = redis;
    }

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        _logger.LogInformation(
            "Guardian started for flag {FlagKey} with probe interval {ProbeIntervalSeconds}s",
            _options.FlagKey,
            _options.ProbeIntervalSeconds);

        await ProbeTargetsAsync(stoppingToken);

        using var timer = new PeriodicTimer(TimeSpan.FromSeconds(_options.ProbeIntervalSeconds));
        while (await timer.WaitForNextTickAsync(stoppingToken))
        {
            await ProbeTargetsAsync(stoppingToken);
        }
    }

    private async Task ProbeTargetsAsync(CancellationToken stoppingToken)
    {
        var stableProbe = await ProbeAsync("stable", _options.StableHealthUrl, stoppingToken);
        _telemetryState.RecordStableProbe(stableProbe.StatusCode, stableProbe.Healthy, stableProbe.Details);

        var canaryProbe = await ProbeAsync("canary", _options.CanaryHealthUrl, stoppingToken);
        var consecutiveFailures = _telemetryState.RecordCanaryProbe(
            canaryProbe.StatusCode,
            canaryProbe.Healthy,
            canaryProbe.Details);

        _logger.LogInformation(
            "Probe summary stable={StableStatusCode} canary={CanaryStatusCode} consecutiveCanaryFailures={Failures}",
            stableProbe.StatusCode,
            canaryProbe.StatusCode,
            consecutiveFailures);

        if (canaryProbe.StatusCode < 500)
        {
            if (_rollbackTriggeredForCurrentFailureWindow)
            {
                _logger.LogInformation("Canary recovered. Guardian re-armed for future rollbacks.");
            }

            _rollbackTriggeredForCurrentFailureWindow = false;
            return;
        }

        if (consecutiveFailures < _options.CanaryFailureThreshold || _rollbackTriggeredForCurrentFailureWindow)
        {
            return;
        }

        try
        {
            await TriggerRollbackAsync(consecutiveFailures, stoppingToken);
            _rollbackTriggeredForCurrentFailureWindow = true;
        }
        catch (Exception exception)
        {
            _rollbackTriggeredForCurrentFailureWindow = false;
            _telemetryState.RecordRollback(
                $"rollback attempt failed after {consecutiveFailures} consecutive canary failures",
                "ROLLBACK_FAILED",
                false,
                Trim(exception.Message));

            _logger.LogCritical(exception, "Rollback attempt failed for flag {FlagKey}", _options.FlagKey);
        }
    }

    private async Task<ProbeResult> ProbeAsync(string targetName, string url, CancellationToken stoppingToken)
    {
        try
        {
            using var request = new HttpRequestMessage(HttpMethod.Get, url);
            using var response = await _httpClientFactory.CreateClient("guardian-probe")
                .SendAsync(request, stoppingToken);

            var details = Trim(await response.Content.ReadAsStringAsync(stoppingToken));
            var statusCode = (int)response.StatusCode;
            var healthy = response.IsSuccessStatusCode;

            _logger.LogInformation("Health probe {Target} -> {StatusCode}", targetName, statusCode);
            return new ProbeResult(targetName, statusCode, healthy, details);
        }
        catch (Exception exception) when (exception is HttpRequestException or TaskCanceledException)
        {
            _logger.LogWarning(exception, "Health probe failed for {Target}", targetName);
            return new ProbeResult(targetName, 503, false, Trim(exception.Message));
        }
    }

    private async Task TriggerRollbackAsync(int consecutiveFailures, CancellationToken stoppingToken)
    {
        var reason = $"auto-rollback after {consecutiveFailures} consecutive 5xx responses from canary";
        var controlPlaneClient = _httpClientFactory.CreateClient("control-plane");
        var toggleRequest = new HttpRequestMessage(
            HttpMethod.Put,
            $"{_options.ControlPlaneBaseUrl.TrimEnd('/')}/api/admin/flags/{_options.FlagId}/toggle")
        {
            Content = JsonContent.Create(new
            {
                enabled = false,
                reason
            })
        };
        toggleRequest.Headers.Add("Accept", "application/json");
        if (!string.IsNullOrWhiteSpace(_options.AdminApiToken))
        {
            toggleRequest.Headers.Add("X-Admin-Token", _options.AdminApiToken);
        }

        using var toggleResponse = await controlPlaneClient.SendAsync(toggleRequest, stoppingToken);
        var responseBody = Trim(await toggleResponse.Content.ReadAsStringAsync(stoppingToken));
        toggleResponse.EnsureSuccessStatusCode();

        var rollbackPayload = JsonSerializer.Serialize(new
        {
            flagKey = _options.FlagKey,
            flagId = _options.FlagId,
            reason,
            triggeredAt = DateTimeOffset.UtcNow,
            action = "FORCED_STABLE_ROUTING"
        });

        await _redis.GetSubscriber().PublishAsync(RedisChannel.Literal(_options.RollbackChannel), rollbackPayload);
        _telemetryState.RecordRollback(reason, "FORCED_STABLE_ROUTING", true, responseBody);

        _logger.LogError(
            "Rollback executed for flag {FlagKey}. Control plane response: {ResponseBody}",
            _options.FlagKey,
            responseBody);
    }

    private static string Trim(string? value)
    {
        if (string.IsNullOrWhiteSpace(value))
        {
            return string.Empty;
        }

        const int maxLength = 240;
        var trimmed = value.Trim();
        return trimmed.Length <= maxLength ? trimmed : trimmed[..maxLength];
    }

    private sealed record ProbeResult(string TargetName, int StatusCode, bool Healthy, string Details);
}
