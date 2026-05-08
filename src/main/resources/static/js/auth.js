function authFetch(url, options = {}) {
    const token = localStorage.getItem("token");

    if (!token) {
        logoutAndRedirect();
        return Promise.reject(new Error("Missing token"));
    }

    options.headers = {
        ...(options.headers || {}),
        "Authorization": `Bearer ${token}`
    };

    return fetch(url, options).then(res => {
        if (res.status === 401) {
            logoutAndRedirect();
            throw new Error("Session expired");
        }

        return res;
    });
}

function logoutAndRedirect() {
    localStorage.removeItem("username");
    localStorage.removeItem("partyCode");
    localStorage.removeItem("role");
    localStorage.removeItem("token");
    window.location.href = "/index.html";
}