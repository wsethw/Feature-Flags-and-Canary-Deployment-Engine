using System.ComponentModel.DataAnnotations;

namespace DotnetGuardian;

public sealed class GuardianOptions
{
    [Required]
    [Url]
    public string StableHealthUrl { get; set; } = "http://app-stable:8080/health";

    [Required]
    [Url]
    public string CanaryHealthUrl { get; set; } = "http://app-canary:8080/health";

    [Required]
    [Url]
    public string ControlPlaneBaseUrl { get; set; } = "http://java-control-plane:8080";

    [Required]
    public string FlagId { get; set; } = "11111111-1111-1111-1111-111111111111";

    [Required]
    public string FlagKey { get; set; } = "new-checkout";

    [Required]
    public string RollbackChannel { get; set; } = "feature-flags:rollback";

    [Range(1, 300)]
    public int ProbeIntervalSeconds { get; set; } = 10;

    [Range(1, 20)]
    public int CanaryFailureThreshold { get; set; } = 3;

    public string AdminApiToken { get; set; } = string.Empty;
}
