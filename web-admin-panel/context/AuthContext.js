"use client";

import { createContext, useContext, useEffect, useState } from "react";
import { onAdminAuthStateChanged } from "../lib/authClient";
import { getAdminAccess } from "../lib/adminAccess";

const AuthContext = createContext({
  user: null,
  profile: null,
  isAdmin: false,
  loading: true,
  refreshAccess: async () => {},
});

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [profile, setProfile] = useState(null);
  const [isAdmin, setIsSuperAdmin] = useState(false);
  const [loading, setLoading] = useState(true);

  const updateAccess = async (firebaseUser) => {
    if (!firebaseUser) {
      setProfile(null);
      setIsSuperAdmin(false);
      return;
    }
    const access = await getAdminAccess(firebaseUser);
    setProfile(access.profile);
    setIsSuperAdmin(access.isAdmin);
  };

  useEffect(() => onAdminAuthStateChanged(async (firebaseUser) => {
    setLoading(true);
    setUser(firebaseUser);
    try {
      await updateAccess(firebaseUser);
    } catch (error) {
      console.error("Administrator whitelist lookup failed:", error);
      setProfile(null);
      setIsSuperAdmin(false);
    } finally {
      setLoading(false);
    }
  }), []);

  return (
    <AuthContext.Provider value={{
      user,
      profile,
      isAdmin,
      loading,
      refreshAccess: () => updateAccess(user),
    }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);
