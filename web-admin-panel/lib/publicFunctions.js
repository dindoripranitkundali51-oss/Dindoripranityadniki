export async function callPublicFunction(name, data) {
  const baseUrl = process.env.NEXT_PUBLIC_API_URL || "https://dindoripranitapi.somee.com/api/v1";
  const token = typeof window !== "undefined" ? localStorage.getItem("jwt_auth_token") || "" : "";
  const res = await fetch(`${baseUrl}/admin/${name}`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "Authorization": token ? `Bearer ${token}` : ""
    },
    body: JSON.stringify(data)
  });
  if (!res.ok) throw new Error(`API call failed: ${res.status}`);
  return { data: await res.json() };
}
