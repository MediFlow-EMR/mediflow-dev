export default function RequestError({ message, onRetry, className }) {
    if (!message) return null;
    return <div role="alert" className={className}><span>{message}</span>{onRetry && <button className="retry-request" type="button" onClick={onRetry}>다시 불러오기</button>}</div>;
}
