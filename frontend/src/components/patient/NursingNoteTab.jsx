import { useState, useEffect } from 'react';
import apiClient from '../../services/apiClient';
import RichTextEditor from '../common/RichTextEditor';
import styles from './NursingNoteTab.module.scss';

/**
 * 간호기록 탭 컴포넌트
 */
const NursingNoteTab = ({ patientId }) => {
  const [notes, setNotes] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [editingNoteId, setEditingNoteId] = useState(null);

  // 간호기록 입력 폼 상태
  const [formData, setFormData] = useState({
    content: '',
    plainText: '',
    category: 'OBSERVATION',
    isImportant: false,
  });

  // 카테고리 옵션
  const categories = [
    { value: 'OBSERVATION', label: '관찰' },
    { value: 'TREATMENT', label: '처치' },
    { value: 'MEDICATION', label: '투약' },
    { value: 'EDUCATION', label: '교육' },
    { value: 'OTHER', label: '기타' },
  ];

  // 간호기록 목록 조회
  const fetchNotes = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await apiClient.get(`/nursing-notes/patient/${patientId}`);
      setNotes(response.data.data || []);
    } catch (err) {
      console.error('간호기록 목록 조회 실패:', err);
      setError('간호기록 목록을 불러올 수 없습니다');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (patientId) {
      fetchNotes();
    }
  }, [patientId]);

  // 에디터 내용 변경 핸들러
  const handleEditorChange = (html, text) => {
    setFormData(prev => ({
      ...prev,
      content: html,
      plainText: text,
    }));
  };

  // 입력값 변경 핸들러
  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value,
    }));
  };

  // 간호기록 등록/수정
  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);

    if (!formData.content.trim()) {
      setError('기록 내용을 입력해주세요');
      return;
    }

    try {
      const payload = {
        patientId,
        content: formData.content,
        plainText: formData.plainText,
        category: formData.category,
        isImportant: formData.isImportant,
      };

      if (editingNoteId) {
        await apiClient.put(`/nursing-notes/${editingNoteId}`, payload);
      } else {
        await apiClient.post('/nursing-notes', payload);
      }
      
      resetForm();
      fetchNotes();
    } catch (err) {
      console.error('간호기록 저장 실패:', err);
      setError(err.response?.data?.message || '간호기록 저장에 실패했습니다');
    }
  };

  // 폼 초기화
  const resetForm = () => {
    setFormData({
      content: '',
      plainText: '',
      category: 'OBSERVATION',
      isImportant: false,
    });
    setEditingNoteId(null);
    setShowForm(false);
  };

  // 수정 모드로 전환
  const handleEdit = (note) => {
    setFormData({
      content: note.content || '',
      plainText: note.plainText || '',
      category: note.category || 'OBSERVATION',
      isImportant: note.isImportant || false,
    });
    setEditingNoteId(note.id);
    setShowForm(true);
  };

  // 시간 포맷
  const formatTime = (dateTime) => {
    const date = new Date(dateTime);
    return date.toLocaleString('ko-KR', {
      timeZone: 'Asia/Seoul',
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      hour12: false,
    });
  };

  if (loading && notes.length === 0) {
    return <div className={styles.loading}>간호기록을 불러오는 중...</div>;
  }

  return (
    <div className={styles.noteTab}>
      <div className={styles.header}>
        <h3>간호기록</h3>
        <button 
          className={styles.addButton}
          onClick={() => {
            if (showForm) {
              resetForm();
            } else {
              setShowForm(true);
            }
          }}
        >
          {showForm ? '취소' : '+ 기록 작성'}
        </button>
      </div>

      {error && <div className={styles.error}>{error}</div>}

      {/* 간호기록 입력 폼 */}
      {showForm && (
        <form className={styles.form} onSubmit={handleSubmit}>
          <div className={styles.formHeader}>
            <div className={styles.formGroup}>
              <label>카테고리</label>
              <select
                name="category"
                value={formData.category}
                onChange={handleChange}
                className={styles.select}
              >
                {categories.map(cat => (
                  <option key={cat.value} value={cat.value}>
                    {cat.label}
                  </option>
                ))}
              </select>
            </div>

            <div className={styles.formGroup}>
              <label className={styles.checkboxLabel}>
                <input
                  type="checkbox"
                  name="isImportant"
                  checked={formData.isImportant}
                  onChange={handleChange}
                />
                <span>중요 기록</span>
              </label>
            </div>
          </div>

          <div className={styles.formGroup}>
            <label>기록 내용</label>
            <RichTextEditor
              value={formData.content}
              onChange={handleEditorChange}
              placeholder="간호기록을 작성하세요..."
            />
          </div>

          <div className={styles.formActions}>
            <button type="submit" className={styles.submitButton}>
              {editingNoteId ? '수정' : '등록'}
            </button>
            {editingNoteId && (
              <button 
                type="button" 
                className={styles.cancelButton}
                onClick={resetForm}
              >
                취소
              </button>
            )}
          </div>
        </form>
      )}

      {/* 간호기록 이력 목록 */}
      <div className={styles.noteList}>
        {notes.length === 0 ? (
          <div className={styles.empty}>등록된 간호기록이 없습니다</div>
        ) : (
          notes.map((note) => (
            <div key={note.id} className={styles.noteItem}>
              <div className={styles.noteHeader}>
                <div className={styles.noteHeaderLeft}>
                  <span className={styles.category}>
                    {categories.find(c => c.value === note.category)?.label || note.category}
                  </span>
                  {note.isImportant && (
                    <span className={styles.importantBadge}>중요</span>
                  )}
                  {note.aiSuggested && (
                    <span className={styles.aiBadge}>AI 제안</span>
                  )}
                </div>
                <div className={styles.noteHeaderRight}>
                  <span className={styles.time}>{formatTime(note.createdAt)}</span>
                  <span className={styles.nurse}>{note.nurseName}</span>
                  {note.canEdit && (
                    <button 
                      className={styles.editButton}
                      onClick={() => handleEdit(note)}
                      title="기록 수정"
                    >
                      수정
                    </button>
                  )}
                </div>
              </div>

              <div 
                className={styles.noteContent}
                dangerouslySetInnerHTML={{ __html: note.content }}
              />

              {note.updatedAt && note.updatedAt !== note.createdAt && (
                <div className={styles.noteFooter}>
                  <span className={styles.updated}>
                    수정됨: {formatTime(note.updatedAt)}
                  </span>
                </div>
              )}
            </div>
          ))
        )}
      </div>
    </div>
  );
};

export default NursingNoteTab;
