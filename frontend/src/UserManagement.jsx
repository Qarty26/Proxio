import { useState, useEffect, useCallback } from "react";
import { apiUrl } from "./apiBase";

const API_BASE = apiUrl("/api/users");
const ROLES = ["ADMIN", "VENDOR", "CUSTOMER"];
const initialForm = { email: "", password: "", fullName: "", role: "" };

function validate(form, isEdit = false) {
  const errors = {};
  if (!form.fullName.trim()) errors.fullName = "Full name is required.";
  else if (form.fullName.trim().length < 2) errors.fullName = "Min 2 characters.";
  if (!form.email.trim()) errors.email = "Email is required.";
  else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) errors.email = "Enter a valid email address.";
  if (!isEdit || form.password) {
    if (!form.password) errors.password = "Password is required.";
    else if (form.password.length < 6) errors.password = "Min 6 characters.";
  }
  if (!form.role) errors.role = "Role is required.";
  return errors;
}

function Toast({ toasts, remove }) {
  return (
    <div style={{
      position: "fixed", top: 24, right: 24, zIndex: 9999,
      display: "flex", flexDirection: "column", gap: 10, pointerEvents: "none"
    }}>
      {toasts.map(t => (
        <div key={t.id} onClick={() => remove(t.id)} style={{
          background: t.type === "error" ? "#ff3b3b" : "#00c48c",
          color: "#fff", padding: "12px 20px", borderRadius: 8,
          fontFamily: "'DM Mono', monospace", fontSize: 13,
          boxShadow: "0 4px 20px rgba(0,0,0,0.25)",
          animation: "slideIn .25s ease", pointerEvents: "all", cursor: "pointer",
          display: "flex", alignItems: "center", gap: 10
        }}>
          <span>{t.type === "error" ? "✗" : "✓"}</span>{t.message}
        </div>
      ))}
    </div>
  );
}

function useToast() {
  const [toasts, setToasts] = useState([]);
  const add = useCallback((message, type = "success") => {
    const id = Date.now();
    setToasts(p => [...p, { id, message, type }]);
    setTimeout(() => setToasts(p => p.filter(t => t.id !== id)), 3500);
  }, []);
  const remove = useCallback(id => setToasts(p => p.filter(t => t.id !== id)), []);
  return { toasts, add, remove };
}

function Field({ label, error, children }) {
  return (
    <div style={{ marginBottom: 18 }}>
      <label style={{
        display: "block", fontSize: 11, letterSpacing: "0.12em",
        textTransform: "uppercase", color: "#888", marginBottom: 6,
        fontFamily: "'DM Mono', monospace"
      }}>{label}</label>
      {children}
      {error && <div style={{ color: "#ff3b3b", fontSize: 12, marginTop: 5, fontFamily: "'DM Mono', monospace" }}>⚠ {error}</div>}
    </div>
  );
}

const inputStyle = (hasError) => ({
  width: "100%", boxSizing: "border-box",
  background: "#0d0d0d", border: `1.5px solid ${hasError ? "#ff3b3b" : "#2a2a2a"}`,
  borderRadius: 6, padding: "10px 14px", color: "#f0f0f0",
  fontFamily: "'DM Mono', monospace", fontSize: 14, outline: "none",
  transition: "border-color .2s",
});

function Modal({ title, onClose, children }) {
  return (
    <div style={{
      position: "fixed", inset: 0, background: "rgba(0,0,0,0.75)",
      display: "flex", alignItems: "center", justifyContent: "center",
      zIndex: 1000, backdropFilter: "blur(4px)"
    }}>
      <div style={{
        background: "#111", border: "1px solid #222", borderRadius: 12,
        width: "100%", maxWidth: 480, padding: 32,
        boxShadow: "0 24px 64px rgba(0,0,0,0.6)",
        animation: "modalIn .2s ease"
      }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 24 }}>
          <h2 style={{ margin: 0, fontFamily: "'Playfair Display', serif", fontSize: 22, color: "#f0f0f0", fontWeight: 700 }}>{title}</h2>
          <button onClick={onClose} style={{ background: "none", border: "none", color: "#555", fontSize: 22, cursor: "pointer", lineHeight: 1, padding: 4 }}>×</button>
        </div>
        {children}
      </div>
    </div>
  );
}

