import { formatDateTime as formatRecordDateTime } from '../../utils/dateFormat';
import { sanitizeClinicalHtml } from '../../utils/clinicalHtml';
import useUnsavedRecord from '../../hooks/useUnsavedRecord';
import CustomSelect from '../common/CustomSelect';
import useRecordList from '../../hooks/useRecordList';
import RequestError from '../common/RequestError';
import { sortRecords } from '../../utils/sortRecords';
import FilterBar from '../common/FilterBar';
import { filterNotes, noteFilterDefaults } from '../../utils/filters';
import { useState, useRef } from 'react';
import apiClient from '../../services/apiClient';
import RichTextEditor from '../common/RichTextEditor';
import styles from './NursingNoteTab.module.scss';

const NursingNoteTab = ({ patientId, onDirtyChange }) => {
  const { items: notes, loading, loadError, refresh: fetchNotes } = useRecordList(`/nursing-notes/patient/${patientId}`, '간호기록 목록을 불러올 수 없습니다');
  const [sortOrder, setSortOrder] = useState('desc');
  const [filters, setFilters] = useState(noteFilterDefaults);
  const filteredNotes = sortRecords(filterNotes(notes, filters), 'createdAt', sortOrder);

  const [error, setError] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [saving, setSaving] = useState(false);
  const saveLock = useRef(false);
  const [editingVersion, setEditingVersion] = useState(null);
  const [editingNoteId, setEditingNoteId] = useState(null);

  const [formData, setFormData] = useState({
    content: '',
    plainText: '',
    category: 'OBSERVATION',
    isImportant: false,
  });

  const categories = [
    { value: 'OBSERVATION', label: '관찰' },
    { value: 'TREATMENT', label: '처치' },
    { value: 'MEDICATION', label: '투약' },
    { value: 'EDUCATION', label: '교육' },
    { value: 'CONSULTATION', label: '상담' },
  ];

  useUnsavedRecord(showForm, onDirtyChange, saving);

  const handleEditorChange = (html, text) => {
    setFormData(prev => ({
      ...prev,
      content: html,
      plainText: text,
    }));
  };

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
    if (saveLock.current) return;
    saveLock.current = true;
    setSaving(true);
    setError(null);

    if (!formData.content.trim()) {
      saveLock.current = false; setSaving(false);
      setError('기록 내용을 입력해주세요');
      return;
    }

    try {
      const payload = {
        patientId,
        version: editingVersion,
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

      setError(err.response?.data?.message || '간호기록 저장에 실패했습니다');
    } finally { saveLock.current = false; setSaving(false); }
  };

  const resetForm = () => {
    setFormData({
      content: '',
      plainText: '',
      category: 'OBSERVATION',
      isImportant: false,
    });
    setEditingNoteId(null);
    setEditingVersion(null);
    setShowForm(false);
  };

  const handleEdit = (note) => {
    setFormData({
      content: note.content || '',
      plainText: note.plainText || '',
      category: note.category || 'OBSERVATION',
      isImportant: note.isImportant || false,
    });
    setEditingNoteId(note.id);
    setEditingVersion(note.version);
    setShowForm(true);
  };

  const formatTime = (dateTime) => {
    return formatRecordDateTime(dateTime);
  };

  if (loading && notes.length === 0) {
    return <div className={styles.loading}>간호기록을 불러오는 중...</div>;
  }

  return (
    <div className={styles.noteTab}>
      <div className={styles.header}>
        <h3>간호기록</h3>
        <button 
          className={styles.addButton} disabled={saving}
          onClick={() => {
            if (showForm) {
              resetForm();
            } else {
              setShowForm(true);
            }
          }}
        >
          {saving ? '저장 중…' : showForm ? '취소' : '+ 기록 작성'}
        </button>
      </div>

      <RequestError className={styles.error} message={error || loadError} onRetry={loadError ? fetchNotes : undefined} />

      {/* 간호기록 입력 폼 */}
      {showForm && (
        <form className={styles.form} onSubmit={handleSubmit} aria-busy={saving}>
          <fieldset className="record-form-fields" disabled={saving}>
          <div className={styles.formHeader}>
            <div className={styles.formGroup}>
              <label>카테고리</label>
              <CustomSelect label="기록 카테고리" value={formData.category} options={categories} onChange={category => setFormData(prev => ({ ...prev, category }))} />
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
              disabled={saving}
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
        </fieldset>
        </form>
      )}

      <FilterBar sortValue={sortOrder} onSortChange={setSortOrder} sortBasis="기록일" label="간호기록" values={filters} onChange={setFilters} onReset={() => setFilters(noteFilterDefaults)}
        searchPlaceholder="기록 내용 검색" count={filteredNotes.length} total={notes.length} fields={[
          { key: 'from', label: '시작일', type: 'date' }, { key: 'to', label: '종료일', type: 'date' },
          { key: 'category', label: '카테고리', type: 'select', options: categories },
          { key: 'author', label: '작성자', type: 'select', options: [...new Set(notes.map(note => note.nurseName).filter(Boolean))].map(name => ({ value: name, label: name })) },
          { key: 'important', label: '중요 기록만', type: 'checkbox' },
        ]} />
      {/* 간호기록 이력 목록 */}
      <div className={styles.noteList}>
        {filteredNotes.length === 0 ? (
          <div className={styles.empty}>{notes.length ? '필터에 맞는 간호기록이 없습니다' : '등록된 간호기록이 없습니다'}</div>
        ) : (
          filteredNotes.map((note) => (
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
                dangerouslySetInnerHTML={{ __html: sanitizeClinicalHtml(note.content) }}
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
