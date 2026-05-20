const originalDoLogin = window.doLogin;
const originalDoLogout = window.doLogout;
const originalNavigate = window.navigate;
const originalUpdateReportStatus = window.updateReportStatus;

const pathSegments = window.location.pathname.split('/').filter(Boolean);
const contextPath = pathSegments.length > 0 ? `/${pathSegments[0]}` : '';
const API_BASE = `${contextPath}/api`;

let currentSessionUser = null;
let clockStarted = false;

configureRoleNavigation();
bootstrapSession();

function configureRoleNavigation() {
    if (typeof ROLES === 'undefined') {
        return;
    }

    ROLES.field.nav = ['dashboard', 'reports', 'teams', 'resources', 'hospitals', 'approvals', 'alerts'];
    ROLES.operator.nav = ['dashboard', 'reports', 'events', 'teams', 'approvals', 'alerts'];
    ROLES.warehouse.nav = ['dashboard', 'resources', 'approvals', 'alerts'];
    ROLES.finance.nav = ['dashboard', 'financials', 'approvals', 'audit', 'alerts'];
}

async function bootstrapSession() {
    try {
        const response = await fetch(`${API_BASE}/auth`, {
            credentials: 'same-origin'
        });

        if (!response.ok) {
            return;
        }

        const data = await response.json();
        if (!data.authenticated || !data.user) {
            return;
        }

        applyAuthenticatedUser(data.user);
        showAuthenticatedApp();
        ensureClockStarted();
        buildSidebar();
        window.navigate('dashboard');
    } catch (error) {
        console.debug('Session bootstrap skipped:', error);
    }
}

function applyAuthenticatedUser(user, fallbackRole) {
    currentSessionUser = {
        ...user,
        role: user.role || fallbackRole,
        username: user.userName || user.username || 'User',
        label: getRoleLabel(user.role || fallbackRole)
    };

    currentUser = currentSessionUser;
    localStorage.setItem('user', JSON.stringify(currentSessionUser));

    document.getElementById('top-avatar').textContent = currentUser.username[0].toUpperCase();
    document.getElementById('top-username').textContent = currentUser.username;
    document.getElementById('top-role').textContent = currentUser.label;
}

function showAuthenticatedApp() {
    document.getElementById('login-screen').classList.add('hidden');
    document.getElementById('app').classList.add('visible');
}

function showLoggedOutApp() {
    document.getElementById('app').classList.remove('visible');
    document.getElementById('login-screen').classList.remove('hidden');
}

function ensureClockStarted() {
    if (!clockStarted && typeof startClock === 'function') {
        startClock();
        clockStarted = true;
    }
}

function getRoleLabel(role) {
    const labels = {
        admin: 'Administrator',
        operator: 'Emergency Operator',
        field: 'Field Officer',
        warehouse: 'Warehouse Manager',
        finance: 'Finance Officer'
    };
    return labels[role] || role || 'User';
}

async function parseJsonResponse(response) {
    let payload = null;
    try {
        payload = await response.json();
    } catch (error) {
        payload = null;
    }

    if (!response.ok) {
        const message = payload?.error || payload?.message || `Request failed (${response.status})`;
        const err = new Error(message);
        err.status = response.status;
        throw err;
    }

    return payload;
}

async function apiGet(path) {
    const response = await fetch(`${API_BASE}${path}`, {
        credentials: 'same-origin'
    });
    return parseJsonResponse(response);
}

async function apiSend(path, options = {}) {
    const response = await fetch(`${API_BASE}${path}`, {
        credentials: 'same-origin',
        ...options
    });
    return parseJsonResponse(response);
}

function formatDateTime(value, includeTime = true) {
    if (!value) {
        return '—';
    }

    const parsed = new Date(value);
    if (Number.isNaN(parsed.getTime())) {
        const text = String(value).replace('T', ' ');
        return includeTime ? text.slice(0, 16) : text.slice(0, 10);
    }

    return parsed.toLocaleString('en-PK', includeTime
        ? { year: 'numeric', month: 'short', day: '2-digit', hour: '2-digit', minute: '2-digit' }
        : { year: 'numeric', month: 'short', day: '2-digit' });
}

function formatDateInput(value) {
    if (!value) {
        return '';
    }

    const parsed = new Date(value);
    if (Number.isNaN(parsed.getTime())) {
        return String(value).slice(0, 10);
    }

    return parsed.toISOString().slice(0, 10);
}

function eventSeverityLabel(level) {
    const labels = {
        1: 'Low',
        2: 'Moderate',
        3: 'High',
        4: 'Severe',
        5: 'Critical'
    };
    return labels[level] || `Level ${level}`;
}

function mapAlertVisualType(alertType) {
    if (alertType === 'LowStock') return 'critical';
    if (alertType === 'Escalation') return 'warning';
    if (alertType === 'Assignment') return 'info';
    return 'info';
}

function parseProcurementAlertMessage(message) {
    const text = String(message || '');
    const match = text.match(/\[PROC_META\|W=(\d+)\|R=(\d+)\|Q=(\d+)\]\s*$/i);
    if (!match) {
        return null;
    }

    return {
        warehouseId: Number(match[1]),
        resourceId: Number(match[2]),
        requestedQty: Number(match[3]),
        summary: text.replace(/\s*\[PROC_META\|.*\]\s*$/i, '').trim()
    };
}

function approvalTypeLabel(type) {
    const labels = {
        ResourceAllocation: 'Resource Allocation',
        RescueDeployment: 'Team Deployment',
        Financial: 'Financial Transaction'
    };
    return labels[type] || type || 'Approval';
}

function approvalReference(type, referenceId) {
    if (type === 'ResourceAllocation') return `ALO-${referenceId}`;
    if (type === 'RescueDeployment') return `ASN-${referenceId}`;
    if (type === 'Financial') return `TXN-${referenceId}`;
    return String(referenceId);
}

function extractNumericId(value) {
    if (typeof value === 'number') {
        return value;
    }

    const match = String(value).match(/(\d+)$/);
    return match ? Number(match[1]) : null;
}

