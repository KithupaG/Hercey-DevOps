const API_BASE = 'api/admin/dashboard';

window.addEventListener('load', async () => {
    await loadDashboardStats();
});

document.addEventListener("DOMContentLoaded", () => {
    const menuLinks = document.querySelectorAll('.menu-link');
    const sections = document.querySelectorAll('.content-section');

    menuLinks.forEach(link => {
        link.addEventListener('click', function (e) {
            e.preventDefault();

            const targetId = this.getAttribute('data-target');
            const targetSection = document.getElementById(targetId);

            if (!targetSection) return;

            sections.forEach(section => {
                section.classList.add('d-none');
            });

            targetSection.classList.remove('d-none');

            menuLinks.forEach(nav => nav.classList.remove('active-nav'));
            this.classList.add('active-nav');

            if (targetId === 'sellerApplications') loadSellerApplications();
            if (targetId === 'productApprovals') loadProductApprovals();
            if (targetId === 'manageUsers') loadUsers();
            if (targetId === 'manageProducts') loadProducts();
            if (targetId === 'dashboard') loadDashboardStats();
        });
    });
});

/* ---------- Selection helpers ---------- */

function getSelectedIds(tbodyId) {
    const checked = document.querySelectorAll(`#${tbodyId} .select-item:checked`);
    return Array.from(checked).map(cb => parseInt(cb.value));
}

function updateSelCount(tbodyId, countId, ...btnIds) {
    const ids = getSelectedIds(tbodyId);
    document.getElementById(countId).textContent = `${ids.length} selected`;
    btnIds.forEach(id => {
        document.getElementById(id).disabled = ids.length === 0;
    });
}

function toggleSelectAll(tbodyId, checkbox) {
    const checked = checkbox.checked;
    document.querySelectorAll(`#${tbodyId} .select-item`).forEach(cb => cb.checked = checked);
    const countId = checkbox.id.replace('SelectAll', 'SelCount');
    const btnIds = getToolbarBtnIds(tbodyId);
    updateSelCount(tbodyId, countId, ...btnIds);
}

function getToolbarBtnIds(tbodyId) {
    const map = {
        'seller-applications-body': ['sellerApproveBtn', 'sellerReactivateBtn'],
        'product-approvals-body': ['prodApprovalApproveBtn', 'prodApprovalReactivateBtn'],
        'manage-users-body': ['userBlockBtn', 'userRemoveBtn', 'userReactivateBtn'],
        'manage-products-body': ['prodRemoveBtn', 'prodReactivateBtn']
    };
    return map[tbodyId] || [];
}

/* ---------- Dashboard ---------- */

async function loadDashboardStats() {
    try {
        const response = await fetch(`${API_BASE}/stats`);
        if (response.ok) {
            const data = await response.json();
            if (data.status) {
                document.getElementById('stat-sellers').textContent = data.pendingSellers;
                document.getElementById('stat-products').textContent = data.pendingProducts;
                document.getElementById('stat-users').textContent = data.totalUsers;
                document.getElementById('stat-all-products').textContent = data.totalProducts;
            }
        }
    } catch (e) {
        console.error('Failed to load dashboard stats:', e);
    }
}

/* ---------- Seller Applications ---------- */