function UserForm({ initial, onSubmit, onCancel, loading }) {
  const isEdit = !!initial?.id;
  const [form, setForm] = useState(initial ? {
    email: initial.email || "", password: "",
    fullName: initial.fullName || "", role: initial.role || "",
  } : initialForm);
  const [errors, setErrors] = useState({});
  const [touched, setTouched] = useState({});

  const set = (key, val) => {
    setForm(p => ({ ...p, [key]: val }));
    if (touched[key]) {
      const errs = validate({ ...form, [key]: val }, isEdit);
      setErrors(p => ({ ...p, [key]: errs[key] }));
    }
  };
  const blur = (key) => {
    setTouched(p => ({ ...p, [key]: true }));
    setErrors(p => ({ ...p, [key]: validate(form, isEdit)[key] }));
  };
  const submit = () => {
    const allTouched = Object.fromEntries(Object.keys(form).map(k => [k, true]));
    setTouched(allTouched);
    const errs = validate(form, isEdit);
    setErrors(errs);
    if (Object.keys(errs).length === 0) onSubmit(form);
  };

  return (
    <div>
      <Field label="Full Name" error={errors.fullName}>
        <input style={inputStyle(!!errors.fullName)} value={form.fullName}
          onChange={e => set("fullName", e.target.value)} onBlur={() => blur("fullName")} placeholder="Jane Doe" />
      </Field>
      <Field label="Email" error={errors.email}>
        <input style={inputStyle(!!errors.email)} type="email" value={form.email}
          onChange={e => set("email", e.target.value)} onBlur={() => blur("email")} placeholder="jane@example.com" />
      </Field>
      <Field label={isEdit ? "New Password (leave blank to keep)" : "Password"} error={errors.password}>
        <input style={inputStyle(!!errors.password)} type="password" value={form.password}
          onChange={e => set("password", e.target.value)} onBlur={() => blur("password")} placeholder="••••••••" />
      </Field>
      <Field label="Role" error={errors.role}>
        <select style={{ ...inputStyle(!!errors.role), cursor: "pointer" }} value={form.role}
          onChange={e => set("role", e.target.value)} onBlur={() => blur("role")}>
          <option value="">— Select role —</option>
          {ROLES.map(r => <option key={r} value={r}>{r}</option>)}
        </select>
      </Field>
      <div style={{ display: "flex", gap: 10, marginTop: 8 }}>
        <button onClick={submit} disabled={loading} style={{
          flex: 1, padding: "11px 0",
          background: loading ? "#222" : "#f0f0f0", color: loading ? "#555" : "#111",
          border: "none", borderRadius: 7, fontFamily: "'DM Mono', monospace",
          fontSize: 13, fontWeight: 700, cursor: loading ? "not-allowed" : "pointer",
          letterSpacing: "0.06em", transition: "background .2s"
        }}>
          {loading ? "Saving…" : isEdit ? "Update User" : "Create User"}
        </button>
        <button onClick={onCancel} style={{
          padding: "11px 20px", background: "none",
          border: "1.5px solid #2a2a2a", borderRadius: 7, color: "#888",
          fontFamily: "'DM Mono', monospace", fontSize: 13, cursor: "pointer"
        }}>Cancel</button>
      </div>
    </div>
  );
}

function DeleteConfirm({ user, onConfirm, onCancel, loading }) {
  return (
    <Modal title="Delete User" onClose={onCancel}>
      <p style={{ color: "#aaa", fontFamily: "'DM Mono', monospace", fontSize: 14, lineHeight: 1.6 }}>
        This will permanently remove <strong style={{ color: "#f0f0f0" }}>{user.fullName}</strong> ({user.email}). Cannot be undone.
      </p>
      <div style={{ display: "flex", gap: 10, marginTop: 20 }}>
        <button onClick={onConfirm} disabled={loading} style={{
          flex: 1, padding: "11px 0", background: "#ff3b3b", color: "#fff",
          border: "none", borderRadius: 7, fontFamily: "'DM Mono', monospace",
          fontSize: 13, fontWeight: 700, cursor: loading ? "not-allowed" : "pointer"
        }}>{loading ? "Deleting…" : "Delete"}</button>
        <button onClick={onCancel} style={{
          padding: "11px 20px", background: "none", border: "1.5px solid #2a2a2a",
          borderRadius: 7, color: "#888", fontFamily: "'DM Mono', monospace", fontSize: 13, cursor: "pointer"
        }}>Cancel</button>
      </div>
    </Modal>
  );
}

