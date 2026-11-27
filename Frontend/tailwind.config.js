module.exports = {
  content: [
    "./src/**/*.{html,ts}",
  ],
  theme: {
    extend: {
      fontFamily: {
        sans: ['"DM Sans"', 'system-ui', '-apple-system', '"Segoe UI"', 'sans-serif'],
        serif: ['"DM Sans"', 'system-ui', '-apple-system', '"Segoe UI"', 'sans-serif'],
        display: ['"DM Sans"', 'system-ui', '-apple-system', '"Segoe UI"', 'sans-serif'],
        mono: ['"DM Sans"', 'system-ui', '-apple-system', '"Segoe UI"', 'sans-serif'],
      },
      colors: {
        eco: {
          red: '#d9251b',
          redDark: '#b31d15',
          redDeep: '#8e1610',
          green: '#039340',
          greenDark: '#027a35',
          greenLight: '#18b55b',
          dark: '#161616',
          surface: '#1f1f1f',
        },
        red: {
          50: '#fef2f1',
          100: '#fde3e1',
          200: '#fccac7',
          300: '#f9a39e',
          400: '#f26d65',
          500: '#e53930',
          600: '#d9251b',
          700: '#b31d15',
          800: '#8e1610',
          900: '#6e130e',
          950: '#3f0805',
        },
        emerald: {
          50: '#eaf9f0',
          100: '#d0f2de',
          200: '#a3e4be',
          300: '#6ad097',
          400: '#2eba6d',
          500: '#10a34d',
          600: '#039340',
          700: '#027a35',
          800: '#04612c',
          900: '#054f25',
          950: '#022c14',
        },
      },
    },
  },
  plugins: [],
}
