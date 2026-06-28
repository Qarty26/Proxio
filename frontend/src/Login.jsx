import { useState } from "react";
import { apiUrl } from "./apiBase";

const inputStyle = (hasError) => ({
  width: "100%", boxSizing: "border-box",
  background: "#0d0d0d", border: `1.5px solid ${hasError ? "#ff3b3b" : "#2a2a2a"}`,
  borderRadius: 6, padding: "11px 14px", color: "#f0f0f0",
  fontFamily: "'DM Mono', monospace", fontSize: 14, outline: "none",
  transition: "border-color .2s",
});

function Field({ label, error, children }) {
  return (
    <div style={{ marginBottom: 20 }}>
      <label style={{
        display: "block", fontSize: 10, letterSpacing: "0.14em",
        textTransform: "uppercase", color: "#555", marginBottom: 7,
        fontFamily: "'DM Mono', monospace"
      }}>{label}</label>
      {children}
      {error && (
        <div style={{ color: "#ff3b3b", fontSize: 12, marginTop: 5, fontFamily: "'DM Mono', monospace" }}>
          ⚠ {error}
        </div>
      )}
    </div>
  );
}

export default function Login({ onLogin, onGoRegister }) {
  const [form, setForm] = useState({ email: "", password: "" });
  const [errors, setErrors] = useState({});
  const [touched, setTouched] = useState({});
  const [loading, setLoading] = useState(false);
  const [serverError, setServerError] = useState("");

  const validate = (f) => {
    const e = {};
    if (!f.email.trim()) e.email = "Email is required.";
    if (!f.password) e.password = "Password is required.";
    return e;
  };

  const set = (key, val) => {
    const next = { ...form, [key]: val };
    setForm(next);
    if (touched[key]) {
      const errs = validate(next);
      setErrors(p => ({ ...p, [key]: errs[key] }));
    }
    setServerError("");
  };

  const blur = (key) => {
    setTouched(p => ({ ...p, [key]: true }));
    setErrors(p => ({ ...p, [key]: validate(form)[key] }));
  };

 const submit = async () => {
  setTouched({ email: true, password: true });

  const errs = validate(form);
  setErrors(errs);

  if (Object.keys(errs).length > 0) return;

  setLoading(true);
  setServerError("");

  try {

    // Trigger Spring Security to generate XSRF-TOKEN cookie
    await fetch(apiUrl("/api/auth/csrf"), {
      method: "GET",
      credentials: "include",
    });

    // Read token from cookie
    const xsrfToken = decodeURIComponent(document.cookie
                                          .split("; ")
                                          .find(c => c.startsWith("XSRF-TOKEN="))
                                          ?.split("=")[1] || ""
                                      );

    // Spring formLogin expects x-www-form-urlencoded
       const params = new URLSearchParams();
    params.append("username", form.email);
    params.append("password", form.password);


    const headers = {
      "Content-Type": "application/x-www-form-urlencoded",
    };
      if (xsrfToken) 
        headers["X-XSRF-TOKEN"] = xsrfToken;
      

    console.log(headers);
    
    const res = await fetch(apiUrl("/api/auth/login"), {
      method: "POST",

      credentials: "include",

      headers:headers,

      body: params,
    });

    console.log(res.body);

    if (res.status === 401 || res.status === 403) {
      setServerError("Invalid username or password.");
      return;
    }

    if (res.status === 500) {
      setServerError("Server error. Try again later.");
      return;
    }

    if (!res.ok) {
      setServerError("Login failed.");
      return;
    }


    const user = await res.json();
    console.log("!@#12312312331");
    console.log(user);
    console.log("!@#12312312331");

    onLogin(user);

  } catch (e) {
    console.error(e);
    setServerError("Could not connect to server.");
  } finally {
    setLoading(false);
  }
};

  const handleKey = (e) => { if (e.key === "Enter") submit(); };

  return (
    <div style={{
      minHeight: "100vh", background: "#0a0a0a",
      display: "flex", alignItems: "center", justifyContent: "center",
      fontFamily: "'DM Mono', monospace", padding: 24,
    }}>
      <style>{`
        @import url('https://fonts.googleapis.com/css2?family=Playfair+Display:ital,wght@0,700;0,900;1,700&family=DM+Mono:wght@400;500&display=swap');
        * { margin:0; padding:0; box-sizing:border-box; }
        body { background:#0a0a0a; }
        input:focus { border-color: #555 !important; }
        @keyframes fadeUp { from{opacity:0;transform:translateY(14px)}to{opacity:1;transform:none} }
      `}</style>

      <div style={{
        position: "fixed", inset: 0, pointerEvents: "none",
        backgroundImage: "linear-gradient(#1a1a1a 1px,transparent 1px),linear-gradient(90deg,#1a1a1a 1px,transparent 1px)",
        backgroundSize: "48px 48px", opacity: 0.35,
      }} />

      <div style={{ position: "relative", width: "100%", maxWidth: 420, animation: "fadeUp .45s ease both" }}>
        <div style={{ textAlign: "center", marginBottom: 40 }}>
          <div style={{
            display: "inline-block", fontSize: 11, letterSpacing: "0.22em",
            color: "#444", textTransform: "uppercase", marginBottom: 12,
          }}>Proxio Marketplace</div>
          <h1 style={{
            fontFamily: "'Playfair Display', serif", fontSize: 46,
            color: "#f0f0f0", fontWeight: 900, letterSpacing: "-0.02em", lineHeight: 1,
          }}>Sign in</h1>
          <div style={{ width: 32, height: 2, background: "#f0f0f0", margin: "14px auto 0" }} />
        </div>

        <div style={{
          background: "#111", border: "1px solid #1e1e1e",
          borderRadius: 12, padding: "36px 32px",
          boxShadow: "0 32px 64px rgba(0,0,0,0.5)",
        }}>
          {serverError && (
            <div style={{
              background: "#ff3b3b11", border: "1px solid #ff3b3b33",
              borderRadius: 7, padding: "10px 14px", marginBottom: 22,
              color: "#ff3b3b", fontSize: 13,
            }}>⚠ {serverError}</div>
          )}

          <Field label="Email" error={errors.email}>
            <input
              style={inputStyle(!!errors.email)}
              placeholder="johndoe"
              value={form.email}
              onChange={e => set("email", e.target.value)}
              onBlur={() => blur("email")}
              onKeyDown={handleKey}
            />
          </Field>

          <Field label="Password" error={errors.password}>
            <input
              style={inputStyle(!!errors.password)}
              type="password" placeholder="••••••••"
              value={form.password}
              onChange={e => set("password", e.target.value)}
              onBlur={() => blur("password")}
              onKeyDown={handleKey}
            />
          </Field>

          <button onClick={submit} disabled={loading} style={{
            width: "100%", marginTop: 8, padding: "13px 0",
            background: loading ? "#1e1e1e" : "#f0f0f0",
            color: loading ? "#555" : "#111",
            border: "none", borderRadius: 7, cursor: loading ? "not-allowed" : "pointer",
            fontFamily: "'DM Mono', monospace", fontSize: 13, fontWeight: 500,
            letterSpacing: "0.08em", transition: "background .2s, color .2s",
          }}>
            {loading ? "Signing in…" : "Sign in →"}
          </button>
        </div>

        <div style={{ textAlign: "center", marginTop: 24, fontSize: 13, color: "#444" }}>
          No account?{" "}
          <button onClick={onGoRegister} style={{
            background: "none", border: "none", color: "#f0f0f0",
            cursor: "pointer", fontFamily: "'DM Mono', monospace",
            fontSize: 13, textDecoration: "underline", textUnderlineOffset: 3,
          }}>Register</button>
        </div>
      </div>
    </div>
  );
}
