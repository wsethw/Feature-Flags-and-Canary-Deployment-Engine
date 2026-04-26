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

function createElement(tagName, options = {}, children = []) {
  const element = document.createElement(tagName);

  if (options.className) {
    element.className = options.className;
  }
  if (options.text !== undefined) {
    element.textContent = String(options.text);
  }
  if (options.attributes) {
    Object.entries(options.attributes).forEach(([name, value]) => {
      element.setAttribute(name, String(value));
    });
  }
  if (options.dataset) {
    Object.entries(options.dataset).forEach(([name, value]) => {
      element.dataset[name] = String(value);
    });
  }

  children.forEach((child) => {
    if (child) {
      element.appendChild(child);
    }
  });

  return element;
}

function replaceWithMessage(container, className, message) {
  container.replaceChildren(createElement("div", { className, text: message }));
}

function showToast(title, message, tone = "default") {
  const toast = createElement("div", { className: "toast" }, [
    createElement("strong", { text: title }),
    createElement("span", { text: message })
  ]);

  if (tone === "error") {
    toast.style.borderColor = "rgba(255, 123, 114, 0.38)";
  }
  if (tone === "success") {
    toast.style.borderColor = "rgba(105, 230, 197, 0.38)";
  }

  elements.toastHost.appendChild(toast);
  setTimeout(() => toast.remove(), 4200);
}

