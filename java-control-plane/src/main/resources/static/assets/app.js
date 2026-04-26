const state = {
  token: sessionStorage.getItem("controlPlane.adminToken") || "",
  overview: null,
  latestPreview: null
};

const elements = {
  authPanel: document.querySelector("#authPanel"),
  authForm: document.querySelector("#authForm"),
  adminTokenInput: document.querySelector("#adminTokenInput"),
  securityBadge: document.querySelector("#securityBadge"),
  refreshButton: document.querySelector("#refreshButton"),
  overviewGrid: document.querySelector("#overviewGrid"),
  flagsList: document.querySelector("#flagsList"),
  flagsCaption: document.querySelector("#flagsCaption"),
  metricFlagCount: document.querySelector("#metricFlagCount"),
  metricCanaryFailures: document.querySelector("#metricCanaryFailures"),
  metricPreviewTarget: document.querySelector("#metricPreviewTarget"),
  guardianPanel: document.querySelector("#guardianPanel"),
  previewResult: document.querySelector("#previewResult"),
  ruleFlagId: document.querySelector("#ruleFlagId"),
  createFlagForm: document.querySelector("#createFlagForm"),
  ruleForm: document.querySelector("#ruleForm"),
  previewForm: document.querySelector("#previewForm"),
  toastHost: document.querySelector("#toastHost")
};

