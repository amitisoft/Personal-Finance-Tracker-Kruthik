/** @type {import("tailwindcss").Config} */
export default {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        brand: {
          50: "#eef2ff",
          100: "#e0e7ff",
          200: "#c7d2fe",
          300: "#a5b4fc",
          400: "#818cf8",
          500: "#4f46e5",
          600: "#4338ca",
          700: "#3730a3",
          800: "#1e3a8a",
          900: "#0f172a"
        },
        neutral: {
          25: "#fcfcfd",
          50: "#f8fafc",
          100: "#f1f5f9",
          200: "#e2e8f0",
          300: "#cbd5e1",
          400: "#94a3b8",
          500: "#64748b",
          600: "#475569",
          700: "#334155",
          800: "#1f2937",
          900: "#111827"
        },
        success: {
          50: "#ecfdf5",
          100: "#d1fae5",
          600: "#059669",
          700: "#047857"
        },
        danger: {
          50: "#fef2f2",
          100: "#fee2e2",
          600: "#dc2626",
          700: "#b91c1c"
        },
        warning: {
          50: "#fffbeb",
          100: "#fef3c7",
          600: "#d97706"
        }
      },
      boxShadow: {
        soft: "0 12px 28px rgba(15, 23, 42, 0.08)",
        panel: "0 2px 8px rgba(15, 23, 42, 0.04)"
      },
      fontFamily: {
        sans: ["Segoe UI", "system-ui", "sans-serif"]
      },
      animation: {
        fadein: "fadein 0.45s ease",
        "pulse-soft": "pulseSoft 1.8s ease-in-out infinite"
      },
      keyframes: {
        fadein: {
          from: { opacity: 0, transform: "translateY(8px)" },
          to: { opacity: 1, transform: "translateY(0)" }
        },
        pulseSoft: {
          "0%, 100%": { opacity: 0.65 },
          "50%": { opacity: 1 }
        }
      }
    }
  },
  plugins: []
};
