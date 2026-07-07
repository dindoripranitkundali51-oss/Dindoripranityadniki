/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./app/**/*.{js,ts,jsx,tsx,mdx}",
    "./pages/**/*.{js,ts,jsx,tsx,mdx}",
    "./components/**/*.{js,ts,jsx,tsx,mdx}",
  ],
  theme: {
    extend: {
      colors: {
        primary: "#3b82f6", // Standard Blue
        secondary: "#64748b", // Slate
        background: "#ffffff",
        surface: "#f1f5f9",
        border: "#e2e8f0",
      },
    },
  },
  plugins: [],
}