function api(path, options = {}) {
  const headers = new Headers(options.headers || {});
  headers.set("Accept", "application/json");
  if (options.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  if (state.token) {
    headers.set("X-Admin-Token", state.token);
  }

  return fetch(path, {
    ...options,
    headers
  }).then(async (response) => {
    const contentType = response.headers.get("Content-Type") || "";
    const payload = contentType.includes("application/json") ? await response.json() : await response.text();

    if (!response.ok) {
      const error = new Error(payload?.message || "The request could not be completed.");
      error.status = response.status;
      error.payload = payload;
      throw error;
    }

    return payload;
  });
}

function showToast(title, message, tone = "default") {
  const toast = document.createElement("div");
  toast.className = "toast";
  if (tone === "error") {
    toast.style.borderColor = "rgba(255, 123, 114, 0.38)";
  }
  if (tone === "success") {
    toast.style.borderColor = "rgba(105, 230, 197, 0.38)";
  }
  toast.innerHTML = `<strong>${title}</strong><span>${message}</span>`;
  elements.toastHost.appendChild(toast);
  setTimeout(() => toast.remove(), 4200);
}

function statusPillClass(status) {
  const normalized = String(status || "").toUpperCase();
  if (normalized === "UP") {
    return "pill pill-success";
  }
  if (normalized === "UNREACHABLE" || normalized === "DOWN") {
    return "pill pill-danger";
  }
  return "pill pill-warning";
}

function renderOverview() {
  if (!state.overview) {
    return;
  }

  const { flags, guardianTelemetry } = state.overview;
  elements.metricFlagCount.textContent = String(flags.length);
  elements.metricCanaryFailures.textContent =
    String(guardianTelemetry?.current?.consecutiveCanaryFailures ?? 0);
  elements.metricPreviewTarget.textContent = state.latestPreview?.targetVersion || "--";
  elements.flagsCaption.textContent =
    `${flags.length} flag(s) under active management`;

  const cards = [
    state.overview.controlPlane,
    state.overview.dataPlane,
    state.overview.guardian,
    state.overview.stable,
    state.overview.canary
  ];

  elements.overviewGrid.innerHTML = cards
    .map((component) => {
      const details = Object.entries(component.details || {})
        .slice(0, 4)
        .map(([key, value]) => `<div><strong>${key}</strong><span>${formatValue(value)}</span></div>`)
        .join("");

      return `
        <article class="status-card">
          <div class="status-meta">
            <span>${component.name}</span>
            <span class="${statusPillClass(component.status)}">${component.status}</span>
          </div>
          <h3>${component.name.replace("-", " ")}</h3>
          <strong>${component.reachable ? "Reachable" : "Attention required"}</strong>
          <div class="status-details">${details || "<span>No telemetry details available.</span>"}</div>
        </article>
      `;
    })
    .join("");

  renderFlags(flags);
  renderGuardianTelemetry(guardianTelemetry);
  renderRuleFlagOptions(flags);
  updateSecurityBadge();
}

function renderFlags(flags) {
  if (!flags.length) {
    elements.flagsList.innerHTML = `
      <div class="empty-state">
        No feature flags were found. Create the first one from the panel on the right.
      </div>
    `;
    return;
  }

  elements.flagsList.innerHTML = flags
    .map((flag) => `
      <article class="flag-card">
        <div class="flag-header">
          <div>
            <h3 class="flag-title">${flag.key}</h3>
            <p class="flag-description">${flag.description}</p>
          </div>
          <span class="${flag.enabled ? "pill pill-success" : "pill pill-muted"}">
            ${flag.enabled ? "Enabled" : "Disabled"}
          </span>
        </div>
        <div class="badge-row">
          <span class="badge pill-muted">${flag.environmentName}</span>
          <span class="badge pill-muted">${flag.rules.length} rule(s)</span>
        </div>
        <div class="badge-row">
          ${flag.rules.map((rule) => `<span class="badge pill">${rule.attribute} ${rule.operator} ${rule.value} → ${rule.targetVersion}</span>`).join("") || '<span class="badge pill-muted">No targeting rules yet</span>'}
        </div>
        <div class="flag-actions">
          <span class="muted">ID ${flag.id}</span>
          <button class="button button-secondary" type="button" data-action="toggle" data-flag-id="${flag.id}" data-next-state="${!flag.enabled}">
            ${flag.enabled ? "Disable" : "Enable"}
          </button>
        </div>
      </article>
    `)
    .join("");
}

function renderRuleFlagOptions(flags) {
  elements.ruleFlagId.innerHTML = flags
    .map((flag) => `<option value="${flag.id}">${flag.key}</option>`)
    .join("");
}

function renderGuardianTelemetry(guardianTelemetry) {
  if (!guardianTelemetry?.current) {
    elements.guardianPanel.innerHTML = `
      <div class="error-state">Guardian telemetry could not be loaded.</div>
    `;
    return;
  }

  const current = guardianTelemetry.current;
  const rollbackHistory = current.rollbackHistory || [];

  elements.guardianPanel.innerHTML = `
    <div class="content-grid">
      <div class="probe-card">
        <strong>Stable target</strong>
        <span>Status code ${current.stable?.statusCode ?? "--"} at ${formatValue(current.stable?.checkedAt)}</span>
      </div>
      <div class="probe-card">
        <strong>Canary target</strong>
        <span>Status code ${current.canary?.statusCode ?? "--"} at ${formatValue(current.canary?.checkedAt)}</span>
      </div>
    </div>
    ${rollbackHistory.length ? rollbackHistory.map((entry) => `
      <div class="rollback-item">
        <strong>${entry.action}</strong>
        <span>${entry.reason}</span>
        <div class="muted">${formatValue(entry.triggeredAt)}</div>
      </div>
    `).join("") : '<div class="empty-state">No rollback events recorded yet.</div>'}
  `;
}

function formatValue(value) {
  if (Array.isArray(value)) {
    return value.join(", ");
  }
  if (value && typeof value === "object") {
    return Object.entries(value)
      .map(([key, inner]) => `${key}:${typeof inner === "object" ? JSON.stringify(inner) : inner}`)
      .join(" | ");
  }
  return value ?? "--";
}

async function loadOverview() {
  try {
    const overview = await api("/api/platform/overview");
    state.overview = overview;
    renderOverview();
    elements.authPanel.classList.add("hidden");
  } catch (error) {
    if (error.status === 401) {
      elements.authPanel.classList.remove("hidden");
      updateSecurityBadge(true);
      if (!state.overview) {
        elements.flagsList.innerHTML = '<div class="error-state">Unlock the admin surface to view operational state.</div>';
      }
      return;
    }

    elements.flagsList.innerHTML = `<div class="error-state">${error.message}</div>`;
    showToast("Refresh failed", error.message, "error");
  }
}

function updateSecurityBadge(locked = false) {
  if (locked || state.token) {
    elements.securityBadge.textContent = state.token ? "Protected mode unlocked" : "Protected mode";
    elements.securityBadge.className = "pill pill-warning";
    return;
  }

  elements.securityBadge.textContent = "Open access mode";
  elements.securityBadge.className = "pill pill-muted";
}

async function handleCreateFlag(event) {
  event.preventDefault();
  const formData = new FormData(event.currentTarget);

  try {
    await api("/api/admin/flags", {
      method: "POST",
      body: JSON.stringify({
        key: formData.get("key"),
        description: formData.get("description"),
        environmentName: formData.get("environmentName"),
        enabled: formData.get("enabled") === "on"
      })
    });
    event.currentTarget.reset();
    showToast("Flag created", "The new feature flag is available for release operations.", "success");
    await loadOverview();
  } catch (error) {
    showToast("Flag creation failed", error.message, "error");
  }
}

async function handleToggleClick(event) {
  const button = event.target.closest("[data-action='toggle']");
  if (!button) {
    return;
  }

  try {
    await api(`/api/admin/flags/${button.dataset.flagId}/toggle`, {
      method: "PUT",
      body: JSON.stringify({
        enabled: button.dataset.nextState === "true",
        reason: button.dataset.nextState === "true" ? "enabled from dashboard" : "disabled from dashboard"
      })
    });
    showToast("Flag updated", "Global rollout state changed successfully.", "success");
    await loadOverview();
  } catch (error) {
    showToast("Toggle failed", error.message, "error");
  }
}

async function handleAddRule(event) {
  event.preventDefault();
  const formData = new FormData(event.currentTarget);
  const flagId = formData.get("flagId");

  try {
    await api(`/api/admin/flags/${flagId}/rules`, {
      method: "POST",
      body: JSON.stringify({
        attribute: formData.get("attribute"),
        operator: formData.get("operator"),
        value: formData.get("value"),
        targetVersion: formData.get("targetVersion")
      })
    });
    event.currentTarget.reset();
    renderRuleFlagOptions(state.overview?.flags || []);
    showToast("Rule added", "Traffic targeting was updated and broadcast to the edge.", "success");
    await loadOverview();
  } catch (error) {
    showToast("Rule creation failed", error.message, "error");
  }
}

async function handlePreview(event) {
  event.preventDefault();
  const formData = new FormData(event.currentTarget);

  try {
    const preview = await api("/api/platform/decision-preview", {
      method: "POST",
      body: JSON.stringify({
        userId: formData.get("userId"),
        country: formData.get("country"),
        platform: formData.get("platform")
      })
    });

    state.latestPreview = preview;
    elements.previewResult.innerHTML = `
      <strong>${preview.targetVersion}</strong>
      <div>Reason: ${preview.reason}</div>
      <div class="muted">Context: ${Object.entries(preview.context || {}).map(([key, value]) => `${key}=${value}`).join(", ")}</div>
    `;
    showToast("Preview ready", "Edge routing decision calculated successfully.", "success");
    renderOverview();
  } catch (error) {
    elements.previewResult.textContent = error.message;
    showToast("Preview failed", error.message, "error");
  }
}

function handleAuth(event) {
  event.preventDefault();
  state.token = elements.adminTokenInput.value.trim();
  sessionStorage.setItem("controlPlane.adminToken", state.token);
  loadOverview().then(() => {
    if (state.overview) {
      showToast("Console unlocked", "Protected operations are available in this browser session.", "success");
    }
  });
}

elements.refreshButton.addEventListener("click", () => {
  loadOverview();
});
elements.authForm.addEventListener("submit", handleAuth);
elements.createFlagForm.addEventListener("submit", handleCreateFlag);
elements.ruleForm.addEventListener("submit", handleAddRule);
elements.previewForm.addEventListener("submit", handlePreview);
elements.flagsList.addEventListener("click", handleToggleClick);

if (state.token) {
  elements.adminTokenInput.value = state.token;
}

loadOverview();
