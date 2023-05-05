// Copyright (c) 2021 Terminus, Inc.
//
// This program is free software: you can use, redistribute, and/or modify
// it under the terms of the GNU Affero General Public License, version 3
// or later ("AGPL"), as published by the Free Software Foundation.
//
// This program is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// FITNESS FOR A PARTICULAR PURPOSE.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program. If not, see <http://www.gnu.org/licenses/>.

// const path = require('path');
const plugin = require('tailwindcss/plugin');
const themeColors = require('./theme-color');

module.exports = {
  important: true,
  darkMode: false, // or 'media' or 'class'
  corePlugins: {
    preflight: false,
    container: false,
  },
  theme: {
    extend: {
      borderRadius: {
        DEFAULT: '3px',
      },
      borderColor: (theme) => ({ ...theme('colors'), normal: '#00000019' }),
      colors: themeColors,
      flex: {
        2: '2 2 0%',
        3: '3 3 0%',
        4: '4 4 0%',
        5: '5 5 0%',
        6: '6 6 0%',
        7: '7 7 0%',
        8: '8 8 0%',
      },
      fontFamily: {
        log: ['Menlo', 'PingFangSC-Regular', 'Consolas', 'Courier', 'monospace'],
        number: ['DINAlternate-Bold'],
      },
      cursor: {
        copy: 'copy',
      },
      opacity: {
        6: '0.06',
      },
      minHeight: {
        '1/4': '25%',
        '1/2': '50%',
        '3/5': '60%',
        '3/4': '75%',
      },
      maxHeight: {
        '1/4': '25%',
        '1/2': '50%',
        '3/5': '60%',
        '3/4': '75%',
      },
      width: {
        '1/10': '10%',
        '3/10': '30%',
        '7/10': '70%',
        '9/10': '90%',
      },
      transitionProperty: {
        height: 'height',
        width: 'width',
      },
      boxShadow: {
        card: '0 1px 4px 0 rgba(48,38,71,0.16)',
        'card-lg': '0 2px 8px 0 rgba(48,38,71,0.16)',
      },
    },
  },
  variants: {
    extend: {
      margin: ['first', 'last'],
      padding: ['first', 'last'],
    },
    zIndex: ['hover'],
  },
  plugins: [
    plugin(({ addVariant, e }) => {
      addVariant('before', ({ modifySelectors, separator }) => {
        modifySelectors(({ className }) => {
          return `.${e(`before${separator}${className}`)}::before`;
        });
      });
    }),
    plugin(({ addUtilities }) => {
      const newUtilities = {
        '.nowrap': {
          overflow: 'hidden',
          'white-space': 'nowrap',
          'text-overflow': 'ellipsis',
        },
        '.not-allowed': {
          color: '#00000066',
          cursor: 'not-allowed',
        },
        '.border-all': {
          border: '1px solid #00000019',
        },
        '.border-top': {
          'border-top': '1px solid #00000019',
        },
        '.border-bottom': {
          'border-bottom': '1px solid #00000019',
        },
        '.border-left': {
          'border-left': '1px solid #00000019',
        },
        '.border-right': {
          'border-right': '1px solid #00000019',
        },
        '.border-dashed': {
          border: '1px dashed #00000019',
        },
      };
      const contentUtilities = {
        '.required': {
          content: 'attr(data-required)',
          color: '#f5222d',
        },
      };

      addUtilities(newUtilities, ['responsive', 'hover']);
      addUtilities(contentUtilities, ['before']);
    }),
  ],
  purge: {
    enabled: process.env.NODE_ENV === 'production',
    content: ['./app/**/*.tsx', './app/**/*.jsx'],
    options: {
      safelist: [/red/, /blue/, /yellow/, /green/, /orange/, /gray/, /cyan/, /grey/, /white/, /purple/, /flex-\d/],
    },
  },
};
