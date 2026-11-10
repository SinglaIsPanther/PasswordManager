/**
 * Secure Password Manager - Chrome Web Client Engine
 * 100% Offline-capable, Real-time WebSocket Synchronization & AES-256 REST API
 */

(function () {
    'use strict';

    // State Variables
    let deviceIdentifier = localStorage.getItem('spm_device_id');
    if (!deviceIdentifier) {
        deviceIdentifier = 'chrome_' + Math.random().toString(36).substring(2, 10) + '_' + Date.now();
        localStorage.setItem('spm_device_id', deviceIdentifier);
    }

    let sessionToken = sessionStorage.getItem('spm_session_token');
    let currentSort = 'RECENT';
    let searchQuery = '';
    let pollingInterval = null;
    let webSocket = null;
    let clipboardClearTimer = null;
    let pendingScreenshotBase64 = null;
    let pendingScreenshotMimeType = null;

    // DOM Elements
    const viewPairing = document.getElementById('viewPairing');
    const viewLogin = document.getElementById('viewLogin');
    const viewDashboard = document.getElementById('viewDashboard');

    const inputPairingCode = document.getElementById('inputPairingCode');
    const btnSubmitPairing = document.getElementById('btnSubmitPairing');
    const pairingStatusBox = document.getElementById('pairingStatusBox');
    const pairingStatusText = document.getElementById('pairingStatusText');

    const inputMasterPassword = document.getElementById('inputMasterPassword');
    const btnToggleMasterPw = document.getElementById('btnToggleMasterPw');
    const btnSubmitLogin = document.getElementById('btnSubmitLogin');
    const loginErrorBox = document.getElementById('loginErrorBox');

    const syncIndicator = document.getElementById('syncIndicator');
    const syncText = document.getElementById('syncText');
    const btnManualSync = document.getElementById('btnManualSync');

    const statTotalCount = document.getElementById('statTotalCount');
    const statFavCount = document.getElementById('statFavCount');
    const tableBody = document.getElementById('tableBodyPasswords');
    const emptyState = document.getElementById('emptyState');
    const inputSearch = document.getElementById('inputSearch');
    const btnClearSearch = document.getElementById('btnClearSearch');

    const btnHeaderAdd = document.getElementById('btnHeaderAdd');
    const btnHeaderGenerator = document.getElementById('btnHeaderGenerator');
    const btnHeaderLogout = document.getElementById('btnHeaderLogout');
    const btnEmptyAdd = document.getElementById('btnEmptyAdd');

    // Modals
    const modalAddEdit = document.getElementById('modalAddEdit');
    const formAddEdit = document.getElementById('formAddEdit');
    const modalDetails = document.getElementById('modalDetails');
    const modalGenerator = document.getElementById('modalGenerator');

    // =========================================================================
    // Initialization & State Router
    // =========================================================================
    document.addEventListener('DOMContentLoaded', () => {
        setupEventListeners();
        checkServerAndSession();
        checkUrlForPairingCode();
    });

    function checkUrlForPairingCode() {
        try {
            let urlCode = null;
            if (window.location.hash) {
                const hashMatch = window.location.hash.match(/pairing_code=([0-9]{6})/);
                if (hashMatch) urlCode = hashMatch[1];
            }
            if (!urlCode && window.location.search) {
                const params = new URLSearchParams(window.location.search);
                urlCode = params.get('code') || params.get('pairing_code');
            }
            if (urlCode && urlCode.length === 6 && inputPairingCode) {
                inputPairingCode.value = urlCode;
                if (!sessionToken) {
                    showView('PAIRING');
                    setTimeout(() => {
                        handlePairingSubmit();
                    }, 500);
                }
            }
        } catch (ignored) {}
    }

    function checkServerAndSession() {
        if (!sessionToken) {
            // कोई टोकन नहीं है -> पेयरिंग स्क्रीन दिखाएं
            showView('PAIRING');
        } else {
            // टोकन मौजूद है -> पासवर्ड लोड करने का प्रयास करें
            fetchPasswords()
                .then(() => {
                    showView('DASHBOARD');
                    initWebSocketSync();
                })
                .catch(err => {
                    if (err && err.error === 'VAULT_LOCKED') {
                        showView('LOGIN');
                    } else {
                        // टोकन अमान्य है
                        sessionToken = null;
                        sessionStorage.removeItem('spm_session_token');
                        showView('PAIRING');
                    }
                });
        }
    }

    function setSyncStatus(status, customText) {
        if (!syncIndicator || !syncText) return;
        if (status === 'syncing') {
            syncIndicator.className = 'sync-pill sync-syncing';
            syncText.textContent = customText || 'सिंक हो रहा है...';
        } else if (status === 'synced') {
            syncIndicator.className = 'sync-pill sync-connected';
            syncText.textContent = customText || 'सिंक पूर्ण ✓';
        } else if (status === 'disconnected') {
            syncIndicator.className = 'sync-pill sync-disconnected';
            syncText.textContent = customText || 'ऑफलाइन';
        } else {
            syncIndicator.className = 'sync-pill sync-connected';
            syncText.textContent = customText || 'सिंक तैयार';
        }
    }

    function showView(viewName) {
        viewPairing.style.display = (viewName === 'PAIRING') ? 'block' : 'none';
        viewLogin.style.display = (viewName === 'LOGIN') ? 'block' : 'none';
        viewDashboard.style.display = (viewName === 'DASHBOARD') ? 'block' : 'none';

        if (btnManualSync) btnManualSync.style.display = (viewName === 'DASHBOARD') ? 'inline-flex' : 'none';
        btnHeaderAdd.style.display = (viewName === 'DASHBOARD') ? 'inline-flex' : 'none';
        btnHeaderGenerator.style.display = (viewName === 'DASHBOARD') ? 'inline-flex' : 'none';
        btnHeaderLogout.style.display = (viewName === 'DASHBOARD') ? 'inline-flex' : 'none';
    }

    // =========================================================================
    // Pairing Workflow
    // =========================================================================
    function handlePairingSubmit() {
        const code = inputPairingCode.value.trim();
        if (code.length !== 6) {
            showToast('कृपया 6-अंकीय मान्य पेयरिंग कोड दर्ज करें!');
            inputPairingCode.focus();
            return;
        }

        btnSubmitPairing.disabled = true;
        pairingStatusBox.style.display = 'flex';
        pairingStatusText.textContent = 'मोबाइल स्क्रीन पर अनुमोदन (Approval) की प्रतीक्षा की जा रही है...';

        fetch('/api/pairing/request', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                device_name: 'Chrome on ' + (navigator.platform || 'Computer'),
                device_type: 'Desktop Chrome',
                device_identifier: deviceIdentifier,
                pairing_code: code
            })
        })
        .then(r => r.json())
        .then(data => {
            if (data.status === 'PENDING') {
                startPollingPairingApproval();
            } else {
                throw new Error(data.message || 'पेयरिंग कोड अमान्य है।');
            }
        })
        .catch(err => {
            btnSubmitPairing.disabled = false;
            pairingStatusBox.style.display = 'none';
            showToast('त्रुटि: ' + err.message);
        });
    }

    function startPollingPairingApproval() {
        if (pollingInterval) clearInterval(pollingInterval);

        pollingInterval = setInterval(() => {
            fetch('/api/pairing/status?device_identifier=' + encodeURIComponent(deviceIdentifier))
                .then(r => r.json())
                .then(data => {
                    if (data.status === 'APPROVED' && data.session_token) {
                        clearInterval(pollingInterval);
                        sessionToken = data.session_token;
                        sessionStorage.setItem('spm_session_token', sessionToken);
                        pairingStatusBox.style.display = 'none';
                        showToast('कंप्यूटर सफलतापूर्वक पेयर हो गया!');
                        checkServerAndSession();
                    } else if (data.status === 'REJECTED') {
                        clearInterval(pollingInterval);
                        btnSubmitPairing.disabled = false;
                        pairingStatusBox.style.display = 'none';
                        showToast('मोबाइल द्वारा पेयरिंग अनुरोध अस्वीकार कर दिया गया।');
                    }
                })
                .catch(() => {});
        }, 1500);
    }

    // =========================================================================
    // Master Password Login Workflow
    // =========================================================================
    function handleLoginSubmit() {
        const masterPw = inputMasterPassword.value.trim();
        loginErrorBox.style.display = 'none';

        if (!masterPw) {
            loginErrorBox.textContent = 'कृपया मास्टर पासवर्ड दर्ज करें!';
            loginErrorBox.style.display = 'block';
            return;
        }

        btnSubmitLogin.disabled = true;
        fetch('/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                master_password: masterPw,
                device_identifier: deviceIdentifier,
                device_name: 'Chrome on PC'
            })
        })
        .then(r => r.json())
        .then(data => {
            btnSubmitLogin.disabled = false;
            if (data.success && data.session_token) {
                sessionToken = data.session_token;
                sessionStorage.setItem('spm_session_token', sessionToken);
                inputMasterPassword.value = '';
                showToast('तिजोरी सफलतापूर्वक अनलॉक हुई!');
                showView('DASHBOARD');
                fetchPasswords();
                initWebSocketSync();
            } else {
                loginErrorBox.textContent = data.message || 'गलत मास्टर पासवर्ड!';
                loginErrorBox.style.display = 'block';
            }
        })
        .catch(err => {
            btnSubmitLogin.disabled = false;
            loginErrorBox.textContent = 'कनेक्शन त्रुटि: ' + err.message;
            loginErrorBox.style.display = 'block';
        });
    }

    // =========================================================================
    // Passwords API & Table Rendering
    // =========================================================================
    function fetchPasswords() {
        let url = '/api/passwords?sort=' + encodeURIComponent(currentSort) + '&_t=' + Date.now();
        if (searchQuery) {
            url += '&q=' + encodeURIComponent(searchQuery);
        }

        return fetch(url, {
            cache: 'no-store',
            headers: {
                'Authorization': 'Bearer ' + sessionToken,
                'X-Requested-With': 'XMLHttpRequest',
                'Cache-Control': 'no-cache, no-store, must-revalidate',
                'Pragma': 'no-cache'
            }
        })
        .then(r => {
            if (!r.ok) {
                return r.json().then(e => { throw e; });
            }
            return r.json();
        })
        .then(data => {
            renderPasswordsTable(data.records || []);
            updateStatsCounters(data.records || []);
            return data;
        });
    }

    function renderPasswordsTable(records) {
        tableBody.innerHTML = '';

        if (!records || records.length === 0) {
            emptyState.style.display = 'block';
            return;
        }

        emptyState.style.display = 'none';

        records.forEach(r => {
            const tr = document.createElement('tr');

            const initial = (r.website_name && r.website_name.length > 0) ? r.website_name[0].toUpperCase() : 'W';
            const hasImgBadge = r.has_screenshot ? '<span class="badge-has-img">📷 स्क्रीनशॉट</span>' : '';
            const roleBadge = r.role ? `<span class="badge-role">${escapeHtml(r.role)}</span>` : '-';
            const favIcon = (r.is_favorite === 1) ? '⭐' : '☆';

            let siteUrlHtml = '-';
            if (r.website_url) {
                siteUrlHtml = `<a href="${escapeHtml(r.website_url)}" target="_blank" rel="noopener" class="site-url">${escapeHtml(r.website_url)} ↗</a>`;
            }

            tr.innerHTML = `
                <td>
                    <div class="website-cell">
                        <div class="site-avatar">${initial}</div>
                        <div>
                            <div class="site-title">${escapeHtml(r.website_name)}</div>
                            ${siteUrlHtml}
                        </div>
                    </div>
                </td>
                <td><strong>${escapeHtml(r.username || '')}</strong></td>
                <td>${roleBadge}</td>
                <td><span class="pw-masked">••••••••••••</span></td>
                <td>${hasImgBadge}</td>
                <td style="text-align: right;">
                    <div class="actions-cell">
                        <button class="btn btn-sm btn-outline btn-action-view" data-id="${r.id}" title="विवरण देखें">विवरण</button>
                        <button class="btn btn-sm btn-outline btn-action-copy-user" data-user="${escapeHtml(r.username || '')}" title="यूजरनेम कॉपी">User</button>
                        <button class="btn btn-sm btn-success btn-action-copy-pw" data-id="${r.id}" title="पासवर्ड कॉपी">Pass</button>
                        <button class="btn-icon btn-action-fav" data-id="${r.id}" title="पसंदीदा">${favIcon}</button>
                    </div>
                </td>
            `;

            tableBody.appendChild(tr);
        });

        attachTableActionListeners();
    }

    function updateStatsCounters(records) {
        statTotalCount.textContent = records.length;
        const favs = records.filter(r => r.is_favorite === 1).length;
        statFavCount.textContent = favs;
    }

    function attachTableActionListeners() {
        document.querySelectorAll('.btn-action-view').forEach(b => {
            b.onclick = () => openDetailsModal(b.dataset.id);
        });

        document.querySelectorAll('.btn-action-copy-user').forEach(b => {
            b.onclick = () => {
                copyTextToClipboard(b.dataset.user, 'यूजरनेम कॉपी किया गया!');
            };
        });

        document.querySelectorAll('.btn-action-copy-pw').forEach(b => {
            b.onclick = () => copyPasswordWithAutoClear(b.dataset.id);
        });

        document.querySelectorAll('.btn-action-fav').forEach(b => {
            b.onclick = () => toggleFavorite(b.dataset.id);
        });
    }

    // =========================================================================
    // Copy Password with 30-Second Auto-Clear Feature
    // =========================================================================
    function copyPasswordWithAutoClear(recordId) {
        fetch('/api/passwords/' + recordId, {
            headers: {
                'Authorization': 'Bearer ' + sessionToken,
                'X-Requested-With': 'XMLHttpRequest'
            }
        })
        .then(r => r.json())
        .then(data => {
            if (data.password) {
                copyTextToClipboard(data.password, 'पासवर्ड कॉपी किया गया! (30 सेकंड में स्वतः साफ़ हो जाएगा)');
                scheduleClipboardClear(data.password);
            }
        })
        .catch(() => showToast('पासवर्ड लोड करने में त्रुटि!'));
    }

    function copyTextToClipboard(text, successMsg) {
        if (!text) return;
        navigator.clipboard.writeText(text).then(() => {
            showToast(successMsg);
        }).catch(() => {
            // Fallback
            const el = document.createElement('textarea');
            el.value = text;
            document.body.appendChild(el);
            el.select();
            document.execCommand('copy');
            document.body.removeChild(el);
            showToast(successMsg);
        });
    }

    function scheduleClipboardClear(copiedPassword) {
        if (clipboardClearTimer) clearTimeout(clipboardClearTimer);

        clipboardClearTimer = setTimeout(() => {
            navigator.clipboard.readText().then(currentClip => {
                if (currentClip === copiedPassword) {
                    navigator.clipboard.writeText('');
                    showToast('सुरक्षा: क्लिपबोर्ड से पासवर्ड साफ़ कर दिया गया।');
                }
            }).catch(() => {
                navigator.clipboard.writeText('');
            });
        }, 30000); // 30 सेकंड
    }

    // =========================================================================
    // Add / Edit Record Submission
    // =========================================================================
    function openAddModal() {
        document.getElementById('modalAddEditTitle').textContent = 'नया पासवर्ड जोड़ें';
        document.getElementById('formRecordId').value = '';
        document.getElementById('formRecordVersion').value = '1';
        document.getElementById('formWebsiteName').value = '';
        document.getElementById('formWebsiteUrl').value = '';
        document.getElementById('formUsername').value = '';
        document.getElementById('formPassword').value = '';
        document.getElementById('formRole').value = '';
        document.getElementById('formNotes').value = '';
        document.getElementById('formIsFavorite').checked = false;

        pendingScreenshotBase64 = null;
        pendingScreenshotMimeType = null;
        document.getElementById('formScreenshotInput').value = '';
        document.getElementById('screenshotPreviewBox').style.display = 'none';

        modalAddEdit.style.display = 'flex';
    }

    function openEditModal(recordId) {
        fetch('/api/passwords/' + recordId, {
            headers: {
                'Authorization': 'Bearer ' + sessionToken,
                'X-Requested-With': 'XMLHttpRequest'
            }
        })
        .then(r => r.json())
        .then(data => {
            document.getElementById('modalAddEditTitle').textContent = 'पासवर्ड विवरण संपादित करें';
            document.getElementById('formRecordId').value = data.id;
            document.getElementById('formRecordVersion').value = data.record_version || 1;
            document.getElementById('formWebsiteName').value = data.website_name || '';
            document.getElementById('formWebsiteUrl').value = data.website_url || '';
            document.getElementById('formUsername').value = data.username || '';
            document.getElementById('formPassword').value = data.password || '';
            document.getElementById('formRole').value = data.role || '';
            document.getElementById('formNotes').value = data.notes || '';
            document.getElementById('formIsFavorite').checked = (data.is_favorite === 1);

            pendingScreenshotBase64 = null;
            pendingScreenshotMimeType = null;
            document.getElementById('formScreenshotInput').value = '';

            if (data.has_screenshot) {
                const img = document.getElementById('screenshotPreviewImg');
                img.src = '/api/passwords/' + data.id + '/screenshot?token=' + encodeURIComponent(sessionToken);
                document.getElementById('screenshotPreviewBox').style.display = 'block';
            } else {
                document.getElementById('screenshotPreviewBox').style.display = 'none';
            }

            modalDetails.style.display = 'none';
            modalAddEdit.style.display = 'flex';
        })
        .catch(() => showToast('रिकॉर्ड लोड करने में त्रुटि!'));
    }

    function handleFormSubmit(e) {
        e.preventDefault();

        const recordId = document.getElementById('formRecordId').value;
        const recordVersion = parseInt(document.getElementById('formRecordVersion').value, 10) || 1;

        const websiteName = document.getElementById('formWebsiteName').value.trim();
        const username = document.getElementById('formUsername').value.trim();
        const password = document.getElementById('formPassword').value.trim();

        if (!websiteName || !username || !password) {
            showToast('कृपया वेबसाइट का नाम, यूजरनेम और पासवर्ड अवश्य भरें!');
            return;
        }

        const payload = {
            website_name: websiteName,
            website_url: document.getElementById('formWebsiteUrl').value.trim(),
            username: username,
            password: password,
            role: document.getElementById('formRole').value.trim(),
            notes: document.getElementById('formNotes').value.trim(),
            is_favorite: document.getElementById('formIsFavorite').checked ? 1 : 0,
            record_version: recordVersion
        };

        if (pendingScreenshotBase64) {
            payload.screenshot_base64 = pendingScreenshotBase64;
            payload.screenshot_mime_type = pendingScreenshotMimeType;
        }

        const method = recordId ? 'PUT' : 'POST';
        const url = recordId ? '/api/passwords/' + recordId : '/api/passwords';

        const btnSave = document.getElementById('btnSaveRecord');
        if (btnSave) {
            btnSave.disabled = true;
            btnSave.textContent = 'सुरक्षित व सिंक हो रहा है...';
        }
        setSyncStatus('syncing', 'डेटा सिंक हो रहा है...');

        fetch(url, {
            method: method,
            cache: 'no-store',
            headers: {
                'Authorization': 'Bearer ' + sessionToken,
                'Content-Type': 'application/json',
                'X-Requested-With': 'XMLHttpRequest',
                'Cache-Control': 'no-cache, no-store, must-revalidate'
            },
            body: JSON.stringify(payload)
        })
        .then(r => {
            if (!r.ok) {
                return r.json().then(e => { throw e; });
            }
            return r.json();
        })
        .then(data => {
            modalAddEdit.style.display = 'none';

            // फॉर्म रीसेट
            formAddEdit.reset();
            document.getElementById('formRecordId').value = '';
            pendingScreenshotBase64 = '';
            pendingScreenshotMimeType = null;
            const previewBox = document.getElementById('screenshotPreviewBox');
            if (previewBox) previewBox.style.display = 'none';

            // यदि सर्च बार में कोई टेक्स्ट था तो उसे साफ़ करें ताकि नया रिकॉर्ड तुरंत दिखाई दे
            if (inputSearch && inputSearch.value) {
                inputSearch.value = '';
                searchQuery = '';
                if (btnClearSearch) btnClearSearch.style.display = 'none';
            }

            showToast(data.message || (recordId ? 'पासवर्ड सफलतापूर्वक अपडेट व सिंक किया गया!' : 'नया पासवर्ड सफलतापूर्वक सुरक्षित व सिंक किया गया!'));
            
            // तत्काल ताज़ा डेटा लोड करें (बिना ब्राउज़र रिफ्रेश किए)
            return fetchPasswords();
        })
        .then(() => {
            setSyncStatus('synced', 'सिंक पूर्ण ✓');
            setTimeout(() => setSyncStatus('ready'), 3500);
        })
        .catch(err => {
            setSyncStatus('ready');
            showToast('त्रुटि: ' + (err.message || 'सेव करने में विफलता।'));
        })
        .finally(() => {
            if (btnSave) {
                btnSave.disabled = false;
                btnSave.textContent = 'सुरक्षित करें (Save)';
            }
        });
    }

    // =========================================================================
    // View Details Modal
    // =========================================================================
    function openDetailsModal(recordId) {
        fetch('/api/passwords/' + recordId, {
            headers: {
                'Authorization': 'Bearer ' + sessionToken,
                'X-Requested-With': 'XMLHttpRequest'
            }
        })
        .then(r => r.json())
        .then(data => {
            document.getElementById('detWebsiteName').textContent = data.website_name || '-';

            const urlA = document.getElementById('detWebsiteUrl');
            if (data.website_url) {
                urlA.href = data.website_url;
                urlA.textContent = data.website_url;
                document.getElementById('detUrlContainer').style.display = 'block';
            } else {
                document.getElementById('detUrlContainer').style.display = 'none';
            }

            document.getElementById('detUsername').textContent = data.username || '-';
            const detPwSpan = document.getElementById('detPassword');
            detPwSpan.textContent = '••••••••••••';
            detPwSpan.dataset.realPw = data.password || '';
            detPwSpan.dataset.visible = '0';
            document.getElementById('btnToggleDetPw').textContent = 'दिखाएं';

            document.getElementById('detRole').textContent = data.role || 'General';
            document.getElementById('detUpdatedAt').textContent = data.updated_at || '-';

            if (data.notes) {
                document.getElementById('detNotes').textContent = data.notes;
                document.getElementById('detNotesBox').style.display = 'block';
            } else {
                document.getElementById('detNotesBox').style.display = 'none';
            }

            // Screenshot Preview
            const imgBox = document.getElementById('detScreenshotBox');
            const img = document.getElementById('detScreenshotImg');
            if (data.has_screenshot) {
                img.src = '/api/passwords/' + data.id + '/screenshot?token=' + encodeURIComponent(sessionToken);
                imgBox.style.display = 'block';
            } else {
                imgBox.style.display = 'none';
            }

            // Buttons
            document.getElementById('btnCopyDetUser').onclick = () => {
                copyTextToClipboard(data.username, 'यूजरनेम कॉपी किया गया!');
            };

            document.getElementById('btnCopyDetPw').onclick = () => {
                copyTextToClipboard(data.password, 'पासवर्ड कॉपी किया गया! (30 सेकंड में स्वतः साफ़ हो जाएगा)');
                scheduleClipboardClear(data.password);
            };

            document.getElementById('btnDetailsEdit').onclick = () => {
                openEditModal(data.id);
            };

            document.getElementById('btnDetailsDelete').onclick = () => {
                deleteRecord(data.id);
            };

            modalDetails.style.display = 'flex';
        })
        .catch(() => showToast('विवरण लोड करने में त्रुटि!'));
    }

    function toggleDetailsPassword() {
        const span = document.getElementById('detPassword');
        const btn = document.getElementById('btnToggleDetPw');
        if (span.dataset.visible === '1') {
            span.textContent = '••••••••••••';
            span.dataset.visible = '0';
            btn.textContent = 'दिखाएं';
        } else {
            span.textContent = span.dataset.realPw;
            span.dataset.visible = '1';
            btn.textContent = 'छिपाएं';
        }
    }

    function deleteRecord(recordId) {
        if (!confirm('क्या आप वाकई इस रिकॉर्ड को स्थायी रूप से हटाना चाहते हैं?')) {
            return;
        }

        setSyncStatus('syncing', 'हटाया व सिंक किया जा रहा है...');
        fetch('/api/passwords/' + recordId, {
            method: 'DELETE',
            cache: 'no-store',
            headers: {
                'Authorization': 'Bearer ' + sessionToken,
                'X-Requested-With': 'XMLHttpRequest'
            }
        })
        .then(r => r.json())
        .then(data => {
            modalDetails.style.display = 'none';
            showToast(data.message || 'रिकॉर्ड हटा दिया गया।');
            return fetchPasswords();
        })
        .then(() => {
            setSyncStatus('synced', 'सिंक पूर्ण ✓');
            setTimeout(() => setSyncStatus('ready'), 3000);
        })
        .catch(() => {
            setSyncStatus('ready');
            showToast('हटाने में त्रुटि!');
        });
    }

    function toggleFavorite(recordId) {
        setSyncStatus('syncing');
        fetch('/api/passwords/' + recordId + '/favorite', {
            method: 'POST',
            cache: 'no-store',
            headers: {
                'Authorization': 'Bearer ' + sessionToken,
                'X-Requested-With': 'XMLHttpRequest'
            }
        })
        .then(r => r.json())
        .then(() => fetchPasswords())
        .then(() => {
            setSyncStatus('synced', 'सिंक पूर्ण ✓');
            setTimeout(() => setSyncStatus('ready'), 2500);
        })
        .catch(() => setSyncStatus('ready'));
    }

    // =========================================================================
    // Synchronization Engine (On-Demand + Passive Real-Time)
    // =========================================================================
    let wsHeartbeatTimer = null;

    function initWebSocketSync() {
        if (!sessionToken) return;

        setSyncStatus('ready');

        // यदि WebSocket पहले से सक्रिय है तो कुछ न करें
        if (webSocket && (webSocket.readyState === WebSocket.OPEN || webSocket.readyState === WebSocket.CONNECTING)) {
            return;
        }

        const protocol = (window.location.protocol === 'https:') ? 'wss://' : 'ws://';
        const wsUrl = protocol + window.location.host + '/ws';

        try {
            webSocket = new WebSocket(wsUrl);

            webSocket.onopen = () => {
                setSyncStatus('ready');

                if (wsHeartbeatTimer) clearInterval(wsHeartbeatTimer);
                wsHeartbeatTimer = setInterval(() => {
                    if (webSocket && webSocket.readyState === WebSocket.OPEN) {
                        try { webSocket.send('ping'); } catch (e) {}
                    }
                }, 30000);
            };

            webSocket.onmessage = (event) => {
                try {
                    if (event.data === 'pong') return;

                    const data = JSON.parse(event.data);
                    if (data.event === 'RECORD_CHANGED') {
                        setSyncStatus('syncing', 'नया डेटा सिंक हो रहा है...');
                        fetchPasswords().then(() => {
                            setSyncStatus('synced', 'सिंक पूर्ण ✓');
                            setTimeout(() => setSyncStatus('ready'), 3000);
                        });
                        showToast(`लाइव अपडेट: "${data.website_name || 'रिकॉर्ड'}" अपडेट हुआ।`);
                    } else if (data.event === 'DEVICE_REVOKED') {
                        if (data.device_identifier === deviceIdentifier) {
                            alert('सुरक्षा सूचना: मोबाइल एप्लीकेशन द्वारा इस कंप्यूटर का एक्सेस समाप्त कर दिया गया है।');
                            sessionToken = null;
                            sessionStorage.removeItem('spm_session_token');
                            location.reload();
                        }
                    }
                } catch (ignored) {}
            };

            webSocket.onclose = () => {
                if (wsHeartbeatTimer) clearInterval(wsHeartbeatTimer);
                // ध्यान दें: WebSocket बंद होने पर लाल एरर या बार-बार रिकनेक्ट लूप नहीं चलाएंगे
                // स्थिति को 'सिंक तैयार' बनाए रखें क्योंकि ऑन-डिमांड REST सिंक हमेशा तैयार है!
                setSyncStatus('ready');
            };

            webSocket.onerror = () => {
                try { webSocket.close(); } catch (ignored) {}
                setSyncStatus('ready');
            };
        } catch (e) {
            setSyncStatus('ready');
        }
    }

    // =========================================================================
    // Password Generator Engine (Inline & Modal)
    // =========================================================================
    function generateNewPassword(length, upper, lower, nums, syms, saveSettings) {
        const payload = {};
        if (length !== undefined) payload.length = length;
        if (upper !== undefined) payload.uppercase = upper;
        if (lower !== undefined) payload.lowercase = lower;
        if (nums !== undefined) payload.numbers = nums;
        if (syms !== undefined) payload.symbols = syms;
        if (saveSettings) payload.saveSettings = true;

        fetch('/api/password-generator', {
            method: 'POST',
            headers: {
                'Authorization': 'Bearer ' + sessionToken,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(payload)
        })
        .then(r => r.json())
        .then(data => {
            if (data.password) {
                document.getElementById('genResultText').textContent = data.password;
            }
        })
        .catch(() => {});
    }

    // =========================================================================
    // Helpers & Event Listeners
    // =========================================================================
    function setupEventListeners() {
        // Pairing & Login
        btnSubmitPairing.onclick = handlePairingSubmit;
        inputPairingCode.onkeydown = (e) => { if (e.key === 'Enter') handlePairingSubmit(); };

        btnSubmitLogin.onclick = handleLoginSubmit;
        inputMasterPassword.onkeydown = (e) => { if (e.key === 'Enter') handleLoginSubmit(); };
        btnToggleMasterPw.onclick = () => {
            inputMasterPassword.type = (inputMasterPassword.type === 'password') ? 'text' : 'password';
        };

        // Header Actions
        btnHeaderAdd.onclick = openAddModal;
        btnEmptyAdd.onclick = openAddModal;
        btnHeaderGenerator.onclick = () => {
            modalGenerator.style.display = 'flex';
            fetch('/api/generator-settings', {
                headers: { 'Authorization': 'Bearer ' + sessionToken }
            })
            .then(r => r.json())
            .then(s => {
                if (s && s.length) {
                    document.getElementById('genLengthRange').value = s.length;
                    document.getElementById('genLengthVal').textContent = s.length;
                    document.getElementById('genUpper').checked = !!s.uppercase;
                    document.getElementById('genLower').checked = !!s.lowercase;
                    document.getElementById('genNumbers').checked = !!s.numbers;
                    document.getElementById('genSymbols').checked = !!s.symbols;
                }
                triggerModalGenerate(false);
            })
            .catch(() => triggerModalGenerate(false));
        };

        if (btnManualSync) {
            btnManualSync.onclick = () => {
                btnManualSync.disabled = true;
                btnManualSync.textContent = '🔄 सिंक हो रहा है...';
                setSyncStatus('syncing', 'डेटा सिंक हो रहा है...');
                fetchPasswords()
                    .then(() => {
                        setSyncStatus('synced', 'सिंक पूर्ण ✓');
                        showToast('डेटाबेस सफलतापूर्वक सिंक हो गया!');
                        setTimeout(() => setSyncStatus('ready'), 3000);
                    })
                    .catch(err => {
                        setSyncStatus('ready');
                        showToast('सिंक त्रुटि: ' + (err.message || 'नेटवर्क समस्या'));
                    })
                    .finally(() => {
                        btnManualSync.disabled = false;
                        btnManualSync.textContent = '🔄 सिंक करें';
                    });
            };
        }

        // विंडो या टैब पर वापस आने पर शांत बैकग्राउंड रिफ्रेश
        window.addEventListener('focus', () => {
            if (sessionToken && viewDashboard.style.display !== 'none') {
                fetchPasswords();
            }
        });

        btnHeaderLogout.onclick = () => {
            fetch('/api/auth/logout', {
                method: 'POST',
                headers: { 'Authorization': 'Bearer ' + sessionToken }
            }).finally(() => {
                sessionToken = null;
                sessionStorage.removeItem('spm_session_token');
                location.reload();
            });
        };

        // Search Input
        inputSearch.oninput = (e) => {
            searchQuery = e.target.value.trim();
            btnClearSearch.style.display = searchQuery ? 'block' : 'none';
            fetchPasswords();
        };

        btnClearSearch.onclick = () => {
            inputSearch.value = '';
            searchQuery = '';
            btnClearSearch.style.display = 'none';
            fetchPasswords();
        };

        // Filter Tabs
        document.querySelectorAll('.tab-btn').forEach(btn => {
            btn.onclick = () => {
                document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
                btn.classList.add('active');
                currentSort = btn.dataset.sort;
                fetchPasswords();
            };
        });

        // Form Submit
        formAddEdit.onsubmit = handleFormSubmit;

        // Form inline password generator (सर्वर/मोबाइल की सहेजी गई प्राथमिकताओं के अनुसार)
        document.getElementById('btnFormGenerate').onclick = () => {
            fetch('/api/password-generator', {
                method: 'POST',
                headers: { 'Authorization': 'Bearer ' + sessionToken, 'Content-Type': 'application/json' },
                body: JSON.stringify({}) // सर्वर में सहेजे गए नियमों (जैसे केवल अंक) के अनुसार जनरेट होगा
            })
            .then(r => r.json())
            .then(data => {
                if (data.password) {
                    document.getElementById('formPassword').value = data.password;
                    showToast('नया पासवर्ड जनरेट किया गया: ' + data.password);
                }
            })
            .catch(() => {
                showToast('पासवर्ड जनरेट करने में त्रुटि हुई!');
            });
        };

        // Screenshot file input & preview
        document.getElementById('formScreenshotInput').onchange = (e) => {
            const file = e.target.files[0];
            if (!file) return;

            pendingScreenshotMimeType = file.type || 'image/jpeg';
            const reader = new FileReader();
            reader.onload = (loadEvent) => {
                const dataUrl = loadEvent.target.result;
                pendingScreenshotBase64 = dataUrl.split(',')[1]; // Base64 raw bytes

                const img = document.getElementById('screenshotPreviewImg');
                img.src = dataUrl;
                document.getElementById('screenshotPreviewBox').style.display = 'block';
            };
            reader.readAsDataURL(file);
        };

        document.getElementById('btnRemoveScreenshot').onclick = () => {
            pendingScreenshotBase64 = '';
            pendingScreenshotMimeType = null;
            document.getElementById('formScreenshotInput').value = '';
            document.getElementById('screenshotPreviewBox').style.display = 'none';
        };

        // Generator Modal slider & switches
        const genRange = document.getElementById('genLengthRange');
        const genVal = document.getElementById('genLengthVal');
        genRange.oninput = () => {
            genVal.textContent = genRange.value;
            triggerModalGenerate();
        };

        document.getElementById('genUpper').onchange = triggerModalGenerate;
        document.getElementById('genLower').onchange = triggerModalGenerate;
        document.getElementById('genNumbers').onchange = triggerModalGenerate;
        document.getElementById('genSymbols').onchange = triggerModalGenerate;
        document.getElementById('btnGenRefresh').onclick = triggerModalGenerate;

        document.getElementById('btnGenCopy').onclick = () => {
            const text = document.getElementById('genResultText').textContent;
            copyTextToClipboard(text, 'पासवर्ड कॉपी किया गया!');
        };

        document.getElementById('btnToggleDetPw').onclick = toggleDetailsPassword;

        // Close Modal buttons
        document.querySelectorAll('[data-close]').forEach(btn => {
            btn.onclick = () => {
                const target = document.getElementById(btn.dataset.close);
                if (target) target.style.display = 'none';
            };
        });

        // Click outside modal to close
        window.onclick = (e) => {
            if (e.target.classList.contains('modal-overlay')) {
                e.target.style.display = 'none';
            }
        };
    }

    function triggerModalGenerate(saveSettings = true) {
        const len = parseInt(document.getElementById('genLengthRange').value, 10);
        const up = document.getElementById('genUpper').checked;
        const low = document.getElementById('genLower').checked;
        const num = document.getElementById('genNumbers').checked;
        const sym = document.getElementById('genSymbols').checked;
        generateNewPassword(len, up, low, num, sym, saveSettings);
    }

    function showToast(message) {
        const toast = document.getElementById('toast');
        toast.textContent = message;
        toast.classList.add('show');
        setTimeout(() => {
            toast.classList.remove('show');
        }, 3200);
    }

    function escapeHtml(str) {
        if (!str) return '';
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }

})();
