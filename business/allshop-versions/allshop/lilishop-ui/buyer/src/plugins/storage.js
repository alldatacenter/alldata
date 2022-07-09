import Cookies from "js-cookie";
const psl = require("psl");

export default {
  setItem: (key, value, options = {}) => {
    if (process.client) {
      console.log(process.client);
      const pPsl = psl.parse(document.domain);
      let domain = pPsl.domain;
      if (/\d+\.\d+\.\d+\.\d+/.test(pPsl.input)) domain = pPsl.input;
      options = { domain, ...options };
    }
    Cookies.set(key, value, options);
  },
  getItem: key => {
    return Cookies.get(key);
  },
  removeItem: (key, options = {}) => {
    if (process.client) {
      const pPsl = psl.parse(document.domain);
      let domain = pPsl.domain;
      if (/\d+\.\d+\.\d+\.\d+/.test(pPsl.input)) domain = pPsl.input;
      options = { domain, ...options };
    }
    Cookies.remove(key, options);
  }
};
