namespace DotnetGuardian;

public sealed class TelemetryState
{
    private readonly object _gate = new();
    private ProbeSnapshot _stable = ProbeSnapshot.Unknown("stable");
    private ProbeSnapshot _canary = ProbeSnapshot.Unknown("canary");
    private int _consecutiveCanaryFailures;
    private readonly List<ProbeSnapshot> _stableHistory = [];
    private readonly List<ProbeSnapshot> _canaryHistory = [];
    private readonly List<RollbackRecord> _rollbackHistory = [];

    public void RecordStableProbe(int statusCode, bool healthy, string? details)
    {
        lock (_gate)
        {
            _stable = new ProbeSnapshot("stable", statusCode, healthy, DateTimeOffset.UtcNow, details);
            AppendProbe(_stableHistory, _stable);
        }
    }

    public int RecordCanaryProbe(int statusCode, bool healthy, string? details)
    {
        lock (_gate)
        {
            _canary = new ProbeSnapshot("canary", statusCode, healthy, DateTimeOffset.UtcNow, details);
            AppendProbe(_canaryHistory, _canary);
            _consecutiveCanaryFailures = statusCode >= 500 ? _consecutiveCanaryFailures + 1 : 0;
            return _consecutiveCanaryFailures;
        }
    }

    public void RecordRollback(string reason, string action, bool succeeded, string? details)
    {
        lock (_gate)
        {
            _rollbackHistory.Insert(0, new RollbackRecord(DateTimeOffset.UtcNow, reason, action, succeeded, details));
            if (_rollbackHistory.Count > 20)
            {
                _rollbackHistory.RemoveAt(_rollbackHistory.Count - 1);
            }
        }
    }

    public bool HasCompletedInitialProbe()
    {
        lock (_gate)
        {
            return _stable.StatusCode > 0 && _canary.StatusCode > 0;
        }
    }

    public TelemetrySnapshot Snapshot()
    {
        lock (_gate)
        {
            return new TelemetrySnapshot(
                _stable,
                _canary,
                _consecutiveCanaryFailures,
                _consecutiveCanaryFailures > 0 ? "ELEVATED" : "NOMINAL",
                _stableHistory.ToArray(),
                _canaryHistory.ToArray(),
                _rollbackHistory.ToArray());
        }
    }

    private static void AppendProbe(List<ProbeSnapshot> history, ProbeSnapshot probe)
    {
        history.Insert(0, probe);
        if (history.Count > 10)
        {
            history.RemoveAt(history.Count - 1);
        }
    }
}

public sealed record ProbeSnapshot(
    string Target,
    int StatusCode,
    bool Healthy,
    DateTimeOffset CheckedAt,
    string? Details)
{
    public static ProbeSnapshot Unknown(string target) =>
        new(target, 0, false, DateTimeOffset.UtcNow, "probe not executed yet");
}

public sealed record RollbackRecord(
    DateTimeOffset TriggeredAt,
    string Reason,
    string Action,
    bool Succeeded,
    string? Details);

public sealed record TelemetrySnapshot(
    ProbeSnapshot Stable,
    ProbeSnapshot Canary,
    int ConsecutiveCanaryFailures,
    string ProtectionMode,
    IReadOnlyCollection<ProbeSnapshot> StableHistory,
    IReadOnlyCollection<ProbeSnapshot> CanaryHistory,
    IReadOnlyCollection<RollbackRecord> RollbackHistory);