const roleColors = { ADMIN: "#f5a623", VENDOR: "#4a90e2", CUSTOMER: "#00c48c" };
function RoleBadge({ role }) {
  return (
    <span style={{
      background: (roleColors[role] || "#888") + "22", color: roleColors[role] || "#aaa",
      border: `1px solid ${(roleColors[role] || "#888")}44`,
      borderRadius: 4, padding: "2px 9px", fontSize: 11,
      fontFamily: "'DM Mono', monospace", letterSpacing: "0.08em"
    }}>{role}</span>
  );
}

function ErrorPage({ code, message, onRetry }) {
  return (
    <div style={{
      minHeight: "100vh", display: "flex", flexDirection: "column",
      alignItems: "center", justifyContent: "center",
      background: "#0a0a0a", fontFamily: "'DM Mono', monospace"
    }}>
      <div style={{ fontSize: 120, fontFamily: "'Playfair Display', serif", color: "#1a1a1a", fontWeight: 900, lineHeight: 1 }}>{code}</div>
      <div style={{ color: "#555", fontSize: 15, marginTop: -16, marginBottom: 32 }}>{message}</div>
      <button onClick={onRetry} style={{
        background: "#f0f0f0", color: "#111", border: "none",
        borderRadius: 7, padding: "10px 28px", cursor: "pointer",
        fontFamily: "'DM Mono', monospace", fontSize: 13, fontWeight: 700
      }}>Try again</button>
    </div>
  );
}

