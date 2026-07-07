export async function uploadPublicPoojaImage(file) {
  // File upload will be handled by .NET API in production
  console.log("Image upload requested:", file.name);
  return `/uploads/${Date.now()}_${file.name}`;
}