function escapeHtml(value) {
    return String(value ?? '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

function buildRestockReference(procurementMeta) {
    return `RSTK:W${procurementMeta.warehouseId}:R${procurementMeta.resourceId}:Q${procurementMeta.requestedQty}`;
}

function canCreateProcurementTransaction(alert) {
    return ['finance', 'admin'].includes(currentUser?.role) && Boolean(alert?.procurementMeta);
}

function renderAlertActions(alert, compact = false) {
    const buttons = [];
    if (canCreateProcurementTransaction(alert)) {
        buttons.push(`<button class="btn btn-primary btn-sm" onclick="openProcurementTransactionFromAlert('${alert.id}')">${compact ? 'Txn' : 'Create Transaction'}</button>`);
    }
    if (!alert.read) {
        buttons.push(`<button class="btn btn-secondary btn-sm" onclick="markAlertRead('${alert.id}')">${compact ? 'Mark Read' : 'Mark Read'}</button>`);
    }
    return buttons.join('');
}

function rerenderPage(page) {
    if (currentPage === page && typeof originalNavigate === 'function') {
        originalNavigate(page);
    }
}

doLogin = window.doLogin = async function doLogin() {
    const username = document.getElementById('l-user').value;
    const password = document.getElementById('l-pass').value;
    const role = document.getElementById('l-role').value;

    const body = new URLSearchParams();
    body.append('username', username);
    body.append('password', password);
    body.append('role', role);

    try {
        const data = await apiSend('/auth/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            },
            body
        });

        if (!data.success || !data.user) {
            showToast(data.message || 'Login failed', 'error');
            return;
        }

        applyAuthenticatedUser(data.user, role);
        showAuthenticatedApp();
        ensureClockStarted();
        buildSidebar();
        window.navigate('dashboard');
        showToast(`Welcome back, ${currentUser.username}!`, 'success');
    } catch (error) {
        console.error('Login error:', error);
        if (typeof originalDoLogin === 'function') {
            originalDoLogin();
            showToast('Backend unavailable, using local demo data', 'warning');
        } else {
            showToast(error.message || 'Login failed', 'error');
        }
    }
};

doLogout = window.doLogout = async function doLogout() {
    try {
        await apiSend('/auth/logout', { method: 'POST' });
    } catch (error) {
        console.debug('Logout request failed:', error);
    } finally {
        currentSessionUser = null;
        localStorage.removeItem('user');
        if (typeof originalDoLogout === 'function') {
            originalDoLogout();
        } else {
            showLoggedOutApp();
        }
    }
};

navigate = window.navigate = function navigate(page) {
    if (typeof originalNavigate === 'function') {
        originalNavigate(page);
    }
    loadPageData(page);
};

async function loadPageData(page) {
    try {
        switch (page) {
            case 'dashboard':
                await loadDashboardData();
                break;
            case 'reports':
                await loadReportsFromAPI();
                break;
            case 'events':
                await loadEventsFromAPI();
                break;
            case 'teams':
                await Promise.all([
                    loadReportsFromAPI({ rerender: false }),
                    loadTeamsFromAPI({ rerender: false })
                ]);
                rerenderPage('teams');
                break;
            case 'resources':
                await Promise.all([
                    loadEventsFromAPI({ rerender: false }),
                    loadResourcesFromAPI({ rerender: false })
                ]);
                rerenderPage('resources');
                break;
            case 'hospitals':
                await Promise.all([
                    loadReportsFromAPI({ rerender: false }),
                    loadHospitalsFromAPI({ rerender: false })
                ]);
                rerenderPage('hospitals');
                break;
            case 'financials':
                await Promise.all([
                    loadEventsFromAPI({ rerender: false }),
                    loadFinancialDataFromAPI({ rerender: false })
                ]);
                rerenderPage('financials');
                break;
            case 'approvals':
                await loadApprovalsFromAPI();
                break;
            case 'alerts':
                await loadAlertsFromAPI();
                break;
            case 'audit':
                await loadAuditLogsFromAPI();
                break;
            default:
                break;
        }
    } catch (error) {
        handleRequestFailure(error, `Could not load ${page}`);
    }
}

async function loadDashboardData() {
    const loaders = [
        loadEventsFromAPI({ rerender: false }),
        loadReportsFromAPI({ rerender: false }),
        loadTeamsFromAPI({ rerender: false }),
        loadResourcesFromAPI({ rerender: false }),
        loadApprovalsFromAPI({ rerender: false }),
        loadAlertsFromAPI({ rerender: false }),
        loadHospitalsFromAPI({ rerender: false })
    ];

    if (currentUser?.role === 'finance' || currentUser?.role === 'admin') {
        loaders.push(loadFinancialDataFromAPI({ rerender: false }));
    }

    const [stats] = await Promise.all([
        apiGet('/dashboard'),
        Promise.allSettled(loaders)
    ]);

    rerenderPage('dashboard');
    updateDashboardWithData(stats);
}

function updateDashboardWithData(data) {
    const stats = data?.stats || {};
    const values = [
        stats.activeEvents ?? 0,
        stats.openReports ?? 0,
        stats.deployedTeams ?? 0,
        stats.pendingApprovals ?? 0,
        stats.patientsAdmitted ?? 0,
        stats.lowStockAlerts ?? 0
    ];

    document.querySelectorAll('.stat-card').forEach((card, index) => {
        const target = card.querySelector('.stat-value');
        if (target && values[index] !== undefined) {
            target.textContent = values[index];
        }
    });
}

async function loadReportsFromAPI({ rerender = true } = {}) {
    const reports = await apiGet('/reports/all');
    DB.emergencyReports = reports.map(report => ({
        id: `RPT-${report.reportId}`,
        reportId: report.reportId,
        type: report.disasterType || 'Other',
        location: report.address,
        severity: report.severityLevel,
        reportedBy: report.citizenInfo || 'Anonymous',
        status: report.status,
        time: formatDateTime(report.reportTime),
        lat: report.latitude,
        lng: report.longitude,
        description: report.description,
        eventId: report.eventId,
        eventName: report.eventName
    }));

    if (rerender) {
        rerenderPage('reports');
    }
}

async function loadEventsFromAPI({ rerender = true } = {}) {
    const events = await apiGet('/events/all');
    DB.disasterEvents = events.map(event => ({
        id: `EVT-${event.eventId}`,
        eventId: event.eventId,
        name: event.eventName,
        type: event.disasterType || 'Other',
        region: event.affectedRegion,
        severity: eventSeverityLabel(event.severityLevel),
        severityLevel: event.severityLevel,
        status: event.status,
        start: formatDateTime(event.startDate, false),
        end: event.endDate ? formatDateTime(event.endDate, false) : '',
        budget: event.budgetAllocated
    }));

    if (rerender) {
        rerenderPage('events');
    }
}

async function loadTeamsFromAPI({ rerender = true } = {}) {
    const [teams, assignments] = await Promise.all([
        apiGet('/teams/all'),
        apiGet('/teams/assignments')
    ]);

    DB.rescueTeams = teams.map(team => ({
        id: team.teamId,
        teamId: team.teamId,
        name: team.teamName,
        type: team.teamType,
        location: `Lat: ${team.currentLat}, Long: ${team.currentLong}`,
        capacity: team.capacity,
        status: team.availabilityStatus
    }));

    DB.teamAssignments = assignments.map(assignment => ({
        id: `ASN-${assignment.assignmentId}`,
        assignmentId: assignment.assignmentId,
        team: assignment.teamName,
        event: assignment.reportLocation,
        assignedAt: formatDateTime(assignment.assignedAt),
        status: assignment.status,
        reportId: assignment.reportId,
        completedAt: assignment.completedAt ? formatDateTime(assignment.completedAt) : null
    }));

    if (rerender) {
        rerenderPage('teams');
    }
}

async function loadResourcesFromAPI({ rerender = true } = {}) {
    const [inventory, allocations] = await Promise.all([
        apiGet('/resources/inventory'),
        apiGet('/resources/allocations')
    ]);

    const resourceMap = new Map();
    const warehouseMap = new Map();

    DB.inventory = inventory.map(item => {
        resourceMap.set(item.resourceId, {
            id: item.resourceId,
            name: item.resourceName,
            type: item.resourceType,
            unit: item.unit,
            threshold: item.thresholdLevel
        });

        warehouseMap.set(item.warehouseId, {
            id: item.warehouseId,
            name: item.warehouseName,
            location: item.city,
            capacity: item.warehouseCapacity
        });

        return {
            warehouse: item.warehouseId,
            resource: item.resourceId,
            available: item.quantityAvailable,
            dispatched: item.quantityDispatched,
            consumed: item.quantityConsumed
        };
    });

    DB.resources = Array.from(resourceMap.values());
    DB.warehouses = Array.from(warehouseMap.values());
    DB.allocations = allocations.map(allocation => ({
        id: `ALO-${allocation.allocationId}`,
        allocationId: allocation.allocationId,
        resource: allocation.resourceName,
        warehouse: allocation.warehouseName,
        event: allocation.eventName,
        reqQty: allocation.requestedQty,
        appQty: allocation.approvedQty,
        status: allocation.status,
        date: formatDateTime(allocation.allocationDate, false),
        requestedByUserId: allocation.requestedBy,
        requestedBy: allocation.requestedByName,
        purpose: allocation.purpose,
        approvalId: allocation.approvalId,
        approvalStatus: allocation.approvalStatus,
        approvalRequestedBy: allocation.approvalRequestedBy,
        approvalRequestedByName: allocation.approvalRequestedByName,
        approvalComments: allocation.approvalComments,
        approvalCount: allocation.approvalCount || 0
    }));

    if (rerender) {
        rerenderPage('resources');
    }
}

async function loadHospitalsFromAPI({ rerender = true } = {}) {
    const hospitals = await apiGet('/hospitals/all');
    const patientLists = await Promise.all(hospitals.map(hospital => apiGet(`/hospitals/patients/${hospital.hospitalId}`)));

    DB.hospitals = hospitals.map((hospital, index) => ({
        id: hospital.hospitalId,
        hospitalId: hospital.hospitalId,
        name: hospital.name,
        location: [hospital.streetNo, hospital.city].filter(Boolean).join(', '),
        totalBeds: hospital.totalBeds,
        availableBeds: hospital.availableBeds,
        criticalCases: patientLists[index].filter(patient => patient.condition === 'Critical').length,
        contact: hospital.contactNumber
    }));

    DB.patients = patientLists.flatMap(patients => patients.map(patient => {
        const report = DB.emergencyReports.find(item => item.reportId === patient.reportId);
        return {
            id: `PAT-${patient.patientId}`,
            patientId: patient.patientId,
            name: patient.name,
            age: patient.age,
            gender: patient.gender,
            condition: patient.condition,
            hospital: patient.hospitalId,
            admission: formatDateTime(patient.admissionTime),
            event: report?.eventName || `RPT-${patient.reportId}`,
            reportId: patient.reportId
        };
    }));

    if (rerender) {
        rerenderPage('hospitals');
    }
}

async function loadFinancialDataFromAPI({ rerender = true } = {}) {
    const [transactions, summary] = await Promise.all([
        apiGet('/financial/all'),
        apiGet('/financial/summary')
    ]);

    DB.financials = transactions.map(transaction => ({
        id: `TXN-${transaction.transactionId}`,
        transactionId: transaction.transactionId,
        type: transaction.transactionType,
        amount: transaction.amount,
        ref: transaction.referenceNumber,
        date: formatDateTime(transaction.transactionDate, false),
        desc: transaction.description,
        by: transaction.userName || `User #${transaction.userId}`,
        event: transaction.eventName || 'General',
        approvalStatus: transaction.approvalStatus || 'Pending'
    }));

    DB.financialSummary = summary;

    if (rerender) {
        rerenderPage('financials');
    }
}

async function loadApprovalsFromAPI({ rerender = true } = {}) {
    const approvals = await apiGet('/approvals/all');
    DB.approvals = approvals.map(approval => ({
        id: `APR-${approval.approvalId}`,
        approvalId: approval.approvalId,
        type: approvalTypeLabel(approval.requestType),
        rawType: approval.requestType,
        ref: approvalReference(approval.requestType, approval.referenceId),
        requestedBy: approval.requestedByName || `User #${approval.requestedBy}`,
        requestedAt: formatDateTime(approval.requestedAt),
        status: approval.status,
        comments: approval.comments || '',
        decisionAt: approval.decisionAt ? formatDateTime(approval.decisionAt) : '',
        approvedBy: approval.approvedByName || ''
    }));

    buildSidebar();
    if (rerender) {
        rerenderPage('approvals');
    }
}

async function loadAlertsFromAPI({ rerender = true } = {}) {
    const alerts = await apiGet('/alerts/all');
    DB.alerts = alerts.map(alert => {
        const procurementMeta = parseProcurementAlertMessage(alert.message);
        const summary = procurementMeta?.summary || alert.message;
        return {
        id: `ALT-${alert.alertId}`,
        alertId: alert.alertId,
        type: mapAlertVisualType(alert.alertType),
        rawType: alert.alertType,
        msg: summary,
        fullMessage: alert.message,
        summary,
        procurementMeta,
        time: formatDateTime(alert.createdAt),
        read: alert.read
    };});

    const alertCount = document.getElementById('alert-count');
    if (alertCount) {
        alertCount.textContent = DB.alerts.filter(alert => !alert.read).length;
    }

    buildSidebar();
    if (rerender) {
        rerenderPage('alerts');
    }
}

renderAlerts = window.renderAlerts = function renderAlerts() {
    return `
    <div class="page-header"><div class="page-title">SYSTEM ALERTS</div><div class="page-sub">Real-time alerts from triggers, thresholds and system events</div></div>
    <div class="card" style="padding:0">
      ${DB.alerts.map(alert => `
      <div class="alert-item" style="${alert.read ? 'opacity:.6' : ''}">
        <span class="alert-dot ${alert.type}"></span>
        <div class="alert-content">
          <div class="alert-msg" style="font-size:13px">${alert.summary}</div>
          <div class="alert-meta"><span>${alert.time}</span><span>${alert.read ? 'Read' : 'Unread'}</span></div>
        </div>
        <div style="display:flex;gap:6px;flex-shrink:0;align-items:center">
          <span class="tag ${alert.type === 'critical' ? 'tag-red' : alert.type === 'warning' ? 'tag-orange' : 'tag-blue'}">${alert.type}</span>
          ${renderAlertActions(alert)}
        </div>
      </div>`).join('')}
    </div>`;
};

showAlertPanel = window.showAlertPanel = function showAlertPanel() {
    const list = document.getElementById('alert-list');
    list.innerHTML = DB.alerts.map(alert => `
    <div class="alert-item" style="${alert.read ? 'opacity:.5' : ''}">
      <span class="alert-dot ${alert.type}"></span>
      <div class="alert-content">
        <div class="alert-msg">${alert.summary}</div>
        <div class="alert-meta"><span>${alert.time}</span></div>
      </div>
      <div style="display:flex;gap:6px;flex-shrink:0">${renderAlertActions(alert, true)}</div>
    </div>`).join('');
    document.getElementById('alert-modal').classList.add('open');
};

async function loadAuditLogsFromAPI() {
    const logs = await apiGet('/audit');
    DB.auditLogs = logs.map(log => ({
        id: `LOG-${log.logId}`,
        user: log.performedByName || `User #${log.performedBy}`,
        action: log.action,
        table: log.tableAffected,
        old: log.oldValue,
        new: log.newValue,
        time: formatDateTime(log.timestamp)
    }));

    rerenderPage('audit');
}

renderApprovals = window.renderApprovals = function renderApprovals() {
    const pending = DB.approvals.filter(approval => approval.status === 'Pending');
    const canApprove = currentUser?.role === 'admin';

    return `
    <div class="page-header"><div class="page-title">APPROVAL WORKFLOW</div><div class="page-sub">${canApprove ? 'Administrator approval queue and history' : 'Track requests sent for approval and decision history'}</div></div>
    <div class="stat-grid" style="grid-template-columns:repeat(3,1fr)">
      <div class="stat-card"><div class="stat-label">Pending</div><div class="stat-value" style="color:var(--yellow)">${pending.length}</div></div>
      <div class="stat-card"><div class="stat-label">Approved</div><div class="stat-value" style="color:var(--green)">${DB.approvals.filter(approval => approval.status === 'Approved').length}</div></div>
      <div class="stat-card"><div class="stat-label">Rejected</div><div class="stat-value" style="color:var(--red)">${DB.approvals.filter(approval => approval.status === 'Rejected').length}</div></div>
    </div>
    <div class="tabs">
      <div class="tab active" onclick="switchTab(this,'apr-pending')">Pending <span style="background:var(--red);color:white;padding:1px 6px;font-size:10px;margin-left:4px">${pending.length}</span></div>
      <div class="tab" onclick="switchTab(this,'apr-history')">History</div>
    </div>
    <div id="apr-pending">
      ${pending.map(approval => `
      <div class="approval-card">
        <div style="width:40px;height:40px;background:var(--bg4);display:flex;align-items:center;justify-content:center;flex-shrink:0">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--orange)" stroke-width="2"><polyline points="9 11 12 14 22 4"/><path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/></svg>
        </div>
        <div class="approval-info">
          <div class="approval-title">${approval.type}</div>
          <div class="approval-meta">
            <span>Ref: <span style="color:var(--text)">${approval.ref}</span></span>
            <span>Requested by: <span style="color:var(--text)">${approval.requestedBy}</span></span>
            <span>At: ${approval.requestedAt}</span>
          </div>
          ${approval.comments ? `<div style="font-size:11px;color:var(--text2);margin-top:8px">${approval.comments}</div>` : ''}
        </div>
        <div class="approval-actions">
          ${canApprove
              ? `<button class="btn btn-success" onclick="approveRequest('${approval.id}',true)">✓ Approve</button><button class="btn btn-danger" onclick="approveRequest('${approval.id}',false)">✕ Reject</button>`
              : `<span class="tag tag-yellow">Awaiting admin decision</span>`}
        </div>
      </div>`).join('')}
      ${pending.length === 0 ? '<div class="empty"><p>No pending approvals</p></div>' : ''}
    </div>
    <div id="apr-history" class="hidden">
      <div class="card"><div class="table-wrap"><table>
        <tr><th>ID</th><th>Type</th><th>Reference</th><th>Requested By</th><th>Status</th><th>Decision</th><th>Comments</th></tr>
        ${DB.approvals.map(approval => `<tr>
          <td class="td-mono">${approval.id}</td>
          <td>${approval.type}</td>
          <td class="td-mono">${approval.ref}</td>
          <td style="color:var(--text2)">${approval.requestedBy}</td>
          <td>${statusTag(approval.status)}</td>
          <td style="color:var(--text2)">${approval.approvedBy || '—'}${approval.decisionAt ? `<br><span class="td-mono">${approval.decisionAt}</span>` : ''}</td>
          <td style="color:var(--text2);font-size:11px">${approval.comments || '—'}</td>
        </tr>`).join('')}
      </table></div></div>
    </div>`;
};

