import React, { Suspense, lazy } from "react";

// Tải từng giao diện riêng: CSS của admin sẽ không ảnh hưởng giao diện user
const Admin = lazy(() => import("./admin"));
const User = lazy(() => import("./user"));

export default function App() {
  const path = window.location.pathname.toLowerCase();

  // /user  -> giao diện user (trang mà QR code dẫn tới)
  // còn lại -> giao diện admin (như hiện tại)
  const isUser = path === "/user" || path.startsWith("/user/") || path === "/play" || path.startsWith("/play/");

  return (
    <Suspense fallback={null}>{isUser ? <User /> : <Admin />}</Suspense>
  );
}
