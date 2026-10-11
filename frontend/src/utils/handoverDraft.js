export function parsePatientSections(summary) {
    const sections = [];
    const pattern = /\*\*(\[[^\n]+\])\*\*\s*\n([\s\S]*?)(?=\n\s*\*\*\[|$)/g;
    let match;
    while ((match = pattern.exec(summary)) !== null) {
        sections.push({ start: match.index, end: pattern.lastIndex, heading: match[1].replace(/^\[|\]$/g, ''), body: match[2].trim() });
    }
    return sections;
}

export function replacePatientSection(summary, heading, body) {
    const section = parsePatientSections(summary).find(item => item.heading === heading);
    if (!section) return summary;
    const tail = summary.slice(section.end).replace(/^\s*(?=\*\*\[)/, '');
    return `${summary.slice(0, section.start)}**[${section.heading}]**\n${body}${tail ? `\n\n${tail}` : ''}`;
}