renderFinancials = window.renderFinancials = function renderFinancials() {
    const totalDonations = DB.financialSummary?.donations ?? DB.financials.filter(item => item.type === 'Donation' && item.approvalStatus === 'Approved').reduce((sum, item) => sum + item.amount, 0);
    const totalExpenses = DB.financialSummary?.expenses ?? DB.financials.filter(item => item.type !== 'Donation' && item.approvalStatus === 'Approved').reduce((sum, item) => sum + item.amount, 0);
    const totalProcurement = DB.financials.filter(item => item.type === 'Procurement' && item.approvalStatus === 'Approved').reduce((sum, item) => sum + item.amount, 0);
    const canCreate = ['finance', 'admin'].includes(currentUser?.role);

    return `
    <div class="page-header"><div class="page-title">FINANCIAL MANAGEMENT</div><div class="page-sub">Transactions, approvals and budget visibility</div></div>
    <div class="fin-summary">
      <div class="fin-box"><div class="fin-box-label">Approved Donations</div><div class="fin-box-value income">PKR ${(totalDonations / 1000).toFixed(0)}K</div></div>
      <div class="fin-box"><div class="fin-box-label">Approved Expenses</div><div class="fin-box-value expense">PKR ${(totalExpenses / 1000).toFixed(0)}K</div></div>
      <div class="fin-box"><div class="fin-box-label">Approved Procurement</div><div class="fin-box-value" style="color:var(--orange)">PKR ${(totalProcurement / 1000).toFixed(0)}K</div></div>
    </div>
    <div class="search-bar"><input class="search-input" placeholder="Search transactions..." oninput="filterTable(this,'fin-table')">${canCreate ? '<button class="btn btn-primary" onclick="openTransactionModal()">+ Add Transaction</button>' : ''}</div>
    <div class="card"><div class="table-wrap"><table id="fin-table">
      <tr><th>Txn ID</th><th>Type</th><th>Amount (PKR)</th><th>Reference</th><th>Event</th><th>Requested By</th><th>Status</th><th>Date</th></tr>
      ${DB.financials.map(item => `<tr>
        <td class="td-mono">${item.id}</td>
        <td>${txnTag(item.type)}</td>
        <td class="td-mono" style="color:${item.type === 'Donation' ? 'var(--green)' : 'var(--red)'}">${item.amount.toLocaleString()}</td>
        <td class="td-mono">${item.ref || '—'}</td>
        <td>${item.event}</td>
        <td style="color:var(--text2)">${item.by}</td>
        <td>${statusTag(item.approvalStatus)}</td>
        <td class="td-mono">${item.date}</td>
      </tr>`).join('')}
    </table></div></div>`;
};

