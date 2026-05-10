import { useState } from "react";

const ROLES = ["ADMIN", "VENDOR", "CUSTOMER"];

const inputStyle = (hasError) => ({
  width: "100%", boxSizing: "border-box",
  background: "#0d0d0d", border: `1.5px solid ${hasError ? "#ff3b3b" : "#2a2a2a"}`,
  borderRadius: 6, padding: "11px 14px", color: "#f0f0f0",
  fontFamily: "'DM Mono', monospace", fontSize: 14, outline: "none",
  transition: "border-color .2s",
});

function Field({ label, error, children }) {
  return (
    <div style={{ marginBottom: 18 }}>
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

function validate(form) {
  const e = {};
  if (!form.fullName.trim()) e.fullName = "Full name is required.";
  else if (form.fullName.trim().length < 2) e.fullName = "Min 2 characters.";
  if (!form.email.trim()) e.email = "Email is required.";
  else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) e.email = "Enter a valid email.";
  if (!form.password) e.password = "Password is required.";
  else if (form.password.length < 6) e.password = "Min 6 characters.";
  if (!form.role) e.role = "Role is required.";
  return e;
}

export default function Register({ onRegistered, onGoLogin }) {
  const [form, setForm] = useState({ fullName: "", email: "", password: "", role: "" });
  const [errors, setErrors] = useState({});
  const [touched, setTouched] = useState({});
  const [loading, setLoading] = useState(false);
  const [serverError, setServerError] = useState("");
  const [success, setSuccess] = useState(false);

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
    const allTouched = Object.fromEntries(Object.keys(form).map(k => [k, true]));
    setTouched(allTouched);
    const errs = validate(form);
    setErrors(errs);
    if (Object.keys(errs).length > 0) return;
   
    setLoading(true);
    setServerError("");
    try {
      const xsrfToken = document.cookie
        .split("; ")
        .find(r => r.startsWith("XSRF-TOKEN="))
        ?.split("=")[1];
      
      const headers = { "Content-Type": "application/json" };
      if (xsrfToken) 
        headers["X-XSRF-TOKEN"] = xsrfToken;
      
      console.log(headers);

      const res = await fetch("http://localhost:8080/api/users", {
        method: "POST",
        headers: headers,
        body: JSON.stringify(form),
        credentials: "include"
      });
      
      if (res.status === 409) { setServerError("An account with this email already exists."); return; }
      if (res.status === 500) { setServerError("Server error. Try again later."); return; }
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        setServerError(err.message || "Registration failed."); return;
      }
      setSuccess(true);
      setTimeout(() => onRegistered?.(), 1800);
    } catch {
      setServerError("Could not connect to server.");
    } finally {
      setLoading(false);
    }
  };

  if (success) return (
    <div style={{
      minHeight: "100vh", background: "#0a0a0a",
      display: "flex", alignItems: "center", justifyContent: "center",
      flexDirection: "column", gap: 16, fontFamily: "'DM Mono', monospace"
    }}>
      <div style={{ fontSize: 48, animation: "pop .4s ease" }}>✓</div>
      <div style={{ color: "#00c48c", fontSize: 15 }}>Account created. Redirecting…</div>
      <style>{`@keyframes pop{from{transform:scale(0)}to{transform:scale(1)}}`}</style>
    </div>
  );

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
        input:focus, select:focus { border-color:#555 !important; }
        @keyframes fadeUp { from{opacity:0;transform:translateY(14px)}to{opacity:1;transform:none} }
      `}</style>

      <div style={{
        position: "fixed", inset: 0, pointerEvents: "none",
        backgroundImage: "linear-gradient(#1a1a1a 1px,transparent 1px),linear-gradient(90deg,#1a1a1a 1px,transparent 1px)",
        backgroundSize: "48px 48px", opacity: 0.35,
      }} />

      <div style={{ position: "relative", width: "100%", maxWidth: 420, animation: "fadeUp .45s ease both" }}>
        {/* Brand */}
        <div style={{ textAlign: "center", marginBottom: 40 }}>
          <div style={{
            display: "inline-block", fontSize: 11, letterSpacing: "0.22em",
            color: "#444", textTransform: "uppercase", marginBottom: 12,
          }}>Proxio Marketplace</div>
          <h1 style={{
            fontFamily: "'Playfair Display', serif", fontSize: 42,
            color: "#f0f0f0", fontWeight: 900, letterSpacing: "-0.02em", lineHeight: 1,
          }}>Create account</h1>
          <div style={{ width: 32, height: 2, background: "#f0f0f0", margin: "14px auto 0" }} />
        </div>

        {/* Card */}
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

          <Field label="Full Name" error={errors.fullName}>
            <input
              style={inputStyle(!!errors.fullName)}
              placeholder="Jane Doe"
              value={form.fullName}
              onChange={e => set("fullName", e.target.value)}
              onBlur={() => blur("fullName")}
            />
          </Field>

          <Field label="Email" error={errors.email}>
            <input
              style={inputStyle(!!errors.email)}
              type="email" placeholder="you@example.com"
              value={form.email}
              onChange={e => set("email", e.target.value)}
              onBlur={() => blur("email")}
            />
          </Field>

          <Field label="Password" error={errors.password}>
            <input
              style={inputStyle(!!errors.password)}
              type="password" placeholder="••••••••"
              value={form.password}
              onChange={e => set("password", e.target.value)}
              onBlur={() => blur("password")}
            />
          </Field>

          <Field label="Role" error={errors.role}>
            <select
              style={{ ...inputStyle(!!errors.role), cursor: "pointer" }}
              value={form.role}
              onChange={e => set("role", e.target.value)}
              onBlur={() => blur("role")}
            >
              <option value="">— Select role —</option>
              {ROLES.map(r => <option key={r} value={r}>{r}</option>)}
            </select>
          </Field>

          <button
            onClick={submit}
            disabled={loading}
            style={{
              width: "100%", marginTop: 8, padding: "13px 0",
              background: loading ? "#1e1e1e" : "#f0f0f0",
              color: loading ? "#555" : "#111",
              border: "none", borderRadius: 7, cursor: loading ? "not-allowed" : "pointer",
              fontFamily: "'DM Mono', monospace", fontSize: 13, fontWeight: 500,
              letterSpacing: "0.08em", transition: "background .2s, color .2s",
            }}
          >
            {loading ? "Creating account…" : "Register →"}
          </button>
        </div>

        <div style={{ textAlign: "center", marginTop: 24, fontSize: 13, color: "#444" }}>
          Already have an account?{" "}
          <button onClick={onGoLogin} style={{
            background: "none", border: "none", color: "#f0f0f0",
            cursor: "pointer", fontFamily: "'DM Mono', monospace",
            fontSize: 13, textDecoration: "underline", textUnderlineOffset: 3,
          }}>Sign in</button>
        </div>
      </div>
    </div>
  );
}
