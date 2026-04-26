using DotnetGuardian;
using Xunit;

namespace DotnetGuardian.Tests;

public class TelemetryStateTests
{
    [Fact]
    public void CanaryFailuresAreCountedAndReset()
    {
        var telemetry = new TelemetryState();

        Assert.Equal(1, telemetry.RecordCanaryProbe(500, false, "boom"));
        Assert.Equal(2, telemetry.RecordCanaryProbe(503, false, "still broken"));
        Assert.Equal(0, telemetry.RecordCanaryProbe(200, true, "recovered"));
    }

    [Fact]
    public void RollbackHistoryKeepsTheMostRecentEntries()
    {
        var telemetry = new TelemetryState();

        for (var index = 0; index < 25; index++)
        {
            telemetry.RecordRollback($"reason-{index}", "FORCED_STABLE_ROUTING", true, "ok");
        }

        var snapshot = telemetry.Snapshot();
        Assert.Equal(20, snapshot.RollbackHistory.Count);
    }
}
