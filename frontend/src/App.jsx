import { useState } from "react";
import Login from "./Login";
import Register from "./Register";
import UserManagement from "./UserManagement";

// ── Unauthorized page ─────────────────────────────────────────────────────────
function Unauthorized({ onLogout }) {
  return (
    <div style={{
      minHeight: "100vh", background: "#0a0a0a",
      display: "flex", flexDirection: "column",
      alignItems: "center", justifyContent: "center",
      fontFamily: "'DM Mono', monospace"
    }}>
      <style>{`@import url('https://fonts.googleapis.com/css2?family=Playfair+Display:wght@900&family=DM+Mono:wght@400;500&display=swap');`}</style>
      <div style={{
        fontSize: 120, fontFamily: "'Playfair Display', serif",
        color: "#1a1a1a", fontWeight: 900, lineHeight: 1, userSelect: "none"
      }}>403</div>
      <div style={{ color: "#555", fontSize: 14, marginTop: -12, marginBottom: 8 }}>
        Admin access only.
      </div>
      <div style={{ color: "#333", fontSize: 12, marginBottom: 32 }}>
        Your account role: <span style={{ color: "#f5a623" }}>{localStorage.getItem("proxio_role") || "unknown"}</span>
      </div>
      <button onClick={onLogout} style={{
        background: "#f0f0f0", color: "#111", border: "none",
        borderRadius: 7, padding: "10px 28px", cursor: "pointer",
        fontFamily: "'DM Mono', monospace", fontSize: 13, fontWeight: 700
      }}>Sign out</button>
    </div>
  );
}

// ── Auth helpers ──────────────────────────────────────────────────────────────
function loadUser() {
  try {
    const s = localStorage.getItem("proxio_user");
    return s ? JSON.parse(s) : null;
  } catch { return null; }
}

function saveUser(user) {
  localStorage.setItem("proxio_user", JSON.stringify(user));
  localStorage.setItem("proxio_role", user.role);
}

function clearUser() {
  localStorage.removeItem("proxio_user");
  localStorage.removeItem("proxio_role");
}

// ── App ───────────────────────────────────────────────────────────────────────
export default function App() {
  const [user, setUser] = useState(loadUser);
  const [page, setPage] = useState("login"); // "login" | "register"

  const handleLogin = (userData) => {
    saveUser(userData);
    setUser(userData);
  };

  const handleLogout = () => {
    clearUser();
    setUser(null);
    setPage("login");
  };

  // Not logged in → show login or register
  if (!user) {
    if (page === "register") {
      return (
        <Register
          onRegistered={() => setPage("login")}
          onGoLogin={() => setPage("login")}
        />
      );
    }
    return (
      <Login
        onLogin={handleLogin}
        onGoRegister={() => setPage("register")}
      />
    );
  }

  // Logged in but not admin → 403
  if (user.role !== "ADMIN") {
    return <Unauthorized onLogout={handleLogout} />;
  }

  // Admin → user management dashboard
  return <UserManagement currentUser={user} onLogout={handleLogout} />;
}
