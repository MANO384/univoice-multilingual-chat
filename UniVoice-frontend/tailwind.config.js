/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        whatsapp: {
          dark: '#111b21',
          panel: '#202c33',
          header: '#2a3942',
          bubbleOut: '#005c4b',
          bubbleIn: '#202c33',
          textMuted: '#8696a0',
          accent: '#00a884'
        }
      }
    },
  },
  plugins: [],
}