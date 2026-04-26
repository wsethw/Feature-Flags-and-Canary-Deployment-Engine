using DotnetGuardian;
using Microsoft.Extensions.Options;
using StackExchange.Redis;

var builder = WebApplication.CreateBuilder(args);

builder.Services
    .AddOptions<GuardianOptions>()
    .Bind(builder.Configuration.GetSection("Guardian"))
    .ValidateDataAnnotations()
    .Validate(options => Guid.TryParse(options.FlagId, out _), "Guardian:FlagId must be a valid GUID.")
    .ValidateOnStart();
builder.Services.AddSingleton<TelemetryState>();
builder.Services.AddHttpClient("guardian-probe", client =>
{
    client.Timeout = TimeSpan.FromSeconds(5);
    client.DefaultRequestHeaders.UserAgent.ParseAdd("dotnet-guardian/1.0");
});
builder.Services.AddHttpClient("control-plane", client =>
{
    client.Timeout = TimeSpan.FromSeconds(5);
    client.DefaultRequestHeaders.UserAgent.ParseAdd("dotnet-guardian/1.0");
});
builder.Services.AddSingleton<IConnectionMultiplexer>(_ =>
    ConnectionMultiplexer.Connect(builder.Configuration.GetConnectionString("Redis") ?? "redis:6379"));
builder.Services.AddHostedService<HealthMonitorWorker>();

var app = builder.Build();

app.MapGet("/health", () => Results.Ok(new
{
    status = "UP",
    service = "dotnet-guardian"
}));

app.MapGet("/ready", (TelemetryState telemetryState) =>
{
    var ready = telemetryState.HasCompletedInitialProbe();
    return ready
        ? Results.Ok(new { status = "READY", service = "dotnet-guardian" })
        : Results.StatusCode(StatusCodes.Status503ServiceUnavailable);
});

app.MapGet("/api/telemetry/status", (TelemetryState telemetryState, IOptions<GuardianOptions> options) =>
{
    var config = options.Value;
    return Results.Ok(new
    {
        service = "dotnet-guardian",
        generatedAt = DateTimeOffset.UtcNow,
        flagKey = config.FlagKey,
        flagId = config.FlagId,
        probeIntervalSeconds = config.ProbeIntervalSeconds,
        canaryFailureThreshold = config.CanaryFailureThreshold,
        current = telemetryState.Snapshot()
    });
});

app.Run();