renderTeams = window.renderTeams = function renderTeams() {
    const canRequestDeployment = ['admin', 'operator', 'field'].includes(currentUser?.role);
    return `
    <div class="page-header"><div class="page-title">RESCUE TEAMS</div><div class="page-sub">Availability, approved assignments and deployment requests</div></div>
    <div class="stat-grid" style="grid-template-columns:repeat(4,1fr)">
      ${['Available', 'Assigned', 'Busy', 'Completed'].map(status => {
          const count = DB.rescueTeams.filter(team => team.status === status).length;
          const colors = { Available: 'var(--green)', Assigned: 'var(--yellow)', Busy: 'var(--red)', Completed: 'var(--blue)' };
          return `<div class="stat-card" style="--accent:${colors[status]}"><div class="stat-label">${status}</div><div class="stat-value">${count}</div></div>`;
      }).join('')}
    </div>
    <div class="search-bar">
      <input class="search-input" placeholder="Search teams..." oninput="filterTable(this,'teams-table')">
    </div>
    <div class="card">
      <div class="table-wrap"><table id="teams-table">
        <tr><th>Team ID</th><th>Name</th><th>Type</th><th>Location</th><th>Capacity</th><th>Status</th><th>Actions</th></tr>
        ${DB.rescueTeams.map(team => `<tr>
          <td class="td-mono">${team.id}</td>
          <td style="font-weight:500">${team.name}</td>
          <td>${teamTypeTag(team.type)}</td>
          <td style="color:var(--text2)">${team.location}</td>
          <td class="td-mono">${team.capacity}</td>
          <td>${teamStatusTag(team.status)}</td>
          <td>${canRequestDeployment ? `<button class="btn btn-secondary btn-sm" onclick="deployTeam('${team.id}')">Request Deployment</button>` : '—'}</td>
        </tr>`).join('')}
      </table></div>
    </div>
    <div class="card">
      <div class="card-header"><div class="card-title"><span class="card-title-dot"></span>Approved Assignments</div></div>
      <div class="table-wrap"><table>
        <tr><th>Assignment ID</th><th>Team</th><th>Incident</th><th>Assigned At</th><th>Status</th></tr>
        ${DB.teamAssignments.map(assignment => `<tr>
          <td class="td-mono">${assignment.id}</td>
          <td>${assignment.team}</td>
          <td>${assignment.event}</td>
          <td class="td-mono">${assignment.assignedAt}</td>
          <td>${statusTag(assignment.status)}</td>
        </tr>`).join('')}
      </table></div>
    </div>`;
};

function allocationWorkflowTag(allocation) {
    if (allocation.status === 'Approved' || allocation.status === 'Rejected' || allocation.status === 'Dispatched') {
        return statusTag(allocation.status);
    }
    if (allocation.approvalCount > 1) {
        return '<span class="tag tag-yellow">ADMIN REVIEW</span>';
    }
    return '<span class="tag tag-blue">WAREHOUSE REVIEW</span>';
}