async function loadSellerApplications() {
    const tbody = document.getElementById('seller-applications-body');
    tbody.innerHTML = '<tr><td colspan="8" class="text-center text-muted py-4">Loading...</td></tr>';

    try {
        const response = await fetch(`${API_BASE}/seller-applications`);
        if (response.ok) {
            const data = await response.json();
            tbody.innerHTML = '';

            if (data.sellers && data.sellers.length > 0) {
                data.sellers.forEach(seller => {
                    const row = document.createElement('tr');
                    row.innerHTML = `
                        <td><input class="form-check-input select-item" type="checkbox" value="${seller.id}" onchange="updateSelCount('seller-applications-body','sellerSelCount','sellerApproveBtn','sellerReactivateBtn')"></td>
                        <td>${seller.id}</td>
                        <td>${seller.companyName}</td>
                        <td>${seller.companyEmail}</td>
                        <td>${seller.companyMobile}</td>
                        <td>${seller.city || '-'}</td>
                        <td>${seller.appliedOn ? seller.appliedOn.split('T')[0] : '-'}</td>
                        <td><span class="badge badge-${seller.status === 'APPROVED' ? 'approved' : seller.status === 'REJECTED' ? 'rejected' : 'pending'}">${seller.status}</span></td>
                    `;
                    tbody.appendChild(row);
                });
            } else {
                tbody.innerHTML = '<tr><td colspan="8" class="text-center text-muted py-4">No seller applications found</td></tr>';
            }
        } else {
            tbody.innerHTML = '<tr><td colspan="8" class="text-center text-muted py-4">Failed to load data</td></tr>';
        }
    } catch (e) {
        tbody.innerHTML = '<tr><td colspan="8" class="text-center text-muted py-4">Error loading data</td></tr>';
    }
    document.getElementById('sellerSelectAll').checked = false;
    updateSelCount('seller-applications-body', 'sellerSelCount', 'sellerApproveBtn', 'sellerReactivateBtn');
}

async function batchApproveSellers() {
    const ids = getSelectedIds('seller-applications-body');
    if (ids.length === 0) return;
    Notiflix.Confirm.show(
        'Approve Sellers',
        `Approve ${ids.length} seller account(s)?`,
        'Yes, Approve',
        'Cancel',
        async () => {
            Notiflix.Loading.standard('Approving...');
            let success = 0, fail = 0;
            for (const id of ids) {
                try {
                    const r = await fetch(`${API_BASE}/seller-applications/approve`, {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ id })
                    });
                    const result = await r.json();
                    if (result.status) success++; else fail++;
                } catch { fail++; }
            }
            Notiflix.Loading.remove();
            if (fail === 0) Notiflix.Notify.success(`${success} seller(s) approved`);
            else Notiflix.Notify.failure(`${success} approved, ${fail} failed`);
            loadSellerApplications();
            loadDashboardStats();
        }
    );
}

async function batchReactivateSellers() {
    const ids = getSelectedIds('seller-applications-body');
    if (ids.length === 0) return;
    Notiflix.Confirm.show(
        'Reactivate Sellers',
        `Reactivate ${ids.length} seller account(s)?`,
        'Yes, Reactivate',
        'Cancel',
        async () => {
            Notiflix.Loading.standard('Reactivating...');
            let success = 0, fail = 0;
            for (const id of ids) {
                try {
                    const r = await fetch(`${API_BASE}/seller-applications/reactivate`, {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ id })
                    });
                    const result = await r.json();
                    if (result.status) success++; else fail++;
                } catch { fail++; }
            }
            Notiflix.Loading.remove();
            if (fail === 0) Notiflix.Notify.success(`${success} seller(s) reactivated`);
            else Notiflix.Notify.failure(`${success} reactivated, ${fail} failed`);
            loadSellerApplications();
            loadDashboardStats();
        }
    );
}

/* ---------- Product Approvals ---------- */

