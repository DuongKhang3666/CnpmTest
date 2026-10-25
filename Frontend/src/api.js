import axios from "axios";

const API_BASE = import.meta.env.VITE_API_BASE || "http://localhost:8080/api";

const api = axios.create({
  baseURL: API_BASE,
  headers: { "Content-Type": "application/json" },
  timeout: 15000,
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem("access_token");
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem("access_token");
      localStorage.removeItem("admin_user");
      window.location.href = "/";
    }
    return Promise.reject(err);
  }
);

export const authApi = {
  login: (email, password) => api.post("/auth/login", { email, password }),
};

export const placeApi = {
  list: (params) => api.get("/admin/places", { params }),
  get: (id) => api.get(`/admin/places/${id}`),
  create: (data) => api.post("/admin/places", data),
  update: (id, data) => api.put(`/admin/places/${id}`, data),
  delete: (id) => api.delete(`/admin/places/${id}`),
  updateStatus: (id, status) => api.patch(`/admin/places/${id}/status`, { status }),
};

export const artifactApi = {
  list: (params) => api.get("/admin/artifacts", { params }),
  get: (id) => api.get(`/admin/artifacts/${id}`),
  create: (data) => api.post("/admin/artifacts", data),
  update: (id, data) => api.put(`/admin/artifacts/${id}`, data),
  delete: (id) => api.delete(`/admin/artifacts/${id}`),
  updateStatus: (id, status) => api.patch(`/admin/artifacts/${id}/status`, { status }),
};

export const mediaApi = {
  uploadSingle: (file) => {
    const fd = new FormData();
    fd.append("file", file);
    return api.post("/admin/media/single", fd, { headers: { "Content-Type": "multipart/form-data" } });
  },
  uploadMultiple: (files) => {
    const fd = new FormData();
    files.forEach((f) => fd.append("files", f));
    return api.post("/admin/media", fd, { headers: { "Content-Type": "multipart/form-data" } });
  },
  delete: (url) => api.delete("/admin/media", { params: { url } }),
};

export const dashboardApi = {
  stats: () => api.get("/admin/dashboard/stats"),
};

export default api;