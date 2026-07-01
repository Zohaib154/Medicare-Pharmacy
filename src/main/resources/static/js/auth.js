const Auth = {
    init() {
        const form = document.getElementById('login-form');
        if (form) {
            form.addEventListener('submit', async (e) => {
                e.preventDefault();
                const username = document.getElementById('login-username').value;
                const password = document.getElementById('login-password').value;
                await this.login(username, password);
            });
        }
    },

    async login(username, password) {
        try {
            const data = await API.post('/auth/login', { username, password });
            
            // Save tokens and user info
            API.saveTokens(data.accessToken, data.refreshToken);
            API.saveUserInfo({
                userId: data.userId,
                username: data.username,
                fullName: data.fullName,
                roles: data.roles
            });

            App.toast(`Authenticated successfully as ${data.fullName}!`, 'success');
            
            // Load and switch to main application
            App.showMainApp(data);

            // If admin, refresh quick-login list to show only active users
            const roles = data.roles || [];
            if (roles.includes('ROLE_ADMIN')) {
                Staff.loadQuickLoginButtons();
            }
        } catch (e) {
            App.toast(e.message || 'Authentication failed. Please check credentials.', 'error');
        }
    },

    async quickLogin(username, password) {
        document.getElementById('login-username').value = username;
        document.getElementById('login-password').value = password;
        await this.login(username, password);
    },

    async logout() {
        const userInfo = API.getUserInfo();
        if (userInfo?.username) {
            try {
                await fetch(`${API.baseUrl}/auth/logout?username=${encodeURIComponent(userInfo.username)}`, {
                    method: 'POST',
                    headers: API.getHeaders()
                });
            } catch (e) {
                console.error('Sign-out invalidate request failed', e);
            }
        }

        API.clearTokens();

        const form = document.getElementById('login-form');
        if (form) form.reset();

        // Restore default quick-login buttons on logout
        this.resetQuickLoginButtons();

        App.toast('Logged out successfully.', 'warning');
        App.showLoginOverlay();
    },

    // Restore default hardcoded quick-login buttons (shown when not yet authenticated)
    resetQuickLoginButtons() {
        const container = document.getElementById('quick-login-btns');
        if (!container) return;
        container.innerHTML = `
            <button class="btn btn-secondary btn-sm" onclick="Auth.quickLogin('admin', 'admin123')">ADMIN</button>
            <button class="btn btn-secondary btn-sm" onclick="Auth.quickLogin('pharmacist', 'pharma123')">PHARMA</button>
            <button class="btn btn-secondary btn-sm" onclick="Auth.quickLogin('cashier', 'cash123')">CASHIER</button>
        `;
    }
};

document.addEventListener('DOMContentLoaded', () => {
    Auth.init();
});