function canForwardAllocationToAdmin(allocation) {
    return currentUser?.role === 'warehouse'
        && allocation.status === 'Pending'
        && allocation.approvalCount <= 1
        && allocation.approvalId;
}

renderResources = window.renderResources = function renderResources() {
    const canRequest = ['field', 'warehouse', 'admin'].includes(currentUser?.role);
    const canRequestProcurement = ['warehouse', 'admin'].includes(currentUser?.role);
    return `
    <div class="page-header"><div class="page-title">RESOURCE MANAGEMENT</div><div class="page-sub">Field requests, warehouse review and dispatch approvals</div></div>
    <div class="tabs">
      <div class="tab active" onclick="switchTab(this,'res-inventory')">Inventory</div>
      <div class="tab" onclick="switchTab(this,'res-allocations')">Allocations</div>
      <div class="tab" onclick="switchTab(this,'res-warehouses')">Warehouses</div>
    </div>
    <div id="res-inventory">
      <div class="search-bar"><input class="search-input" placeholder="Search resources..." oninput="filterTable(this,'inventory-table')">${canRequest ? '<button class="btn btn-primary" onclick="openAllocationModal()">Request Resource</button>' : ''}</div>
      <div class="card"><div class="table-wrap"><table id="inventory-table">
        <tr><th>Resource</th><th>Type</th><th>Warehouse</th><th>Available</th><th>Dispatched</th><th>Consumed</th><th>Threshold</th><th>Status</th><th>Actions</th></tr>
        ${DB.inventory.map(item => {
            const resource = DB.resources.find(entry => entry.id === item.resource);
            const warehouse = DB.warehouses.find(entry => entry.id === item.warehouse);
            if (!resource || !warehouse) return '';
            const lowStock = item.available < resource.threshold;
            return `<tr>
              <td style="font-weight:500">${resource.name}</td>
              <td><span class="tag tag-blue">${resource.type}</span></td>
              <td style="color:var(--text2)">${warehouse.name}</td>
              <td class="td-mono" style="color:${lowStock ? 'var(--red)' : 'var(--green)'}">${item.available} ${resource.unit}</td>
              <td class="td-mono">${item.dispatched}</td>
              <td class="td-mono">${item.consumed}</td>
              <td class="td-mono">${resource.threshold}</td>
              <td>${lowStock ? '<span class="tag tag-red">LOW STOCK</span>' : '<span class="tag tag-green">ADEQUATE</span>'}</td>
              <td>${lowStock && canRequestProcurement ? `<button class="btn btn-secondary btn-sm" onclick="openProcurementRequestModal(${item.warehouse}, ${item.resource})">Request Procurement</button>` : '—'}</td>
            </tr>`;
        }).join('')}
      </table></div></div>
    </div>
    <div id="res-allocations" class="hidden">
      <div class="card"><div class="table-wrap"><table id="allocations-table">
        <tr><th>ID</th><th>Resource</th><th>Warehouse</th><th>Event</th><th>Requested By</th><th>Req. Qty</th><th>App. Qty</th><th>Workflow</th><th>Date</th><th>Actions</th></tr>
        ${DB.allocations.map(allocation => `<tr>
          <td class="td-mono">${allocation.id}</td>
          <td>${allocation.resource}</td>
          <td>${allocation.warehouse}</td>
          <td>${allocation.event}</td>
          <td style="color:var(--text2)">${allocation.requestedBy}</td>
          <td class="td-mono">${allocation.reqQty}</td>
          <td class="td-mono">${allocation.appQty ?? '—'}</td>
          <td>${allocationWorkflowTag(allocation)}</td>
          <td class="td-mono">${allocation.date}</td>
          <td>${canForwardAllocationToAdmin(allocation)
              ? `<button class="btn btn-secondary btn-sm" onclick="forwardAllocationToAdmin(${allocation.approvalId})">Send To Admin</button>`
              : '—'}</td>
        </tr>`).join('')}
      </table></div></div>
    </div>
    <div id="res-warehouses" class="hidden">
      <div style="display:grid;grid-template-columns:repeat(auto-fill,minmax(260px,1fr));gap:16px">
        ${DB.warehouses.map(warehouse => {
            const items = DB.inventory.filter(item => item.warehouse === warehouse.id);
            const totalStock = items.reduce((sum, item) => sum + item.available, 0);
            const pct = warehouse.capacity > 0 ? Math.min(100, Math.round(totalStock / warehouse.capacity * 100)) : 0;
            return `<div class="card" style="margin-bottom:0">
              <div style="padding:16px 20px;border-bottom:1px solid var(--border)">
                <div style="font-size:14px;font-weight:600;color:var(--text)">${warehouse.name}</div>
                <div style="font-size:11px;color:var(--text2);margin-top:2px">${warehouse.location} • Cap: ${warehouse.capacity.toLocaleString()}</div>
              </div>
              <div class="card-body">
                <div style="margin-bottom:12px">
                  <div style="display:flex;justify-content:space-between;margin-bottom:6px;font-size:11px;color:var(--text2)"><span>Utilization</span><span>${pct}%</span></div>
                  <div style="height:6px;background:var(--bg4)"><div style="height:100%;width:${pct}%;background:${pct > 80 ? 'var(--red)' : pct > 60 ? 'var(--orange)' : 'var(--green)'}"></div></div>
                </div>
                <div style="font-size:11px;color:var(--text2)">${items.length} resource types tracked</div>
              </div>
            </div>`;
        }).join('')}
      </div>
    </div>`;
};

openProcurementRequestModal = window.openProcurementRequestModal = async function openProcurementRequestModal(warehouseId, resourceId) {
    await loadResourcesFromAPI({ rerender: false });

    const inventoryItem = DB.inventory.find(item => item.warehouse === Number(warehouseId) && item.resource === Number(resourceId));
    const resource = DB.resources.find(item => item.id === Number(resourceId));
    const warehouse = DB.warehouses.find(item => item.id === Number(warehouseId));

    if (!inventoryItem || !resource || !warehouse) {
        showToast('Could not load the low-stock inventory item', 'error');
        return;
    }

    const suggestedQty = Math.max((resource.threshold || 0) - inventoryItem.available, 1);

    document.getElementById('gmodal-title').textContent = 'REQUEST PROCUREMENT';
    document.getElementById('gmodal-body').innerHTML = `
    <input id="procurement-warehouse-id" type="hidden" value="${warehouse.id}">
    <input id="procurement-resource-id" type="hidden" value="${resource.id}">
    <div style="background:var(--bg3);border:1px solid var(--border);padding:12px 16px;margin-bottom:20px">
      <div style="font-size:11px;color:var(--text2);margin-bottom:4px">Low stock escalation</div>
      <div style="font-size:13px;font-weight:600">${resource.name} at ${warehouse.name}</div>
      <div style="margin-top:6px;font-size:12px;color:var(--text2)">Available: ${inventoryItem.available} ${resource.unit} • Threshold: ${resource.threshold}</div>
    </div>
    <div class="form-grid">
      <div class="field"><label>Restock Quantity</label><input id="procurement-qty" type="number" value="${suggestedQty}" min="1"></div>
      <div class="field form-full"><label>Reason</label><textarea id="procurement-reason" placeholder="Explain why finance should raise a procurement transaction">Inventory is below threshold and needs replenishment.</textarea></div>
    </div>
    <div class="form-actions"><button class="btn btn-secondary" onclick="closeModal('generic-modal')">Cancel</button><button class="btn btn-primary" onclick="submitProcurementRequest()">Send To Finance</button></div>`;
    document.getElementById('generic-modal').classList.add('open');
};