async function loadProductApprovals() {
    const tbody = document.getElementById('product-approvals-body');
    tbody.innerHTML = '<tr><td colspan="8" class="text-center text-muted py-4">Loading...</td></tr>';

    try {
        const response = await fetch(`${API_BASE}/product-approvals`);
        if (response.ok) {
            const data = await response.json();
            tbody.innerHTML = '';

            if (data.products && data.products.length > 0) {
                data.products.forEach(product => {
                    const row = document.createElement('tr');
                    row.innerHTML = `
                        <td><input class="form-check-input select-item" type="checkbox" value="${product.id}" onchange="updateSelCount('product-approvals-body','prodApprovalSelCount','prodApprovalApproveBtn','prodApprovalReactivateBtn')"></td>
                        <td>${product.id}</td>
                        <td>${product.title}</td>
                        <td>${product.seller || '-'}</td>
                        <td>${product.category || '-'}</td>
                        <td>${product.price}</td>
                        <td>${product.submittedOn ? product.submittedOn.split('T')[0] : '-'}</td>
                        <td><span class="badge badge-${product.status === 'APPROVED' ? 'approved' : product.status === 'REJECTED' ? 'rejected' : 'pending'}">${product.status}</span></td>
                    `;
                    tbody.appendChild(row);
                });
            } else {
                tbody.innerHTML = '<tr><td colspan="8" class="text-center text-muted py-4">No products found</td></tr>';
            }
        } else {
            tbody.innerHTML = '<tr><td colspan="8" class="text-center text-muted py-4">Failed to load data</td></tr>';
        }
    } catch (e) {
        tbody.innerHTML = '<tr><td colspan="8" class="text-center text-muted py-4">Error loading data</td></tr>';
    }
    document.getElementById('prodApprovalSelectAll').checked = false;
    updateSelCount('product-approvals-body', 'prodApprovalSelCount', 'prodApprovalApproveBtn', 'prodApprovalReactivateBtn');
}

async function batchApproveProducts() {
    const ids = getSelectedIds('product-approvals-body');
    if (ids.length === 0) return;
    Notiflix.Confirm.show(
        'Approve Products',
        `Approve ${ids.length} product(s)?`,
        'Yes, Approve',
        'Cancel',
        async () => {
            Notiflix.Loading.standard('Approving...');
            let success = 0, fail = 0;
            for (const id of ids) {
                try {
                    const r = await fetch(`${API_BASE}/product-approvals/approve`, {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ id })
                    });
                    const result = await r.json();
                    if (result.status) success++; else fail++;
                } catch { fail++; }
            }
            Notiflix.Loading.remove();
            if (fail === 0) Notiflix.Notify.success(`${success} product(s) approved`);
            else Notiflix.Notify.failure(`${success} approved, ${fail} failed`);
            loadProductApprovals();
            loadDashboardStats();
        }
    );
}

async function batchReactivateApprovalProducts() {
    const ids = getSelectedIds('product-approvals-body');
    if (ids.length === 0) return;
    Notiflix.Confirm.show(
        'Remove Products',
        `Remove ${ids.length} product(s) from listing?`,
        'Yes, Remove',
        'Cancel',
        async () => {
            Notiflix.Loading.standard('Removing...');
            let success = 0, fail = 0;
            for (const id of ids) {
                try {
                    const r = await fetch(`${API_BASE}/product-approvals/reactivate`, {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ id })
                    });
                    const result = await r.json();
                    if (result.status) success++; else fail++;
                } catch { fail++; }
            }
            Notiflix.Loading.remove();
            if (fail === 0) Notiflix.Notify.success(`${success} product(s) removed`);
            else Notiflix.Notify.failure(`${success} removed, ${fail} failed`);
            loadProductApprovals();
            loadDashboardStats();
        }
    );
}

/* ---------- Manage Users ---------- */

