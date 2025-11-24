import { useRef, useEffect } from 'react';
import styles from './RichTextEditor.module.scss';

/**
 * 간단한 리치 텍스트 에디터
 * contentEditable 기반
 */
const RichTextEditor = ({ value, onChange, placeholder }) => {
  const editorRef = useRef(null);

  // 초기값 설정
  useEffect(() => {
    if (editorRef.current && value !== editorRef.current.innerHTML) {
      editorRef.current.innerHTML = value || '';
    }
  }, [value]);

  // 에디터 내용 변경 핸들러
  const handleInput = () => {
    if (editorRef.current && onChange) {
      const html = editorRef.current.innerHTML;
      const text = editorRef.current.innerText;
      onChange(html, text);
    }
  };

  // 포맷 적용
  const applyFormat = (command, value = null) => {
    document.execCommand(command, false, value);
    editorRef.current?.focus();
  };

  // 형광펜 적용
  const applyHighlight = (color) => {
    applyFormat('backColor', color);
  };

  // 글자색 적용
  const applyTextColor = (color) => {
    applyFormat('foreColor', color);
  };

  return (
    <div className={styles.editorContainer}>
      {/* 툴바 */}
      <div className={styles.toolbar}>
        <div className={styles.toolbarGroup}>
          <span className={styles.groupLabel}>형광펜</span>
          <button
            type="button"
            className={`${styles.toolButton} ${styles.highlightButton}`}
            onClick={() => applyHighlight('#fff59d')}
            title="노란색 형광펜"
          >
            <span className={styles.highlightSample} style={{ backgroundColor: '#fff59d' }}>A</span>
          </button>
          <button
            type="button"
            className={`${styles.toolButton} ${styles.highlightButton}`}
            onClick={() => applyHighlight('#ffccbc')}
            title="핑크색 형광펜"
          >
            <span className={styles.highlightSample} style={{ backgroundColor: '#ffccbc' }}>A</span>
          </button>
          <button
            type="button"
            className={`${styles.toolButton} ${styles.highlightButton}`}
            onClick={() => applyHighlight('#b2dfdb')}
            title="민트색 형광펜"
          >
            <span className={styles.highlightSample} style={{ backgroundColor: '#b2dfdb' }}>A</span>
          </button>
          <button
            type="button"
            className={`${styles.toolButton} ${styles.removeButton}`}
            onClick={() => applyHighlight('transparent')}
            title="형광펜 제거"
          >
            <span>✕</span>
          </button>
        </div>

        <div className={styles.toolbarDivider} />

        <div className={styles.toolbarGroup}>
          <span className={styles.groupLabel}>서식</span>
          <button
            type="button"
            className={`${styles.toolButton} ${styles.formatButton}`}
            onClick={() => applyFormat('bold')}
            title="굵게"
          >
            <strong>B</strong>
          </button>
          <button
            type="button"
            className={`${styles.toolButton} ${styles.formatButton}`}
            onClick={() => applyFormat('underline')}
            title="밑줄"
          >
            <u>U</u>
          </button>
        </div>

        <div className={styles.toolbarDivider} />

        <div className={styles.toolbarGroup}>
          <span className={styles.groupLabel}>글자색</span>
          <button
            type="button"
            className={`${styles.toolButton} ${styles.colorButton}`}
            onClick={() => applyTextColor('#000000')}
            title="검정"
          >
            <span className={styles.colorSample} style={{ color: '#000000' }}>A</span>
          </button>
          <button
            type="button"
            className={`${styles.toolButton} ${styles.colorButton}`}
            onClick={() => applyTextColor('#ef4444')}
            title="빨강"
          >
            <span className={styles.colorSample} style={{ color: '#ef4444' }}>A</span>
          </button>
          <button
            type="button"
            className={`${styles.toolButton} ${styles.colorButton}`}
            onClick={() => applyTextColor('#3b82f6')}
            title="파랑"
          >
            <span className={styles.colorSample} style={{ color: '#3b82f6' }}>A</span>
          </button>
        </div>
      </div>

      {/* 에디터 영역 */}
      <div
        ref={editorRef}
        className={styles.editor}
        contentEditable
        onInput={handleInput}
        data-placeholder={placeholder || '내용을 입력하세요...'}
        suppressContentEditableWarning
      />
    </div>
  );
};

export default RichTextEditor;