submitProcurementRequest = window.submitProcurementRequest = async function submitProcurementRequest() {
    const payload = {
        warehouseId: Number(document.getElementById('procurement-warehouse-id').value),
        resourceId: Number(document.getElementById('procurement-resource-id').value),
        requestedQty: Number(document.getElementById('procurement-qty').value),
        reason: document.getElementById('procurement-reason').value.trim()
    };

    if (!payload.requestedQty || payload.requestedQty <= 0 || !payload.reason) {
        showToast('Please enter the restock quantity and reason', 'error');
        return;
    }

    try {
        await apiSend('/resources/procurement', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(payload)
        });

        closeModal('generic-modal');
        await Promise.all([
            loadResourcesFromAPI({ rerender: false }),
            loadDashboardData()
        ]);
        rerenderPage('resources');
        showToast('Procurement request sent to finance officers', 'success');
    } catch (error) {
        handleRequestFailure(error, 'Could not send the procurement request');
    }
};

openNewReportModal = window.openNewReportModal = async function openNewReportModal() {
    await loadEventsFromAPI({ rerender: false });

    document.getElementById('gmodal-title').textContent = 'NEW EMERGENCY REPORT';
    document.getElementById('gmodal-body').innerHTML = `
    <div class="form-grid">
      <div class="field"><label>Related Event</label><select id="report-event">${DB.disasterEvents.map(event => `<option value="${event.eventId}">${event.name}</option>`).join('')}</select></div>
      <div class="field"><label>Severity Level (1-5)</label><select id="report-severity">${[1, 2, 3, 4, 5].map(level => `<option value="${level}">${level} - ${eventSeverityLabel(level)}</option>`).join('')}</select></div>
      <div class="field form-full"><label>Location / Address</label><input id="report-address" type="text" placeholder="Enter incident location"></div>
      <div class="field"><label>Latitude</label><input id="report-lat" type="number" placeholder="31.5204" step="0.0001"></div>
      <div class="field"><label>Longitude</label><input id="report-lng" type="number" placeholder="74.3587" step="0.0001"></div>
      <div class="field form-full"><label>Reporter Info</label><input id="report-citizen" type="text" placeholder="Citizen or officer name / contact"></div>
      <div class="field form-full"><label>Description</label><textarea id="report-description" placeholder="Detailed description of the incident..."></textarea></div>
    </div>
    <div class="form-actions"><button class="btn btn-secondary" onclick="closeModal('generic-modal')">Cancel</button><button class="btn btn-primary" onclick="submitReport()">Submit Report</button></div>`;
    document.getElementById('generic-modal').classList.add('open');
};

submitReport = window.submitReport = async function submitReport() {
    const payload = {
        eventId: Number(document.getElementById('report-event').value),
        severityLevel: Number(document.getElementById('report-severity').value),
        address: document.getElementById('report-address').value.trim(),
        latitude: Number(document.getElementById('report-lat').value),
        longitude: Number(document.getElementById('report-lng').value),
        citizenInfo: document.getElementById('report-citizen').value.trim(),
        description: document.getElementById('report-description').value.trim()
    };

    if (!payload.address || Number.isNaN(payload.latitude) || Number.isNaN(payload.longitude) || !payload.description) {
        showToast('Please fill the report form completely', 'error');
        return;
    }

    try {
        await apiSend('/reports', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(payload)
        });

        closeModal('generic-modal');
        await Promise.all([
            loadReportsFromAPI({ rerender: false }),
            loadDashboardData()
        ]);
        rerenderPage('reports');
        showToast('Emergency report submitted successfully', 'success');
    } catch (error) {
        handleRequestFailure(error, 'Could not submit the report');
    }
};

openNewEventModal = window.openNewEventModal = function openNewEventModal() {
    document.getElementById('gmodal-title').textContent = 'CREATE DISASTER EVENT';
    document.getElementById('gmodal-body').innerHTML = `
    <div class="form-grid">
      <div class="field form-full"><label>Event Name</label><input id="event-name" type="text" placeholder="e.g. Lahore Mega Flood 2026"></div>
      <div class="field"><label>Disaster Type</label><select id="event-type">${['Flood', 'Earthquake', 'Wildfire', 'Other'].map(type => `<option value="${type}">${type}</option>`).join('')}</select></div>
      <div class="field"><label>Severity Level</label><select id="event-severity">${[1, 2, 3, 4, 5].map(level => `<option value="${level}">${level} - ${eventSeverityLabel(level)}</option>`).join('')}</select></div>
      <div class="field"><label>Affected Region</label><input id="event-region" type="text" placeholder="Punjab"></div>
      <div class="field"><label>Allocated Budget (PKR)</label><input id="event-budget" type="number" placeholder="5000000"></div>
      <div class="field"><label>Start Date</label><input id="event-start" type="date"></div>
      <div class="field"><label>End Date</label><input id="event-end" type="date"></div>
    </div>
    <div class="form-actions"><button class="btn btn-secondary" onclick="closeModal('generic-modal')">Cancel</button><button class="btn btn-primary" onclick="submitEvent()">Create Event</button></div>`;
    document.getElementById('generic-modal').classList.add('open');
};

submitEvent = window.submitEvent = async function submitEvent() {
    const payload = {
        eventName: document.getElementById('event-name').value.trim(),
        disasterType: document.getElementById('event-type').value,
        severityLevel: Number(document.getElementById('event-severity').value),
        affectedRegion: document.getElementById('event-region').value.trim(),
        budgetAllocated: Number(document.getElementById('event-budget').value || 0),
        startDate: document.getElementById('event-start').value,
        endDate: document.getElementById('event-end').value,
        status: 'Active'
    };

    if (!payload.eventName || !payload.affectedRegion || !payload.startDate) {
        showToast('Please complete the event form', 'error');
        return;
    }

    try {
        await apiSend('/events', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(payload)
        });

        closeModal('generic-modal');
        await Promise.all([
            loadEventsFromAPI({ rerender: false }),
            loadDashboardData()
        ]);
        rerenderPage('events');
        showToast('Disaster event created successfully', 'success');
    } catch (error) {
        handleRequestFailure(error, 'Could not create the disaster event');
    }
};

openAllocationModal = window.openAllocationModal = async function openAllocationModal() {
    await Promise.all([
        loadEventsFromAPI({ rerender: false }),
        loadResourcesFromAPI({ rerender: false })
    ]);

    document.getElementById('gmodal-title').textContent = 'REQUEST RESOURCE ALLOCATION';
    document.getElementById('gmodal-body').innerHTML = `
    <div class="form-grid">
      <div class="field"><label>Resource</label><select id="allocation-resource">${DB.resources.map(resource => `<option value="${resource.id}">${resource.name}</option>`).join('')}</select></div>
      <div class="field"><label>Warehouse</label><select id="allocation-warehouse">${DB.warehouses.map(warehouse => `<option value="${warehouse.id}">${warehouse.name}</option>`).join('')}</select></div>
      <div class="field"><label>Disaster Event</label><select id="allocation-event">${DB.disasterEvents.map(event => `<option value="${event.eventId}">${event.name}</option>`).join('')}</select></div>
      <div class="field"><label>Requested Quantity</label><input id="allocation-qty" type="number" placeholder="100"></div>
      <div class="field form-full"><label>Purpose</label><textarea id="allocation-purpose" placeholder="Reason for allocation request..."></textarea></div>
    </div>
    <div class="form-actions"><button class="btn btn-secondary" onclick="closeModal('generic-modal')">Cancel</button><button class="btn btn-primary" onclick="submitAllocationRequest()">Submit Request</button></div>`;
    document.getElementById('generic-modal').classList.add('open');
};