async function loadUsers() {
    const tbody = document.getElementById('manage-users-body');
    tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted py-4">Loading...</td></tr>';

    try {
        const response = await fetch(`${API_BASE}/users`);
        if (response.ok) {
            const data = await response.json();
            tbody.innerHTML = '';

            if (data.users && data.users.length > 0) {
                data.users.forEach(user => {
                    const row = document.createElement('tr');
                    const initials = (user.firstName.charAt(0) + user.lastName.charAt(0)).toUpperCase();
                    row.innerHTML = `
                        <td><input class="form-check-input select-item" type="checkbox" value="${user.id}" onchange="updateSelCount('manage-users-body','userSelCount','userBlockBtn','userRemoveBtn','userReactivateBtn')"></td>
                        <td>${user.id}</td>
                        <td>
                            <div class="d-flex align-items-center gap-2">
                                <div class="user-avatar">${initials}</div>
                                <span>${user.firstName} ${user.lastName}</span>
                            </div>
                        </td>
                        <td>${user.email}</td>
                        <td>${user.mobile}</td>
                        <td><span class="badge badge-${user.status === 'VERIFIED' ? 'approved' : user.status === 'INACTIVE' ? 'rejected' : 'pending'}">${user.status}</span></td>
                        <td>${user.joined ? user.joined.split('T')[0] : '-'}</td>
                    `;
                    tbody.appendChild(row);
                });
            } else {
                tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted py-4">No users found</td></tr>';
            }
        } else {
            tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted py-4">Failed to load data</td></tr>';
        }
    } catch (e) {
        tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted py-4">Error loading data</td></tr>';
    }
    document.getElementById('userSelectAll').checked = false;
    updateSelCount('manage-users-body', 'userSelCount', 'userBlockBtn', 'userRemoveBtn', 'userReactivateBtn');
}

async function batchBlockUsers() {
    const ids = getSelectedIds('manage-users-body');
    if (ids.length === 0) return;
    batchUserStatus(ids, 'BLOCKED', 'Block');
}

async function batchRemoveUsers() {
    const ids = getSelectedIds('manage-users-body');
    if (ids.length === 0) return;
    batchUserStatus(ids, 'INACTIVE', 'Deactivate');
}

async function batchReactivateUsers() {
    const ids = getSelectedIds('manage-users-body');
    if (ids.length === 0) return;
    batchUserStatus(ids, 'VERIFIED', 'Reactivate');
}

async function batchUserStatus(ids, status, label) {
    Notiflix.Confirm.show(
        `${label} Users`,
        `${label} ${ids.length} user(s)?`,
        `Yes, ${label}`,
        'Cancel',
        async () => {
            Notiflix.Loading.standard(`${label}ing...`);
            let success = 0, fail = 0;
            for (const id of ids) {
                try {
                    const r = await fetch(`${API_BASE}/users/update-status`, {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ id, status })
                    });
                    const result = await r.json();
                    if (result.status) success++; else fail++;
                } catch { fail++; }
            }
            Notiflix.Loading.remove();
            if (fail === 0) Notiflix.Notify.success(`${success} user(s) ${label.toLowerCase()}ed`);
            else Notiflix.Notify.failure(`${success} ${label.toLowerCase()}ed, ${fail} failed`);
            loadUsers();
        }
    );
}

/* ---------- Manage Products ---------- */

async function loadProducts() {
    const tbody = document.getElementById('manage-products-body');
    tbody.innerHTML = '<tr><td colspan="8" class="text-center text-muted py-4">Loading...</td></tr>';

    try {
        const response = await fetch(`${API_BASE}/products`);
        if (response.ok) {
            const data = await response.json();
            tbody.innerHTML = '';

            if (data.products && data.products.length > 0) {
                data.products.forEach(product => {
                    const row = document.createElement('tr');
                    row.innerHTML = `
                        <td><input class="form-check-input select-item" type="checkbox" value="${product.id}" onchange="updateSelCount('manage-products-body','prodSelCount','prodRemoveBtn','prodReactivateBtn')"></td>
                        <td>${product.id}</td>
                        <td>${product.title}</td>
                        <td>${product.seller || '-'}</td>
                        <td>${product.category || '-'}</td>
                        <td>${product.price}</td>
                        <td>${product.stock}</td>
                        <td><span class="badge badge-${product.status === 'APPROVED' ? 'approved' : product.status === 'INACTIVE' ? 'rejected' : 'pending'}">${product.status}</span></td>
                    `;
                    tbody.appendChild(row);
                });
            } else {
                tbody.innerHTML = '<tr><td colspan="8" class="text-center text-muted py-4">No products found</td></tr>';
            }
        } else {
            tbody.innerHTML = '<tr><td colspan="8" class="text-center text-muted py-4">Failed to load data</td></tr>';
        }
    } catch (e) {
        tbody.innerHTML = '<tr><td colspan="8" class="text-center text-muted py-4">Error loading data</td></tr>';
    }
    document.getElementById('prodSelectAll').checked = false;
    updateSelCount('manage-products-body', 'prodSelCount', 'prodRemoveBtn', 'prodReactivateBtn');
}

