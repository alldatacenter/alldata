export const updateTitle = (title: string) => {
  const el = window.document.querySelector('title');
  if (!el) return;
  el.innerText = title;
};
