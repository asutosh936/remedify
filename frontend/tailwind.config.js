/** @type {import('tailwindcss').Config} */
export default {
  content: [
    './index.html',
    './src/**/*.{js,ts,jsx,tsx}',
  ],
  theme: {
    extend: {
      colors: {
        // Linear Design System Colors
        canvas: '#010102',
        'surface-1': '#1a1a1f',
        'surface-2': '#25252d',
        'surface-3': '#313139',
        'surface-4': '#3d3d45',
        hairline: '#23252a',
        'hairline-strong': '#2d2d33',
        'hairline-tertiary': '#37373f',

        // Text colors
        ink: '#f7f8f8',
        'ink-muted': '#d0d6e0',
        'ink-subtle': '#8a8f98',
        'ink-tertiary': '#62666d',

        // Primary accent (Lavender)
        primary: '#5e6ad2',
        'primary-hover': '#828fff',
        'primary-focus': '#5e69d1',

        // Semantic colors
        success: '#27a644',
        error: '#d1333d',
        warning: '#d97706',

        // Inverse
        'inverse-canvas': '#ffffff',
        'inverse-surface-1': '#f3f3f3',
        'inverse-surface-2': '#e5e5e5',
      },
      fontFamily: {
        sans: ['-apple-system', 'system-ui', 'Segoe UI', 'Roboto', 'Helvetica Neue', 'Arial', 'sans-serif'],
        mono: ['ui-monospace', 'SF Mono', 'Menlo', 'Monaco', 'Courier New', 'monospace'],
      },
      fontSize: {
        'display-xl': ['80px', { lineHeight: '1.05', letterSpacing: '-3.0px', fontWeight: '600' }],
        'display-lg': ['56px', { lineHeight: '1.10', letterSpacing: '-1.8px', fontWeight: '600' }],
        'display-md': ['40px', { lineHeight: '1.15', letterSpacing: '-1.0px', fontWeight: '600' }],
        'headline': ['28px', { lineHeight: '1.20', letterSpacing: '-0.6px', fontWeight: '600' }],
        'card-title': ['22px', { lineHeight: '1.25', letterSpacing: '-0.4px', fontWeight: '500' }],
        'subhead': ['20px', { lineHeight: '1.40', letterSpacing: '-0.2px', fontWeight: '400' }],
        'body-lg': ['18px', { lineHeight: '1.50', letterSpacing: '-0.1px', fontWeight: '400' }],
        'body': ['16px', { lineHeight: '1.50', letterSpacing: '-0.05px', fontWeight: '400' }],
        'body-sm': ['14px', { lineHeight: '1.50', letterSpacing: '0px', fontWeight: '400' }],
        'caption': ['12px', { lineHeight: '1.40', letterSpacing: '0px', fontWeight: '400' }],
        'button': ['14px', { lineHeight: '1.20', letterSpacing: '0px', fontWeight: '500' }],
        'eyebrow': ['13px', { lineHeight: '1.30', letterSpacing: '0.4px', fontWeight: '500' }],
      },
      spacing: {
        'xxs': '4px',
        'xs': '8px',
        'sm': '12px',
        'md': '16px',
        'lg': '24px',
        'xl': '32px',
        'xxl': '48px',
        'section': '96px',
      },
      borderRadius: {
        'xs': '4px',
        'sm': '6px',
        'md': '8px',
        'lg': '12px',
        'xl': '16px',
        'xxl': '24px',
      },
    },
  },
  plugins: [],
}
