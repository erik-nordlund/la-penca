function authFetch(url, options = {}) {
    return doAuthFetch(url, options, true);
}

function doAuthFetch(url, options = {}, allowRefresh) {
    const token = localStorage.getItem("token");

    if (!token) {
        if (allowRefresh) {
            return refreshAccessToken()
                .then(() => doAuthFetch(url, options, false))
                .catch(() => {
                    logoutAndRedirect();
                    throw new Error("Session expired");
                });
        }

        logoutAndRedirect();
        return Promise.reject(new Error("Missing token"));
    }

    options.headers = {
        ...(options.headers || {}),
        "Authorization": `Bearer ${token}`
    };

    return fetch(url, options).then(res => {
        if (res.status !== 401 || !allowRefresh) {
            return res;
        }

        return refreshAccessToken()
            .then(() => doAuthFetch(url, options, false))
            .catch(() => {
                logoutAndRedirect();
                throw new Error("Session expired");
            });
    });
}

function refreshAccessToken() {
    const refreshToken = localStorage.getItem("refreshToken");

    if (!refreshToken) {
        return Promise.reject(new Error("Missing refresh token"));
    }

    return fetch("/auth/refresh", {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({
            refreshToken
        })
    })
        .then(res => {
            if (!res.ok) {
                throw new Error("Could not refresh token");
            }

            return res.json();
        })
        .then(user => {
            localStorage.setItem("username", user.username);
            localStorage.setItem("role", user.role);
            localStorage.setItem("token", user.token);
            localStorage.setItem("refreshToken", user.refreshToken);
        });
}

function logoutAndRedirect() {
    localStorage.removeItem("username");
    localStorage.removeItem("partyCode");
    localStorage.removeItem("role");
    localStorage.removeItem("token");
    localStorage.removeItem("refreshToken");
    window.location.href = "/index.html";
}