function statusPillClass(status) {
  const normalized = String(status || "").toUpperCase();
  if (normalized === "UP" || normalized === "READY") {
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

  const flags = Array.isArray(state.overview.flags) ? state.overview.flags : [];
  const { guardianTelemetry } = state.overview;
  elements.metricFlagCount.textContent = String(flags.length);
  elements.metricCanaryFailures.textContent =
    String(guardianTelemetry?.current?.consecutiveCanaryFailures ?? 0);
  elements.metricPreviewTarget.textContent = state.latestPreview?.targetVersion || "--";
  elements.flagsCaption.textContent = `${flags.length} flag(s) under active management`;

  const cards = [
    state.overview.controlPlane,
    state.overview.dataPlane,
    state.overview.guardian,
    state.overview.stable,
    state.overview.canary
  ];

  elements.overviewGrid.replaceChildren(...cards.map(renderStatusCard));
  renderFlags(flags);
  renderGuardianTelemetry(guardianTelemetry);
  renderRuleFlagOptions(flags);
  updateSecurityBadge();
}

function renderStatusCard(component = {}) {
  const details = Object.entries(component.details || {})
    .slice(0, 4)
    .map(([key, value]) => createElement("div", {}, [
      createElement("strong", { text: key }),
      createElement("span", { text: formatValue(value) })
    ]));

  const detailsContainer = createElement("div", { className: "status-details" });
  if (details.length) {
    detailsContainer.replaceChildren(...details);
  } else {
    detailsContainer.replaceChildren(createElement("span", { text: "No telemetry details available." }));
  }

  const name = component.name || "unknown";
  return createElement("article", { className: "status-card" }, [
    createElement("div", { className: "status-meta" }, [
      createElement("span", { text: name }),
      createElement("span", { className: statusPillClass(component.status), text: component.status || "UNKNOWN" })
    ]),
    createElement("h3", { text: name.replace("-", " ") }),
    createElement("strong", { text: component.reachable ? "Reachable" : "Attention required" }),
    detailsContainer
  ]);
}

function renderFlags(flags) {
  if (!flags.length) {
    replaceWithMessage(
      elements.flagsList,
      "empty-state",
      "No feature flags were found. Create the first one from the panel on the right."
    );
    return;
  }

  elements.flagsList.replaceChildren(...flags.map(renderFlagCard));
}

function renderFlagCard(flag) {
  const rules = Array.isArray(flag.rules) ? flag.rules : [];
  const ruleBadges = rules.length
    ? rules.map((rule) => createElement("span", {
      className: "badge pill",
      text: `${rule.attribute} ${rule.operator} ${rule.value} -> ${rule.targetVersion}`
    }))
    : [createElement("span", { className: "badge pill-muted", text: "No targeting rules yet" })];

  return createElement("article", { className: "flag-card" }, [
    createElement("div", { className: "flag-header" }, [
      createElement("div", {}, [
        createElement("h3", { className: "flag-title", text: flag.key }),
        createElement("p", { className: "flag-description", text: flag.description })
      ]),
      createElement("span", {
        className: flag.enabled ? "pill pill-success" : "pill pill-muted",
        text: flag.enabled ? "Enabled" : "Disabled"
      })
    ]),
    createElement("div", { className: "badge-row" }, [
      createElement("span", { className: "badge pill-muted", text: flag.environmentName }),
      createElement("span", { className: "badge pill-muted", text: `${rules.length} rule(s)` })
    ]),
    createElement("div", { className: "badge-row" }, ruleBadges),
    createElement("div", { className: "flag-actions" }, [
      createElement("span", { className: "muted", text: `ID ${flag.id}` }),
      createElement("button", {
        className: "button button-secondary",
        text: flag.enabled ? "Disable" : "Enable",
        attributes: { type: "button" },
        dataset: {
          action: "toggle",
          flagId: flag.id,
          nextState: !flag.enabled
        }
      })
    ])
  ]);
}

function renderRuleFlagOptions(flags) {
  if (!flags.length) {
    elements.ruleFlagId.replaceChildren(createElement("option", {
      text: "No flags available",
      attributes: { value: "" }
    }));
    return;
  }

  elements.ruleFlagId.replaceChildren(...flags.map((flag) =>
    createElement("option", { text: flag.key, attributes: { value: flag.id } })
  ));
}

function renderGuardianTelemetry(guardianTelemetry) {
  if (!guardianTelemetry?.current) {
    replaceWithMessage(elements.guardianPanel, "error-state", "Guardian telemetry could not be loaded.");
    return;
  }

  const current = guardianTelemetry.current;
  const rollbackHistory = Array.isArray(current.rollbackHistory) ? current.rollbackHistory : [];
  const contentGrid = createElement("div", { className: "content-grid" }, [
    renderProbeCard("Stable target", current.stable),
    renderProbeCard("Canary target", current.canary)
  ]);

  const historyNodes = rollbackHistory.length
    ? rollbackHistory.map(renderRollbackItem)
    : [createElement("div", { className: "empty-state", text: "No rollback events recorded yet." })];

  elements.guardianPanel.replaceChildren(contentGrid, ...historyNodes);
}

function renderProbeCard(title, probe) {
  return createElement("div", { className: "probe-card" }, [
    createElement("strong", { text: title }),
    createElement("span", {
      text: `Status code ${probe?.statusCode ?? "--"} at ${formatValue(probe?.checkedAt)}`
    })
  ]);
}

function renderRollbackItem(entry) {
  return createElement("div", { className: "rollback-item" }, [
    createElement("strong", { text: entry.action || "ROLLBACK_EVENT" }),
    createElement("span", { text: entry.reason || "No reason supplied" }),
    createElement("div", { className: "muted", text: formatValue(entry.triggeredAt) })
  ]);
}

function formatValue(value) {
  if (value === undefined || value === null || value === "") {
    return "--";
  }
  if (Array.isArray(value)) {
    return value.map(formatValue).join(", ");
  }
  if (typeof value === "object") {
    return Object.entries(value)
      .map(([key, inner]) => `${key}:${typeof inner === "object" ? JSON.stringify(inner) : formatValue(inner)}`)
      .join(" | ");
  }
  return String(value);
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
        replaceWithMessage(
          elements.flagsList,
          "error-state",
          "Unlock the admin surface to view operational state."
        );
      }
      return;
    }

    replaceWithMessage(elements.flagsList, "error-state", error.message);
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

    const contextSummary = Object.entries(preview.context || {})
      .map(([key, value]) => `${key}=${value}`)
      .join(", ");

    state.latestPreview = preview;
    elements.previewResult.replaceChildren(
      createElement("strong", { text: preview.targetVersion }),
      createElement("div", { text: `Reason: ${preview.reason}` }),
      createElement("div", { className: "muted", text: `Context: ${contextSummary}` })
    );
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
