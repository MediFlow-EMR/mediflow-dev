import { currentRecordDateTime } from '../../utils/dateFormat';
import DatePicker from './DatePicker';
import CustomSelect from './CustomSelect';
import { dateValue } from '../../utils/calendar';
import styles from './DateTimePicker.module.scss';
const options = count => Array.from({ length: count }, (_, value) => ({ value: String(value).padStart(2, '0'), label: String(value).padStart(2, '0') }));
const hours = options(24);
const minutes = options(60);
export default function DateTimePicker({ id, label, value = '', onChange, disabled }) {
    const [date = '', time = '00:00'] = value.split('T');
    const [hour = '00', minute = '00'] = time.split(':');
    const setTime = (h, m) => onChange(`${date || currentRecordDateTime().slice(0, 10)}T${h}:${m}`);
    return <div className={styles.field} role="group" aria-label={label}>
        <DatePicker id={id} label={`${label} 날짜`} selectedDate={date} disabled={disabled}
            onDateChange={day => onChange(`${dateValue(day)}T${hour}:${minute}`)} />
        <div className={styles.time}>
            <CustomSelect label={`${label} 시`} value={hour} options={hours} disabled={disabled} onChange={next => setTime(next, minute)} />
            <span aria-hidden="true">:</span>
            <CustomSelect label={`${label} 분`} value={minute} options={minutes} disabled={disabled} onChange={next => setTime(hour, next)} />
        </div>
    </div>;
}