export default function UserManagement({ currentUser, onLogout }) {
  const [users, setUsers] = useState([]);
  const [status, setStatus] = useState("loading");
  const [errorCode, setErrorCode] = useState(null);
  const [modal, setModal] = useState(null);
  const [actionLoading, setActionLoading] = useState(false);
  const [search, setSearch] = useState("");
  const { toasts, add: toast, remove: removeToast } = useToast();

  const load = useCallback(async () => {
    setStatus("loading");
    try {
      const res = await fetch(API_BASE, { credentials: "include" });
      if (res.status === 500) { setErrorCode(500); setStatus("error"); return; }
      if (!res.ok) { setErrorCode(res.status); setStatus("error"); return; }
      setUsers(await res.json());
      setStatus("ok");
    } catch { setErrorCode(503); setStatus("error"); }
  }, []);

  useEffect(() => { load(); }, [load]);

  const handleCreate = async (form) => {
    setActionLoading(true);
    try {      
      const xsrfToken = document.cookie
        .split("; ")
        .find(r => r.startsWith("XSRF-TOKEN="))
        ?.split("=")[1];
      
      const headers = { "Content-Type": "application/json" };

      if (xsrfToken) 
        headers["X-XSRF-TOKEN"] = xsrfToken;

      const res = await fetch(API_BASE, {
        method: "POST", headers: headers,
        body: JSON.stringify(form),
        credentials: "include",
      });
      if (!res.ok) { const err = await res.json().catch(() => ({})); toast(err.message || "Could not create user.", "error"); return; }
      const created = await res.json();
      setUsers(p => [...p, created]);
      setModal(null); toast("User created successfully.");
    } catch { toast("Network error.", "error"); }
    finally { setActionLoading(false); }
  };

  const handleUpdate = async (form) => {
    setActionLoading(true);
    try {    
      const xsrfToken = document.cookie
        .split("; ")
        .find(r => r.startsWith("XSRF-TOKEN="))
        ?.split("=")[1];
      
      const headers = { "Content-Type": "application/json" };

      if (xsrfToken) 
        headers["X-XSRF-TOKEN"] = xsrfToken;
      
      const res = await fetch(`${API_BASE}/${modal.user.id}`, {
        method: "PUT", headers: headers,
        body: JSON.stringify(form),
        credentials: "include",
      });
      if (res.status === 404) { toast("User not found.", "error"); setModal(null); load(); return; }
      if (!res.ok) { const err = await res.json().catch(() => ({})); toast(err.message || "Could not update user.", "error"); return; }
      const updated = await res.json();
      setUsers(p => p.map(u => u.id === updated.id ? updated : u));
      setModal(null); toast("User updated successfully.");
    } catch { toast("Network error.", "error"); }
    finally { setActionLoading(false); }
  };

  const handleDelete = async () => {
    setActionLoading(true);
    try {
      const xsrfToken = document.cookie
        .split("; ")
        .find(r => r.startsWith("XSRF-TOKEN="))
        ?.split("=")[1];
      
      const headers = { "Content-Type": "application/json" };

      if (xsrfToken) 
        headers["X-XSRF-TOKEN"] = xsrfToken;
      const res = await fetch(`${API_BASE}/${modal.user.id}`, { method: "DELETE", headers: headers, credentials: "include" });
      if (res.status === 404) { toast("User not found.", "error"); setModal(null); load(); return; }
      if (!res.ok) { toast("Could not delete user.", "error"); return; }
      setUsers(p => p.filter(u => u.id !== modal.user.id));
      setModal(null); toast("User deleted.");
    } catch { toast("Network error.", "error"); }
    finally { setActionLoading(false); }
  };

  const filtered = users.filter(u =>
    u.fullName?.toLowerCase().includes(search.toLowerCase()) ||
    u.email?.toLowerCase().includes(search.toLowerCase()) ||
    u.role?.toLowerCase().includes(search.toLowerCase())
  );

  if (status === "loading") return (
    <div style={{ minHeight: "100vh", display: "flex", alignItems: "center", justifyContent: "center", background: "#0a0a0a", fontFamily: "'DM Mono', monospace", color: "#333", fontSize: 13 }}>
      <div style={{ textAlign: "center" }}>
        <div style={{ fontSize: 32, marginBottom: 12, animation: "spin 1s linear infinite" }}>◌</div>
        Loading users…
      </div>
    </div>
  );

  if (status === "error") return (
    <ErrorPage
      code={errorCode}
      message={errorCode === 404 ? "Resource not found." : errorCode === 500 ? "Internal server error." : "Could not connect to server."}
      onRetry={load}
    />
  );

  return (
    <>
      <style>{`
        @import url('https://fonts.googleapis.com/css2?family=Playfair+Display:wght@700;900&family=DM+Mono:ital,wght@0,400;0,500;1,400&display=swap');
        * { margin:0; padding:0; box-sizing:border-box; }
        body { background:#0a0a0a; }
        input:focus, select:focus { border-color:#555 !important; }
        @keyframes slideIn { from{opacity:0;transform:translateX(16px)}to{opacity:1;transform:none} }
        @keyframes modalIn { from{opacity:0;transform:scale(.97)}to{opacity:1;transform:none} }
        @keyframes spin { to{transform:rotate(360deg)} }
        @keyframes fadeUp { from{opacity:0;transform:translateY(10px)}to{opacity:1;transform:none} }
        tr:hover td { background:#141414 !important; }
        .action-btn:hover { opacity:.75 !important; }
      `}</style>

      <Toast toasts={toasts} remove={removeToast} />

      <div style={{ minHeight: "100vh", background: "#0a0a0a", padding: "40px 32px", maxWidth: 1100, margin: "0 auto" }}>

        {/* Header */}
        <div style={{ marginBottom: 40, animation: "fadeUp .4s ease", display: "flex", justifyContent: "space-between", alignItems: "flex-start" }}>
          <div>
            <div style={{ display: "flex", alignItems: "baseline", gap: 16, marginBottom: 4 }}>
              <h1 style={{ fontFamily: "'Playfair Display', serif", fontSize: 38, color: "#f0f0f0", fontWeight: 900, letterSpacing: "-0.02em" }}>Users</h1>
              <span style={{ fontFamily: "'DM Mono', monospace", fontSize: 12, color: "#444", letterSpacing: "0.1em" }}>PROXIO / ADMIN</span>
            </div>
            <div style={{ width: 40, height: 2, background: "#f0f0f0", marginBottom: 8 }} />
            <p style={{ color: "#555", fontFamily: "'DM Mono', monospace", fontSize: 13 }}>
              Manage platform accounts — {users.length} total
            </p>
          </div>

          {/* Session info */}
          <div style={{ display: "flex", alignItems: "center", gap: 16, marginTop: 8 }}>
            <div style={{ textAlign: "right" }}>
              <div style={{ color: "#f0f0f0", fontFamily: "'DM Mono', monospace", fontSize: 13 }}>
                {currentUser?.fullName || currentUser?.email}
              </div>
              <div style={{ marginTop: 4 }}>
                <RoleBadge role={currentUser?.role} />
              </div>
            </div>
            <button onClick={onLogout} style={{
              background: "none", border: "1px solid #2a2a2a", color: "#666",
              borderRadius: 7, padding: "8px 16px", cursor: "pointer",
              fontFamily: "'DM Mono', monospace", fontSize: 12,
            }}>Sign out</button>
          </div>
        </div>

        {/* Toolbar */}
        <div style={{ display: "flex", gap: 12, marginBottom: 24, animation: "fadeUp .4s ease .05s both" }}>
          <input
            value={search} onChange={e => setSearch(e.target.value)}
            placeholder="Search by name, email, or role…"
            style={{
              flex: 1, background: "#0d0d0d", border: "1.5px solid #1e1e1e",
              borderRadius: 7, padding: "10px 16px", color: "#f0f0f0",
              fontFamily: "'DM Mono', monospace", fontSize: 13, outline: "none"
            }}
          />
          <button onClick={() => setModal({ type: "create" })} style={{
            background: "#f0f0f0", color: "#111", border: "none",
            borderRadius: 7, padding: "10px 22px", cursor: "pointer",
            fontFamily: "'DM Mono', monospace", fontSize: 13, fontWeight: 700,
            letterSpacing: "0.05em", whiteSpace: "nowrap"
          }}>+ New User</button>
        </div>

        {/* Table */}
        <div style={{ border: "1px solid #1a1a1a", borderRadius: 10, overflow: "hidden", animation: "fadeUp .4s ease .1s both" }}>
          <table style={{ width: "100%", borderCollapse: "collapse" }}>
            <thead>
              <tr style={{ background: "#0d0d0d", borderBottom: "1px solid #1a1a1a" }}>
                {["ID", "Full Name", "Email", "Role", ""].map(h => (
                  <th key={h} style={{
                    padding: "12px 16px", textAlign: "left", fontSize: 10,
                    letterSpacing: "0.14em", color: "#555", textTransform: "uppercase",
                    fontFamily: "'DM Mono', monospace", fontWeight: 500
                  }}>{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {filtered.length === 0 ? (
                <tr><td colSpan={5} style={{ padding: 48, textAlign: "center", color: "#333", fontFamily: "'DM Mono', monospace", fontSize: 13 }}>
                  {search ? "No users match your search." : "No users yet. Create one above."}
                </td></tr>
              ) : filtered.map((u, i) => (
                <tr key={u.id} style={{ borderBottom: "1px solid #141414", animation: `fadeUp .3s ease ${i * 0.04}s both` }}>
                  <td style={{ padding: "13px 16px", color: "#444", fontFamily: "'DM Mono', monospace", fontSize: 12 }}>#{u.id}</td>
                  <td style={{ padding: "13px 16px", color: "#f0f0f0", fontFamily: "'DM Mono', monospace", fontSize: 14 }}>{u.fullName}</td>
                  <td style={{ padding: "13px 16px", color: "#888", fontFamily: "'DM Mono', monospace", fontSize: 13 }}>{u.email}</td>
                  <td style={{ padding: "13px 16px" }}><RoleBadge role={u.role} /></td>
                  <td style={{ padding: "13px 16px", textAlign: "right" }}>
                    <button className="action-btn" onClick={() => setModal({ type: "edit", user: u })} style={{
                      background: "none", border: "1px solid #2a2a2a", color: "#aaa",
                      borderRadius: 5, padding: "5px 14px", cursor: "pointer",
                      fontFamily: "'DM Mono', monospace", fontSize: 12, marginRight: 8, transition: "opacity .15s"
                    }}>Edit</button>
                    <button className="action-btn" onClick={() => setModal({ type: "delete", user: u })} style={{
                      background: "none", border: "1px solid #2a2a2a", color: "#ff3b3b",
                      borderRadius: 5, padding: "5px 14px", cursor: "pointer",
                      fontFamily: "'DM Mono', monospace", fontSize: 12, transition: "opacity .15s"
                    }}>Delete</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <div style={{ marginTop: 32, textAlign: "center", color: "#2a2a2a", fontFamily: "'DM Mono', monospace", fontSize: 11 }}>
          PROXIO © {new Date().getFullYear()}
        </div>
      </div>

      {modal?.type === "create" && (
        <Modal title="New User" onClose={() => setModal(null)}>
          <UserForm onSubmit={handleCreate} onCancel={() => setModal(null)} loading={actionLoading} />
        </Modal>
      )}
      {modal?.type === "edit" && (
        <Modal title="Edit User" onClose={() => setModal(null)}>
          <UserForm initial={modal.user} onSubmit={handleUpdate} onCancel={() => setModal(null)} loading={actionLoading} />
        </Modal>
      )}
      {modal?.type === "delete" && (
        <DeleteConfirm user={modal.user} onConfirm={handleDelete} onCancel={() => setModal(null)} loading={actionLoading} />
      )}
    </>
  );
}