submitAllocationRequest = window.submitAllocationRequest = async function submitAllocationRequest() {
    const payload = {
        resourceId: Number(document.getElementById('allocation-resource').value),
        warehouseId: Number(document.getElementById('allocation-warehouse').value),
        eventId: Number(document.getElementById('allocation-event').value),
        requestedQty: Number(document.getElementById('allocation-qty').value),
        purpose: document.getElementById('allocation-purpose').value.trim()
    };

    if (!payload.requestedQty || !payload.purpose) {
        showToast('Please enter quantity and purpose', 'error');
        return;
    }

    try {
        await apiSend('/resources', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(payload)
        });

        closeModal('generic-modal');
        await Promise.all([
            loadResourcesFromAPI({ rerender: false }),
            loadApprovalsFromAPI({ rerender: false }),
            loadDashboardData()
        ]);
        rerenderPage('resources');
        showToast('Allocation request sent for admin approval', 'success');
    } catch (error) {
        handleRequestFailure(error, 'Could not submit the resource request');
    }
};

openTransactionModal = window.openTransactionModal = async function openTransactionModal(defaults = {}) {
    await loadEventsFromAPI({ rerender: false });

    const transactionType = defaults.transactionType || 'Donation';
    const amount = defaults.amount ?? '';
    const description = defaults.description || '';
    const referenceNumber = defaults.referenceNumber || '';
    const sourceAlertId = defaults.sourceAlertId || '';
    const procurementMeta = defaults.procurementMeta || null;

    document.getElementById('gmodal-title').textContent = 'NEW FINANCIAL TRANSACTION';
    document.getElementById('gmodal-body').innerHTML = `
    ${procurementMeta ? `<div style="background:var(--bg3);border:1px solid var(--border);padding:12px 16px;margin-bottom:20px"><div style="font-size:11px;color:var(--text2);margin-bottom:4px">Procurement request</div><div style="font-size:13px;font-weight:600">${escapeHtml(procurementMeta.summary || description)}</div><div style="margin-top:6px;font-size:12px;color:var(--text2)">Restock quantity: ${procurementMeta.requestedQty}</div></div>` : ''}
    <input id="txn-source-alert-id" type="hidden" value="${escapeHtml(sourceAlertId)}">
    <div class="form-grid">
      <div class="field"><label>Transaction Type</label><select id="txn-type"><option value="Donation" ${transactionType === 'Donation' ? 'selected' : ''}>Donation</option><option value="Expense" ${transactionType === 'Expense' ? 'selected' : ''}>Expense</option><option value="Procurement" ${transactionType === 'Procurement' ? 'selected' : ''}>Procurement</option></select></div>
      <div class="field"><label>Amount (PKR)</label><input id="txn-amount" type="number" placeholder="100000" value="${escapeHtml(amount)}"></div>
      <div class="field form-full"><label>Description</label><input id="txn-description" type="text" placeholder="Brief description of transaction" value="${escapeHtml(description)}"></div>
      <div class="field"><label>Reference Number</label><input id="txn-reference" type="text" placeholder="e.g. FIN-2026-001" value="${escapeHtml(referenceNumber)}" ${procurementMeta ? 'readonly' : ''}></div>
      <div class="field"><label>Related Event</label><select id="txn-event"><option value="">General / Not linked</option>${DB.disasterEvents.map(event => `<option value="${event.eventId}" ${Number(defaults.eventId) === event.eventId ? 'selected' : ''}>${event.name}</option>`).join('')}</select></div>
    </div>
    <div class="form-actions"><button class="btn btn-secondary" onclick="closeModal('generic-modal')">Cancel</button><button class="btn btn-primary" onclick="submitFinancialTransaction()">Submit for Approval</button></div>`;
    document.getElementById('generic-modal').classList.add('open');
};

submitFinancialTransaction = window.submitFinancialTransaction = async function submitFinancialTransaction() {
    const sourceAlertId = extractNumericId(document.getElementById('txn-source-alert-id')?.value);
    const payload = {
        transactionType: document.getElementById('txn-type').value,
        amount: Number(document.getElementById('txn-amount').value),
        description: document.getElementById('txn-description').value.trim(),
        referenceNumber: document.getElementById('txn-reference').value.trim(),
        eventId: Number(document.getElementById('txn-event').value || 0)
    };

    if (!payload.amount || !payload.description) {
        showToast('Please enter amount and description', 'error');
        return;
    }

    try {
        await apiSend('/financial', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(payload)
        });

        if (sourceAlertId) {
            await apiSend(`/alerts/read/${sourceAlertId}`, { method: 'PUT' }).catch(() => null);
        }

        closeModal('generic-modal');
        await Promise.all([
            loadFinancialDataFromAPI({ rerender: false }),
            loadApprovalsFromAPI({ rerender: false }),
            loadAlertsFromAPI({ rerender: false }).catch(() => null),
            loadDashboardData()
        ]);
        rerenderPage('financials');
        showToast(sourceAlertId ? 'Procurement transaction sent for admin approval' : 'Financial request sent for admin approval', 'success');
    } catch (error) {
        handleRequestFailure(error, 'Could not submit the financial request');
    }
};

openProcurementTransactionFromAlert = window.openProcurementTransactionFromAlert = async function openProcurementTransactionFromAlert(alertId) {
    const numericId = extractNumericId(alertId);
    const alert = DB.alerts.find(item => item.alertId === numericId || item.id === alertId);
    if (!alert?.procurementMeta) {
        showToast('This alert does not contain a procurement request', 'error');
        return;
    }

    await openTransactionModal({
        transactionType: 'Procurement',
        description: alert.procurementMeta.summary,
        referenceNumber: buildRestockReference(alert.procurementMeta),
        procurementMeta: alert.procurementMeta,
        sourceAlertId: numericId
    });
};

assignTeamModal = window.assignTeamModal = async function assignTeamModal(reportId, preferredTeamId = null) {
    await Promise.all([
        loadReportsFromAPI({ rerender: false }),
        loadTeamsFromAPI({ rerender: false })
    ]);

    const numericReportId = extractNumericId(reportId);
    const report = DB.emergencyReports.find(item => item.reportId === numericReportId || item.id === reportId);
    const availableTeams = DB.rescueTeams.filter(team => team.status === 'Available' || team.id === preferredTeamId);

    document.getElementById('gmodal-title').textContent = 'REQUEST TEAM DEPLOYMENT';
    document.getElementById('gmodal-body').innerHTML = `
    <div style="background:var(--bg3);border:1px solid var(--border);padding:12px 16px;margin-bottom:20px">
      <div style="font-size:11px;color:var(--text2);margin-bottom:4px">Report: ${report?.id || reportId}</div>
      <div style="font-size:13px;font-weight:600">${report?.type || 'Incident'} — ${report?.location || 'Unknown location'}</div>
      <div style="margin-top:4px">${statusTag(report?.status || 'Pending')}</div>
    </div>
    <div class="form-grid">
      <div class="field form-full"><label>Select Team</label><select id="team-request-id">${availableTeams.map(team => `<option value="${team.id}" ${preferredTeamId === team.id ? 'selected' : ''}>${team.name} (${team.type}) — ${team.location}</option>`).join('')}</select></div>
      <div class="field form-full"><label>Deployment Notes</label><textarea id="team-request-notes" placeholder="Special instructions or escalation details..."></textarea></div>
    </div>
    <div class="form-actions"><button class="btn btn-secondary" onclick="closeModal('generic-modal')">Cancel</button><button class="btn btn-primary" onclick="submitTeamDeploymentRequest(${numericReportId})">Send Approval Request</button></div>`;
    document.getElementById('generic-modal').classList.add('open');
};

