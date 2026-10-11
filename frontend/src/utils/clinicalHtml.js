import DOMPurify from 'dompurify';

DOMPurify.addHook('afterSanitizeAttributes', node => {
    if (!node.hasAttribute?.('style')) return;
    const color = node.style.color;
    const background = node.style.backgroundColor;
    node.removeAttribute('style');
    if (color) node.style.color = color;
    if (background) node.style.backgroundColor = background;
});

export function sanitizeClinicalHtml(html) {
    return DOMPurify.sanitize(html || '', {
        ALLOWED_TAGS: ['p', 'br', 'div', 'span', 'b', 'strong', 'i', 'em', 'u', 's', 'ul', 'ol', 'li', 'font'],
        ALLOWED_ATTR: ['style', 'color'],
        ALLOW_DATA_ATTR: false,
    });
}
