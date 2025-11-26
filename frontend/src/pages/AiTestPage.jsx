import { useState } from 'react';
import styles from './AiTestPage.module.scss';

/**
 * AI 테스트 페이지
 */
const AiTestPage = () => {
  const [question, setQuestion] = useState('');
  const [answer, setAnswer] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleAsk = async () => {
    if (!question.trim()) {
      setError('질문을 입력해주세요');
      return;
    }

    setLoading(true);
    setError('');
    setAnswer('');

    try {
      const response = await fetch('/api/ai/ask', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        credentials: 'include',
        body: JSON.stringify({ question }),
      });

      const data = await response.json();

      if (data.success) {
        setAnswer(data.data.answer);
      } else {
        setError(data.message || 'AI 응답을 받지 못했습니다');
      }
    } catch (err) {
      console.error('AI 질문 중 오류:', err);
      setError('AI 응답 중 오류가 발생했습니다');
    } finally {
      setLoading(false);
    }
  };

  const handleKeyPress = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleAsk();
    }
  };

  return (
    <div className={styles.aiTestPage}>
      <div className={styles.header}>
        <h1>🤖 Gemini AI 테스트</h1>
        <p>Gemini API가 제대로 작동하는지 테스트해보세요</p>
      </div>

      <div className={styles.chatContainer}>
        <div className={styles.inputSection}>
          <textarea
            className={styles.questionInput}
            placeholder="AI에게 질문해보세요... (예: 안녕하세요, 간호사의 역할은 무엇인가요?)"
            value={question}
            onChange={(e) => setQuestion(e.target.value)}
            onKeyPress={handleKeyPress}
            disabled={loading}
            rows={4}
          />
          <button
            className={styles.askButton}
            onClick={handleAsk}
            disabled={loading || !question.trim()}
          >
            {loading ? (
              <>
                <span className={styles.spinner}></span>
                AI가 생각 중...
              </>
            ) : (
              <>
                <span className={styles.icon}>✨</span>
                질문하기
              </>
            )}
          </button>
        </div>

        {error && (
          <div className={styles.error}>
            <span className={styles.errorIcon}>⚠️</span>
            {error}
          </div>
        )}

        {answer && (
          <div className={styles.answerSection}>
            <div className={styles.answerHeader}>
              <span className={styles.aiIcon}>🤖</span>
              <strong>Gemini AI 응답</strong>
            </div>
            <div className={styles.answerContent}>
              {answer}
            </div>
          </div>
        )}

        {!answer && !error && !loading && (
          <div className={styles.placeholder}>
            <div className={styles.placeholderIcon}>💬</div>
            <p>AI에게 무엇이든 질문해보세요!</p>
            <p className={styles.hint}>Enter를 눌러 질문할 수 있습니다</p>
          </div>
        )}
      </div>
    </div>
  );
};

export default AiTestPage;

