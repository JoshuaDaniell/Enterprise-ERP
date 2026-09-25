/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      fontFamily: {
        sans: ['Inter', 'system-ui', '-apple-system', 'sans-serif'],
      },
      colors: {
        neon: {
          cyan: '#06d6a0',
          violet: '#8338ec',
          coral: '#ff6b6b',
          mint: '#38e8c6',
          sky: '#4cc9f0',
          pink: '#f72585',
          gold: '#ffd166',
        },
      },
      boxShadow: {
        'neon-sm': '0 0 10px rgba(6,214,160,0.15)',
        'neon-md': '0 0 20px rgba(131,56,236,0.12)',
        'neon-lg': '0 4px 40px rgba(6,214,160,0.1), 0 0 20px rgba(131,56,236,0.08)',
        'glow': '0 0 15px rgba(76,201,240,0.25)',
      },
      backgroundImage: {
        'neon-wash': 'linear-gradient(135deg, rgba(131,56,236,0.08), rgba(6,214,160,0.08))',
      },
      animation: {
        'fade-in': 'fadeIn 0.3s ease-out',
        'slide-up': 'slideUp 0.35s ease-out',
        'glow-pulse': 'glowPulse 2s ease-in-out infinite',
      },
      keyframes: {
        fadeIn: {
          '0%': { opacity: '0' },
          '100%': { opacity: '1' },
        },
        slideUp: {
          '0%': { opacity: '0', transform: 'translateY(12px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
        glowPulse: {
          '0%, 100%': { boxShadow: '0 0 5px rgba(6,214,160,0.2)' },
          '50%': { boxShadow: '0 0 20px rgba(6,214,160,0.4)' },
        },
      },
    },
  },
  plugins: [],
}