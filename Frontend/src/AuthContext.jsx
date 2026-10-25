/* eslint-disable react-refresh/only-export-components */
import { createContext, useState, useCallback, useContext } from "react";

const readStoredUser = () => {
  try {
    const stored = localStorage.getItem("admin_user");
    return stored ? JSON.parse(stored) : null;
  } catch {
    return null;
  }
};

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(readStoredUser);
  const [token, setToken] = useState(() => localStorage.getItem("access_token"));

  const login = useCallback(async (email, password) => {
    const { authApi } = await import("./api.js");
    const res = await authApi.login(email, password);
    const { token: jwtToken, email: userEmail, role, id } = res.data;
    const userData = { id, email: userEmail, role };
    localStorage.setItem("access_token", jwtToken);
    localStorage.setItem("admin_user", JSON.stringify(userData));
    setToken(jwtToken);
    setUser(userData);
    return userData;
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem("access_token");
    localStorage.removeItem("admin_user");
    setToken(null);
    setUser(null);
  }, []);

  const value = { user, token, login, logout, isAuthenticated: !!token, loading: false };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