async function batchRemoveProducts() {
    const ids = getSelectedIds('manage-products-body');
    if (ids.length === 0) return;
    Notiflix.Confirm.show(
        'Remove Products',
        `Remove ${ids.length} product(s) from marketplace?`,
        'Yes, Remove',
        'Cancel',
        async () => {
            Notiflix.Loading.standard('Removing...');
            let success = 0, fail = 0;
            for (const id of ids) {
                try {
                    const r = await fetch(`${API_BASE}/products/remove`, {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ id })
                    });
                    const result = await r.json();
                    if (result.status) success++; else fail++;
                } catch { fail++; }
            }
            Notiflix.Loading.remove();
            if (fail === 0) Notiflix.Notify.success(`${success} product(s) removed`);
            else Notiflix.Notify.failure(`${success} removed, ${fail} failed`);
            loadProducts();
            loadDashboardStats();
        }
    );
}

async function batchReactivateProducts() {
    const ids = getSelectedIds('manage-products-body');
    if (ids.length === 0) return;
    Notiflix.Confirm.show(
        'Reactivate Products',
        `Reactivate ${ids.length} product(s)?`,
        'Yes, Reactivate',
        'Cancel',
        async () => {
            Notiflix.Loading.standard('Reactivating...');
            let success = 0, fail = 0;
            for (const id of ids) {
                try {
                    const r = await fetch(`${API_BASE}/products/reactivate`, {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ id })
                    });
                    const result = await r.json();
                    if (result.status) success++; else fail++;
                } catch { fail++; }
            }
            Notiflix.Loading.remove();
            if (fail === 0) Notiflix.Notify.success(`${success} product(s) reactivated`);
            else Notiflix.Notify.failure(`${success} reactivated, ${fail} failed`);
            loadProducts();
            loadDashboardStats();
        }
    );
}

/* ---------- Filter ---------- */

function filterTable(tbodyId, query) {
    const rows = document.querySelectorAll(`#${tbodyId} tr`);
    const q = query.toLowerCase();
    rows.forEach(row => {
        if (row.querySelector('.select-item')) {
            row.style.display = row.textContent.toLowerCase().includes(q) ? '' : 'none';
        }
    });
}

/* ---------- Export Report ---------- */

function exportReport(type) {
    Notiflix.Loading.standard('Generating report...');
    const filenameMap = {
        sellers: 'seller_applications.pdf',
        'product-approvals': 'product_approvals.pdf',
        users: 'users.pdf',
        products: 'products.pdf'
    };
    const filename = filenameMap[type] || 'report.pdf';
    fetch(`${API_BASE}/../report/${type}`)
        .then(response => {
            if (!response.ok) throw new Error('Failed to generate report');
            return response.blob();
        })
        .then(blob => {
            Notiflix.Loading.remove();
            const url = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = filename;
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            URL.revokeObjectURL(url);
        })
        .catch(() => {
            Notiflix.Loading.remove();
            Notiflix.Notify.failure('Failed to generate report');
        });
}

/* ---------- Logout ---------- */

function signOut() {
    Notiflix.Confirm.show(
        'Logout',
        'Are you sure you want to logout?',
        'Yes, Logout',
        'Cancel',
        async () => {
            try {
                const response = await fetch('api/admin/logout');
                if (response.ok) {
                    window.location.href = 'admin_sign_in.html';
                } else {
                    Notiflix.Notify.failure('Logout failed');
                }
            } catch (e) {
                Notiflix.Notify.failure('Error during logout');
            }
        }
    );
}
