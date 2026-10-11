import styles from './OrderTab.module.scss';

  // 식약처 허가정보는 긴 단일 문자열로 내려오므로 문장/번호 항목별로 나눠 표시한다.
const normalizeDrugText = (text) => text
    .replaceAll('&nbsp;', ' ')
    .replaceAll('&#x2022;', '•')
    .replaceAll('&#x2981;', '•')
    .replace(/\s*ㆍ\s*/g, '\n')
    .replaceAll('&lt;', '<')
    .replaceAll('&gt;', '>')
    .replaceAll('&amp;', '&');

export function DrugDocument({ text }) {
    if (!text) return null;

    const items = normalizeDrugText(text)
      .replace(/\s*(\[\[SECTION:[^\]]+\]\])\s*/g, '\n$1\n')
      .replace(/\s+(?=○\s)/g, '\n')
      .replace(/\s+(?=\d+[.)]\s)/g, '\n')
      .split(/\n+|(?<=다\.)\s+(?=[가-힣A-Z0-9])/)
      .map((item) => item.trim())
      .filter(Boolean);

    return (
      <ul className={styles.documentList}>
        {items.map((item, index) => {
          const sectionMatch = item.match(/^\[\[SECTION:(.+)\]\]$/);
          if (sectionMatch) {
            return (
              <li className={styles.clinicalDocumentHeading} key={`${index}-${item.slice(0, 12)}`}>
                {sectionMatch[1].replace(/[<>]/g, '')}
              </li>
            );
          }
          const numberMatch = item.match(/^(\d+[.)])\s*(.*)$/);
          if (numberMatch) {
            return (
              <li className={styles.clinicalNumberRow} key={`${index}-${item.slice(0, 12)}`}>
                <span>{numberMatch[1]}</span>
                <p>{numberMatch[2]}</p>
              </li>
            );
          }
          const topicMatch = item.match(
            /^○\s*(유효균종|적응증|성인 및 12세 이상 소아|신생아|영ㆍ유아 및 소아(?:\([^)]*\))?|고령자|수막염|임질|수술 전ㆍ후 감염증예방|신장 및 간기능 장애)\s*[:：-]?\s*(.*)$/
          );
          if (topicMatch) {
            const detailItems = topicMatch[1] === '적응증'
              ? topicMatch[2].split(/\s+-\s+/).map((value) => value.trim()).filter(Boolean)
              : [];
            return (
              <li className={styles.clinicalTopicRow} key={`${index}-${item.slice(0, 12)}`}>
                <span>{topicMatch[1]}</span>
                {detailItems.length > 1 ? (
                  <div className={styles.indicationGrid}>
                    {detailItems.map((value) => <span key={value}>{value}</span>)}
                  </div>
                ) : (
                  <p>{topicMatch[2]}</p>
                )}
              </li>
            );
          }
          if (item.startsWith('○')) {
            return (
              <li className={styles.clinicalTopicRow} key={`${index}-${item.slice(0, 12)}`}>
                <p>{item.replace(/^○\s*/, '')}</p>
              </li>
            );
          }
          return <li key={`${index}-${item.slice(0, 12)}`}>{item}</li>;
        })}
      </ul>
    );
}

export function CautionDocument({ text }) {
    if (!text) return null;

    const rows = normalizeDrugText(text)
      .replace(/\s*(\[\[SECTION:[^\]]+\]\])\s*/g, '\n$1\n')
      .replace(/\s+(?=\d+\)\s)/g, '\n')
      .split(/\n+/)
      .map((row) => row.trim())
      .filter(Boolean);

    return (
      <div className={styles.cautionRows}>
        {rows.map((row, index) => {
          const sectionMatch = row.match(/^\[\[SECTION:(.+)\]\]$/);
          const match = row.match(/^(\d+\))\s*(.*)$/);

          if (sectionMatch) {
            return (
              <div className={styles.cautionSectionTitle} key={`${index}-${row.slice(0, 12)}`}>
                {sectionMatch[1].replace(/[<>]/g, '')}
              </div>
            );
          }

          return (
            <div className={styles.cautionRow} key={`${index}-${row.slice(0, 12)}`}>
              {match ? (
                <>
                  <span className={styles.cautionNumber}>{match[1]}</span>
                  <p>{match[2]}</p>
                </>
              ) : (
                <p className={styles.cautionIntro}>{row}</p>
              )}
            </div>
          );
        })}
      </div>
    );
}

