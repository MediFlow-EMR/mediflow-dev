import { useEffect, useId, useRef } from 'react';
import styles from './ConfirmModal.module.scss';

const ConfirmModal = ({ isOpen = true, onClose, onConfirm, title, message, danger = true, confirmText = '확인' }) => {
  const titleId = useId();
  const messageId = useId();
  const panelRef = useRef(null);
  const cancelRef = useRef(null);
  const closeRef = useRef(onClose);
  useEffect(() => { closeRef.current = onClose; }, [onClose]);
  useEffect(() => {
    if (!isOpen) return;
    const previousFocus = document.activeElement;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    cancelRef.current?.focus({ preventScroll: true });
    const keyDown = event => {
      if (event.key === 'Escape') { event.preventDefault(); closeRef.current(); }
      if (event.key !== 'Tab') return;
      const buttons = panelRef.current?.querySelectorAll('button:not(:disabled)');
      if (!buttons?.length) return;
      const first = buttons[0], last = buttons[buttons.length - 1];
      if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus(); }
      else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus(); }
    };
    document.addEventListener('keydown', keyDown);
    return () => {
      document.body.style.overflow = previousOverflow;
      document.removeEventListener('keydown', keyDown);
      previousFocus?.focus({ preventScroll: true });
    };
  }, [isOpen]);
  if (!isOpen) return null;
  return <div className={styles.modalOverlay} onClick={onClose}>
    <div ref={panelRef} role="alertdialog" aria-modal="true" aria-labelledby={titleId} aria-describedby={messageId}
      className={styles.modalContent} onClick={event => event.stopPropagation()}>
      <h3 id={titleId}>{title}</h3><p id={messageId}>{message}</p>
      <div className={styles.actions}>
        <button ref={cancelRef} type="button" className={styles.cancelButton} onClick={onClose}>취소</button>
        <button type="button" className={danger ? styles.confirmButton : styles.primaryConfirmButton} onClick={onConfirm}>{confirmText}</button>
      </div>
    </div>
  </div>;
};
export default ConfirmModal;