submitTeamDeploymentRequest = window.submitTeamDeploymentRequest = async function submitTeamDeploymentRequest(reportId) {
    const teamId = document.getElementById('team-request-id').value;
    const notes = document.getElementById('team-request-notes').value.trim();
    const body = new URLSearchParams({
        teamId,
        reportId: String(reportId)
    });

    try {
        await apiSend('/teams', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            },
            body
        });

        closeModal('generic-modal');
        await Promise.all([
            loadTeamsFromAPI({ rerender: false }),
            loadApprovalsFromAPI({ rerender: false }),
            loadDashboardData()
        ]);
        rerenderPage('teams');
        if (notes) {
            console.debug('Team request notes:', notes);
        }
        showToast('Team deployment request sent for admin approval', 'success');
    } catch (error) {
        handleRequestFailure(error, 'Could not submit the deployment request');
    }
};

deployTeam = window.deployTeam = function deployTeam(teamId) {
    const team = DB.rescueTeams.find(item => item.id === extractNumericId(teamId) || item.id === teamId);
    if (!team || team.status !== 'Available') {
        showToast('Team is not available for deployment', 'error');
        return;
    }

    window.assignTeamModal(DB.emergencyReports[0]?.id, team.id);
};

updateReportStatus = window.updateReportStatus = async function updateReportStatus(id, status) {
    const numericId = extractNumericId(id);
    if (!numericId) {
        if (typeof originalUpdateReportStatus === 'function') {
            originalUpdateReportStatus(id, status);
        }
        return;
    }

    try {
        await apiSend(`/reports/${numericId}?status=${encodeURIComponent(status)}`, {
            method: 'PUT'
        });

        await Promise.all([
            loadReportsFromAPI({ rerender: false }),
            loadDashboardData()
        ]);
        rerenderPage('reports');
        showToast(`Report ${id} marked as ${status}`, 'success');
    } catch (error) {
        handleRequestFailure(error, 'Could not update the report status');
    }
};

approveRequest = window.approveRequest = async function approveRequest(id, approve) {
    const numericId = extractNumericId(id);
    if (!numericId) {
        showToast('Invalid approval request', 'error');
        return;
    }

    const status = approve ? 'Approved' : 'Rejected';
    const promptMessage = approve ? 'Optional approval comments:' : 'Optional rejection reason:';
    const comments = window.prompt(promptMessage, '') ?? '';

    try {
        await apiSend(`/approvals/${numericId}?status=${encodeURIComponent(status)}&comments=${encodeURIComponent(comments)}`, {
            method: 'PUT'
        });

        await Promise.all([
            loadApprovalsFromAPI({ rerender: false }),
            loadResourcesFromAPI({ rerender: false }).catch(() => null),
            loadTeamsFromAPI({ rerender: false }).catch(() => null),
            loadFinancialDataFromAPI({ rerender: false }).catch(() => null),
            loadReportsFromAPI({ rerender: false }).catch(() => null),
            loadAuditLogsFromAPI().catch(() => null),
            loadDashboardData()
        ]);
        rerenderPage('approvals');
        showToast(approve ? 'Request approved successfully' : 'Request rejected successfully', approve ? 'success' : 'warning');
    } catch (error) {
        handleRequestFailure(error, 'Could not update the approval request');
    }
};

forwardAllocationToAdmin = window.forwardAllocationToAdmin = async function forwardAllocationToAdmin(approvalId) {
    const comments = window.prompt('Optional warehouse review note for admin:', '') ?? '';

    try {
        await apiSend(`/resources/forward/${approvalId}?comments=${encodeURIComponent(comments)}`, {
            method: 'PUT'
        });
        await Promise.all([
            loadResourcesFromAPI({ rerender: false }),
            loadApprovalsFromAPI({ rerender: false }),
            loadDashboardData()
        ]);
        rerenderPage('resources');
        showToast('Request sent to administrator for final approval', 'success');
    } catch (error) {
        handleRequestFailure(error, 'Could not send the resource request to admin');
    }
};

markAlertRead = window.markAlertRead = async function markAlertRead(id) {
    const numericId = extractNumericId(id);
    if (!numericId) {
        return;
    }

    try {
        const currentAlert = DB.alerts.find(alert => alert.alertId === numericId || alert.id === id);
        if (currentAlert) {
            currentAlert.read = true;
        }
        await apiSend(`/alerts/read/${numericId}`, { method: 'PUT' });
        await loadAlertsFromAPI({ rerender: false });
        if (document.getElementById('alert-modal').classList.contains('open')) {
            showAlertPanel();
        }
        rerenderPage('alerts');
    } catch (error) {
        handleRequestFailure(error, 'Could not mark the alert as read');
    }
};

openPatientModal = window.openPatientModal = async function openPatientModal(defaultHospitalId = '') {
    await Promise.all([
        loadReportsFromAPI({ rerender: false }),
        loadHospitalsFromAPI({ rerender: false })
    ]);

    document.getElementById('gmodal-title').textContent = 'ADMIT PATIENT';
    document.getElementById('gmodal-body').innerHTML = `
    <div class="form-grid">
      <div class="field form-full"><label>Patient Name</label><input id="patient-name" type="text" placeholder="Full name"></div>
      <div class="field"><label>Age</label><input id="patient-age" type="number" placeholder="0"></div>
      <div class="field"><label>Gender</label><select id="patient-gender"><option value="Male">Male</option><option value="Female">Female</option><option value="Other">Other</option></select></div>
      <div class="field"><label>Condition</label><select id="patient-condition"><option value="Stable">Stable</option><option value="Critical">Critical</option></select></div>
      <div class="field"><label>Assign Hospital</label><select id="patient-hospital">${DB.hospitals.map(hospital => `<option value="${hospital.hospitalId}" ${String(defaultHospitalId) === String(hospital.hospitalId) ? 'selected' : ''}>${hospital.name} (${hospital.availableBeds} beds free)</option>`).join('')}</select></div>
      <div class="field"><label>Linked Report</label><select id="patient-report">${DB.emergencyReports.map(report => `<option value="${report.reportId}">${report.id} — ${report.location}</option>`).join('')}</select></div>
    </div>
    <div class="form-actions"><button class="btn btn-secondary" onclick="closeModal('generic-modal')">Cancel</button><button class="btn btn-primary" onclick="submitPatientAdmission()">Admit Patient</button></div>`;
    document.getElementById('generic-modal').classList.add('open');
};

submitPatientAdmission = window.submitPatientAdmission = async function submitPatientAdmission() {
    const payload = {
        name: document.getElementById('patient-name').value.trim(),
        age: Number(document.getElementById('patient-age').value),
        gender: document.getElementById('patient-gender').value,
        condition: document.getElementById('patient-condition').value,
        hospitalId: Number(document.getElementById('patient-hospital').value),
        reportId: Number(document.getElementById('patient-report').value)
    };

    if (!payload.name || !payload.age) {
        showToast('Please fill the patient details', 'error');
        return;
    }

    try {
        await apiSend('/hospitals/admit', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(payload)
        });

        closeModal('generic-modal');
        await Promise.all([
            loadHospitalsFromAPI({ rerender: false }),
            loadDashboardData()
        ]);
        rerenderPage('hospitals');
        showToast('Patient admitted successfully', 'success');
    } catch (error) {
        handleRequestFailure(error, 'Could not admit the patient');
    }
};

assignPatient = window.assignPatient = function assignPatient(hospitalId) {
    window.openPatientModal(hospitalId);
};

openNewTeamModal = window.openNewTeamModal = function openNewTeamModal() {
    showToast('Team creation is not implemented in the backend yet', 'warning');
};

openDonorModal = window.openDonorModal = function openDonorModal() {
    showToast('Donor management is not implemented in the backend yet', 'warning');
};

function handleRequestFailure(error, fallbackMessage) {
    console.error(error);
    if (error?.status === 401) {
        showToast('Your session has expired. Please log in again.', 'error');
        showLoggedOutApp();
        return;
    }

    showToast(error?.message || fallbackMessage, 'error');
}
